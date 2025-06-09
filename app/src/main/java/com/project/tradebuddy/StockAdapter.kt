package com.project.tradebuddy

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StockAdapter(private val stockList: List<StockItem>) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: StockViewHolder,
        position: Int
    ) {
        val item = stockList[position]
        holder.imgLogo.setImageResource(item.logoResId)
        holder.txtSymbol.text = item.symbol
        holder.txtCompany.text = item.company
        holder.txtPrice.text = item.price
        holder.txtChange.text = item.change
        holder.txtChange.setTextColor(
            if (item.isPositive) Color.parseColor("#008000")else Color.parseColor("#FF0000"))
    }

    override fun getItemCount(): Int = stockList.size

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val imgLogo = itemView.findViewById<ImageView>(R.id.imgStockLogo)
        val txtSymbol = itemView.findViewById<TextView>(R.id.txtStockSymbol)
        val txtCompany = itemView.findViewById<TextView>(R.id.txtCompanyName)
        val txtPrice = itemView.findViewById<TextView>(R.id.txtStockPrice)
        val txtChange = itemView.findViewById<TextView>(R.id.txtStockChange)
    }
}