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
    
    /**
     * Format with currency symbol but simplified for chart labels
     * (no grouping separators, fewer decimal places)
     */
    fun formatSimple(amount: Double): String {
        val symbol = getCurrencySymbol(PrefsManager.getCurrency())
        return if (amount >= 1000) {
            // Format as K for thousands
            "${symbol}${String.format("%.1f", amount / 1000)}K"
        } else {
            // Format with one decimal place
            "${symbol}${String.format("%.1f", amount)}"
        }
    }
    
    /** Maps currency codes to a symbol. */
    private fun getCurrencySymbol(code: String): String {
        return when (code) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "CHF" -> "CHF"
            "CNY" -> "¥"
            "INR" -> "₹"
            "LKR" -> "Rs"
            "HKD" -> "HK$"
            "SGD" -> "S$"
            "NZD" -> "NZ$"
            "KRW" -> "₩"
            else -> code
        }
    }
}