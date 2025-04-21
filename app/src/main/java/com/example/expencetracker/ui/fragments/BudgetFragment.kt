package com.example.expencetracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.CategoryBudgetAdapter
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.ui.dialogs.BudgetDialogFragment
import com.example.expencetracker.util.BudgetCalculator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Currency

class BudgetFragment : Fragment() {

    private lateinit var tvTotalStatus: TextView
    private lateinit var progressTotal: ProgressBar
    private lateinit var rvCats: RecyclerView
    private lateinit var fabSet: FloatingActionButton
    private lateinit var adapter: CategoryBudgetAdapter

    private val fmt by lazy {
        NumberFormat.getCurrencyInstance().apply {
            currency = java.util.Currency.getInstance(PrefsManager.getCurrency())
        }
    }

    // ── lifecycle ──────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_budget, container, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        tvTotalStatus = v.findViewById(R.id.tvTotalStatus)
        progressTotal = v.findViewById(R.id.progressTotalBudget)
        rvCats        = v.findViewById(R.id.rvCategoryBudgets)
        fabSet        = v.findViewById(R.id.fabSetBudget)

        rvCats.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryBudgetAdapter()
        rvCats.adapter = adapter

        // ←‑‑ Replace the old listener with this one
        fabSet.setOnClickListener {
            BudgetDialogFragment().show(parentFragmentManager, "budgetDlg")
        }

        // Refresh the screen as soon as a budget is saved in the dialog
        parentFragmentManager.setFragmentResultListener(
            "budget_saved",
            viewLifecycleOwner
        ) { _, _ -> refreshUI() }
    }
    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // ── UI refresh ────────────────────────────────────────────
    private fun refreshUI() {
        val totalLimit = PrefsManager.getTotalBudget()
        val monthTx   = BudgetCalculator.transactionsThisMonth(PrefsManager.loadTransactions())
        val spent     = BudgetCalculator.spentTotal(monthTx)

        // total progress
        if (totalLimit <= 0.0) {
            tvTotalStatus.text = getString(R.string.no_budget_set)
            progressTotal.progress = 0
            progressTotal.progressTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.green_500)
        } else {
            val pct = (spent / totalLimit).coerceIn(0.0, 1.0)
            progressTotal.progress = (pct * 100).toInt()
            progressTotal.progressTintList =
                ContextCompat.getColorStateList(requireContext(), when {
                    pct < 0.75 -> R.color.green_500
                    pct <= 1   -> R.color.amber_600
                    else       -> R.color.red_600
                })
            tvTotalStatus.text = getString(
                R.string.budget_status_fmt,
                fmt.format(spent),
                fmt.format(totalLimit)
            )
        }

        // category list
        val spentMap = BudgetCalculator.spentByCategory(monthTx)
        val limits   = PrefsManager.loadCategoryBudgets()
        adapter.submitData(spentMap, limits)
    }
}