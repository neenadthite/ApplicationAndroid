package com.tools.worklog


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tools.worklog.R
import com.tools.worklog.Task


class TaskAdapter(
    private val items: MutableList<Task>,
    private val onStartStop: (Task) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {


    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvElapsed: TextView = view.findViewById(R.id.tvElapsed)
        val btnStartStop: Button = view.findViewById(R.id.btnStartStop)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.tvTitle.text = t.title
        val total = if (t.running) t.elapsedMillis + (android.os.SystemClock.elapsedRealtime() - t.lastStart) else t.elapsedMillis
        holder.tvElapsed.text = formatMillis(total)
        holder.btnStartStop.text = if (t.running) "Stop" else "Start"


        holder.btnStartStop.setOnClickListener { onStartStop(t) }
        holder.btnEdit.setOnClickListener { onEdit(t) }
        holder.btnDelete.setOnClickListener { onDelete(t) }
    }


    override fun getItemCount(): Int = items.size


    fun setItems(new: List<Task>) {
        items.clear()
        items.addAll(new)
        notifyDataSetChanged()
    }


    fun addItem(t: Task) {
        items.add(t)
        notifyItemInserted(items.size - 1)
    }


    private fun formatMillis(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}