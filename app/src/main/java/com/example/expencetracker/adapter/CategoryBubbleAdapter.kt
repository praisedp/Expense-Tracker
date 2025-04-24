package com.example.expencetracker.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.data.Category
import com.example.expencetracker.data.TxType
import com.example.expencetracker.ui.AddCategoryActivity
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying categories as selectable bubbles in a RecyclerView.
 */
class CategoryBubbleAdapter(
    private var categories: List<Category>,
    private val onCategorySelected: (category: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition = -1
    private var currentType: TxType = TxType.EXPENSE
    
    // Define view types
    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_ADD_BUTTON = 1
    }

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
                // Set text color to black for unselected items
                tvCategory.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
            }
            
            // Always set stroke width to 0 to remove borders
            cardCategory.strokeWidth = 0

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
    
    inner class AddCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryBubble)
        private val cardCategory: MaterialCardView = itemView.findViewById(R.id.cardCategoryBubble)
        
        fun bind() {
            // Set visual style for add category button
            tvCategory.text = "➕ Add Category"
            
            // Style the card differently from regular categories
            cardCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorBackground))
            // Use dashed border or other visual cue
            tvCategory.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
            
            // Always set stroke width to 0 to remove borders
            cardCategory.strokeWidth = 0
            
            // Set click listener to navigate to AddCategoryActivity
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, AddCategoryActivity::class.java)
                // Pass the current transaction type to pre-select in the category creation form
                intent.putExtra("default_category_type", currentType.name)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size) TYPE_CATEGORY else TYPE_ADD_BUTTON
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_bubble, parent, false)
                CategoryViewHolder(view)
            }
            TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_bubble, parent, false)
                AddCategoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                holder.bind(categories[position], position)
            }
            is AddCategoryViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun getItemCount(): Int = categories.size + 1 // +1 for the add button

    /**
     * Updates the adapter with a new list of categories.
     * This is typically called when the transaction type changes.
     */
    fun updateCategories(newCategories: List<Category>, type: TxType = TxType.EXPENSE) {
        categories = newCategories
        currentType = type
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