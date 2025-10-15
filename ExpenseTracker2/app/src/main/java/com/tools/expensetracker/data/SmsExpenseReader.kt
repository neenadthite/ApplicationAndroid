package com.tools.expensetracker.utils

import android.content.Context
import android.net.Uri
import com.tools.expensetracker.data.Expense
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SmsExpenseReader {

    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun readBankMessages(context: Context): List<Expense> {
        val smsList = mutableListOf<Expense>()
        val uriSms = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "date", "body")

        val cursor = context.contentResolver.query(uriSms, projection, null, null, "date DESC")
        cursor?.use {
            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val address = it.getString(it.getColumnIndexOrThrow("address"))

                // Simple filters: modify as needed
                if (body.contains("debited", ignoreCase = true) || body.contains("spent", ignoreCase = true)) {
                    val amountRegex = Regex("(?i)(rs\\.?|inr)\\s*([0-9,.]+)")
                    val match = amountRegex.find(body)
                    val amount = match?.groupValues?.get(2)?.replace(",", "")?.toDoubleOrNull() ?: continue

                    val date = LocalDate.now() // use current for simplicity
                    val category = detectCategory(body)
                    val note = "From ${address.take(20)}"

                    smsList.add(Expense(date = date, category = category, amount = amount, note = note))
                }
            }
        }
        return smsList
    }

    private fun detectCategory(body: String): String {
        return when {
            body.contains("fuel", true) -> "Transport"
            body.contains("upi", true) || body.contains("merchant", true) -> "DailyNeeds"
            body.contains("school", true) || body.contains("fee", true) -> "Education"
            else -> "Food"
        }
    }
}
