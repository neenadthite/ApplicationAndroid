package com.tools.expensetracker.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.tools.expensetracker.R
import com.tools.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class ChartFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var monthSelector: Spinner
    private lateinit var yearSelector: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        monthSelector = view.findViewById(R.id.monthSelector)
        yearSelector = view.findViewById(R.id.yearSelector)
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
        yearSelector.setSelection(0)

        val reloadChart: () -> Unit = {
            val monthName = monthSelector.selectedItem.toString()
            val monthNumber = Month.valueOf(monthName.uppercase()).value
            val year = yearSelector.selectedItem.toString()
            loadChartData(monthNumber, year)
        }

        monthSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = reloadChart()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        yearSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = reloadChart()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        reloadChart()
    }

    private fun loadChartData(month: Int, year: String) {
        val monthStr = "%02d".format(month)

        lifecycleScope.launch {
            val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
            val expenses = dao.getByCurrentMonth(monthStr, year)
            val grouped = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }

            val entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }
            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            val dataSet = PieDataSet(entries, "Expenses for $monthName $year")
            dataSet.colors = listOf(
                Color.rgb(244, 67, 54),
                Color.rgb(33, 150, 243),
                Color.rgb(76, 175, 80),
                Color.rgb(255, 193, 7)
            )
            val data = PieData(dataSet)
            pieChart.data = data
            pieChart.setUsePercentValues(true)
            pieChart.description.text = "Monthly Expense Breakdown"
            pieChart.invalidate()
        }
    }
}
