package com.example.incomeplanner.logic

import com.example.incomeplanner.model.Goal
import com.example.incomeplanner.model.IncomeEntry
import com.example.incomeplanner.model.PeriodType
import com.example.incomeplanner.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class DefaultProgressCalculatorTest {

    private lateinit var calculator: DefaultProgressCalculator

    // Helper to create a specific timestamp
    private fun getTimestamp(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Before
    fun setUp() {
        calculator = DefaultProgressCalculator()
    }

    @Test
    fun `calculateProgress with no goals and no income`() {
        val currentTimestamp = getTimestamp(2024, Calendar.JUNE, 26)
        val progressMap = calculator.calculateProgress(emptyList(), emptyList(), currentTimestamp)
        assertTrue(progressMap.isEmpty())
    }

    // --- Daily Goal Tests ---
    @Test
    fun `daily goal active, no income`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val dailyGoal = Goal(title = "Daily Spend", amount = 20.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today))
        val goals = listOf(dailyGoal)

        val progressMap = calculator.calculateProgress(goals, emptyList(), today)

        assertFalse(progressMap.isEmpty())
        val dailyProgressList = progressMap[PeriodType.DAILY]
        assertEquals(1, dailyProgressList?.size)
        val progress = dailyProgressList?.first()!!
        assertEquals(0.0, progress.achievedAmount, 0.01)
        assertEquals(20.0, progress.totalTargetAmount, 0.01)
        assertEquals(0.0, progress.progressPercentage, 0.01)
        assertFalse(progress.isAchieved)
    }

    @Test
    fun `daily goal, income matches goal`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val dailyGoal = Goal(title = "Daily Coffee", amount = 5.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today))
        val income = listOf(IncomeEntry(amount = 5.0, date = today, description = "Coffee money"))

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), income, today)
        val progress = progressMap[PeriodType.DAILY]?.first()!!

        assertEquals(5.0, progress.achievedAmount, 0.01)
        assertEquals(5.0, progress.totalTargetAmount, 0.01)
        assertEquals(100.0, progress.progressPercentage, 0.01)
        assertTrue(progress.isAchieved)
        assertEquals(0.0, progress.surplusAmount, 0.01)
    }

    @Test
    fun `daily goal, income exceeds goal (surplus)`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val dailyGoal = Goal(title = "Lunch", amount = 10.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today))
        val income = listOf(IncomeEntry(amount = 15.0, date = today, description = "Lunch budget met"))

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), income, today)
        val progress = progressMap[PeriodType.DAILY]?.first()!!

        assertEquals(15.0, progress.achievedAmount, 0.01)
        assertEquals(10.0, progress.totalTargetAmount, 0.01)
        assertEquals(150.0, progress.progressPercentage, 0.01)
        assertTrue(progress.isAchieved)
        assertEquals(5.0, progress.surplusAmount, 0.01)
    }

    @Test
    fun `daily goal, income less than goal`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val dailyGoal = Goal(title = "Snacks", amount = 7.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today))
        val income = listOf(IncomeEntry(amount = 3.0, date = today, description = "Bought a small snack"))

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), income, today)
        val progress = progressMap[PeriodType.DAILY]?.first()!!

        assertEquals(3.0, progress.achievedAmount, 0.01)
        assertEquals(7.0, progress.totalTargetAmount, 0.01)
        assertTrue(progress.progressPercentage < 100.0 && progress.progressPercentage > 0)
        assertFalse(progress.isAchieved)
    }

    @Test
    fun `daily goal not active for current day (startDate in future)`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val tomorrow = getTimestamp(2024, Calendar.JUNE, 27)
        val dailyGoal = Goal(title = "Future Goal", amount = 10.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(tomorrow))

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), emptyList(), today)
        assertTrue(progressMap[PeriodType.DAILY]?.isEmpty() ?: true)
    }

    @Test
    fun `daily goal not active (isActive false)`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        val dailyGoal = Goal(title = "Inactive Goal", amount = 10.0, periodType = PeriodType.DAILY, startDate = DateUtil.getStartOfDay(today), isActive = false)

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), emptyList(), today)
        assertTrue(progressMap[PeriodType.DAILY]?.isEmpty() ?: true)
    }

    // --- Weekly Goal Tests ---
    @Test
    fun `weekly goal, income meets goal spread across week`() {
        val currentMonday = getTimestamp(2024, Calendar.JUNE, 24) // Monday
        val weeklyGoal = Goal(title = "Groceries", amount = 100.0, periodType = PeriodType.WEEKLY, startDate = DateUtil.getStartOfWeek(currentMonday))
        val income = listOf(
            IncomeEntry(amount = 50.0, date = getTimestamp(2024, Calendar.JUNE, 24), description = "Aldi trip"), // Monday
            IncomeEntry(amount = 50.0, date = getTimestamp(2024, Calendar.JUNE, 27), description = "Trader Joes") // Thursday
        )

        // Calculate progress for any day within that week, e.g., Wednesday
        val progressMap = calculator.calculateProgress(listOf(weeklyGoal), income, getTimestamp(2024, Calendar.JUNE, 26))
        val progress = progressMap[PeriodType.WEEKLY]?.first()!!

        assertEquals(100.0, progress.achievedAmount, 0.01)
        assertEquals(100.0, progress.totalTargetAmount, 0.01)
        assertEquals(100.0, progress.progressPercentage, 0.01)
        assertTrue(progress.isAchieved)
    }

    @Test
    fun `weekly goal, income only from outside the current week for calculation`() {
        val currentMonday = getTimestamp(2024, Calendar.JUNE, 24) // Monday
        val nextMonday = getTimestamp(2024, Calendar.JULY, 1)
        val weeklyGoal = Goal(title = "Entertainment", amount = 50.0, periodType = PeriodType.WEEKLY, startDate = DateUtil.getStartOfWeek(currentMonday))
        val income = listOf(
            IncomeEntry(amount = 60.0, date = nextMonday, description = "Concert next week")
        )

        // Calculate progress for a day in the first week
        val progressMap = calculator.calculateProgress(listOf(weeklyGoal), income, getTimestamp(2024, Calendar.JUNE, 26))
        val progress = progressMap[PeriodType.WEEKLY]?.first()!!

        assertEquals(0.0, progress.achievedAmount, 0.01) // No income from *this* week
        assertEquals(50.0, progress.totalTargetAmount, 0.01)
        assertFalse(progress.isAchieved)
    }


    // --- Monthly Goal Tests ---
    @Test
    fun `monthly goal, income meets goal`() {
        val currentDayInMonth = getTimestamp(2024, Calendar.JUNE, 15)
        val monthlyGoal = Goal(title = "Rent", amount = 1000.0, periodType = PeriodType.MONTHLY, startDate = DateUtil.getStartOfMonth(currentDayInMonth))
        val income = listOf(
            IncomeEntry(amount = 700.0, date = getTimestamp(2024, Calendar.JUNE, 1), description = "Paycheck 1"),
            IncomeEntry(amount = 300.0, date = getTimestamp(2024, Calendar.JUNE, 15), description = "Paycheck 2")
        )

        val progressMap = calculator.calculateProgress(listOf(monthlyGoal), income, currentDayInMonth)
        val progress = progressMap[PeriodType.MONTHLY]?.first()!!

        assertEquals(1000.0, progress.achievedAmount, 0.01)
        assertEquals(1000.0, progress.totalTargetAmount, 0.01)
        assertEquals(100.0, progress.progressPercentage, 0.01)
        assertTrue(progress.isAchieved)
    }

    @Test
    fun `goal startDate after current period end`() {
        val today = getTimestamp(2024, Calendar.JUNE, 26)
        // Goal starts next month
        val dailyGoal = Goal(
            title = "Future Daily",
            amount = 10.0,
            periodType = PeriodType.DAILY,
            startDate = DateUtil.getStartOfDay(getTimestamp(2024, Calendar.JULY, 1))
        )

        val progressMap = calculator.calculateProgress(listOf(dailyGoal), emptyList(), today)
        assertTrue(progressMap[PeriodType.DAILY]?.isEmpty() ?: true) // Should not be considered for today
    }
}
