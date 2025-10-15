package com.tools.expensetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tools.expensetracker.data.Expense
import com.tools.expensetracker.data.ExpenseDatabase
import com.tools.expensetracker.ui.CategoryListFragment
import com.tools.expensetracker.ui.ChartFragment
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dateInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var bottomNav: BottomNavigationView

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val categoryList = listOf("Food", "Transport", "Education", "DailyNeeds")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dateInput = findViewById(R.id.dateInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        amountInput = findViewById(R.id.amountInput)
        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        bottomNav = findViewById(R.id.bottomNav)

        // Date picker
        dateInput.setOnClickListener {
            val today = Calendar.getInstance()
            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val date = LocalDate.of(year, month + 1, day)
                    dateInput.setText(date.toString())
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )
            picker.show()
        }

        // Category dropdown
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoryList
        )
        categorySpinner.adapter = adapter

        addButton.setOnClickListener { addExpense() }

        // Bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CategoryListFragment())
                        .commit()
                    true
                }

                R.id.nav_chart -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ChartFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_all
    }

    private fun addExpense() {
        val dateStr = dateInput.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val amountStr = amountInput.text.toString()
        val note = noteInput.text.toString()

        if (dateStr.isBlank() || amountStr.isBlank()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = ExpenseDatabase.getDatabase(this@MainActivity).expenseDao()
                val expense = Expense(
                    date = LocalDate.parse(dateStr, dateFmt),
                    category = category,
                    amount = amountStr.toDouble(),
                    note = note
                )
                dao.insert(expense)
                clearInputs()
                Toast.makeText(this@MainActivity, "Expense added!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearInputs() {
        dateInput.text.clear()
        amountInput.text.clear()
        noteInput.text.clear()
        categorySpinner.setSelection(0)
    }
}
