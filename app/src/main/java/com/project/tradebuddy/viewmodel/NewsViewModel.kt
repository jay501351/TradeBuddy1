package com.project.tradebuddy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.tradebuddy.NewsItem
import com.project.tradebuddy.RetrofitClient
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    private val _newsList = MutableLiveData<List<NewsItem>>()
    val newsList: LiveData<List<NewsItem>> get() = _newsList

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchNews(apiKey: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getFinancialNews(apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _newsList.postValue(response.body()!!.results)
                    _error.value = null
                }else{
                    _error.value ="Error: ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch news: ${e.message}"
            }
        }
    }
}