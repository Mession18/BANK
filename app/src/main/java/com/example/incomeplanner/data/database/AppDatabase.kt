package com.example.incomeplanner.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.incomeplanner.model.Goal
import com.example.incomeplanner.model.IncomeEntry

@Database(entities = [Goal::class, IncomeEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun incomeEntryDao(): IncomeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "income_planner_database"
                )
                // Add migrations here if schema changes in the future
                // .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
