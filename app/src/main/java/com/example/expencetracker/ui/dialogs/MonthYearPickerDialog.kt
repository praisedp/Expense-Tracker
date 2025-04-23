package com.example.expencetracker.ui.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.Calendar

/**
 * Simple month-year picker dialog that returns (year, zero-based month).
 * The day spinner and calendar grid are hidden so users pick only month & year.
 */
class MonthYearPickerDialog(
    private val onMonthYear: (year: Int, month0: Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()

        // Pass a spinner theme so many devices skip the calendar grid automatically
        val dlg = DatePickerDialog(
            requireContext(),
            android.R.style.Theme_Holo_Dialog,          // spinner style
            { _: DatePicker, y: Int, m: Int, _ -> onMonthYear(y, m) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)              // ignored
        )

        // Hide the day spinner
        dlg.datePicker.findViewById<View>(
            resources.getIdentifier("android:id/day", null, null)
        )?.visibility = View.GONE

        // Hide the calendar grid view (if present)
        dlg.datePicker.calendarView.visibility = View.GONE

        return dlg
    }
}