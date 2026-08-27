package com.supreme.priceintelligence.settings

enum class AppContrastMode(val displayName: String) {
    STANDARD("Standard"),
    HIGH("High contrast")
}

enum class AppSurfaceStyle(val displayName: String) {
    SOFT("Soft"),
    SOLID("Solid"),
    GLASS("Glass")
}

enum class PriceEmphasis(val displayName: String) {
    NORMAL("Normal"),
    BOLD("Bold prices")
}

enum class SectionStartState(val displayName: String) {
    COLLAPSED("Collapsed"),
    EXPANDED("Expanded")
}

enum class BreakdownLayout(val displayName: String) {
    KPI_CARDS("KPI cards"),
    COMPACT_STRIP("Compact strip")
}

enum class BreakdownValueMode(val displayName: String) {
    COUNTS("Counts"),
    COUNTS_AND_PERCENTAGES("Counts + percent")
}

enum class PriorityProductLimit(
    val displayName: String,
    val count: Int
) {
    THREE("3 products", 3),
    FIVE("5 products", 5),
    TEN("10 products", 10)
}

enum class PrioritySortMode(val displayName: String) {
    RUPEE_GAP("Largest ₹ gap"),
    PERCENTAGE_GAP("Largest % gap")
}

enum class PriorityRowStyle(val displayName: String) {
    COMPACT("Compact"),
    DETAILED("Detailed")
}

enum class AdvancedInfoLevel(val displayName: String) {
    ESSENTIAL("Essential"),
    STANDARD("Standard"),
    FULL("Full analysis")
}

enum class PriceHistoryRange(
    val displayName: String,
    val days: Int
) {
    SEVEN_DAYS("7 days", 7),
    FOURTEEN_DAYS("14 days", 14),
    THIRTY_DAYS("30 days", 30)
}

enum class HistoryGraphStyle(val displayName: String) {
    LINE("Line"),
    AREA("Area"),
    STEP("Step")
}

enum class GraphSize(
    val displayName: String,
    val heightDp: Int
) {
    COMPACT("Compact", 112),
    STANDARD("Standard", 150),
    LARGE("Large", 190)
}

enum class GraphPointMode(val displayName: String) {
    TAP_ONLY("Show when tapped"),
    ALWAYS_LATEST("Always show latest"),
    HIDDEN("Hide details")
}

enum class RetailerChartPalette(
    val displayName: String
) {
    ORIGINAL("Original"),
    EMERALD_INDIGO("Emerald + indigo"),
    COPPER_TEAL("Copper + teal"),
    GOLD_AMETHYST("Gold + amethyst"),
    CORAL_SAPPHIRE("Coral + sapphire"),
    CYAN_VIOLET("Cyan + violet"),
    AMBER_SKY("Amber + sky"),
    PINK_AQUA("Pink + aqua"),
    CUSTOM("Custom hex")
}

data class CustomRetailerChartColors(
    val amazonHex: String = "#FF9900",
    val flipkartHex: String = "#2874F0"
)

enum class MovementDefaultRetailer(val displayName: String) {
    ALL("All"),
    AMAZON("Amazon"),
    FLIPKART("Flipkart")
}

enum class MovementLayout(val displayName: String) {
    OVERVIEW_FIRST("Overview first"),
    PRODUCTS_FIRST("Changed products first")
}

enum class MovementProductSort(val displayName: String) {
    LATEST_CHANGE("Latest change"),
    RUPEE_CHANGE("Largest ₹ change"),
    PERCENTAGE_CHANGE("Largest % change"),
    PRODUCT_NAME("Product name")
}

enum class MovementDirectionFilter(val displayName: String) {
    BOTH("Both"),
    INCREASES("Price increases"),
    DECREASES("Price decreases")
}

enum class MovementProductGraphState(val displayName: String) {
    EXPANDED("Expanded"),
    COLLAPSED("Collapsed")
}

data class InsightCustomization(
    val contrastMode: AppContrastMode =
        AppContrastMode.STANDARD,
    val surfaceStyle: AppSurfaceStyle =
        AppSurfaceStyle.SOFT,
    val reduceTransparency: Boolean = false,
    val priceEmphasis: PriceEmphasis =
        PriceEmphasis.NORMAL,
    val shopOverviewStartState: SectionStartState =
        SectionStartState.COLLAPSED,
    val breakdownStartState: SectionStartState =
        SectionStartState.COLLAPSED,
    val breakdownLayout: BreakdownLayout =
        BreakdownLayout.KPI_CARDS,
    val breakdownValueMode: BreakdownValueMode =
        BreakdownValueMode.COUNTS,
    val prioritiesStartState: SectionStartState =
        SectionStartState.COLLAPSED,
    val priorityProductLimit: PriorityProductLimit =
        PriorityProductLimit.FIVE,
    val prioritySortMode: PrioritySortMode =
        PrioritySortMode.RUPEE_GAP,
    val priorityRowStyle: PriorityRowStyle =
        PriorityRowStyle.DETAILED,
    val advancedInfoStartState: SectionStartState =
        SectionStartState.COLLAPSED,
    val advancedInfoLevel: AdvancedInfoLevel =
        AdvancedInfoLevel.STANDARD,
    val priceHistoryRange: PriceHistoryRange =
        PriceHistoryRange.THIRTY_DAYS,
    val historyGraphStyle: HistoryGraphStyle =
        HistoryGraphStyle.LINE,
    val graphSize: GraphSize = GraphSize.STANDARD,
    val graphPointMode: GraphPointMode =
        GraphPointMode.TAP_ONLY,
    val retailerChartPalette: RetailerChartPalette =
        RetailerChartPalette.ORIGINAL,
    val customRetailerChartColors:
        CustomRetailerChartColors =
        CustomRetailerChartColors(),
    val movementDefaultRetailer: MovementDefaultRetailer =
        MovementDefaultRetailer.ALL,
    val movementLayout: MovementLayout =
        MovementLayout.OVERVIEW_FIRST,
    val movementProductSort: MovementProductSort =
        MovementProductSort.LATEST_CHANGE,
    val movementDirectionFilter: MovementDirectionFilter =
        MovementDirectionFilter.BOTH,
    val movementGraphStyle: HistoryGraphStyle =
        HistoryGraphStyle.LINE,
    val movementProductGraphState: MovementProductGraphState =
        MovementProductGraphState.EXPANDED
)

enum class PersonalizationPreset(
    val displayName: String,
    val description: String
) {
    SUPREME(
        "Supreme Default",
        "Balanced original appearance and information."
    ),
    QUICK_SHOP(
        "Quick Shop",
        "Compact cards with immediate shop decisions."
    ),
    ANALYST(
        "Analyst",
        "Full history and detailed price movement."
    ),
    COMFORTABLE(
        "Comfortable",
        "Large text, solid surfaces and high contrast."
    )
}

fun personalizationForPreset(
    preset: PersonalizationPreset
): AppCustomization = when (preset) {
    PersonalizationPreset.SUPREME -> AppCustomization()

    PersonalizationPreset.QUICK_SHOP ->
        AppCustomization(
            displayDensity = AppDisplayDensity.COMPACT,
            dashboardCardStyle = DashboardCardStyle.COMPACT,
            dashboardPageSize = DashboardPageSize.FIFTEEN,
            insightCustomization = InsightCustomization(
                priceEmphasis = PriceEmphasis.BOLD,
                shopOverviewStartState = SectionStartState.EXPANDED,
                breakdownStartState = SectionStartState.EXPANDED,
                breakdownLayout = BreakdownLayout.COMPACT_STRIP,
                prioritiesStartState = SectionStartState.EXPANDED,
                priorityProductLimit = PriorityProductLimit.THREE,
                priorityRowStyle = PriorityRowStyle.COMPACT,
                priceHistoryRange = PriceHistoryRange.SEVEN_DAYS,
                graphSize = GraphSize.COMPACT,
                graphPointMode = GraphPointMode.HIDDEN,
                movementProductGraphState =
                    MovementProductGraphState.COLLAPSED
            )
        )

    PersonalizationPreset.ANALYST ->
        AppCustomization(
            dashboardDefaultSort = DashboardDefaultSort.BEST_SAVING,
            dashboardPageSize = DashboardPageSize.TWENTY,
            insightCustomization = InsightCustomization(
                shopOverviewStartState = SectionStartState.EXPANDED,
                breakdownStartState = SectionStartState.EXPANDED,
                breakdownValueMode =
                    BreakdownValueMode.COUNTS_AND_PERCENTAGES,
                prioritiesStartState = SectionStartState.EXPANDED,
                priorityProductLimit = PriorityProductLimit.TEN,
                prioritySortMode = PrioritySortMode.PERCENTAGE_GAP,
                advancedInfoStartState = SectionStartState.EXPANDED,
                advancedInfoLevel = AdvancedInfoLevel.FULL,
                historyGraphStyle = HistoryGraphStyle.AREA,
                graphSize = GraphSize.LARGE,
                graphPointMode = GraphPointMode.ALWAYS_LATEST,
                movementProductSort =
                    MovementProductSort.PERCENTAGE_CHANGE,
                movementGraphStyle = HistoryGraphStyle.AREA
            )
        )

    PersonalizationPreset.COMFORTABLE ->
        AppCustomization(
            textSize = AppTextSize.LARGE,
            displayDensity = AppDisplayDensity.COMFORTABLE,
            insightCustomization = InsightCustomization(
                contrastMode = AppContrastMode.HIGH,
                surfaceStyle = AppSurfaceStyle.SOLID,
                reduceTransparency = true,
                priceEmphasis = PriceEmphasis.BOLD,
                priorityProductLimit = PriorityProductLimit.THREE,
                graphSize = GraphSize.LARGE
            )
        )
}

fun matchingPersonalizationPreset(
    customization: AppCustomization
): PersonalizationPreset? {
    val comparableCustomization =
        customization.copy(
            savedColorPreset = null,
            savedPersonalizationPreset = null
        )

    return PersonalizationPreset.entries
        .firstOrNull { preset ->
            personalizationForPreset(preset) ==
                comparableCustomization
        }
}

internal fun readInsightCustomization(
    storedValue: String?
): InsightCustomization {
    val parts = storedValue?.split(',').orEmpty()
    if (parts.firstOrNull() != INSIGHT_PROFILE_VERSION) {
        return InsightCustomization()
    }

    return InsightCustomization(
        contrastMode = enumInsight(parts.getOrNull(1), AppContrastMode.STANDARD),
        surfaceStyle = enumInsight(parts.getOrNull(2), AppSurfaceStyle.SOFT),
        reduceTransparency = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false,
        priceEmphasis = enumInsight(parts.getOrNull(4), PriceEmphasis.NORMAL),
        shopOverviewStartState = enumInsight(parts.getOrNull(5), SectionStartState.COLLAPSED),
        breakdownStartState = enumInsight(parts.getOrNull(6), SectionStartState.COLLAPSED),
        breakdownLayout = enumInsight(parts.getOrNull(7), BreakdownLayout.KPI_CARDS),
        breakdownValueMode = enumInsight(parts.getOrNull(8), BreakdownValueMode.COUNTS),
        prioritiesStartState = enumInsight(parts.getOrNull(9), SectionStartState.COLLAPSED),
        priorityProductLimit = enumInsight(parts.getOrNull(10), PriorityProductLimit.FIVE),
        prioritySortMode = enumInsight(parts.getOrNull(11), PrioritySortMode.RUPEE_GAP),
        priorityRowStyle = enumInsight(parts.getOrNull(12), PriorityRowStyle.DETAILED),
        advancedInfoStartState = enumInsight(parts.getOrNull(13), SectionStartState.COLLAPSED),
        advancedInfoLevel = enumInsight(parts.getOrNull(14), AdvancedInfoLevel.STANDARD),
        priceHistoryRange = enumInsight(parts.getOrNull(15), PriceHistoryRange.THIRTY_DAYS),
        historyGraphStyle = enumInsight(parts.getOrNull(16), HistoryGraphStyle.LINE),
        graphSize = enumInsight(parts.getOrNull(17), GraphSize.STANDARD),
        graphPointMode = enumInsight(parts.getOrNull(18), GraphPointMode.TAP_ONLY),
        movementDefaultRetailer = enumInsight(parts.getOrNull(19), MovementDefaultRetailer.ALL),
        movementLayout = enumInsight(parts.getOrNull(20), MovementLayout.OVERVIEW_FIRST),
        movementProductSort = enumInsight(parts.getOrNull(21), MovementProductSort.LATEST_CHANGE),
        movementDirectionFilter = enumInsight(parts.getOrNull(22), MovementDirectionFilter.BOTH),
        movementGraphStyle = enumInsight(parts.getOrNull(23), HistoryGraphStyle.LINE),
        movementProductGraphState = enumInsight(parts.getOrNull(24), MovementProductGraphState.EXPANDED),
        retailerChartPalette = enumInsight(
            parts.getOrNull(25),
            RetailerChartPalette.ORIGINAL
        ),
        customRetailerChartColors =
            readCustomRetailerChartColors(
                parts.getOrNull(26)
            )
    )
}

internal fun writeInsightCustomization(
    customization: InsightCustomization
): String = listOf(
    INSIGHT_PROFILE_VERSION,
    customization.contrastMode.name,
    customization.surfaceStyle.name,
    customization.reduceTransparency.toString(),
    customization.priceEmphasis.name,
    customization.shopOverviewStartState.name,
    customization.breakdownStartState.name,
    customization.breakdownLayout.name,
    customization.breakdownValueMode.name,
    customization.prioritiesStartState.name,
    customization.priorityProductLimit.name,
    customization.prioritySortMode.name,
    customization.priorityRowStyle.name,
    customization.advancedInfoStartState.name,
    customization.advancedInfoLevel.name,
    customization.priceHistoryRange.name,
    customization.historyGraphStyle.name,
    customization.graphSize.name,
    customization.graphPointMode.name,
    customization.movementDefaultRetailer.name,
    customization.movementLayout.name,
    customization.movementProductSort.name,
    customization.movementDirectionFilter.name,
    customization.movementGraphStyle.name,
    customization.movementProductGraphState.name,
    customization.retailerChartPalette.name,
    writeCustomRetailerChartColors(
        customization.customRetailerChartColors
    )
).joinToString(",")

private fun readCustomRetailerChartColors(
    storedValue: String?
): CustomRetailerChartColors {
    val parts =
        storedValue
            ?.split(';')
            .orEmpty()

    val defaults =
        CustomRetailerChartColors()

    if (parts.firstOrNull() != "r1") {
        return defaults
    }

    return CustomRetailerChartColors(
        amazonHex =
            normalizePaletteHex(
                parts.getOrNull(1).orEmpty()
            ) ?: defaults.amazonHex,
        flipkartHex =
            normalizePaletteHex(
                parts.getOrNull(2).orEmpty()
            ) ?: defaults.flipkartHex
    )
}

private fun writeCustomRetailerChartColors(
    colors: CustomRetailerChartColors
): String =
    listOf(
        "r1",
        colors.amazonHex,
        colors.flipkartHex
    ).joinToString(";")

private inline fun <reified T : Enum<T>> enumInsight(
    value: String?,
    defaultValue: T
): T = enumValues<T>().firstOrNull { it.name == value } ?: defaultValue

private const val INSIGHT_PROFILE_VERSION = "i1"