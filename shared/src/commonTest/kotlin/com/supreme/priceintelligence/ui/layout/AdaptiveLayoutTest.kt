package com.supreme.priceintelligence.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveLayoutTest {

    @Test
    fun standardTextKeepsWideRows() {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = 380f,
            fontScale = 1f
        )

        assertEquals(
            AdaptiveTextClass.STANDARD,
            policy.textClass
        )
        assertFalse(policy.shouldStack(340f))
        assertEquals(2, policy.importantTextMaxLines)
    }

    @Test
    fun largeTextUsesFlexibleRows() {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = 380f,
            fontScale = 1.16f
        )

        assertEquals(
            AdaptiveTextClass.LARGE,
            policy.textClass
        )
        assertTrue(policy.shouldStack(340f))
        assertEquals(
            Int.MAX_VALUE,
            policy.importantTextMaxLines
        )
    }

    @Test
    fun narrowScreenUsesFlexibleRows() {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = 320f,
            fontScale = 1f
        )

        assertTrue(policy.isNarrow)
        assertTrue(policy.shouldStack(340f))
        assertEquals(
            Int.MAX_VALUE,
            policy.importantTextMaxLines
        )
    }

    @Test
    fun accessibilityFontScaleIsRecognised() {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = 600f,
            fontScale = 1.4f
        )

        assertEquals(
            AdaptiveTextClass.ACCESSIBILITY,
            policy.textClass
        )
        assertTrue(policy.isLargeText)
        assertTrue(policy.shouldStack(340f))
    }

    @Test
    fun invalidMeasurementsUseSafeValues() {
        val policy = adaptiveLayoutPolicy(
            availableWidthDp = Float.NaN,
            fontScale = Float.NaN
        )

        assertEquals(0f, policy.availableWidthDp)
        assertEquals(1f, policy.fontScale)
        assertTrue(policy.isNarrow)
    }
}