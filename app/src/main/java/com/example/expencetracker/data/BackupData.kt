package com.example.expencetracker.data

data class BackupData(
    val transactions: List<Transaction>,
    val categories:    List<Category>,
    val totalBudget:   Double,
    val categoryBudgets: List<CategoryBudget>,
    val currency:      String
)