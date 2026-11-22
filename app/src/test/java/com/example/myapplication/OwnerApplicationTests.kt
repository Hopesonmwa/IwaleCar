package com.example.myapplication

import com.example.myapplication.data.ApplicationStatus
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.OwnerApplicationValidator
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Default admin account, owner sign-up/application flow, admin approval and post-login routing. */
class OwnerApplicationTests {

    private val applicant = UserAccount(
        fullName = "Tendai Phiri",
        phoneNumber = "999111000",
        email = "tendai.owner@gmail.com",
        password = "Pass123!#",
        role = "Customer",
        requestedRole = "Car Wash Owner",
        primaryLocation = "Blantyre"
    )

    @Before
    fun setUp() {
        UserRepository.resetToDefaultData()
        OwnerApplicationRepository.resetToDefaultData()
        UserRepository.registerUser(applicant)
    }

    private fun submitValid() = runBlocking {
        OwnerApplicationRepository.submit(
            applicant = applicant,
            businessName = "Sparkle Wash",
            registrationNumber = "BR-2026-001",
            businessAddress = "Plot 7, Chilomoni",
            city = "Blantyre",
            numberOfBays = "3",
            experienceYears = "2",
            description = "Foam and pressure wash"
        )
    }

    // ---------- Default admin ----------

    @Test
    fun default_admin_can_authenticate_and_is_routed_to_admin_dashboard() {
        assertTrue(UserRepository.validateCredentials("admin@email.com", "2400@hope24"))
        assertFalse(UserRepository.validateCredentials("admin@email.com", "wrong"))

        val admin = UserRepository.findUser("admin@email.com")
        assertNotNull(admin)
        assertEquals("Administrator", admin?.role)
        assertEquals(Screen.ADMIN_DASHBOARD, routeForUser(admin))
    }

    // ---------- Routing after login ----------

    @Test
    fun owner_applicant_is_routed_to_owner_application_screen() {
        assertEquals(Screen.OWNER_ONBOARDING, routeForUser(applicant))
    }

    @Test
    fun customer_and_approved_owner_routing() {
        val customer = applicant.copy(requestedRole = "Customer")
        assertEquals(Screen.HOME, routeForUser(customer))
        assertEquals(Screen.OWNER_DASHBOARD, routeForUser(applicant.copy(role = "Car Wash Owner")))
        assertEquals(Screen.HOME, routeForUser(null))
    }

    // ---------- Validation ----------

    @Test
    fun validator_rejects_bad_input_and_accepts_good_input() {
        assertNotNull(OwnerApplicationValidator.validateFullApplication("", "999111", "city", "dist", "addr", "Mon-Sat", "06:30 AM", "08:00 PM"))
        assertNotNull(OwnerApplicationValidator.validateFullApplication("Biz", "", "city", "dist", "addr", "Mon-Sat", "06:30 AM", "08:00 PM"))
        assertNotNull(OwnerApplicationValidator.validateFullApplication("Biz", "999111", "", "dist", "addr", "Mon-Sat", "06:30 AM", "08:00 PM"))
        assertNotNull(OwnerApplicationValidator.validateFullApplication("Biz", "999111", "city", "", "addr", "Mon-Sat", "06:30 AM", "08:00 PM"))
        assertNotNull(OwnerApplicationValidator.validateFullApplication("Biz", "999111", "city", "dist", "", "Mon-Sat", "06:30 AM", "08:00 PM"))
        assertNull(OwnerApplicationValidator.validateFullApplication("Biz", "999111", "Blantyre", "Blantyre Urban", "Plot 7", "Monday - Saturday", "06:30 AM", "08:00 PM"))
    }

    // ---------- Application lifecycle ----------

    @Test
    fun submit_creates_pending_application() {
        val result = submitValid()
        assertTrue(result.isSuccess)
        val stored = OwnerApplicationRepository.getApplicationFor(applicant.email)
        assertEquals(ApplicationStatus.PENDING, stored?.status)
        assertEquals("Sparkle Wash", stored?.businessName)
        assertEquals(1, OwnerApplicationRepository.pendingCount())
    }

    @Test
    fun cannot_submit_twice_while_pending() {
        assertTrue(submitValid().isSuccess)
        assertTrue(submitValid().isFailure)
        assertEquals(1, OwnerApplicationRepository.getAll().size)
    }

    @Test
    fun invalid_submission_is_rejected_and_not_stored() {
        val result = runBlocking {
            OwnerApplicationRepository.submit(applicant, "", "", "", "", "0", "", "")
        }
        assertTrue(result.isFailure)
        assertNull(OwnerApplicationRepository.getApplicationFor(applicant.email))
    }

    @Test
    fun admin_approval_promotes_applicant_to_car_wash_owner() {
        submitValid()
        val result = runBlocking {
            OwnerApplicationRepository.approve(applicant.email, "admin@email.com")
        }
        assertTrue(result.isSuccess)
        assertEquals(ApplicationStatus.APPROVED, result.getOrNull()?.status)
        assertEquals("admin@email.com", result.getOrNull()?.reviewedBy)
        assertEquals(0, OwnerApplicationRepository.pendingCount())

        val user = UserRepository.findUser(applicant.email)
        assertEquals("Car Wash Owner", user?.role)
        assertEquals(Screen.OWNER_DASHBOARD, routeForUser(user))
    }

    @Test
    fun rejection_requires_notes_then_allows_resubmission() {
        submitValid()

        val noNotes = runBlocking { OwnerApplicationRepository.reject(applicant.email, "admin@email.com", "  ") }
        assertTrue(noNotes.isFailure)
        assertEquals(ApplicationStatus.PENDING, OwnerApplicationRepository.getApplicationFor(applicant.email)?.status)

        val rejected = runBlocking {
            OwnerApplicationRepository.reject(applicant.email, "admin@email.com", "Missing registration number")
        }
        assertTrue(rejected.isSuccess)
        val stored = OwnerApplicationRepository.getApplicationFor(applicant.email)
        assertEquals(ApplicationStatus.REJECTED, stored?.status)
        assertEquals("Missing registration number", stored?.reviewNotes)
        // Rejected applicants stay customers.
        assertEquals("Customer", UserRepository.findUser(applicant.email)?.role)

        // They can fix and resubmit; the application goes back to PENDING without the old notes.
        assertTrue(submitValid().isSuccess)
        val resubmitted = OwnerApplicationRepository.getApplicationFor(applicant.email)
        assertEquals(ApplicationStatus.PENDING, resubmitted?.status)
        assertEquals("", resubmitted?.reviewNotes)
        assertEquals(1, OwnerApplicationRepository.getAll().size)
    }

    @Test
    fun an_already_reviewed_application_cannot_be_reviewed_again() {
        submitValid()
        runBlocking { OwnerApplicationRepository.approve(applicant.email, "admin@email.com") }
        val again = runBlocking { OwnerApplicationRepository.reject(applicant.email, "admin@email.com", "Changed my mind") }
        assertTrue(again.isFailure)
        assertEquals(ApplicationStatus.APPROVED, OwnerApplicationRepository.getApplicationFor(applicant.email)?.status)
    }

    @Test
    fun approving_an_unknown_application_fails() {
        val result = runBlocking { OwnerApplicationRepository.approve("nobody@gmail.com", "admin@email.com") }
        assertTrue(result.isFailure)
    }
}
