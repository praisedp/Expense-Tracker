package com.example.expencetracker.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.data.TxType
import com.example.expencetracker.util.BudgetCalculator
import com.example.expencetracker.util.CurrencyFormatter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsFragment : Fragment() {

    // ─── Budget Progress Views ───────────────────────────────────
    private lateinit var tvTotalStatus: TextView
    private lateinit var progressTotal: ProgressBar

    // ─── Charts ────────────────────────────────────────────────
    private lateinit var cashFlowChart: LineChart
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

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

        // Update data
        refreshAnalytics()
    }

    override fun onResume() {
        super.onResume()
        refreshAnalytics()
    }

    // ─── Analytics refresh ───────────────────────────────────────
    private fun refreshAnalytics() {
        // Update budget progress
        updateBudgetProgress()
        
        // Update cash flow chart
        val allTx = PrefsManager.loadTransactions()
        updateCashFlowChart(allTx)
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
    
    private fun updateCashFlowChart(allTransactions: List<com.example.expencetracker.data.Transaction>) {
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
        
        // Move to the most recent week
        val lastEntry = entries.last()
        cashFlowChart.moveViewToX(lastEntry.x)
        
        // Force a redraw with the new viewport settings
        cashFlowChart.invalidate()
    }
} 