package com.example.incomeplanner.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateUtilTest {

    private fun getCalendar(year: Int, month: Int, day: Int, hour: Int = 10, minute: Int = 30, second: Int = 0): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month) // Calendar months are 0-indexed
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testGetStartOfDay() {
        val specificDate = getCalendar(2024, Calendar.JUNE, 23, 15, 30, 45)
        val startOfDay = DateUtil.getStartOfDay(specificDate.timeInMillis)
        val expectedStart = getCalendar(2024, Calendar.JUNE, 23, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfDay)
    }

    @Test
    fun testGetEndOfDay() {
        val specificDate = getCalendar(2024, Calendar.JUNE, 23, 15, 30, 45)
        val endOfDay = DateUtil.getEndOfDay(specificDate.timeInMillis)

        val expectedEndCalendar = getCalendar(2024, Calendar.JUNE, 23, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        val expectedEnd = expectedEndCalendar.timeInMillis

        assertEquals(expectedEnd, endOfDay)
    }

    @Test
    fun testGetStartOfWeek_MidWeek() { // Assuming Monday is start of week
        // Wednesday, June 26, 2024
        val specificDate = getCalendar(2024, Calendar.JUNE, 26)
        val startOfWeek = DateUtil.getStartOfWeek(specificDate.timeInMillis)
        // Expected: Monday, June 24, 2024
        val expectedStart = getCalendar(2024, Calendar.JUNE, 24, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfWeek)
    }

    @Test
    fun testGetStartOfWeek_IsMonday() {
        // Monday, June 24, 2024
        val specificDate = getCalendar(2024, Calendar.JUNE, 24)
        val startOfWeek = DateUtil.getStartOfWeek(specificDate.timeInMillis)
        val expectedStart = getCalendar(2024, Calendar.JUNE, 24, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfWeek)
    }

    @Test
    fun testGetEndOfWeek_MidWeek() { // Assuming Monday is start of week, Sunday is end
        // Wednesday, June 26, 2024
        val specificDate = getCalendar(2024, Calendar.JUNE, 26)
        val endOfWeek = DateUtil.getEndOfWeek(specificDate.timeInMillis)
        // Expected: Sunday, June 30, 2024
        val expectedEndCalendar = getCalendar(2024, Calendar.JUNE, 30, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfWeek)
    }

    @Test
    fun testGetEndOfWeek_IsSunday() {
        // Sunday, June 30, 2024
        val specificDate = getCalendar(2024, Calendar.JUNE, 30)
        val endOfWeek = DateUtil.getEndOfWeek(specificDate.timeInMillis)
        val expectedEndCalendar = getCalendar(2024, Calendar.JUNE, 30, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfWeek)
    }


    @Test
    fun testGetStartOfMonth() {
        val specificDate = getCalendar(2024, Calendar.JUNE, 23)
        val startOfMonth = DateUtil.getStartOfMonth(specificDate.timeInMillis)
        val expectedStart = getCalendar(2024, Calendar.JUNE, 1, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfMonth)
    }

    @Test
    fun testGetEndOfMonth_June() { // 30 days
        val specificDate = getCalendar(2024, Calendar.JUNE, 23)
        val endOfMonth = DateUtil.getEndOfMonth(specificDate.timeInMillis)
        val expectedEndCalendar = getCalendar(2024, Calendar.JUNE, 30, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfMonth)
    }

    @Test
    fun testGetEndOfMonth_FebruaryLeapYear() { // 29 days
        val specificDate = getCalendar(2024, Calendar.FEBRUARY, 15)
        val endOfMonth = DateUtil.getEndOfMonth(specificDate.timeInMillis)
        val expectedEndCalendar = getCalendar(2024, Calendar.FEBRUARY, 29, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfMonth)
    }

    @Test
    fun testGetEndOfMonth_FebruaryNonLeapYear() { // 28 days
        val specificDate = getCalendar(2023, Calendar.FEBRUARY, 15)
        val endOfMonth = DateUtil.getEndOfMonth(specificDate.timeInMillis)
        val expectedEndCalendar = getCalendar(2023, Calendar.FEBRUARY, 28, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfMonth)
    }
     @Test
    fun testWeekSpanningMonths_Start() {
        // Test with a date like July 1, 2024 (Monday)
        val specificDate = getCalendar(2024, Calendar.JULY, 1)
        val startOfWeek = DateUtil.getStartOfWeek(specificDate.timeInMillis)
        // Expected: Monday, July 1, 2024
        val expectedStart = getCalendar(2024, Calendar.JULY, 1, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfWeek)

        val endOfWeek = DateUtil.getEndOfWeek(specificDate.timeInMillis)
        // Expected: Sunday, July 7, 2024
        val expectedEndCalendar = getCalendar(2024, Calendar.JULY, 7, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfWeek)
    }

    @Test
    fun testWeekSpanningMonths_End() {
        // Test with a date like June 30, 2024 (Sunday)
        val specificDate = getCalendar(2024, Calendar.JUNE, 30)
        val startOfWeek = DateUtil.getStartOfWeek(specificDate.timeInMillis)
        // Expected: Monday, June 24, 2024
        val expectedStart = getCalendar(2024, Calendar.JUNE, 24, 0, 0, 0).timeInMillis
        assertEquals(expectedStart, startOfWeek)

        val endOfWeek = DateUtil.getEndOfWeek(specificDate.timeInMillis)
        // Expected: Sunday, June 30, 2024
        val expectedEndCalendar = getCalendar(2024, Calendar.JUNE, 30, 23, 59, 59)
        expectedEndCalendar.set(Calendar.MILLISECOND, 999)
        assertEquals(expectedEndCalendar.timeInMillis, endOfWeek)
    }
}
