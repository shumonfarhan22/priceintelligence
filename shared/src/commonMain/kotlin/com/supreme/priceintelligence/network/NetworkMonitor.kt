package com.supreme.priceintelligence.network

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>

    /**
     * Stops platform network callbacks when the app screen is closed.
     */
    fun stop()
}