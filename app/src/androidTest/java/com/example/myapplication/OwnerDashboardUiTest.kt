package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.OwnerDashboardScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerDashboardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        UserRepository.resetToDefaultData()
        FacilityRepository.resetToDefaultData()
    }

    @After
    fun tearDown() {
        UserRepository.resetToDefaultData()
        FacilityRepository.resetToDefaultData()
    }

    @Test
    fun ownerDashboard_rendersHeaderAndMetrics() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerDashboardScreen()
            }
        }

        composeTestRule.onNodeWithText("Owner Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Bays & Tiers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's Revenue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Bookings").assertIsDisplayed()
    }

    @Test
    fun ownerDashboard_manageBaysClick_triggersCallback() {
        var manageBaysClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerDashboardScreen(
                    onManageBaysClick = { manageBaysClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Manage Bays & Tiers").performClick()
        assertTrue(manageBaysClicked)
    }

    @Test
    fun ownerDashboard_viewAllClick_triggersBookingsCallback() {
        var bookingsClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerDashboardScreen(
                    onBookingsClick = { bookingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("View All").performClick()
        assertTrue(bookingsClicked)
    }

    @Test
    fun ownerDashboard_setupDialog_manageBaysClick() {
        var manageBaysClicked = false

        UserRepository.setCurrentUser(
            UserAccount(
                fullName = "New Owner",
                phoneNumber = "999888999",
                email = "incomplete_owner@gmail.com",
                password = "1234",
                role = "Car Wash Owner",
                primaryLocation = "Lilongwe"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerDashboardScreen(
                    onManageBaysClick = { manageBaysClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Set Up Your Car Wash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Bays").performClick()

        assertTrue(manageBaysClicked)
    }
}
