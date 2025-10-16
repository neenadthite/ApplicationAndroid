package com.tools.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

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

    // Auto-updating Flow for all expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<Expense>>

    // For month/year filters (used in CategoryListFragment)
    @Query("""
        SELECT * FROM expenses 
        WHERE strftime('%m', date / 1000, 'unixepoch') = :month 
        AND strftime('%Y', date / 1000, 'unixepoch') = :year
        ORDER BY date DESC
    """)
    fun getByMonthFlow(month: String, year: String): kotlinx.coroutines.flow.Flow<List<Expense>>

    //Delete a single expense
    @Delete
    suspend fun delete(expense: Expense)

}
