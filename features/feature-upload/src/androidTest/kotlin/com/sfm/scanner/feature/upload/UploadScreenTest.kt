package com.sfm.scanner.feature.upload

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(state: UploadUiState, onStartNewScan: () -> Unit = {}) {
        composeTestRule.setContent {
            ScanAppTheme {
                UploadScreenContent(uiState = state, onStartNewScan = onStartNewScan)
            }
        }
    }

    @Test
    fun authenticating_state_shows_preparing_label_and_progress_indicator() {
        setContent(UploadUiState.Authenticating)
        composeTestRule.onNodeWithText("Preparing upload...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Preparing upload, please wait").assertIsDisplayed()
    }

    @Test
    fun uploading_state_shows_filename_and_percent() {
        setContent(UploadUiState.Uploading(percent = 42, filename = "scan-abc.zip"))
        composeTestRule.onNodeWithText("Uploading scan...").assertIsDisplayed()
        composeTestRule.onNodeWithText("scan-abc.zip").assertIsDisplayed()
        composeTestRule.onNodeWithText("42%").assertIsDisplayed()
    }

    @Test
    fun success_state_shows_title_body_and_new_scan_button() {
        setContent(UploadUiState.Success)
        composeTestRule.onNodeWithText("Upload complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your scan has been uploaded successfully.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start New Scan").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Upload successful").assertIsDisplayed()
    }

    @Test
    fun failed_state_shows_will_retry_body_and_new_scan_button() {
        setContent(UploadUiState.Failed(filename = "scan-xyz.zip"))
        composeTestRule.onNodeWithText("Upload failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Will retry automatically when connected.").assertIsDisplayed()
        composeTestRule.onNodeWithText("scan-xyz.zip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start New Scan").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Upload failed").assertIsDisplayed()
    }

    @Test
    fun retry_queued_state_shows_queued_note_and_new_scan_button() {
        setContent(UploadUiState.RetryQueued)
        composeTestRule.onNodeWithText("Upload failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upload has been queued and will retry automatically.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start New Scan").assertIsDisplayed()
    }

    @Test
    fun start_new_scan_button_invokes_callback_in_success_state() {
        var clicked = false
        setContent(UploadUiState.Success, onStartNewScan = { clicked = true })
        composeTestRule.onNodeWithText("Start New Scan").performClick()
        assertTrue("Start New Scan click should trigger callback", clicked)
    }
}
