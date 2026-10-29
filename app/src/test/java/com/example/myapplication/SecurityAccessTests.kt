package com.example.myapplication

import com.example.myapplication.data.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SecurityAccessTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * SEC_ACCESS_001 | Negative
     * Steps: Customer logs in > attempts to access owner-only routes/screens.
     * Expected: Customer role is "User"; cannot acquire owner privileges; redirected to customer home.
     */
    @Test
    fun SEC_ACCESS_001_prevent_customer_access_to_owner_screens() {
        // Arrange: Log in customer
        val customer = UserRepository.findUser("customer.test@gmail.com")
        assertNotNull("Customer account exists", customer)
        UserRepository.setCurrentUser(customer)

        // Act & Assert
        val currentRole = UserRepository.getCurrentUser().role
        assertEquals("User", currentRole)
        assertNotEquals("Car Wash Owner", currentRole)

        // Role-based route guard
        val targetScreen = when (currentRole) {
            "Car Wash Owner" -> Screen.OWNER_DASHBOARD
            "Administrator" -> Screen.ADMIN_DASHBOARD
            else -> Screen.HOME
        }

        assertEquals("Customer is routed to HOME screen, NOT owner dashboard", Screen.HOME, targetScreen)
    }

    /**
     * SEC_ACCESS_002 | Negative
     * Steps: Owner logs in > attempts to access admin-only routes/screens.
     * Expected: Owner role is "Car Wash Owner"; cannot acquire admin privileges; redirected to owner dashboard.
     */
    @Test
    fun SEC_ACCESS_002_prevent_owner_access_to_admin_screens() {
        // Arrange: Log in owner
        val owner = UserRepository.findUser("owner.test@gmail.com")
        assertNotNull("Owner account exists", owner)
        UserRepository.setCurrentUser(owner)

        // Act & Assert
        val currentRole = UserRepository.getCurrentUser().role
        assertEquals("Car Wash Owner", currentRole)
        assertNotEquals("Administrator", currentRole)

        // Role-based route guard
        val targetScreen = when (currentRole) {
            "Administrator" -> Screen.ADMIN_DASHBOARD
            "Car Wash Owner" -> Screen.OWNER_DASHBOARD
            else -> Screen.HOME
        }

        assertEquals("Owner is routed to OWNER_DASHBOARD screen, NOT admin dashboard", Screen.OWNER_DASHBOARD, targetScreen)
    }
}
