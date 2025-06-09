package com.example.incomeplanner.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.incomeplanner.model.Goal

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): Goal?

    @Update
    suspend fun update(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("SELECT * FROM goals ORDER BY startDate DESC")
    suspend fun getAllGoals(): List<Goal>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY startDate DESC")
    suspend fun getActiveGoals(): List<Goal>
}
