package com.supreme.priceintelligence

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.AndroidNetworkMonitor
import com.supreme.priceintelligence.settings.AndroidAppPreferences

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val databaseBuilder = remember {
                getDatabaseBuilder(applicationContext)
            }

            val networkMonitor = remember {
                AndroidNetworkMonitor(applicationContext)
            }

            val appPreferences = remember {
                AndroidAppPreferences(applicationContext)
            }

            DisposableEffect(networkMonitor) {
                onDispose {
                    networkMonitor.stop()
                }
            }

            App(
                databaseBuilder = databaseBuilder,
                networkMonitor = networkMonitor,
                appPreferences = appPreferences
            )
        }
    }
}
