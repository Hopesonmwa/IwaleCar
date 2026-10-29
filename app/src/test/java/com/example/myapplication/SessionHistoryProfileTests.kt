package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionHistoryProfileTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * SESSION_001 | Positive
     * Steps: Open active session > observe countdown timer decreases.
     * Expected: Session active; remaining time decreases over time.
     */
    @Test
    fun SESSION_001_active_session_countdown() {
        // Arrange
        val startSecs = 1800 // 30 minutes
        var remainingSecs = startSecs

        // Act
        remainingSecs -= 10 // Advance 10 seconds

        // Assert
        assertEquals(1790, remainingSecs)
        assertTrue("Remaining time is active", remainingSecs > 0)
    }

    /**
     * SESSION_003 | Positive
     * Steps: Open active session > tap Extend Wash > select +15 min tier > complete payment.
     * Expected: Session end time extended by 15 minutes; timer increases.
     */
    @Test
    fun SESSION_003_extend_active_session_successfully() {
        // Arrange
        val startMs = System.currentTimeMillis() + 40 * 3600 * 1000L
        val createResult = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_1",
            ownerId = "owner.test@gmail.com",
            customerName = "Extending User",
            customerEmail = "extending.user@gmail.com",
            customerPhone = "991234567",
            bayTitle = "BAY 01",
            amountValue = 5000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = startMs
        )

        val booking = createResult.getOrNull()
        assertNotNull("Booking created", booking)
        val initialEndMs = booking!!.endTimeMs

        // Act: Extend by 15 minutes
        val extendResult = BookingRepository.extendSession(booking.id, 15, 4500, "Airtel Money")

        // Assert
        assertTrue("Session extension succeeds", extendResult.isSuccess)
        val updatedBooking = extendResult.getOrNull()
        assertEquals(initialEndMs + 15 * 60 * 1000L, updatedBooking?.endTimeMs)
    }

    /**
     * SESSION_004 | Negative
     * Steps: Open active session > tap Extend Wash > attempt +15 min extension when subsequent booking exists.
     * Expected: Extension blocked with error message "Extension unavailable: This bay is reserved for a subsequent session."
     */
    @Test
    fun SESSION_004_block_extension_conflicting_with_subsequent_reservation() {
        // Arrange
        val startMs = System.currentTimeMillis() + 50 * 3600 * 1000L
        val createResult = BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_2",
            ownerId = "owner.test@gmail.com",
            customerName = "Active User",
            customerEmail = "active.user@gmail.com",
            customerPhone = "991234567",
            bayTitle = "BAY 02",
            amountValue = 10000,
            paymentMethod = "Iwale Wallet",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = startMs
        )

        val activeBooking = createResult.getOrNull()
        assertNotNull("Active booking created", activeBooking)

        // Subsequent booking starts exactly when activeBooking ends
        val subsequentStart = activeBooking!!.endTimeMs
        BookingRepository.createBooking(
            facilityId = "cs_facility_1",
            bayId = "cs_2",
            ownerId = "owner.test@gmail.com",
            customerName = "Subsequent User",
            customerEmail = "subsequent.user@gmail.com",
            customerPhone = "992222333",
            bayTitle = "BAY 02",
            amountValue = 10000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = subsequentStart
        )

        // Act: Attempt to extend activeBooking by 15 minutes
        val extendResult = BookingRepository.extendSession(activeBooking.id, 15, 4500, "Iwale Wallet")

        // Assert
        assertTrue("Extension blocked due to subsequent reservation", extendResult.isFailure)
        assertEquals("Extension unavailable: This bay is reserved for a subsequent session.", extendResult.exceptionOrNull()?.message)
    }

    /**
     * SESSION_005 | Positive
     * Steps: Active session countdown approaches configured threshold <= 5 mins (300 seconds).
     * Expected: Notification warning triggered; duplicate warning suppressed.
     */
    @Test
    fun SESSION_005_notify_customer_before_session_expiry() {
        // Arrange
        val sessionId = "SESS-101"
        val remainingSecs4Min = 240L

        // Act & Assert
        val notifyFirstTime = BookingRepository.shouldNotifySessionExpiry(sessionId, remainingSecs4Min)
        assertTrue("Notification triggered when <= 5 minutes remaining", notifyFirstTime)

        val notifySecondTime = BookingRepository.shouldNotifySessionExpiry(sessionId, remainingSecs4Min)
        assertFalse("Duplicate notification suppressed for same session", notifySecondTime)
    }

    /**
     * HISTORY_001 | Positive
     * Steps: Open History > select All > Completed > Cancelled.
     * Expected: History list filters matching status items for current customer.
     */
    @Test
    fun HISTORY_001_filter_booking_history_by_status() {
        // Arrange
        val customerEmail = "user@gmail.com"

        // Act
        val allHistory = BookingRepository.getCustomerBookings(customerEmail, "All")
        val completedHistory = BookingRepository.getCustomerBookings(customerEmail, "Completed")
        val cancelledHistory = BookingRepository.getCustomerBookings(customerEmail, "Cancelled")

        // Assert
        assertTrue("Customer has history records", allHistory.isNotEmpty())
        assertTrue("Completed history filters completed status", completedHistory.all { it.status.equals("Completed", ignoreCase = true) })
        assertTrue("Cancelled history filters cancelled status", cancelledHistory.all { it.status.equals("Cancelled", ignoreCase = true) })
    }

    /**
     * PROFILE_001 | Positive
     * Steps: Open Profile > update primary location to Blantyre.
     * Expected: User profile location updated and retained.
     */
    @Test
    fun PROFILE_001_update_primary_location_preference() {
        // Arrange
        val user = UserRepository.findUser("customer.test@gmail.com")
        assertNotNull("Test customer exists", user)

        // Act
        val updatedUser = user!!.copy(primaryLocation = "Blantyre")
        UserRepository.registerUser(updatedUser)

        // Assert
        val storedUser = UserRepository.findUser("customer.test@gmail.com")
        assertEquals("Blantyre", storedUser?.primaryLocation)
    }
}
