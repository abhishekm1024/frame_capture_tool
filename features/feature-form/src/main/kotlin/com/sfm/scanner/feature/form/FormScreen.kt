@file:OptIn(ExperimentalMaterial3Api::class)

package com.sfm.scanner.feature.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sfm.scanner.core.ui.components.PrimaryButton

@Composable
fun FormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScan: (FormData) -> Unit,
    viewModel: FormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FormUiEffect.NavigateToScan -> onNavigateToScan(effect.formData)
            }
        }
    }

    FormScreenContent(
        uiState = uiState,
        onDropdownSelected = viewModel::onDropdownSelected,
        onSizeChanged = viewModel::onSizeChanged,
        onSizeFocusLost = viewModel::onSizeFocusLost,
        onDetailChanged = viewModel::onDetailChanged,
        onGtChanged = viewModel::onGtChanged,
        onGtFocusLost = viewModel::onGtFocusLost,
        onProceed = viewModel::onProceed,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
internal fun FormScreenContent(
    uiState: FormUiState,
    onDropdownSelected: (String) -> Unit,
    onSizeChanged: (String) -> Unit,
    onSizeFocusLost: () -> Unit,
    onDetailChanged: (String) -> Unit,
    onGtChanged: (String) -> Unit,
    onGtFocusLost: () -> Unit,
    onProceed: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val sizeFocusRequester = remember { FocusRequester() }
    val detailFocusRequester = remember { FocusRequester() }
    val gtFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Details",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DropdownField(
                selectedKey = uiState.dropdownSelection,
                onSelected = onDropdownSelected,
                onNextFocus = { sizeFocusRequester.requestFocus() },
            )

            SizeField(
                value = uiState.sizeInput,
                error = uiState.sizeError,
                focusRequester = sizeFocusRequester,
                onValueChange = onSizeChanged,
                onFocusLost = onSizeFocusLost,
                onNextFocus = { detailFocusRequester.requestFocus() },
            )

            DetailField(
                value = uiState.detailInput,
                error = uiState.detailError,
                focusRequester = detailFocusRequester,
                onValueChange = { onDetailChanged(it.take(DETAIL_MAX_LENGTH)) },
                onNextFocus = { gtFocusRequester.requestFocus() },
            )

            GtField(
                value = uiState.gtInput,
                error = uiState.gtError,
                focusRequester = gtFocusRequester,
                onValueChange = onGtChanged,
                onFocusLost = onGtFocusLost,
                onDone = { focusManager.clearFocus() },
            )

            PrimaryButton(
                label = "Proceed to Scan",
                onClick = onProceed,
                enabled = uiState.proceedEnabled,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .semantics { contentDescription = "Proceed to scan screen" },
            )
        }
    }
}

@Composable
private fun DropdownField(
    selectedKey: String?,
    onSelected: (String) -> Unit,
    onNextFocus: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = selectedKey?.let { key ->
        FormOptions.all.find { it.key == key }?.label
    } ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            placeholder = { Text("Select type...") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNextFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = MaterialTheme.shapes.small,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FormOptions.all.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.key)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SizeField(
    value: String,
    error: String?,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    onNextFocus: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Size") },
            placeholder = { Text("Enter size...") },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onNextFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (wasFocused && !focusState.isFocused) onFocusLost()
                }
                .then(
                    if (error != null) Modifier.semantics { error(error) } else Modifier,
                ),
            shape = MaterialTheme.shapes.small,
        )
        if (error != null) {
            Text(
                text = "⚠ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun DetailField(
    value: String,
    error: String?,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onNextFocus: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Detail") },
            placeholder = { Text("Enter detail...") },
            isError = error != null,
            singleLine = true,
            trailingIcon = if (value.length > 10) {
                { Text(text = "${value.length} / 16", style = MaterialTheme.typography.labelSmall) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onNextFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .then(
                    if (error != null) Modifier.semantics { error(error) } else Modifier,
                ),
            shape = MaterialTheme.shapes.small,
        )
        if (error != null) {
            Text(
                text = "⚠ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun GtField(
    value: String,
    error: String?,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    onDone: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Ground Truth (optional)") },
            placeholder = { Text("e.g. 12.3, 15.0, 0.42") },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (wasFocused && !focusState.isFocused) onFocusLost()
                }
                .then(
                    if (error != null) Modifier.semantics { error(error) } else Modifier,
                ),
            shape = MaterialTheme.shapes.small,
        )
        if (error != null) {
            Text(
                text = "⚠ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

private const val DETAIL_MAX_LENGTH = 16
