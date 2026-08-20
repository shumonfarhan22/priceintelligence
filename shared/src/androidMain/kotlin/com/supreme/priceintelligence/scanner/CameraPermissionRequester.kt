package com.supreme.priceintelligence.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberCameraPermissionRequester(
    onResult: (Boolean) -> Unit
): CameraPermissionRequester {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        currentOnResult.value(granted)
    }

    return remember(context, launcher) {
        object : CameraPermissionRequester {
            override fun requestPermission() {
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    currentOnResult.value(true)
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}
