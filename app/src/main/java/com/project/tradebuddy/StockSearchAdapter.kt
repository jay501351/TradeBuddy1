package com.project.tradebuddy.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R
import com.project.tradebuddy.StockSearchItem

class StockSearchAdapter(
    private val onItemClick: (StockSearchItem) -> Unit
) : ListAdapter<StockSearchItem, StockSearchAdapter.StockViewHolder>(DiffCallback()) {

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSymbol: TextView = itemView.findViewById(R.id.tvSymbol)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvExchange: TextView = itemView.findViewById(R.id.tvExchange)

        fun bind(stock: StockSearchItem) {
            tvSymbol.text = stock.symbol
            tvName.text = stock.instrument_name
            tvExchange.text = stock.exchange
            itemView.setOnClickListener { onItemClick(stock) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<StockSearchItem>() {
        override fun areItemsTheSame(oldItem: StockSearchItem, newItem: StockSearchItem) =
            oldItem.symbol == newItem.symbol

        override fun areContentsTheSame(oldItem: StockSearchItem, newItem: StockSearchItem) =
            oldItem == newItem
    }
}
