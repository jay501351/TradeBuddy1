package com.project.tradebuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NewsAdapter(private var items: List<NewsItem>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NewsViewHolder,
        position: Int
    ) {
        val news = items[position]
        holder.title.text = news.title
        holder.date.text = news.pubDate
        holder.desc.text = news.description
    }

    override fun getItemCount() = items.size

    inner class NewsViewHolder(view: View):RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.txtTitle)
        val date: TextView = view.findViewById(R.id.txtDate)
        val desc: TextView = view.findViewById(R.id.txtDesc)
    }

    fun updateData(newItems: List<NewsItem>){
        items = newItems
        notifyDataSetChanged()
    }
}