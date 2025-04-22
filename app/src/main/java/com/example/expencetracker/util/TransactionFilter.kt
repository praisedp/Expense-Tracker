package com.example.expencetracker.util

import com.example.expencetracker.data.Transaction

object TransactionFilter {
    /** Returns only those with date in [startMillis]..[endMillis]. */
    fun byDateRange(
        transactions: List<Transaction>,
        startMillis: Long,
        endMillis: Long
    ): List<Transaction> =
        transactions.filter { it.date in startMillis..endMillis }
}