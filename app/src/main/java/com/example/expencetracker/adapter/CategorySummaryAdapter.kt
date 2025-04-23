package com.example.expencetracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.CategoryRow
import com.example.expencetracker.util.CurrencyFormatter
import java.text.NumberFormat

class CategorySummaryAdapter(
    private val onItemClick: (CategoryRow) -> Unit
) :
    ListAdapter<CategoryRow, RecyclerView.ViewHolder>(diff) {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_SHOW_ALL = 1

        private val diff = object : DiffUtil.ItemCallback<CategoryRow>() {
            override fun areItemsTheSame(o: CategoryRow, n: CategoryRow) = o.name == n.name
            override fun areContentsTheSame(o: CategoryRow, n: CategoryRow) = o == n
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.name == "expense_show_all" || item.name == "income_show_all" ||
                  item.name == "expense_hide" || item.name == "income_hide") {
            TYPE_SHOW_ALL
        } else {
            TYPE_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SHOW_ALL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_all, parent, false)
                ShowAllViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_summary, parent, false)
                CategoryViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is CategoryViewHolder -> holder.bind(item)
            is ShowAllViewHolder -> holder.bind(item)
        }
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCatName)
        private val tvAmt: TextView = itemView.findViewById(R.id.tvCatAmount)
        private val bar: ProgressBar = itemView.findViewById(R.id.progressCat)

        fun bind(row: CategoryRow) {
            tvName.text = row.name
            tvAmt.text = CurrencyFormatter.format(row.amount)
            bar.progress = row.percent.toInt()
            bar.progressTintList =
                android.content.res.ColorStateList.valueOf(row.color)
            itemView.setOnClickListener { onItemClick(row) }
        }
    }

    inner class ShowAllViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvShowAll: TextView = itemView.findViewById(R.id.tvShowAll)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivExpandIcon)

        fun bind(item: CategoryRow) {
            // Is this a hide button or show all button?
            val isHideButton = item.name.endsWith("_hide")
            
            // Update text and icon
            tvShowAll.text = if (isHideButton) "Hide Categories" else "Show All Categories"
            ivIcon.setImageResource(
                if (isHideButton) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
            
            // Clicking just passes the item to the listener
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}