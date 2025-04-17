package com.example.expencetracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import com.example.expencetracker.data.Category
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionActivity : AppCompatActivity() {

    private lateinit var rgTxType: RadioGroup
    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvDate: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // A variable to hold the selected date (as epoch millis)
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_transaction)

        // Bind views
        rgTxType = findViewById(R.id.rgTxType)
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvDate = findViewById(R.id.tvDate)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // Set up the spinner with actual categories based on selected type
        updateCategorySpinner()

        // Listener to update spinner when the transaction type changes
        rgTxType.setOnCheckedChangeListener { _, _ ->
            updateCategorySpinner()
        }

        // Set initial date text
        updateDateDisplay(selectedDate)

        // When tvDate is clicked, open a DatePickerDialog
        tvDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selYear, selMonth, selDayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(selYear, selMonth, selDayOfMonth)
                selectedDate = cal.timeInMillis
                updateDateDisplay(selectedDate)
            }, year, month, day).show()
        }

        // Save button click - Validate and save the transaction
        btnSave.setOnClickListener {
            if (validateInput()) {
                val title = etTitle.text.toString().trim()
                val amount = etAmount.text.toString().toDouble()
                val category = spinnerCategory.selectedItem.toString()
                val type = if (rgTxType.checkedRadioButtonId == R.id.rbIncome) TxType.INCOME else TxType.EXPENSE

                // Create a new Transaction object
                val newTx = Transaction(
                    id = System.currentTimeMillis(),
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    date = selectedDate
                )

                // Load existing transactions, add the new one, and save them
                val currentTxs = PrefsManager.loadTransactions().toMutableList()
                currentTxs.add(newTx)
                PrefsManager.saveTransactions(currentTxs)

                Toast.makeText(this, "Transaction Saved", Toast.LENGTH_SHORT).show()
                finish()  // Close the activity and return to the previous screen
            }
        }

        // Cancel button click - Simply finish the activity without saving
        btnCancel.setOnClickListener {
            finish()
        }
    }

    // Function to update the spinner with categories based on selected transaction type
    private fun updateCategorySpinner() {
        // Load the saved categories from SharedPreferences
        val savedCategories = PrefsManager.loadCategories()
        // Determine the selected transaction type (income or expense)
        val selectedType = if (rgTxType.checkedRadioButtonId == R.id.rbIncome) TxType.INCOME else TxType.EXPENSE

        // Filter the categories based on the selected type. If there are none, provide fallback default categories.
        val filteredCategories = if (savedCategories.isNotEmpty()) {
            savedCategories.filter { it.type == selectedType }
        } else {
            if (selectedType == TxType.INCOME)
                listOf(
                    Category("Salary", TxType.INCOME, "💰"),
                    Category("Bonus", TxType.INCOME, "💸"),
                    Category("Other Income", TxType.INCOME, "💲")
                )
            else
                listOf(
                    Category("Food", TxType.EXPENSE, "🍔"),
                    Category("Bills", TxType.EXPENSE, "📄"),
                    Category("Entertainment", TxType.EXPENSE, "🎬"),
                    Category("Other Expenses", TxType.EXPENSE, "🛒")
                )
        }

        // Now, map the Category objects to display strings that include both the emoji and the category name.
        val categoryNames = filteredCategories.map { "${it.emoji} ${it.name}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    // Helper function to update the date display in tvDate
    private fun updateDateDisplay(timeInMillis: Long) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvDate.text = sdf.format(Date(timeInMillis))
    }

    // Validate user input; return true if all fields are valid
    private fun validateInput(): Boolean {
        if (etTitle.text.isNullOrBlank()) {
            etTitle.error = "Title is required"
            return false
        }
        if (etAmount.text.isNullOrBlank()) {
            etAmount.error = "Amount is required"
            return false
        }
        if (etAmount.text.toString().toDoubleOrNull() == null || etAmount.text.toString().toDouble() <= 0) {
            etAmount.error = "Enter a valid amount"
            return false
        }
        // You can also add a check here to ensure that the spinner has a valid selection, if needed.
        return true
    }
}