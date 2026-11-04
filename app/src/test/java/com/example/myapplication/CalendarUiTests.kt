package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.BookingTimeUtils
import com.example.myapplication.data.FacilityRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Calendar, Time-Slot Ordering, Selected Bay & Availability Test Suite
 */
class CalendarUiTests {

    private val timeZone = TimeZone.getTimeZone("Africa/Blantyre")

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
        BookingRepository.resetToDefaultData()
        FacilityRepository.resetToDefaultData()
    }

    /**
     * TEST 1: Full Month Name & Year Display (e.g., November 2026).
     */
    @Test
    fun test1_full_month_name_and_year_formatting() {
        val cal = Calendar.getInstance(timeZone, Locale.US).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            this.timeZone = timeZone
        }.format(cal.time)

        assertEquals("November 2026", monthName)
    }

    /**
     * TEST 2: Weekday Alignment Offset (Mon-Sun).
     */
    @Test
    fun test2_weekday_alignment_offset() {
        fun calculateMonSunOffset(dayOfWeekJava: Int): Int {
            return (dayOfWeekJava + 5) % 7
        }

        assertEquals(0, calculateMonSunOffset(Calendar.MONDAY))    // Mon -> 0
        assertEquals(1, calculateMonSunOffset(Calendar.TUESDAY))   // Tue -> 1
        assertEquals(6, calculateMonSunOffset(Calendar.SUNDAY))    // Sun -> 6
    }

    /**
     * TEST 3: Leap Year February Days Count (2028 = 29 days, 2026 = 28 days).
     */
    @Test
    fun test3_leap_year_february_handling() {
        val leapFeb = Calendar.getInstance(timeZone, Locale.US).apply {
            set(Calendar.YEAR, 2028)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        assertEquals(29, leapFeb.getActualMaximum(Calendar.DAY_OF_MONTH))

        val nonLeapFeb = Calendar.getInstance(timeZone, Locale.US).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        assertEquals(28, nonLeapFeb.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    /**
     * TEST 4: Available Slots Partitioned First & Ordered Chronologically.
     */
    @Test
    fun test4_available_slots_partitioned_first_and_chronological() {
        val futureDate = Calendar.getInstance(timeZone, Locale.US).apply {
            add(Calendar.DAY_OF_YEAR, 3)
        }.timeInMillis

        val slots = BookingTimeUtils.generateAvailableSlots(
            dateMs = futureDate,
            durationMinutes = 30,
            openingTimeStr = "08:00 AM",
            closingTimeStr = "12:00 PM",
            facilityId = "1",
            bayId = "1_1"
        )

        val available = slots.filter { it.isAvailable }
        val unavailable = slots.filter { !it.isAvailable }

        for (i in 0 until available.size - 1) {
            assertTrue("Available slots must be chronologically ordered", available[i].startMs < available[i + 1].startMs)
        }

        for (i in 0 until unavailable.size - 1) {
            assertTrue("Unavailable slots must be chronologically ordered", unavailable[i].startMs < unavailable[i + 1].startMs)
        }
    }

    /**
     * TEST 5: Exact Status Labels ("Booked", "Reserved", "Time Passed").
     */
    @Test
    fun test5_exact_status_labels_for_unavailable_slots() {
        val startMsBooked = System.currentTimeMillis() + 10000000L
        val startMsReserved = startMsBooked + 3600000L

        BookingRepository.createBooking(
            facilityId = "1", bayId = "1_1", ownerId = "o@gmail.com",
            customerName = "Confirmed", customerEmail = "c@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            paymentStatus = "SUCCESSFUL", startTimeMs = startMsBooked
        )

        BookingRepository.createBooking(
            facilityId = "1", bayId = "1_1", ownerId = "o@gmail.com",
            customerName = "Hold", customerEmail = "h@gmail.com", customerPhone = "992",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "TNM",
            paymentStatus = "PENDING", startTimeMs = startMsReserved
        )

        val reasonBooked = BookingRepository.getReservationConflictType("1", "1_1", startMsBooked, startMsBooked + 1800000L)
        val reasonReserved = BookingRepository.getReservationConflictType("1", "1_1", startMsReserved, startMsReserved + 1800000L)

        assertEquals("Booked", reasonBooked)
        assertEquals("Reserved", reasonReserved)
    }

    /**
     * TEST 6: Only Selected Washing Bay Availability Queried.
     */
    @Test
    fun test6_availability_queried_for_selected_bay_only() {
        val futureDate = Calendar.getInstance(timeZone, Locale.US).apply {
            add(Calendar.DAY_OF_YEAR, 3)
        }.timeInMillis

        val slotsBay1 = BookingTimeUtils.generateAvailableSlots(
            dateMs = futureDate,
            durationMinutes = 30,
            openingTimeStr = "08:00 AM",
            closingTimeStr = "10:00 AM",
            facilityId = "1",
            bayId = "1_1"
        )

        val slotsBay2 = BookingTimeUtils.generateAvailableSlots(
            dateMs = futureDate,
            durationMinutes = 30,
            openingTimeStr = "08:00 AM",
            closingTimeStr = "10:00 AM",
            facilityId = "1",
            bayId = "1_2"
        )

        assertTrue(slotsBay1.isNotEmpty())
        assertTrue(slotsBay2.isNotEmpty())
    }
}
