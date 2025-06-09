package com.example.incomeplanner

import android.app.Application
import com.example.incomeplanner.data.database.AppDatabase

class IncomePlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AppDatabase singleton here to ensure it's ready
        AppDatabase.getDatabase(this)
    }
}
