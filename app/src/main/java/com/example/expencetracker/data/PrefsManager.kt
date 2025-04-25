package com.example.expencetracker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

object PrefsManager {

    private const val PREFS = "finance_prefs"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_BUDGET_TOTAL = "budget_total"
    private const val KEY_BUDGET_CATS  = "budget_categories"
    
    // PIN Lock related keys
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_SESSION_UNLOCKED = "session_unlocked"
    
    // Onboarding related keys
    private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    private const val KEY_FORCE_ONBOARDING = "force_onboarding"

    private const val KEY_DAILY_REMINDER = "daily_reminder_enabled"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    
    // Session flag to track if the user has unlocked this session
    private var isSessionUnlocked = false

    // Initialize SharedPreferences – call this in your MainActivity or Application class.
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Reset session unlock status on app initialization
        isSessionUnlocked = false
    }
    
    // ───────── Onboarding Methods ─────────────────────────────────────
    
    /**
     * Set whether the user has seen the onboarding screens
     * @param seen true if the user has seen the onboarding
     */
    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, seen).apply()
    }
    
    /**
     * Check if the user has seen the onboarding screens
     * @return true if the user has seen the onboarding
     */
    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    }
    
    /**
     * Set whether to force showing the onboarding screens
     * @param force true to force showing the onboarding
     */
    fun setForceOnboarding(force: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_ONBOARDING, force).apply()
    }
    
    /**
     * Check if the onboarding should be forcibly shown
     * @return true if the onboarding should be forcibly shown
     */
    fun shouldForceOnboarding(): Boolean {
        return prefs.getBoolean(KEY_FORCE_ONBOARDING, true)
    }
    
    /**
     * Check if the onboarding should be shown
     * @return true if the onboarding should be shown
     */
    fun shouldShowOnboarding(): Boolean {
        return shouldForceOnboarding() || !hasSeenOnboarding()
    }
    
    // ───────── PIN Lock Methods ────────────────────────────────────────
    
    /**
     * Set a new PIN for app lock
     * @param pin The 4-digit PIN to set
     * @return true if the PIN was successfully set
     */
    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return false
        }
        
        val pinHash = hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, pinHash)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        
        // Automatically mark session as unlocked when setting a new PIN
        isSessionUnlocked = true
        return true
    }
    
    /**
     * Check if the entered PIN matches the stored PIN
     * @param pin The PIN to verify
     * @return true if the PIN matches
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        val isCorrect = storedHash == inputHash
        
        if (isCorrect) {
            // Mark session as unlocked when PIN is verified
            isSessionUnlocked = true
        }
        
        return isCorrect
    }
    
    /**
     * Check if PIN lock is enabled
     * @return true if PIN lock is enabled
     */
    fun isPinEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, false)
    }
    
    /**
     * Clear the PIN and disable PIN lock
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }
    
    /**
     * Check if the current session is unlocked
     * @return true if the session is unlocked
     */
    fun isSessionUnlocked(): Boolean {
        return isSessionUnlocked
    }
    
    /**
     * Mark the current session as unlocked (after successful PIN entry)
     */
    fun markSessionUnlocked() {
        isSessionUnlocked = true
    }
    
    /**
     * Reset the session unlock status (e.g., when app is completely closed)
     */
    fun resetSessionUnlock() {
        isSessionUnlocked = false
    }
    
    /**
     * Hash the PIN using SHA-256
     * @param pin The PIN to hash
     * @return The hashed PIN
     */
    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
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

    /**
     * Remove all transactions belonging to the given category
     * so they no longer appear in your transaction lists.
     */
    fun setLastBackupTime(timestamp: Long) {
        prefs.edit()
            .putLong("last_backup_time", timestamp)
            .apply()
    }
    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }
    fun deleteTransactionsByCategory(categoryName: String) {
        // Remove all transactions where the stored category label **or** the plain
        // name (stripped of the leading emoji) matches the deleted category.
        val updated = loadTransactions()
            .filter { tx ->
                val raw = tx.category
                val plain = raw.substringAfter(" ", raw)   // drops the leading emoji
                !(raw == categoryName || plain == categoryName)
            }
        saveTransactions(updated)
    }

    // Delete a category by its name.
    fun deleteCategory(name: String): Boolean {
        val categories = loadCategories().toMutableList()
        // Remove category(s) where the name matches.
        val removed = categories.removeIf { it.name == name }
        if (removed) {
            saveCategories(categories)
            // -- ALSO remove any per‑category budgets that reference this category --
            val budgets = loadCategoryBudgets().toMutableList()
            val budgetRemoved = budgets.removeIf { it.category.endsWith(" $name") || it.category == name }
            if (budgetRemoved) {
                saveCategoryBudgets(budgets)
            }
        }
        return removed
    }

    /**
     * Rename a category everywhere it is referenced so old budget rows or
     * transactions don't linger and cause duplicates.
     *
     * @param oldLabel The old "emoji name" label stored in prefs (e.g. "🍔 Food")
     * @param newLabel The new "emoji name" label (e.g. "🥑 Food & Dining")
     */
    fun renameCategory(oldLabel: String, newLabel: String) {
        // ---- update per‑category budgets ----
        val budgets = loadCategoryBudgets().toMutableList()
        val idx = budgets.indexOfFirst { it.category == oldLabel }
        if (idx >= 0) {
            budgets[idx] = CategoryBudget(newLabel, budgets[idx].limit)
            saveCategoryBudgets(budgets)
        }

        // ---- update transactions so reports stay correct ----
        val txs = loadTransactions().toMutableList()
        var changed = false
        txs.forEach { tx ->
            if (tx.category == oldLabel) {
                tx.category = newLabel
                changed = true
            }
        }
        if (changed) saveTransactions(txs)
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

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REMINDER, enabled).apply()
    }
    
    fun isDailyReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_REMINDER, false)
    }
}