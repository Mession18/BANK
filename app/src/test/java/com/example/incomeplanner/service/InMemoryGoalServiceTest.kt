package com.example.incomeplanner.service

import com.example.incomeplanner.model.Goal
import com.example.incomeplanner.model.PeriodType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class InMemoryGoalServiceTest {

    private lateinit var goalService: GoalService

    @Before
    fun setUp() {
        // Use getInMemoryInstance() to specifically test the in-memory logic
        goalService = InMemoryGoalService.getInMemoryInstance()
        // Clear previous data if the instance is a true singleton and shared across tests
        runTest { // Ensure this runs before each test if clearing is needed
            (goalService as InMemoryGoalService).getAllGoals().let { result ->
                 if (result is Result.Success) {
                    result.data.forEach { goalService.deleteGoal(it.id) }
                }
            }
        }
    }

    @Test
    fun `addGoal successfully adds a goal`() = runTest {
        val goal = Goal(title = "Test Goal", amount = 100.0, periodType = PeriodType.MONTHLY, startDate = System.currentTimeMillis())
        val result = goalService.addGoal(goal)
        assertTrue(result is Result.Success)

        val retrieved = goalService.getGoal(goal.id)
        assertTrue(retrieved is Result.Success)
        assertEquals(goal, (retrieved as Result.Success).data)
    }

    @Test
    fun `getGoal returns null for non-existent goal`() = runTest {
        val result = goalService.getGoal(UUID.randomUUID().toString())
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun `updateGoal successfully updates an existing goal`() = runTest {
        val goal = Goal(title = "Initial Title", amount = 50.0, periodType = PeriodType.WEEKLY, startDate = System.currentTimeMillis())
        goalService.addGoal(goal)

        val updatedGoal = goal.copy(title = "Updated Title", amount = 75.0)
        val updateResult = goalService.updateGoal(updatedGoal)
        assertTrue(updateResult is Result.Success)

        val retrieved = goalService.getGoal(goal.id)
        assertTrue(retrieved is Result.Success)
        assertEquals("Updated Title", (retrieved as Result.Success).data?.title)
        assertEquals(75.0, (retrieved as Result.Success).data?.amount, 0.01)
    }

    @Test
    fun `updateGoal fails for non-existent goal`() = runTest {
        val nonExistentGoal = Goal(id = "non-existent-id", title = "Ghost", amount = 10.0, periodType = PeriodType.DAILY, startDate = 0)
        val result = goalService.updateGoal(nonExistentGoal)
        assertTrue(result is Result.Error)
    }


    @Test
    fun `deleteGoal successfully removes a goal`() = runTest {
        val goal = Goal(title = "To Delete", amount = 20.0, periodType = PeriodType.DAILY, startDate = System.currentTimeMillis())
        goalService.addGoal(goal)

        val deleteResult = goalService.deleteGoal(goal.id)
        assertTrue(deleteResult is Result.Success)

        val retrieved = goalService.getGoal(goal.id)
        assertTrue(retrieved is Result.Success)
        assertNull((retrieved as Result.Success).data)
    }

    @Test
    fun `deleteGoal fails for non-existent goal`() = runTest {
        val result = goalService.deleteGoal("non-existent-id")
        assertTrue(result is Result.Error)
    }


    @Test
    fun `getAllGoals returns all added goals`() = runTest {
        val goal1 = Goal(title = "G1", amount = 10.0, periodType = PeriodType.DAILY, startDate = 1L)
        val goal2 = Goal(title = "G2", amount = 20.0, periodType = PeriodType.WEEKLY, startDate = 2L)
        goalService.addGoal(goal1)
        goalService.addGoal(goal2)

        val allGoalsResult = goalService.getAllGoals()
        assertTrue(allGoalsResult is Result.Success)
        val allGoals = (allGoalsResult as Result.Success).data
        assertEquals(2, allGoals.size)
        assertTrue(allGoals.contains(goal1))
        assertTrue(allGoals.contains(goal2))
    }

    @Test
    fun `getActiveGoals returns only active goals`() = runTest {
        val activeGoal = Goal(title = "Active", amount = 100.0, periodType = PeriodType.MONTHLY, startDate = System.currentTimeMillis(), isActive = true)
        val inactiveGoal = Goal(title = "Inactive", amount = 50.0, periodType = PeriodType.DAILY, startDate = System.currentTimeMillis(), isActive = false)
        goalService.addGoal(activeGoal)
        goalService.addGoal(inactiveGoal)

        val activeGoalsResult = goalService.getActiveGoals()
        assertTrue(activeGoalsResult is Result.Success)
        val activeGoals = (activeGoalsResult as Result.Success).data
        assertEquals(1, activeGoals.size)
        assertEquals(activeGoal, activeGoals.first())
    }
}
