// app/java/com/example/expencetracker/ui/CategoryTransactionsActivity.kt
package com.example.expencetracker.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.util.TransactionFilter

class CategoryTransactionsActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_transactions)

        // Show category name
        val tvCatTitle = findViewById<TextView>(R.id.tvCatTitle)
        tvCatTitle.text = intent.getStringExtra("label") ?: ""

        rv = findViewById(R.id.rvTransByCat)
        rv.layoutManager = LinearLayoutManager(this)
        tvEmpty = findViewById(R.id.tvCatEmpty)

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

        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun matches(tx: Transaction, label: String): Boolean {
        val raw   = tx.category
        val plain = raw.substringAfter(" ", raw)
        return raw.equals(label, true) || plain.equals(label, true)
    }
}