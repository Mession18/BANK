package com.example.incomeplanner.service

import com.example.incomeplanner.model.Goal

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

interface GoalService {
    suspend fun addGoal(goal: Goal): Result<Unit>
    suspend fun getGoal(id: String): Result<Goal?>
    suspend fun updateGoal(goal: Goal): Result<Unit>
    suspend fun deleteGoal(id: String): Result<Unit>
    suspend fun getAllGoals(): Result<List<Goal>>
    suspend fun getActiveGoals(): Result<List<Goal>>
}

// InMemoryGoalService is kept, but its getInstance might not be the primary one used by the app.
// Or, it can be removed if RoomGoalService is the sole implementation going forward.
// For this refactor, we make its methods suspend and update its companion object.
class InMemoryGoalService private constructor() : GoalService {
    private val goals = mutableListOf<Goal>()

    companion object {
        @Volatile
        private var inMemoryInstance: InMemoryGoalService? = null

        @Volatile
        private var roomInstance: GoalService? = null

        // Get the Room-backed service instance
        fun getInstance(context: android.content.Context): GoalService =
            roomInstance ?: synchronized(this) {
                roomInstance ?: RoomGoalService(com.example.incomeplanner.data.database.AppDatabase.getDatabase(context.applicationContext).goalDao()).also { roomInstance = it }
            }

        // Optional: A way to get the pure in-memory instance, e.g., for tests
        fun getInMemoryInstance(): GoalService =
            inMemoryInstance ?: synchronized(this) {
                inMemoryInstance ?: InMemoryGoalService().also { inMemoryInstance = it }
            }
    }

    override suspend fun addGoal(goal: Goal): Result<Unit> {
        return try {
            goals.add(goal)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGoal(id: String): Result<Goal?> {
        return try {
            Result.Success(goals.find { it.id == id })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            val index = goals.indexOfFirst { it.id == goal.id }
            if (index != -1) {
                goals[index] = goal
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Goal not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteGoal(id: String): Result<Unit> {
        return try {
            if (goals.removeIf { it.id == id }) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Goal not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllGoals(): Result<List<Goal>> {
        return try {
            Result.Success(goals.toList())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getActiveGoals(): Result<List<Goal>> {
        return try {
            Result.Success(goals.filter { it.isActive }.toList())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class RoomGoalService(private val goalDao: com.example.incomeplanner.data.database.GoalDao) : GoalService {
    override suspend fun addGoal(goal: Goal): Result<Unit> {
        return try {
            goalDao.insert(goal)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGoal(id: String): Result<Goal?> {
        return try {
            Result.Success(goalDao.getGoalById(id))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            goalDao.update(goal)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteGoal(id: String): Result<Unit> {
        return try {
            goalDao.deleteGoalById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllGoals(): Result<List<Goal>> {
        return try {
            Result.Success(goalDao.getAllGoals())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getActiveGoals(): Result<List<Goal>> {
        return try {
            Result.Success(goalDao.getActiveGoals())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
