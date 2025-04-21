package com.example.expencetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.CategoryBudget
import androidx.core.widget.doAfterTextChanged

/**
 * Lets user type per‑category budget numbers.
 * Reads / writes through the supplied mutable list.
 */
@Suppress("OPT_IN_USAGE", "MemberVisibilityCanBePrivate")
class BudgetInputAdapter(
    private val categories: List<String>,
    /** existing budgets will be mutated to reflect user edits */
    private val limits: MutableList<CategoryBudget>
) : RecyclerView.Adapter<BudgetInputAdapter.BudgetVH>() {

    override fun onCreateViewHolder(p: ViewGroup, v: Int): BudgetVH =
        BudgetVH(LayoutInflater.from(p.context)
            .inflate(R.layout.item_budget_input, p, false))

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(h: BudgetVH, pos: Int) = h.bind(categories[pos])

    inner class BudgetVH(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvLabel: TextView = itemView.findViewById(R.id.tvCatLabel)
        private val etLimit: EditText = itemView.findViewById(R.id.etCatLimit)

        fun bind(category: String) {
            tvLabel.text = category

            // prefill existing limit
            val existing = limits.firstOrNull { it.category == category }
            if (existing != null) etLimit.setText(existing.limit.toString())

            // update list on text change
            etLimit.doAfterTextChanged {
                val valNum = it?.toString()?.toDoubleOrNull() ?: 0.0
                limits.removeAll { it.category == category }
                if (valNum > 0) limits.add(CategoryBudget(category, valNum))
            }
        }
    }
}