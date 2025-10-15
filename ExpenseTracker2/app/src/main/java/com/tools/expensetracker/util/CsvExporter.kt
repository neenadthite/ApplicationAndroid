package com.tools.expensetracker.util

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.tools.expensetracker.data.Expense
import java.io.File
import java.io.FileWriter

object CsvExporter {
    fun export(context: Context, expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "expenses_export.csv")

        try {
            FileWriter(file).use { writer ->
                writer.append("ID,Date,Category,Amount,Note\n")
                expenses.forEach {
                    writer.append("${it.id},${it.date},${it.category},${it.amount},${it.note}\n")
                }
            }
            Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
