package com.example.expencetracker.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R

class CurrencyAdapter(
    private val currencies: List<String> = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF",
        "CNY", "INR", "LKR", "HKD", "SGD", "NZD", "KRW"
    ),
    private val currencySymbols: Map<String, String> = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", 
        "AUD" to "A$", "CAD" to "C$", "CHF" to "Fr", 
        "CNY" to "¥", "INR" to "₹", "LKR" to "Rs", 
        "HKD" to "HK$", "SGD" to "S$", "NZD" to "NZ$", "KRW" to "₩"
    ),
    private val onCurrencySelected: (String) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    // Keep track of the currently selected currency position
    private var selectedPosition = 0 // Default to USD (first item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_currency, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currencyCode = currencies[position]
        val symbol = currencySymbols[currencyCode] ?: currencyCode.first().toString()

        holder.bind(currencyCode, symbol, position == selectedPosition)
        holder.itemView.setOnClickListener {
            // Update selected position
            val previousSelected = selectedPosition
            selectedPosition = position
            
            // Notify items changed for UI update
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            
            // Callback to activity
            onCurrencySelected(currencyCode)
        }
    }

    override fun getItemCount(): Int = currencies.size

    fun setSelectedCurrency(currencyCode: String) {
        val newPosition = currencies.indexOf(currencyCode)
        if (newPosition >= 0 && newPosition != selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(newPosition)
        }
    }

    inner class CurrencyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCurrencyIcon: TextView = itemView.findViewById(R.id.tvCurrencyIcon)
        private val tvCurrencyCode: TextView = itemView.findViewById(R.id.tvCurrencyCode)
        private val ivSelected: ImageView = itemView.findViewById(R.id.ivSelected)

        fun bind(currencyCode: String, symbol: String, isSelected: Boolean) {
            tvCurrencyIcon.text = symbol
            tvCurrencyCode.text = currencyCode
            ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // Make the card appear selected
            itemView.isSelected = isSelected
            itemView.alpha = if (isSelected) 1.0f else 0.7f
        }
    }
} 