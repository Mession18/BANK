package com.example.incomeplanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.incomeplanner.logic.DefaultProgressCalculator
import com.example.incomeplanner.model.PeriodType
import com.example.incomeplanner.service.GoalService
import com.example.incomeplanner.service.InMemoryGoalService
import com.example.incomeplanner.service.InMemoryIncomeService
import com.example.incomeplanner.service.IncomeService
import com.example.incomeplanner.ui.calendar.CalendarAdapter
import com.example.incomeplanner.ui.calendar.CalendarDay
import com.example.incomeplanner.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanActivity : AppCompatActivity() {

    private lateinit var monthYearTextView: TextView
    private lateinit var prevMonthButton: Button
    private lateinit var nextMonthButton: Button
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthlyGoalSummaryTextView: TextView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var weeklyGoalsRecyclerView: RecyclerView
    private lateinit var weeklyGoalAdapter: com.example.incomeplanner.ui.calendar.WeeklyGoalAdapter
    private lateinit var calendarEmptyStateTextView: TextView
    private lateinit var weeklyGoalsEmptyStateTextView: TextView
    private lateinit var loadingProgressBar: android.widget.ProgressBar

    private var currentCalendar: Calendar = Calendar.getInstance()

    // Services and Calculator
    private val goalService: GoalService by lazy { InMemoryGoalService.getInstance(applicationContext) }
    private val incomeService: IncomeService by lazy { InMemoryIncomeService.getInstance(applicationContext) }
    private val progressCalculator: DefaultProgressCalculator = DefaultProgressCalculator()

    private lateinit var addIncomeFab: com.google.android.material.floatingactionbutton.FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        // Initialize services here if not using by lazy or if context is strictly needed at this point
        // However, by lazy with applicationContext should be fine.

        monthYearTextView = findViewById(R.id.monthYearTextView)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        monthlyGoalSummaryTextView = findViewById(R.id.monthlyGoalSummaryTextView)
        weeklyGoalsRecyclerView = findViewById(R.id.weeklyGoalsRecyclerView)
        calendarEmptyStateTextView = findViewById(R.id.calendarEmptyStateTextView)
        weeklyGoalsEmptyStateTextView = findViewById(R.id.weeklyGoalsEmptyStateTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Setup Calendar RecyclerView
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(emptyList())
        calendarRecyclerView.adapter = calendarAdapter

        // Setup Weekly Goals RecyclerView
        weeklyGoalsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        weeklyGoalAdapter = com.example.incomeplanner.ui.calendar.WeeklyGoalAdapter(emptyList())
        weeklyGoalsRecyclerView.adapter = weeklyGoalAdapter

        addIncomeFab = findViewById(R.id.addIncomeFab)

        // Setup button listeners
        prevMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateCalendarView()
        }

        nextMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendarView()
        }

        addIncomeFab.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.incomeplanner.ui.AddIncomeActivity::class.java))
        }

        // Add some dummy data for testing
        // addSampleData() // Optional: Call this to populate with some test goals/income
        updateCalendarView()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the activity
        updateCalendarView()
    }

    private fun updateCalendarView() {
        loadingProgressBar.visibility = android.view.View.VISIBLE
        calendarRecyclerView.visibility = android.view.View.GONE
        // Keep weekly goals recycler view hidden initially too, its state will be set in updateWeeklyGoalsDisplay
        weeklyGoalsRecyclerView.visibility = android.view.View.GONE
        calendarEmptyStateTextView.visibility = android.view.View.GONE
        weeklyGoalsEmptyStateTextView.visibility = android.view.View.GONE

        androidx.lifecycle.lifecycleScope.launch {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            monthYearTextView.text = sdf.format(currentCalendar.time)

            val allGoalsResult = goalService.getAllGoals()
            val allIncomeResult = incomeService.getAllIncomeEntries()

            if (allGoalsResult is com.example.incomeplanner.service.Result.Error || allIncomeResult is com.example.incomeplanner.service.Result.Error) {
                monthlyGoalSummaryTextView.text = "Error loading data"
                calendarAdapter.updateData(emptyList())
                weeklyGoalAdapter.updateData(emptyList())
                loadingProgressBar.visibility = android.view.View.GONE
                calendarEmptyStateTextView.visibility = android.view.View.VISIBLE
                // Also show weekly empty state in case of general error
                weeklyGoalsEmptyStateTextView.visibility = android.view.View.VISIBLE
                return@launch
            }

            val goals = (allGoalsResult as com.example.incomeplanner.service.Result.Success).data
            val incomeEntries = (allIncomeResult as com.example.incomeplanner.service.Result.Success).data

            // --- Monthly Goal Summary ---
            val monthStart = DateUtil.getStartOfMonth(currentCalendar.timeInMillis)
        val monthEnd = DateUtil.getEndOfMonth(currentCalendar.timeInMillis)

        val monthlyGoals = goals.filter { it.periodType == PeriodType.MONTHLY && it.isActive && it.startDate <= monthEnd }
        val monthlyProgressMap = progressCalculator.calculateProgress(monthlyGoals, incomeEntries, currentCalendar.timeInMillis)

        val totalMonthlyAchieved = monthlyProgressMap[PeriodType.MONTHLY]?.sumOf { it.achievedAmount } ?: 0.0
        val totalMonthlyTarget = monthlyProgressMap[PeriodType.MONTHLY]?.sumOf { it.totalTargetAmount } ?: 0.0
        val monthlyPercentage = if (totalMonthlyTarget > 0) (totalMonthlyAchieved / totalMonthlyTarget) * 100 else 0.0
        monthlyGoalSummaryTextView.text = String.format(Locale.getDefault(), "Month: $%.2f / $%.2f (%.0f%%)",
            totalMonthlyAchieved, totalMonthlyTarget, monthlyPercentage)


        // --- Calendar Day Cells ---
        val days = ArrayList<CalendarDay>()
        val calendar = currentCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonthInWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val daysInPrevMonthToAdd = if (calendar.firstDayOfWeek == Calendar.SUNDAY) {
            (firstDayOfMonthInWeek - Calendar.SUNDAY + 7) % 7
        } else {
            (firstDayOfMonthInWeek - Calendar.MONDAY + 7) % 7
        }

        val prevMonthCalendar = calendar.clone() as Calendar
        prevMonthCalendar.add(Calendar.MONTH, -1)
        prevMonthCalendar.set(Calendar.DAY_OF_MONTH, prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) - daysInPrevMonthToAdd + 1)

        for (i in 0 until daysInPrevMonthToAdd) {
            val dayTimestamp = prevMonthCalendar.timeInMillis
            val dailyStatusData = getDailyStatusData(goals, incomeEntries, dayTimestamp)
            days.add(CalendarDay(
                dayOfMonth = prevMonthCalendar.get(Calendar.DAY_OF_MONTH).toString(),
                isCurrentMonth = false,
                date = dayTimestamp,
                dailyIncomeStatus = dailyStatusData.statusString,
                achieved = dailyStatusData.achieved,
                target = dailyStatusData.target,
                isAchieved = dailyStatusData.isAchieved,
                hasSurplus = dailyStatusData.hasSurplus
            ))
            prevMonthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            val dayTimestamp = calendar.timeInMillis
            val dailyStatusData = getDailyStatusData(goals, incomeEntries, dayTimestamp)
            days.add(CalendarDay(
                dayOfMonth = i.toString(),
                isCurrentMonth = true,
                date = dayTimestamp,
                dailyIncomeStatus = dailyStatusData.statusString,
                achieved = dailyStatusData.achieved,
                target = dailyStatusData.target,
                isAchieved = dailyStatusData.isAchieved,
                hasSurplus = dailyStatusData.hasSurplus
            ))
        }

        val daysInNextMonthToAdd = (7 - (days.size % 7)) % 7
        val nextMonthCalendar = currentCalendar.clone() as Calendar
        nextMonthCalendar.add(Calendar.MONTH, 1)
        nextMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        for (i in 0 until daysInNextMonthToAdd) {
            val dayTimestamp = nextMonthCalendar.timeInMillis
            val dailyStatusData = getDailyStatusData(goals, incomeEntries, dayTimestamp)
            days.add(CalendarDay(
                dayOfMonth = nextMonthCalendar.get(Calendar.DAY_OF_MONTH).toString(),
                isCurrentMonth = false,
                date = dayTimestamp,
                dailyIncomeStatus = dailyStatusData.statusString,
                achieved = dailyStatusData.achieved,
                target = dailyStatusData.target,
                isAchieved = dailyStatusData.isAchieved,
                hasSurplus = dailyStatusData.hasSurplus
            ))
            nextMonthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // This is where data is ready for calendar
        // loadingProgressBar.visibility = android.view.View.GONE // Moved to end of block

        // Check if there's any meaningful data to display in the calendar.
        // This could be based on if there are any goals for the month, or any income entries in 'days'.
        // A simple check: if 'goals' affecting the current month are empty and 'incomeEntries' for the month are empty.
        // For now, we'll check if 'days' has any items with income or target.
        val hasCalendarData = days.any { it.achieved > 0 || it.target > 0 } ||
                              goals.any { it.periodType == PeriodType.MONTHLY && it.isActive && DateUtil.getStartOfMonth(currentCalendar.timeInMillis) <= it.startDate && it.startDate <= DateUtil.getEndOfMonth(currentCalendar.timeInMillis)}


        if (!hasCalendarData && goals.none{ it.periodType == PeriodType.MONTHLY}) { // More specific check
            calendarEmptyStateTextView.visibility = android.view.View.VISIBLE
            calendarRecyclerView.visibility = android.view.View.GONE
        } else {
            calendarEmptyStateTextView.visibility = android.view.View.GONE
            calendarRecyclerView.visibility = android.view.View.VISIBLE
            calendarAdapter.updateData(days)
        }

        updateWeeklyGoalsDisplay(goals, incomeEntries, currentCalendar.clone() as Calendar)

        // Hide loading bar after all UI updates are staged
        loadingProgressBar.visibility = android.view.View.GONE
        }
    }

    private data class DailyStatusData(
        val statusString: String,
        val achieved: Double,
        val target: Double,
        val isAchieved: Boolean,
        val hasSurplus: Boolean
    )

    private fun getDailyStatusData(goals: List<com.example.incomeplanner.model.Goal>, incomeEntries: List<com.example.incomeplanner.model.IncomeEntry>, dayTimestamp: Long): DailyStatusData {
        val dailyGoals = goals.filter { it.periodType == PeriodType.DAILY && it.isActive && it.startDate <= dayTimestamp }

        var totalDailyAchieved = 0.0
        var totalDailyTarget = 0.0
        var isAchievedOverall = false
        var hasSurplusOverall = false

        if (dailyGoals.isNotEmpty()) {
            val progressData = progressCalculator.calculateProgress(dailyGoals, incomeEntries, dayTimestamp)
            val dailyProgressList = progressData[PeriodType.DAILY] ?: emptyList()

            if (dailyProgressList.isNotEmpty()) {
                totalDailyAchieved = dailyProgressList.sumOf { it.achievedAmount }
                totalDailyTarget = dailyProgressList.sumOf { it.totalTargetAmount }
                isAchievedOverall = totalDailyTarget > 0 && totalDailyAchieved >= totalDailyTarget
                hasSurplusOverall = totalDailyTarget > 0 && totalDailyAchieved > totalDailyTarget
            }
        } else {
             val relevantEntries = incomeEntries.filter { it.date >= DateUtil.getStartOfDay(dayTimestamp) && it.date <= DateUtil.getEndOfDay(dayTimestamp) }
             totalDailyAchieved = relevantEntries.sumOf { it.amount }
        }

        val statusString = if (totalDailyTarget > 0) {
            val percentage = (totalDailyAchieved / totalDailyTarget * 100).toInt()
            String.format(Locale.getDefault(), "$%.2f / $%.2f (%d%%)", totalDailyAchieved, totalDailyTarget, percentage)
        } else if (totalDailyAchieved > 0) {
            String.format(Locale.getDefault(), "$%.2f (No Goal)", totalDailyAchieved)
        } else {
            ""
        }

        return DailyStatusData(statusString, totalDailyAchieved, totalDailyTarget, isAchievedOverall, hasSurplusOverall)
    }

     private fun updateWeeklyGoalsDisplay(
        allGoals: List<com.example.incomeplanner.model.Goal>,
        allIncomeEntries: List<com.example.incomeplanner.model.IncomeEntry>,
        calendarForMonth: Calendar
    ) {
        val weeklyGoalItems = mutableListOf<com.example.incomeplanner.ui.calendar.WeeklyGoalProgressDisplay>()
        val tempCalendar = calendarForMonth.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1) // Start from the first day of the month

        // Set to the first day of the week for the first week of the month
        tempCalendar.set(Calendar.DAY_OF_WEEK, tempCalendar.firstDayOfWeek)
        // If the first day of the week is in the previous month, and we are in the first week of current month,
        // we still want to show this week if it contains days from current month.
        // The DateUtil.getStartOfWeek should correctly give us the start of the week, even if it's in the previous month.

        val monthBeingDisplayed = calendarForMonth.get(Calendar.MONTH)
        val yearBeingDisplayed = calendarForMonth.get(Calendar.YEAR)

        val sdfWeek = SimpleDateFormat("MMM d", Locale.getDefault())
        val processedWeeks = mutableSetOf<String>() // To avoid duplicate processing of a week

        // Iterate up to 6 weeks to cover all possible layouts of a month within weeks
        for (weekIteration in 0..5) {
            val weekStartTimestamp = DateUtil.getStartOfWeek(tempCalendar.timeInMillis)
            val weekEndTimestamp = DateUtil.getEndOfWeek(tempCalendar.timeInMillis)

            // Check if this week is relevant to the month being displayed
            // A week is relevant if it overlaps with the current month at all
            val weekStartCal = DateUtil.getCalendarInstance(weekStartTimestamp)
            val weekEndCal = DateUtil.getCalendarInstance(weekEndTimestamp)

            val weekIdentifier = "${weekStartCal.get(Calendar.YEAR)}-${weekStartCal.get(Calendar.WEEK_OF_YEAR)}"

            if (!processedWeeks.contains(weekIdentifier) &&
                (weekStartCal.get(Calendar.MONTH) == monthBeingDisplayed && weekStartCal.get(Calendar.YEAR) == yearBeingDisplayed ||
                 weekEndCal.get(Calendar.MONTH) == monthBeingDisplayed && weekEndCal.get(Calendar.YEAR) == yearBeingDisplayed ||
                 (weekStartCal.before(calendarForMonth) && weekEndCal.after(calendarForMonth))) // Week spans over the first day of the month
            ) {
                processedWeeks.add(weekIdentifier)
                val weeklyGoalsForThisPeriod = allGoals.filter {
                    it.periodType == PeriodType.WEEKLY && it.isActive && it.startDate <= weekEndTimestamp
                }

                if (weeklyGoalsForThisPeriod.isNotEmpty()) {
                    val progressDataMap = progressCalculator.calculateProgress(
                        weeklyGoalsForThisPeriod,
                        allIncomeEntries,
                        weekStartTimestamp
                    )

                    progressDataMap[PeriodType.WEEKLY]?.forEach { progress ->
                        val weekDesc = "${sdfWeek.format(weekStartTimestamp)} - ${sdfWeek.format(weekEndTimestamp)}"
                        weeklyGoalItems.add(com.example.incomeplanner.ui.calendar.WeeklyGoalProgressDisplay(
                            weekDescription = weekDesc,
                            progressPercentage = progress.progressPercentage.toInt(),
                            progressText = String.format(Locale.getDefault(), "$%.2f / $%.2f (%.0f%%)",
                                progress.achievedAmount, progress.totalTargetAmount, progress.progressPercentage),
                            goalTitle = progress.goal.title ?: "Weekly Goal"
                        ))
                    }
                }
            }
            // Move to the next week
            tempCalendar.timeInMillis = weekStartTimestamp // Ensure tempCalendar is at the start of the current week
            tempCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            // If we moved into a week that is entirely in the next month, and the start of that week is after the displayed month's end, stop.
            // If we moved into a week that is entirely in the next month, and the start of that week is after the displayed month's end, stop.
            if (tempCalendar.get(Calendar.MONTH) != monthBeingDisplayed && tempCalendar.timeInMillis > DateUtil.getEndOfMonth(calendarForMonth.timeInMillis)) {
                 if (DateUtil.getStartOfWeek(tempCalendar.timeInMillis) > DateUtil.getEndOfMonth(calendarForMonth.timeInMillis)) break
            }
        }

        if (weeklyGoalItems.isEmpty()) {
            weeklyGoalsEmptyStateTextView.visibility = android.view.View.VISIBLE
            weeklyGoalsRecyclerView.visibility = android.view.View.GONE
        } else {
            weeklyGoalsEmptyStateTextView.visibility = android.view.View.GONE
            weeklyGoalsRecyclerView.visibility = android.view.View.VISIBLE
            weeklyGoalAdapter.updateData(weeklyGoalItems.distinctBy { it.goalTitle + it.weekDescription })
        }
    }


    // Optional: Method to add sample data for testing
    /*
    private fun addSampleData() {
        val today = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = today

        // Sample daily goal
        goalService.addGoal(com.example.incomeplanner.model.Goal(title = "Daily Coffee Money", amount = 5.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today)))

        // Sample weekly goal
        goalService.addGoal(com.example.incomeplanner.model.Goal(title = "Weekly Groceries", amount = 75.0, periodType = PeriodType.WEEKLY, startDate = DateUtil.getStartOfWeek(today)))

        // Sample monthly goal
        goalService.addGoal(com.example.incomeplanner.model.Goal(title = "Monthly Rent", amount = 800.0, periodType = PeriodType.MONTHLY, startDate = DateUtil.getStartOfMonth(today)))

        // Sample income
        incomeService.addIncomeEntry(com.example.incomeplanner.model.IncomeEntry(amount = 10.0, date = today, description = "Freelance tip"))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        incomeService.addIncomeEntry(com.example.incomeplanner.model.IncomeEntry(amount = 4.0, date = calendar.timeInMillis, description = "Found change"))
        calendar.add(Calendar.DAY_OF_YEAR, -5)
        incomeService.addIncomeEntry(com.example.incomeplanner.model.IncomeEntry(amount = 80.0, date = calendar.timeInMillis, description = "Side hustle payout"))
    }
    */
}
