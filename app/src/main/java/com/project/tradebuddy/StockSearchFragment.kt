package com.project.tradebuddy.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem
import com.project.tradebuddy.WatchlistManager
import com.project.tradebuddy.api.TwelveDataService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockSearchFragment : Fragment() {

    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockSearchAdapter
    private lateinit var api: TwelveDataService
    private var searchJob: Job? = null

    private val apiKey = "YOUR_TWELVE_DATA_API_KEY"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stock_search, container, false)

        searchInput = view.findViewById(R.id.etSearch)
        recyclerView = view.findViewById(R.id.recyclerSearch)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = StockSearchAdapter { selectedStock ->
            WatchlistManager.addStock(requireContext(), selectedStock)
            parentFragmentManager.popBackStack() // Go back to watchlist
        }
        recyclerView.adapter = adapter

        setupRetrofit()
        setupSearchListener()

        return view
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.twelvedata.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TwelveDataService::class.java)
    }

    private fun setupSearchListener() {
        searchInput.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    fetchSearchResults(query)
                }
            } else {
                adapter.submitList(emptyList())
            }
        }
    }

    private suspend fun fetchSearchResults(query: String) {
        try {
            val response = api.searchStocks(query, apiKey)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data != null) {
                    adapter.submitList(body.data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
