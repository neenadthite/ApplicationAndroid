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
import java.time.format.TextStyle
import java.util.*

class ChartFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var monthSelector: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        monthSelector = view.findViewById(R.id.monthSelector)
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

        val currentMonthIndex = LocalDate.now().monthValue - 1
        monthSelector.setSelection(currentMonthIndex)

        monthSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val month = months[pos].substring(0, 2)
                loadChartData(month)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChartData(month: String) {
        val year = LocalDate.now().year.toString()

        lifecycleScope.launch {
            val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
            val expenses = dao.getByCurrentMonth(month, year)
            val grouped = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }

            val entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Expenses for ${monthToName(month)}")
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

    private fun monthToName(month: String): String {
        return try {
            val monthInt = month.toInt()
            LocalDate.of(2024, monthInt, 1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
