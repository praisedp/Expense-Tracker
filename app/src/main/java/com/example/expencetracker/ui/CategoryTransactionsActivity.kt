// app/java/com/example/expencetracker/ui/CategoryTransactionsActivity.kt
package com.example.expencetracker.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import com.example.expencetracker.util.CurrencyFormatter
import com.example.expencetracker.util.TransactionFilter

class CategoryTransactionsActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCategoryTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_transactions)

        // Setup back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Show category name
        val tvCatTitle = findViewById<TextView>(R.id.tvCatTitle)
        tvCatTitle.text = intent.getStringExtra("label") ?: ""

        rv = findViewById(R.id.rvTransByCat)
        rv.layoutManager = LinearLayoutManager(this)
        tvEmpty = findViewById(R.id.tvCatEmpty)
        tvCategoryTotal = findViewById(R.id.tvCategoryTotal)

        // Load & filter transactions
        val label = intent.getStringExtra("label") ?: return
        val allTx = PrefsManager.loadTransactions()
        val list = when (intent.getStringExtra("filter_mode")) {
            "MONTH" -> {
                val from = intent.getLongExtra("from", 0L)
                val to   = intent.getLongExtra("to",   0L)
                TransactionFilter.byDateRange(allTx, from, to)
                    .filter { matches(it, label) }
            }
            else -> allTx.filter { matches(it, label) }
        }

        // Hook up adapter
        val cats = PrefsManager.loadCategories()
        val adapter = TransactionAdapter(list.toMutableList(), cats) { /* no-op */ }
        rv.adapter = adapter

        // Show empty state if needed
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        
        // Calculate and display category total
        calculateAndDisplayTotal(list)
    }

    private fun matches(tx: Transaction, label: String): Boolean {
        val raw   = tx.category
        val plain = raw.substringAfter(" ", raw)
        return raw.equals(label, true) || plain.equals(label, true)
    }
    
    private fun calculateAndDisplayTotal(transactions: List<Transaction>) {
        // Sum all expenses (negative) and income (positive) for this category
        val total = transactions.sumOf { 
            if (it.type == TxType.EXPENSE) -it.amount else it.amount 
        }
        
        // Format the total with the currency symbol
        val formattedTotal = CurrencyFormatter.format(total)
        
        // Apply color based on whether it's positive or negative
        tvCategoryTotal.text = formattedTotal
        tvCategoryTotal.setTextColor(
            if (total >= 0) 
                resources.getColor(R.color.colorIncome, theme)
            else 
                resources.getColor(R.color.colorExpense, theme)
        )
    }
}