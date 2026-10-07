package com.startuga.turnotrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startuga.turnotrack.ui.TurnoTrackRoot
import com.startuga.turnotrack.ui.theme.TurnoTrackTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TurnoTrackApplication).container
        // Lê o tema guardado antes de desenhar, para não haver um "piscar" de cores ao abrir.
        val initialTheme = runBlocking { container.settings.themeMode.first() }

        setContent {
            val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = initialTheme)
            TurnoTrackTheme(themeMode) {
                TurnoTrackRoot(container)
            }
        }
    }
}
