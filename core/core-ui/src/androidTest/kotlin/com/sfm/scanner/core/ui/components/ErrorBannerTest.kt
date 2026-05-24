package com.sfm.scanner.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorBanner_showsMessage() {
        composeTestRule.setContent {
            ScanAppTheme {
                ErrorBanner(message = "Upload failed.")
            }
        }

        composeTestRule.onNodeWithText("Upload failed.").assertIsDisplayed()
    }

    @Test
    fun errorBanner_noActionButtonWhenActionLabelIsNull() {
        composeTestRule.setContent {
            ScanAppTheme {
                ErrorBanner(message = "Something went wrong.", actionLabel = null, onAction = null)
            }
        }

        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorBanner_showsActionButtonWhenProvided() {
        composeTestRule.setContent {
            ScanAppTheme {
                ErrorBanner(
                    message = "Upload failed.",
                    actionLabel = "Retry",
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun errorBanner_invokesOnActionWhenButtonClicked() {
        var actionTriggered = false

        composeTestRule.setContent {
            ScanAppTheme {
                ErrorBanner(
                    message = "Error occurred.",
                    actionLabel = "Retry",
                    onAction = { actionTriggered = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(actionTriggered)
    }

    @Test
    fun errorBanner_showsBothMessageAndAction() {
        composeTestRule.setContent {
            ScanAppTheme {
                ErrorBanner(
                    message = "Camera permission denied.",
                    actionLabel = "Settings",
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Camera permission denied.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
