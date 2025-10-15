package com.tools.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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

    @Query("SELECT COUNT(*) FROM expenses WHERE date = :date AND amount = :amount AND note = :note")
    suspend fun exists(date: Long, amount: Double, note: String): Int
}
