package com.project.tradebuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WatchlistAdapter(private val onStockClick: (Stock) -> Unit): RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {
    private val stockList = mutableListOf<Stock>()

    fun setStocks(stocks: List<Stock>) {
        stockList.clear()
        stockList.addAll(stocks)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_watchlist,parent,false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: StockViewHolder,
        position: Int
    ) {
        val stock = stockList[position]
        holder.bind(stock)
        holder.itemView.setOnClickListener {
            onStockClick(stock)
        }
    }

    override fun getItemCount(): Int  = stockList.size

    class StockViewHolder(itemView : View): RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.stockName)
        private val symbol: TextView = itemView.findViewById(R.id.stockSymbol)
        private val price: TextView = itemView.findViewById(R.id.stockPrice)
        private val change: TextView = itemView.findViewById(R.id.stockChange)

        fun bind(stock: Stock) {
            name.text = stock.name
            symbol.text = stock.symbol
            price.text = "₹${stock.price}"

            val changeStr = String.format("%.2f (%.2f%%)", stock.change, stock.changePercent)
            change.text = if (stock.change >= 0) "+$changeStr" else changeStr

            // Set color dynamically
            val color = if (stock.change >= 0)
                android.graphics.Color.parseColor("#2E7D32") // green
            else
                android.graphics.Color.parseColor("#C62828") // red
            change.setTextColor(color)
        }
    }
}