package com.project.tradebuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Watchlist : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        recyclerView = view.findViewById(R.id.recyclerStockList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val stocks = listOf(
            StockItem(R.color.black, "AAPL", "Apple Inc.", "$120", "(+1.5%)", true),
            StockItem(R.color.black, "GOOGL", "Alphabet Inc.", "$2,560", "(+0.9%)", false),
            StockItem(R.color.black, "AMZN", "Amazon.com Inc.", "$3,270", "(-0.3%)", true)
        )

        adapter = StockAdapter(stocks)
        recyclerView.adapter = adapter

        return view
    }
}
