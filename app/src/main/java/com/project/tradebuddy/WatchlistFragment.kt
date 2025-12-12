package com.project.tradebuddy.ui.watchlist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.project.tradebuddy.ChartFragment
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem
import com.project.tradebuddy.WatchlistManager
import com.project.tradebuddy.api.TwelveDataService
import com.project.tradebuddy.WatchlistPickerAdapter
import com.project.tradebuddy.ui.search.StockSearchFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class WatchlistFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WatchlistAdapter

    private var imgMenu: ImageView? = null
    private var tvCurrentList: TextView? = null
    private var btnAddList: MaterialButton? = null
    private var toolbar: MaterialToolbar? = null

    private var watchlistNames: MutableList<String> = mutableListOf()
    private var currentListIndex = 0

    private val RECYCLER_ID = R.id.recyclerWatchlist
    private val IMG_MENU_ID = R.id.btnMenu
    private val TV_LIST_ID = R.id.tvCurrentList
    private val BTN_ADD_LIST_ID = R.id.btnAddList
    private val TOOLBAR_ID = R.id.toolbar
    private val FRAGMENT_CONTAINER_ID = R.id.fragment_container // ensure this matches your Activity layout

    // Twelve Data
    private lateinit var twelveApi: TwelveDataService
    private val TWELVE_DATA_API_KEY = "6ef0d621d2f242feabb69587a0b578cf" // keep your API key here

    // polling job
    private var priceJob: Job? = null

    // backoff / batching tuning (tweak these for your TwelveData plan)
    private val BASE_POLL_INTERVAL_MS = 15_000L    // base polling interval (15s)
    private val MAX_BACKOFF_MS = 60_000L          // max backoff (60s)
    private val BATCH_SIZE = 3                    // symbols per batch
    private var currentBackoffMs = BASE_POLL_INTERVAL_MS

    // handler for snackbar auto-dismiss (used elsewhere)
    private val mainHandler = Handler(Looper.getMainLooper())

    // gson for robust parsing
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init retrofit/twelveApi
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.twelvedata.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        twelveApi = retrofit.create(TwelveDataService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        // RecyclerView + adapter — long-press removal included
        recyclerView = view.findViewById(RECYCLER_ID)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = WatchlistAdapter(
            onItemClick = { stockItem ->
                openChartFor(stockItem.symbol)
            },
            onItemLongClick = { stockItem ->
                confirmAndRemoveStock(stockItem)
            }
        )
        recyclerView.adapter = adapter

        // setup swipe-to-delete (with Undo)
        setupSwipeToDelete()

        // header views
        imgMenu = view.findViewById(IMG_MENU_ID)
        tvCurrentList = view.findViewById(TV_LIST_ID)
        btnAddList = view.findViewById(BTN_ADD_LIST_ID)
        toolbar = view.findViewById(TOOLBAR_ID)

        // toolbar menu click -> open stock search on action_add_stock
        toolbar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_stock -> {
                    openStockSearch()
                    true
                }
                else -> false
            }
        }

        btnAddList?.setOnClickListener {
            showCreateListDialog()
        }

        imgMenu?.setOnClickListener {
            showWatchlistPickerDialogCustom()
        }

        loadAvailableWatchlists()
        loadCurrentWatchlist()

        return view
    }

    override fun onResume() {
        super.onResume()
        // reload lists (in case user added a stock or created a list)
        loadAvailableWatchlists()
        val currentName = WatchlistManager.getCurrentWatchlistName(requireContext())
        if (currentName != null) {
            currentListIndex = watchlistNames.indexOf(currentName).coerceAtLeast(0)
            tvCurrentList?.text = watchlistNames.getOrNull(currentListIndex) ?: currentName
            loadWatchlistByName(currentName)
        } else {
            loadCurrentWatchlist()
        }

        // start polling
        startPricePolling()
    }

    override fun onPause() {
        super.onPause()
        stopPricePolling()
    }

    // --- swipe setup ---
    private fun setupSwipeToDelete() {
        // visuals
        val deleteDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
        val background = ColorDrawable()
        // use a built in red color to avoid missing resource
        val backgroundColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        val clearPaint = Paint()

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // use adapterPosition (more broadly available than absoluteAdapterPosition)
                val pos = viewHolder.adapterPosition
                if (pos == RecyclerView.NO_POSITION) return

                // remove from adapter and keep a copy for undo
                val removedItem = adapter.removeAt(pos) ?: run {
                    adapter.notifyDataSetChanged()
                    return
                }

                // remove from persistent storage immediately (you already do this elsewhere; keep consistent)
                val currentName = WatchlistManager.getCurrentWatchlistName(requireContext()) ?: "Default"
                try {
                    WatchlistManager.removeStockFromList(requireContext(), currentName, removedItem.symbol)
                } catch (e: Exception) {
                    Log.w("WatchlistFragment", "Failed removing from storage: ${e.message}")
                }

                // show UNDO snackbar with 10s auto-dismiss
                val parentView = requireActivity().findViewById<View>(android.R.id.content)
                val snackbar = Snackbar.make(parentView, "${removedItem.symbol} removed", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Undo") {
                        // restore in-memory list and persistent storage
                        val insertPos = pos.coerceIn(0, adapter.itemCount)
                        adapter.addAt(insertPos, removedItem)
                        try {
                            WatchlistManager.addStockToList(requireContext(), currentName, removedItem)
                        } catch (e: Exception) {
                            Log.w("WatchlistFragment", "Failed re-adding to storage on undo: ${e.message}")
                        }
                        recyclerView.scrollToPosition(insertPos)
                    }

                snackbar.show()
                // auto-dismiss after 10 seconds
                mainHandler.postDelayed({ snackbar.dismiss() }, 10_000L)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                // draw red background depending on swipe direction
                background.color = backgroundColor
                if (dX > 0) {
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                } else {
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)

                // draw delete icon centered vertically
                deleteDrawable?.let { icon ->
                    val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight

                    if (dX > 0) {
                        // left side
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else if (dX < 0) {
                        // right side
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else {
                        icon.setBounds(0, 0, 0, 0)
                    }
                    icon.draw(c)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // --- helpers ---

    private fun loadAvailableWatchlists() {
        val names = WatchlistManager.getAllWatchlistNames(requireContext())
        watchlistNames = names.toMutableList()
        if (watchlistNames.isEmpty()) {
            // ensure at least "Default" exists
            WatchlistManager.createWatchlist(requireContext(), "Default")
            watchlistNames = WatchlistManager.getAllWatchlistNames(requireContext()).toMutableList()
        }
        val cur = WatchlistManager.getCurrentWatchlistName(requireContext())
        currentListIndex = if (!cur.isNullOrEmpty()) watchlistNames.indexOf(cur).coerceAtLeast(0) else 0
        tvCurrentList?.text = watchlistNames.getOrNull(currentListIndex) ?: "Default"
    }

    private fun loadCurrentWatchlist() {
        val list = WatchlistManager.getWatchlist(requireContext())
        adapter.setStocks(list)
    }

    private fun loadWatchlistByName(name: String) {
        // set as current
        WatchlistManager.setCurrentWatchlistName(requireContext(), name)
        val list = WatchlistManager.getWatchlistByName(requireContext(), name)
        adapter.setStocks(list)
    }

    private fun showCreateListDialog() {
        val edit = android.widget.EditText(requireContext())
        edit.hint = "List name"
        AlertDialog.Builder(requireContext())
            .setTitle("Create watchlist")
            .setView(edit)
            .setPositiveButton("Create") { dialog, _ ->
                val name = edit.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val created = WatchlistManager.createWatchlist(requireContext(), name)
                    if (created) {
                        loadAvailableWatchlists()
                        // set current to new one
                        val idx = watchlistNames.indexOf(name)
                        if (idx >= 0) currentListIndex = idx
                        WatchlistManager.setCurrentWatchlistName(requireContext(), name)
                        tvCurrentList?.text = name
                        loadWatchlistByName(name)
                        Toast.makeText(requireContext(), "Created $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "List with that name already exists", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmAndRemoveStock(stock: StockSearchItem) {
        val currentName = WatchlistManager.getCurrentWatchlistName(requireContext()) ?: "Default"
        AlertDialog.Builder(requireContext())
            .setTitle("Remove stock")
            .setMessage("Remove ${stock.symbol} from \"$currentName\"?")
            .setPositiveButton("Remove") { dialog, _ ->
                try {
                    WatchlistManager.removeStockFromList(requireContext(), currentName, stock.symbol)
                    // refresh shown list
                    loadWatchlistByName(currentName)
                    Toast.makeText(requireContext(), "${stock.symbol} removed", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.w("WatchlistFragment", "Failed to remove stock: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to remove ${stock.symbol}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Custom RecyclerView dialog for picking/deleting watchlists
     */
    private fun showWatchlistPickerDialogCustom() {
        loadAvailableWatchlists()
        val dlgView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_watchlist_picker, null)
        val rv = dlgView.findViewById<RecyclerView>(R.id.recyclerDialogLists)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val initialSelected = currentListIndex.coerceIn(0, watchlistNames.size - 1)
        var adapterDialog: WatchlistPickerAdapter? = null
        var pickerDialog: AlertDialog? = null

        adapterDialog = WatchlistPickerAdapter(
            items = watchlistNames,
            selectedIndex = initialSelected,
            onItemClick = { index, name ->
                currentListIndex = index
                WatchlistManager.setCurrentWatchlistName(requireContext(), name)
                tvCurrentList?.text = name
                loadWatchlistByName(name)
            },
            onDeleteClick = { index, name ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete watchlist")
                    .setMessage("Delete \"$name\"? This will remove all stocks in that list.")
                    .setPositiveButton("Delete") { delDialog, _ ->
                        try {
                            val backupItems: List<StockSearchItem> = WatchlistManager.getWatchlistByName(requireContext(), name)
                            WatchlistManager.deleteWatchlist(requireContext(), name)
                            pickerDialog?.dismiss()
                            Toast.makeText(requireContext(), "\"$name\" deleted", Toast.LENGTH_SHORT).show()
                            loadAvailableWatchlists()
                            adapterDialog?.updateItems(watchlistNames)
                            val newCurrent = WatchlistManager.getCurrentWatchlistName(requireContext())
                            if (!newCurrent.isNullOrEmpty()) {
                                tvCurrentList?.text = newCurrent
                                loadWatchlistByName(newCurrent)
                            } else {
                                val first = watchlistNames.firstOrNull() ?: "Default"
                                WatchlistManager.setCurrentWatchlistName(requireContext(), first)
                                tvCurrentList?.text = first
                                loadWatchlistByName(first)
                            }

                            // show UNDO snackbar (indefinite, auto-dismiss 10s)
                            val parentView = requireActivity().findViewById<View>(android.R.id.content)
                            val snackbar = Snackbar.make(parentView, "\"$name\" deleted", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Undo") {
                                    try {
                                        val created = WatchlistManager.createWatchlist(requireContext(), name)
                                        if (created) {
                                            if (backupItems.isNotEmpty()) {
                                                for (s in backupItems) {
                                                    WatchlistManager.addStockToList(requireContext(), name, s)
                                                }
                                            }
                                            loadAvailableWatchlists()
                                            adapterDialog?.updateItems(watchlistNames)
                                            WatchlistManager.setCurrentWatchlistName(requireContext(), name)
                                            tvCurrentList?.text = name
                                            loadWatchlistByName(name)
                                            Toast.makeText(requireContext(), "\"$name\" restored", Toast.LENGTH_SHORT).show()
                                        } else {
                                            if (backupItems.isNotEmpty()) {
                                                for (s in backupItems) {
                                                    WatchlistManager.addStockToList(requireContext(), name, s)
                                                }
                                            }
                                            loadAvailableWatchlists()
                                            adapterDialog?.updateItems(watchlistNames)
                                            WatchlistManager.setCurrentWatchlistName(requireContext(), name)
                                            tvCurrentList?.text = name
                                            loadWatchlistByName(name)
                                            Toast.makeText(requireContext(), "\"$name\" restored", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.w("WatchlistFragment", "Failed to undo delete: ${e.message}")
                                        Toast.makeText(requireContext(), "Failed to restore \"$name\"", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            snackbar.show()
                            mainHandler.postDelayed({ snackbar.dismiss() }, 10_000L)

                        } catch (e: Exception) {
                            Log.w("WatchlistFragment", "Failed to delete list: ${e.message}")
                            Toast.makeText(requireContext(), "Failed to delete $name", Toast.LENGTH_SHORT).show()
                        }
                        delDialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rv.adapter = adapterDialog

        val builder = AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .setNegativeButton("Close", null)

        pickerDialog = builder.create()
        dlgView.alpha = 0f
        dlgView.translationY = 50f
        pickerDialog?.show()
        dlgView.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }

    /**
     * Helpers for batching and error parsing
     */
    private fun chunkSymbols(csv: String, batchSize: Int = BATCH_SIZE): List<String> {
        if (csv.isBlank()) return emptyList()
        val symbols = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (symbols.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        var i = 0
        while (i < symbols.size) {
            val end = (i + batchSize).coerceAtMost(symbols.size)
            chunks.add(symbols.subList(i, end).joinToString(","))
            i = end
        }
        return chunks
    }

    private fun parsePossibleApiError(rawJson: String): Pair<Int, String>? {
        return try {
            val elem = gson.fromJson(rawJson, JsonElement::class.java)
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val code = if (obj.has("code")) {
                    try { obj.get("code").asInt } catch (_: Exception) { null }
                } else null
                val msg = if (obj.has("message")) obj.get("message").asString else null
                if (code != null && msg != null) Pair(code, msg) else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Price polling — batching + backoff to avoid 429 rate-limit errors.
     */
    private fun startPricePolling() {
        // cancel previous
        priceJob?.cancel()
        currentBackoffMs = BASE_POLL_INTERVAL_MS

        priceJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    val csv = adapter.getSymbolsCsv()
                    Log.d("WatchlistFragment", "Symbols CSV: '$csv' -> ${csv.split(",").map { it.trim() }}")
                    if (csv.isBlank()) {
                        Log.d("WatchlistFragment", "No symbols to poll")
                    } else {
                        // chunk symbols to reduce credits per call
                        val batches = chunkSymbols(csv, BATCH_SIZE)
                        var anySuccess = false

                        for (batch in batches) {
                            if (!isActive) break

                            Log.d("WatchlistFragment", "Polling batch for: $batch (backoff=${currentBackoffMs}ms)")

                            val resp = try {
                                twelveApi.getQuotesRaw(batch, TWELVE_DATA_API_KEY)
                            } catch (e: Exception) {
                                Log.e("WatchlistFragment", "Network error while fetching quotes for $batch", e)
                                null
                            }

                            if (resp == null) {
                                // network error — increase backoff and stop this cycle
                                currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                Log.w("WatchlistFragment", "Null response for $batch — increasing backoff to $currentBackoffMs")
                                break
                            }

                            if (!resp.isSuccessful) {
                                val code = resp.code()
                                val errBody = try { resp.errorBody()?.string() } catch (e: Exception) { "error-reading-body:${e.message}" }
                                Log.w("WatchlistFragment", "Quotes request failed for $batch: code=$code body=$errBody")

                                if (code == 429) {
                                    // rate limited — try to read Retry-After header
                                    val retryAfterHeader = resp.headers()["Retry-After"]?.toLongOrNull()
                                    if (retryAfterHeader != null && retryAfterHeader > 0) {
                                        val waitMs = (retryAfterHeader * 1000L).coerceAtMost(MAX_BACKOFF_MS)
                                        currentBackoffMs = waitMs
                                        Log.w("WatchlistFragment", "Server requested Retry-After=$retryAfterHeader seconds -> backoff=${currentBackoffMs}ms")
                                    } else {
                                        currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                        Log.w("WatchlistFragment", "Rate limited (429) — increasing backoff to $currentBackoffMs")
                                    }

                                    view?.post {
                                        Toast.makeText(requireContext(), "Rate limited by API — slowing updates", Toast.LENGTH_SHORT).show()
                                    }

                                    break // don't continue other batches this cycle
                                } else {
                                    // other non-200 — back off moderately and stop cycle
                                    currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                    Log.w("WatchlistFragment", "Non-200 response ($code) — increasing backoff to $currentBackoffMs")
                                    break
                                }
                            } else {
                                // successful response, read raw body
                                val body: ResponseBody? = resp.body()
                                val raw = try {
                                    body?.string() ?: "null"
                                } catch (e: IOException) {
                                    Log.e("WatchlistFragment", "Failed reading response body for $batch", e)
                                    "null"
                                }

                                Log.d("WatchlistFragment", "Raw quotes response for $batch: $raw")

                                if (raw == "null" || raw.isBlank()) {
                                    Log.w("WatchlistFragment", "Empty response for $batch")
                                    currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                    break
                                }

                                val apiErr = parsePossibleApiError(raw)
                                if (apiErr != null) {
                                    val (errCode, errMsg) = apiErr
                                    Log.w("WatchlistFragment", "API error object for $batch: code=$errCode msg=$errMsg")
                                    if (errCode == 429) {
                                        currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                    } else {
                                        currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                    }
                                    break
                                }

                                // parse and extract prices
                                try {
                                    val jsonElem: JsonElement = gson.fromJson(raw, JsonElement::class.java)
                                    val mapToAdapter = mutableMapOf<String, Pair<Double, Double?>>()

                                    if (jsonElem.isJsonObject) {
                                        val obj = jsonElem.asJsonObject

                                        // If top-level looks like one symbol object, try parse directly
                                        if (obj.has("price") || obj.has("last") || obj.has("close")) {
                                            parseSingleQuoteJson(obj, null, mapToAdapter)
                                        } else {
                                            // treat as map-of-symbols: iterate keys and try parsing each value
                                            for (k in obj.keySet()) {
                                                val v = obj.get(k)
                                                if (v != null) {
                                                    if (v.isJsonObject) {
                                                        parseSingleQuoteJson(v.asJsonObject, k, mapToAdapter)
                                                    } else if (v.isJsonPrimitive) {
                                                        val p = v.asString.toDoubleOrNull()
                                                        if (p != null) mapToAdapter[k] = Pair(p, null)
                                                    } else if (v.isJsonArray) {
                                                        val arr = v.asJsonArray
                                                        if (arr.size() > 0 && arr[0].isJsonObject) {
                                                            parseSingleQuoteJson(arr[0].asJsonObject, k, mapToAdapter)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Log.w("WatchlistFragment", "Quotes JSON root is not an object: $jsonElem")
                                    }

                                    if (mapToAdapter.isNotEmpty()) {
                                        adapter.updatePrices(mapToAdapter)
                                        anySuccess = true
                                        // successful fetch -> reduce backoff gradually toward base
                                        currentBackoffMs = (currentBackoffMs / 2).coerceAtLeast(BASE_POLL_INTERVAL_MS)
                                        if (currentBackoffMs < BASE_POLL_INTERVAL_MS) currentBackoffMs = BASE_POLL_INTERVAL_MS
                                    } else {
                                        Log.w("WatchlistFragment", "No prices parsed for batch $batch")
                                        Log.d("WatchlistFragment", "Raw that failed to parse: $raw")
                                    }
                                } catch (e: Exception) {
                                    Log.e("WatchlistFragment", "Failed parsing json for $batch", e)
                                    currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                                    break
                                }
                            }

                            // small delay between batches to avoid burstiness
                            delay(250L)
                        } // end batches loop
                    }
                } catch (e: Exception) {
                    Log.e("WatchlistFragment", "price poll failed", e)
                    currentBackoffMs = (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                }

                // sleep before next cycle using currentBackoffMs
                Log.d("WatchlistFragment", "Sleeping for $currentBackoffMs ms before next poll cycle")
                delay(currentBackoffMs)
            }
        }
    }

    private fun stopPricePolling() {
        priceJob?.cancel()
        priceJob = null
    }

    /**
     * Extended parser: tries many possible fields and nested shapes.
     * If guessedSymbol is provided, it's used when the object doesn't include a symbol field.
     */
    private fun parseSingleQuoteJson(obj: JsonObject, guessedSymbol: String?, out: MutableMap<String, Pair<Double, Double?>>) {
        try {
            // Determine symbol: prefer explicit field, fall back to guessedSymbol (map key)
            val symbol = when {
                obj.has("symbol") -> obj.get("symbol").asString
                obj.has("symbol_name") -> obj.get("symbol_name").asString
                !guessedSymbol.isNullOrEmpty() -> guessedSymbol
                else -> null
            } ?: return

            // Helper to try many candidate fields for a numeric value
            fun findNumeric(vararg names: String): Double? {
                for (n in names) {
                    if (obj.has(n)) {
                        val e = obj.get(n)
                        if (e != null && !e.isJsonNull && e.isJsonPrimitive) {
                            val s = e.asString
                            val d = s.toDoubleOrNull()
                            if (d != null) return d
                        }
                    }
                }
                return null
            }

            // Try direct simple fields
            var price: Double? = findNumeric("price", "last", "close", "ask", "bid", "prev_close", "previous_close", "value")
            var prev: Double? = findNumeric("previous_close", "prev_close", "close_prev", "close", "previous")

            // If still null, check nested arrays e.g. {"values":[{"close":"..."}]}
            if (price == null) {
                val arrNames = listOf("values", "data", "items")
                for (arrName in arrNames) {
                    if (obj.has(arrName) && obj.get(arrName).isJsonArray) {
                        val arr = obj.getAsJsonArray(arrName)
                        if (arr.size() > 0 && arr[0].isJsonObject) {
                            val o0 = arr[0].asJsonObject
                            fun findIn(o: JsonObject, vararg names: String): Double? {
                                for (n in names) {
                                    if (o.has(n) && o.get(n).isJsonPrimitive) {
                                        val d = o.get(n).asString.toDoubleOrNull()
                                        if (d != null) return d
                                    }
                                }
                                return null
                            }
                            price = findIn(o0, "price", "last", "close", "ask", "bid", "value")
                            if (prev == null) prev = findIn(o0, "previous_close", "prev_close", "close")
                            if (price != null) break
                        }
                    }
                }
            }

            // Final fallback: nested objects like { "quote": { "price": "..." } }
            if (price == null) {
                val nestedNames = listOf("quote", "quotes", "result")
                for (n in nestedNames) {
                    if (obj.has(n) && obj.get(n).isJsonObject) {
                        val nested = obj.getAsJsonObject(n)
                        val cand = listOf("price", "last", "close", "ask", "bid", "value")
                        for (c in cand) {
                            if (nested.has(c) && nested.get(c).isJsonPrimitive) {
                                val d = nested.get(c).asString.toDoubleOrNull()
                                if (d != null) { price = d; break }
                            }
                        }
                        if (prev == null) {
                            val d2 = nested.entrySet().firstOrNull { e -> arrayOf("previous_close","prev_close","close").contains(e.key) && e.value.isJsonPrimitive }?.let { it.value.asString.toDoubleOrNull() }
                            if (d2 != null) prev = d2
                        }
                    }
                    if (price != null) break
                }
            }

            // compute pct change if possible
            val pct: Double? = if (price != null && prev != null && prev != 0.0) ((price - prev) / prev * 100.0) else null

            if (price != null) {
                out[symbol] = Pair(price, pct)
            } else {
                // nothing found — log important debug info
                Log.w("WatchlistFragment", "Could not find price for symbol='$symbol' in object keys=${obj.keySet()}")
                Log.d("WatchlistFragment", "Object for symbol='$symbol': ${gson.toJson(obj)}")
            }
        } catch (e: Exception) {
            Log.e("WatchlistFragment", "parseSingleQuoteJson extended parser error: ${e.message}", e)
        }
    }

    private fun openChartFor(symbol: String?) {
        val frag = ChartFragment().apply {
            arguments = Bundle().apply { putString("symbol", symbol) }
        }
        parentFragmentManager.beginTransaction()
            .replace(FRAGMENT_CONTAINER_ID, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun openStockSearch() {
        val frag = StockSearchFragment()
        parentFragmentManager.beginTransaction()
            .replace(FRAGMENT_CONTAINER_ID, frag)
            .addToBackStack(null)
            .commit()
    }
}
