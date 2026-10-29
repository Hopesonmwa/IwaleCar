package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.ui.ManageBaysScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageBaysUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun manageBaysScreen_rendersTitleAndBays() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ManageBaysScreen()
            }
        }

        composeTestRule.onNodeWithText("Manage Bays & Pricing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Bay").assertIsDisplayed()
    }

    @Test
    fun manageBaysScreen_openAddBayModal_showsModal() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ManageBaysScreen()
            }
        }

        composeTestRule.onNodeWithText("Add Bay").performClick()

        composeTestRule.onNodeWithText("Add New Bay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bay Name / Number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service / Package Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Bay Configuration").assertIsDisplayed()
    }

    @Test
    fun manageBaysScreen_clickRemoveBay_showsConfirmationDialog() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ManageBaysScreen()
            }
        }

        composeTestRule.onAllNodesWithText("Remove").onFirst().performClick()

        composeTestRule.onNodeWithText("Remove Bay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
