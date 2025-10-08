package com.tools.expensetracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// -----------------------------
// Expense Data Model
// -----------------------------
data class Expense(
    val id: Long,
    val date: LocalDate,
    val category: String,
    val amount: Double,
    val note: String
)

// -----------------------------
// Expense Repository (in-memory)
// -----------------------------
object ExpenseStore {
    private var nextId = 1L
    private val expenses = mutableListOf<Expense>()

    fun add(date: LocalDate, category: String, amount: Double, note: String) {
        val expense = Expense(nextId++, date, category, amount, note)
        expenses.add(expense)
    }

    fun all(): List<Expense> = expenses.toList()

    fun total(): Double = expenses.sumOf { it.amount }
}

// -----------------------------
// RecyclerView Adapter
// -----------------------------
class ExpenseAdapter(private val data: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.itemDate)
        val category: TextView = itemView.findViewById(R.id.itemCategory)
        val amount: TextView = itemView.findViewById(R.id.itemAmount)
        val note: TextView = itemView.findViewById(R.id.itemNote)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = data[position]
        holder.date.text = expense.date.toString()
        holder.category.text = expense.category
        holder.amount.text = "₹%.2f".format(expense.amount)
        holder.note.text = expense.note
    }

    override fun getItemCount(): Int = data.size
}

// -----------------------------
// Main Activity
// -----------------------------
class MainActivity : AppCompatActivity() {

    private lateinit var dateInput: EditText
    private lateinit var categoryInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var addButton: Button
    private lateinit var totalText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAllButton: Button
    private lateinit var filterButton: Button
    private lateinit var exportButton: Button

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        dateInput = findViewById(R.id.dateInput)
        categoryInput = findViewById(R.id.categoryInput)
        amountInput = findViewById(R.id.amountInput)
        noteInput = findViewById(R.id.noteInput)
        addButton = findViewById(R.id.addButton)
        totalText = findViewById(R.id.totalText)
        recyclerView = findViewById(R.id.expenseList)
        viewAllButton = findViewById(R.id.viewAllButton)
        filterButton = findViewById(R.id.filterButton)
        exportButton = findViewById(R.id.exportButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        updateList()

        // Add Expense Button
        addButton.setOnClickListener {
            val dateStr = dateInput.text.toString()
            val category = categoryInput.text.toString()
            val amountStr = amountInput.text.toString()
            val note = noteInput.text.toString()

            if (dateStr.isBlank() || category.isBlank() || amountStr.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val date = LocalDate.parse(dateStr, dateFmt)
                val amount = amountStr.toDouble()
                ExpenseStore.add(date, category, amount, note)
                clearInputs()
                updateList()
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid input format", Toast.LENGTH_SHORT).show()
            }
        }

        // View All Button
        viewAllButton.setOnClickListener {
            updateList()
        }

        // Filter Button (basic placeholder)
        filterButton.setOnClickListener {
            Toast.makeText(this, "Filter feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Export Button (basic placeholder)
        exportButton.setOnClickListener {
            Toast.makeText(this, "Export to CSV feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        dateInput.text.clear()
        categoryInput.text.clear()
        amountInput.text.clear()
        noteInput.text.clear()
    }

    private fun updateList() {
        val data = ExpenseStore.all()
        recyclerView.adapter = ExpenseAdapter(data)
        totalText.text = "Total: ₹%.2f".format(ExpenseStore.total())
    }
}
