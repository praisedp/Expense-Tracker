package com.example.expencetracker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}