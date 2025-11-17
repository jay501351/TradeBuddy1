package com.project.tradebuddy

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WatchlistManager {
    private const val PREF_NAME = "watchlist_prefs"
    private const val KEY_WATCHLIST = "watchlist"

    private val gson = Gson()

    fun addStock(context: Context, stock: StockSearchItem) {
        val watchlist = getWatchlist(context).toMutableList()
        if (watchlist.none { it.symbol == stock.symbol }) {  // avoid duplicates
            watchlist.add(stock)
            saveWatchlist(context, watchlist)
        }
    }

    fun removeStock(context: Context, symbol: String) {
        val watchlist = getWatchlist(context).toMutableList()
        watchlist.removeAll { it.symbol == symbol }
        saveWatchlist(context, watchlist)
    }

    fun getWatchlist(context: Context): List<StockSearchItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WATCHLIST, null)
        val type = object : TypeToken<List<StockSearchItem>>() {}.type
        return if (json != null) gson.fromJson(json, type) else emptyList()
    }

    private fun saveWatchlist(context: Context, watchlist: List<StockSearchItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WATCHLIST, gson.toJson(watchlist)).apply()
    }

}