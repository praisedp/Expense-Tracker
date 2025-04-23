package com.example.expencetracker.data

data class CategoryRow(
    val emoji: String,
    val name:  String,
    val amount: Double,
    val percent: Double,
    val color: Int // 0-100
)