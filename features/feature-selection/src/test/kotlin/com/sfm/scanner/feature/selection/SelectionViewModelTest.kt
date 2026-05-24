package com.sfm.scanner.feature.selection

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SelectionViewModelTest {

    private lateinit var viewModel: SelectionViewModel

    private val optionA = SelectionOption(id = "a", displayLabel = "A")
    private val optionB = SelectionOption(id = "b", displayLabel = "B")

    @Before
    fun setUp() {
        viewModel = SelectionViewModel()
    }

    @Test
    fun `initial state has no selection`() {
        assertNull(viewModel.uiState.value.selectedOption)
    }

    @Test
    fun `onOptionSelected updates selectedOption and replaces previous selection`() {
        viewModel.onOptionSelected(optionA)
        assertEquals(optionA, viewModel.uiState.value.selectedOption)

        viewModel.onOptionSelected(optionB)
        assertEquals(optionB, viewModel.uiState.value.selectedOption)
    }

    @Test
    fun `onContinue does not emit effect when no option is selected`() = runTest {
        viewModel.effects.test {
            viewModel.onContinue()
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `onContinue emits NavigateToForm with selected option id`() = runTest {
        viewModel.onOptionSelected(optionA)

        viewModel.effects.test {
            viewModel.onContinue()
            assertEquals(SelectionUiEffect.NavigateToForm("a"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
