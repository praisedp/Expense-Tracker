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
        val chip = Chip(context)
        chip.text = text

        when (type) {
            ChipType.INCOME -> {
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.income_category_bg)
                )
                chip.chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.income_category_stroke)
                )
                chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            ChipType.EXPENSE -> {
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.expense_category_bg)
                )
                chip.chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.expense_category_stroke)
                )
                chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }

        // Enhanced floating bubble appearance
        chip.chipStrokeWidth = 1f
        chip.chipCornerRadius = 16f
        chip.isCheckable = false
        chip.isClickable = true
        chip.elevation = 4f
        chip.setPadding(12, 8, 12, 8)

        // Optional icon if desired
        if (type == ChipType.INCOME) {
            chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_income_small)
            chip.isChipIconVisible = true
        } else {
            chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_expense_small)
            chip.isChipIconVisible = true
        }

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