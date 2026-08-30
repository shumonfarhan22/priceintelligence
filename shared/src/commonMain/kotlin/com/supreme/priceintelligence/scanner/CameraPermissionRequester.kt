package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable

interface CameraPermissionRequester {
    fun requestPermission()

    /** Opens this app's system settings after permission was denied. */
    fun openAppSettings()
}

@Composable
expect fun rememberCameraPermissionRequester(
    onResult: (Boolean) -> Unit
): CameraPermissionRequester
