package com.example.expencetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.CategoryBudget
import com.example.expencetracker.data.PrefsManager
import java.text.NumberFormat
import java.util.Currency

/**
 * RecyclerView adapter that shows, for each category:
 *   • name
 *   • spent / limit text
 *   • a small horizontal progress bar (hidden when no limit set)
 */
class CategoryBudgetAdapter :
    RecyclerView.Adapter<CategoryBudgetAdapter.CatVH>() {

    private val currencyFmt by lazy {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(PrefsManager.getCurrency())
        }
    }
    private var rows: List<Row> = emptyList()

    /** Called by BudgetFragment to refresh data. */
    fun submitData(
        spentMap: Map<String, Double>,
        limits: List<CategoryBudget>
    ) {
        val limitMap = limits.associateBy { it.category }
        val categories = (spentMap.keys + limits.map { it.category }).toSet()

        rows = categories.map { cat ->
            Row(
                category = cat,
                spent = spentMap[cat] ?: 0.0,
                limit = limitMap[cat]?.limit ?: 0.0
            )
        }.sortedBy { it.category }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatVH =
        CatVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_budget_category, parent, false)
        )

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: CatVH, position: Int) =
        holder.bind(rows[position])

    // ────────── ViewHolder ────────────────────────────────────────────
    inner class CatVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCatName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvCatAmount)
        private val bar: ProgressBar = itemView.findViewById(R.id.progressCat)

        fun bind(row: Row) {
            tvName.text = row.category
            if (row.limit <= 0.0) {
                // No per-category limit → hide bar
                tvAmount.text = currencyFmt.format(row.spent)
                bar.visibility = View.INVISIBLE
            } else {
                val pct = (row.spent / row.limit).coerceIn(0.0, 1.0)
                bar.visibility = View.VISIBLE
                bar.progress = (pct * 100).toInt()
                tvAmount.text = "${currencyFmt.format(row.spent)} / ${currencyFmt.format(row.limit)}"
            }
        }
    }

    data class Row(
        val category: String,
        val spent: Double,
        val limit: Double
    )
}