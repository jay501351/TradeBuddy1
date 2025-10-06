package com.project.tradebuddy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.project.tradebuddy.ui.watchlist.WatchlistFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        // BottomNavigationView setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        loadFragment(WatchlistFragment())
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_watchList -> loadFragment(WatchlistFragment())
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
