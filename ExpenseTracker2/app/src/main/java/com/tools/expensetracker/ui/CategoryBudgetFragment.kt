package com.tools.expensetracker.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.tools.expensetracker.R
import com.tools.expensetracker.data.CategoryBudget
import com.tools.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class CategoryBudgetFragment : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var monthSelector: Spinner
    private lateinit var yearSelector: Spinner
    private lateinit var setBudgetButton: Button
    private lateinit var dao: com.tools.expensetracker.data.ExpenseDao

    private val categories = listOf("Food", "Transport", "Education", "DailyNeeds", "Others")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_category_budget, container, false)
        barChart = view.findViewById(R.id.barChart)
        monthSelector = view.findViewById(R.id.monthSelector)
        yearSelector = view.findViewById(R.id.yearSelector)
        setBudgetButton = view.findViewById(R.id.setCategoryBudgetButton)

        dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()

        setupSelectors()
        setupButton()

        return view
    }

    private fun setupSelectors() {
        val months = Month.values().map { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
        val years = (2020..LocalDate.now().year).map { it.toString() }.reversed()

        monthSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        yearSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        monthSelector.setSelection(LocalDate.now().monthValue - 1)
        yearSelector.setSelection(0)

        loadChartData()
    }

    private fun setupButton() {
        setBudgetButton.setOnClickListener {
            val month = monthSelector.selectedItem.toString()
            val year = yearSelector.selectedItem.toString()

            val dialogLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 10)
            }

            val inputs = mutableMapOf<String, EditText>()
            categories.forEach { cat ->
                val input = EditText(requireContext()).apply {
                    hint = "Budget for $cat"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                dialogLayout.addView(input)
                inputs[cat] = input
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Set Category Budgets for $month $year")
                .setView(dialogLayout)
                .setPositiveButton("Save") { _, _ ->
                    lifecycleScope.launch {
                        inputs.forEach { (category, editText) ->
                            val amount = editText.text.toString().toDoubleOrNull() ?: 0.0
                            dao.insertCategoryBudget(CategoryBudget(0, category, month, year, amount))
                        }
                        Toast.makeText(requireContext(), "Category budgets saved!", Toast.LENGTH_SHORT).show()
                        loadChartData()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadChartData() {
        val month = monthSelector.selectedItem.toString()
        val year = yearSelector.selectedItem.toString()
        val monthStr = "%02d".format(Month.valueOf(month.uppercase()).value)

        lifecycleScope.launch {
            val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
            val budgets = dao.getCategoryBudgets(month, year)
            val expenses = dao.getByCurrentMonth(monthStr, year)

            val spentByCategory = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { e -> e.amount } }

            val entriesBudget = mutableListOf<BarEntry>()
            val entriesSpent = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()

            categories.forEachIndexed { index, category ->
                val budgetAmount = budgets.find { it.category == category }?.amount ?: 0.0
                val spentAmount = spentByCategory[category] ?: 0.0
                entriesBudget.add(BarEntry(index.toFloat(), budgetAmount.toFloat()))
                entriesSpent.add(BarEntry(index.toFloat(), spentAmount.toFloat()))
                labels.add(category)
            }

            val budgetSet = BarDataSet(entriesBudget, "Budget")
            budgetSet.color = Color.rgb(76, 175, 80)
            val spentSet = BarDataSet(entriesSpent, "Spent")
            spentSet.color = Color.rgb(244, 67, 54)

            val data = BarData(budgetSet, spentSet)
            data.barWidth = 0.4f
            barChart.data = data
            barChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
            barChart.xAxis.granularity = 1f
            barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            barChart.description = Description().apply { text = "Budget vs Spent by Category" }
            barChart.groupBars(0f, 0.2f, 0.05f)
            barChart.invalidate()
        }
    }
}
