package com.supreme.priceintelligence

import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.supreme.priceintelligence.dashboard.AndroidPriceChangeNotifier
import com.supreme.priceintelligence.dashboard.handlePriceChangeNotificationIntent
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.AndroidNetworkMonitor
import com.supreme.priceintelligence.settings.AndroidAppPreferences

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handlePriceChangeNotificationIntent(intent)

        DailyPriceRefreshScheduler.ensureScheduled(
            applicationContext
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Activity-result launchers must be registered before this Activity
        // reaches STARTED. Creating the notifier inside Compose can be too
        // late on Android and crashes the app during its first composition.
        val priceChangeNotifier =
            AndroidPriceChangeNotifier(this)

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
                appPreferences = appPreferences,
                priceChangeNotifier = priceChangeNotifier,
                onThemeApplied = { _, isDarkTheme ->
                    WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    ).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePriceChangeNotificationIntent(intent)
    }
}
