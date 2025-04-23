package com.example.expencetracker.util

import com.example.expencetracker.data.Transaction

object TransactionFilter {
    fun byDateRange(
        list: List<Transaction>,
        from: Long,
        to:   Long
    ): List<Transaction> = list.filter { it.date in from..to }
}