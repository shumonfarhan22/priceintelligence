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

    override var automaticPriceRefreshLedger: String
        get() = preferences.stringForKey(
            AUTOMATIC_PRICE_REFRESH_LEDGER_KEY
        ).orEmpty()
        set(value) {
            preferences.setObject(
                value = value,
                forKey = AUTOMATIC_PRICE_REFRESH_LEDGER_KEY
            )
        }

    override var smartRefreshProfile: String
        get() = preferences.stringForKey(
            SMART_REFRESH_PROFILE_KEY
        ).orEmpty()
        set(value) {
            preferences.setObject(
                value = value,
                forKey = SMART_REFRESH_PROFILE_KEY
            )
        }

    override var priceChangeNotificationsEnabled: Boolean
        get() = preferences.boolForKey(
            PRICE_CHANGE_NOTIFICATIONS_KEY
        )
        set(value) {
            preferences.setBool(
                value = value,
                forKey = PRICE_CHANGE_NOTIFICATIONS_KEY
            )
        }

    private companion object {
        const val ADVANCED_MODE_KEY = "advanced_mode_enabled"
        const val THEME_MODE_KEY = "theme_mode"
        const val AUTOMATIC_PRICE_REFRESH_LEDGER_KEY =
            "automatic_price_refresh_ledger"
        const val SMART_REFRESH_PROFILE_KEY =
            "smart_refresh_profile"
        const val PRICE_CHANGE_NOTIFICATIONS_KEY =
            "price_change_notifications_enabled"
    }
}