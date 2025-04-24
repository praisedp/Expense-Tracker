package com.example.expencetracker.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.ui.fragments.BudgetFragment
import com.example.expencetracker.ui.fragments.HomeFragment
import com.example.expencetracker.ui.fragments.SettingsFragment
import com.example.expencetracker.ui.fragments.TransactionFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val pinLockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // PIN verification failed, exit the app
            finishAffinity()
        }
    }
    
    // Key for storing the selected navigation item
    private val KEY_SELECTED_NAV_ITEM = "selected_nav_item"
    private var selectedNavItemId = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark mode preference before super.onCreate()
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode_enabled", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences with PrefsManager
        PrefsManager.init(this)
        
        // Restore selected navigation item from saved instance state or recreations
        if (savedInstanceState != null) {
            selectedNavItemId = savedInstanceState.getInt(KEY_SELECTED_NAV_ITEM, R.id.nav_home)
        }

        // Set up BottomNavigationView selection listener
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            selectedNavItemId = item.itemId
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
        
        // Initialize with the correct fragment based on saved or current state
        val initialFragment = when (selectedNavItemId) {
            R.id.nav_transactions -> TransactionFragment()
            R.id.nav_budget -> BudgetFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> HomeFragment()
        }
        
        loadFragment(initialFragment)
        bottomNavigation.selectedItemId = selectedNavItemId
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current navigation selection
        outState.putInt(KEY_SELECTED_NAV_ITEM, selectedNavItemId)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if PIN lock is enabled and the session is not unlocked
        if (PrefsManager.isPinEnabled() && !PrefsManager.isSessionUnlocked()) {
            // Launch PIN entry screen
            val intent = Intent(this, LockActivity::class.java)
            pinLockLauncher.launch(intent)
        }
    }

    // Helper function to load a fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}