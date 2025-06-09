package com.project.tradebuddy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        // BottomNavigationView setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        loadFragment(Watchlist())
        bottomNav.setOnItemSelectedListener {

            Log.d("Hello","Hello")
            
            when (it.itemId) {
                R.id.nav_watchList -> loadFragment(Watchlist())
                R.id.nav_chart -> loadFragment(ChartFragment())
                R.id.nav_explore -> loadFragment(ExploreFragment())
                R.id.nav_menu -> loadFragment(MenuFragment())
                else -> false
            }
        }
    }

    private fun loadFragment(fragment:androidx.fragment.app.Fragment):Boolean{
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container,fragment)
            .commit()
        return true
    }
}
