package com.tools.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAll(): List<Expense>

    // Get only current month’s data
    @Query("SELECT * FROM expenses WHERE strftime('%m', date) = :month AND strftime('%Y', date) = :year ORDER BY date DESC")
    suspend fun getByCurrentMonth(month: String, year: String): List<Expense>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

}
