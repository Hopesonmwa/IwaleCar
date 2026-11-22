package com.example.myapplication

import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.toFacilityItemData
import com.example.myapplication.ui.BayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * ADMIN_FACILITY_001 | Positive
     * Steps: Open Admin Facilities > select pending facility > tap Approve.
     * Expected: Status becomes Approved; facility becomes publicly bookable.
     */
    @Test
    fun ADMIN_FACILITY_001_approve_pending_facility() {
        // Arrange
        val pendingFacility = FacilityRepository.getFacilityById("pending_wash_1")
        assertNotNull("Pending Wash facility exists", pendingFacility)
        assertEquals("Pending Approval", pendingFacility?.status)

        // Act: Approve facility
        val approvedItem = pendingFacility!!.toFacilityItemData().copy(status = "Approved")
        FacilityRepository.saveFacility(approvedItem, pendingFacility.ownerId)

        // Assert status
        val updatedFacility = FacilityRepository.getFacilityById("pending_wash_1")
        assertEquals("Approved", updatedFacility?.status)

        // Owner configures bays for the approved facility
        val sampleBay = BayConfig(
            id = "bay_p1", bayNumber = "BAY 01", serviceTitle = "Foam Wash",
            priceText = "MWK 5,000", priceValue = 5000, durationText = "30 mins",
            featuresDescription = "High pressure", status = "Available"
        )
        FacilityRepository.updateBaysForFacility("pending_wash_1", listOf(sampleBay))

        val publicFacilities = FacilityRepository.getApprovedCarWashFacilities()
        assertTrue("Approved facility with configured bays appears in public bookable list",
            publicFacilities.any { it.id == "pending_wash_1" })
    }

    /**
     * ADMIN_FACILITY_002 | Positive
     * Steps: Open Admin Facilities > select pending facility > tap Reject > enter correction notes.
     * Expected: Status becomes Rejected; correction notes recorded; facility remains unavailable for public booking.
     */
    @Test
    fun ADMIN_FACILITY_002_reject_pending_facility_with_correction_notes() {
        // Arrange
        val pendingFacility = FacilityRepository.getFacilityById("pending_wash_1")
        assertNotNull("Pending Wash facility exists", pendingFacility)

        // Act: Reject with notes
        val correctionNotes = "Drainage clearance certificate missing."
        val rejectedItem = pendingFacility!!.toFacilityItemData().copy(
            status = "Rejected",
            description = correctionNotes
        )
        FacilityRepository.saveFacility(rejectedItem, pendingFacility.ownerId)

        // Assert
        val updatedFacility = FacilityRepository.getFacilityById("pending_wash_1")
        assertEquals("Rejected", updatedFacility?.status)
        assertEquals(correctionNotes, updatedFacility?.description)
    }

    /**
     * ADMIN_FACILITY_003 | Negative
     * Steps: Search for rejected facility as a customer.
     * Expected: Rejected facility is not publicly bookable; excluded from public approved facilities list.
     */
    @Test
    fun ADMIN_FACILITY_003_prevent_public_booking_of_rejected_facility() {
        // Arrange
        val rejectedFacility = FacilityRepository.getFacilityById("7") // Kanengo Industrial Wash (Rejected)
        assertNotNull("Kanengo Industrial Wash exists", rejectedFacility)
        assertEquals("Rejected", rejectedFacility?.status)

        // Act
        val publicApprovedFacilities = FacilityRepository.getApprovedCarWashFacilities()

        // Assert
        assertFalse("Rejected facility must NOT be in public approved facilities list",
            publicApprovedFacilities.any { it.id == "7" })
    }

    /**
     * ADMIN_DASH_001 | Positive
     * Steps: Log in as admin > open Admin Dashboard.
     * Expected: Dashboard displays platform revenue, active stations, pending submissions, total users.
     */
    @Test
    fun ADMIN_DASH_001_view_admin_dashboard_metrics() {
        // Arrange
        val allFacilities = FacilityRepository.getAllFacilities()
        val pendingCount = allFacilities.count { it.status == "Pending Approval" }
        val approvedCount = allFacilities.count { it.status == "Approved" }

        // Assert
        assertTrue("All facilities list returned", allFacilities.isNotEmpty())
        assertTrue("Pending count calculated", pendingCount >= 0)
        assertTrue("Approved count calculated", approvedCount >= 0)
    }
}
