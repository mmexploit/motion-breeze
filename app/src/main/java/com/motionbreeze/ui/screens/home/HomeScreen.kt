package com.motionbreeze.ui.screens.home

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motionbreeze.data.SettingsRepository
import com.motionbreeze.service.OverlayService

@Composable
fun HomeScreen(
    settingsRepository: SettingsRepository,
    onNavigateToSettings: () -> Unit,
    activity: android.app.Activity,
) {
    val context = LocalContext.current
    val settings = settingsRepository.readSettings()
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val isRunning by OverlayService.runningState.collectAsState()

    var showBatteryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning && !settingsRepository.hasShownBatteryPrompt()) {
            val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                showBatteryDialog = true
            }
        }
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = {
                showBatteryDialog = false
                settingsRepository.setBatteryPromptShown()
            },
            title = { Text("Keep Motion Breeze Running") },
            text = {
                Text(
                    "Some phones may stop the overlay service to save battery. " +
                    "Allow Motion Breeze to run without restrictions for reliable motion cues."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    settingsRepository.setBatteryPromptShown()
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    settingsRepository.setBatteryPromptShown()
                }) {
                    Text("Not now")
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!hasOverlayPermission) {
                MissingPermissionCard(
                    onGrantPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onUseInAppOnly = {
                        context.startActivity(Intent(context, OverlayService::class.java).apply {
                            action = OverlayService.ACTION_START_IN_APP
                        })
                    },
                )
            } else {
                ActiveStateCard(
                    isRunning = isRunning,
                    onStart = {
                        context.startForegroundService(
                            Intent(context, OverlayService::class.java).apply {
                                action = OverlayService.ACTION_START_OVERLAY
                            }
                        )
                    },
                    onStop = {
                        context.startService(
                            Intent(context, OverlayService::class.java).apply {
                                action = OverlayService.ACTION_STOP
                            }
                        )
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                InAppOnlyButton(
                    onUseInApp = {
                        context.startActivity(Intent(context, OverlayService::class.java).apply {
                            action = OverlayService.ACTION_START_IN_APP
                        })
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (settings.autoActivate.autoActivate) "Auto-activate: On" else "Auto-activate: Off",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                )
            }
        }
    }
}

@Composable
private fun ActiveStateCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isRunning) "Motion cues are active" else "Motion cues are inactive",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = if (isRunning) onStop else onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }
        }
    }
}

@Composable
private fun InAppOnlyButton(
    onUseInApp: () -> Unit,
) {
    OutlinedButton(
        onClick = onUseInApp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Use in-app only")
    }
}

@Composable
private fun MissingPermissionCard(
    onGrantPermission: () -> Unit,
    onUseInAppOnly: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Overlay permission required",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Motion Breeze needs permission to display over other apps for system-wide motion cues.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGrantPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant Permission")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onUseInAppOnly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use in-app only")
            }
        }
    }
}