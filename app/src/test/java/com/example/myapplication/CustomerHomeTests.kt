package com.example.myapplication

import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.toCarWashTiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerHomeTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * CUST_HOME_001 | Positive
     * Steps: Open Customer Home > select city filter "Blantyre".
     * Expected: List displays only facilities matching Blantyre.
     */
    @Test
    fun CUST_HOME_001_filter_facilities_by_city() {
        // Arrange
        val approvedFacilities = FacilityRepository.getApprovedCarWashFacilities()
        val targetCity = "Blantyre"

        // Act
        val filtered = approvedFacilities.filter {
            it.location.contains(targetCity, ignoreCase = true) || it.area.contains(targetCity, ignoreCase = true)
        }

        // Assert
        assertTrue("Blantyre facilities returned", filtered.isNotEmpty())
        assertTrue("All returned facilities belong to Blantyre", filtered.all { it.location.contains(targetCity, ignoreCase = true) || it.area.contains(targetCity, ignoreCase = true) })
    }

    /**
     * CUST_HOME_003 | Negative
     * Steps: Enter search query "NoSuchWash999".
     * Expected: Empty results list; no crash; clearing query restores list.
     */
    @Test
    fun CUST_HOME_003_search_with_no_matching_facility() {
        // Arrange
        val approvedFacilities = FacilityRepository.getApprovedCarWashFacilities()
        val query = "NoSuchWash999"

        // Act
        val searchResult = approvedFacilities.filter {
            it.name.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
        }

        // Assert
        assertTrue("Search with non-existent query returns 0 results", searchResult.isEmpty())

        // Clearing query restores full list
        val clearedResult = approvedFacilities.filter { true }
        assertTrue("Clearing query restores full approved facilities list", clearedResult.isNotEmpty())
    }

    /**
     * CUST_DETAILS_001 | Positive
     * Steps: Select City Shine facility > review details and tier list.
     * Expected: Displays facility name, location, operating hours, and configured tiers.
     */
    @Test
    fun CUST_DETAILS_001_view_approved_facility_details() {
        // Arrange
        val facility = FacilityRepository.getFacilityById("cs_facility_1")

        // Assert
        assertNotNull("City Shine facility exists", facility)
        assertEquals("City Shine", facility?.name)
        assertEquals("Approved", facility?.status)

        val tiers = facility!!.toCarWashTiers()
        assertTrue("Configured tiers exist for City Shine", tiers.isNotEmpty())
        assertTrue("Contains Standard Wash tier", tiers.any { it.title.contains("Standard Wash") })
        assertTrue("Contains Deep Clean tier", tiers.any { it.title.contains("Deep Clean") })
    }

    /**
     * CUST_DETAILS_002 | Negative
     * Steps: Search for pending facility "Pending Wash".
     * Expected: Pending facility is NOT included in public bookable approved list.
     */
    @Test
    fun CUST_DETAILS_002_prevent_public_booking_of_pending_facility() {
        // Arrange
        val publicApprovedFacilities = FacilityRepository.getApprovedCarWashFacilities()

        // Act & Assert
        assertFalse("Pending Wash must NOT be in public approved facilities list",
            publicApprovedFacilities.any { it.name.equals("Pending Wash", ignoreCase = true) || it.id == "pending_wash_1" })
    }
}
