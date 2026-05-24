package com.sfm.scanner.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabeledTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun labeledTextField_showsLabel() {
        composeTestRule.setContent {
            ScanAppTheme {
                LabeledTextField(label = "Detail", value = "", onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText("Detail").assertExists()
    }

    @Test
    fun labeledTextField_showsCurrentValue() {
        composeTestRule.setContent {
            ScanAppTheme {
                LabeledTextField(label = "Size", value = "42", onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText("42").assertExists()
    }

    @Test
    fun labeledTextField_noErrorMessageWhenErrorIsNull() {
        composeTestRule.setContent {
            ScanAppTheme {
                LabeledTextField(label = "Field", value = "", onValueChange = {}, error = null)
            }
        }

        // Error node should not appear
        composeTestRule.onNodeWithText("Only alphanumeric").assertDoesNotExist()
    }

    @Test
    fun labeledTextField_showsErrorMessage() {
        val errorMsg = "Only alphanumeric characters allowed"

        composeTestRule.setContent {
            ScanAppTheme {
                LabeledTextField(
                    label = "Detail",
                    value = "bad!",
                    onValueChange = {},
                    error = errorMsg,
                )
            }
        }

        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }

    @Test
    fun labeledTextField_invokesOnValueChange() {
        var captured = ""
        var value by mutableStateOf("")

        composeTestRule.setContent {
            ScanAppTheme {
                LabeledTextField(
                    label = "Input",
                    value = value,
                    onValueChange = {
                        value = it
                        captured = it
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Input").performTextInput("Hello")
        assertEquals("Hello", captured)
    }
}
