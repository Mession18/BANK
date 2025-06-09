package com.example.incomeplanner.data.database

import androidx.room.TypeConverter
import com.example.incomeplanner.model.PeriodType

class Converters {
    @TypeConverter
    fun fromPeriodType(value: PeriodType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPeriodType(value: String?): PeriodType? {
        return value?.let { PeriodType.valueOf(it) }
    }
}
