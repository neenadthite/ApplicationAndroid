package com.tools.expensetracker.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        loadCurrentMonthChart()
        return view
    }

    private fun loadCurrentMonthChart() {
        val now = LocalDate.now()
        val month = "%02d".format(now.monthValue)
        val year = now.year.toString()

        lifecycleScope.launch {
            val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
            val expenses = dao.getByCurrentMonth(month, year)
            val grouped = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }

            val entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Expenses for ${now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}")
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
