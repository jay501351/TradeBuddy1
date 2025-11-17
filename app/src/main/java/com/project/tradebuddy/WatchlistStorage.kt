package com.project.tradebuddy

import android.content.Context
import com.google.gson.Gson
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken


class WatchlistStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWatchlist(name:String, stocks:List<Stock>){
        val json = gson.toJson(stocks)
        prefs.edit { putString(name, json) }
    }

    fun getWatchlist(name:String): MutableList<Stock>{
        val json = prefs.getString(name,null)
        return if (json!=null){
            val type = object : TypeToken<MutableList<Stock>>() {}.type
            gson.fromJson(json,type)
        }else mutableListOf()
    }

    fun getAllList(): MutableSet<String>{
        return prefs.all.keys.toMutableSet()
    }

    fun deleteWatchlist(name:String){
        prefs.edit { remove(name) }
    }

    fun saveLastSelectedList(name: String){
        prefs.edit { putString("last_selected_list", name) }
    }

    fun getLastSelectedList():String?{
        return prefs.getString("last_selected_list",null)
    }
}