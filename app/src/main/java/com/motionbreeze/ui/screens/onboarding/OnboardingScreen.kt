package com.motionbreeze.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motionbreeze.data.SettingsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

enum class OnboardingStep {
    OVERLAY_PERMISSION,
    ACTIVITY_RECOGNITION,
    COMPLETE,
}

@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.OVERLAY_PERMISSION) }
    val settings = settingsRepository.readSettings()

    val activityRecognitionPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        rememberPermissionState(
            android.Manifest.permission.ACTIVITY_RECOGNITION
        )
    } else null

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val canDrawOverlays = Settings.canDrawOverlays(context)
        settingsRepository.setHasOverlayPermission(canDrawOverlays)
        if (canDrawOverlays) {
            currentStep = OnboardingStep.ACTIVITY_RECOGNITION
        }
    }

    when (currentStep) {
        OnboardingStep.OVERLAY_PERMISSION -> {
            OverlayPermissionStep(
                hasPermission = Settings.canDrawOverlays(context),
                onRequestPermission = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                },
                onSkip = {
                    settingsRepository.setHasOverlayPermission(false)
                    currentStep = OnboardingStep.ACTIVITY_RECOGNITION
                },
            )
        }
        OnboardingStep.ACTIVITY_RECOGNITION -> {
            ActivityRecognitionStep(
                hasPermission = if (activityRecognitionPermission != null) {
                    activityRecognitionPermission.hasPermission
                } else true,
                onRequestPermission = {
                    activityRecognitionPermission?.launchPermissionRequest()
                },
                onSkip = {
                    settingsRepository.setHasActivityRecognitionPermission(false)
                    currentStep = OnboardingStep.COMPLETE
                    onComplete()
                },
                onContinue = {
                    settingsRepository.setHasActivityRecognitionPermission(
                        activityRecognitionPermission?.hasPermission ?: true
                    )
                    currentStep = OnboardingStep.COMPLETE
                    onComplete()
                },
            )
        }
        OnboardingStep.COMPLETE -> onComplete()
    }
}

@Composable
private fun OverlayPermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit,
) {
    OnboardingStepLayout(
        icon = {
            Icon(
                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (hasPermission) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = if (hasPermission) "Overlay Permission Granted" else "Display Over Other Apps",
        description = if (hasPermission) {
            "Great! Motion Breeze can now show motion cues over other apps."
        } else {
            "Motion Breeze needs permission to display motion cue dots over other apps. " +
            "This is essential for reducing motion sickness while using your phone in a moving vehicle.\n\n" +
            "You'll be taken to Settings → Apps → Special permissions → Display over other apps."
        },
        primaryAction = if (hasPermission) "Continue" else "Grant Permission",
        onPrimaryAction = if (hasPermission) onSkip else onRequestPermission,
        secondaryAction = if (hasPermission) null else "Skip for now",
        onSecondaryAction = onSkip,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ActivityRecognitionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingStepLayout(
        icon = {
            Icon(
                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (hasPermission) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = if (hasPermission) "Activity Recognition Granted" else "Vehicle Detection",
        description = if (hasPermission) {
            "Motion Breeze can detect when you're in a vehicle and auto-activate."
        } else {
            "Motion Breeze can automatically start motion cues when it detects you're in a vehicle. " +
            "This requires activity recognition permission.\n\n" +
            "You can always start it manually if you skip this."
        },
        primaryAction = if (hasPermission) "Continue" else "Grant Permission",
        onPrimaryAction = if (hasPermission) onContinue else onRequestPermission,
        secondaryAction = if (hasPermission) null else "Skip for now",
        onSecondaryAction = onSkip,
    )
}

@Composable
private fun OnboardingStepLayout(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    secondaryAction: String?,
    onSecondaryAction: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(primaryAction)
            }
            if (secondaryAction != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(secondaryAction)
                }
            }
        }
    }
}