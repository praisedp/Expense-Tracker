package com.example.expencetracker.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expencetracker.R
import com.example.expencetracker.data.TxType
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.adapter.TransactionAdapter
import com.example.expencetracker.data.PrefsManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.example.expencetracker.ui.AddEditTransactionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TransactionFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_transaction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.rvTransactions)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 1. Load transactions and categories
        allTransactions = PrefsManager.loadTransactions()
            .sortedByDescending { it.date }
        val allCategories = PrefsManager.loadCategories()

        // 2. Pass both into the adapter
        adapter = TransactionAdapter(allTransactions, allCategories) { tx ->
            showOptionsDialog(tx)
        }
        recyclerView.adapter = adapter

        // Attach swipe-to-delete on full left swipe
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
        val background = ColorDrawable(Color.parseColor("#f44336"))  // red

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                // 1. Capture the deleted transaction and its position
                val pos = vh.adapterPosition
                val deletedTx = allTransactions[pos]

                // 2. Remove from SharedPreferences and UI
                PrefsManager.deleteTransaction(deletedTx.id)
                adapter.updateData(PrefsManager.loadTransactions())

                // 3. Show Snackbar with UNDO
                Snackbar.make(recyclerView, "Transaction deleted", 3000)
                    .setAction("UNDO") {
                        // Restore in SharedPreferences
                        val restoredList = PrefsManager.loadTransactions().toMutableList()
                        restoredList.add(pos, deletedTx)
                        PrefsManager.saveTransactions(restoredList)
                        // Refresh UI
                        allTransactions = restoredList
                        adapter.updateData(allTransactions)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
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
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)

        // Find and configure the '+' FAB
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddTransaction)
        fabAdd.setOnClickListener {
            startActivity(
                AddEditTransactionActivity.createIntent(requireContext(), null /* no id for new */)
            )
        }

        setupTabs()
    }

    private fun filter(type: TxType?) {
        val filtered = if (type == null) {
            allTransactions
        } else {
            allTransactions.filter { it.type == type }
        }
        adapter.updateData(
            filtered.sortedByDescending { it.date }
        )
    }

    // Custom tab setup as provided by AI designer
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
        filter(tabLayout.selectedTabPosition.let { if (it == 1) TxType.INCOME else if (it == 2) TxType.EXPENSE else null })
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