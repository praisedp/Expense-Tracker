package com.example.expencetracker.util

import android.content.Context
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import java.text.NumberFormat
import com.example.expencetracker.data.TxType

/**
 * Checks the user’s budget after every transaction save and
 * fires a notification if thresholds are crossed.
 */
object BudgetAlertManager {
    /** tracks which categories have already triggered which alert this month */
    private var monthKey   = ""                         // e.g. "2025‑04"
    private var alerted90  = mutableSetOf<String>()     // cat names for 90 %
    private var alerted100 = mutableSetOf<String>()     // cat names for 100 %
    private const val THRESHOLD_APPROACH = 0.90      // 90 %
    private const val THRESHOLD_EXCEEDED = 1.00      // 100 %

    fun check(context: Context) {
        val cal = java.util.Calendar.getInstance()
        val nowKey = String.format(
            "%04d-%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1     // Calendar.MONTH is 0‑based
        )
        if (nowKey != monthKey) {
            // new month → reset memory
            monthKey = nowKey
            alerted90.clear()
            alerted100.clear()
        }
        val totalLimit = PrefsManager.getTotalBudget()
        if (totalLimit <= 0) return            // no budget set

        val monthTx   = BudgetCalculator.transactionsThisMonth(PrefsManager.loadTransactions())
        val spent     = BudgetCalculator.spentTotal(monthTx)

        val pct = spent / totalLimit
        when {
            pct >= THRESHOLD_EXCEEDED ->
            NotificationHelper.showBudgetAlert(
                    context,
                    context.getString(R.string.budget_exceeded),
                    context.getString(R.string.budget_exceeded_msg),
                    isExceeded = true
                )

            pct >= THRESHOLD_APPROACH ->
            NotificationHelper.showBudgetAlert(
                    context,
                    context.getString(R.string.budget_approach),
                    context.getString(R.string.budget_approach_msg,
                        (pct * 100).toInt()),
                    isExceeded = false
                )
        }

        // ── per‑category alerts ──────────────────────────────────────

        val catSpentMap = BudgetCalculator.spentByCategory(monthTx)
        val catLimits   = PrefsManager.loadCategoryBudgets()
        val fmt = NumberFormat.getCurrencyInstance()

        catLimits.forEach { limitEntry ->
            val spent = catSpentMap[limitEntry.category] ?: 0.0
            val pct   = spent / limitEntry.limit
            when {
                pct >= THRESHOLD_EXCEEDED && alerted100.add(limitEntry.category) -> {
                    NotificationHelper.showBudgetAlert(
                        context,
                        context.getString(R.string.cat_budget_exceeded),
                        "${limitEntry.category} budget exceeded (${fmt.format(spent)} / ${fmt.format(limitEntry.limit)})",
                        isExceeded = true
                    )
                }
                pct >= THRESHOLD_APPROACH  &&
                        !alerted100.contains(limitEntry.category) &&   // don’t warn 90 % after 100 %
                        alerted90.add(limitEntry.category) -> {

                    NotificationHelper.showBudgetAlert(
                        context,
                        context.getString(R.string.cat_budget_approach),
                        "${limitEntry.category} budget 90 % used",
                        isExceeded = false
                    )
                }
            }
        }
    }
}