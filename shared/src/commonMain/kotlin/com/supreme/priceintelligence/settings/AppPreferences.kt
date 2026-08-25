package com.supreme.priceintelligence.settings

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode {
            val normalizedValue = value
                ?.trim()
                ?.uppercase()

            return entries.firstOrNull { mode ->
                mode.name == normalizedValue
            } ?: DARK
        }
    }
}

interface AppPreferences {
    var advancedModeEnabled: Boolean
    var themeMode: AppThemeMode
    var automaticPriceRefreshLedger: String
    var smartRefreshProfile: String
    var priceChangeNotificationsEnabled: Boolean
}