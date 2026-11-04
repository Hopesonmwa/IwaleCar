package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.BookingTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class BookingPhase3Tests {

    private val timeZone = TimeZone.getTimeZone("Africa/Blantyre")

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
        BookingRepository.resetToDefaultData()
    }

    @Test
    fun PHASE3_001_select_valid_future_date() {
        val futureCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            add(Calendar.DAY_OF_YEAR, 2)
        }
        assertFalse(BookingTimeUtils.isDateInPast(futureCal.timeInMillis))
    }

    @Test
    fun PHASE3_002_reject_past_date() {
        val pastCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            add(Calendar.DAY_OF_YEAR, -2)
        }
        assertTrue(BookingTimeUtils.isDateInPast(pastCal.timeInMillis))
    }

    @Test
    fun PHASE3_003_select_valid_time_within_operating_hours() {
        val cal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val endMs = startMs + 30 * 60 * 1000L

        assertTrue(BookingTimeUtils.isWithinOperatingHours(startMs, endMs, "06:30 AM", "08:00 PM"))
    }

    @Test
    fun PHASE3_004_reject_time_outside_operating_hours() {
        val earlyCal = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = earlyCal.timeInMillis
        val endMs = startMs + 30 * 60 * 1000L

        assertFalse(BookingTimeUtils.isWithinOperatingHours(startMs, endMs, "06:30 AM", "08:00 PM"))
    }

    @Test
    fun PHASE3_005_calculate_correct_end_time() {
        val startMs = 1000000L
        val durationMins = 40
        val expectedEndMs = startMs + durationMins * 60 * 1000L
        val recordResult = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Test",
            customerEmail = "test@gmail.com",
            customerPhone = "991234567",
            bayTitle = "Bay 1",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            durationMinutes = durationMins,
            startTimeMs = startMs
        )
        assertTrue(recordResult.isSuccess)
        assertEquals(expectedEndMs, recordResult.getOrNull()?.endTimeMs)
    }

    @Test
    fun PHASE3_006_detect_overlapping_bookings_same_bay() {
        val start1 = System.currentTimeMillis() + 100000L

        val r1 = BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "A", customerEmail = "a@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )
        assertTrue(r1.isSuccess)

        // Overlapping request [start1 + 10m, start1 + 40m]
        val overlapStart = start1 + 10 * 60 * 1000L
        val overlapEnd = start1 + 40 * 60 * 1000L
        assertTrue(BookingRepository.hasOverlappingReservation("fac_1", "bay_1", overlapStart, overlapEnd))
    }

    @Test
    fun PHASE3_007_allow_non_overlapping_bookings_same_bay() {
        val start1 = System.currentTimeMillis() + 200000L
        val end1 = start1 + 30 * 60 * 1000L

        val r1 = BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "A", customerEmail = "a@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )
        assertTrue(r1.isSuccess)

        // Non-overlapping subsequent request [end1, end1 + 30m]
        assertFalse(BookingRepository.hasOverlappingReservation("fac_1", "bay_1", end1, end1 + 30 * 60 * 1000L))
    }

    @Test
    fun PHASE3_008_allow_simultaneous_bookings_different_bays() {
        val start1 = System.currentTimeMillis() + 300000L
        val end1 = start1 + 30 * 60 * 1000L

        val r1 = BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "A", customerEmail = "a@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )
        assertTrue(r1.isSuccess)

        // Same time slot on different bay bay_2
        assertFalse(BookingRepository.hasOverlappingReservation("fac_1", "bay_2", start1, end1))
    }

    @Test
    fun PHASE3_009_allow_booking_starting_exactly_when_earlier_ends() {
        val start1 = System.currentTimeMillis() + 400000L
        val end1 = start1 + 30 * 60 * 1000L

        val r1 = BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "A", customerEmail = "a@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )
        assertTrue(r1.isSuccess)

        // Booking starting exactly at end1
        assertFalse(BookingRepository.hasOverlappingReservation("fac_1", "bay_1", end1, end1 + 30 * 60 * 1000L))
    }

    @Test
    fun PHASE3_010_reject_booking_when_availability_changes_before_confirmation() {
        val start1 = System.currentTimeMillis() + 500000L

        // Slot becomes reserved
        BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "A", customerEmail = "a@gmail.com", customerPhone = "991",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )

        // Attempt second booking attempt right before confirmation
        val attemptResult = BookingRepository.createBooking(
            facilityId = "fac_1", bayId = "bay_1", ownerId = "o@gmail.com",
            customerName = "B", customerEmail = "b@gmail.com", customerPhone = "992",
            bayTitle = "Bay 1", amountValue = 5000, paymentMethod = "Airtel",
            durationMinutes = 30, startTimeMs = start1
        )
        assertTrue(attemptResult.isFailure)
    }

    @Test
    fun PHASE3_011_recalculate_availability_when_date_changes() {
        val date1 = System.currentTimeMillis()
        val date2 = date1 + 24 * 3600 * 1000L

        val slots1 = BookingTimeUtils.generateAvailableSlots(date1, 30, "06:30 AM", "08:00 PM", "fac_1", "bay_1")
        val slots2 = BookingTimeUtils.generateAvailableSlots(date2, 30, "06:30 AM", "08:00 PM", "fac_1", "bay_1")

        assertTrue(slots1.isNotEmpty())
        assertTrue(slots2.isNotEmpty())
    }

    @Test
    fun PHASE3_012_recalculate_availability_when_bay_changes() {
        val date1 = System.currentTimeMillis() + 600000L
        val slotsBay1 = BookingTimeUtils.generateAvailableSlots(date1, 30, "06:30 AM", "08:00 PM", "fac_1", "bay_1")
        val slotsBay2 = BookingTimeUtils.generateAvailableSlots(date1, 30, "06:30 AM", "08:00 PM", "fac_1", "bay_2")

        assertTrue(slotsBay1.isNotEmpty())
        assertTrue(slotsBay2.isNotEmpty())
    }

    @Test
    fun PHASE3_013_recalculate_availability_when_duration_changes() {
        val date1 = System.currentTimeMillis() + 700000L
        val slots30m = BookingTimeUtils.generateAvailableSlots(date1, 30, "06:30 AM", "08:00 PM", "fac_1", "bay_1")
        val slots60m = BookingTimeUtils.generateAvailableSlots(date1, 60, "06:30 AM", "08:00 PM", "fac_1", "bay_1")

        assertTrue(slots30m.isNotEmpty())
        assertTrue(slots60m.isNotEmpty())
        assertTrue(slots30m.size >= slots60m.size)
    }
}
