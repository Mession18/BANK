package com.example.incomeplanner.service

import com.example.incomeplanner.model.IncomeEntry

// Assuming Result is in the same package or imported
// sealed class Result<out T> {
//     data class Success<out T>(val data: T) : Result<T>()
//     data class Error(val exception: Exception) : Result<Nothing>()
// }

interface IncomeService {
    suspend fun addIncomeEntry(entry: IncomeEntry): Result<Unit>
    suspend fun getIncomeEntry(id: String): Result<IncomeEntry?>
    suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit>
    suspend fun deleteIncomeEntry(id: String): Result<Unit>
    suspend fun getIncomeEntriesForDateRange(startDate: Long, endDate: Long): Result<List<IncomeEntry>>
    suspend fun getAllIncomeEntries(): Result<List<IncomeEntry>>
}

class InMemoryIncomeService private constructor() : IncomeService {
    private val incomeEntries = mutableListOf<IncomeEntry>()

    companion object {
        @Volatile
        private var inMemoryInstance: InMemoryIncomeService? = null

        @Volatile
        private var roomInstance: IncomeService? = null

        fun getInstance(context: android.content.Context): IncomeService =
            roomInstance ?: synchronized(this) {
                roomInstance ?: RoomIncomeService(com.example.incomeplanner.data.database.AppDatabase.getDatabase(context.applicationContext).incomeEntryDao()).also { roomInstance = it }
            }

        fun getInMemoryInstance(): IncomeService =
            inMemoryInstance ?: synchronized(this) {
                inMemoryInstance ?: InMemoryIncomeService().also { inMemoryInstance = it }
            }
    }

    override suspend fun addIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return try {
            incomeEntries.add(entry)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getIncomeEntry(id: String): Result<IncomeEntry?> {
        return try {
            Result.Success(incomeEntries.find { it.id == id })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return try {
            val index = incomeEntries.indexOfFirst { it.id == entry.id }
            if (index != -1) {
                incomeEntries[index] = entry
                Result.Success(Unit)
            } else {
                Result.Error(Exception("IncomeEntry not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteIncomeEntry(id: String): Result<Unit> {
        return try {
            if (incomeEntries.removeIf { it.id == id }) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("IncomeEntry not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getIncomeEntriesForDateRange(startDate: Long, endDate: Long): Result<List<IncomeEntry>> {
        return try {
            Result.Success(incomeEntries.filter { it.date in startDate..endDate }.toList())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllIncomeEntries(): Result<List<IncomeEntry>> {
        return try {
            Result.Success(incomeEntries.toList())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class RoomIncomeService(private val incomeEntryDao: com.example.incomeplanner.data.database.IncomeEntryDao) : IncomeService {
    override suspend fun addIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return try {
            incomeEntryDao.insert(entry)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getIncomeEntry(id: String): Result<IncomeEntry?> {
        return try {
            Result.Success(incomeEntryDao.getIncomeEntryById(id))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return try {
            incomeEntryDao.update(entry)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteIncomeEntry(id: String): Result<Unit> {
        return try {
            incomeEntryDao.deleteIncomeEntryById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getIncomeEntriesForDateRange(startDate: Long, endDate: Long): Result<List<IncomeEntry>> {
        return try {
            Result.Success(incomeEntryDao.getIncomeEntriesForDateRange(startDate, endDate))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAllIncomeEntries(): Result<List<IncomeEntry>> {
        return try {
            Result.Success(incomeEntryDao.getAllIncomeEntries())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
