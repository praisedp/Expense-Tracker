package com.example.expencetracker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.expencetracker.R

class CurrencySelectionActivity : AppCompatActivity() {

    // Sample list of currencies; you could expand this list.
    private val currencies = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF",
        "CNY", "INR", "LKR", "HKD", "SGD", "NZD", "KRW"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_selection)

        val lvCurrencies: ListView = findViewById(R.id.lvCurrencies)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, currencies)
        lvCurrencies.adapter = adapter

        lvCurrencies.setOnItemClickListener { _, _, position, _ ->
            val selectedCurrency = currencies[position]
            val resultIntent = Intent()
            resultIntent.putExtra("selected_currency", selectedCurrency)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}