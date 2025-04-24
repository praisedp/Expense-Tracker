package com.example.expencetracker.util

import com.example.expencetracker.data.Transaction
import com.example.expencetracker.data.TxType
import java.util.*

/**
 * Helper functions for "this‑month" budget calculations.
 *
 * All amounts returned are **expenses only** (TxType.EXPENSE).
 */
object BudgetCalculator {

    /** Returns epoch‑millis start & end of the current calendar month. */
    fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        // start = 1st 00:00 of this month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        // end = first day of next month 00:00
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    /** Filters the supplied list for expenses in the current month. */
    fun transactionsThisMonth(all: List<Transaction>): List<Transaction> {
        val (start, end) = currentMonthRange()
        return all.filter { it.type == TxType.EXPENSE && it.date in start until end }
    }

    /** Sum of expense amounts for the list supplied (already filtered). */
    fun spentTotal(monthTx: List<Transaction>): Double =
        monthTx.sumOf { Math.abs(it.amount) }

    /** Map category → amount spent (for the list supplied). */
    fun spentByCategory(monthTx: List<Transaction>): Map<String, Double> =
        monthTx.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { Math.abs(it.amount) } }
}