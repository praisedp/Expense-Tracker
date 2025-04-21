package com.example.expencetracker.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.ui.AddCategoryActivity
import com.example.expencetracker.ui.AddEditTransactionActivity
import com.example.expencetracker.ui.CurrencySelectionActivity
import com.example.expencetracker.util.CurrencyConverter
import kotlinx.coroutines.launch
import com.example.expencetracker.data.CategoryBudget

class SettingsFragment : Fragment() {

    private val REQUEST_CURRENCY = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Button for launching AddEditTransactionActivity
        val btnAddTransaction: Button = view.findViewById(R.id.btnAddTransaction)
        btnAddTransaction.setOnClickListener {
            val intent = Intent(activity, AddEditTransactionActivity::class.java)
            startActivity(intent)
        }

        // Button for launching AddCategoryActivity
        val btnAddCategory: Button = view.findViewById(R.id.btnAddCategory)
        btnAddCategory.setOnClickListener {
            val intent = Intent(activity, AddCategoryActivity::class.java)
            startActivity(intent)
        }

        // Button for changing currency
        val btnChangeCurrency: Button = view.findViewById(R.id.btnChangeCurrency)
        btnChangeCurrency.setOnClickListener {
            val intent = Intent(activity, CurrencySelectionActivity::class.java)
            startActivityForResult(intent, REQUEST_CURRENCY)
        }

        return view
    }

    @Deprecated("Use Activity Result API instead")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CURRENCY && resultCode == Activity.RESULT_OK) {
            val newCurrency = data?.getStringExtra("selected_currency") ?: return
            val currentCurrency = PrefsManager.getCurrency()

            // Debug log the current and new currency
            Log.d("CurrencyDebug", "Current currency: $currentCurrency, New currency: $newCurrency")

            // Only update if the currency has changed.
            if (newCurrency != currentCurrency) {
                lifecycleScope.launch {
                    // Fetch conversion rate from current to new currency.
                    val conversionRate = CurrencyConverter.fetchConversionRate(currentCurrency, newCurrency)
                    // Debug log the conversion rate.
                    Log.d(
                        "CurrencyDebug",
                        "Conversion rate from $currentCurrency to $newCurrency: $conversionRate"
                    )

                    // Load existing transactions.
                    val transactions = PrefsManager.loadTransactions().toMutableList()
                    // For each transaction, update its amount using the conversion rate.
                    transactions.forEach { tx ->
                        val oldAmount = tx.amount
                        tx.amount = oldAmount * conversionRate
                        // Log the update for each transaction.
                        Log.d(
                            "CurrencyDebug",
                            "Updated Transaction ${tx.id}: $oldAmount -> ${tx.amount}"
                        )
                    }
                    // Save updated transactions.
                    PrefsManager.saveTransactions(transactions)
                    // Convert overall monthly budget
                    val oldTotal = PrefsManager.getTotalBudget()
                    if (oldTotal > 0.0) {
                        PrefsManager.setTotalBudget(oldTotal * conversionRate)
                    }

                    // Convert each per-category budget
                    val oldCatBudgets = PrefsManager.loadCategoryBudgets()
                    val newCatBudgets = oldCatBudgets.map { it.copy(limit = it.limit * conversionRate) }
                    PrefsManager.saveCategoryBudgets(newCatBudgets)
                    // Update stored currency.
                    PrefsManager.setCurrency(newCurrency)
                    Toast.makeText(context, "Currency updated to $newCurrency", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Currency remains unchanged", Toast.LENGTH_SHORT).show()
            }
        }
    }
}