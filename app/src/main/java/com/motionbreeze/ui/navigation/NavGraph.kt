package com.motionbreeze.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.motionbreeze.data.SettingsRepository
import com.motionbreeze.ui.screens.home.HomeScreen
import com.motionbreeze.ui.screens.onboarding.OnboardingScreen
import com.motionbreeze.ui.screens.settings.SettingsScreen

@Composable
fun MotionBreezeNavHost(
    activity: android.app.Activity,
) {
    val navController = rememberNavController()
    val settingsRepository = remember { SettingsRepository(activity) }
    val settings = settingsRepository.readSettings()

    val startDestination = if (settings.hasCompletedOnboarding) "home" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable("onboarding") {
            OnboardingScreen(
                settingsRepository = settingsRepository,
                onComplete = {
                    settingsRepository.setOnboardingComplete()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable("home") {
            HomeScreen(
                settingsRepository = settingsRepository,
                onNavigateToSettings = { navController.navigate("settings") },
                activity = activity,
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun remember(calculation: () -> SettingsRepository): SettingsRepository {
    return androidx.compose.runtime.remember { calculation() }
}