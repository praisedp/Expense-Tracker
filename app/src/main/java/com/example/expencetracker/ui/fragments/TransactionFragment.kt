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

class TransactionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rvTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)
        loadTransactions()
    }

    // Override onResume to refresh the data when the fragment resumes.
    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        val transactions = PrefsManager.loadTransactions()
        recyclerView.adapter = TransactionAdapter(transactions) { transaction ->
            showDeleteTransactionDialog(transaction)
        }
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                if (PrefsManager.deleteTransaction(transaction.id)) {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions()  // Refresh list after deletion.
                } else {
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}