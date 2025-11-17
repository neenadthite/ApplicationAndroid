package com.tools.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String,   // e.g. "January"
    val year: String,    // e.g. "2025"
    val amount: Double   // Monthly budget limit
)
