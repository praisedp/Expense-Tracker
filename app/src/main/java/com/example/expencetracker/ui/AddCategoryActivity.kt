package com.example.expencetracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expencetracker.R
import com.example.expencetracker.data.Category
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.TxType

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var etCategoryName: EditText
    private lateinit var etCategoryEmoji: EditText
    private lateinit var rgCategoryType: RadioGroup
    private lateinit var rbCatIncome: RadioButton
    private lateinit var rbCatExpense: RadioButton
    private lateinit var btnSaveCategory: Button
    private lateinit var btnCancelCategory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        // 1) Bind views
        etCategoryName    = findViewById(R.id.etCategoryName)
        etCategoryEmoji   = findViewById(R.id.etCategoryEmoji)
        rgCategoryType    = findViewById(R.id.rgCategoryType)
        rbCatIncome       = findViewById(R.id.rbCatIncome)
        rbCatExpense      = findViewById(R.id.rbCatExpense)
        btnSaveCategory   = findViewById(R.id.btnSaveCategory)
        btnCancelCategory = findViewById(R.id.btnCancelCategory)

        // 2) Detect edit mode
        val isEdit       = intent.hasExtra("edit_category_name")
        val originalName = intent.getStringExtra("edit_category_name")
        val originalEmoji= intent.getStringExtra("edit_category_emoji")
        val originalType = intent.getStringExtra("edit_category_type")
        val defaultType  = intent.getStringExtra("default_category_type")

        if (isEdit && originalName != null && originalType != null) {
            // Pre-fill fields
            etCategoryName.setText(originalName)
            etCategoryEmoji.setText(originalEmoji ?: "")
            if (originalType == TxType.INCOME.name) {
                rbCatIncome.isChecked = true
            } else {
                rbCatExpense.isChecked = true
            }
            btnSaveCategory.text = getString(R.string.update_category)
        } else if (!isEdit && defaultType != null) {
            // For new categories, select the type based on the transaction screen's current type
            if (defaultType == TxType.INCOME.name) {
                rbCatIncome.isChecked = true
            } else {
                rbCatExpense.isChecked = true
            }
        }

        // 3) Save / Update logic
        btnSaveCategory.setOnClickListener {
            val name  = etCategoryName.text.toString().trim()
            val emoji = etCategoryEmoji.text.toString().trim()
            val type  = if (rgCategoryType.checkedRadioButtonId == R.id.rbCatIncome)
                TxType.INCOME else TxType.EXPENSE

            if (name.isEmpty() || emoji.isEmpty()) {
                Toast.makeText(this, "Please enter both name and emoji", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prevent adding a category with a duplicate name (ignore emoji/type)
            val existingCats = PrefsManager.loadCategories()
            if (!isEdit) {
                // On add: block if any existing name matches
                if (existingCats.any { it.name.equals(name, ignoreCase = true) }) {
                    Toast.makeText(this, "Category name already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                // On edit: block if renaming would collide with another category
                if (existingCats.any { it.name.equals(name, ignoreCase = true) && !name.equals(originalName, ignoreCase = true) }) {
                    Toast.makeText(this, "Category name already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Load existing
            val list = PrefsManager.loadCategories().toMutableList()

            if (isEdit && originalName != null) {
                // Replace existing entry
                val idx = list.indexOfFirst { it.name == originalName }
                if (idx >= 0) {
                    list[idx] = Category(name, type, emoji)
                } else {
                    list.add(Category(name, type, emoji))
                }
            } else {
                // Add new entry
                list.add(Category(name, type, emoji))
            }

            // ---- keep budgets & transactions in sync if the label changed ----
            if (isEdit && originalName != null) {
                val oldLabel = (originalEmoji?.let { "$it " } ?: "") + originalName
                val newLabel = "$emoji $name"
                if (oldLabel.trim() != newLabel.trim()) {
                    PrefsManager.renameCategory(oldLabel.trim(), newLabel.trim())
                }
            }

            PrefsManager.saveCategories(list)
            Toast.makeText(this,
                if (isEdit) "Category updated" else "Category added",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        // 4) Cancel button
        btnCancelCategory.setOnClickListener {
            finish()
        }
    }
}