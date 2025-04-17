package com.example.expencetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onLongClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        fun bind(category: Category) {
            tvCategory.text = "${category.emoji} ${category.name}"
            itemView.setOnLongClickListener {
                onLongClick(category)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}