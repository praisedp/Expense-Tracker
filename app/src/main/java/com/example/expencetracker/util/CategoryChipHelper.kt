package com.example.expencetracker.util
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.expencetracker.R
object CategoryChipHelper {

    enum class ChipType {
        INCOME,
        EXPENSE
    }

    fun createCategoryChip(context: Context, text: String, type: ChipType): Chip {
        // Create chip with the appropriate style
        val chip = when (type) {
            ChipType.INCOME -> Chip(context, null, R.style.Widget_App_Chip_Income)
            ChipType.EXPENSE -> Chip(context, null, R.style.Widget_App_Chip_Expense)
        }
        
        chip.text = text
        
        // Basic chip properties
        chip.chipStrokeWidth = 1f
        chip.chipCornerRadius = 16f
        chip.isCheckable = false
        chip.isClickable = true
        chip.elevation = 2f
        chip.setPadding(12, 8, 12, 8)

        // Set the appropriate icon
        if (type == ChipType.INCOME) {
            chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_income_small)
            chip.isChipIconVisible = true
        } else {
            chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_expense_small)
            chip.isChipIconVisible = true
        }

        // Force text color to prevent it from using themed colors
        chip.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))

        return chip
    }

    fun addCategoriesToChipGroup(
        context: Context,
        chipGroup: ChipGroup,
        categories: List<String>,
        type: ChipType
    ) {
        chipGroup.removeAllViews()

        for (category in categories) {
            val chip = createCategoryChip(context, category, type)
            chipGroup.addView(chip)
        }
    }
}