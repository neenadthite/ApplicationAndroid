package com.tools.expensetracker.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tools.expensetracker.R
import com.tools.expensetracker.data.Expense
import com.tools.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class CategoryListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalText: TextView
    private lateinit var monthSelector: Spinner
    private lateinit var yearSelector: Spinner
    private lateinit var dao: com.tools.expensetracker.data.ExpenseDao


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_category_list, container, false)

        recyclerView = view.findViewById(R.id.categoryRecycler)
        totalText = view.findViewById(R.id.totalText)
        monthSelector = view.findViewById(R.id.monthSelector)
        yearSelector = view.findViewById(R.id.yearSelector)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()

        setupSelectors()
        return view
    }

    private fun setupSelectors() {
        val months = Month.values().map {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        val years = (2020..LocalDate.now().year).map { it.toString() }.reversed()

        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        monthSelector.adapter = monthAdapter
        yearSelector.adapter = yearAdapter

        val currentMonthIndex = LocalDate.now().monthValue - 1
        monthSelector.setSelection(currentMonthIndex)
        yearSelector.setSelection(0) // current year (reversed list)

        val reloadData: () -> Unit = {
            val selectedMonthName = monthSelector.selectedItem.toString()
            val selectedMonthNumber = Month.valueOf(selectedMonthName.uppercase()).value
            val selectedYear = yearSelector.selectedItem.toString()
            loadExpensesByMonth(selectedMonthNumber, selectedYear)
        }

        monthSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = reloadData()
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        yearSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = reloadData()
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        reloadData() // load initial data
    }

    private fun loadExpensesByMonth(month: Int, year: String) {
        val monthStr = "%02d".format(month)

        lifecycleScope.launch {
            val list = dao.getByCurrentMonth(monthStr, year)
            val total = list.sumOf { it.amount }

            val adapter = ExpenseAdapter(
                expenses = list.toMutableList(),
                onEditClick = { expense -> showEditDialog(expense) },
                onDeleteClick = { expense -> confirmDelete(expense) }
            )
            recyclerView.adapter = adapter

            totalText.text = "Total for $year ${Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())}: ₹%.2f".format(total)

            when {
                total < 5000 -> totalText.setBackgroundColor(Color.parseColor("#C8E6C9"))
                total < 10000 -> totalText.setBackgroundColor(Color.parseColor("#FFF59D"))
                else -> totalText.setBackgroundColor(Color.parseColor("#FFCDD2"))
            }
        }
    }

    private fun showEditDialog(expense: Expense) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val amountInput = EditText(requireContext()).apply {
            hint = "Amount"
            setText(expense.amount.toString())
        }
        val noteInput = EditText(requireContext()).apply {
            hint = "Note"
            setText(expense.note)
        }

        layout.addView(amountInput)
        layout.addView(noteInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updated = expense.copy(
                    amount = amountInput.text.toString().toDoubleOrNull() ?: expense.amount,
                    note = noteInput.text.toString()
                )
                lifecycleScope.launch {
                    dao.update(updated)
                    val month = expense.date.monthValue
                    loadExpensesByMonth(month, expense.date.year.toString())
                    Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Confirm delete
    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    dao.delete(expense)
                    Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Refresh after edit/delete
    fun refreshList() {
        val selectedMonthName = monthSelector.selectedItem.toString()
        val selectedMonthNumber = Month.valueOf(selectedMonthName.uppercase()).value
        val selectedYear = yearSelector.selectedItem.toString()
        loadExpensesByMonth(selectedMonthNumber, selectedYear)
    }

}
