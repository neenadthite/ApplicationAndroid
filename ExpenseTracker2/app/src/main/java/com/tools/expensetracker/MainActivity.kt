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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tools.expensetracker.utils.SmsExpenseReader
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    private lateinit var dateInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var bottomNav: BottomNavigationView

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val categoryList = listOf("Food", "Transport", "Education", "DailyNeeds")

    private val SMS_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dateInput = findViewById(R.id.dateInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        amountInput = findViewById(R.id.amountInput)
        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        bottomNav = findViewById(R.id.bottomNav)

        //Check SMS permission
        checkSmsPermission()

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

        //Add Button to sync entries from SMS
        findViewById<Button>(R.id.syncSmsButton).setOnClickListener {
            lifecycleScope.launch {
                val dao = ExpenseDatabase.getDatabase(this@MainActivity).expenseDao()
                val smsExpenses = SmsExpenseReader.readBankMessages(this@MainActivity)

                var addedCount = 0
                var skippedCount = 0

                for (expense in smsExpenses)  {
                    val exists = dao.exists( expense.amount, expense.note, expense.category)
                    if (exists == 0) {
                        dao.insert(expense)
                        addedCount++
                    } else {
                        skippedCount++
                    }
                }
                Toast.makeText(this@MainActivity, "Imported ${addedCount} entries and Skipped ${skippedCount} entries!", Toast.LENGTH_LONG).show()
            }
        }

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
                val exists = dao.exists( expense.amount, expense.note, expense.category)
                if (exists == 0) {
                    dao.insert(expense)
                    Toast.makeText(this@MainActivity, "Expense added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Duplicate entry skipped!", Toast.LENGTH_SHORT).show()
                }
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

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), SMS_PERMISSION_CODE)
        }
    }
}
