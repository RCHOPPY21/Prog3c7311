package com.example.flowfund

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// DAO = Data Access Object — this is how we talk to the database
@Dao
interface ExpenseDao {

    // Save a new expense
    @Insert
    suspend fun insertExpense(expense: Expense)

    // Get ALL expenses (newest first)
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    suspend fun getAllExpenses(): List<Expense>

    // Get expenses for a specific category
    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY id DESC")
    suspend fun getExpensesByCategory(category: String): List<Expense>
}