package com.project.tradebuddy.ui.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.project.tradebuddy.ChartFragment
import com.project.tradebuddy.R
import com.project.tradebuddy.Stock
import com.project.tradebuddy.WatchlistAdapter

class WatchlistFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        recyclerView = view.findViewById(R.id.watchlistRecyclerView)

        adapter = WatchlistAdapter { stock ->
            val fragment = ChartFragment().apply {
                arguments = Bundle().apply { putString("symbol", stock.symbol) }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val stocks = listOf(
            Stock("NASDAQ:AAPL", "Apple Inc.", 228.75, +2.10, +0.93),
            Stock("NASDAQ:TSLA", "Tesla Inc.", 260.43, -1.25, -0.48),
            Stock("NASDAQ:AMZN", "Amazon.com Inc.", 181.12, +0.95, +0.53),
            Stock("NASDAQ:MSFT", "Microsoft Corp.", 412.18, +3.35, +0.82),
            Stock("NASDAQ:GOOG", "Alphabet Inc. (Google)", 173.90, -0.70, -0.40),
            Stock("NASDAQ:META", "Meta Platforms Inc.", 509.25, +5.60, +1.11),
            Stock("NASDAQ:NVDA", "NVIDIA Corp.", 1080.35, +22.75, +2.15),
            Stock("NASDAQ:NFLX", "Netflix Inc.", 615.80, -4.10, -0.66),
            Stock("NASDAQ:AMD", "AMD Inc.", 173.75, +1.45, +0.84),
            Stock("NASDAQ:INTC", "Intel Corp.", 34.22, -0.22, -0.64),
            Stock("NYSE:JPM", "JPMorgan Chase & Co.", 200.12, +1.80, +0.91),
            Stock("NYSE:BAC", "Bank of America Corp.", 40.85, +0.30, +0.74),
            Stock("NYSE:XOM", "ExxonMobil Corp.", 120.45, -0.55, -0.45),
            Stock("NYSE:KO", "Coca-Cola Co.", 63.28, +0.40, +0.64),
            Stock("NYSE:MCD", "McDonald’s Corp.", 282.12, +2.12, +0.76)
        )
        adapter.setStocks(stocks)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_stock -> {
                    Toast.makeText(requireContext(), "Add Stock Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        return view
    }
}
