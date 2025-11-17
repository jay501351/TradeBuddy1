package com.project.tradebuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.tradebuddy.R

class WatchlistPickerAdapter(
    private var items: List<String>,
    private val selectedIndex: Int = 0,
    private val onItemClick: (index: Int, name: String) -> Unit,
    private val onDeleteClick: (index: Int, name: String) -> Unit
) : RecyclerView.Adapter<WatchlistPickerAdapter.VH>() {

    private var currentSelected = selectedIndex

    fun updateItems(newItems: List<String>) {
        items = newItems
        if (currentSelected >= items.size) currentSelected = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist_picker, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        holder.tvName.text = name

        // Highlight selected
        if (position == currentSelected) {
            holder.tvName.alpha = 1.0f
            holder.tvName.setTypeface(holder.tvName.typeface, android.graphics.Typeface.BOLD)
        } else {
            holder.tvName.alpha = 0.9f
            holder.tvName.setTypeface(holder.tvName.typeface, android.graphics.Typeface.NORMAL)
        }

        // Safe click: re-check position using adapterPosition
        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                currentSelected = pos
                notifyDataSetChanged()
                onItemClick(pos, items[pos])
            }
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClick(pos, items[pos])
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvListNameRow)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteListRow)
    }
}
