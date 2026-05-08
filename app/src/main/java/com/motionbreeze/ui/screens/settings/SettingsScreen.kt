package com.motionbreeze.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motionbreeze.data.AppSettings
import com.motionbreeze.data.AutoActivateSettings
import com.motionbreeze.data.DotSettings
import com.motionbreeze.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
) {
    var settings by remember { mutableStateOf(settingsRepository.readSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            DotConfigurationSection(
                dots = settings.dots,
                onDotsChange = { newDots ->
                    settingsRepository.updateDotSettings(newDots)
                    settings = settingsRepository.readSettings()
                },
            )

            AutoActivateSection(
                autoActivate = settings.autoActivate,
                onAutoActivateChange = { newAutoActivate ->
                    settingsRepository.updateAutoActivate(newAutoActivate)
                    settings = settingsRepository.readSettings()
                },
            )

            OpacitySection(
                dots = settings.dots,
                onDotsChange = { newDots ->
                    settingsRepository.updateDotSettings(newDots)
                    settings = settingsRepository.readSettings()
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DotConfigurationSection(
    dots: DotSettings,
    onDotsChange: (DotSettings) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dot Configuration",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SliderSetting(
                label = "Dots per side",
                value = dots.dotsPerSide,
                valueRange = 1..10,
                onValueChange = { onDotsChange(dots.copy(dotsPerSide = it)) },
            )

            SliderSetting(
                label = "Dot size (dp)",
                value = dots.dotSizeDp,
                valueRange = 4..20,
                onValueChange = { onDotsChange(dots.copy(dotSizeDp = it)) },
            )

            SliderSetting(
                label = "Motion sensitivity",
                value = (dots.sensitivity * 10).toInt(),
                valueRange = 1..20,
                onValueChange = { onDotsChange(dots.copy(sensitivity = it / 10f)) },
                valueLabel = "${(dots.sensitivity * 10).toInt() / 10f}x",
            )
        }
    }
}

@Composable
private fun AutoActivateSection(
    autoActivate: AutoActivateSettings,
    onAutoActivateChange: (AutoActivateSettings) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Auto-Activation",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-activate in vehicle")
                    Text(
                        "Starts motion cues automatically when driving is detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoActivate.autoActivate,
                    onCheckedChange = {
                        onAutoActivateChange(autoActivate.copy(autoActivate = it))
                    },
                )
            }

            if (autoActivate.autoActivate) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confirm before starting")
                        Text(
                            "Show notification to confirm before auto-starting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoActivate.confirmBeforeStart,
                        onCheckedChange = {
                            onAutoActivateChange(autoActivate.copy(confirmBeforeStart = it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OpacitySection(
    dots: DotSettings,
    onDotsChange: (DotSettings) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dot Appearance",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SliderSetting(
                label = "Minimum opacity",
                value = (dots.minOpacity * 100).toInt(),
                valueRange = 0..100,
                onValueChange = { onDotsChange(dots.copy(minOpacity = it / 100f)) },
                valueLabel = "${(dots.minOpacity * 100).toInt()}%",
            )

            SliderSetting(
                label = "Maximum opacity",
                value = (dots.maxOpacity * 100).toInt(),
                valueRange = 0..100,
                onValueChange = { onDotsChange(dots.copy(maxOpacity = it / 100f)) },
                valueLabel = "${(dots.maxOpacity * 100).toInt()}%",
            )
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    valueLabel: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel ?: value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = valueRange.last - valueRange.first - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}