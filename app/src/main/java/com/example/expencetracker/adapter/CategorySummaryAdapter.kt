package com.example.expencetracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.CategoryRow
import java.text.NumberFormat

class CategorySummaryAdapter :
    ListAdapter<CategoryRow, CategorySummaryAdapter.RowVH>(diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return RowVH(v)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) =
        holder.bind(getItem(position))

    class RowVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val tvEmoji: TextView   = itemView.findViewById(R.id.tvCatEmoji)
        private val tvName:  TextView   = itemView.findViewById(R.id.tvCatName)
        private val tvAmt:   TextView   = itemView.findViewById(R.id.tvCatAmount)
        private val bar:     ProgressBar= itemView.findViewById(R.id.progressCat)
        private val moneyFmt = NumberFormat.getCurrencyInstance()

        fun bind(row: CategoryRow) {
//            tvEmoji.text = row.emoji
            tvName.text  = row.name
            tvAmt.text   = moneyFmt.format(row.amount)
            bar.progress = row.percent.toInt()
            bar.progressTintList = android.content.res.ColorStateList.valueOf(row.color)
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<CategoryRow>() {
            override fun areItemsTheSame(o: CategoryRow, n: CategoryRow) = o.name == n.name
            override fun areContentsTheSame(o: CategoryRow, n: CategoryRow) = o == n
        }
    }
}