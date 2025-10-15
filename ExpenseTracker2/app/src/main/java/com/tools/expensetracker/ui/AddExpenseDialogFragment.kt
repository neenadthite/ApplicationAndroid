package com.tools.expensetracker.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.tools.expensetracker.R
import com.tools.expensetracker.data.Expense
import com.tools.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddExpenseDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_expense, null)

        val categorySpinner = view.findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val amountInput = view.findViewById<TextInputEditText>(R.id.amountInput)
        val noteInput = view.findViewById<TextInputEditText>(R.id.noteInput)

        val categories = listOf("Food", "Transport", "Education", "DailyNeeds")
        categorySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        builder.setView(view)
            .setTitle("Add Expense")
            .setPositiveButton("Save") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val note = noteInput.text.toString()
                val category = categorySpinner.selectedItem.toString()

                if (amount != null) {
                    lifecycleScope.launch {
                        val dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
                        val expense = Expense(0,LocalDate.now(), category, amount, note)
                        dao.insert(expense)
                        Toast.makeText(requireContext(), "Expense added", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }
}
