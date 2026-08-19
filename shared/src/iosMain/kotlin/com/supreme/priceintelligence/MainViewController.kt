package com.supreme.priceintelligence

import androidx.compose.ui.window.ComposeUIViewController
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.IosNetworkMonitor

fun MainViewController() = ComposeUIViewController {
    App(
        databaseBuilder = getDatabaseBuilder(),
        networkMonitor = IosNetworkMonitor()
    )
}