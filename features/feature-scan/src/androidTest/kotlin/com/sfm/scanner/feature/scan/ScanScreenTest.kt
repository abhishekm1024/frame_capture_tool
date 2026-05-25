package com.sfm.scanner.feature.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun error_state_permission_camera_shows_message_and_open_settings_button() {
        composeTestRule.setContent {
            ScanAppTheme {
                ScanScreenContent(
                    uiState = ScanUiState.Error(ScanStrings.ERROR_PERMISSION_CAMERA),
                    onSurfaceProvider = {},
                    onToggleTorch = {},
                    onStartScan = {},
                    onStopScan = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithText(ScanStrings.ERROR_PERMISSION_CAMERA).assertIsDisplayed()
        composeTestRule.onNodeWithText(ScanStrings.BUTTON_OPEN_SETTINGS).assertIsDisplayed()
    }

    @Test
    fun error_state_ar_unsupported_shows_message_only() {
        composeTestRule.setContent {
            ScanAppTheme {
                ScanScreenContent(
                    uiState = ScanUiState.Error(ScanStrings.ERROR_AR_UNSUPPORTED),
                    onSurfaceProvider = {},
                    onToggleTorch = {},
                    onStartScan = {},
                    onStopScan = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithText(ScanStrings.ERROR_AR_UNSUPPORTED).assertIsDisplayed()
        // No "Open Settings" button for AR-unsupported error
        composeTestRule.onNodeWithText(ScanStrings.BUTTON_OPEN_SETTINGS).assertDoesNotExist()
    }

    @Test
    fun ready_state_shows_start_button_and_instruction() {
        composeTestRule.setContent {
            ScanAppTheme {
                ScanScreenContent(
                    uiState = ScanUiState.Ready(torchOn = false, arDisplayState = ArDisplayState.TRACKING),
                    onSurfaceProvider = {},
                    onToggleTorch = {},
                    onStartScan = {},
                    onStopScan = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithText(ScanStrings.BUTTON_START).assertIsDisplayed()
        composeTestRule.onNodeWithText(ScanStrings.INSTRUCTION_READY).assertIsDisplayed()
    }

    @Test
    fun scanning_state_shows_stop_button_and_frame_counter() {
        composeTestRule.setContent {
            ScanAppTheme {
                ScanScreenContent(
                    uiState = ScanUiState.Scanning(
                        frameCount = 42,
                        torchOn = false,
                        arDisplayState = ArDisplayState.TRACKING,
                    ),
                    onSurfaceProvider = {},
                    onToggleTorch = {},
                    onStartScan = {},
                    onStopScan = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithText(ScanStrings.BUTTON_STOP).assertIsDisplayed()
        composeTestRule.onNodeWithText("Frames: 42").assertIsDisplayed()
    }

    @Test
    fun packaging_screen_content_shows_label() {
        composeTestRule.setContent {
            ScanAppTheme {
                PackagingScreenContent()
            }
        }
        composeTestRule.onNodeWithText(ScanStrings.PACKAGING_LABEL).assertIsDisplayed()
    }
}
