package com.project.tradebuddy.ui.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.ChartFragment
import com.project.tradebuddy.R
import com.project.tradebuddy.WatchlistManager
import com.project.tradebuddy.StockSearchItem

class WatchlistFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)
        recyclerView = view.findViewById(R.id.recyclerWatchlist)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = WatchlistAdapter {stockItem ->
            openChartFor(stockItem.symbol)
        }
        recyclerView.adapter = adapter

        loadWatchlist()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadWatchlist()
    }

    private fun loadWatchlist() {
        val watchlist = WatchlistManager.getWatchlist(requireContext())
        adapter.setStocks(watchlist)
    }

    private fun openChartFor(symbol: String?) {
        val frag = ChartFragment().apply {
            arguments = Bundle().apply { putString("symbol", symbol) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, frag)
            .addToBackStack(null)
            .commit()
    }
}
