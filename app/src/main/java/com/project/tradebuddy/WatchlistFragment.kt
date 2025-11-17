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
import com.project.tradebuddy.ui.search.StockSearchFragment

class WatchlistFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WatchlistAdapter

    private var imgMenu: ImageView? = null
    private var tvCurrentList: TextView? = null
    private var btnAddList: MaterialButton? = null
    private var toolbar: MaterialToolbar? = null

    private var watchlistNames: List<String> = emptyList()
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

        // RecyclerView + adapter
        recyclerView = view.findViewById(RECYCLER_ID)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = WatchlistAdapter { stockItem ->
            openChartFor(stockItem.symbol)
        }
        recyclerView.adapter = adapter

        // header views
        imgMenu = view.findViewById(IMG_MENU_ID)
        tvCurrentList = view.findViewById(TV_LIST_ID)
        btnAddList = view.findViewById(BTN_ADD_LIST_ID)
        toolbar = view.findViewById(TOOLBAR_ID)

        // toolbar menu click (ensure your res/menu/watchlist_menu has a search action id if you want this)
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
            if (watchlistNames.size <= 1) {
                Toast.makeText(requireContext(), "No multiple watchlists available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentListIndex = (currentListIndex + 1) % watchlistNames.size
            val newName = watchlistNames[currentListIndex]
            tvCurrentList?.text = newName
            loadWatchlistByName(newName)
            Toast.makeText(requireContext(), "Switched to $newName", Toast.LENGTH_SHORT).show()
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
        watchlistNames = WatchlistManager.getAllWatchlistNames(requireContext())
        if (watchlistNames.isEmpty()) {
            // ensure at least "Default" exists
            WatchlistManager.createWatchlist(requireContext(), "Default")
            watchlistNames = WatchlistManager.getAllWatchlistNames(requireContext())
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
