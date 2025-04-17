package com.example.expencetracker.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.Category

class TransactionListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var transactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use a layout file defined below
        return inflater.inflate(R.layout.fragment_transaction_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rvTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)
        loadData()
    }

    // Load transactions from SharedPreferences and update adapter
    private fun loadData() {
        transactions = PrefsManager.loadTransactions().toMutableList()
        // Load categories to pass into the adapter
        val categories: List<Category> = PrefsManager.loadCategories()
        val adapter = TransactionAdapter(transactions, categories) { transaction ->
            showDeleteTransactionDialog(transaction)
        }
        recyclerView.adapter = adapter
    }

    // Show a confirmation dialog before deleting a transaction
    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                val removed = PrefsManager.deleteTransaction(transaction.id)
                if (removed) {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    loadData()  // Refresh list
                } else {
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}