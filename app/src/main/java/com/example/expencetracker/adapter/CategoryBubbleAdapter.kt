package com.example.expencetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.Category
import com.example.expencetracker.data.TxType
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying categories as selectable bubbles in a RecyclerView.
 */
class CategoryBubbleAdapter(
    private var categories: List<Category>,
    private val onCategorySelected: (category: String) -> Unit
) : RecyclerView.Adapter<CategoryBubbleAdapter.CategoryViewHolder>() {

    private var selectedPosition = -1

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryBubble)
        private val cardCategory: MaterialCardView = itemView.findViewById(R.id.cardCategoryBubble)

        fun bind(category: Category, position: Int) {
            val displayText = "${category.emoji} ${category.name}"
            tvCategory.text = displayText

            // Set background color based on selection status and category type
            val isSelected = position == selectedPosition
            
            if (isSelected) {
                // Set card background for selected state
                if (category.type == TxType.INCOME) {
                    cardCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorIncome))
                } else {
                    cardCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorExpense))
                }
                // Set text color to white for selected items
                tvCategory.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorSurface))
            } else {
                // Set card background for unselected state
                cardCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorBackground))
                // Set text color based on category type
                val textColor = if (category.type == TxType.INCOME) 
                    R.color.colorIncome else R.color.colorExpense
                tvCategory.setTextColor(ContextCompat.getColor(itemView.context, textColor))
            }
            
            // Set stroke for unselected items
            if (!isSelected) {
                cardCategory.strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                cardCategory.setStrokeColor(ContextCompat.getColor(itemView.context, 
                    if (category.type == TxType.INCOME) R.color.colorIncome else R.color.colorExpense))
            } else {
                cardCategory.strokeWidth = 0
            }

            // Set click listener
            itemView.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                
                // Update previously selected item and currently selected item
                if (previousSelected != -1) {
                    notifyItemChanged(previousSelected)
                }
                notifyItemChanged(selectedPosition)
                
                // Notify the parent activity about the selection
                onCategorySelected(displayText)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_bubble, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount(): Int = categories.size

    /**
     * Updates the adapter with a new list of categories.
     * This is typically called when the transaction type changes.
     */
    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        selectedPosition = -1  // Reset selection
        notifyDataSetChanged()
    }

    /**
     * Programmatically select a category by its display text.
     * Used when editing an existing transaction.
     */
    fun selectCategory(categoryText: String) {
        val index = categories.indexOfFirst { "${it.emoji} ${it.name}" == categoryText }
        if (index >= 0 && index != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = index
            
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
        }
    }
} 