package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.ui.SignUpScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signUpScreen_rendersCoreFields() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen()
            }
        }

        composeTestRule.onNodeWithText("Full Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phone Number (+265)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Email Address").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Type").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signUpScreen_selectRole_displaysRoleOption() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen()
            }
        }

        composeTestRule.onNodeWithText("Car Wash Owner").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Car Wash Owner").assertIsDisplayed()
    }

    @Test
    fun signUpScreen_signInLink_triggersCallback() {
        var signInClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen(
                    onSignInClick = { signInClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Sign In").performScrollTo().performClick()
        assertTrue(signInClicked)
    }
}
