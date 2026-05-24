package com.sfm.scanner.feature.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectionId: String = checkNotNull(
        savedStateHandle[FormDestination.ARG_SELECTION_ID]
    )

    private val _uiState = MutableStateFlow(FormUiState())
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FormUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<FormUiEffect> = _effects.asSharedFlow()

    // Tracks whether each field has been focused-and-left at least once.
    // Error visibility for size and gt is deferred until first focus-loss.
    private var sizeTouched = false
    private var gtTouched = false

    fun onDropdownSelected(key: String) {
        _uiState.update { state ->
            state.copy(
                dropdownSelection = key,
                proceedEnabled = computeProceedEnabled(
                    dropdownKey = key,
                    sizeInput = state.sizeInput,
                    detailInput = state.detailInput,
                    gtInput = state.gtInput,
                ),
            )
        }
    }

    fun onSizeChanged(input: String) {
        val error = if (sizeTouched) FormValidator.validateSize(input) else null
        _uiState.update { state ->
            state.copy(
                sizeInput = input,
                sizeError = error,
                proceedEnabled = computeProceedEnabled(
                    dropdownKey = state.dropdownSelection,
                    sizeInput = input,
                    detailInput = state.detailInput,
                    gtInput = state.gtInput,
                ),
            )
        }
    }

    fun onSizeFocusLost() {
        sizeTouched = true
        _uiState.update { state ->
            state.copy(sizeError = FormValidator.validateSize(state.sizeInput))
        }
    }

    fun onDetailChanged(input: String) {
        val error = computeDetailDisplayError(input)
        _uiState.update { state ->
            state.copy(
                detailInput = input,
                detailError = error,
                proceedEnabled = computeProceedEnabled(
                    dropdownKey = state.dropdownSelection,
                    sizeInput = state.sizeInput,
                    detailInput = input,
                    gtInput = state.gtInput,
                ),
            )
        }
    }

    fun onGtChanged(input: String) {
        val error = if (gtTouched) FormValidator.validateGt(input) else null
        _uiState.update { state ->
            state.copy(
                gtInput = input,
                gtError = error,
                proceedEnabled = computeProceedEnabled(
                    dropdownKey = state.dropdownSelection,
                    sizeInput = state.sizeInput,
                    detailInput = state.detailInput,
                    gtInput = input,
                ),
            )
        }
    }

    fun onGtFocusLost() {
        gtTouched = true
        _uiState.update { state ->
            state.copy(gtError = FormValidator.validateGt(state.gtInput))
        }
    }

    fun onProceed() {
        val state = _uiState.value
        if (!state.proceedEnabled) return
        val formData = FormData(
            initialSelection = selectionId,
            dropdownSelection = checkNotNull(state.dropdownSelection),
            size = checkNotNull(state.sizeInput.toIntOrNull()),
            detail = state.detailInput,
            gt = FormValidator.parseGt(state.gtInput),
        )
        _effects.tryEmit(FormUiEffect.NavigateToScan(formData))
    }

    // Suppress error for empty detail to avoid premature red state on untouched field;
    // proceed button is still disabled because isDetailValid("") == false.
    private fun computeDetailDisplayError(input: String): String? {
        if (input.isEmpty()) return null
        return FormValidator.validateDetail(input)
    }

    private fun computeProceedEnabled(
        dropdownKey: String?,
        sizeInput: String,
        detailInput: String,
        gtInput: String,
    ): Boolean =
        dropdownKey != null &&
            FormValidator.isSizeValid(sizeInput) &&
            FormValidator.isDetailValid(detailInput) &&
            FormValidator.isGtValid(gtInput)
}
