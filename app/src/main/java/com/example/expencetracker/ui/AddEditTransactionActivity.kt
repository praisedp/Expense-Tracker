package com.example.expencetracker.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.CategoryBubbleAdapter
import com.example.expencetracker.data.*
import com.example.expencetracker.util.BudgetAlertManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionActivity : AppCompatActivity() {

    private lateinit var rgTxType: RadioGroup
    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var rvCategoryBubbles: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingTxId: Long? = null        // null = add‑mode, else edit‑mode
    private var selectedCategory: String? = null // Track the selected category
    private lateinit var categoryAdapter: CategoryBubbleAdapter

    // ─── life‑cycle ────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_transaction)

        bindViews()
        configureDatePicker()
        configureButtons()
        configureTypeRadioListener()
        setupCategoryRecyclerView()  // New method to set up RecyclerView

        // If launched for EDIT, pre‑populate fields
        editingTxId = intent.getLongExtra(EXTRA_TX_ID, -1L).takeIf { it != -1L }
        editingTxId?.let { prefillFields(it) }       // switch to edit‑mode
        updateCategoryBubbles()                      // build category list
    }

    // ─── view helpers ──────────────────────────────────────────────────
    private fun bindViews() {
        rgTxType = findViewById(R.id.rgTxType)
        etTitle  = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        rvCategoryBubbles = findViewById(R.id.rvCategoryBubbles)
        tvDate   = findViewById(R.id.tvDate)
        btnSave  = findViewById(R.id.btnSave)
        btnCancel= findViewById(R.id.btnCancel)
    }

    private fun configureDatePicker() {
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = cal.timeInMillis
                tvDate.text = formatDate(selectedDate)
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }
        tvDate.text = formatDate(selectedDate)
    }

    private fun configureButtons() {
        btnSave.setOnClickListener { onSave() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun configureTypeRadioListener() =
        rgTxType.setOnCheckedChangeListener { _, _ -> updateCategoryBubbles() }

    private fun setupCategoryRecyclerView() {
        // Configure FlexboxLayoutManager programmatically for better control
        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        
        rvCategoryBubbles.layoutManager = layoutManager
        
        // Initialize adapter with empty list, will be populated later
        categoryAdapter = CategoryBubbleAdapter(emptyList()) { category ->
            selectedCategory = category  // Store selected category
        }
        rvCategoryBubbles.adapter = categoryAdapter
    }

    // ─── edit‑mode prefill ─────────────────────────────────────────────
    private fun prefillFields(txId: Long) {
        val tx = PrefsManager.loadTransactions().firstOrNull { it.id == txId } ?: return
        etTitle.setText(tx.title)
        etAmount.setText(tx.amount.toString())
        selectedDate = tx.date
        tvDate.text = formatDate(selectedDate)

        if (tx.type == TxType.INCOME) rgTxType.check(R.id.rbIncome) else rgTxType.check(R.id.rbExpense)

        // Store the category text to select it after adapter is populated
        selectedCategory = tx.category
    }

    // ─── category population ───────────────────────────────────────────
    private fun updateCategoryBubbles() {
        val saved = PrefsManager.loadCategories()
        val type = if (rgTxType.checkedRadioButtonId == R.id.rbIncome) TxType.INCOME else TxType.EXPENSE
        val filteredCategories = saved.filter { it.type == type }
        
        // Update adapter with filtered categories and pass the current type
        categoryAdapter.updateCategories(filteredCategories, type)
        
        // If we're editing, select the original category
        selectedCategory?.let { originalCategory ->
            categoryAdapter.selectCategory(originalCategory)
        }
    }

    // Override onResume to refresh categories when returning from AddCategoryActivity
    override fun onResume() {
        super.onResume()
        // Refresh categories when returning to this activity (e.g., after adding a new category)
        updateCategoryBubbles()
    }

    // ─── save logic ───────────────────────────────────────────────────
    private fun onSave() {
        if (!validate()) return
        
        try {
            val title = etTitle.text.toString().trim()
            val amount = etAmount.text.toString().toDouble()
            
            // Use the selected category from the adapter
            val category = selectedCategory ?: throw IllegalStateException("No category selected")
            
            val type = if (rgTxType.checkedRadioButtonId == R.id.rbIncome) TxType.INCOME else TxType.EXPENSE
            
            // Convert amount to positive or negative based on type
            val adjustedAmount = if (type == TxType.INCOME) Math.abs(amount) else -Math.abs(amount)
    
            val list = PrefsManager.loadTransactions().toMutableList()
    
            if (editingTxId == null) {
                // ADD
                list.add(
                    Transaction(
                        id = System.currentTimeMillis(),
                        title = title,
                        amount = adjustedAmount,
                        category = category,
                        type = type,
                        date = selectedDate
                    )
                )
            } else {
                // EDIT: find and replace
                val index = list.indexOfFirst { it.id == editingTxId }
                if (index != -1) {
                    list[index] = list[index].copy(
                        title = title,
                        amount = adjustedAmount,
                        category = category,
                        type = type,
                        date = selectedDate
                    )
                }
            }
    
            PrefsManager.saveTransactions(list)
            BudgetAlertManager.check(this)
            setResult(RESULT_OK)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── validation  ──────────────────────────────────────────────────
    private fun validate(): Boolean {
        var isValid = true

        // Validate title
        when {
            etTitle.text.isNullOrBlank() -> {
                etTitle.error = "Title is required"
                isValid = false
            }
            etTitle.text.toString().trim().length < 2 -> {
                etTitle.error = "Title must be at least 2 characters"
                isValid = false
            }
            etTitle.text.toString().trim().length > 50 -> {
                etTitle.error = "Title must be less than 50 characters"
                isValid = false
            }
        }

        // Validate amount
        val amountText = etAmount.text.toString()
        when {
            amountText.isBlank() -> {
                etAmount.error = "Amount is required"
                isValid = false
            }
            amountText.toDoubleOrNull() == null -> {
                etAmount.error = "Enter a valid number"
                isValid = false
            }
            amountText.toDouble() <= 0 -> {
                etAmount.error = "Amount must be greater than zero"
                isValid = false
            }
            amountText.toDouble() > 9999999.99 -> {
                etAmount.error = "Amount is too large (max: 9,999,999.99)"
                isValid = false
            }
            // Check precision - too many decimal places
            amountText.contains(".") && amountText.substringAfter(".").length > 2 -> {
                etAmount.error = "Only two decimal places allowed"
                isValid = false
            }
        }

        // Validate category is selected
        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Validate date
        if (selectedDate > System.currentTimeMillis()) {
            Toast.makeText(this, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    // ─── utils ────────────────────────────────────────────────────────
    private fun formatDate(ms: Long): String =
        SimpleDateFormat("yyyy‑MM‑dd", Locale.getDefault()).format(Date(ms))

    companion object {
        private const val EXTRA_TX_ID = "tx_id"

        fun createIntent(ctx: Context, txId: Long? = null): Intent =
            Intent(ctx, AddEditTransactionActivity::class.java).apply {
                txId?.let { putExtra(EXTRA_TX_ID, it) }
            }
    }
}