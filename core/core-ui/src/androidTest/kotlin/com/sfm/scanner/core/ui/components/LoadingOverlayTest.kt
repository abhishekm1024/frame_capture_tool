package com.sfm.scanner.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingOverlay_rendersWithoutLabel() {
        composeTestRule.setContent {
            ScanAppTheme {
                LoadingOverlay()
            }
        }

        // The spinner (CircularProgressIndicator) is present; no crash means it rendered
        composeTestRule.onNodeWithText("Uploading").assertDoesNotExist()
    }

    @Test
    fun loadingOverlay_showsLabelWhenProvided() {
        composeTestRule.setContent {
            ScanAppTheme {
                LoadingOverlay(label = "Uploading…")
            }
        }

        composeTestRule.onNodeWithText("Uploading…").assertIsDisplayed()
    }

    @Test
    fun loadingOverlay_noLabelTextWhenNull() {
        composeTestRule.setContent {
            ScanAppTheme {
                LoadingOverlay(label = null)
            }
        }

        composeTestRule.onNodeWithText("null").assertDoesNotExist()
    }
}
