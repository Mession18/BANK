package com.example.incomeplanner.logic

import com.example.incomeplanner.model.Goal
import com.example.incomeplanner.model.IncomeEntry
import com.example.incomeplanner.model.PeriodType
import com.example.incomeplanner.util.DateUtil
import java.util.concurrent.TimeUnit

data class GoalProgress(
    val goal: Goal,
    val achievedAmount: Double,
    val totalTargetAmount: Double,
    val progressPercentage: Double,
    val surplusAmount: Double,
    val isAchieved: Boolean,
    val relevantEntries: List<IncomeEntry>
)

interface ProgressCalculator {
    fun calculateProgress(
        goals: List<Goal>,
        incomeEntries: List<IncomeEntry>,
        currentTimestamp: Long
    ): Map<PeriodType, List<GoalProgress>>
}

class DefaultProgressCalculator : ProgressCalculator {

    override fun calculateProgress(
        goals: List<Goal>,
        incomeEntries: List<IncomeEntry>,
        currentTimestamp: Long
    ): Map<PeriodType, List<GoalProgress>> {
        val activeGoals = goals.filter { it.isActive && it.startDate <= currentTimestamp }
        val progressMap = mutableMapOf<PeriodType, MutableList<GoalProgress>>()

        activeGoals.forEach { goal ->
            val (periodStart, periodEnd, targetAmountMultiplier) = when (goal.periodType) {
                PeriodType.DAILY -> Triple(
                    DateUtil.getStartOfDay(currentTimestamp),
                    DateUtil.getEndOfDay(currentTimestamp),
                    1.0
                )
                PeriodType.WEEKLY -> Triple(
                    DateUtil.getStartOfWeek(currentTimestamp),
                    DateUtil.getEndOfWeek(currentTimestamp),
                    1.0
                )
                PeriodType.MONTHLY -> Triple(
                    DateUtil.getStartOfMonth(currentTimestamp),
                    DateUtil.getEndOfMonth(currentTimestamp),
                    1.0
                )
            }

            // Filter entries relevant to the goal's start date and the current period
            val relevantEntries = incomeEntries.filter {
                it.date in periodStart..periodEnd && it.date >= goal.startDate
            }

            val achievedAmount = relevantEntries.sumOf { it.amount }
            val totalTargetAmount = goal.amount * targetAmountMultiplier // For now, multiplier is 1.0

            val progressPercentage = if (totalTargetAmount > 0) {
                (achievedAmount / totalTargetAmount) * 100
            } else {
                if (achievedAmount > 0) 100.0 else 0.0 // Achieved something with no target
            }
            val surplusAmount = maxOf(0.0, achievedAmount - totalTargetAmount)
            val isAchieved = achievedAmount >= totalTargetAmount

            val goalProgress = GoalProgress(
                goal = goal,
                achievedAmount = achievedAmount,
                totalTargetAmount = totalTargetAmount,
                progressPercentage = progressPercentage,
                surplusAmount = surplusAmount,
                isAchieved = isAchieved,
                relevantEntries = relevantEntries
            )

            progressMap.getOrPut(goal.periodType) { mutableListOf() }.add(goalProgress)
        }
        return progressMap
    }
}
