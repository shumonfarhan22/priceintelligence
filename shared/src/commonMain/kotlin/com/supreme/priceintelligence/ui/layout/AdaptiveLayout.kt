package com.supreme.priceintelligence.ui.layout

enum class AdaptiveTextClass {
    STANDARD,
    LARGE,
    ACCESSIBILITY
}

data class AdaptiveLayoutPolicy(
    val availableWidthDp: Float,
    val fontScale: Float,
    val textClass: AdaptiveTextClass
) {
    val isLargeText: Boolean
        get() =
            textClass != AdaptiveTextClass.STANDARD

    val isNarrow: Boolean
        get() =
            availableWidthDp < 340f

    val importantTextMaxLines: Int
        get() =
            if (isLargeText || isNarrow) {
                Int.MAX_VALUE
            } else {
                2
            }

    fun shouldStack(
        minimumWidthForRowDp: Float
    ): Boolean =
        availableWidthDp <
                minimumWidthForRowDp ||
                isLargeText
}

fun adaptiveLayoutPolicy(
    availableWidthDp: Float,
    fontScale: Float
): AdaptiveLayoutPolicy {
    val safeWidth =
        if (
            availableWidthDp.isFinite() &&
            availableWidthDp > 0f
        ) {
            availableWidthDp
        } else {
            0f
        }

    val safeFontScale =
        if (
            fontScale.isFinite() &&
            fontScale > 0f
        ) {
            fontScale
        } else {
            1f
        }

    val textClass = when {
        safeFontScale >= 1.30f ->
            AdaptiveTextClass.ACCESSIBILITY

        safeFontScale >= 1.10f ->
            AdaptiveTextClass.LARGE

        else ->
            AdaptiveTextClass.STANDARD
    }

    return AdaptiveLayoutPolicy(
        availableWidthDp = safeWidth,
        fontScale = safeFontScale,
        textClass = textClass
    )
}