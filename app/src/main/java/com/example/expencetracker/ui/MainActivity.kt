package com.example.expencetracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.ui.fragments.BudgetFragment
import com.example.expencetracker.ui.fragments.HomeFragment
import com.example.expencetracker.ui.fragments.SettingsFragment
import com.example.expencetracker.ui.fragments.TransactionFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences with PrefsManager
        PrefsManager.init(this)

        // Load the default fragment (for example, HomeFragment or TransactionFragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Set up BottomNavigationView selection listener
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_transactions -> {
                    loadFragment(TransactionFragment())
                    true
                }
                R.id.nav_budget -> {
                    loadFragment(BudgetFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Helper function to load a fragment
    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}