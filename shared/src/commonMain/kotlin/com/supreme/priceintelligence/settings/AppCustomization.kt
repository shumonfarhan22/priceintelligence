package com.supreme.priceintelligence.settings

enum class AppAccentColor(
    val displayName: String
) {
    SUPREME("Supreme Adaptive"),
    EMERALD("Supreme Emerald"),
    GOLD("Royal Gold"),
    INDIGO("Indigo"),
    OCEAN("Ocean Blue"),
    TEAL("Teal"),
    SAPPHIRE("Sapphire"),
    AMETHYST("Amethyst"),
    COPPER("Copper")
}

enum class AppFontStyle(
    val displayName: String
) {
    NATIVE("Native"),
    MODERN("Modern"),
    FRIENDLY("Friendly"),
    EDITORIAL("Editorial"),
    CLASSIC("Classic"),
    TECHNICAL("Technical"),
    COMPACT("Compact"),
    SPACIOUS("Spacious")
}

enum class AppTextSize(
    val displayName: String,
    val scale: Float
) {
    STANDARD("Standard", 1f),
    COMFORTABLE("Comfortable", 1.08f),
    LARGE("Large", 1.16f)
}

enum class AppDisplayDensity(
    val displayName: String
) {
    COMFORTABLE("Comfortable"),
    COMPACT("Compact")
}

enum class AppMotionPreference(
    val displayName: String
) {
    SYSTEM("Follow phone"),
    SMOOTH("Smooth"),
    REDUCED("Reduced")
}

enum class DashboardCardStyle(
    val displayName: String
) {
    DETAILED("Detailed"),
    COMPACT("Compact"),
    PRICE_FOCUSED("Price focused")
}

enum class DashboardDefaultSort(
    val displayName: String
) {
    MOST_VIEWED("Most viewed"),
    BEST_SAVING("Best saving"),
    ALPHABETICAL("Alphabetical"),
    RECENT("Recent")
}

enum class DashboardPageSize(
    val displayName: String,
    val productCount: Int
) {
    TEN("10", 10),
    FIFTEEN("15", 15),
    TWENTY("20", 20)
}

enum class PriceMovementDefaultRange(
    val displayName: String
) {
    SEVEN_DAYS("7 days"),
    FOURTEEN_DAYS("14 days"),
    THIRTY_DAYS("30 days")
}

enum class PriceAlertDirection(
    val displayName: String
) {
    BOTH("Both"),
    INCREASES_ONLY("Increases"),
    DECREASES_ONLY("Decreases")
}

enum class PriceAlertThreshold(
    val displayName: String
) {
    ANY("Any change"),
    RUPEES_50("₹50 or more"),
    PERCENT_2("2% or more"),
    PERCENT_5("5% or more")
}

data class AppCustomization(
    val accentColor: AppAccentColor =
        AppAccentColor.SUPREME,
    val fontStyle: AppFontStyle =
        AppFontStyle.NATIVE,
    val textSize: AppTextSize =
        AppTextSize.STANDARD,
    val displayDensity: AppDisplayDensity =
        AppDisplayDensity.COMFORTABLE,
    val motionPreference: AppMotionPreference =
        AppMotionPreference.SYSTEM,
    val hapticsEnabled: Boolean = true,
    val dashboardCardStyle: DashboardCardStyle =
        DashboardCardStyle.DETAILED,
    val dashboardDefaultSort: DashboardDefaultSort =
        DashboardDefaultSort.MOST_VIEWED,
    val dashboardPageSize: DashboardPageSize =
        DashboardPageSize.TEN,
    val priceMovementDefaultRange:
    PriceMovementDefaultRange =
        PriceMovementDefaultRange.THIRTY_DAYS,
    val priceAlertDirection: PriceAlertDirection =
        PriceAlertDirection.BOTH,
    val priceAlertThreshold: PriceAlertThreshold =
        PriceAlertThreshold.ANY,
    val insightCustomization: InsightCustomization =
        InsightCustomization()
)

fun readAppCustomization(
    storedValue: String?
): AppCustomization {
    val parts = storedValue
        ?.split('|')
        .orEmpty()

    if (parts.firstOrNull() != PROFILE_VERSION) {
        return AppCustomization()
    }

    return AppCustomization(
        accentColor =
            enumValueOrDefault(
                value = parts.getOrNull(1),
                defaultValue = AppAccentColor.SUPREME
            ),
        fontStyle =
            enumValueOrDefault(
                value = parts.getOrNull(2),
                defaultValue = AppFontStyle.NATIVE
            ),
        textSize =
            enumValueOrDefault(
                value = parts.getOrNull(3),
                defaultValue = AppTextSize.STANDARD
            ),
        displayDensity =
            enumValueOrDefault(
                value = parts.getOrNull(4),
                defaultValue =
                    AppDisplayDensity.COMFORTABLE
            ),
        motionPreference =
            enumValueOrDefault(
                value = parts.getOrNull(5),
                defaultValue =
                    AppMotionPreference.SYSTEM
            ),
        hapticsEnabled =
            parts.getOrNull(6)
                ?.toBooleanStrictOrNull()
                ?: true,
        dashboardCardStyle =
            enumValueOrDefault(
                value = parts.getOrNull(7),
                defaultValue =
                    DashboardCardStyle.DETAILED
            ),
        dashboardDefaultSort =
            enumValueOrDefault(
                value = parts.getOrNull(8),
                defaultValue =
                    DashboardDefaultSort.MOST_VIEWED
            ),
        dashboardPageSize =
            enumValueOrDefault(
                value = parts.getOrNull(9),
                defaultValue =
                    DashboardPageSize.TEN
            ),
        priceMovementDefaultRange =
            enumValueOrDefault(
                value = parts.getOrNull(10),
                defaultValue =
                    PriceMovementDefaultRange
                        .THIRTY_DAYS
            ),
        priceAlertDirection =
            enumValueOrDefault(
                value = parts.getOrNull(11),
                defaultValue =
                    PriceAlertDirection.BOTH
            ),
        priceAlertThreshold =
            enumValueOrDefault(
                value = parts.getOrNull(12),
                defaultValue =
                    PriceAlertThreshold.ANY
            ),
        insightCustomization =
            readInsightCustomization(
                parts.getOrNull(13)
            )
    )
}

fun writeAppCustomization(
    customization: AppCustomization
): String =
    listOf(
        PROFILE_VERSION,
        customization.accentColor.name,
        customization.fontStyle.name,
        customization.textSize.name,
        customization.displayDensity.name,
        customization.motionPreference.name,
        customization.hapticsEnabled.toString(),
        customization.dashboardCardStyle.name,
        customization.dashboardDefaultSort.name,
        customization.dashboardPageSize.name,
        customization.priceMovementDefaultRange.name,
        customization.priceAlertDirection.name,
        customization.priceAlertThreshold.name,
        writeInsightCustomization(
            customization.insightCustomization
        )
    ).joinToString("|")

private inline fun <reified T : Enum<T>>
        enumValueOrDefault(
    value: String?,
    defaultValue: T
): T =
    enumValues<T>()
        .firstOrNull { option ->
            option.name == value
        }
        ?: defaultValue

private const val PROFILE_VERSION = "v1"
