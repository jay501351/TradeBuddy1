package com.project.tradebuddy.ui.watchlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem
import java.text.DecimalFormat

/**
 * Adapter for the Watchlist RecyclerView.
 *
 * - Exposes helper methods removeAt/addAt/getItem/indexOfSymbol for swipe/undo and fragment operations.
 * - Keeps a local priceMap to allow partial updates via updatePrices().
 */
class WatchlistAdapter(
    private val onItemClick: (StockSearchItem) -> Unit,
    private val onItemLongClick: ((StockSearchItem) -> Unit)? = null
) : RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {

    internal val stocks = mutableListOf<StockSearchItem>() // internal so fragment can inspect if needed
    private val priceMap = mutableMapOf<String, Pair<Double, Double?>>() // symbol -> (price, percent)

    private val df = DecimalFormat("#,##0.00")

    /**
     * Replace the current list of stocks with [list].
     */
    fun setStocks(list: List<StockSearchItem>) {
        stocks.clear()
        stocks.addAll(list)
        // keep only prices for current symbols
        priceMap.keys.retainAll(stocks.map { it.symbol })
        notifyDataSetChanged()
    }

    /**
     * Safe getter for an item at [position].
     */
    fun getItem(position: Int): StockSearchItem? = stocks.getOrNull(position)

    /**
     * Insert [item] at [position] (or append if position out of range).
     */
    fun addAt(position: Int, item: StockSearchItem) {
        val pos = position.coerceIn(0, stocks.size)
        stocks.add(pos, item)
        notifyItemInserted(pos)
    }

    /**
     * Remove item at [position] and return it, or null if position invalid.
     * Also clears cached price for that symbol.
     */
    fun removeAt(position: Int): StockSearchItem? {
        if (position < 0 || position >= stocks.size) return null
        val removed = stocks.removeAt(position)
        notifyItemRemoved(position)
        removed.symbol?.let { priceMap.remove(it) }
        return removed
    }

    /**
     * Return first index of [symbol] (case-insensitive) or -1 if not found.
     */
    fun indexOfSymbol(symbol: String?): Int {
        if (symbol == null) return -1
        return stocks.indexOfFirst { it.symbol?.equals(symbol, ignoreCase = true) == true }
    }

    /**
     * Comma-separated symbols suitable for API requests.
     */
    fun getSymbolsCsv(): String {
        return stocks.mapNotNull { it.symbol?.trim() }.filter { it.isNotEmpty() }.joinToString(",")
    }

    /**
     * Update the internal price map with [symbolToPrice] and notify only changed item positions.
     * symbolToPrice: Map<symbol, Pair(price, percent?)>
     */
    fun updatePrices(symbolToPrice: Map<String, Pair<Double, Double?>>) {
        if (symbolToPrice.isEmpty()) return

        val positionsToNotify = mutableSetOf<Int>()

        for ((symRaw, pair) in symbolToPrice) {
            val sym = symRaw
            val old = priceMap[sym]
            // only update if price changed (or not present)
            val oldPrice = old?.first
            val newPrice = pair.first
            val oldPct = old?.second
            val newPct = pair.second

            val priceChanged = oldPrice == null || oldPrice != newPrice
            val pctChanged = (oldPct ?: Double.NaN) != (newPct ?: Double.NaN)

            if (priceChanged || pctChanged) {
                priceMap[sym] = pair
                // find all positions with this symbol (usually one)
                stocks.forEachIndexed { idx, s ->
                    if (s.symbol?.equals(sym, ignoreCase = true) == true) positionsToNotify.add(idx)
                }
            }
        }

        // notify changed positions (only visible ones will redraw)
        positionsToNotify.forEach { pos ->
            if (pos in 0 until itemCount) notifyItemChanged(pos)
        }
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
                // color green/red (try-catch for older APIs)
                try {
                    val color = if (percent >= 0)
                        itemView.context.getColor(android.R.color.holo_green_dark)
                    else
                        itemView.context.getColor(android.R.color.holo_red_dark)
                    changeTv?.setTextColor(color)
                } catch (e: Exception) {
                    // ignore color fallback
                }
            }
        }
    }
}
