package com.example.incomeplanner.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.incomeplanner.R

data class WeeklyGoalProgressDisplay(
    val weekDescription: String,
    val progressPercentage: Int,
    val progressText: String,
    val goalTitle: String // Added to show which goal this progress is for
)

class WeeklyGoalAdapter(private var items: List<WeeklyGoalProgressDisplay>) :
    RecyclerView.Adapter<WeeklyGoalAdapter.WeeklyGoalViewHolder>() {

    class WeeklyGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val weekDescriptionTextView: TextView = itemView.findViewById(R.id.weekDescriptionTextView)
        val weeklyProgressBar: ProgressBar = itemView.findViewById(R.id.weeklyProgressBar)
        val weeklyProgressTextView: TextView = itemView.findViewById(R.id.weeklyProgressTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyGoalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.weekly_goal_item, parent, false)
        return WeeklyGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyGoalViewHolder, position: Int) {
        val item = items[position]
        holder.weekDescriptionTextView.text = "${item.goalTitle} (${item.weekDescription})" // Include goal title
        holder.weeklyProgressBar.progress = item.progressPercentage
        holder.weeklyProgressTextView.text = item.progressText
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<WeeklyGoalProgressDisplay>) {
        items = newItems
        notifyDataSetChanged() // Consider DiffUtil later
    }
}
