package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               HomeScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_rendersGreetingAndHeader() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen()
            }
        }

        composeTestRule.onNodeWithText("Nearby Car Washes").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lilongwe").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysGreetingBasedOnLoginEmail() {
        UserRepository.setCurrentUser(
            UserAccount(
                fullName = "Chaps Hopeson",
                phoneNumber = "999111222",
                email = "chaps.hopeson@gmail.com",
                password = "Pass123!",
                role = "User",
                primaryLocation = "Lilongwe"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen()
            }
        }

        composeTestRule.onNodeWithText("Hello, chaps.hopeson").assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchNoResults_showsEmptyCard() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen()
            }
        }

        composeTestRule.onNodeWithText("Search express bays...").performTextInput("NonExistentStation999")

        composeTestRule.onNodeWithText("No car wash facilities found.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Search").assertIsDisplayed()
    }

    @Test
    fun homeScreen_clearSearchButton_clearsQueryAndRestores() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen()
            }
        }

        composeTestRule.onNodeWithText("Search express bays...").performTextInput("NonExistentStation999")
        composeTestRule.onNodeWithText("Clear Search").performClick()

        composeTestRule.onNodeWithText("Nearby Car Washes").assertIsDisplayed()
    }
}
