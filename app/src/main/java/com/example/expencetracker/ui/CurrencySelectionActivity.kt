package com.example.expencetracker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.expencetracker.R

class CurrencySelectionActivity : AppCompatActivity() {

    // Sample list of currencies; you could expand this list.
    private val currencies = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF",
        "CNY", "INR", "LKR", "HKD", "SGD", "NZD", "KRW"
    )

    // Map of currency codes to symbols
    private val currencySymbols = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", 
        "AUD" to "A$", "CAD" to "C$", "CHF" to "Fr", 
        "CNY" to "¥", "INR" to "₹", "LKR" to "Rs", 
        "HKD" to "HK$", "SGD" to "S$", "NZD" to "NZ$", "KRW" to "₩"
    )

    private lateinit var adapter: CurrencyAdapter
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_selection)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set up currency list with custom adapter
        val lvCurrencies: ListView = findViewById(R.id.lvCurrencies)
        adapter = CurrencyAdapter()
        lvCurrencies.adapter = adapter

        // Set up search functionality
        searchEditText = findViewById(R.id.etSearch)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        lvCurrencies.setOnItemClickListener { _, _, position, _ ->
            val selectedCurrency = adapter.getItem(position) ?: ""
            val resultIntent = Intent()
            resultIntent.putExtra("selected_currency", selectedCurrency)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
    
    // Custom adapter for currency items
    inner class CurrencyAdapter : ArrayAdapter<String>(
        this@CurrencySelectionActivity,
        R.layout.item_currency_list,
        R.id.text1,
        currencies
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            
            val currencyCode = getItem(position) ?: ""
            val currencyIcon = view.findViewById<TextView>(R.id.tvCurrencyIcon)
            
            // Set currency symbol
            currencyIcon.text = currencySymbols[currencyCode] ?: currencyCode.first().toString()
            
            return view
        }
    }
}