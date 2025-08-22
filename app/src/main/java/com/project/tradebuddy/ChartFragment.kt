package com.project.tradebuddy

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment

class ChartFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var spinner: Spinner

    // Use free symbols like AAPL, TSLA, AMZN (NASDAQ)
    private val stockSymbols = mapOf(
        "Apple (AAPL)" to "NASDAQ:AAPL",
        "Tesla (TSLA)" to "NASDAQ:TSLA",
        "Amazon (AMZN)" to "NASDAQ:AMZN",
        "Microsoft (MSFT)" to "NASDAQ:MSFT",
        "Google (GOOG)" to "NASDAQ:GOOG"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        // Init views
        webView = view.findViewById(R.id.chartWebView)
        spinner = view.findViewById(R.id.stockSpinner)

        setupWebView()
        setupSpinner()

        // Load default stock
        loadTradingViewChart(stockSymbols.values.first())

        return view
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            stockSymbols.keys.toList()
        )
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedSymbol = stockSymbols.values.toList()[position]
                loadTradingViewChart(selectedSymbol)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTradingViewChart(symbol: String) {
        val html = """
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
              </head>
              <body style="margin:0;padding:0;">
                <div id="tradingview_widget"></div>
                <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
                <script type="text/javascript">
                  new TradingView.widget({
                    "width": "100%",
                    "height": "100%",
                    "symbol": "$symbol",
                    "interval": "5",
                    "timezone": "Etc/UTC",
                    "theme": "light",
                    "style": "1",
                    "locale": "en",
                    "toolbar_bg": "#f1f3f6",
                    "enable_publishing": false,
                    "allow_symbol_change": false,
                    "hide_side_toolbar": false,
                    "container_id": "tradingview_widget"
                  });
                </script>
              </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
