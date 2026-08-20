package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable

interface CameraPermissionRequester {
    fun requestPermission()
}

@Composable
expect fun rememberCameraPermissionRequester(
    onResult: (Boolean) -> Unit
): CameraPermissionRequester
