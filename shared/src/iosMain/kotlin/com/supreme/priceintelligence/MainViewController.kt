package com.supreme.priceintelligence

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.IosNetworkMonitor

fun MainViewController() = ComposeUIViewController {
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
        networkMonitor = networkMonitor
    )
}