package com.example.expencetracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager

class BudgetFragment : Fragment() {

    private lateinit var etBudget: EditText
    private lateinit var btnSaveBudget: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBudgetStatus: TextView

    // For demonstration: using dummy values
    private var monthlyBudget: Double = 1000.0
    private var spent: Double = 750.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etBudget = view.findViewById(R.id.etBudget)
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget)
        progressBar = view.findViewById(R.id.progressBarBudget)
        tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus)

        // Pre-load budget value (normally from PrefsManager)
        etBudget.setText(monthlyBudget.toString())

        // Calculate current budget status
        updateBudgetStatus()

        btnSaveBudget.setOnClickListener {
            // Save the new budget and update UI (persist budget using PrefsManager later)
            val input = etBudget.text.toString().toDoubleOrNull()
            if (input != null && input > 0) {
                monthlyBudget = input
                updateBudgetStatus()
                // TODO: Save monthlyBudget to PrefsManager
            }
        }
    }

    private fun updateBudgetStatus() {
        // Update progress bar (percentage of budget spent)
        val progress = ((spent / monthlyBudget) * 100).toInt()
        progressBar.progress = progress

        // Update status text based on budget usage
        val statusText = when {
            spent >= monthlyBudget -> "Budget Exceeded!"
            spent >= monthlyBudget * 0.9 -> "Approaching Budget Limit!"
            else -> "Within Budget"
        }
        tvBudgetStatus.text = "Spent: $$spent / Budget: $$monthlyBudget. $statusText"
    }
}