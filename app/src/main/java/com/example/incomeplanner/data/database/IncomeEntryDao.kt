package com.example.incomeplanner.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.incomeplanner.model.IncomeEntry

@Dao
interface IncomeEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: IncomeEntry)

    @Query("SELECT * FROM income_entries WHERE id = :id")
    suspend fun getIncomeEntryById(id: String): IncomeEntry?

    @Update
    suspend fun update(entry: IncomeEntry)

    @Query("DELETE FROM income_entries WHERE id = :id")
    suspend fun deleteIncomeEntryById(id: String)

    @Query("SELECT * FROM income_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getIncomeEntriesForDateRange(startDate: Long, endDate: Long): List<IncomeEntry>

    @Query("SELECT * FROM income_entries ORDER BY date DESC")
    suspend fun getAllIncomeEntries(): List<IncomeEntry>
}
