package com.tools.worklog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "elapsedMillis") var elapsedMillis: Long = 0L,
    @ColumnInfo(name = "running") var running: Boolean = false,
    @ColumnInfo(name = "lastStart") var lastStart: Long = 0L,
// date stored as ISO yyyy-MM-dd
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "notes") var notes: String? = null
)


// Lightweight in-memory model used by UI (optional)
data class Task(
    val id: Long,
    var title: String,
    var elapsedMillis: Long = 0L,
    var running: Boolean = false,
    var lastStart: Long = 0L,
    val date: String,
    var notes: String? = null
)