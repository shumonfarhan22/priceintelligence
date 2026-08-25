package com.supreme.priceintelligence.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeModeTest {

    @Test
    fun storedThemeValuesAreRestored() {
        assertEquals(
            AppThemeMode.SYSTEM,
            AppThemeMode.fromStoredValue("SYSTEM")
        )
        assertEquals(
            AppThemeMode.LIGHT,
            AppThemeMode.fromStoredValue("LIGHT")
        )
        assertEquals(
            AppThemeMode.DARK,
            AppThemeMode.fromStoredValue("DARK")
        )
    }

    @Test
    fun storedThemeParsingIsSafe() {
        assertEquals(
            AppThemeMode.LIGHT,
            AppThemeMode.fromStoredValue(" light ")
        )
        assertEquals(
            AppThemeMode.DARK,
            AppThemeMode.fromStoredValue(null)
        )
        assertEquals(
            AppThemeMode.DARK,
            AppThemeMode.fromStoredValue("unknown")
        )
    }
}