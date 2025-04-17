package com.example.expencetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)

        fun bind(transaction: Transaction) {
            tvTitle.text = transaction.title

            // Get the current currency from PrefsManager and map it to a symbol.
            val currentCurrency = PrefsManager.getCurrency()
            val currencySymbol = getCurrencySymbol(currentCurrency)

            // Format the amount to two decimals.
            val formattedAmount = String.format("%.2f", transaction.amount)
            tvAmount.text = "$currencySymbol$formattedAmount"

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            tvDate.text = sdf.format(Date(transaction.date))

            if (transaction.category.isNotBlank()) {
                tvCategory.visibility = View.VISIBLE
                tvCategory.text = transaction.category
            } else {
                tvCategory.visibility = View.GONE
            }

            itemView.setOnLongClickListener {
                onLongClick(transaction)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    // Helper function: maps currency code to a symbol.
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
            "LKR" -> "Rs" // You may choose a different representation for LKR if you prefer.
            "HKD" -> "HK$"
            "SGD" -> "S$"
            "NZD" -> "NZ$"
            "KRW" -> "₩"
            else -> code
        }
    }
}