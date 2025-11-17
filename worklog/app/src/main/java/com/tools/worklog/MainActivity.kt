package com.tools.worklog

import android.app.AlertDialog
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tools.worklog.TaskAdapter
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
// copy previous day's tasks to today (reset elapsed/run state, keep notes)
                    val copies = prev.map { e ->
                        TaskEntity(
                            title = e.title,
                            elapsedMillis = 0L,
                            running = false,
                            lastStart = 0L,
                            date = today,
                            notes = e.notes
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


    // Adds dialog that also accepts initial timing in hh:mm:ss (optional) and notes
    private fun addTaskDialog() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 8) }
        val titleInput = EditText(this).apply { hint = "Task title"; inputType = InputType.TYPE_CLASS_TEXT }
        val timeInput = EditText(this).apply { hint = "Initial time (HH:MM:SS) - optional"; inputType = InputType.TYPE_CLASS_TEXT }
        val notesInput = EditText(this).apply { hint = "Notes (optional)"; inputType = InputType.TYPE_CLASS_TEXT }
        container.addView(titleInput)
        container.addView(timeInput)
        container.addView(notesInput)


        AlertDialog.Builder(this)
            .setTitle("New Task for $selectedDateIso")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().ifBlank { "Untitled" }
                val timeText = timeInput.text.toString().trim()
                val notes = notesInput.text.toString().trim().ifBlank { null }
                val millis = if (timeText.isBlank()) 0L else parseHmsToMillis(timeText)
                lifecycleScope.launch {
                    repo.addTask(TaskEntity(title = title, date = selectedDateIso, elapsedMillis = millis, notes = notes))
// reload
                    loadTasksForDate(selectedDateIso)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // Edit dialog now allows changing title, elapsed time (HH:MM:SS), and notes
    private fun editTaskDialog(task: Task) {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 8) }
        val titleInput = EditText(this).apply { setText(task.title); inputType = InputType.TYPE_CLASS_TEXT }
        val timeInput = EditText(this).apply { setText(formatMillis(task.elapsedMillis)); inputType = InputType.TYPE_CLASS_TEXT }
        val notesInput = EditText(this).apply { setText(task.notes ?: ""); inputType = InputType.TYPE_CLASS_TEXT }
        val info = TextView(this).apply { text = "Enter total elapsed time (HH:MM:SS). If you want to adjust while running, stopping will store the current running time first." }
            container.addView(titleInput)
            container.addView(timeInput)
            container.addView(notesInput)
            container.addView(info)


            AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(container)
                .setPositiveButton("Save") { _, _ ->
                    val newTitle = titleInput.text.toString().ifBlank { task.title }
                    val notes = notesInput.text.toString().trim().ifBlank { null }
                    val timeText = timeInput.text.toString().trim()
                    val millis = parseHmsToMillisSafe(timeText, task.elapsedMillis)
                    lifecycleScope.launch {
                        repo.updateTask(TaskEntity(id = task.id, title = newTitle, elapsedMillis = millis, running = false, lastStart = 0L, date = task.date, notes = notes))
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

        // Utility: parse HH:MM:SS to millis. Throws for bad format.
        private fun parseHmsToMillis(hms: String): Long {
            val parts = hms.split(":").map { it.trim() }
            if (parts.size !in 1..3) throw IllegalArgumentException("Bad time format")
            val reversed = parts.reversed()
            var seconds = 0L
            try {
                if (reversed.size >= 1) seconds += reversed[0].toLong()
                if (reversed.size >= 2) seconds += reversed[1].toLong() * 60
                if (reversed.size == 3) seconds += reversed[2].toLong() * 3600
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Bad number in time")
            }
            return seconds * 1000
        }


        private fun parseHmsToMillisSafe(hms: String, fallback: Long): Long {
            return try {
                if (hms.isBlank()) fallback else parseHmsToMillis(hms)
            } catch (e: Exception) {
// if user enters bad value, keep existing
                fallback
            }
        }


        private fun formatMillis(ms: Long): String {
            val totalSeconds = ms / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

companion object {
    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun isoToday(): String = LocalDate.now().format(dtf)
}
}