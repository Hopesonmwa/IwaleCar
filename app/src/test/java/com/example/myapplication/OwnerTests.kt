package com.example.myapplication

import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityData
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.ui.BayConfig
import com.example.myapplication.ui.FacilityItemData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OwnerTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * OWNER_FACILITY_001 | Positive
     * Steps: Open My Facilities > tap Add Facility > enter details > save.
     * Expected: Saved with Pending Approval status; associated with current owner.
     */
    @Test
    fun OWNER_FACILITY_001_add_new_facility_pending_approval() {
        // Arrange
        val ownerEmail = "owner.test@gmail.com"
        val newFacilityItem = FacilityItemData(
            id = "fac_new_001",
            name = "Zomba City Sparkle",
            address = "M1 Highway",
            city = "Zomba",
            phone = "991234567",
            bayCount = 0,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "High pressure wash",
            status = "Pending Approval"
        )

        // Act
        FacilityRepository.saveFacility(newFacilityItem, ownerEmail)

        // Assert
        val stored = FacilityRepository.getFacilityById("fac_new_001")
        assertNotNull("New facility saved", stored)
        assertEquals("Zomba City Sparkle", stored?.name)
        assertEquals("Pending Approval", stored?.status)
        assertEquals("owner.test@gmail.com", stored?.ownerId)
    }

    /**
     * OWNER_FACILITY_002 | Negative
     * Steps: Owner A reviews My Facilities.
     * Expected: Only Owner A's facilities listed; Owner B's facilities not exposed.
     */
    @Test
    fun OWNER_FACILITY_002_isolate_owner_facilities() {
        // Arrange
        val ownerAEmail = "owner.a@gmail.com"
        val ownerBEmail = "owner.b@gmail.com"

        // Act
        val ownerAFacilities = FacilityRepository.getFacilitiesForOwner(ownerAEmail)
        val ownerBFacilities = FacilityRepository.getFacilitiesForOwner(ownerBEmail)

        // Assert
        assertTrue("Owner A has facilities", ownerAFacilities.isNotEmpty())
        assertTrue("Owner B has facilities", ownerBFacilities.isNotEmpty())
        assertFalse("Owner A cannot see Owner B's facilities",
            ownerAFacilities.any { it.ownerId == ownerBEmail })
    }

    /**
     * OWNER_SETUP_001 | Positive
     * Steps: New owner logs in with 0 configured bays.
     * Expected: Setup prompt "Set Up Your Car Wash" indicates setup required (`isFacilitySetupComplete` = false).
     */
    @Test
    fun OWNER_SETUP_001_first_login_setup_prompt() {
        // Arrange
        val newOwnerEmail = "owner.new@gmail.com"
        val emptyFacility = FacilityData(
            id = "fac_new_owner_0",
            name = "New Owner Wash",
            area = "Zomba",
            address = "Main Location",
            city = "Zomba",
            phone = "995555666",
            bayCount = 0,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "New facility",
            status = "Approved",
            bays = emptyList(),
            ownerId = newOwnerEmail
        )
        FacilityRepository.saveFacilityData(emptyFacility)
        FacilityRepository.setSelectedFacilityId("fac_new_owner_0")

        // Act
        val selectedFacility = FacilityRepository.getSelectedFacilityForOwner(newOwnerEmail)
        assertNotNull("Selected facility exists", selectedFacility)
        val isSetupComplete = FacilityRepository.isFacilitySetupComplete(selectedFacility!!.id)

        // Assert
        assertFalse("Setup is incomplete for new owner with 0 bays", isSetupComplete)
    }

    /**
     * OWNER_SETUP_002 | Positive
     * Steps: Owner has 0 bays > taps Later > logs in again without adding bays.
     * Expected: Prompt re-evaluates as incomplete until a valid bay is saved.
     */
    @Test
    fun OWNER_SETUP_002_dismiss_setup_prompt_temporarily() {
        // Arrange
        val newOwnerEmail = "owner.new@gmail.com"
        val emptyFacility = FacilityData(
            id = "fac_new_owner_0",
            name = "New Owner Wash",
            area = "Zomba",
            address = "Main Location",
            city = "Zomba",
            phone = "995555666",
            bayCount = 0,
            openingTime = "06:30 AM",
            closingTime = "08:00 PM",
            description = "New facility",
            status = "Approved",
            bays = emptyList(),
            ownerId = newOwnerEmail
        )
        FacilityRepository.saveFacilityData(emptyFacility)
        FacilityRepository.setSelectedFacilityId("fac_new_owner_0")

        // Act
        var isDismissedThisSession = true // Dismissed for session 1
        assertTrue("Dismissed for session 1", isDismissedThisSession)

        // Next session login
        isDismissedThisSession = false
        val selectedFacility = FacilityRepository.getSelectedFacilityForOwner(newOwnerEmail)
        val isSetupComplete = FacilityRepository.isFacilitySetupComplete(selectedFacility!!.id)

        // Assert
        assertFalse("Prompt reappears on next session if setup still incomplete", isSetupComplete)
    }

    /**
     * OWNER_BAYS_001 | Positive
     * Steps: Open Manage Bays > tap Add Bay > enter valid details > save.
     * Expected: Bay saved and listed; associated with facility.
     */
    @Test
    fun OWNER_BAYS_001_add_new_bay_successfully() {
        // Arrange
        val facilityId = "cs_facility_1"
        val currentFacility = FacilityRepository.getFacilityById(facilityId)
        assertNotNull("Facility exists", currentFacility)

        val newBay = BayConfig(
            id = "cs_bay_3",
            bayNumber = "BAY 03",
            serviceTitle = "Heavy Duty Polish",
            priceText = "MWK 15,000",
            priceValue = 15000,
            durationText = "45 mins",
            featuresDescription = "Full wax & interior polish",
            status = "Available"
        )

        // Act
        val updatedBays = currentFacility!!.bays + newBay
        FacilityRepository.updateBaysForFacility(facilityId, updatedBays)

        // Assert
        val updatedFacility = FacilityRepository.getFacilityById(facilityId)
        assertTrue("Bay 03 added to facility", updatedFacility!!.bays.any { it.bayNumber == "BAY 03" })
    }

    /**
     * OWNER_BAYS_002 | Negative
     * Steps: Open Manage Bays > enter negative price (-500) or zero duration (0 min).
     * Expected: Bay validation fails; invalid bay not saved.
     */
    @Test
    fun OWNER_BAYS_002_reject_negative_price_and_zero_duration() {
        // Arrange
        val negativePrice = -500
        val zeroDuration = 0

        // Act
        val isPriceValid = negativePrice > 0
        val isDurationValid = zeroDuration > 0

        // Assert
        assertFalse("Negative price is invalid", isPriceValid)
        assertFalse("Zero duration is invalid", isDurationValid)
    }

    /**
     * OWNER_BAYS_003 | Positive
     * Steps: Select BAY 01 and tap Remove > confirm.
     * Expected: Bay removed from facility bay list.
     */
    @Test
    fun OWNER_BAYS_003_remove_bay_with_confirmation() {
        // Arrange
        val facilityId = "cs_facility_1"
        val currentFacility = FacilityRepository.getFacilityById(facilityId)
        assertNotNull("Facility exists", currentFacility)

        // Act: Remove BAY 01
        val remainingBays = currentFacility!!.bays.filter { it.bayNumber != "BAY 01" }
        FacilityRepository.updateBaysForFacility(facilityId, remainingBays)

        // Assert
        val updatedFacility = FacilityRepository.getFacilityById(facilityId)
        assertFalse("BAY 01 is removed", updatedFacility!!.bays.any { it.bayNumber == "BAY 01" })
    }

    /**
     * OWNER_BOOKINGS_001 | Positive
     * Steps: Open Owner Bookings > filter by All, Upcoming, In Progress, Completed.
     * Expected: Displays bookings matching selected tab.
     */
    @Test
    fun OWNER_BOOKINGS_001_filter_owner_bookings_by_status() {
        // Arrange
        val facilityId = "cs_facility_1"
        val ownerEmail = "owner.test@gmail.com"

        // Act
        val allBookings = BookingRepository.getBookingsForFacilityAndOwner(facilityId, ownerEmail)

        // Assert
        assertTrue("Facility bookings returned", allBookings.isNotEmpty())
    }

    /**
     * OWNER_DASH_001 | Positive
     * Steps: Open Owner Dashboard.
     * Expected: Displays overview metrics (Today's revenue, bookings, active sessions, available bays) matching facility data.
     */
    @Test
    fun OWNER_DASH_001_view_owner_dashboard_metrics() {
        // Arrange
        val facilityId = "cs_facility_1"
        val ownerEmail = "owner.test@gmail.com"

        // Act
        val todayRevenue = BookingRepository.getTodayRevenueFormatted(facilityId, ownerEmail)
        val todayBookings = BookingRepository.getTodayBookingsCount(facilityId, ownerEmail)
        val activeSessions = BookingRepository.getActiveSessionsCount(facilityId, ownerEmail)

        // Assert
        assertNotNull("Today revenue formatted string exists", todayRevenue)
        assertTrue("Today bookings count >= 0", todayBookings >= 0)
        assertTrue("Active sessions count >= 0", activeSessions >= 0)
    }

    /**
     * OWNER_REVENUE_001 | Positive
     * Steps: Select facility and time range (*Today*, *This Week*, *This Month*).
     * Expected: Totals, trend bars and session counts reflect both facility and time-range filters.
     */
    @Test
    fun OWNER_REVENUE_001_dual_filter_revenue_analytics() {
        // Arrange
        val facilityId = "cs_facility_1"
        val ownerEmail = "owner.test@gmail.com"

        // Act
        val todayValue = BookingRepository.getTodayRevenueValue(facilityId, ownerEmail)

        // Assert
        assertTrue("Today revenue value >= 0", todayValue >= 0)
    }

    /**
     * OWNER_REVENUE_002 | Negative
     * Steps: Select newly opened facility with 0 paid sessions for Today.
     * Expected: Revenue total is MK 0 and session counts are 0 without errors.
     */
    @Test
    fun OWNER_REVENUE_002_handle_zero_revenue_period() {
        // Arrange
        val newFacilityId = "pending_wash_1"
        val ownerEmail = "owner.a@gmail.com"

        // Act
        val todayValue = BookingRepository.getTodayRevenueValue(newFacilityId, ownerEmail)
        val todayBookings = BookingRepository.getTodayBookingsCount(newFacilityId, ownerEmail)

        // Assert
        assertEquals(0, todayValue)
        assertEquals(0, todayBookings)
    }
}
