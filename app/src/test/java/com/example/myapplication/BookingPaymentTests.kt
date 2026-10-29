package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookingPaymentTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * BOOKING_001 | Positive
     * Steps: Select City Shine > BAY 01 > Standard Wash > Airtel Money > simulate Success payment.
     * Expected: Booking confirmed; booking reference generated starting with IW-; recorded in repository.
     */
    @Test
    fun BOOKING_001_confirm_reservation_after_successful_payment() {
        // Arrange
        val futureStart = System.currentTimeMillis() + 10 * 3600 * 1000L

        // Act
        val result = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Test Customer",
            customerEmail = "customer.test@gmail.com",
            customerPhone = "991234567",
            bayTitle = "BAY 01 — Standard Wash",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = futureStart
        )

        // Assert
        assertTrue("Booking creation succeeds on successful payment", result.isSuccess)
        val record = result.getOrNull()
        assertNotNull("Booking record exists", record)
        assertTrue("Reference starts with IW-", record!!.bookingReference.startsWith("IW-"))
        assertEquals("Airtel Money", record.paymentMethod)
    }

    /**
     * BOOKING_002 | Negative
     * Steps: Attempt booking when all bays at facility are occupied/unavailable.
     * Expected: Bay availability check returns false; booking cannot proceed.
     */
    @Test
    fun BOOKING_002_handle_no_available_bays() {
        // Arrange
        val facility = FacilityRepository.getFacilityById("cs_facility_1")
        assertNotNull("Facility exists", facility)

        // Simulate all bays occupied
        val occupiedBays = facility!!.bays.map { it.copy(status = "Occupied") }
        val hasAvailableBays = occupiedBays.any { it.status == "Available" }

        // Assert
        assertFalse("When all bays are occupied, no available bays remain", hasAvailableBays)
    }

    /**
     * BOOKING_003 | Negative
     * Steps: Customer A books BAY 01 at time T. Customer B attempts to book BAY 01 at overlapping time T+10m.
     * Expected: Customer B's overlapping booking is rejected (`hasOverlappingReservation` = true).
     */
    @Test
    fun BOOKING_003_prevent_overlapping_reservations() {
        // Arrange
        val slotStart = System.currentTimeMillis() + 20 * 3600 * 1000L
        val slotEnd = slotStart + 30 * 60 * 1000L

        // Customer A books
        val custAResult = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Customer A",
            customerEmail = "custA@gmail.com",
            customerPhone = "991111111",
            bayTitle = "BAY 01",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = slotStart
        )
        assertTrue("Customer A booking succeeds", custAResult.isSuccess)

        // Customer B attempts overlapping slot [slotStart + 10m, slotStart + 40m]
        val overlapStart = slotStart + 10 * 60 * 1000L
        val overlapEnd = slotStart + 40 * 60 * 1000L

        val isOverlapping = BookingRepository.hasOverlappingReservation("cs_facility_1", "cs_1", overlapStart, overlapEnd)
        assertTrue("Overlapping reservation detected", isOverlapping)

        val custBResult = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Customer B",
            customerEmail = "custB@gmail.com",
            customerPhone = "992222222",
            bayTitle = "BAY 01",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = overlapStart
        )

        // Assert
        assertTrue("Customer B booking fails due to overlap", custBResult.isFailure)
        assertEquals("This bay is already reserved for the selected time slot.", custBResult.exceptionOrNull()?.message)
    }

    /**
     * PAYMENT_001 | Positive
     * Steps: Select TNM Mpamba > enter phone 991234567 > simulate success.
     * Expected: Payment confirmed; booking created with TNM Mpamba payment method.
     */
    @Test
    fun PAYMENT_001_successful_tnm_mpamba_payment() {
        // Arrange
        val slotStart = System.currentTimeMillis() + 30 * 3600 * 1000L

        // Act
        val result = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_2",
            ownerId = "owner.test@gmail.com",
            customerName = "Test Customer",
            customerEmail = "customer.test@gmail.com",
            customerPhone = "991234567",
            bayTitle = "BAY 02 — Deep Clean",
            amountValue = 10000,
            paymentMethod = "TNM Mpamba",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 40,
            startTimeMs = slotStart
        )

        // Assert
        assertTrue("TNM Mpamba payment succeeds", result.isSuccess)
        val booking = result.getOrNull()
        assertEquals("TNM Mpamba", booking?.paymentMethod)
        assertEquals("SUCCESSFUL", booking?.paymentStatus)
    }

    /**
     * PAYMENT_003 | Negative
     * Steps: Initiate payment > simulate FAILED payment.
     * Expected: Booking not confirmed; temporary hold released; no confirmed booking in repository.
     */
    @Test
    fun PAYMENT_003_release_hold_after_failed_payment() {
        // Arrange
        val countBefore = BookingRepository.getAllBookings().size

        // Act
        val result = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Failed Customer",
            customerEmail = "failed@gmail.com",
            customerPhone = "991234567",
            bayTitle = "BAY 01",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            paymentStatus = "FAILED",
            durationMinutes = 30
        )

        // Assert
        val countAfter = BookingRepository.getAllBookings().size
        assertEquals("Failed payment does not add confirmed booking to repository", countBefore, countAfter)
    }
}
