package com.example.expencetracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.expencetracker.R
import java.util.Calendar

/**
 * Modern month-year picker dialog that returns (year, zero-based month).
 * Uses NumberPickers for a clean, modern look.
 */
class MonthYearPickerDialog(
    private val onMonthYear: (year: Int, month0: Int) -> Unit,
    private val onCancel: () -> Unit = {}
) : DialogFragment() {

    private val cal: Calendar = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonth = cal.get(Calendar.MONTH)
    
    // Month names for display
    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June", 
        "July", "August", "September", "October", "November", "December"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_month_year_picker, null)
        
        val monthPicker = view.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = view.findViewById<NumberPicker>(R.id.yearPicker)
        
        // Configure month picker
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = monthNames
        monthPicker.value = currentMonth
        
        // Configure year picker (from 10 years ago to 10 years in future)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear
        
        builder.setView(view)
            .setTitle("Select Month & Year")
            .setPositiveButton("OK") { _, _ ->
                onMonthYear(yearPicker.value, monthPicker.value)
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
        
        return builder.create()
    }
}