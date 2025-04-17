package com.example.expencetracker.data

data class Transaction(
    val id: Long = System.currentTimeMillis(),  // Unique ID using current time
    var title: String,                            // Title of the transaction
    var amount: Double,                           // Amount
    var category: String,                         // Category of expense (or a label for income)
    var type: TxType,                             // Type: INCOME or EXPENSE
    var date: Long                                // Date stored as epoch milliseconds
)