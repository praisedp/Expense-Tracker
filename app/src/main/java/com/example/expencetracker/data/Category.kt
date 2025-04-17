package com.example.expencetracker.data

data class Category(
    val name: String,      // Name of the category (e.g., Food, Bills)
    val type: TxType,      // Type: INCOME or EXPENSE
    val emoji: String      // Emoji representing the category icon (e.g., "🍔", "💰")
)