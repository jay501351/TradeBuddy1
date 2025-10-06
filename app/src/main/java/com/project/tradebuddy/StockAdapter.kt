package com.project.tradebuddy.ui.watchlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R
import com.project.tradebuddy.Stock

class WatchlistAdapter(
    private val onItemClick: (Stock) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {

    private val stocks = mutableListOf<Stock>()

    fun setStocks(list: List<Stock>) {
        stocks.clear()
        stocks.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_watchlist, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stocks[position]
        holder.bind(stock)
        holder.itemView.setOnClickListener { onItemClick(stock) }
    }

    override fun getItemCount(): Int = stocks.size

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.stockName)
        private val symbol: TextView = itemView.findViewById(R.id.stockSymbol)
        private val price: TextView = itemView.findViewById(R.id.stockPrice)
        private val change: TextView = itemView.findViewById(R.id.stockChange)

        fun bind(stock: Stock) {
            name.text = stock.name
            symbol.text = stock.symbol
            price.text = "₹${stock.price}"
            val changeText = "${if (stock.change >= 0) "+" else ""}${stock.change} (${stock.changePercent}%)"
            change.text = changeText
            change.setTextColor(if (stock.change >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
        }
    }
}
