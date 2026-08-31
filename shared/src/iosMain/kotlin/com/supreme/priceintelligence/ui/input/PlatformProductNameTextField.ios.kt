@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class
)

package com.supreme.priceintelligence.ui.input

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.supreme.priceintelligence.ui.theme.supremeColors
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIKeyboardAppearanceDark
import platform.UIKit.UIKeyboardAppearanceLight
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldDelegateProtocol
import platform.UIKit.UITextFieldViewMode
import platform.UIKit.UIToolbar
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal actual fun PlatformProductNameTextField(
    state: TextFieldState,
    placeholder: String,
    onNext: () -> Unit,
    modifier: Modifier
) {
    val currentState = rememberUpdatedState(state)
    val currentOnNext = rememberUpdatedState(onNext)
    var isFocused by remember { mutableStateOf(false) }

    val focusedContainerColor =
        MaterialTheme.supremeColors.panelMuted
    val unfocusedContainerColor =
        MaterialTheme.supremeColors.panel
    val focusedBorderColor =
        MaterialTheme.colorScheme.primary
    val unfocusedBorderColor =
        MaterialTheme.supremeColors.border
    val textColor = MaterialTheme.colorScheme.onSurface
    val tintColor = MaterialTheme.colorScheme.primary
    val usesDarkKeyboard =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val coordinator = remember {
        ProductNameTextFieldCoordinator(
            onTextChanged = { value ->
                val activeState = currentState.value
                if (activeState.text.toString() != value) {
                    activeState.setTextAndPlaceCursorAtEnd(value)
                }
            },
            onNext = {
                currentOnNext.value()
            },
            onFocusChanged = { focused ->
                isFocused = focused
            }
        )
    }

    UIKitView(
        factory = {
            UITextField(
                frame = CGRectZero.readValue()
            ).apply {
                delegate = coordinator
                borderStyle =
                    UITextBorderStyle.UITextBorderStyleNone
                clearButtonMode =
                    UITextFieldViewMode.UITextFieldViewModeWhileEditing
                leftView = UIView(
                    frame = CGRectMake(
                        x = 0.0,
                        y = 0.0,
                        width = 14.0,
                        height = 1.0
                    )
                )
                leftViewMode =
                    UITextFieldViewMode.UITextFieldViewModeAlways
                keyboardType = UIKeyboardTypeDefault
                returnKeyType = UIReturnKeyType.UIReturnKeyNext
                autocapitalizationType =
                    UITextAutocapitalizationType
                        .UITextAutocapitalizationTypeWords
                autocorrectionType =
                    UITextAutocorrectionType
                        .UITextAutocorrectionTypeDefault
                textContentType = null
                layer.cornerRadius = 12.0
                layer.borderWidth = 1.0

                addTarget(
                    target = coordinator,
                    action = NSSelectorFromString("editingChanged:"),
                    forControlEvents =
                        UIControlEventEditingChanged
                )

                inputAccessoryView =
                    productNameAccessoryToolbar(coordinator)
            }
        },
        modifier = modifier,
        update = { textField ->
            val composeText = state.text.toString()
            if (textField.text != composeText) {
                textField.text = composeText
            }

            textField.placeholder = placeholder
            textField.textColor = textColor.toUIColor()
            textField.tintColor = tintColor.toUIColor()
            textField.backgroundColor = (
                if (isFocused) {
                    focusedContainerColor
                } else {
                    unfocusedContainerColor
                }
            ).toUIColor()
            textField.layer.borderColor = (
                if (isFocused) {
                    focusedBorderColor
                } else {
                    unfocusedBorderColor
                }
            ).toUIColor().CGColor
            textField.keyboardAppearance =
                if (usesDarkKeyboard) {
                    UIKeyboardAppearanceDark
                } else {
                    UIKeyboardAppearanceLight
                }
        },
        onRelease = { textField ->
            textField.delegate = null
            coordinator.release(textField)
        },
        properties = UIKitInteropProperties(
            interactionMode =
                UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true
        )
    )
}

private fun productNameAccessoryToolbar(
    coordinator: ProductNameTextFieldCoordinator
): UIToolbar = UIToolbar().apply {
    val flexibleSpace = UIBarButtonItem(
        barButtonSystemItem =
            UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace,
        target = null,
        action = null
    )
    val nextButton = UIBarButtonItem(
        title = "Next",
        style = UIBarButtonItemStyle.UIBarButtonItemStyleDone,
        target = coordinator,
        action = NSSelectorFromString(
            coordinator::performNext.name
        )
    )

    items = listOf(flexibleSpace, nextButton)
    sizeToFit()
}

private class ProductNameTextFieldCoordinator(
    private val onTextChanged: (String) -> Unit,
    private val onNext: () -> Unit,
    private val onFocusChanged: (Boolean) -> Unit
) : NSObject(), UITextFieldDelegateProtocol {
    private var activeTextField: UITextField? = null

    @ObjCAction
    fun editingChanged(textField: UITextField) {
        onTextChanged(textField.text.orEmpty())
    }

    @ObjCAction
    fun performNext() {
        activeTextField?.resignFirstResponder()
        dispatch_async(dispatch_get_main_queue()) {
            onNext()
        }
    }

    override fun textFieldDidBeginEditing(
        textField: UITextField
    ) {
        activeTextField = textField
        onFocusChanged(true)
    }

    override fun textFieldDidEndEditing(
        textField: UITextField
    ) {
        if (activeTextField === textField) {
            activeTextField = null
        }
        onFocusChanged(false)
    }

    override fun textFieldShouldClear(
        textField: UITextField
    ): Boolean {
        onTextChanged("")
        return true
    }

    override fun textFieldShouldReturn(
        textField: UITextField
    ): Boolean {
        activeTextField = textField
        performNext()
        return true
    }

    fun release(textField: UITextField) {
        if (activeTextField === textField) {
            activeTextField = null
        }
    }
}

private fun Color.toUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble()
)
