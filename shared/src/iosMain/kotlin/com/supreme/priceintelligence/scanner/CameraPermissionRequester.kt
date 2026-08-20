package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCameraPermissionRequester(
    onResult: (Boolean) -> Unit
): CameraPermissionRequester {
    val currentOnResult = rememberUpdatedState(onResult)

    return remember {
        object : CameraPermissionRequester {
            override fun requestPermission() {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> currentOnResult.value(true)
                    AVAuthorizationStatusDenied,
                    AVAuthorizationStatusRestricted -> currentOnResult.value(false)
                    AVAuthorizationStatusNotDetermined -> {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            dispatch_async(dispatch_get_main_queue()) {
                                currentOnResult.value(granted)
                            }
                        }
                    }
                    else -> currentOnResult.value(false)
                }
            }
        }
    }
}
