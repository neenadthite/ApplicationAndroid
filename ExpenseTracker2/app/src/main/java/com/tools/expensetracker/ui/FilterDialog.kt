package com.tools.expensetracker.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.time.LocalDate

class FilterDialog(
    private val context: Context,
    private val onFilter: (from: LocalDate?, to: LocalDate?, category: String?) -> Unit
) {
    fun show() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val fromInput = EditText(context).apply { hint = "From (YYYY-MM-DD)" }
        val toInput = EditText(context).apply { hint = "To (YYYY-MM-DD)" }
        val categoryInput = EditText(context).apply { hint = "Category (optional)" }

        layout.addView(fromInput)
        layout.addView(toInput)
        layout.addView(categoryInput)

        AlertDialog.Builder(context)
            .setTitle("Filter Expenses")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                try {
                    val from = fromInput.text.toString().takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    val to = toInput.text.toString().takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    val category = categoryInput.text.toString().takeIf { it.isNotBlank() }
                    onFilter(from, to, category)
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
