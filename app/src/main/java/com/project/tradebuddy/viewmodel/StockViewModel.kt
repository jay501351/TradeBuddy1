package com.project.tradebuddy.viewmodel

import androidx.lifecycle.*
import com.project.tradebuddy.network.RetrofitInstance
import com.project.tradebuddy.network.StockValue
import com.project.tradebuddy.util.Constants.API_KEY
import kotlinx.coroutines.launch

class StockViewModel : ViewModel() {
    private val _stockValues = MutableLiveData<List<StockValue>>()
    val stockValue: LiveData<List<StockValue>> get() = _stockValues

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchStockData(symbol: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getTimeSeries(
                    symbol = symbol,
                    interval = "1min",
                    apiKey = API_KEY
                )

                if (response.isSuccessful && response.body() != null) {
                    _stockValues.postValue(response.body()!!.values)
                    _error.postValue(null)
                } else {
                    _error.postValue("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("Error: ${e.message}")
            }
        }
    }
}
