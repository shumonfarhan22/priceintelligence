package com.supreme.priceintelligence.inventory

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.supreme.priceintelligence.data.InventoryItem

internal class InventoryEditorTextState(
    val productName: TextFieldState,
    val purchaseCost: TextFieldState,
    val shopPrice: TextFieldState,
    val barcode: TextFieldState,
    val amazonUrl: TextFieldState,
    val flipkartUrl: TextFieldState
) {
    fun load(item: InventoryItem) {
        productName.setTextAndPlaceCursorAtEnd(item.productName)
        purchaseCost.setTextAndPlaceCursorAtEnd(
            item.purchaseCost?.toString().orEmpty()
        )
        shopPrice.setTextAndPlaceCursorAtEnd(item.shopPrice.toString())
        barcode.setTextAndPlaceCursorAtEnd(item.barcode.orEmpty())
        amazonUrl.setTextAndPlaceCursorAtEnd(item.amazonUrl.orEmpty())
        flipkartUrl.setTextAndPlaceCursorAtEnd(item.flipkartUrl.orEmpty())
    }

    fun clear() {
        productName.setTextAndPlaceCursorAtEnd("")
        purchaseCost.setTextAndPlaceCursorAtEnd("")
        shopPrice.setTextAndPlaceCursorAtEnd("")
        barcode.setTextAndPlaceCursorAtEnd("")
        amazonUrl.setTextAndPlaceCursorAtEnd("")
        flipkartUrl.setTextAndPlaceCursorAtEnd("")
    }

    fun formWithEditingItem(editingItem: InventoryItem?): InventoryFormState =
        InventoryFormState(
            editingItem = editingItem,
            productName = productName.text.toString(),
            purchaseCost = purchaseCost.text.toString(),
            shopPrice = shopPrice.text.toString(),
            barcode = barcode.text.toString(),
            amazonUrl = amazonUrl.text.toString(),
            flipkartUrl = flipkartUrl.text.toString()
        )
}

@Composable
internal fun rememberInventoryEditorTextState(): InventoryEditorTextState {
    val productName = rememberTextFieldState()
    val purchaseCost = rememberTextFieldState()
    val shopPrice = rememberTextFieldState()
    val barcode = rememberTextFieldState()
    val amazonUrl = rememberTextFieldState()
    val flipkartUrl = rememberTextFieldState()

    return remember(
        productName,
        purchaseCost,
        shopPrice,
        barcode,
        amazonUrl,
        flipkartUrl
    ) {
        InventoryEditorTextState(
            productName = productName,
            purchaseCost = purchaseCost,
            shopPrice = shopPrice,
            barcode = barcode,
            amazonUrl = amazonUrl,
            flipkartUrl = flipkartUrl
        )
    }
}
