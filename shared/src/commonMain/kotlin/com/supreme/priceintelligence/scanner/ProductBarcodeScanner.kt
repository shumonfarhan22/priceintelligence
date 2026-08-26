package com.supreme.priceintelligence.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.ScannerUiOptions
import org.ncgroup.kscan.ScannerView
import org.ncgroup.kscan.scannerColors

@Composable
fun ProductBarcodeScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit,
    onCanceled: () -> Unit,
    hapticFeedbackEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scannerController = remember {
        ScannerController()
    }
    var flashlightEnabled by remember {
        mutableStateOf(false)
    }
    val scanHapticFeedback = rememberScanHapticFeedback()

    Dialog(
        onDismissRequest = onCanceled,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ScannerView(
                modifier = Modifier.fillMaxSize(),
                codeTypes = listOf(
                    BarcodeFormat.FORMAT_EAN_13,
                    BarcodeFormat.FORMAT_EAN_8,
                    BarcodeFormat.FORMAT_UPC_A,
                    BarcodeFormat.FORMAT_UPC_E
                ),
                colors = scannerColors(
                    headerContainerColor = Color.Transparent,
                    headerNavigationIconColor = Color.Transparent,
                    headerTitleColor = Color.Transparent,
                    headerActionIconColor = Color.Transparent,
                    zoomControllerContainerColor = Color.Transparent,
                    zoomControllerContentColor = Color.Transparent,
                    barcodeFrameColor = Color.Transparent
                ),
                scannerUiOptions = ScannerUiOptions(
                    headerTitle = "",
                    showZoom = false,
                    showTorch = false
                ),
                scannerController = scannerController
            ) { result ->
                when (result) {
                    is BarcodeResult.OnSuccess -> {
                        if (hapticFeedbackEnabled) {
                            scanHapticFeedback.scanSucceeded()
                        }
                        onScanned(result.barcode.data)
                    }

                    is BarcodeResult.OnFailed ->
                        onError(
                            result.exception.message
                                ?: "The barcode could not be scanned"
                        )

                    BarcodeResult.OnCanceled ->
                        onCanceled()
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.99f
                    }
            ) {
                val cutoutWidth = size.width * 0.70f
                val cutoutHeight = cutoutWidth
                val left = (size.width - cutoutWidth) / 2f
                val top = (size.height - cutoutHeight) / 2f
                val right = left + cutoutWidth
                val bottom = top + cutoutHeight

                drawRect(
                    color = Color.Black.copy(alpha = 0.65f)
                )

                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(left, top),
                    size = Size(
                        width = cutoutWidth,
                        height = cutoutHeight
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )

                val bracketColor = Color(0xFF10B981)
                val bracketStroke = 4.dp.toPx()
                val bracketLength = 40.dp.toPx()

                drawLine(
                    color = bracketColor,
                    start = Offset(left, top),
                    end = Offset(left + bracketLength, top),
                    strokeWidth = bracketStroke
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(left, top),
                    end = Offset(left, top + bracketLength),
                    strokeWidth = bracketStroke
                )

                drawLine(
                    color = bracketColor,
                    start = Offset(right, top),
                    end = Offset(right - bracketLength, top),
                    strokeWidth = bracketStroke
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(right, top),
                    end = Offset(right, top + bracketLength),
                    strokeWidth = bracketStroke
                )

                drawLine(
                    color = bracketColor,
                    start = Offset(left, bottom),
                    end = Offset(left + bracketLength, bottom),
                    strokeWidth = bracketStroke
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(left, bottom),
                    end = Offset(left, bottom - bracketLength),
                    strokeWidth = bracketStroke
                )

                drawLine(
                    color = bracketColor,
                    start = Offset(right, bottom),
                    end = Offset(right - bracketLength, bottom),
                    strokeWidth = bracketStroke
                )
                drawLine(
                    color = bracketColor,
                    start = Offset(right, bottom),
                    end = Offset(right, bottom - bracketLength),
                    strokeWidth = bracketStroke
                )
            }

            IconButton(
                onClick = onCanceled,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 14.dp, top = 10.dp)
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close scanner",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 38.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (flashlightEnabled) {
                                Color(0xFFFFD700).copy(alpha = 0.20f)
                            } else {
                                Color.Black.copy(alpha = 0.42f)
                            }
                        )
                        .border(
                            width = if (flashlightEnabled) {
                                2.dp
                            } else {
                                1.dp
                            },
                            color = if (flashlightEnabled) {
                                Color(0xFFFFD700)
                            } else {
                                Color.White.copy(alpha = 0.24f)
                            },
                            shape = CircleShape
                        )
                        .clickable {
                            flashlightEnabled = !flashlightEnabled
                            scannerController.setTorch(flashlightEnabled)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashlightOn,
                        contentDescription = if (flashlightEnabled) {
                            "Turn flashlight off"
                        } else {
                            "Turn flashlight on"
                        },
                        tint = if (flashlightEnabled) {
                            Color(0xFFFFD700)
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Point the camera at a barcode",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
