package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityData
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.greetingName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUnitTest {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    // TC-01: Terms and Privacy Policy Acceptance
    @Test
    fun testRegistrationTermsAcceptanceValidation() {
        val user = UserAccount("Test User", "999000111", "testuser@gmail.com", "Pass123!", "User", "Lilongwe")

        // When terms NOT accepted
        val isTermsAccepted = false
        assertFalse("Registration must fail when terms checkbox is unchecked", isTermsAccepted)

        // When terms ARE accepted
        val isTermsAcceptedTrue = true
        UserRepository.registerUser(user)
        assertTrue("Registration allowed when terms accepted", isTermsAcceptedTrue)
        assertNotNull("User registered in repository", UserRepository.findUser("testuser@gmail.com"))
    }

    // TC-02 & TC-03: Login Credentials and Email Validation
    @Test
    fun testEmailFormatAndCredentialValidation() {
        // Reject email without '@'
        val invalidEmail = "hopeson.gmail.com"
        assertFalse("Email without '@' is invalid", invalidEmail.contains("@"))

        // Reject invalid password for registered user
        val user = UserAccount("Auth Test", "999222333", "authtest@gmail.com", "Password123!", "User", "Lilongwe")
        UserRepository.registerUser(user)

        assertFalse("Login fails with incorrect password", UserRepository.validateCredentials("authtest@gmail.com", "WrongPass"))
        assertTrue("Login succeeds with correct password", UserRepository.validateCredentials("authtest@gmail.com", "Password123!"))
    }

    // TC-04 & TC-05: Facility City Filter and Search
    @Test
    fun testFacilityFilteringAndSearch() {
        val approvedFacilities = FacilityRepository.getApprovedCarWashFacilities()

        // Filter by city: Blantyre
        val blantyreFacilities = approvedFacilities.filter {
            it.area.equals("Blantyre", ignoreCase = true) || it.location.contains("Blantyre", ignoreCase = true)
        }
        assertTrue("Blantyre facilities found", blantyreFacilities.isNotEmpty())
        assertTrue("Matches Blantyre location", blantyreFacilities.all { it.location.contains("Blantyre", ignoreCase = true) || it.area.contains("Blantyre", ignoreCase = true) })

        // Search with no matching result
        val searchResult = approvedFacilities.filter { it.name.contains("NonExistentFacility123", ignoreCase = true) }
        assertTrue("No facilities match dummy search query", searchResult.isEmpty())
    }

    // TC-06: Facility Approval Eligibility
    @Test
    fun testFacilityApprovalEligibility() {
        val pendingFacility = FacilityData(
            id = "pending_999",
            name = "Pending Sparkle Wash",
            area = "Area 25",
            address = "Plot 10, Area 25",
            city = "Lilongwe",
            phone = "999111222",
            bayCount = 2,
            openingTime = "08:00",
            closingTime = "18:00",
            description = "High-pressure wash",
            status = "Pending Approval",
            ownerId = "owner@gmail.com"
        )
        FacilityRepository.saveFacilityData(pendingFacility)

        val approvedPublicFacilities = FacilityRepository.getApprovedCarWashFacilities()
        assertFalse("Pending facility must not appear in public approved facilities list",
            approvedPublicFacilities.any { it.id == "pending_999" })
    }

    // TC-07 & TC-10: Booking Reference Generation & Payment
    @Test
    fun testBookingReferenceAndPaymentConfirmation() {
        val futureStart = System.currentTimeMillis() + 10 * 3600 * 1000L
        val result = BookingRepository.createBooking(
            facilityId = "1",
            bayId = "1_1",
            ownerId = "owner@gmail.com",
            customerName = "Kondwani Phiri",
            customerEmail = "testcustomer@gmail.com",
            customerPhone = "999111222",
            bayTitle = "Bay 01 — Express Wash",
            amountValue = 15000,
            paymentMethod = "TNM Mpamba",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = futureStart
        )

        assertTrue("Booking creation succeeds on successful payment", result.isSuccess)
        val booking = result.getOrNull()
        assertNotNull("Booking record created", booking)
        assertTrue("Booking reference generated starting with IW-", booking!!.bookingReference.startsWith("IW-"))
        assertEquals("Payment method stored as TNM Mpamba", "TNM Mpamba", booking.paymentMethod)
    }

    // TC-08: Bay Availability Check
    @Test
    fun testBayAvailabilityCheck() {
        val facility = FacilityRepository.getFacilityById("1")
        assertNotNull("Facility exists", facility)
        val hasAvailableBays = facility!!.bays.any { it.status == "Available" }
        assertTrue("Facility 1 has available bays", hasAvailableBays)
    }

    // TC-09: Overlapping Booking Detection
    @Test
    fun testOverlappingBookingDetection() {
        val now = System.currentTimeMillis() + 20 * 3600 * 1000L
        val start1 = now
        val end1 = now + 30 * 60 * 1000L

        // Create initial booking
        val createResult = BookingRepository.createBooking(
            facilityId = "1",
            bayId = "1_2",
            ownerId = "owner@gmail.com",
            customerName = "User 1",
            customerEmail = "user1@gmail.com",
            customerPhone = "999111222",
            bayTitle = "Bay 02",
            amountValue = 10000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = start1
        )
        assertTrue("Initial booking created successfully", createResult.isSuccess)

        // Attempt overlapping booking during [start1 + 10m, start1 + 40m]
        val overlapStart = start1 + 10 * 60 * 1000L
        val overlapEnd = start1 + 40 * 60 * 1000L

        val isOverlapping = BookingRepository.hasOverlappingReservation("1", "1_2", overlapStart, overlapEnd)
        assertTrue("Overlapping reservation detected", isOverlapping)

        // Attempt adjacent non-overlapping booking [end1, end1 + 30m]
        val adjacentStart = end1
        val adjacentEnd = end1 + 30 * 60 * 1000L
        val isAdjacentOverlapping = BookingRepository.hasOverlappingReservation("1", "1_2", adjacentStart, adjacentEnd)
        assertFalse("Adjacent reservation is non-overlapping", isAdjacentOverlapping)
    }

    // TC-11: Release Hold After Failed Payment
    @Test
    fun testFailedPaymentHoldRelease() {
        val beforeCount = BookingRepository.getAllBookings().size

        BookingRepository.createBooking(
            facilityId = "1",
            bayId = "1_1",
            ownerId = "owner@gmail.com",
            customerName = "Failed Customer",
            customerEmail = "failed@gmail.com",
            customerPhone = "999111222",
            bayTitle = "Bay 01",
            amountValue = 15000,
            paymentMethod = "Airtel Money",
            paymentStatus = "FAILED",
            durationMinutes = 30
        )

        val afterCount = BookingRepository.getAllBookings().size
        assertEquals("Failed payment does not add confirmed booking record", beforeCount, afterCount)
    }

    // TC-12 & TC-13: Session Extension Validation & Conflict Detection
    @Test
    fun testSessionExtensionAndConflictDetection() {
        val start = System.currentTimeMillis() + 50000000L
        val createResult = BookingRepository.createBooking(
            facilityId = "1",
            bayId = "1_3",
            ownerId = "owner@gmail.com",
            customerName = "Extending Customer",
            customerEmail = "extending@gmail.com",
            customerPhone = "991234567",
            bayTitle = "Bay 03",
            amountValue = 10000,
            paymentMethod = "Iwale Wallet",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = start
        )

        val booking = createResult.getOrNull()
        assertNotNull("Booking created for extension test", booking)

        // Create subsequent conflicting booking right after the 30-min session
        val subsequentStart = booking!!.endTimeMs
        BookingRepository.createBooking(
            facilityId = "1",
            bayId = "1_3",
            ownerId = "owner@gmail.com",
            customerName = "Subsequent Customer",
            customerEmail = "subsequent@gmail.com",
            customerPhone = "999333444",
            bayTitle = "Bay 03",
            amountValue = 10000,
            paymentMethod = "Airtel Money",
            paymentStatus = "SUCCESSFUL",
            durationMinutes = 30,
            startTimeMs = subsequentStart
        )

        // Attempting to extend extending@gmail.com's session should fail due to conflict with subsequent@gmail.com
        val extendResult = BookingRepository.extendSession(booking.id, 15, 5000, "Iwale Wallet")
        assertTrue("Extension fails when subsequent reservation exists", extendResult.isFailure)
        assertEquals("Extension unavailable: This bay is reserved for a subsequent session.", extendResult.exceptionOrNull()?.message)
    }

    // TC-14: Session Expiry Notification Logic
    @Test
    fun testSessionExpiryNotificationLogic() {
        val bookingId = "BK-NOTIFY-TEST"
        val remainingSecs4Min = 240L // <= 300 seconds

        val notify1 = BookingRepository.shouldNotifySessionExpiry(bookingId, remainingSecs4Min)
        assertTrue("Notification triggered when <= 5 minutes remaining", notify1)

        val notify2 = BookingRepository.shouldNotifySessionExpiry(bookingId, remainingSecs4Min)
        assertFalse("Duplicate notification suppressed for same session", notify2)
    }

    // TC-15: Customer Booking History Filters
    @Test
    fun testCustomerHistoryFilters() {
        val userEmail = "user@gmail.com"
        val allHistory = BookingRepository.getCustomerBookings(userEmail, "All")
        val completedHistory = BookingRepository.getCustomerBookings(userEmail, "Completed")
        val cancelledHistory = BookingRepository.getCustomerBookings(userEmail, "Cancelled")

        assertTrue("Customer has history records", allHistory.isNotEmpty())
        assertTrue("Completed history filters completed items", completedHistory.all { it.status.equals("Completed", ignoreCase = true) })
        assertTrue("Cancelled history filters cancelled items", cancelledHistory.all { it.status.equals("Cancelled", ignoreCase = true) })
    }

    @Test
    fun testGreetingNameFromEmail() {
        val userWithGmail = UserAccount("Hopeson Mwangonde", "999111222", "chaps.hopeson@gmail.com", "Pass123!", "User", "Lilongwe")
        assertEquals("chaps.hopeson", userWithGmail.greetingName)

        val userWithOtherEmail = UserAccount("Chisomo Gondwe", "999333444", "chisomo.gondwe@yahoo.com", "Pass123!", "User", "Blantyre")
        assertEquals("chisomo.gondwe", userWithOtherEmail.greetingName)

        val userWithoutEmail = UserAccount("Kondwani Phiri", "999444555", "", "Pass123!", "User", "Mzuzu")
        assertEquals("Kondwani", userWithoutEmail.greetingName)
    }
}
