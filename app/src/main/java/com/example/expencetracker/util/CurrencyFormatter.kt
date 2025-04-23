package com.example.expencetracker.util

import com.example.expencetracker.data.PrefsManager
import java.text.NumberFormat
import java.util.Currency

/**
 * Always formats with the user-selected currency code.
 * Call format(x) for a String, or numberFormat() to reuse the instance.
 */
object CurrencyFormatter {
    fun format(amount: Double): String = numberFormat().format(amount)

    fun numberFormat(): NumberFormat = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(PrefsManager.getCurrency())
    }
}