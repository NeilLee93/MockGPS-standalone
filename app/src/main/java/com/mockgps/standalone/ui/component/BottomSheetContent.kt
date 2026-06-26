// app/src/main/java/com/mockgps/standalone/ui/component/BottomSheetContent.kt
package com.mockgps.standalone.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mockgps.standalone.R
import com.mockgps.standalone.ui.MockMode
import com.mockgps.standalone.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    uiState: UiState,
    onLatChange: (Double) -> Unit,
    onLonChange: (Double) -> Unit,
    onModeChange: (MockMode) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Coordinate row with copy
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoordField(
                label = stringResource(R.string.label_latitude),
                value = uiState.lat,
                onValueChange = onLatChange,
                modifier = Modifier.weight(1f)
            )
            CoordField(
                label = stringResource(R.string.label_longitude),
                value = uiState.lon,
                onValueChange = onLonChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                clipboard.setText(AnnotatedString("${uiState.lat}, ${uiState.lon}"))
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copied))
            }
        }

        // Mode toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MockMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = uiState.mode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(idx, MockMode.entries.size),
                    label = {
                        Text(
                            if (mode == MockMode.STATIC) stringResource(R.string.mode_static)
                            else stringResource(R.string.mode_walker)
                        )
                    }
                )
            }
        }

        // Walker config (only when WALKER mode)
        if (uiState.mode == MockMode.WALKER) {
            SliderRow(
                label = stringResource(R.string.label_radius),
                value = uiState.walkerRadius,
                valueRange = 50f..2000f,
                displayText = "${uiState.walkerRadius.toInt()} m",
                onValueChange = onRadiusChange
            )
            SliderRow(
                label = stringResource(R.string.label_speed),
                value = uiState.walkerSpeed,
                valueRange = 1f..10f,
                displayText = "${"%.1f".format(uiState.walkerSpeed)} m/s",
                onValueChange = onSpeedChange
            )
        }

        // Provider warning
        if (!uiState.providerReady) {
            val context = LocalContext.current
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.status_no_provider),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        )
                    }) {
                        Text(stringResource(R.string.goto_dev_settings))
                    }
                }
            }
        }

        // Start / Stop
        Button(
            onClick = { if (uiState.isRunning) onStop() else onStart() },
            modifier = Modifier.fillMaxWidth(),
            colors = if (uiState.isRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else
                ButtonDefaults.buttonColors()
        ) {
            Text(if (uiState.isRunning) stringResource(R.string.btn_stop) else stringResource(R.string.btn_start))
        }

        extraContent()
    }
}

@Composable
private fun CoordField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier
) {
    var text by remember(value) { mutableStateOf("%.6f".format(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toDoubleOrNull()?.let(onValueChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(displayText, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
