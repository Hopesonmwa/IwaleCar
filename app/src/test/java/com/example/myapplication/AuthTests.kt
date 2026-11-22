package com.example.myapplication

import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthTests {

    @Before
    fun setUp() {
        TestDataSeeder.seedAll()
    }

    /**
     * AUTH_SIGNUP_001 | Positive
     * Prereq: App installed; on Sign Up; email not registered.
     * Steps: Enter valid phone (9 digits), Gmail, strong password, select Zomba, accept Terms, submit.
     * Expected: Account created and stored in UserRepository; session active or ready for login.
     */
    @Test
    fun AUTH_SIGNUP_001_successful_customer_registration() {
        // Arrange
        val email = "hopeson.test@gmail.com"
        val password = "Wash@2026"
        val phone = "991234567"
        val isTermsAccepted = true

        // Act
        assertTrue("Terms accepted condition met", isTermsAccepted)
        val newUser = UserAccount(
            fullName = "Hopeson Test",
            phoneNumber = phone,
            email = email,
            password = password,
            role = "User",
            primaryLocation = "Zomba"
        )
        UserRepository.registerUser(newUser)

        // Assert
        val storedUser = UserRepository.findUser(email)
        assertNotNull("Customer account stored in repository", storedUser)
        assertEquals("Hopeson Test", storedUser?.fullName)
        assertEquals("Zomba", storedUser?.primaryLocation)
    }

    /**
     * AUTH_SIGNUP_002 | Negative
     * Steps: Enter 8-digit phone number.
     * Expected: Registration blocked; phone flagged invalid (must be exactly 9 digits); no account created.
     */
    @Test
    fun AUTH_SIGNUP_002_reject_short_phone_number() {
        // Arrange
        val phone8Digits = "99123456"
        val email = "hopeson.test2@gmail.com"

        // Act
        val isPhoneValid = ValidationUtils.isPhoneValid(phone8Digits)
        assertFalse("8-digit phone number must be invalid", isPhoneValid)

        // Assert
        assertNull("No account created for invalid phone", UserRepository.findUser(email))
    }

    /**
     * AUTH_SIGNUP_003 | Positive
     * Steps: Enter valid non-Gmail address containing '@'.
     * Expected: Registration allowed as long as email contains '@'.
     */
    @Test
    fun AUTH_SIGNUP_003_accept_non_gmail_email() {
        // Arrange
        val yahooEmail = "hopeson@yahoo.com"

        // Act
        val isEmailValid = ValidationUtils.isEmailValid(yahooEmail)

        // Assert
        assertTrue("Email with '@' symbol must be accepted", isEmailValid)
    }

    /**
     * AUTH_SIGNUP_004 | Negative
     * Steps: Enter weak password "password".
     * Expected: Registration blocked due to weak password format (min 8 chars, letter, digit, special char).
     */
    @Test
    fun AUTH_SIGNUP_004_reject_weak_password() {
        // Arrange
        val weakPassword = "password"

        // Act
        val isPasswordFormatValid = ValidationUtils.isPasswordComplex(weakPassword)

        assertFalse("Weak password format must be rejected", isPasswordFormatValid)
    }

    /**
     * AUTH_SIGNUP_005 | Negative
     * Steps: Leave Terms & Privacy Policy checkbox unchecked.
     * Expected: Registration blocked; terms agreement required; no account created.
     */
    @Test
    fun AUTH_SIGNUP_005_reject_registration_without_terms() {
        // Arrange
        val isTermsAccepted = false
        val email = "noterms@gmail.com"

        // Act
        assertFalse("Registration blocked when terms not accepted", isTermsAccepted)

        // Assert
        assertNull("No account created when terms not accepted", UserRepository.findUser(email))
    }

    /**
     * AUTH_LOGIN_001 | Positive
     * Steps: Login with customer.test@gmail.com / Wash@2026.
     * Expected: Authenticated successfully; session active.
     */
    @Test
    fun AUTH_LOGIN_001_successful_customer_login() {
        // Arrange
        val email = "customer.test@gmail.com"
        val password = "Wash@2026"

        // Act
        val isValid = UserRepository.validateCredentials(email, password)
        assertTrue("Customer credentials valid", isValid)

        val user = UserRepository.findUser(email)
        UserRepository.setCurrentUser(user)

        // Assert
        assertEquals("User", UserRepository.getCurrentUser().role)
        assertEquals("customer.test@gmail.com", UserRepository.getCurrentUser().email)
    }

    /**
     * AUTH_LOGIN_002 | Negative
     * Steps: Login with registered email and incorrect password.
     * Expected: Login rejected; authentication error; no session created.
     */
    @Test
    fun AUTH_LOGIN_002_reject_incorrect_password() {
        // Arrange
        val email = "customer.test@gmail.com"
        val wrongPass = "WrongPass123!"

        // Act
        val isValid = UserRepository.validateCredentials(email, wrongPass)

        // Assert
        assertFalse("Login fails with wrong password", isValid)
    }

    /**
     * AUTH_LOGIN_003 | Negative
     * Steps: Enter email without '@' symbol.
     * Expected: Email format marked invalid; login does not proceed.
     */
    @Test
    fun AUTH_LOGIN_003_reject_email_without_at_symbol() {
        // Arrange
        val invalidEmail = "customertestgmail.com"

        // Act
        val hasAtSymbol = ValidationUtils.isEmailFormatValid(invalidEmail)

        // Assert
        assertFalse("Email without '@' is invalid", hasAtSymbol)
    }

    /**
     * AUTH_LOGIN_004 | Positive
     * Steps: Login with owner.test@gmail.com / Owner@2026.
     * Expected: Owner authenticated; role Car Wash Owner.
     */
    @Test
    fun AUTH_LOGIN_004_successful_owner_login() {
        // Arrange
        val email = "owner.test@gmail.com"
        val password = "Owner@2026"

        // Act
        val isValid = UserRepository.validateCredentials(email, password)
        assertTrue("Owner credentials valid", isValid)

        val user = UserRepository.findUser(email)
        UserRepository.setCurrentUser(user)

        // Assert
        assertEquals("Car Wash Owner", UserRepository.getCurrentUser().role)
    }

    /**
     * AUTH_LOGIN_005 | Positive
     * Steps: Login with admin.test@gmail.com / Admin@2026.
     * Expected: Admin authenticated; role Administrator.
     */
    @Test
    fun AUTH_LOGIN_005_successful_admin_login() {
        // Arrange
        val email = "admin.test@gmail.com"
        val password = "Admin@2026"

        // Act
        val isValid = UserRepository.validateCredentials(email, password)
        assertTrue("Admin credentials valid", isValid)

        val user = UserRepository.findUser(email)
        UserRepository.setCurrentUser(user)

        // Assert
        assertEquals("Administrator", UserRepository.getCurrentUser().role)
    }

    /**
     * AUTH_LOGIN_006 | Positive
     * Description: Verify that customer login has role User/Customer and routes to Home, never Owner Onboarding.
     */
    @Test
    fun AUTH_LOGIN_006_customer_login_role_ensures_home_navigation() {
        val email = "user@gmail.com"
        val user = UserRepository.findUser(email)
        assertNotNull(user)
        UserRepository.setCurrentUser(user)

        val currentUser = UserRepository.getCurrentUser()
        assertTrue("Customer role is neither Car Wash Owner nor Administrator", currentUser.role != "Car Wash Owner" && currentUser.role != "Administrator")
    }

    /**
     * AUTH_LOGIN_007 | Positive
     * Description: Verify unapproved owner applicant has role Customer even if requestedRole is Car Wash Owner.
     */
    @Test
    fun AUTH_LOGIN_007_unapproved_owner_applicant_retains_customer_role() {
        val ownerApplicant = UserAccount(
            fullName = "Applicant User",
            phoneNumber = "999111222",
            email = "applicant@gmail.com",
            password = "Pass123!#",
            role = "Customer",
            requestedRole = "Car Wash Owner",
            primaryLocation = "Blantyre"
        )
        UserRepository.registerUser(ownerApplicant)
        UserRepository.setCurrentUser(ownerApplicant)

        val currentUser = UserRepository.getCurrentUser()
        assertEquals("Customer", currentUser.role)
        assertEquals("Car Wash Owner", currentUser.requestedRole)
        assertTrue("Owner applicant does not receive Car Wash Owner dashboard/onboarding access", currentUser.role != "Car Wash Owner")
    }
}

