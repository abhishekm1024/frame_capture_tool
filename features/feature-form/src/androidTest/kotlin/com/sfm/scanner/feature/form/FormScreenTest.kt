package com.sfm.scanner.feature.form

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setInitialContent() {
        composeTestRule.setContent {
            ScanAppTheme {
                FormScreenContent(
                    uiState = FormUiState(),
                    onDropdownSelected = {},
                    onSizeChanged = {},
                    onSizeFocusLost = {},
                    onDetailChanged = {},
                    onGtChanged = {},
                    onGtFocusLost = {},
                    onProceed = {},
                    onNavigateBack = {},
                )
            }
        }
    }

    @Test
    fun proceedButton_isDisabled_withNoInput() {
        setInitialContent()
        composeTestRule.onNodeWithText("Proceed to Scan").assertIsNotEnabled()
    }

    @Test
    fun proceedButton_isEnabled_afterAllRequiredFieldsFilledIn() {
        composeTestRule.setContent {
            ScanAppTheme {
                FormScreenContent(
                    uiState = FormUiState(
                        dropdownSelection = "type_a",
                        sizeInput = "5",
                        detailInput = "ABC",
                        proceedEnabled = true,
                    ),
                    onDropdownSelected = {},
                    onSizeChanged = {},
                    onSizeFocusLost = {},
                    onDetailChanged = {},
                    onGtChanged = {},
                    onGtFocusLost = {},
                    onProceed = {},
                    onNavigateBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Proceed to Scan").assertIsEnabled()
    }

    @Test
    fun proceedButton_isDisabled_whenSizeIsInvalid() {
        composeTestRule.setContent {
            ScanAppTheme {
                FormScreenContent(
                    uiState = FormUiState(
                        dropdownSelection = "type_a",
                        sizeInput = "abc",
                        sizeError = "Must be a positive integer",
                        detailInput = "ABC",
                        proceedEnabled = false,
                    ),
                    onDropdownSelected = {},
                    onSizeChanged = {},
                    onSizeFocusLost = {},
                    onDetailChanged = {},
                    onGtChanged = {},
                    onGtFocusLost = {},
                    onProceed = {},
                    onNavigateBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Proceed to Scan").assertIsNotEnabled()
    }

    @Test
    fun dropdownMenu_displaysOptions_whenExpanded() {
        setInitialContent()
        composeTestRule.onNodeWithText("Select type...").assertExists()
        composeTestRule.onNodeWithText("Type A").assertDoesNotExist()

        composeTestRule.onNodeWithText("Select type...").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Type A").assertExists()
        composeTestRule.onNodeWithText("Type B").assertExists()
    }

    @Test
    fun detailField_showsCharacterCounter_whenInputExceeds10Chars() {
        composeTestRule.setContent {
            ScanAppTheme {
                FormScreenContent(
                    uiState = FormUiState(detailInput = "12345678901"),
                    onDropdownSelected = {},
                    onSizeChanged = {},
                    onSizeFocusLost = {},
                    onDetailChanged = {},
                    onGtChanged = {},
                    onGtFocusLost = {},
                    onProceed = {},
                    onNavigateBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("11 / 16").assertExists()
    }
}
