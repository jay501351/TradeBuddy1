package com.project.tradebuddy

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WatchlistAdapter(stocks: LiveData<List<WatchlistItem>>) : RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder>() {

    private var items = listOf<WatchlistItem>()
    inner class WatchlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(R.id.stockLogo)
        val symbol: TextView = itemView.findViewById(R.id.stockSymbol)
        val company: TextView = itemView.findViewById(R.id.companyName)
        val price: TextView = itemView.findViewById(R.id.stockPrice)
        val change: TextView = itemView.findViewById(R.id.stockChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val view =  LayoutInflater.from(parent.context).inflate(R.layout.item_watchlist, parent, false)
        return WatchlistViewHolder(view)

    }

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
       val item = items[position]
        holder.symbol.text = item.symbol
        holder.company.text = item.companyName
        holder.price.text = item.price.toString()

        val changeText = "${item.change}% (${item.changePercent}%)"
        holder.change.text = changeText

        if (item.change >= 0){
            holder.change.setTextColor(Color.GREEN)
        }else{
            holder.change.setTextColor(Color.RED)
        }

        item.logoUrl?.let {
            Glide.with(holder.itemView).load(it).into(holder.logo)
        }
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<WatchlistItem>) {
        items = newItems
        notifyDataSetChanged()
    }

}