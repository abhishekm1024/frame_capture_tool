package com.sfm.scanner.core.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class PrimaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_showsLabel() {
        composeTestRule.setContent {
            ScanAppTheme {
                PrimaryButton(label = "Continue", onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Continue").assertExists()
    }

    @Test
    fun primaryButton_enabledByDefault() {
        composeTestRule.setContent {
            ScanAppTheme {
                PrimaryButton(label = "Go", onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Go").assertIsEnabled()
    }

    @Test
    fun primaryButton_disabledWhenEnabledFalse() {
        composeTestRule.setContent {
            ScanAppTheme {
                PrimaryButton(label = "Go", onClick = {}, enabled = false)
            }
        }

        composeTestRule.onNodeWithText("Go").assertIsNotEnabled()
    }

    @Test
    fun primaryButton_invokesOnClick() {
        var clicked = false

        composeTestRule.setContent {
            ScanAppTheme {
                PrimaryButton(label = "Click Me", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Click Me").performClick()
        assertTrue(clicked)
    }

    @Test
    fun primaryButton_doesNotInvokeOnClickWhenDisabled() {
        var clicked = false

        composeTestRule.setContent {
            ScanAppTheme {
                PrimaryButton(label = "Click Me", onClick = { clicked = true }, enabled = false)
            }
        }

        composeTestRule.onNodeWithText("Click Me").performClick()
        assertTrue(!clicked)
    }
}
