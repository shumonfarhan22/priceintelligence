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

    private companion object {
        const val ADVANCED_MODE_KEY = "advanced_mode_enabled"
    }
}