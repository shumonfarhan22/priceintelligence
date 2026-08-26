package com.supreme.priceintelligence.settings

import android.content.Context

class AndroidAppPreferences(
    context: Context
) : AppPreferences {

    private val preferences = context.getSharedPreferences(
        "price_intelligence_settings",
        Context.MODE_PRIVATE
    )

    override var advancedModeEnabled: Boolean
        get() = preferences.getBoolean(ADVANCED_MODE_KEY, false)
        set(value) {
            preferences
                .edit()
                .putBoolean(ADVANCED_MODE_KEY, value)
                .apply()
        }

    override var themeMode: AppThemeMode
        get() = AppThemeMode.fromStoredValue(
            preferences.getString(THEME_MODE_KEY, null)
        )
        set(value) {
            preferences
                .edit()
                .putString(THEME_MODE_KEY, value.name)
                .apply()
        }

    override var customizationProfile: String
        get() = preferences.getString(
            CUSTOMIZATION_PROFILE_KEY,
            ""
        ).orEmpty()
        set(value) {
            preferences
                .edit()
                .putString(
                    CUSTOMIZATION_PROFILE_KEY,
                    value
                )
                .apply()
        }

    override var automaticPriceRefreshLedger: String
        get() = preferences.getString(
            AUTOMATIC_PRICE_REFRESH_LEDGER_KEY,
            ""
        ).orEmpty()
        set(value) {
            preferences
                .edit()
                .putString(
                    AUTOMATIC_PRICE_REFRESH_LEDGER_KEY,
                    value
                )
                .apply()
        }

    override var smartRefreshProfile: String
        get() = preferences.getString(
            SMART_REFRESH_PROFILE_KEY,
            ""
        ).orEmpty()
        set(value) {
            preferences
                .edit()
                .putString(
                    SMART_REFRESH_PROFILE_KEY,
                    value
                )
                .apply()
        }

    override var priceChangeNotificationsEnabled: Boolean
        get() = preferences.getBoolean(
            PRICE_CHANGE_NOTIFICATIONS_KEY,
            false
        )
        set(value) {
            preferences
                .edit()
                .putBoolean(
                    PRICE_CHANGE_NOTIFICATIONS_KEY,
                    value
                )
                .apply()
        }

    private companion object {
        const val ADVANCED_MODE_KEY = "advanced_mode_enabled"
        const val THEME_MODE_KEY = "theme_mode"
        const val CUSTOMIZATION_PROFILE_KEY =
            "customization_profile"
        const val AUTOMATIC_PRICE_REFRESH_LEDGER_KEY =
            "automatic_price_refresh_ledger"
        const val SMART_REFRESH_PROFILE_KEY =
            "smart_refresh_profile"
        const val PRICE_CHANGE_NOTIFICATIONS_KEY =
            "price_change_notifications_enabled"
    }
}