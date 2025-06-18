package com.project.tradebuddy

import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class NewsAdapter(private var items: List<NewsItem>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = items[position]
        holder.title.text = news.title
        holder.date.text = news.pubDate
        holder.desc.text = news.description

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(news.image_url)
            .listener(object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("Glide Error","Load Failed",e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .centerCrop()
            .into(holder.image)
        Log.d("NewsAdapter", "Image URL: ${news.image_url}")

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,NewsWebViewActivity::class.java)
            intent.putExtra("url",news.link)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    inner class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtTitle)
        val date: TextView = view.findViewById(R.id.txtDate)
        val desc: TextView = view.findViewById(R.id.txtDesc)
        val image: ImageView = view.findViewById(R.id.newsImage)
    }

    fun updateData(newItems: List<NewsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
