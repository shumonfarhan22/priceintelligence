package com.supreme.priceintelligence

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.IosNetworkMonitor
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.IosAppPreferences
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

private fun AppThemeMode.toIosInterfaceStyle(): UIUserInterfaceStyle =
    when (this) {
        AppThemeMode.SYSTEM ->
            UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified

        AppThemeMode.LIGHT ->
            UIUserInterfaceStyle.UIUserInterfaceStyleLight

        AppThemeMode.DARK ->
            UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }

fun MainViewController(): UIViewController {
    val appPreferences = IosAppPreferences()
    var controllerReference: UIViewController? = null

    val controller = ComposeUIViewController {
        val databaseBuilder = remember {
            getDatabaseBuilder()
        }

        val networkMonitor = remember {
            IosNetworkMonitor()
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
            onThemeApplied = { themeMode, _ ->
                controllerReference?.let { activeController ->
                    activeController.overrideUserInterfaceStyle =
                        themeMode.toIosInterfaceStyle()

                    activeController
                        .setNeedsStatusBarAppearanceUpdate()
                }
            }
        )
    }

    controllerReference = controller
    controller.overrideUserInterfaceStyle =
        appPreferences.themeMode.toIosInterfaceStyle()

    return controller
}