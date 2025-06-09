package com.example.incomeplanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "income_entries")
data class IncomeEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val date: Long,
    val description: String?
)
