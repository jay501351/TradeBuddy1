package com.project.tradebuddy.ui.watchlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem
import java.text.DecimalFormat

class WatchlistAdapter(
    private val onItemClick: (StockSearchItem) -> Unit,
    private val onItemLongClick: ((StockSearchItem) -> Unit)? = null
) : RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {

    internal val stocks = mutableListOf<StockSearchItem>() // internal to allow fragment to read if needed
    private val priceMap = mutableMapOf<String, Pair<Double, Double?>>() // symbol -> (price, percent)

    private val df = DecimalFormat("#,##0.00")

    fun setStocks(list: List<StockSearchItem>) {
        stocks.clear()
        stocks.addAll(list)
        // keep only prices for current symbols
        priceMap.keys.retainAll(stocks.map { it.symbol })
        notifyDataSetChanged()
    }

    /**
     * Update prices map and refresh the visible list.
     * symbolToPrice: Map<symbol, Pair(price, percent?)>
     */
    fun updatePrices(symbolToPrice: Map<String, Pair<Double, Double?>>) {
        for ((sym, pair) in symbolToPrice) {
            priceMap[sym] = pair
        }
        // Simple approach: refresh all. For better perf, update visible positions only.
        notifyDataSetChanged()
    }

    fun getSymbolsCsv(): String {
        return stocks.mapNotNull { it.symbol?.trim() }.filter { it.isNotEmpty() }.joinToString(",")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stocks[position]
        holder.bind(stock)

        val sym = stock.symbol
        val pair = priceMap[sym]
        holder.setPriceAndChange(pair?.first, pair?.second)

        holder.itemView.setOnClickListener { onItemClick(stock) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(stock)
            true
        }
    }

    override fun getItemCount(): Int = stocks.size

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.stockName)
        private val symbolTv: TextView = itemView.findViewById(R.id.stockSymbol)
        private val priceTv: TextView? = itemView.findViewById(R.id.tvPrice)
        private val changeTv: TextView? = itemView.findViewById(R.id.tvChange)
        private val df = DecimalFormat("#,##0.00")

        fun bind(stock: StockSearchItem) {
            nameTv.text = stock.instrument_name ?: ""
            symbolTv.text = stock.symbol ?: ""
        }

        fun setPriceAndChange(price: Double?, percent: Double?) {
            if (price == null) {
                priceTv?.text = "--"
                changeTv?.text = ""
                return
            }
            priceTv?.text = df.format(price)

            if (percent == null) {
                changeTv?.text = ""
            } else {
                val sign = if (percent >= 0) "+" else ""
                changeTv?.text = String.format("%s%.2f%%", sign, percent)
                // color green/red
                try {
                    val color = if (percent >= 0)
                        itemView.context.getColor(android.R.color.holo_green_dark)
                    else
                        itemView.context.getColor(android.R.color.holo_red_dark)
                    changeTv?.setTextColor(color)
                } catch (e: Exception) {
                    // fallback: ignore
                }
            }
        }
    }
}
