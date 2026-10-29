package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.UserAccount
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_rendersCoreElements() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen()
            }
        }

        composeTestRule.onNodeWithText("IWALE ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Car").assertIsDisplayed()
        composeTestRule.onNodeWithText("MALAWI'S SMART CAR WASH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email Address").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In  →").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun loginScreen_invalidEmail_showsErrorMessage() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen()
            }
        }

        composeTestRule.onNodeWithText("Email Address").assertIsDisplayed()
        composeTestRule.onNodeWithText("carwash@gmail.com").performTextClearance()
        composeTestRule.onNodeWithText("").performTextInput("invalidemail")

        composeTestRule.onNodeWithText("Sign In  →").performClick()

        composeTestRule.onNodeWithText("Invalid email format. Email address must contain '@'.").assertIsDisplayed()
    }

    @Test
    fun loginScreen_invalidCredentials_showsErrorMessage() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen()
            }
        }

        composeTestRule.onNodeWithText("carwash@gmail.com").performTextClearance()
        composeTestRule.onNodeWithText("").performTextInput("unknownuser@gmail.com")

        composeTestRule.onNodeWithText("Sign In  →").performClick()

        composeTestRule.onNodeWithText("Incorrect email or password. Please check your credentials or create an account.").assertIsDisplayed()
    }

    @Test
    fun loginScreen_successfulLogin_triggersCallback() {
        var loggedInUser: UserAccount? = null
        var callbackTriggered = false

        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen(
                    onSignInClick = { user ->
                        loggedInUser = user
                        callbackTriggered = true
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Sign In  →").performClick()

        assertTrue(callbackTriggered)
        assertNotNull(loggedInUser)
        assertEquals("carwash@gmail.com", loggedInUser?.email)
    }

    @Test
    fun loginScreen_createAccountClick_triggersCallback() {
        var createAccountClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen(
                    onCreateAccountClick = { createAccountClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Create Account").performClick()

        assertTrue(createAccountClicked)
    }
}
