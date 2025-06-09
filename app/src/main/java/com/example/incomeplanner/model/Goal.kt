package com.example.incomeplanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String?,
    val amount: Double,
    val periodType: PeriodType,
    val startDate: Long,
    val isActive: Boolean = true
)
