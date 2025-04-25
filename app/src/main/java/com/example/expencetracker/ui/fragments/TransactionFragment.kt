package com.example.expencetracker.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import com.example.expencetracker.ui.AddEditTransactionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

class TransactionFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var searchView: SearchView
    private var allTransactions: List<Transaction> = emptyList()
    private var currentFilterType: TxType? = null
    private var currentSearchQuery: String = ""
    private var searchJob: Job? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.rvTransactions)
        searchView = view.findViewById(R.id.svSearch)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Setup focus management with the main layout
        val mainLayout = view.findViewById<View>(R.id.transactionMainLayout)
        mainLayout.setOnClickListener {
            clearSearchViewFocus()
        }
        
        // Setup touch listener on recyclerView to clear focus
        recyclerView.setOnTouchListener { _, _ ->
            if (searchView.hasFocus()) {
                clearSearchViewFocus()
            }
            false
        }

        // 1. Load transactions and categories
        allTransactions = PrefsManager.loadTransactions()
            .sortedByDescending { it.date }
        val allCategories = PrefsManager.loadCategories()

        // 2. Pass both into the adapter
        adapter = TransactionAdapter(allTransactions, allCategories) { tx ->
            showOptionsDialog(tx)
        }
        recyclerView.adapter = adapter

        // Set up search view
        setupSearchView()

        // Set up tabs
        setupTabs()

        // Attach swipe-to-delete on full left swipe
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
        val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#f44336"))  // red

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                try {
                    // 1. Capture the deleted transaction and its position
                    val pos = vh.adapterPosition
                    val currentDisplayedList = adapter.getCurrentList()
                    
                    // Check if position is valid
                    if (pos < 0 || pos >= currentDisplayedList.size) {
                        // Invalid position, reload the list to reset the UI
                        applyFilters()
                        return
                    }
                    
                    val deletedTx = currentDisplayedList[pos]
    
                    // 2. Remove from SharedPreferences
                    if (PrefsManager.deleteTransaction(deletedTx.id)) {
                        // 3. Reload all transactions from SharedPreferences
                        allTransactions = PrefsManager.loadTransactions()
                            .sortedByDescending { it.date }
                        
                        // 4. Re-apply the current filters to update the UI
                        applyFilters()
        
                        // 5. Show Snackbar with UNDO
                        Snackbar.make(recyclerView, "Transaction deleted", 3000)
                            .setAction("UNDO") {
                                // Restore in SharedPreferences
                                val restoredList = PrefsManager.loadTransactions().toMutableList()
                                restoredList.add(deletedTx)
                                PrefsManager.saveTransactions(restoredList)
                                
                                // Reload transactions and re-apply filters
                                allTransactions = PrefsManager.loadTransactions()
                                    .sortedByDescending { it.date }
                                applyFilters()
                            }
                            .show()
                    } else {
                        // If deletion failed, just refresh the UI
                        applyFilters()
                    }
                } catch (e: Exception) {
                    // If any exception occurs, log it and refresh the UI
                    android.util.Log.e("TransactionFragment", "Error in swipe deletion: ${e.message}")
                    applyFilters()
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                val itemView = vh.itemView
                // Draw red background from right to swipe distance
                val dx = dX.toInt()
                background.setBounds(
                    itemView.right + dx,
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)
                // Draw delete icon centered vertically on the right
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - deleteIcon.intrinsicWidth
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + deleteIcon.intrinsicHeight
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon.draw(c)

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Set up FAB
        view.findViewById<FloatingActionButton>(R.id.fabAddTransaction).setOnClickListener {
            startActivity(AddEditTransactionActivity.createIntent(requireContext()))
        }
    }

    private fun setupSearchView() {
        // Set query hint programmatically
        searchView.queryHint = "Search by name, category, date, amount..."
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                clearSearchViewFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Cancel previous search job if it exists
                searchJob?.cancel()
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Debounce search to avoid excessive filtering
                searchRunnable = Runnable {
                    newText?.let { 
                        lifecycleScope.launch {
                            performSearch(it) 
                        }
                    }
                }
                
                searchHandler.postDelayed(searchRunnable!!, 300)
                return true
            }
        })
    }

    /**
     * Helper method to clear focus from search view and hide keyboard
     */
    private fun clearSearchViewFocus() {
        if (searchView.hasFocus()) {
            searchView.clearFocus()
            val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun performSearch(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    private fun setupTabs() {
        // Clear existing tabs
        tabLayout.removeAllTabs()

        // Add tabs with custom designs
        val allTab = tabLayout.newTab().setText("All")
        val incomeTab = tabLayout.newTab().setText("Income")
        val expensesTab = tabLayout.newTab().setText("Expenses")

        tabLayout.addTab(allTab)
        tabLayout.addTab(incomeTab)
        tabLayout.addTab(expensesTab)

        // Make tabs fill the layout width evenly
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout.tabMode = TabLayout.MODE_FIXED

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> filter(null)
                    1 -> filter(TxType.INCOME)
                    2 -> filter(TxType.EXPENSE)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        allTransactions = PrefsManager.loadTransactions()
            .sortedByDescending { it.date }
        applyFilters()
    }
    
    private fun filter(type: TxType?) {
        currentFilterType = type
        applyFilters()
    }
    
    private fun applyFilters() {
        val query = currentSearchQuery
        val type = currentFilterType
        
        // If both query and type filter are empty, show all transactions
        if (query.isEmpty() && type == null) {
            adapter.updateTransactions(allTransactions)
            return
        }
        
        lifecycleScope.launch(Dispatchers.Default) {
            val filteredTransactions = if (query.isEmpty()) {
                // Only filter by type
                allTransactions.filter { type == null || it.type == type }
            } else {
                // Apply smart search with type filter
                filterTransactions(query).filter { type == null || it.type == type }
            }
            
            withContext(Dispatchers.Main) {
                adapter.updateTransactions(filteredTransactions)
            }
        }
    }
    
    /**
     * Smart search function that filters transactions by various criteria
     */
    private fun filterTransactions(query: String): List<Transaction> {
        if (query.isBlank()) return allTransactions

        // Split on whitespace to allow multi-term search (month + category, etc.)
        val tokens = query.lowercase().trim().split("\\s+".toRegex())

        return allTransactions.filter { tx ->
            tokens.all { token -> matchToken(tx, token) }
        }
    }

    /**
     * Returns true if a single search token matches any field of the transaction.
     */
    private fun matchToken(tx: Transaction, token: String): Boolean {
        // 1. Title match
        val title = tx.title.lowercase()
        if (title.contains(token) || isFuzzyMatch(title, token)) return true

        // 2. Category match
        val cat = tx.category.lowercase()
        if (cat.contains(token) || isFuzzyMatch(cat, token)) return true

        // 3. Amount match
        val amtToken = token.replace(",", ".")
        val txAmt = abs(tx.amount)
        amtToken.toDoubleOrNull()?.let { qAmt ->
            if (abs(txAmt - qAmt) < 0.01) return true
        }
        val fmtAmt = String.format(Locale.getDefault(), "%.2f", txAmt)
        if (fmtAmt.contains(amtToken) || fmtAmt.replace(".", ",").contains(token)) return true

        // 4. Date match: full ISO, month name, day, or month+day
        val date = Date(tx.date)
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date).lowercase()
        if (iso.contains(token)) return true

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(date).lowercase()
        if (monthName.startsWith(token) || token.startsWith(monthName)) return true

        val monthDay = SimpleDateFormat("MMMM d", Locale.getDefault()).format(date).lowercase()
        if (monthDay.contains(token)) return true

        val day = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH).toString()
        if (token == day || token.endsWith(" $day") || token.startsWith("$day ")) return true

        // No match
        return false
    }
    
    /**
     * Basic Levenshtein distance implementation for fuzzy matching
     */
    private fun isFuzzyMatch(text: String, query: String): Boolean {
        // For very short queries, require exact match to avoid false positives
        if (query.length <= 2) {
            return false
        }
        
        // Calculate Levenshtein distance
        val distance = calculateLevenshteinDistance(text, query)
        
        // Allow more typos for longer queries
        val maxAllowedDistance = when {
            query.length <= 3 -> 1
            query.length <= 5 -> 2
            else -> 3
        }
        
        return distance <= maxAllowedDistance
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        // Basic implementation to handle approximate string matching
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        // Create two work vectors
        var v0 = IntArray(s2.length + 1) { it }
        var v1 = IntArray(s2.length + 1)

        for (i in s1.indices) {
            // Initial value for edit distance from s2[0..j] to s1[0..i]
            v1[0] = i + 1

            for (j in s2.indices) {
                // Calculate cost - 0 if characters match, 1 otherwise
                val cost = if (s1[i] == s2[j]) 0 else 1

                // Compute minimum of delete, insert, substitute
                v1[j + 1] = minOf(
                    v1[j] + 1,          // Delete
                    v0[j + 1] + 1,      // Insert
                    v0[j] + cost        // Substitute
                )
            }

            // Swap arrays for next iteration
            val temp = v0
            v0 = v1
            v1 = temp
        }

        return v0[s2.length]
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                if (PrefsManager.deleteTransaction(transaction.id)) {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions()
                } else {
                    Toast.makeText(context, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    // ─── Modern options dialog for long-press (without Material Components) ───────────
    private fun showOptionsDialog(tx: Transaction) {
        // Create a custom dialog
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_transaction_options)

        // Set dialog window attributes for better appearance
        dialog.window?.apply {
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_rounded_dialog))
            // Calculate 85% of screen width
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            // Add animations
            setWindowAnimations(R.style.DialogAnimation)
        }

        // Set up the buttons with click listeners
        dialog.findViewById<TextView>(R.id.tv_edit).setOnClickListener {
            editTransaction(tx)
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.tv_delete).setOnClickListener {
            showDeleteTransactionDialog(tx)
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun editTransaction(tx: Transaction) {
        val intent = AddEditTransactionActivity.createIntent(requireContext(), tx.id)
        startActivity(intent)          // you can also use startActivityForResult if you want a callback
    }
}