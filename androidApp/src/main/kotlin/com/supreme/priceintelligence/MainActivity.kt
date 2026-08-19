package com.supreme.priceintelligence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.network.AndroidNetworkMonitor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val databaseBuilder = remember {
                getDatabaseBuilder(applicationContext)
            }

            val networkMonitor = remember {
                AndroidNetworkMonitor(applicationContext)
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
    }
}