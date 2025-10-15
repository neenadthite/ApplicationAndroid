package com.tools.expensetracker.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
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
    private lateinit var dao: com.tools.expensetracker.data.ExpenseDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_category_list, container, false)
        recyclerView = view.findViewById(R.id.categoryRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
        loadCurrentMonthExpenses()
        return view
    }

    private fun loadCurrentMonthExpenses() {
        val now = LocalDate.now()
        val month = "%02d".format(now.monthValue)
        val year = now.year.toString()

        lifecycleScope.launch {
            val list = dao.getByCurrentMonth(month, year)
            recyclerView.adapter = ExpenseAdapter(list) { expense ->
                showEditDialog(expense)
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
                    loadCurrentMonthExpenses()
                    Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
