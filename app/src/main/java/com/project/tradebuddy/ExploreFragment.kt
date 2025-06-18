package com.project.tradebuddy

import com.project.tradebuddy.viewmodel.NewsViewModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ExploreFragment : Fragment() {

    private lateinit var viewModel : NewsViewModel
    private lateinit var adapter : NewsAdapter
    private  var API_KEY = "pub_0c9007fe32d44198a342235e9decb19b"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val retryLayout = view.findViewById<View>(R.id.retryLayout)
        val txtError = view.findViewById<TextView>(R.id.txtError)
        val btnRetry = view.findViewById<View>(R.id.btnRetry)
        val recyclerView = view.findViewById<RecyclerView>(R.id.newsRecycler)

            adapter = NewsAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel = ViewModelProvider(this).get(NewsViewModel::class.java)
        viewModel.newsList.observe(viewLifecycleOwner) {
            adapter.updateData(it)
            recyclerView.visibility = View.VISIBLE
            retryLayout.visibility = View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if(errorMsg != null){
                recyclerView.visibility = View.GONE
                retryLayout.visibility = View.VISIBLE
                txtError.text = errorMsg
            }
        }
        viewModel.fetchNews(API_KEY)
    }
}