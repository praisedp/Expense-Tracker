package com.example.expencetracker.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.ui.AddCategoryActivity
import com.example.expencetracker.ui.CurrencySelectionActivity
import com.example.expencetracker.ui.LockActivity
import com.example.expencetracker.ui.SetPinActivity
import com.example.expencetracker.util.BackupManager
import com.example.expencetracker.util.CategoryChipHelper
import com.example.expencetracker.util.CurrencyConverter
import kotlinx.coroutines.launch
import com.example.expencetracker.data.TxType
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import com.google.android.material.switchmaterial.SwitchMaterial
import android.app.AlertDialog

class SettingsFragment : Fragment() {
    private val REQUEST_CURRENCY = 101

    private lateinit var openBackupPicker: ActivityResultLauncher<Intent>
    private lateinit var tvLastBackup: TextView
    private lateinit var switchPinLock: Switch
    private lateinit var btnChangePin: Button
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var btnInternalBackup: Button
    private lateinit var btnExport: Button
    private lateinit var btnRestore: Button
    private val prefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    
    private val setPinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // PIN was successfully set
            switchPinLock.isChecked = true
            btnChangePin.visibility = View.VISIBLE
            Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
        } else {
            // PIN setup was cancelled, reset the switch
            switchPinLock.isChecked = PrefsManager.isPinEnabled()
        }
        updatePinLockViews()
    }
    
    private val changePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "PIN changed successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val disablePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // PIN verified, now disable it
            PrefsManager.clearPin()
            switchPinLock.isChecked = false
            btnChangePin.visibility = View.GONE
            Toast.makeText(context, "PIN lock disabled", Toast.LENGTH_SHORT).show()
        } else {
            // PIN verification failed, reset the switch to checked
            switchPinLock.isChecked = true
        }
    }

    // Add a separate launcher for PIN verification before changing
    private val verifyForChangePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // PIN verified, now launch the change PIN screen
            val activity = requireActivity() as AppCompatActivity
            val intent = Intent(requireContext(), SetPinActivity::class.java).apply {
                putExtra("mode", SetPinActivity.MODE_NEW_PIN) // Just set a new PIN
            }
            startActivityForResult(intent, SetPinActivity.REQUEST_CODE_CHANGE_PIN)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize PIN lock views
        switchPinLock = view.findViewById(R.id.switchPinLock)
        btnChangePin = view.findViewById(R.id.btnChangePin)
        
        // Set initial state
        updatePinLockViews()
        
        // Set up PIN lock listeners
        switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Turn on PIN lock
                if (!PrefsManager.isPinEnabled()) {
                    // Launch PIN setup
                    val activity = requireActivity() as AppCompatActivity
                    SetPinActivity.startForNewPin(activity)
                    // Reset switch until PIN is confirmed (it will be updated in activity result)
                    switchPinLock.isChecked = false
                }
            } else {
                // Turn off PIN lock
                if (PrefsManager.isPinEnabled()) {
                    // Verify current PIN before disabling
                    val intent = Intent(requireContext(), LockActivity::class.java)
                    disablePinLauncher.launch(intent)
                    // Reset switch until PIN is verified (it will be updated in the launcher result)
                    switchPinLock.isChecked = true
                }
            }
        }
        
        btnChangePin.setOnClickListener {
            // Use the dedicated launcher for change PIN verification
            val intent = Intent(requireContext(), LockActivity::class.java)
            verifyForChangePinLauncher.launch(intent)
        }
        
        // Populate category chips via helper
        val incomes = PrefsManager.loadCategories()
            .filter { it.type == TxType.INCOME }
            .map { "${it.emoji} ${it.name}" }
        val expenses = PrefsManager.loadCategories()
            .filter { it.type == TxType.EXPENSE }
            .map { "${it.emoji} ${it.name}" }

        CategoryChipHelper.addCategoriesToChipGroup(
            requireContext(),
            view.findViewById(R.id.chipGroupIncome),
            incomes,
            CategoryChipHelper.ChipType.INCOME
        )
        CategoryChipHelper.addCategoriesToChipGroup(
            requireContext(),
            view.findViewById(R.id.chipGroupExpense),
            expenses,
            CategoryChipHelper.ChipType.EXPENSE
        )

        // 3. FAB to add a new category
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddCategory)
        fabAdd.setOnClickListener {
            // Launch AddCategoryActivity for a new category (no extras)
            startActivity(Intent(requireContext(), AddCategoryActivity::class.java))
        }

        // Register the file‐picker result handler
        openBackupPicker = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri: Uri ->
                    // Import backup on background thread
                    lifecycleScope.launch {
                        val success = BackupManager.importBackup(requireContext(), uri)
                        val msgRes = if (success) R.string.restore_success else R.string.restore_failure
                        Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Find both new buttons
        btnInternalBackup = view.findViewById(R.id.btnInternalBackup)
        btnExport = view.findViewById(R.id.btnExport)
        btnRestore = view.findViewById(R.id.btnRestore)

        // Button for changing currency
        val btnChangeCurrency: Button = view.findViewById(R.id.btnChangeCurrency)
        btnChangeCurrency.setOnClickListener {
            val intent = Intent(activity, CurrencySelectionActivity::class.java)
            startActivityForResult(intent, REQUEST_CURRENCY)
        }

        // Internal Backup
        btnInternalBackup.setOnClickListener {
            try {
                val path = BackupManager.createInternalBackup(requireContext())
                Toast.makeText(context, "Internal backup created successfully", Toast.LENGTH_SHORT).show()

                // Record backup timestamp and update UI
                val now = System.currentTimeMillis()
                PrefsManager.setLastBackupTime(now)
                val fmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                tvLastBackup.text = "Last backup: ${fmt.format(Date(now))}"

            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.backup_failure, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }

        // Export (Previously "Backup")
        btnExport.setOnClickListener {
            try {
                val path = BackupManager.exportToDownloads(requireContext())
                Toast.makeText(context, getString(R.string.backup_success, path), Toast.LENGTH_LONG).show()

                // Also record timestamp for consistency
                val now = System.currentTimeMillis()
                PrefsManager.setLastBackupTime(now)
                val fmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                tvLastBackup.text = "Last backup: ${fmt.format(Date(now))}"

            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.backup_failure, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }

        // Restore - updated to check for internal backup first
        btnRestore.setOnClickListener {
            // Check if we have an internal backup
            if (BackupManager.internalBackupExists(requireContext())) {
                // Show dialog asking whether to restore from internal or external
                val options = arrayOf("Internal Backup", "External File")
                AlertDialog.Builder(requireContext())
                    .setTitle("Restore From")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> restoreFromInternal()
                            1 -> restoreFromExternal()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // No internal backup, go straight to file picker
                restoreFromExternal()
            }
        }

        // --- Show current currency dynamically ---
        val tvCurrency = view.findViewById<TextView>(R.id.tvCurrentCurrency)
        val currCode = PrefsManager.getCurrency()
        val currSymbol = Currency.getInstance(currCode).symbol
        tvCurrency.text = "$currCode - $currSymbol"

        // --- Show last backup time ---
        tvLastBackup = view.findViewById<TextView>(R.id.tvLastBackup)
        val lastMillis = PrefsManager.getLastBackupTime()
        tvLastBackup.text = if (lastMillis > 0L) {
            val fmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            "Last backup: ${fmt.format(Date(lastMillis))}"
        } else {
            "Last backup: never"
        }

        // Initialize dark mode switch
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode_enabled", false)
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            
            // Save current fragment position before recreating
            val currentFragmentId = R.id.nav_settings
            val activity = requireActivity()
            
            // Apply night mode
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            
            // Recreate the activity but restore settings fragment
            activity.recreate()
        }

        return view
    }

    @Deprecated("Use Activity Result API instead")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle PIN change result
        if (requestCode == SetPinActivity.REQUEST_CODE_CHANGE_PIN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "PIN changed successfully", Toast.LENGTH_SHORT).show()
            }
            updatePinLockViews()
            return
        }
        
        // Handle PIN verification for disabling
        if (requestCode == LockActivity.REQUEST_CODE_UNLOCK) {
            if (resultCode == Activity.RESULT_OK) {
                // PIN verified - disable it
                PrefsManager.clearPin()
                switchPinLock.isChecked = false
                btnChangePin.visibility = View.GONE
                Toast.makeText(context, "PIN lock disabled", Toast.LENGTH_SHORT).show()
            } else {
                // PIN verification failed, reset the switch to checked
                switchPinLock.isChecked = true
            }
            updatePinLockViews()
            return
        }
        
        if (requestCode == REQUEST_CURRENCY && resultCode == Activity.RESULT_OK) {
            val newCurrency = data?.getStringExtra("selected_currency") ?: return
            val currentCurrency = PrefsManager.getCurrency()

            // Debug log the current and new currency
            Log.d("CurrencyDebug", "Current currency: $currentCurrency, New currency: $newCurrency")

            // Only update if the currency has changed.
            if (newCurrency != currentCurrency) {
                lifecycleScope.launch {
                    // Fetch conversion rate from current to new currency.
                    val conversionRate = CurrencyConverter.fetchConversionRate(currentCurrency, newCurrency)
                    // Debug log the conversion rate.
                    Log.d(
                        "CurrencyDebug",
                        "Conversion rate from $currentCurrency to $newCurrency: $conversionRate"
                    )

                    // Load existing transactions.
                    val transactions = PrefsManager.loadTransactions().toMutableList()
                    // For each transaction, update its amount using the conversion rate.
                    transactions.forEach { tx ->
                        val oldAmount = tx.amount
                        tx.amount = oldAmount * conversionRate
                        // Log the update for each transaction.
                        Log.d(
                            "CurrencyDebug",
                            "Updated Transaction ${tx.id}: $oldAmount -> ${tx.amount}"
                        )
                    }
                    // Save updated transactions.
                    PrefsManager.saveTransactions(transactions)
                    // Convert overall monthly budget
                    val oldTotal = PrefsManager.getTotalBudget()
                    if (oldTotal > 0.0) {
                        PrefsManager.setTotalBudget(oldTotal * conversionRate)
                    }

                    // Convert each per-category budget
                    val oldCatBudgets = PrefsManager.loadCategoryBudgets()
                    val newCatBudgets = oldCatBudgets.map { it.copy(limit = it.limit * conversionRate) }
                    PrefsManager.saveCategoryBudgets(newCatBudgets)
                    // Update stored currency.
                    PrefsManager.setCurrency(newCurrency)
                    Toast.makeText(context, "Currency updated to $newCurrency", Toast.LENGTH_SHORT).show()

                    // Refresh the displayed currency immediately
                    val tvCurrency = requireView().findViewById<TextView>(R.id.tvCurrentCurrency)
                    val updatedCode = PrefsManager.getCurrency()
                    val updatedSymbol = Currency.getInstance(updatedCode).symbol
                    tvCurrency.text = "$updatedCode - $updatedSymbol"
                }
            } else {
                Toast.makeText(context, "Currency remains unchanged", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /**
     * Show dialog with Edit / Delete options for a selected category.
     */
    private fun showCategoryOptionsDialog(category: com.example.expencetracker.data.Category) {
        val options = arrayOf("Edit", "Delete", "Cancel")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Category: ${category.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> editCategory(category)
                    1 -> deleteCategory(category)
                    // Cancel does nothing
                }
            }
            .show()
    }

    /**
     * Launch AddCategoryActivity in "edit" mode, passing the category name and emoji.
     */
    private fun editCategory(category: com.example.expencetracker.data.Category) {
        val intent = Intent(requireContext(), AddCategoryActivity::class.java).apply {
            putExtra("edit_category_name", category.name)
            putExtra("edit_category_type", category.type.name)
            putExtra("edit_category_emoji", category.emoji)
        }
        startActivity(intent)
    }

    /**
     * Delete the given category and refresh the UI.
     */
    private fun deleteCategory(category: com.example.expencetracker.data.Category) {
        // Remove category from storage
        if (PrefsManager.deleteCategory(category.name)) {
            // Remove any transactions that stored either "🍔 Food" or just "Food"
            PrefsManager.deleteTransactionsByCategory("${category.emoji} ${category.name}")

            Toast.makeText(requireContext(), "${category.name} deleted", Toast.LENGTH_SHORT).show()

            // Refresh chips after deletion
            val categories = PrefsManager.loadCategories()
            val chipIncome  = requireView().findViewById<ChipGroup>(R.id.chipGroupIncome)
            val chipExpense = requireView().findViewById<ChipGroup>(R.id.chipGroupExpense)

            chipIncome.removeAllViews()
            chipExpense.removeAllViews()

            categories.forEach { cat ->
                val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = "${cat.emoji} ${cat.name}"
                    isClickable = true
                    isCheckable = false
                    setOnLongClickListener {
                        showCategoryOptionsDialog(cat)
                        true
                    }
                }
                if (cat.type == TxType.INCOME) {
                    chipIncome.addView(chip)
                } else {
                    chipExpense.addView(chip)
                }
            }
        } else {
            Toast.makeText(requireContext(), "Could not delete ${category.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh chips on resume
        val categories = PrefsManager.loadCategories()
        val chipIncome  = requireView().findViewById<ChipGroup>(R.id.chipGroupIncome)
        val chipExpense = requireView().findViewById<ChipGroup>(R.id.chipGroupExpense)

        chipIncome.removeAllViews()
        chipExpense.removeAllViews()

        categories.forEach { category ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                id = View.generateViewId()
                text = "${category.emoji} ${category.name}"
                isClickable = true
                isCheckable = false
                setOnLongClickListener {
                    showCategoryOptionsDialog(category)
                    true
                }
            }
            if (category.type == TxType.INCOME) {
                chipIncome.addView(chip)
            } else {
                chipExpense.addView(chip)
            }
        }

        // Update PIN lock state in case it was changed elsewhere
        updatePinLockViews()
    }

    private fun updatePinLockViews() {
        val isPinEnabled = PrefsManager.isPinEnabled()
        switchPinLock.isChecked = isPinEnabled
        btnChangePin.visibility = if (isPinEnabled) View.VISIBLE else View.GONE
    }

    // Add helper methods for restore operations
    private fun restoreFromInternal() {
        lifecycleScope.launch {
            // Show loading indicator or disable UI if needed
            val success = BackupManager.restoreInternalBackup(requireContext())
            val message = if (success) "Internal backup restored successfully" 
                          else "Failed to restore internal backup"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun restoreFromExternal() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        openBackupPicker.launch(intent)
    }
}