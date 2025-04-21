package com.example.expencetracker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PrefsManager {

    private const val PREFS = "finance_prefs"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_BUDGET_TOTAL = "budget_total"
    private const val KEY_BUDGET_CATS  = "budget_categories"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // Initialize SharedPreferences – call this in your MainActivity or Application class.
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // Save a list of transactions as a JSON string.
    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        prefs.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    // Load transactions from SharedPreferences.
    fun loadTransactions(): List<Transaction> {
        val json = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type)
    }

    // Save a list of categories as a JSON string.
    fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CATEGORIES, json).apply()
    }

    // Load categories from SharedPreferences.
    fun loadCategories(): List<Category> {
        val json = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type)
    }
    // Delete a transaction by its id.
    fun deleteTransaction(id: Long): Boolean {
        val transactions = loadTransactions().toMutableList()
        // Remove transaction(s) with the given id.
        val removed = transactions.removeIf { it.id == id }
        if (removed) {
            saveTransactions(transactions)
        }
        return removed
    }

    // Delete a category by its name.
    fun deleteCategory(name: String): Boolean {
        val categories = loadCategories().toMutableList()
        // Remove category(s) where the name matches.
        val removed = categories.removeIf { it.name == name }
        if (removed) {
            saveCategories(categories)
        }
        return removed
    }

    private const val KEY_CURRENCY = "currency"

    // Save and load the current currency code
    fun setCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
    }

    fun getCurrency(): String {
        return prefs.getString(KEY_CURRENCY, "USD") ?: "USD" // Default to USD
    }

    // ───────── Budget (total) ───────────────────────────────────────────
    fun setTotalBudget(amount: Double) {
        prefs.edit().putFloat(KEY_BUDGET_TOTAL, amount.toFloat()).apply()
    }

    /** Returns saved monthly budget, or 0.0 if none set */
    fun getTotalBudget(): Double =
        prefs.getFloat(KEY_BUDGET_TOTAL, 0f).toDouble()

    // ───────── Budget (per‑category) ────────────────────────────────────
    fun saveCategoryBudgets(list: List<CategoryBudget>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_BUDGET_CATS, json).apply()
    }

    fun loadCategoryBudgets(): List<CategoryBudget> {
        val json = prefs.getString(KEY_BUDGET_CATS, null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<CategoryBudget>>() {}.type
        return gson.fromJson(json, type)
    }
}