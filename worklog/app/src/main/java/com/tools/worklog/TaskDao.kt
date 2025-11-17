package com.tools.worklog

import androidx.room.*


@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id ASC")
    suspend fun tasksForDate(date: String): List<TaskEntity>


    @Insert
    suspend fun insert(task: TaskEntity): Long


    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>


    @Update
    suspend fun update(task: TaskEntity)


    @Delete
    suspend fun delete(task: TaskEntity)


    @Query("SELECT * FROM tasks WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun latestBefore(date: String): List<TaskEntity>
}