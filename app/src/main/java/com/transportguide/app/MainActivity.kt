package com.transportguide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.transportguide.app.ui.TripScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme())
                darkColorScheme(primary = Color(0xFF8AB4F8), secondary = Color(0xFFF6AE2D), tertiary = Color(0xFF81C995))
            else
                lightColorScheme(primary = Color(0xFF1A5FB4), secondary = Color(0xFFC77700), tertiary = Color(0xFF1E8E3E))
            MaterialTheme(colorScheme = colors) { TripScreen() }
        }
    }
}
