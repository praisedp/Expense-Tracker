package com.example.expencetracker.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.BudgetInputAdapter
import com.example.expencetracker.data.CategoryBudget
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.PrefsManager.loadCategories
import com.example.expencetracker.data.PrefsManager.loadCategoryBudgets
import com.example.expencetracker.data.PrefsManager.setTotalBudget
import com.example.expencetracker.data.PrefsManager.saveCategoryBudgets
import android.widget.Toast
import com.example.expencetracker.data.TxType

/** Bottom‑sheet to set total & per‑category budgets */
class BudgetDialogFragment : BottomSheetDialogFragment() {

    interface OnBudgetChanged {
        fun onBudgetSaved()
    }

    override fun onCreateView(
        i: LayoutInflater, c: ViewGroup?, b: Bundle?
    ): View = i.inflate(R.layout.dialog_budget, c, false)

    override fun onViewCreated(v: View, b: Bundle?) {
        val etTotal = v.findViewById<TextInputEditText>(R.id.etTotalBudget)
        val rv      = v.findViewById<RecyclerView>(R.id.rvCatInputs)
        val btnSave = v.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancel)

        // prefill total
        etTotal.setText(PrefsManager.getTotalBudget().takeIf { it > 0 }?.toString() ?: "")

        // prep per‑category inputs
        val catNames = loadCategories()
            .filter { it.type == TxType.EXPENSE }               // ← keep only expenses
            .map { "${it.emoji} ${it.name}" }
        val limits = PrefsManager.loadCategoryBudgets().toMutableList()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = BudgetInputAdapter(catNames, limits)

        btnSave.setOnClickListener {
            val total = etTotal.text.toString().toDoubleOrNull() ?: 0.0
            val sumSub = limits.sumOf { it.limit }

            if (total <= 0) {
                Toast.makeText(context, "Enter total budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sumSub > total) {
                Toast.makeText(context, "Category limits exceed total", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setTotalBudget(total)
            saveCategoryBudgets(limits)
            parentFragmentManager.setFragmentResult("budget_saved", Bundle())
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }
    }
}