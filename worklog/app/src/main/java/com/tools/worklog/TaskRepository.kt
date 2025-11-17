package com.tools.worklog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate


class TaskRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.taskDao()


    suspend fun getTasksForDate(dateIso: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        dao.tasksForDate(dateIso)
    }


    suspend fun addTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.insert(task)
    }


    suspend fun addTasks(tasks: List<TaskEntity>) = withContext(Dispatchers.IO) {
        dao.insertAll(tasks)
    }


    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.update(task)
    }


    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.delete(task)
    }


    // Find the most recent day's tasks before the given date (used to copy into a new day)
    suspend fun latestBefore(dateIso: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        dao.latestBefore(dateIso)
    }
}