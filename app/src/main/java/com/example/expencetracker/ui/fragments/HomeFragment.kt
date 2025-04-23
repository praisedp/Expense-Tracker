package com.example.expencetracker.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.R
import com.example.expencetracker.adapter.CategorySummaryAdapter
import com.example.expencetracker.data.CategoryRow          // ← data class you created
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.TxType
import com.example.expencetracker.databinding.FragmentHomeBinding
import com.example.expencetracker.util.DateUtils
import com.example.expencetracker.util.TransactionFilter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import com.example.expencetracker.ui.dialogs.MonthYearPickerDialog
import com.example.expencetracker.util.CurrencyFormatter
import java.util.Calendar
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

private enum class FilterMode { ALL, MONTH }

class HomeFragment : Fragment() {

    // ─── View Binding ─────────────────────────────────────────────
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ─── State ────────────────────────────────────────────────────
    private var filterMode = FilterMode.ALL
    private var monthStart = 0L
    private var monthEnd   = 0L
    private var isExpenseExpanded = false
    private var isIncomeExpanded = false
    private val MAX_VISIBLE_CATEGORIES = 3

    // ─── Category Lists ─────────────────────────────────────────────
    private var allExpenseRows: List<CategoryRow> = emptyList()
    private var allIncomeRows: List<CategoryRow> = emptyList()

    // ─── Adapters ────────────────────────────────────────────────
    private val expenseAdapter by lazy { CategorySummaryAdapter { openCategory(it) } }
    private val incomeAdapter  by lazy { CategorySummaryAdapter { openCategory(it) } }

    // ─── Lifecycle ───────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // RecyclerViews
        binding.rvExpenseCats.adapter = expenseAdapter
        binding.rvIncomeCats.adapter  = incomeAdapter

        setupFilterBar()
        refreshDashboard()
        setupChartDefaults(binding.pieExpense)
        setupChartDefaults(binding.pieIncome)
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()          // refresh after returning from Add/Edit
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ─── Filter Bar (All / Month) ────────────────────────────────
    private fun setupFilterBar() {
        val tl = binding.tabFilter
        tl.removeAllTabs()
        tl.addTab(tl.newTab().setText("All"))
        tl.addTab(tl.newTab().setText("Month"))

        tl.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { filterMode = FilterMode.ALL;   refreshDashboard() }
                    1 ->   showMonthPicker()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                if (tab.position == 1) showMonthPicker()
            }
        })
    }

    private fun showMonthPicker() {
        MonthYearPickerDialog(
            onMonthYear = { year, month0 ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month0)
                }
                monthStart = DateUtils.startOfMonth(cal.timeInMillis)
                monthEnd = DateUtils.endOfMonth(cal.timeInMillis)
                filterMode = FilterMode.MONTH
                refreshDashboard()
            },
            onCancel = {
                // Switch back to "All" tab when canceled
                binding.tabFilter.getTabAt(0)?.select()
                filterMode = FilterMode.ALL
                refreshDashboard()
            }
        ).show(parentFragmentManager, "myMonthPicker")
    }

    // ─── Dashboard calculation & binding ─────────────────────────
    private fun refreshDashboard() {
        val allTx = PrefsManager.loadTransactions()

        val list = when (filterMode) {
            FilterMode.ALL   -> allTx
            FilterMode.MONTH -> TransactionFilter.byDateRange(allTx, monthStart, monthEnd)
        }

        // Totals
        val totalIncome  = list.filter { it.type == TxType.INCOME  }.sumOf { it.amount }
        val totalExpense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        val balance      = totalIncome - totalExpense

        val moneyFmt = CurrencyFormatter.numberFormat()
        binding.tvBalance.text      = moneyFmt.format(balance)
        binding.tvTotalIncome.text  = "+ ${moneyFmt.format(totalIncome)}"
        binding.tvTotalExpense.text = "- ${moneyFmt.format(totalExpense)}"

        // Category splits
        val expByCat = list.filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
        val incByCat = list.filter { it.type == TxType.INCOME }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }

        val totalExpForPct = if (totalExpense == 0.0) 1.0 else totalExpense
        val totalIncForPct = if (totalIncome  == 0.0) 1.0 else totalIncome

        // Custom color palettes for expense and income
        val expensePalette = intArrayOf(
            resources.getColor(R.color.expense_color_1, null),
            resources.getColor(R.color.expense_color_2, null),
            resources.getColor(R.color.expense_color_3, null),
            resources.getColor(R.color.expense_color_4, null),
            resources.getColor(R.color.expense_color_5, null),
            resources.getColor(R.color.expense_color_6, null),
            resources.getColor(R.color.expense_color_7, null),
            resources.getColor(R.color.expense_color_8, null)
        )
        
        val incomePalette = intArrayOf(
            resources.getColor(R.color.income_color_1, null),
            resources.getColor(R.color.income_color_2, null),
            resources.getColor(R.color.income_color_3, null),
            resources.getColor(R.color.income_color_4, null),
            resources.getColor(R.color.income_color_5, null),
            resources.getColor(R.color.income_color_6, null),
            resources.getColor(R.color.income_color_7, null),
            resources.getColor(R.color.income_color_8, null)
        )

        // ─── Category splits (descending order) ─────────────────────────
        allExpenseRows = expByCat.entries
            .sortedByDescending { it.value }               // biggest spend first
            .mapIndexed { idx, e ->
                val color = expensePalette[idx % expensePalette.size]
                CategoryRow(
                    emojiOf(e.key),
                    e.key,
                    e.value,
                    e.value / totalExpForPct * 100,        // percent of total expense
                    color
                )
            }

        allIncomeRows = incByCat.entries
            .sortedByDescending { it.value }               // biggest income first
            .mapIndexed { idx, e ->
                val color = incomePalette[idx % incomePalette.size]
                CategoryRow(
                    emojiOf(e.key),
                    e.key,
                    e.value,
                    e.value / totalIncForPct * 100,        // percent of total income
                    color
                )
            }

        updateExpenseList()
        updateIncomeList()
        updateExpenseChart(allExpenseRows, totalExpense)
        updateIncomeChart(allIncomeRows, totalIncome)
    }

    // Update expense list with either top categories or all categories
    private fun updateExpenseList() {
        // Reset expansion state if we only have a few categories
        if (allExpenseRows.size <= MAX_VISIBLE_CATEGORIES) {
            isExpenseExpanded = false
            expenseAdapter.submitList(allExpenseRows)
            return
        }

        val showAllOrHideRow = CategoryRow(
            emoji = "",
            name = if (isExpenseExpanded) "expense_hide" else "expense_show_all",
            amount = 0.0,
            percent = 0.0,
            color = ColorTemplate.MATERIAL_COLORS[0]
        )

        val displayList = if (isExpenseExpanded) {
            // Show all categories + Hide button
            allExpenseRows + showAllOrHideRow
        } else {
            // Show top categories + Show All button
            allExpenseRows.take(MAX_VISIBLE_CATEGORIES) + showAllOrHideRow
        }
        
        expenseAdapter.submitList(displayList)
    }

    // Update income list with either top categories or all categories
    private fun updateIncomeList() {
        // Reset expansion state if we only have a few categories
        if (allIncomeRows.size <= MAX_VISIBLE_CATEGORIES) {
            isIncomeExpanded = false
            incomeAdapter.submitList(allIncomeRows)
            return
        }

        val showAllOrHideRow = CategoryRow(
            emoji = "",
            name = if (isIncomeExpanded) "income_hide" else "income_show_all",
            amount = 0.0,
            percent = 0.0,
            color = ColorTemplate.COLORFUL_COLORS[0]
        )

        val displayList = if (isIncomeExpanded) {
            // Show all categories + Hide button
            allIncomeRows + showAllOrHideRow
        } else {
            // Show top categories + Show All button
            allIncomeRows.take(MAX_VISIBLE_CATEGORIES) + showAllOrHideRow
        }
        
        incomeAdapter.submitList(displayList)
    }

    // Handle clicks on categories including the "Show All" option
    private fun openCategory(row: CategoryRow) {
        when (row.name) {
            "expense_show_all", "expense_hide" -> {
                isExpenseExpanded = !isExpenseExpanded
                updateExpenseList()
                return
            }
            "income_show_all", "income_hide" -> {
                isIncomeExpanded = !isIncomeExpanded
                updateIncomeList()
                return
            }
        }

        // Regular category click behavior
        val ctx = requireContext()
        val intent = Intent(ctx, com.example.expencetracker.ui.CategoryTransactionsActivity::class.java).apply {
            putExtra("label", row.name)
            putExtra("filter_mode", filterMode.name)
            if (filterMode == FilterMode.MONTH) {
                putExtra("from", monthStart)
                putExtra("to", monthEnd)
            }
        }
        startActivity(intent)
    }

    /** Finds the emoji for this category name (or ❓ fallback). */
    private fun emojiOf(cat: String): String =
        PrefsManager.loadCategories()
            .firstOrNull { it.name.equals(cat, ignoreCase = true) }
            ?.emoji ?: "❓"

    /** One-time styling for a PieChart so it looks like a doughnut. */
    private fun setupChartDefaults(chart: com.github.mikephil.charting.charts.PieChart) {
        chart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            legend.isEnabled      = false
            isRotationEnabled     = false
            holeRadius            = 60f       // already set in XML – here for clarity
            transparentCircleRadius = 0f
            setDrawEntryLabels(false)
            setCenterTextSize(14f)
        }
    }

    private fun updateExpenseChart(rows: List<CategoryRow>, total: Double) {
        val entries = rows.map { PieEntry(it.amount.toFloat(), it.emoji) }
        val colors = rows.map { it.color }.toMutableList()

        val ds = PieDataSet(entries, "Expenses").apply {
            setDrawValues(false)
            setColors(colors)
        }
        binding.pieExpense.data = PieData(ds)
        
        // Style the center text for expenses (red with minus sign)
        val spannableText = android.text.SpannableString("- ${CurrencyFormatter.format(total)}")
        spannableText.setSpan(
            android.text.style.ForegroundColorSpan(resources.getColor(R.color.colorExpenseDark, null)),
            0, spannableText.length, 
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableText.setSpan(
            android.text.style.RelativeSizeSpan(1.2f),
            0, spannableText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.pieExpense.centerText = spannableText
        binding.pieExpense.invalidate()
    }

    private fun updateIncomeChart(rows: List<CategoryRow>, total: Double) {
        val entries = rows.map { PieEntry(it.amount.toFloat(), it.emoji) }
        val colors = rows.map { it.color }.toMutableList()

        val ds = PieDataSet(entries, "Income").apply {
            setDrawValues(false)
            setColors(colors)
        }
        binding.pieIncome.data = PieData(ds)
        
        // Style the center text for income (green with plus sign)
        val spannableText = android.text.SpannableString("+ ${CurrencyFormatter.format(total)}")
        spannableText.setSpan(
            android.text.style.ForegroundColorSpan(resources.getColor(R.color.colorIncomeDark, null)),
            0, spannableText.length, 
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableText.setSpan(
            android.text.style.RelativeSizeSpan(1.2f),
            0, spannableText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.pieIncome.centerText = spannableText
        binding.pieIncome.invalidate()
    }
}

