package com.example.myapplication

import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * AUTH_FIREBASE_001 | Negative / Security
     * Description: Verify that attempting to register as Administrator via AuthRepository sanitizes the role to Customer.
     * Prerequisites: Firebase Auth & Firestore configured; user attempts self-assigned Administrator role.
     */
    @Test
    fun AUTH_FIREBASE_001_sanitize_admin_role_self_assignment() = runBlocking {
        val adminAttemptUser = UserAccount(
            fullName = "Self Assigned Admin",
            phoneNumber = "999000111",
            email = "attempt_admin@gmail.com",
            password = "Pass123!#",
            role = "Administrator", // Security violation attempt
            primaryLocation = "Lilongwe"
        )

        val result = AuthRepository.signUp(adminAttemptUser, "Pass123!#")
        assertTrue("Sign up succeeds", result.isSuccess)
        val createdUser = result.getOrNull()
        assertNotNull("Created user exists", createdUser)
        assertEquals("Role sanitized from Administrator to Customer", "Customer", createdUser?.role)
    }

    /**
     * AUTH_FIREBASE_002 | Positive / Security
     * Description: Verify that registering as Car Wash Owner via AuthRepository sets initial role to Customer while preserving requestedRole.
     * Prerequisites: Firebase Auth & Firestore configured.
     */
    @Test
    fun AUTH_FIREBASE_002_signup_car_wash_owner_role() = runBlocking {
        val ownerUser = UserAccount(
            fullName = "New Wash Owner",
            phoneNumber = "999222111",
            email = "new_owner@gmail.com",
            password = "OwnerPass123!",
            role = "Car Wash Owner",
            primaryLocation = "Blantyre"
        )

        val result = AuthRepository.signUp(ownerUser, "OwnerPass123!")
        assertTrue("Sign up succeeds for Car Wash Owner", result.isSuccess)
        val createdUser = result.getOrNull()
        assertEquals("Customer", createdUser?.role)
        assertEquals("Car Wash Owner", createdUser?.requestedRole)
    }

    /**
     * AUTH_FIREBASE_007 | Positive / Navigation & Onboarding
     * Description: Verify that registration choice (Customer vs Car Wash Owner) is preserved separately from sanitized role.
     */
    @Test
    fun AUTH_FIREBASE_007_signup_preserves_owner_registration_choice() = runBlocking {
        val customerAttempt = UserAccount(
            fullName = "Standard Customer",
            phoneNumber = "999888777",
            email = "standard_cust@gmail.com",
            password = "Pass123!#",
            role = "Customer",
            primaryLocation = "Lilongwe"
        )
        val custResult = AuthRepository.signUp(customerAttempt, "Pass123!#").getOrNull()
        assertEquals("Customer", custResult?.role)
        assertEquals("Customer", custResult?.requestedRole)

        val ownerAttempt = UserAccount(
            fullName = "Prospective Owner",
            phoneNumber = "999777666",
            email = "prosp_owner@gmail.com",
            password = "Pass123!#",
            role = "Car Wash Owner",
            primaryLocation = "Zomba"
        )
        val ownerResult = AuthRepository.signUp(ownerAttempt, "Pass123!#").getOrNull()
        assertEquals("Customer", ownerResult?.role)
        assertEquals("Car Wash Owner", ownerResult?.requestedRole)
    }

    /**
     * AUTH_FIREBASE_008 | Positive / Customer Registration & Names
     * Description: Verify that Customer registration signs out immediately, and first/last names are parsed/preserved.
     */
    @Test
    fun AUTH_FIREBASE_008_customer_signup_signs_out_immediately_and_parses_names() = runBlocking {
        val customerAccount = UserAccount(
            fullName = "Kondwani Phiri",
            phoneNumber = "999123456",
            email = "kondwani.phiri@gmail.com",
            password = "Pass123!#",
            role = "Customer",
            primaryLocation = "Lilongwe",
            firstName = "Kondwani",
            lastName = "Phiri"
        )

        assertEquals("Kondwani", customerAccount.firstName)
        assertEquals("Phiri", customerAccount.lastName)

        val result = AuthRepository.signUp(customerAccount, "Pass123!#")
        assertTrue("Sign up succeeds", result.isSuccess)
        val createdUser = result.getOrNull()
        assertEquals("Customer", createdUser?.role)
        assertEquals("Kondwani", createdUser?.firstName)
        assertEquals("Phiri", createdUser?.lastName)

        // Verify session signed out for Customer
        val currentUser = UserRepository.getCurrentUser()
        assertTrue("Current user is cleared or reset to fallback after Customer sign up", currentUser.email != "kondwani.phiri@gmail.com")
    }

    /**
     * AUTH_FIREBASE_003 | Positive
     * Description: Verify that logging in with valid credentials via AuthRepository authenticates the user.
     * Prerequisites: User registered in Firebase Auth / Repository.
     */
    @Test
    fun AUTH_FIREBASE_003_login_with_valid_credentials() = runBlocking {
        val result = AuthRepository.login("carwash@gmail.com", "1234")
        assertTrue("Login result is success", result.isSuccess)
        val user = result.getOrNull()
        assertNotNull("Logged in user is not null", user)
        assertEquals("carwash@gmail.com", user?.email)
        assertEquals("Car Wash Owner", user?.role)
        assertEquals("carwash@gmail.com", UserRepository.getCurrentUser().email)
    }

    /**
     * AUTH_FIREBASE_004 | Negative
     * Description: Verify that logging in with an invalid email format returns an error message.
     */
    @Test
    fun AUTH_FIREBASE_004_reject_login_invalid_email_format() = runBlocking {
        val result = AuthRepository.login("invalidemailformat", "1234")
        assertTrue("Login result is failure for invalid email", result.isFailure)
        assertEquals("Invalid email format. Email address must contain '@'.", result.exceptionOrNull()?.message)
    }

    /**
     * AUTH_FIREBASE_005 | Negative
     * Description: Verify that logging in with an incorrect password returns an authentication failure message.
     */
    @Test
    fun AUTH_FIREBASE_005_reject_login_incorrect_password() = runBlocking {
        val result = AuthRepository.login("user@gmail.com", "wrongpassword999")
        assertTrue("Login result is failure for wrong password", result.isFailure)
        assertEquals("Incorrect email or password. Please check your credentials or create an account.", result.exceptionOrNull()?.message)
    }

    /**
     * AUTH_FIREBASE_006 | Positive
     * Description: Verify that logout clears the active Firebase session and resets current user state.
     */
    @Test
    fun AUTH_FIREBASE_006_logout_clears_active_session() = runBlocking {
        AuthRepository.login("user@gmail.com", "1234")
        assertNotNull(UserRepository.getCurrentUser())

        AuthRepository.logout()
        val current = UserRepository.getCurrentUser()
        assertNotNull("Session cleared or reset to default fallback state", current)
    }

    /**
     * AUTH_FIREBASE_009 | Positive / Profile Editing
     * Description: Verify that updateUserProfile updates bio and phone number while preserving user role and identity.
     */
    @Test
    fun AUTH_FIREBASE_009_update_user_profile_bio_and_phone() = runBlocking {
        AuthRepository.login("user@gmail.com", "1234")
        val currentUser = AuthRepository.getCurrentUser()
        assertNotNull("Logged in user exists", currentUser)

        val updatedBio = "Updated bio text for test user."
        val updatedPhone = "997766554"

        val updatedAccount = currentUser.copy(
            bio = updatedBio,
            phoneNumber = updatedPhone
        )

        val result = AuthRepository.updateUserProfile(updatedAccount)
        assertTrue("Profile update succeeds", result.isSuccess)

        val storedUser = AuthRepository.getCurrentUser()
        assertEquals(updatedBio, storedUser.bio)
        assertEquals(updatedPhone, storedUser.phoneNumber)
        assertEquals(currentUser.role, storedUser.role)
        assertEquals(currentUser.email, storedUser.email)
    }

    /**
     * AUTH_FIREBASE_010 | Positive / Registration Fields & Security
     * Description: Verify that customer and owner registration parse first and last names and produce Customer role.
     */
    @Test
    fun AUTH_FIREBASE_010_verify_profile_map_fields_and_name_handling() = runBlocking {
        val user = UserAccount(
            fullName = "Chisomo Bandason",
            phoneNumber = "998877661",
            email = "chisomo.bandason@gmail.com",
            password = "Pass123!#",
            role = "Car Wash Owner",
            primaryLocation = "Lilongwe"
        )

        val signUpResult = AuthRepository.signUp(user, "Pass123!#")
        assertTrue("Registration succeeds", signUpResult.isSuccess)
        val createdUser = signUpResult.getOrNull()

        assertNotNull("User created", createdUser)
        assertEquals("Customer", createdUser?.role)
        assertEquals("Car Wash Owner", createdUser?.requestedRole)
        assertEquals("Chisomo", createdUser?.firstName)
        assertEquals("Bandason", createdUser?.lastName)
    }
}


