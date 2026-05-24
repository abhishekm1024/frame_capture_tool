package com.sfm.scanner.feature.selection

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testOptions = listOf(
        SelectionOption(id = "x", displayLabel = "Option X"),
        SelectionOption(id = "y", displayLabel = "Option Y"),
    )

    @Test
    fun continueButton_isDisabled_whenNoOptionSelected() {
        composeTestRule.setContent {
            ScanAppTheme {
                SelectionScreenContent(
                    uiState = SelectionUiState(options = testOptions, selectedOption = null),
                    onOptionSelected = {},
                    onContinue = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun continueButton_isEnabled_afterOptionSelected() {
        var selected: SelectionOption? = null
        composeTestRule.setContent {
            ScanAppTheme {
                SelectionScreenContent(
                    uiState = SelectionUiState(
                        options = testOptions,
                        selectedOption = selected,
                    ),
                    onOptionSelected = { selected = it },
                    onContinue = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Option X").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.setContent {
            ScanAppTheme {
                SelectionScreenContent(
                    uiState = SelectionUiState(
                        options = testOptions,
                        selectedOption = testOptions.first(),
                    ),
                    onOptionSelected = {},
                    onContinue = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Continue").assertIsEnabled()
    }
}
