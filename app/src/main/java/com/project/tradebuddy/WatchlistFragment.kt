package com.project.tradebuddy.ui.watchlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.project.tradebuddy.ChartFragment
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem
import com.project.tradebuddy.WatchlistManager
import com.project.tradebuddy.WatchlistPickerAdapter
import com.project.tradebuddy.ui.search.StockSearchFragment

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        // RecyclerView + adapter — long-press removal included if your adapter supports it
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

        // NEW: open custom picker dialog
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
     * NEW: custom RecyclerView dialog for picking/deleting watchlists
     */
    private fun showWatchlistPickerDialogCustom() {
        loadAvailableWatchlists()

        val dlgView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_watchlist_picker, null)
        val rv = dlgView.findViewById<RecyclerView>(R.id.recyclerDialogLists)
        rv.layoutManager = LinearLayoutManager(requireContext())

        val initialSelected = currentListIndex.coerceIn(0, watchlistNames.size - 1)

        // make adapterDialog nullable so lambdas may reference it safely
        var adapterDialog: WatchlistPickerAdapter? = null

        adapterDialog = WatchlistPickerAdapter(
            items = watchlistNames,
            selectedIndex = initialSelected,
            onItemClick = { index, name ->
                // on row click: switch immediately
                currentListIndex = index
                WatchlistManager.setCurrentWatchlistName(requireContext(), name)
                tvCurrentList?.text = name
                loadWatchlistByName(name)
            },
            onDeleteClick = { index, name ->
                // ask confirm, then delete
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete watchlist")
                    .setMessage("Delete \"$name\"? This will remove all stocks in that list.")
                    .setPositiveButton("Delete") { delDialog, _ ->
                        try {
                            // perform deletion
                            WatchlistManager.deleteWatchlist(requireContext(), name)
                            Toast.makeText(requireContext(), "\"$name\" deleted", Toast.LENGTH_SHORT).show()
                            // refresh lists and update dialog adapter
                            loadAvailableWatchlists()
                            adapterDialog?.updateItems(watchlistNames)
                            // adjust selection & displayed list
                            val newCurrent = WatchlistManager.getCurrentWatchlistName(requireContext())
                            if (!newCurrent.isNullOrEmpty()) {
                                tvCurrentList?.text = newCurrent
                                loadWatchlistByName(newCurrent)
                            } else {
                                // fallback: set first or Default
                                val first = watchlistNames.firstOrNull() ?: "Default"
                                WatchlistManager.setCurrentWatchlistName(requireContext(), first)
                                tvCurrentList?.text = first
                                loadWatchlistByName(first)
                            }
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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .setNegativeButton("Close", null)
            .create()

        dialog.show()
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
        // instantiate StockSearchFragment directly (you already have it)
        val frag = StockSearchFragment()
        parentFragmentManager.beginTransaction()
            .replace(FRAGMENT_CONTAINER_ID, frag)
            .addToBackStack(null)
            .commit()
    }
}
