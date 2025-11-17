package com.project.tradebuddy

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WatchlistManager {
    private const val PREF_NAME = "watchlist_prefs"
    private const val KEY_WATCHLISTS = "watchlists_map"      // Map<String, List<StockSearchItem>>
    private const val KEY_CURRENT = "watchlist_current_name"

    private val gson = Gson()

    // --- Public API ---

    // Get all watchlist names (ordered by insertion)
    fun getAllWatchlistNames(context: Context): List<String> {
        val map = loadMap(context)
        return map.keys.toList()
    }

    // Create a new watchlist (returns true if created, false if name exists)
    fun createWatchlist(context: Context, name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        val map = loadMap(context).toMutableMap()
        if (map.containsKey(normalized)) return false
        map[normalized] = emptyList()
        saveMap(context, map)
        // If there was no current watchlist, set this as current
        val current = getCurrentWatchlistName(context)
        if (current == null) setCurrentWatchlistName(context, normalized)
        return true
    }

    // Get the list by name (or empty list if not found)
    fun getWatchlistByName(context: Context, name: String): List<StockSearchItem> {
        val map = loadMap(context)
        return map[name] ?: emptyList()
    }

    // Add a stock to a named list (avoids duplicates). Returns true if added.
    fun addStockToList(context: Context, listName: String, stock: StockSearchItem): Boolean {
        val map = loadMap(context).toMutableMap()
        val list = map[listName]?.toMutableList() ?: mutableListOf()
        if (list.any { it.symbol == stock.symbol }) return false
        list.add(stock)
        map[listName] = list
        saveMap(context, map)
        return true
    }

    // Remove a stock from a named list
    fun removeStockFromList(context: Context, listName: String, symbol: String) {
        val map = loadMap(context).toMutableMap()
        val list = map[listName]?.toMutableList() ?: return
        list.removeAll { it.symbol == symbol }
        map[listName] = list
        saveMap(context, map)
    }

    // Get or create a default watchlist name, and return its list
    fun getWatchlist(context: Context): List<StockSearchItem> {
        val cur = getCurrentWatchlistName(context) ?: run {
            // create default if none
            createWatchlist(context, "Default")
            setCurrentWatchlistName(context, "Default")
            "Default"
        }
        return getWatchlistByName(context, cur)
    }

    // Set/get current watchlist name
    fun setCurrentWatchlistName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT, name).apply()
    }

    fun getCurrentWatchlistName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT, null)
    }

    // Delete an entire watchlist (if you want)
    fun deleteWatchlist(context: Context, name: String) {
        val map = loadMap(context).toMutableMap()
        if (!map.containsKey(name)) return
        map.remove(name)
        saveMap(context, map)
        val current = getCurrentWatchlistName(context)
        if (current == name) {
            // pick another or null
            val newCurrent = map.keys.firstOrNull()
            if (newCurrent != null) setCurrentWatchlistName(context, newCurrent) else {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_CURRENT).apply()
            }
        }
    }

    // --- Internal persistence helpers ---

    private fun loadMap(context: Context): Map<String, List<StockSearchItem>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WATCHLISTS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, List<StockSearchItem>>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveMap(context: Context, map: Map<String, List<StockSearchItem>>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_WATCHLISTS, json).apply()
    }
}
