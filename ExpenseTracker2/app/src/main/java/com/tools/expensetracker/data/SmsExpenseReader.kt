package com.tools.expensetracker.utils

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.tools.expensetracker.data.Expense
import java.util.regex.Pattern

object SmsExpenseReader {

    fun readBankMessages(context: Context): List<Expense> {
        val smsList = mutableListOf<Expense>()
        val uriSms = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY
        )

        // Query SMS inbox, most recent first
        val cursor = context.contentResolver.query(
            uriSms,
            projection,
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val address = it.getString(addressIdx) ?: "Unknown"
                val smsTimestamp = it.getLong(dateIdx) //  actual SMS timestamp in millis

                // Filter bank-like or transactional messages
                if (body.contains("debited", true) || body.contains("spent", true) ||
                    body.contains("purchase", true) || body.contains("txn", true)) {

                    val amount = extractAmount(body)
                    if (amount <= 0) continue

                    val category = detectCategory(body)
                    val note = "From ${address.take(20)}"

                    // store actual timestamp (not LocalDate)
                    val expense = Expense(
                        date = java.time.Instant.ofEpochMilli(smsTimestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate(),
                        category = category,
                        amount = amount,
                        note = note
                    )

                    smsList.add(expense)
                }
            }
        }

        return smsList
    }

    private fun extractAmount(body: String): Double {
        val pattern = Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,.]+)")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        } else 0.0
    }

    private fun detectCategory(body: String): String {
        return when {
            body.contains("fuel", true) || body.contains("petrol", true) || body.contains("IRCTC", true) -> "Transport"
            body.contains("upi", true) || body.contains("merchant", true) || body.contains("grocery", true) -> "DailyNeeds"
            body.contains("school", true) || body.contains("fee", true) || body.contains("book", true) -> "Education"
            body.contains("restaurant", true) || body.contains("food", true) || body.contains("swiggy", true) || body.contains("zomato", true) -> "Food"
            else -> "Others"
        }
    }
}
