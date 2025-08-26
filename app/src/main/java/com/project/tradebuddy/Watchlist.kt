package com.project.tradebuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.launch


class Watchlist : Fragment() {
    private lateinit var dao: WatchlistDao
    private lateinit var adapter: WatchlistAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.watchlistRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "watchlist_db"
        ).build()

        dao = db.watchlistDao()

        lifecycleScope.launch {
            val stocks = dao.getAll()
            adapter = WatchlistAdapter(stocks)
            recyclerView.adapter = adapter
        }
        return view
    }
}
