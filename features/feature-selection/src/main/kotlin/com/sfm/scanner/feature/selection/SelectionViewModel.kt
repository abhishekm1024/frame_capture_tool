package com.sfm.scanner.feature.selection

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
class SelectionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SelectionUiState())
    val uiState: StateFlow<SelectionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SelectionUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<SelectionUiEffect> = _effects.asSharedFlow()

    fun onOptionSelected(option: SelectionOption) {
        _uiState.update { it.copy(selectedOption = option) }
    }

    fun onContinue() {
        val selected = _uiState.value.selectedOption ?: return
        _effects.tryEmit(SelectionUiEffect.NavigateToForm(selected.id))
    }
}
