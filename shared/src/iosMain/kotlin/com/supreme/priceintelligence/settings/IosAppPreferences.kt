package com.supreme.priceintelligence.settings

import platform.Foundation.NSUserDefaults

class IosAppPreferences : AppPreferences {

    private val preferences = NSUserDefaults.standardUserDefaults

    override var advancedModeEnabled: Boolean
        get() = preferences.boolForKey(ADVANCED_MODE_KEY)
        set(value) {
            preferences.setBool(
                value = value,
                forKey = ADVANCED_MODE_KEY
            )
        }

    override var themeMode: AppThemeMode
        get() = AppThemeMode.fromStoredValue(
            preferences.stringForKey(THEME_MODE_KEY)
        )
        set(value) {
            preferences.setObject(
                value = value.name,
                forKey = THEME_MODE_KEY
            )
        }

    private companion object {
        const val ADVANCED_MODE_KEY = "advanced_mode_enabled"
        const val THEME_MODE_KEY = "theme_mode"
    }
}