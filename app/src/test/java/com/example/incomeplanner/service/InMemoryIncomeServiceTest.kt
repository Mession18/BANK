package com.example.incomeplanner.service

import com.example.incomeplanner.model.IncomeEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class InMemoryIncomeServiceTest {

    private lateinit var incomeService: IncomeService

    @Before
    fun setUp() {
        incomeService = InMemoryIncomeService.getInMemoryInstance()
        // Clear data before each test
        runTest {
            (incomeService as InMemoryIncomeService).getAllIncomeEntries().let { result ->
                if (result is Result.Success) {
                    result.data.forEach { incomeService.deleteIncomeEntry(it.id) }
                }
            }
        }
    }

    @Test
    fun `addIncomeEntry successfully adds an entry`() = runTest {
        val entry = IncomeEntry(amount = 100.0, date = System.currentTimeMillis(), description = "Salary")
        val result = incomeService.addIncomeEntry(entry)
        assertTrue(result is Result.Success)

        val retrieved = incomeService.getIncomeEntry(entry.id)
        assertTrue(retrieved is Result.Success)
        assertEquals(entry, (retrieved as Result.Success).data)
    }

    @Test
    fun `getIncomeEntry returns null for non-existent entry`() = runTest {
        val result = incomeService.getIncomeEntry(UUID.randomUUID().toString())
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun `updateIncomeEntry successfully updates an existing entry`() = runTest {
        val entry = IncomeEntry(amount = 50.0, date = System.currentTimeMillis(), description = "Initial Desc")
        incomeService.addIncomeEntry(entry)

        val updatedEntry = entry.copy(amount = 75.0, description = "Updated Desc")
        val updateResult = incomeService.updateIncomeEntry(updatedEntry)
        assertTrue(updateResult is Result.Success)

        val retrieved = incomeService.getIncomeEntry(entry.id)
        assertTrue(retrieved is Result.Success)
        assertEquals(75.0, (retrieved as Result.Success).data?.amount, 0.01)
        assertEquals("Updated Desc", (retrieved as Result.Success).data?.description)
    }

    @Test
    fun `updateIncomeEntry fails for non-existent entry`() = runTest {
        val nonExistentEntry = IncomeEntry(id = "non-existent", amount = 10.0, date = 0L, description = "ghost")
        val updateResult = incomeService.updateIncomeEntry(nonExistentEntry)
        assertTrue(updateResult is Result.Error)
    }

    @Test
    fun `deleteIncomeEntry successfully removes an entry`() = runTest {
        val entry = IncomeEntry(amount = 20.0, date = System.currentTimeMillis(), description = "To Delete")
        incomeService.addIncomeEntry(entry)

        val deleteResult = incomeService.deleteIncomeEntry(entry.id)
        assertTrue(deleteResult is Result.Success)

        val retrieved = incomeService.getIncomeEntry(entry.id)
        assertTrue(retrieved is Result.Success)
        assertNull((retrieved as Result.Success).data)
    }

    @Test
    fun `deleteIncomeEntry fails for non-existent entry`() = runTest {
        val deleteResult = incomeService.deleteIncomeEntry("non-existent")
        assertTrue(deleteResult is Result.Error)
    }

    @Test
    fun `getAllIncomeEntries returns all added entries`() = runTest {
        val entry1 = IncomeEntry(amount = 10.0, date = 1L, description = "E1")
        val entry2 = IncomeEntry(amount = 20.0, date = 2L, description = "E2")
        incomeService.addIncomeEntry(entry1)
        incomeService.addIncomeEntry(entry2)

        val allEntriesResult = incomeService.getAllIncomeEntries()
        assertTrue(allEntriesResult is Result.Success)
        val allEntries = (allEntriesResult as Result.Success).data
        assertEquals(2, allEntries.size)
        assertTrue(allEntries.contains(entry1))
        assertTrue(allEntries.contains(entry2))
    }

    @Test
    fun `getIncomeEntriesForDateRange returns correct entries`() = runTest {
        val date1 = System.currentTimeMillis()
        val date2 = date1 + 10000
        val date3 = date2 + 10000
        val outsideRangeDate = date1 - 100000

        val entry1InRage = IncomeEntry(amount = 1.0, date = date1, description = "Entry 1")
        val entry2InRage = IncomeEntry(amount = 2.0, date = date2, description = "Entry 2")
        val entryOutsideRange = IncomeEntry(amount = 3.0, date = outsideRangeDate, description = "Entry 3")

        incomeService.addIncomeEntry(entry1InRage)
        incomeService.addIncomeEntry(entry2InRage)
        incomeService.addIncomeEntry(entryOutsideRange)

        val rangeResult = incomeService.getIncomeEntriesForDateRange(date1, date2)
        assertTrue(rangeResult is Result.Success)
        val entriesInRange = (rangeResult as Result.Success).data
        assertEquals(2, entriesInRange.size)
        assertTrue(entriesInRange.contains(entry1InRage))
        assertTrue(entriesInRange.contains(entry2InRage))
        assertFalse(entriesInRange.contains(entryOutsideRange))
    }
     @Test
    fun `getIncomeEntriesForDateRange with no entries in range`() = runTest {
        val date1 = System.currentTimeMillis()
        val date2 = date1 + 10000
        val entryOutsideRange = IncomeEntry(amount = 3.0, date = date1 - 100000, description = "Entry Out")
        incomeService.addIncomeEntry(entryOutsideRange)

        val rangeResult = incomeService.getIncomeEntriesForDateRange(date1, date2)
        assertTrue(rangeResult is Result.Success)
        val entriesInRange = (rangeResult as Result.Success).data
        assertTrue(entriesInRange.isEmpty())
    }
}
