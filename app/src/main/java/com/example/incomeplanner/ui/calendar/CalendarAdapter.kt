package com.example.incomeplanner.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.incomeplanner.R

data class CalendarDay(
    val dayOfMonth: String,
    val isCurrentMonth: Boolean,
    val date: Long, // Timestamp for the start of the day
    var dailyIncomeStatus: String, // e.g., "$15 / $20"
    var achieved: Double = 0.0,
    var target: Double = 0.0,
    var isAchieved: Boolean = false,
    var hasSurplus: Boolean = false
)

class CalendarAdapter(private var days: List<CalendarDay>) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayOfMonthTextView: TextView = itemView.findViewById(R.id.dayOfMonthTextView)
        val dailyIncomeTextView: TextView = itemView.findViewById(R.id.dailyIncomeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.calendar_day_cell, parent, false)
        // Potentially set fixed size for items if not done in XML, for grid performance
        // val layoutParams = view.layoutParams
        // layoutParams.height = (parent.height / 6) // Example: if you want 6 rows visible
        // view.layoutParams = layoutParams
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        holder.dayOfMonthTextView.text = day.dayOfMonth

        if (day.isCurrentMonth && day.dailyIncomeStatus.isBlank() && day.target == 0.0 && day.achieved == 0.0) {
            holder.dailyIncomeTextView.text = "-"
        } else {
            holder.dailyIncomeTextView.text = day.dailyIncomeStatus
        }

        holder.itemView.setBackgroundColor(Color.TRANSPARENT) // Reset background

        if (day.isCurrentMonth) {
            holder.dayOfMonthTextView.setTextColor(Color.BLACK)
            holder.itemView.alpha = 1.0f

            if (day.target > 0) { // Only color if there was a target
                if (day.isAchieved) {
                    holder.itemView.setBackgroundColor(Color.parseColor("#A5D6A7")) // Light Green
                }
                if (day.hasSurplus) { // hasSurplus implies isAchieved
                     holder.itemView.setBackgroundColor(Color.parseColor("#81C784")) // Darker Green for surplus
                } else if (day.isAchieved) { // Achieved but not surplus
                    holder.itemView.setBackgroundColor(Color.parseColor("#A5D6A7"))
                }
            } else if (day.achieved > 0 && day.target == 0.0) { // Income with no goal target
                 holder.itemView.setBackgroundColor(Color.parseColor("#E1BEE7")) // Light purple
            }
            // If !isAchieved and target > 0, background remains transparent (or default)

        } else { // Not current month
            holder.dayOfMonthTextView.setTextColor(Color.LTGRAY)
            holder.itemView.alpha = 0.6f
            // Potentially set a different background for non-current month days if desired
            // holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5")); // example: very light grey
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateData(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged() // Consider using DiffUtil for better performance later
    }
}
