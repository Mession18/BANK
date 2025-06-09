package com.example.incomeplanner.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.incomeplanner.R
import com.example.incomeplanner.logic.DefaultProgressCalculator
import com.example.incomeplanner.model.Goal
import com.example.incomeplanner.model.IncomeEntry
import com.example.incomeplanner.model.PeriodType
import com.example.incomeplanner.service.InMemoryGoalService
import com.example.incomeplanner.service.InMemoryIncomeService
import com.example.incomeplanner.service.Result
import com.example.incomeplanner.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    GlobalScope.launch { // Use GlobalScope for widget updates
        val views = RemoteViews(context.packageName, R.layout.income_widget_layout)
        val currentTime = System.currentTimeMillis()

        // Access services using getInstance() with context
        val goalService = InMemoryGoalService.getInstance(context.applicationContext)
        val incomeService = InMemoryIncomeService.getInstance(context.applicationContext)
        val progressCalculator = DefaultProgressCalculator()

        val goalsResult = goalService.getActiveGoals()
        val incomeResult = incomeService.getAllIncomeEntries()

        if (goalsResult is Result.Success && incomeResult is Result.Success) {
            val activeGoals = goalsResult.data
        val allIncome = incomeResult.data

        // Today's Progress
        val dailyGoals = activeGoals.filter { it.periodType == PeriodType.DAILY }
        val todayProgressMap = progressCalculator.calculateProgress(dailyGoals, allIncome, currentTime)
        val todayGoalProgress = todayProgressMap[PeriodType.DAILY]?.firstOrNull() // Assuming one primary daily goal or sum
        views.setTextViewText(R.id.widgetTodayProgressTextView, formatProgressString("Today", todayGoalProgress))

        // This Week's Progress
        val weeklyGoals = activeGoals.filter { it.periodType == PeriodType.WEEKLY }
        // Ensure currentTimestamp for weekly is start of the week for accurate calculation by DefaultProgressCalculator
        val startOfWeekTs = DateUtil.getStartOfWeek(currentTime)
        val weekProgressMap = progressCalculator.calculateProgress(weeklyGoals, allIncome, startOfWeekTs)
        val weekGoalProgress = weekProgressMap[PeriodType.WEEKLY]?.firstOrNull() // Assuming one primary weekly goal or sum
        views.setTextViewText(R.id.widgetWeekProgressTextView, formatProgressString("This Week", weekGoalProgress))

        // This Month's Progress
        val monthlyGoals = activeGoals.filter { it.periodType == PeriodType.MONTHLY }
         // Ensure currentTimestamp for monthly is start of the month
        val startOfMonthTs = DateUtil.getStartOfMonth(currentTime)
        val monthProgressMap = progressCalculator.calculateProgress(monthlyGoals, allIncome, startOfMonthTs)
        val monthGoalProgress = monthProgressMap[PeriodType.MONTHLY]?.firstOrNull() // Assuming one primary monthly goal or sum
        views.setTextViewText(R.id.widgetMonthProgressTextView, formatProgressString("This Month", monthGoalProgress))

    } else {
        // Handle error case: display N/A or error message
        views.setTextViewText(R.id.widgetTodayProgressTextView, "Today: Error loading data")
        views.setTextViewText(R.id.widgetWeekProgressTextView, "This Week: Error loading data")
        views.setTextViewText(R.id.widgetMonthProgressTextView, "This Month: Error loading data")
    }

    // Update last updated time
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    views.setTextViewText(R.id.widgetLastUpdatedTextView, "Last updated: ${sdf.format(Date(currentTime))}")

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

private fun formatProgressString(prefix: String, progress: com.example.incomeplanner.logic.GoalProgress?): String {
    return if (progress != null) {
        // If there are multiple goals of the same period type, the calculator returns a list.
        // Here we are just taking the first one as an example.
        // A more robust solution might sum them up or pick a "primary" one.
        String.format(
            Locale.getDefault(),
            "%s: $%.2f / $%.2f (%.0f%%)",
            prefix,
            progress.achievedAmount,
            progress.totalTargetAmount,
            progress.progressPercentage
        )
    } else {
        "$prefix: N/A"
    }
}
