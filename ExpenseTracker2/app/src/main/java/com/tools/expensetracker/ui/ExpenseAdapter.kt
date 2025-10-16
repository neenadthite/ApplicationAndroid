package com.tools.expensetracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tools.expensetracker.R
import com.tools.expensetracker.data.Expense

/**
 * RecyclerView Adapter for displaying expenses
 * Supports item click for editing an expense
 */
class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.itemDate)
        val categoryText: TextView = itemView.findViewById(R.id.itemCategory)
        val amountText: TextView = itemView.findViewById(R.id.itemAmount)
        val noteText: TextView = itemView.findViewById(R.id.itemNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.dateText.text = expense.date.toString()
        holder.categoryText.text = expense.category
        holder.amountText.text = "₹%.2f".format(expense.amount)
        holder.noteText.text = expense.note

        // handle click to edit
        holder.itemView.setOnClickListener {
            onItemClick(expense)
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun removeExpense(position: Int): Expense {
        val expense = expenses.toMutableList().removeAt(position)
        notifyItemRemoved(position)
        return expense
    }
}

