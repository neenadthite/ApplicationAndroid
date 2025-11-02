package com.tools.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAll(): List<Expense>

    // Get only current month’s data
    @Query("SELECT * FROM expenses WHERE strftime('%m', date) = :month AND strftime('%Y', date) = :year ORDER BY date DESC")
    suspend fun getByCurrentMonth(month: String, year: String): List<Expense>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Check if the same entry exists
    @Query("""
        SELECT COUNT(*) FROM expenses
        WHERE amount = :amount AND note = :note AND category = :category
    """)
    suspend fun exists( amount: Double, note: String, category: String): Int

     // Update an existing expense entry.
    @Update
    suspend fun update(expense: Expense)

    // Delete a specific expense entry.
    @Delete
    suspend fun delete(expense: Expense)

    // Budget management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBudget(month: String, year: String): Budget?

    @Query("DELETE FROM budgets WHERE month = :month AND year = :year")
    suspend fun deleteBudget(month: String, year: String)
}
