package com.example.expencetracker.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.expencetracker.R
import com.example.expencetracker.data.Category
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import com.example.expencetracker.util.BudgetCalculator
import com.example.expencetracker.util.CurrencyFormatter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AnalyticsFragment : Fragment() {

    // ─── Budget Progress Views ───────────────────────────────────
    private lateinit var tvTotalStatus: TextView
    private lateinit var progressTotal: ProgressBar

    // ─── Charts ────────────────────────────────────────────────
    private lateinit var cashFlowChart: LineChart
    private lateinit var incomeVsExpenseChart: BarChart
    private lateinit var toggleTimeFrame: MaterialButtonToggleGroup
    private lateinit var btnMonth: MaterialButton
    private lateinit var btnWeek: MaterialButton
    
    // ─── Month-over-Month Change Views ─────────────────────────
    private lateinit var tvIncomeValue: TextView
    private lateinit var tvIncomeChange: TextView
    private lateinit var tvExpenseValue: TextView
    private lateinit var tvExpenseChange: TextView
    
    // ─── Category Benchmark Views ─────────────────────────────
    private lateinit var spinnerPrimaryCategory: Spinner
    private lateinit var spinnerSecondaryCategory: Spinner
    private lateinit var tvPrimaryCategoryName: TextView
    private lateinit var tvSecondaryCategoryName: TextView
    private lateinit var tvPrimaryCategoryAmount: TextView
    private lateinit var tvSecondaryCategoryAmount: TextView
    private lateinit var progressPrimaryCategory: ProgressBar
    private lateinit var progressSecondaryCategory: ProgressBar
    private lateinit var tvCategoryComparison: TextView
    private lateinit var tvCategoryDifference: TextView
    
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val weekFormat = SimpleDateFormat("'W'w", Locale.getDefault())
    
    // Store category selections to avoid frequent reloading
    private var primaryCategoryIndex = 0
    private var secondaryCategoryIndex = 1
    private var availableCategories: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Budget progress views
        tvTotalStatus = view.findViewById(R.id.tvTotalStatus)
        progressTotal = view.findViewById(R.id.progressTotalBudget)

        // Cash flow chart
        cashFlowChart = view.findViewById(R.id.chartCashFlow)
        setupCashFlowChart()
        
        // Income vs Expense chart
        incomeVsExpenseChart = view.findViewById(R.id.chartIncomeVsExpense)
        toggleTimeFrame = view.findViewById(R.id.toggleTimeFrame)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnWeek = view.findViewById(R.id.btnWeek)
        
        setupIncomeVsExpenseChart()
        setupTimeFrameToggle()
        
        // Initialize Month-over-Month Views
        tvIncomeValue = view.findViewById(R.id.tvIncomeValue)
        tvIncomeChange = view.findViewById(R.id.tvIncomeChange)
        tvExpenseValue = view.findViewById(R.id.tvExpenseValue)
        tvExpenseChange = view.findViewById(R.id.tvExpenseChange)
        
        // Initialize Category Benchmark Views
        spinnerPrimaryCategory = view.findViewById(R.id.spinnerPrimaryCategory)
        spinnerSecondaryCategory = view.findViewById(R.id.spinnerSecondaryCategory)
        tvPrimaryCategoryName = view.findViewById(R.id.tvPrimaryCategoryName)
        tvSecondaryCategoryName = view.findViewById(R.id.tvSecondaryCategoryName)
        tvPrimaryCategoryAmount = view.findViewById(R.id.tvPrimaryCategoryAmount)
        tvSecondaryCategoryAmount = view.findViewById(R.id.tvSecondaryCategoryAmount)
        progressPrimaryCategory = view.findViewById(R.id.progressPrimaryCategory)
        progressSecondaryCategory = view.findViewById(R.id.progressSecondaryCategory)
        tvCategoryComparison = view.findViewById(R.id.tvCategoryComparison)
        tvCategoryDifference = view.findViewById(R.id.tvCategoryDifference)
        
        setupCategorySpinners()

        // Update data
        refreshAnalytics()
    }
    
    private fun setupCategorySpinners() {
        // Get expense categories only (we'll focus on expense comparisons)
        val categories = PrefsManager.loadCategories()
            .filter { it.type == TxType.EXPENSE }
            .map { "${it.emoji} ${it.name}" }
            .toMutableList()
        
        // If we have less than 2 categories, add defaults
        if (categories.size < 2) {
            if (categories.isEmpty()) {
                categories.add("🍔 Dining Out")
            }
            categories.add("🥬 Groceries")
        }
        
        availableCategories = categories
        
        // Setup adapters for both spinners
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        spinnerPrimaryCategory.adapter = adapter
        spinnerSecondaryCategory.adapter = adapter
        
        // Set initial selections to different categories if possible
        spinnerPrimaryCategory.setSelection(0)
        spinnerSecondaryCategory.setSelection(minOf(1, categories.size - 1))
        
        // Setup listeners
        spinnerPrimaryCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                primaryCategoryIndex = position
                updateCategoryComparison()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerSecondaryCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                secondaryCategoryIndex = position
                updateCategoryComparison()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupTimeFrameToggle() {
        // Default to month view
        btnMonth.isChecked = true
        
        toggleTimeFrame.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // Refresh chart data based on selection
                val allTx = PrefsManager.loadTransactions()
                if (checkedId == R.id.btnMonth) {
                    updateIncomeVsExpenseChartMonthly(allTx)
                } else {
                    updateIncomeVsExpenseChartWeekly(allTx)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAnalytics()
    }

    // ─── Analytics refresh ───────────────────────────────────────
    private fun refreshAnalytics() {
        // Update budget progress
        updateBudgetProgress()
        
        // Load all transactions
        val allTx = PrefsManager.loadTransactions()
        
        // Update cash flow chart
        updateCashFlowChart(allTx)
        
        // Update income vs expense chart based on selected time frame
        if (::toggleTimeFrame.isInitialized) {
            if (toggleTimeFrame.checkedButtonId == R.id.btnMonth) {
                updateIncomeVsExpenseChartMonthly(allTx)
            } else {
                updateIncomeVsExpenseChartWeekly(allTx)
            }
        }
        
        // Update month-over-month comparisons
        updateMonthOverMonthChange(allTx)
        
        // Update category benchmarks
        updateCategoryComparison()
    }

    // ─── Budget Progress Update ───────────────────────────────────
    private fun updateBudgetProgress() {
        val totalLimit = PrefsManager.getTotalBudget()
        val monthTx = BudgetCalculator.transactionsThisMonth(PrefsManager.loadTransactions())
        val spent = BudgetCalculator.spentTotal(monthTx)

        // total progress
        if (totalLimit <= 0.0) {
            tvTotalStatus.text = getString(R.string.no_budget_set)
            progressTotal.progress = 0
            progressTotal.progressTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.colorIncome)
        } else {
            val pct = (spent / totalLimit).coerceIn(0.0, 1.0)
            progressTotal.progress = (pct * 100).toInt()
            progressTotal.progressTintList =
                ContextCompat.getColorStateList(requireContext(), when {
                    pct < 0.75 -> R.color.colorIncome
                    pct <= 1   -> R.color.colorWarning
                    else       -> R.color.colorExpense
                })
            tvTotalStatus.text = getString(
                R.string.budget_status_fmt,
                CurrencyFormatter.format(spent),
                CurrencyFormatter.format(totalLimit)
            )
        }
    }

    // ─── Cash Flow Chart Setup and Update ───────────────────────
    private fun setupCashFlowChart() {
        cashFlowChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            
            // Disable right axis
            axisRight.isEnabled = false
            
            // Style left axis (amounts)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#20000000") // Transparent grid
                textColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
            }
            
            // Style X axis (dates)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Convert float index to date
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
                // Set label count to avoid too many labels
                setLabelCount(3, true)
                granularity = 24 * 60 * 60 * 1000f // One day in milliseconds
            }
            
            // Enable touch gestures
            setTouchEnabled(true)
            isDragEnabled = true  // Enable scrolling
            setScaleEnabled(false) // Disable scaling/zooming
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            isHighlightPerDragEnabled = true
            
            // Animate on data change
            animateX(1000)
            
            extraBottomOffset = 10f // Extra space at bottom
            
            // No auto scaling
            isAutoScaleMinMaxEnabled = false
        }
    }
    
    private fun updateCashFlowChart(allTransactions: List<Transaction>) {
        // Get end date (today) and calculate start date (12 weeks ago instead of 6)
        val endCal = Calendar.getInstance()
        val endDate = endCal.timeInMillis
        
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.WEEK_OF_YEAR, -12) // Go back 12 weeks for more historical data
        val startDate = startCal.timeInMillis
        
        // Filter transactions within the date range
        val relevantTransactions = allTransactions.filter { 
            it.date in startDate..endDate 
        }
        
        // Group transactions by week and calculate net balance for each week
        val weeklyBalances = mutableListOf<Pair<Long, Double>>()
        
        // Generate all 12 weeks to ensure we have entries even for weeks without transactions
        for (i in 0 until 12) {
            val weekCal = Calendar.getInstance()
            weekCal.add(Calendar.WEEK_OF_YEAR, -11 + i) // Start from 11 weeks ago to current week
            
            // Properly set to beginning of week (Sunday)
            weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)
            val weekStart = weekCal.timeInMillis
            
            // Move to end of week (Saturday)
            weekCal.add(Calendar.DAY_OF_YEAR, 6)
            val weekEnd = weekCal.timeInMillis
            
            // Filter transactions for this week
            val weekTransactions = relevantTransactions.filter {
                it.date in weekStart..weekEnd
            }
            
            // Calculate net balance for this week (income - expense)
            val income = weekTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val expense = weekTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            val netBalance = income - expense
            
            // Use middle of week as the x-axis position for better visualization
            val middleOfWeek = weekStart + (weekEnd - weekStart) / 2
            weeklyBalances.add(Pair(middleOfWeek, netBalance))
        }
        
        // Create entries for the chart
        val entries = weeklyBalances.map { (date, balance) ->
            Entry(date.toFloat(), balance.toFloat())
        }.sortedBy { it.x } // Ensure sorted by date
        
        if (entries.isEmpty()) {
            cashFlowChart.clear()
            return
        }
        
        // Create a dataset from the entries
        val lineDataSet = LineDataSet(entries, "Cash Flow").apply {
            // Style the line
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            lineWidth = 2f
            
            // Style the points
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            circleRadius = 4f
            
            // Add value labels
            setDrawValues(false)
            
            // Enable highlighting
            setDrawHighlightIndicators(true)
            highLightColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            
            // Fill area below line
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
            fillAlpha = 50 // Semi-transparent
            
            // Mode determines how the line connects points
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curved line
        }
        
        // Set the data to the chart
        cashFlowChart.data = LineData(lineDataSet)
        
        // Refresh the chart
        cashFlowChart.invalidate()
        
        // CRITICAL: Apply the one week visible range AFTER setting data
        // Calculate one week in milliseconds (7 * 24 * 60 * 60 * 1000)
        val oneWeekMs = 7 * 24 * 60 * 60 * 1000f
        
        // Set exact viewport limits
        cashFlowChart.setVisibleXRangeMaximum(oneWeekMs)
        cashFlowChart.setVisibleXRangeMinimum(oneWeekMs)
        
        // Move viewport to show most recent data
        cashFlowChart.moveViewToX(endDate.toFloat())
    }
    
    // ─── Income vs Expense Chart Setup and Update ───────────────────────
    private fun setupIncomeVsExpenseChart() {
        incomeVsExpenseChart.apply {
            description.isEnabled = false
            
            // Enable legend to show income vs expense
            legend.apply {
                isEnabled = true
                textColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
                textSize = 12f
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 20f // Add more space between legend entries
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE // Use circle form
                formSize = 12f // Larger legend indicator
            }
            
            // Disable right axis
            axisRight.isEnabled = false
            
            // Style left axis (amounts)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#15000000") // Very light grid
                axisLineWidth = 1.5f // Slightly thicker line
                gridLineWidth = 0.8f // Thinner grid lines
                textColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
                axisMinimum = 0f // Start at 0
                spaceTop = 15f // Add space on top for visual comfort
            }
            
            // Style X axis (months/weeks)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
                granularity = 1f
                labelCount = 6
                textSize = 10f // Smaller text size
                valueFormatter = IndexAxisValueFormatter() // Will set labels in data update
                setDrawAxisLine(true)
                axisLineWidth = 1.5f // Slightly thicker x-axis line
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
                yOffset = 10f // Add some padding below labels
            }
            
            // Other settings
            setFitBars(true)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = true
            animateY(1200) // Slightly longer animation
            extraBottomOffset = 10f // Extra space at bottom
            extraTopOffset = 8f // Extra space at top
            setNoDataText("No data available")
            
            // Use Currency formatter for Y-axis values
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return CurrencyFormatter.formatSimple(value.toDouble())
                }
            }
            
            // Enhance appearance
            setBorderWidth(0.5f)
            setBorderColor(Color.parseColor("#20000000"))
            
            // Improve performance
            isDoubleTapToZoomEnabled = false
            setExtraOffsets(10f, 10f, 10f, 10f) // Add padding all around
        }
    }
    
    private fun updateIncomeVsExpenseChartMonthly(allTransactions: List<Transaction>) {
        // Print debug info
        android.util.Log.d("AnalyticsFragment", "Updating monthly chart with ${allTransactions.size} transactions")
        
        // Get data for the last 6 months
        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.MONTH, -5) // Go back 5 months (for 6 months total)
        
        // Set both calendars to the first day of their respective months
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        
        val monthlyData = mutableListOf<Triple<String, Double, Double>>() // Month label, Income, Expense
        val xLabels = mutableListOf<String>()
        
        // Process each month
        val currentCal = startCal.clone() as Calendar
        while (currentCal <= endCal) {
            val monthStart = currentCal.timeInMillis
            
            // Move to end of month
            currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            currentCal.set(Calendar.HOUR_OF_DAY, 23)
            currentCal.set(Calendar.MINUTE, 59)
            currentCal.set(Calendar.SECOND, 59)
            val monthEnd = currentCal.timeInMillis
            
            // Filter transactions for this month
            val monthTransactions = allTransactions.filter {
                it.date in monthStart..monthEnd
            }
            
            // Calculate income and expense totals
            val income = monthTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            
            // Format month label
            val monthLabel = monthFormat.format(Date(monthStart))
            xLabels.add(monthLabel)
            
            // Add to data
            monthlyData.add(Triple(monthLabel, income, expense))
            
            // Print debug info for this month
            android.util.Log.d("AnalyticsFragment", "Month: $monthLabel, Income: $income, Expense: $expense")
            
            // Move to first day of next month
            currentCal.add(Calendar.MONTH, 1)
            currentCal.set(Calendar.DAY_OF_MONTH, 1)
            currentCal.set(Calendar.HOUR_OF_DAY, 0)
            currentCal.set(Calendar.MINUTE, 0)
            currentCal.set(Calendar.SECOND, 0)
        }
        
        // Create dummy data if no real data exists
        if (monthlyData.isEmpty() || monthlyData.all { it.second == 0.0 && it.third == 0.0 }) {
            android.util.Log.d("AnalyticsFragment", "No data found - adding dummy data")
            // Show message
            Toast.makeText(requireContext(), "No transaction data found. Adding sample data.", Toast.LENGTH_SHORT).show()
            
            // Add dummy data for testing
            xLabels.clear()
            monthlyData.clear()
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
            for (i in 0 until 6) {
                xLabels.add(months[i])
                monthlyData.add(Triple(months[i], (1000 + i * 500).toDouble(), (800 + i * 300).toDouble()))
            }
        }
        
        // Create entries for income and expense
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        
        for (i in monthlyData.indices) {
            val (_, income, expense) = monthlyData[i]
            incomeEntries.add(BarEntry(i.toFloat(), income.toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), expense.toFloat()))
        }
        
        // Log entries for debugging
        android.util.Log.d("AnalyticsFragment", "Income entries: ${incomeEntries.size}, Expense entries: ${expenseEntries.size}")
        
        // Create dataset for income in monthly view
        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            // Use a nice light green color for income
            color = Color.parseColor("#2ECC71") // Light green
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
            valueTextSize = 10f
            
            // Enable values on bars for better visibility of data
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 100) return ""  // Don't draw very small values
                    return CurrencyFormatter.formatSimple(value.toDouble())
                }
            }
            
            // Set up single color instead of gradient for better visibility
            highLightAlpha = 90
            setDrawIcons(false)
        }
        
        // Create dataset for expense in monthly view
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            // Use a more vibrant orange/red color for expense
            color = Color.parseColor("#E74C3C") // Bright orange-red
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
            valueTextSize = 10f
            
            // Enable values on bars for better visibility of data
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 100) return ""  // Don't draw very small values
                    return CurrencyFormatter.formatSimple(value.toDouble())
                }
            }
            
            // Set up single color instead of gradient for better visibility
            highLightAlpha = 90
            setDrawIcons(false)
        }
        
        // Group datasets for grouped bar chart
        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(incomeDataSet)
        dataSets.add(expenseDataSet)
        
        // Create and set the data
        val data = BarData(dataSets)
        data.barWidth = 0.35f // Adjust width to avoid overlap
        
        // Set bar positions to avoid overlap
        incomeVsExpenseChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        incomeVsExpenseChart.data = data
        incomeVsExpenseChart.groupBars(0f, 0.1f, 0.05f) // Group bars with some spacing
        
        // Force y-axis to have a minimum range for better visualization
        incomeVsExpenseChart.axisLeft.axisMinimum = 0f
        incomeVsExpenseChart.axisLeft.axisMaximum = incomeVsExpenseChart.data.yMax * 1.1f
        incomeVsExpenseChart.setFitBars(true)
        incomeVsExpenseChart.setVisibleXRangeMaximum(6f) // Show max 6 bars at a time
        incomeVsExpenseChart.invalidate()
        
        // Log after setup
        android.util.Log.d("AnalyticsFragment", "Chart setup complete")
    }
    
    private fun updateIncomeVsExpenseChartWeekly(allTransactions: List<Transaction>) {
        // Print debug info
        android.util.Log.d("AnalyticsFragment", "Updating weekly chart with ${allTransactions.size} transactions")
        
        // Get data for the last 6 weeks
        val endCal = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.WEEK_OF_YEAR, -5) // Go back 5 weeks (for 6 weeks total)
        
        // Set both calendars to the first day of their respective weeks
        startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        
        val weeklyData = mutableListOf<Triple<String, Double, Double>>() // Week label, Income, Expense
        val xLabels = mutableListOf<String>()
        
        // Process each week
        val currentCal = startCal.clone() as Calendar
        while (currentCal <= endCal) {
            val weekStart = currentCal.timeInMillis
            
            // Move to end of week
            currentCal.add(Calendar.DAY_OF_WEEK, 6)
            currentCal.set(Calendar.HOUR_OF_DAY, 23)
            currentCal.set(Calendar.MINUTE, 59)
            currentCal.set(Calendar.SECOND, 59)
            val weekEnd = currentCal.timeInMillis
            
            // Filter transactions for this week
            val weekTransactions = allTransactions.filter {
                it.date in weekStart..weekEnd
            }
            
            // Calculate income and expense totals
            val income = weekTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val expense = weekTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            
            // Format week label
            val weekLabel = weekFormat.format(Date(weekStart))
            xLabels.add(weekLabel)
            
            // Add to data
            weeklyData.add(Triple(weekLabel, income, expense))
            
            // Print debug info for this week
            android.util.Log.d("AnalyticsFragment", "Week: $weekLabel, Income: $income, Expense: $expense")
            
            // Move to first day of next week
            currentCal.add(Calendar.DAY_OF_YEAR, 1)
            currentCal.set(Calendar.DAY_OF_WEEK, currentCal.firstDayOfWeek)
            currentCal.set(Calendar.HOUR_OF_DAY, 0)
            currentCal.set(Calendar.MINUTE, 0)
            currentCal.set(Calendar.SECOND, 0)
        }
        
        // Create dummy data if no real data exists
        if (weeklyData.isEmpty() || weeklyData.all { it.second == 0.0 && it.third == 0.0 }) {
            android.util.Log.d("AnalyticsFragment", "No weekly data found - adding dummy data")
            // Show message
            Toast.makeText(requireContext(), "No transaction data found. Adding sample data.", Toast.LENGTH_SHORT).show()
            
            // Add dummy data for testing
            xLabels.clear()
            weeklyData.clear()
            val weeks = listOf("W1", "W2", "W3", "W4", "W5", "W6")
            for (i in 0 until 6) {
                xLabels.add(weeks[i])
                weeklyData.add(Triple(weeks[i], (500 + i * 200).toDouble(), (400 + i * 150).toDouble()))
            }
        }
        
        // Create entries for income and expense
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        
        for (i in weeklyData.indices) {
            val (_, income, expense) = weeklyData[i]
            incomeEntries.add(BarEntry(i.toFloat(), income.toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), expense.toFloat()))
        }
        
        // Log entries for debugging
        android.util.Log.d("AnalyticsFragment", "Weekly - Income entries: ${incomeEntries.size}, Expense entries: ${expenseEntries.size}")
        
        // Create dataset for income in weekly view
        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            // Use a nice light green color for income
            color = Color.parseColor("#2ECC71") // Light green
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
            valueTextSize = 10f
            
            // Enable values on bars for better visibility of data
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 100) return ""  // Don't draw very small values
                    return CurrencyFormatter.formatSimple(value.toDouble())
                }
            }
            
            // Set up single color instead of gradient for better visibility
            highLightAlpha = 90
            setDrawIcons(false)
        }
        
        // Create dataset for expense in weekly view
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            // Use a more vibrant orange/red color for expense
            color = Color.parseColor("#E74C3C") // Bright orange-red
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textSecondary)
            valueTextSize = 10f
            
            // Enable values on bars for better visibility of data
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 100) return ""  // Don't draw very small values
                    return CurrencyFormatter.formatSimple(value.toDouble())
                }
            }
            
            // Set up single color instead of gradient for better visibility
            highLightAlpha = 90
            setDrawIcons(false)
        }
        
        // Group datasets for grouped bar chart
        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(incomeDataSet)
        dataSets.add(expenseDataSet)
        
        // Create and set the data
        val data = BarData(dataSets)
        data.barWidth = 0.35f // Adjust width to avoid overlap
        
        // Set bar positions to avoid overlap
        incomeVsExpenseChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        incomeVsExpenseChart.data = data
        incomeVsExpenseChart.groupBars(0f, 0.1f, 0.05f) // Group bars with some spacing
        
        // Force y-axis to have a minimum range for better visualization
        incomeVsExpenseChart.axisLeft.axisMinimum = 0f
        incomeVsExpenseChart.axisLeft.axisMaximum = incomeVsExpenseChart.data.yMax * 1.1f
        incomeVsExpenseChart.setFitBars(true)
        incomeVsExpenseChart.setVisibleXRangeMaximum(6f) // Show max 6 bars at a time
        incomeVsExpenseChart.invalidate()
        
        // Log after setup
        android.util.Log.d("AnalyticsFragment", "Weekly chart setup complete")
    }
    
    // ─── Month-over-Month Change Update ───────────────────────
    private fun updateMonthOverMonthChange(allTransactions: List<Transaction>) {
        // Define current and previous month periods
        val currentCal = Calendar.getInstance()
        val previousCal = Calendar.getInstance()
        previousCal.add(Calendar.MONTH, -1)
        
        // Calculate first and last day of current month
        val currentStartCal = Calendar.getInstance()
        currentStartCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), 1, 0, 0, 0)
        currentStartCal.set(Calendar.MILLISECOND, 0)
        val currentStart = currentStartCal.timeInMillis
        
        val currentEndCal = Calendar.getInstance()
        currentEndCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), 
                      currentCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        currentEndCal.set(Calendar.MILLISECOND, 999)
        val currentEnd = currentEndCal.timeInMillis
        
        // Calculate first and last day of previous month
        val previousStartCal = Calendar.getInstance()
        previousStartCal.set(previousCal.get(Calendar.YEAR), previousCal.get(Calendar.MONTH), 1, 0, 0, 0)
        previousStartCal.set(Calendar.MILLISECOND, 0)
        val previousStart = previousStartCal.timeInMillis
        
        val previousEndCal = Calendar.getInstance()
        previousEndCal.set(previousCal.get(Calendar.YEAR), previousCal.get(Calendar.MONTH), 
                       previousCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        previousEndCal.set(Calendar.MILLISECOND, 999)
        val previousEnd = previousEndCal.timeInMillis
        
        // Calculate income for current and previous month
        val currentIncome = allTransactions
            .filter { it.type == TxType.INCOME && it.date in currentStart..currentEnd }
            .sumOf { it.amount }
        
        val previousIncome = allTransactions
            .filter { it.type == TxType.INCOME && it.date in previousStart..previousEnd }
            .sumOf { it.amount }
        
        // Calculate expenses for current and previous month
        val currentExpense = allTransactions
            .filter { it.type == TxType.EXPENSE && it.date in currentStart..currentEnd }
            .sumOf { it.amount }
        
        val previousExpense = allTransactions
            .filter { it.type == TxType.EXPENSE && it.date in previousStart..previousEnd }
            .sumOf { it.amount }
        
        // Get previous month name for display
        val previousMonthName = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(previousStart))
        
        // Update income UI
        tvIncomeValue.text = CurrencyFormatter.format(currentIncome)
        
        val incomeDelta = currentIncome - previousIncome
        val incomePctChange = if (previousIncome > 0) {
            (incomeDelta / previousIncome) * 100
        } else if (currentIncome > 0) {
            100.0 // Interpret as +100% if previously zero
        } else {
            0.0 // Both zero
        }
        
        // Format with sign and 1 decimal place
        val incomeChangeText = if (incomePctChange >= 0) {
            "+${String.format("%.1f", incomePctChange)}% vs $previousMonthName"
        } else {
            "${String.format("%.1f", incomePctChange)}% vs $previousMonthName"
        }
        
        tvIncomeChange.text = incomeChangeText
        
        // Set color based on whether increased income is good
        val incomeChangeColor = if (incomePctChange >= 0) {
            ContextCompat.getColor(requireContext(), R.color.colorIncome)
        } else {
            ContextCompat.getColor(requireContext(), R.color.colorExpense)
        }
        tvIncomeChange.setTextColor(incomeChangeColor)
        
        // Update expense UI
        tvExpenseValue.text = CurrencyFormatter.format(currentExpense)
        
        val expenseDelta = currentExpense - previousExpense
        val expensePctChange = if (previousExpense > 0) {
            (expenseDelta / previousExpense) * 100
        } else if (currentExpense > 0) {
            100.0 // Interpret as +100% if previously zero
        } else {
            0.0 // Both zero
        }
        
        // Format with sign and 1 decimal place, negative sign for expenses is good
        val expenseChangeText = if (expensePctChange >= 0) {
            "+${String.format("%.1f", expensePctChange)}% vs $previousMonthName"
        } else {
            "${String.format("%.1f", expensePctChange)}% vs $previousMonthName"
        }
        
        tvExpenseChange.text = expenseChangeText
        
        // For expenses, lower is better (negative change is good)
        val expenseChangeColor = if (expensePctChange < 0) {
            ContextCompat.getColor(requireContext(), R.color.colorIncome)
        } else {
            ContextCompat.getColor(requireContext(), R.color.colorExpense)
        }
        tvExpenseChange.setTextColor(expenseChangeColor)
    }
    
    // ─── Category-to-Category Benchmark Update ───────────────────────
    private fun updateCategoryComparison() {
        if (availableCategories.isEmpty() || !::spinnerPrimaryCategory.isInitialized) {
            return
        }
        
        // Get selected categories
        val primaryCategory = availableCategories[primaryCategoryIndex]
        val secondaryCategory = availableCategories[secondaryCategoryIndex]
        
        // Update category names in display
        val primaryCategoryName = primaryCategory.substringAfter(' ')
        val secondaryCategoryName = secondaryCategory.substringAfter(' ')
        
        tvPrimaryCategoryName.text = "$primaryCategoryName:"
        tvSecondaryCategoryName.text = "$secondaryCategoryName:"
        
        // Calculate sum for each category this month
        val cal = Calendar.getInstance()
        val startOfMonth = Calendar.getInstance().apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfMonth = Calendar.getInstance().apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // Function to sum transactions for a category
        fun sumForCategory(category: String, start: Long, end: Long): Double {
            val emoji = category.substringBefore(' ')
            val name = category.substringAfter(' ')
            return PrefsManager.loadTransactions()
                .filter { tx ->
                    tx.type == TxType.EXPENSE && 
                    tx.date in start..end && 
                    (tx.category == category || tx.category.endsWith(name))
                }
                .sumOf { it.amount }
        }
        
        val primarySum = sumForCategory(primaryCategory, startOfMonth, endOfMonth)
        val secondarySum = sumForCategory(secondaryCategory, startOfMonth, endOfMonth)
        
        // Update amount texts
        tvPrimaryCategoryAmount.text = CurrencyFormatter.format(primarySum)
        tvSecondaryCategoryAmount.text = CurrencyFormatter.format(secondarySum)
        
        // Calculate ratio and update progress bars
        val maxProgress = 100
        if (secondarySum > 0 && primarySum > 0) {
            val ratio = (primarySum / secondarySum)
            // Set progress relative to the larger value
            val secondaryProgress = maxProgress
            val primaryProgress = (ratio * maxProgress).toInt().coerceIn(1, maxProgress)
            
            progressPrimaryCategory.progress = primaryProgress
            progressSecondaryCategory.progress = secondaryProgress
            
            // Update comparison text
            val pctString = String.format("%.0f", ratio * 100)
            tvCategoryComparison.text = "$primaryCategoryName is $pctString% of $secondaryCategoryName spending"
            
            // Calculate difference
            val diff = secondarySum - primarySum
            tvCategoryDifference.text = if (diff > 0) {
                "You spent ${CurrencyFormatter.format(diff)} more on $secondaryCategoryName"
            } else if (diff < 0) {
                "You spent ${CurrencyFormatter.format(abs(diff))} more on $primaryCategoryName"
            } else {
                "You spent the same on both categories"
            }
        } else {
            // Handle case when one or both are zero
            if (primarySum > 0) {
                progressPrimaryCategory.progress = maxProgress
                progressSecondaryCategory.progress = 0
                tvCategoryComparison.text = "No spending on $secondaryCategoryName this month"
                tvCategoryDifference.text = "You only spent on $primaryCategoryName"
            } else if (secondarySum > 0) {
                progressPrimaryCategory.progress = 0
                progressSecondaryCategory.progress = maxProgress
                tvCategoryComparison.text = "No spending on $primaryCategoryName this month"
                tvCategoryDifference.text = "You only spent on $secondaryCategoryName"
            } else {
                // Both zero
                progressPrimaryCategory.progress = 0
                progressSecondaryCategory.progress = 0
                tvCategoryComparison.text = "No spending on either category"
                tvCategoryDifference.text = "Add transactions to see comparisons"
            }
        }
    }
} 