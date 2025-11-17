package com.tools.worklog

import android.app.AlertDialog
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.worklog.TaskAdapter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {


    private lateinit var repo: TaskRepository
    private lateinit var adapter: TaskAdapter
    private var selectedDateIso: String = isoToday()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_calendar)


        repo = TaskRepository(this)


        val calendar = findViewById<CalendarView>(R.id.calendarView)
        val rv = findViewById<RecyclerView>(R.id.recyclerTasks)
        val btnAdd = findViewById<Button>(R.id.btnAdd)


        adapter = TaskAdapter(mutableListOf(), this::toggleTask, this::editTaskDialog, this::deleteTask)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter


// Initialize with today
        selectedDateIso = isoToday()
        loadTasksForDate(selectedDateIso)


// When user selects a date
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
// month is 0-based from CalendarView -> convert to LocalDate
            val m = month + 1
            val dateIso = String.format("%04d-%02d-%02d", year, m, dayOfMonth)
            selectedDateIso = dateIso
            loadTasksForDate(dateIso)
        }


// Add task tied to selected date
        btnAdd.setOnClickListener { addTaskDialog() }
    }

    override fun onResume() {
        super.onResume()
// If day changed since last opened, ensure today's tasks exist (auto-copy last available day's tasks)
        val today = isoToday()
        if (today != selectedDateIso) {
            selectedDateIso = today
            loadTasksForDate(today)
        }
        lifecycleScope.launch {
            ensureTasksForToday()
        }
    }


    private fun ensureTasksForToday() {
        lifecycleScope.launch {
            val today = isoToday()
            val existing = repo.getTasksForDate(today)
            if (existing.isEmpty()) {
// find latest before today
                val prev = repo.latestBefore(today)
                if (prev.isNotEmpty()) {
// copy previous day's tasks to today (reset elapsed/run state)
                    val copies = prev.map { e ->
                        TaskEntity(
                            title = e.title,
                            elapsedMillis = 0L,
                            running = false,
                            lastStart = 0L,
                            date = today
                        )
                    }
                    repo.addTasks(copies)
// reload UI
                    loadTasksForDate(today)
                }
            }
        }
    }


    private fun loadTasksForDate(dateIso: String) {
        lifecycleScope.launch {
            val list = repo.getTasksForDate(dateIso)
            val uiList = list.map { e -> Task(e.id, e.title, e.elapsedMillis, e.running, e.lastStart, e.date) }
            adapter.setItems(uiList)
        }
    }


    private fun addTaskDialog() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT }
        AlertDialog.Builder(this)
            .setTitle("New Task for $selectedDateIso")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val title = input.text.toString().ifBlank { "Untitled" }
                lifecycleScope.launch {
                    val id = repo.addTask(TaskEntity(title = title, date = selectedDateIso))
// reload
                    loadTasksForDate(selectedDateIso)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


private fun editTaskDialog(task: Task) {
    val input = EditText(this).apply { setText(task.title); inputType = InputType.TYPE_CLASS_TEXT }
    AlertDialog.Builder(this)
        .setTitle("Edit Task")
        .setView(input)
        .setPositiveButton("Save") { _, _ ->
            val newTitle = input.text.toString().ifBlank { task.title }
            lifecycleScope.launch {
                repo.updateTask(TaskEntity(id = task.id, title = newTitle, elapsedMillis = task.elapsedMillis, running = task.running, lastStart = task.lastStart, date = task.date))
                loadTasksForDate(selectedDateIso)
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}


private fun deleteTask(task: Task) {
    lifecycleScope.launch {
        repo.deleteTask(TaskEntity(id = task.id, title = task.title, elapsedMillis = task.elapsedMillis, running = task.running, lastStart = task.lastStart, date = task.date))
        loadTasksForDate(selectedDateIso)
    }
}


private fun toggleTask(task: Task) {
    lifecycleScope.launch {
        val now = SystemClock.elapsedRealtime()
        val updated = if (task.running) {
// stop
            val elapsed = task.elapsedMillis + (now - task.lastStart)
            TaskEntity(id = task.id, title = task.title, elapsedMillis = elapsed, running = false, lastStart = 0L, date = task.date)
        } else {
// start
            TaskEntity(id = task.id, title = task.title, elapsedMillis = task.elapsedMillis, running = true, lastStart = now, date = task.date)
        }
        repo.updateTask(updated)
        loadTasksForDate(selectedDateIso)
    }
}


companion object {
    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun isoToday(): String = LocalDate.now().format(dtf)
}
}