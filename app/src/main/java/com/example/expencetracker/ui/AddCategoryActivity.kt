package com.example.expencetracker.ui

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
    private lateinit var rgCategoryType: RadioGroup
    private lateinit var rbCatIncome: RadioButton
    private lateinit var rbCatExpense: RadioButton
    private lateinit var btnSaveCategory: Button
    private lateinit var btnCancelCategory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        // Bind views
        etCategoryName = findViewById(R.id.etCategoryName)
        val etCategoryEmoji: EditText = findViewById(R.id.etCategoryEmoji)
        rgCategoryType = findViewById(R.id.rgCategoryType)
        rbCatIncome = findViewById(R.id.rbCatIncome)
        rbCatExpense = findViewById(R.id.rbCatExpense)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        btnCancelCategory = findViewById(R.id.btnCancelCategory)

        // Save button: Validate input and save the new category
        btnSaveCategory.setOnClickListener {
            val catName = etCategoryName.text.toString().trim()
            if (catName.isEmpty()) {
                etCategoryName.error = "Category name is required"
                return@setOnClickListener
            }

            // Get the emoji, default to an empty string if not provided
            val catEmoji = etCategoryEmoji.text.toString().trim()

            val catType = if (rgCategoryType.checkedRadioButtonId == R.id.rbCatIncome)
                TxType.INCOME else TxType.EXPENSE

            // Create a new Category including the emoji
            val newCategory = Category(name = catName, type = catType, emoji = catEmoji)

            // Retrieve existing categories, add the new one, and save them
            val currentCategories = PrefsManager.loadCategories().toMutableList()
            currentCategories.add(newCategory)
            PrefsManager.saveCategories(currentCategories)

            Toast.makeText(this, "Category Saved", Toast.LENGTH_SHORT).show()
            finish() // Close the activity
        }

        // Cancel button simply finishes the activity
        btnCancelCategory.setOnClickListener {
            finish()
        }
    }
}