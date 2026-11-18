package com.nave.moto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nave.moto.presentation.NaveApp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nave.moto.presentation.theme.LocalEs
import com.nave.moto.presentation.theme.LocalHaptics
import com.nave.moto.presentation.theme.NaveMotoTheme
import com.nave.moto.presentation.viewmodel.NaveMotoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: NaveMotoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by viewModel.uiState.collectAsStateWithLifecycle()
            // Tamaño de texto propio multiplicado por el del sistema (tope 2x para no romper el diseño)
            val d = LocalDensity.current
            val density = Density(d.density, (d.fontScale * ui.settings.fontScale).coerceAtMost(2f))
            CompositionLocalProvider(
                LocalEs provides (ui.settings.language == "es"),
                LocalHaptics provides ui.settings.haptics,
                LocalDensity provides density
            ) {
                NaveMotoTheme(darkTheme = ui.settings.darkTheme, highContrast = ui.settings.highContrast) { NaveApp(viewModel, ui) }
            }
        }
    }
}
