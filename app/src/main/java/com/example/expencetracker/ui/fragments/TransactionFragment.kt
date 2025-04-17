package com.example.expencetracker.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expencetracker.R
import com.example.expencetracker.data.TxType
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager

class TransactionFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.rvTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 1. Load transactions and categories
        allTransactions = PrefsManager.loadTransactions()
        val allCategories = PrefsManager.loadCategories()

// 2. Pass both into the adapter
        adapter = TransactionAdapter(allTransactions, allCategories) { tx ->
            showDeleteTransactionDialog(tx)
        }
        recyclerView.adapter = adapter

        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Income"))
        tabLayout.addTab(tabLayout.newTab().setText("Expenses"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> filter(null)
                    1 -> filter(TxType.INCOME)
                    2 -> filter(TxType.EXPENSE)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun filter(type: TxType?) {
        val filtered = if (type == null) {
            allTransactions
        } else {
            allTransactions.filter { it.type == type }
        }
        adapter.updateData(filtered)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        allTransactions = PrefsManager.loadTransactions()
        filter(tabLayout.selectedTabPosition.let { if (it == 1) TxType.INCOME else if (it == 2) TxType.EXPENSE else null })
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                if (PrefsManager.deleteTransaction(transaction.id)) {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions()
                } else {
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}