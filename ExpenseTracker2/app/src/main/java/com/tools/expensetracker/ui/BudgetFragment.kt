package com.tools.expensetracker.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tools.expensetracker.R
import com.tools.expensetracker.data.Budget
import com.tools.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class BudgetFragment : Fragment() {

    private lateinit var monthSelector: Spinner
    private lateinit var yearSelector: Spinner
    private lateinit var budgetText: TextView
    private lateinit var setBudgetButton: Button
    private lateinit var dao: com.tools.expensetracker.data.ExpenseDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_budget, container, false)

        monthSelector = view.findViewById(R.id.monthSelector)
        yearSelector = view.findViewById(R.id.yearSelector)
        budgetText = view.findViewById(R.id.budgetText)
        setBudgetButton = view.findViewById(R.id.setBudgetButton)

        dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()

        setupSelectors()
        setupButton()

        return view
    }

    private fun setupSelectors() {
        val months = Month.values().map {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        val years = (2020..LocalDate.now().year).map { it.toString() }.reversed()

        monthSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        yearSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        val currentMonthIndex = LocalDate.now().monthValue - 1
        monthSelector.setSelection(currentMonthIndex)
        yearSelector.setSelection(0)

        loadBudget()
    }

    private fun setupButton() {
        setBudgetButton.setOnClickListener {
            val month = monthSelector.selectedItem.toString()
            val year = yearSelector.selectedItem.toString()

            val input = EditText(requireContext())
            input.hint = "Enter budget amount"

            AlertDialog.Builder(requireContext())
                .setTitle("Set Monthly Budget")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val amount = input.text.toString().toDoubleOrNull()
                    if (amount != null) {
                        lifecycleScope.launch {
                            dao.insertBudget(Budget(month = month, year = year, amount = amount))
                            Toast.makeText(requireContext(), "Budget set for $month $year", Toast.LENGTH_SHORT).show()
                            loadBudget()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadBudget() {
        val month = monthSelector.selectedItem.toString()
        val year = yearSelector.selectedItem.toString()

        lifecycleScope.launch {
            val budget = dao.getBudget(month, year)
            val totalSpent = dao.getByCurrentMonth("%02d".format(Month.valueOf(month.uppercase()).value), year)
                .sumOf { it.amount }

            if (budget != null) {
                val remaining = budget.amount - totalSpent
                budgetText.text = "Budget: ₹%.2f\nSpent: ₹%.2f\nRemaining: ₹%.2f".format(budget.amount, totalSpent, remaining)

                when {
                    remaining < 0 -> budgetText.setBackgroundColor(Color.parseColor("#FFCDD2")) // over budget
                    remaining < budget.amount * 0.25 -> budgetText.setBackgroundColor(Color.parseColor("#FFF59D")) // close to limit
                    else -> budgetText.setBackgroundColor(Color.parseColor("#C8E6C9")) // within safe range
                }
            } else {
                budgetText.text = "No budget set for $month $year"
                budgetText.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
