package com.example.flowfund

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val photoPath: String? = null,
    val isRecurring: Boolean = false    // true = recurring expense
)