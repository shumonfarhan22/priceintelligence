package com.supreme.priceintelligence.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun ProductBarcodeScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit,
    onCanceled: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerView(
        modifier = modifier,
        codeTypes = listOf(
            BarcodeFormat.FORMAT_CODE_128,
            BarcodeFormat.FORMAT_CODE_39,
            BarcodeFormat.FORMAT_CODE_93,
            BarcodeFormat.FORMAT_CODABAR,
            BarcodeFormat.FORMAT_EAN_13,
            BarcodeFormat.FORMAT_EAN_8,
            BarcodeFormat.FORMAT_ITF,
            BarcodeFormat.FORMAT_UPC_A,
            BarcodeFormat.FORMAT_UPC_E
        )
    ) { result ->
        when (result) {
            is BarcodeResult.OnSuccess -> onScanned(result.barcode.data)
            is BarcodeResult.OnFailed -> onError(
                result.exception.message ?: "The barcode could not be scanned"
            )
            BarcodeResult.OnCanceled -> onCanceled()
        }
    }
}
