import androidx.lifecycle.*
import com.project.tradebuddy.NewsItem
import com.project.tradebuddy.RetrofitClient
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    private val _newsList = MutableLiveData<List<NewsItem>>()
    val newsList: LiveData<List<NewsItem>> = _newsList

    fun fetchNews(apiKey: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getFinancialNews(apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _newsList.postValue(response.body()!!.results)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
