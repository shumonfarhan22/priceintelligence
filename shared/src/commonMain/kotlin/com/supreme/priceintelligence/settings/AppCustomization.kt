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

enum class AppColorPalette(
    val displayName: String,
    val description: String
) {
    SUPREME_HARMONY(
        "Supreme Harmony",
        "Emerald actions with indigo highlights."
    ),
    OCEAN_COPPER(
        "Ocean Copper",
        "Sapphire actions with warm copper details."
    ),
    ROYAL_AMETHYST(
        "Royal Amethyst",
        "Violet actions with royal gold highlights."
    ),
    CUSTOM(
        "Custom Palette",
        "Choose every important colour yourself."
    )
}

enum class CustomPaletteRole(
    val displayName: String,
    val description: String
) {
    PRIMARY(
        "Primary actions",
        "Buttons, selected navigation and main controls."
    ),
    SECONDARY(
        "Highlights",
        "Secondary information and decorative emphasis."
    ),
    COMPETITIVE(
        "Competitive",
        "Good shop-price results and positive states."
    ),
    WARNING(
        "Needs check",
        "Freshness, attention and waiting states."
    ),
    REVIEW(
        "Review / error",
        "Lower online prices, failed checks and errors."
    )
}

data class CustomAppColorPalette(
    val primaryHex: String = "#10B981",
    val secondaryHex: String = "#8B7CF6",
    val competitiveHex: String = "#34D399",
    val warningHex: String = "#F59E0B",
    val reviewHex: String = "#FB7185"
) {
    fun hexFor(
        role: CustomPaletteRole
    ): String =
        when (role) {
            CustomPaletteRole.PRIMARY ->
                primaryHex

            CustomPaletteRole.SECONDARY ->
                secondaryHex

            CustomPaletteRole.COMPETITIVE ->
                competitiveHex

            CustomPaletteRole.WARNING ->
                warningHex

            CustomPaletteRole.REVIEW ->
                reviewHex
        }

    fun withHex(
        role: CustomPaletteRole,
        value: String
    ): CustomAppColorPalette {
        val normalized =
            normalizePaletteHex(value)
                ?: return this

        return when (role) {
            CustomPaletteRole.PRIMARY ->
                copy(primaryHex = normalized)

            CustomPaletteRole.SECONDARY ->
                copy(secondaryHex = normalized)

            CustomPaletteRole.COMPETITIVE ->
                copy(competitiveHex = normalized)

            CustomPaletteRole.WARNING ->
                copy(warningHex = normalized)

            CustomPaletteRole.REVIEW ->
                copy(reviewHex = normalized)
        }
    }
}

fun normalizePaletteHex(
    value: String
): String? {
    val rawValue =
        value
            .trim()
            .removePrefix("#")

    if (rawValue.length != 6) {
        return null
    }

    val isHexadecimal =
        rawValue.all { character ->
            character in '0'..'9' ||
                character in 'a'..'f' ||
                character in 'A'..'F'
        }

    if (!isHexadecimal) {
        return null
    }

    return "#" + rawValue.uppercase()
}

enum class AppFontStyle(
    val displayName: String
) {
    SYSTEM("System"),
    ROBOTO("Roboto"),
    INTER("Inter"),
    OPEN_SANS("Open Sans"),
    LATO("Lato"),
    MONTSERRAT("Montserrat"),
    POPPINS("Poppins"),
    TECHNICAL("Technical")
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
    val appColorPalette: AppColorPalette =
        AppColorPalette.SUPREME_HARMONY,
    val customColorPalette: CustomAppColorPalette =
        CustomAppColorPalette(),
    val fontStyle: AppFontStyle =
        AppFontStyle.SYSTEM,
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
            readFontStyle(
                value = parts.getOrNull(2)
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
            ),
        appColorPalette =
            enumValueOrDefault(
                value = parts.getOrNull(14),
                defaultValue =
                    AppColorPalette.SUPREME_HARMONY
            ),
        customColorPalette =
            readCustomColorPalette(
                parts.getOrNull(15)
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
        ),
        customization.appColorPalette.name,
        writeCustomColorPalette(
            customization.customColorPalette
        )
    ).joinToString("|")

private fun readCustomColorPalette(
    storedValue: String?
): CustomAppColorPalette {
    val parts =
        storedValue
            ?.split(';')
            .orEmpty()

    if (parts.firstOrNull() != "c1") {
        return CustomAppColorPalette()
    }

    val defaults =
        CustomAppColorPalette()

    return CustomAppColorPalette(
        primaryHex =
            normalizePaletteHex(
                parts.getOrNull(1).orEmpty()
            ) ?: defaults.primaryHex,
        secondaryHex =
            normalizePaletteHex(
                parts.getOrNull(2).orEmpty()
            ) ?: defaults.secondaryHex,
        competitiveHex =
            normalizePaletteHex(
                parts.getOrNull(3).orEmpty()
            ) ?: defaults.competitiveHex,
        warningHex =
            normalizePaletteHex(
                parts.getOrNull(4).orEmpty()
            ) ?: defaults.warningHex,
        reviewHex =
            normalizePaletteHex(
                parts.getOrNull(5).orEmpty()
            ) ?: defaults.reviewHex
    )
}

private fun writeCustomColorPalette(
    palette: CustomAppColorPalette
): String =
    listOf(
        "c1",
        palette.primaryHex,
        palette.secondaryHex,
        palette.competitiveHex,
        palette.warningHex,
        palette.reviewHex
    ).joinToString(";")

private fun readFontStyle(
    value: String?
): AppFontStyle =
    when (value) {
        "NATIVE" -> AppFontStyle.SYSTEM
        "MODERN" -> AppFontStyle.INTER
        "FRIENDLY" -> AppFontStyle.POPPINS
        "EDITORIAL" -> AppFontStyle.LATO
        "CLASSIC" -> AppFontStyle.LATO
        "COMPACT" -> AppFontStyle.ROBOTO
        "SPACIOUS" -> AppFontStyle.MONTSERRAT
        else ->
            enumValueOrDefault(
                value = value,
                defaultValue = AppFontStyle.SYSTEM
            )
    }

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
