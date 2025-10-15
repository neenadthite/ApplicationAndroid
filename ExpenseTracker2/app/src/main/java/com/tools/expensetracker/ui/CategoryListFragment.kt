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

class CategoryListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalText: TextView
    private lateinit var monthSelector: Spinner
    private lateinit var dao: com.tools.expensetracker.data.ExpenseDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_category_list, container, false)

        recyclerView = view.findViewById(R.id.categoryRecycler)
        totalText = view.findViewById(R.id.totalText)
        monthSelector = view.findViewById(R.id.monthSelector)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()

        setupMonthSelector()
        return view
    }

    private fun setupMonthSelector() {
        val months = listOf(
            "01 - January", "02 - February", "03 - March", "04 - April",
            "05 - May", "06 - June", "07 - July", "08 - August",
            "09 - September", "10 - October", "11 - November", "12 - December"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        monthSelector.adapter = adapter

        // Select current month by default
        val currentMonthIndex = LocalDate.now().monthValue - 1
        monthSelector.setSelection(currentMonthIndex)

        monthSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val month = months[position].substring(0, 2)
                loadExpensesByMonth(month)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadExpensesByMonth(month: String) {
        val year = LocalDate.now().year.toString()

        lifecycleScope.launch {
            val list = dao.getByCurrentMonth(month, year)
            val total = list.sumOf { it.amount }

            recyclerView.adapter = ExpenseAdapter(list) { expense ->
                showEditDialog(expense)
            }

            totalText.text = "Total Expense: ₹%.2f".format(total)

            when {
                total < 5000 -> totalText.setBackgroundColor(Color.parseColor("#C8E6C9")) // Green
                total < 10000 -> totalText.setBackgroundColor(Color.parseColor("#FFF59D")) // Yellow
                else -> totalText.setBackgroundColor(Color.parseColor("#FFCDD2")) // Red
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
                    dao.insert(updated)
                    val month = "%02d".format(expense.date.monthValue)
                    loadExpensesByMonth(month)
                    Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
