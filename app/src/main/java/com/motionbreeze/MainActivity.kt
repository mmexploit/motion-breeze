package com.motionbreeze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.motionbreeze.ui.theme.MotionBreezeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = (application as MotionBreezeApp).settingsRepository
        setContent {
            MotionBreezeTheme {
                MotionBreezeNavHost(
                    activity = this,
                    settingsRepository = settingsRepository,
                )
            }
        }
    }
}