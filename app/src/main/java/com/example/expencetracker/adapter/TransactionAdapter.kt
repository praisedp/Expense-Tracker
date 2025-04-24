package com.example.expencetracker.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.Category
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val categories: List<Category>,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val ivCategoryIcon: TextView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(transaction: Transaction) {
            // Title
            tvTitle.text = transaction.title

            // Amount formatting with currency symbol and two decimals
            val currencyCode = PrefsManager.getCurrency()
            val symbol = getCurrencySymbol(currencyCode)
            val absAmount = String.format(Locale.getDefault(), "%.2f", abs(transaction.amount))

// 3) Prepend a "−" only for expenses
            val displayText = if (transaction.type == TxType.EXPENSE) {
                "-$symbol$absAmount"
            } else {
                "$symbol$absAmount"
            }
            tvAmount.text = displayText

// 4) Color red for expenses, green for income
            val colorRes = if (transaction.type == TxType.EXPENSE) {
                R.color.colorExpense
            } else {
                R.color.colorIncome
            }
            tvAmount.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            // Date formatting
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvDate.text = sdf.format(Date(transaction.date))

            // Category text
            if (transaction.category.isNotBlank()) {
                tvCategory.visibility = View.VISIBLE
                tvCategory.text = transaction.category
            } else {
                tvCategory.visibility = View.GONE
            }

            val emoji = transaction.category.substringBefore(" ")

            // 2) Display it in the circular TextView
            ivCategoryIcon.text = emoji

            // 3) (Optional) Retint the background if you still want coloring:
            (ivCategoryIcon.background as? GradientDrawable)
                ?.setColor(getCategoryColor(transaction.category))



            // Long‑press to delete/edit
            itemView.setOnLongClickListener {
                onLongClick(transaction)
                true
            }
        }
    }

    /**
     * Replace the displayed list and refresh.
     */
    fun updateData(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    // Update the transactions list with new data - this is an alias for updateData
    fun updateTransactions(newTransactions: List<Transaction>) {
        updateData(newTransactions)
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

    /** Returns a background color for the category icon circle. */
    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase(Locale.getDefault())) {
            "food" -> Color.parseColor("#FFC107")       // Amber
            "transport" -> Color.parseColor("#2196F3")  // Blue
            "shopping" -> Color.parseColor("#9C27B0")   // Purple
            else -> Color.parseColor("#E3F2FD")         // Light Blue default
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

    /** Extension to format a double to [digits] decimal places. */
    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}