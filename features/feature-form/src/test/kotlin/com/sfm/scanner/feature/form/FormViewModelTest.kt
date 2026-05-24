package com.sfm.scanner.feature.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormViewModelTest {

    private lateinit var viewModel: FormViewModel

    private val testSelectionId = "option_a"

    @Before
    fun setUp() {
        viewModel = FormViewModel(
            SavedStateHandle(mapOf(FormDestination.ARG_SELECTION_ID to testSelectionId)),
        )
    }

    @Test
    fun `initial state has proceedEnabled false`() {
        assertFalse(viewModel.uiState.value.proceedEnabled)
    }

    @Test
    fun `proceedEnabled remains false when only dropdown is selected`() {
        viewModel.onDropdownSelected("type_a")
        assertFalse(viewModel.uiState.value.proceedEnabled)
    }

    @Test
    fun `proceedEnabled remains false when dropdown and size valid but detail empty`() {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("5")
        assertFalse(viewModel.uiState.value.proceedEnabled)
    }

    @Test
    fun `proceedEnabled becomes true when all required fields are valid`() {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("5")
        viewModel.onDetailChanged("ABC")
        assertTrue(viewModel.uiState.value.proceedEnabled)
    }

    @Test
    fun `proceedEnabled becomes false when detail changed to invalid after being valid`() {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("5")
        viewModel.onDetailChanged("ABC")
        assertTrue(viewModel.uiState.value.proceedEnabled)

        viewModel.onDetailChanged("ABC!")
        assertFalse(viewModel.uiState.value.proceedEnabled)
    }

    @Test
    fun `sizeError is null before focus-lost even when input is invalid`() {
        viewModel.onSizeChanged("abc")
        assertNull(viewModel.uiState.value.sizeError)
    }

    @Test
    fun `sizeError is shown after focus-lost with invalid input`() {
        viewModel.onSizeChanged("abc")
        viewModel.onSizeFocusLost()
        assertFalse(viewModel.uiState.value.sizeError == null)
    }

    @Test
    fun `sizeError updates live after field has been touched`() {
        viewModel.onSizeChanged("abc")
        viewModel.onSizeFocusLost()
        viewModel.onSizeChanged("5")
        assertNull(viewModel.uiState.value.sizeError)
    }

    @Test
    fun `gtError is null before focus-lost`() {
        viewModel.onGtChanged("not-a-number")
        assertNull(viewModel.uiState.value.gtError)
    }

    @Test
    fun `gtError is shown after focus-lost with invalid input`() {
        viewModel.onGtChanged("not-a-number")
        viewModel.onGtFocusLost()
        assertFalse(viewModel.uiState.value.gtError == null)
    }

    @Test
    fun `onProceed does not emit effect when proceedEnabled is false`() = runTest {
        viewModel.effects.test {
            viewModel.onProceed()
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `onProceed emits NavigateToScan with correct FormData when all fields valid`() = runTest {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("5")
        viewModel.onDetailChanged("ABC")

        viewModel.effects.test {
            viewModel.onProceed()
            val effect = awaitItem() as FormUiEffect.NavigateToScan
            assertEquals(testSelectionId, effect.formData.initialSelection)
            assertEquals("type_a", effect.formData.dropdownSelection)
            assertEquals(5, effect.formData.size)
            assertEquals("ABC", effect.formData.detail)
            assertNull(effect.formData.gt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onProceed includes parsed gt list when gt field has valid input`() = runTest {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("5")
        viewModel.onDetailChanged("ABC")
        viewModel.onGtChanged("1.0, 2.5")

        viewModel.effects.test {
            viewModel.onProceed()
            val effect = awaitItem() as FormUiEffect.NavigateToScan
            assertEquals(listOf(1.0, 2.5), effect.formData.gt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onProceed stores null for gt when field is empty`() = runTest {
        viewModel.onDropdownSelected("type_a")
        viewModel.onSizeChanged("10")
        viewModel.onDetailChanged("XYZ")
        viewModel.onGtChanged("")

        viewModel.effects.test {
            viewModel.onProceed()
            val effect = awaitItem() as FormUiEffect.NavigateToScan
            assertNull(effect.formData.gt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
