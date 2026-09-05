export const PROFILE_VERSION = 'p2';
export const INSIGHT_PROFILE_VERSION = 'i1';
export const MAX_SAVED_PERSONALIZATION_PRESETS = 5;
export const MAX_SAVED_PRESET_NAME_LENGTH = 24;

export type PersonalizationPreviewTarget =
  | 'LAUNCH_HUB'
  | 'QUICK_COMPARE'
  | 'PRODUCT_DETAILS'
  | 'PRICE_MOVEMENT'
  | 'ALERTS';

export type PersonalizationSection =
  | 'APPEARANCE'
  | 'QUICK_COMPARE'
  | 'SHOP_SUMMARY'
  | 'PRODUCT_DETAILS'
  | 'PRICE_MOVEMENT'
  | 'ALERTS_BEHAVIOUR';

export type AppThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

export type AppAccentColor =
  | 'SUPREME'
  | 'EMERALD'
  | 'GOLD'
  | 'INDIGO'
  | 'OCEAN'
  | 'TEAL'
  | 'SAPPHIRE'
  | 'AMETHYST'
  | 'COPPER';

export type AppColorPalette =
  | 'SUPREME_HARMONY'
  | 'OCEAN_COPPER'
  | 'ROYAL_AMETHYST'
  | 'CUSTOM';

export type CustomPaletteRole =
  | 'PRIMARY'
  | 'SECONDARY'
  | 'COMPETITIVE'
  | 'WARNING'
  | 'REVIEW';

export interface CustomAppColorPalette {
  primaryHex: string;
  secondaryHex: string;
  competitiveHex: string;
  warningHex: string;
  reviewHex: string;
}

export const DEFAULT_CUSTOM_PALETTE: CustomAppColorPalette = {
  primaryHex: '#10B981',
  secondaryHex: '#8B7CF6',
  competitiveHex: '#34D399',
  warningHex: '#F59E0B',
  reviewHex: '#FB7185',
};

export type AppFontStyle =
  | 'SYSTEM'
  | 'ROBOTO'
  | 'INTER'
  | 'OPEN_SANS'
  | 'LATO'
  | 'MONTSERRAT'
  | 'POPPINS'
  | 'TECHNICAL';

export type AppTextSize = 'STANDARD' | 'COMFORTABLE' | 'LARGE';

export type AppDisplayDensity = 'COMFORTABLE' | 'COMPACT';

export type AppMotionPreference = 'SYSTEM' | 'SMOOTH' | 'REDUCED';

export type LaunchTileIconStyle =
  | 'CLEAN'
  | 'CLASSIC'
  | 'BUSINESS'
  | 'PRODUCT'
  | 'DATA';

export interface LaunchTileIconPreferences {
  insights: LaunchTileIconStyle;
  inventory: LaunchTileIconStyle;
  priceMovement: LaunchTileIconStyle;
  quickCompare: LaunchTileIconStyle;
}

export const DEFAULT_TILE_ICONS: LaunchTileIconPreferences = {
  insights: 'CLEAN',
  inventory: 'CLEAN',
  priceMovement: 'CLEAN',
  quickCompare: 'CLEAN',
};

export type DashboardCardStyle = 'DETAILED' | 'COMPACT' | 'PRICE_FOCUSED';

export type DashboardDefaultSort =
  | 'MOST_VIEWED'
  | 'BEST_SAVING'
  | 'ALPHABETICAL'
  | 'RECENT';

export type DashboardPageSize = 10 | 15 | 20;

export type PriceMovementDefaultRange = 'SEVEN_DAYS' | 'FOURTEEN_DAYS' | 'THIRTY_DAYS';

export type PriceAlertDirection = 'BOTH' | 'INCREASES_ONLY' | 'DECREASES_ONLY';

export type PriceAlertThreshold = 'ANY' | 'RUPEES_50' | 'PERCENT_2' | 'PERCENT_5';

export type AppContrastMode = 'STANDARD' | 'HIGH';

export type AppSurfaceStyle = 'SOFT' | 'SOLID' | 'GLASS';

export type PriceEmphasis = 'NORMAL' | 'BOLD';

export type SectionStartState = 'COLLAPSED' | 'EXPANDED';

export type BreakdownLayout = 'KPI_CARDS' | 'COMPACT_STRIP';

export type BreakdownValueMode = 'COUNTS' | 'COUNTS_AND_PERCENTAGES';

export type PriorityProductLimit = 3 | 5 | 10;

export type PrioritySortMode = 'RUPEE_GAP' | 'PERCENTAGE_GAP';

export type PriorityRowStyle = 'COMPACT' | 'DETAILED';

export type AdvancedInfoLevel = 'ESSENTIAL' | 'STANDARD' | 'FULL';

export type PriceHistoryRange = 'SEVEN_DAYS' | 'FOURTEEN_DAYS' | 'THIRTY_DAYS';

export type HistoryGraphStyle = 'LINE' | 'AREA' | 'STEP';

export type GraphSize = 'COMPACT' | 'STANDARD' | 'LARGE';

export type GraphPointMode = 'TAP_ONLY' | 'ALWAYS_LATEST' | 'HIDDEN';

export type RetailerChartPalette =
  | 'ORIGINAL'
  | 'EMERALD_INDIGO'
  | 'COPPER_TEAL'
  | 'GOLD_AMETHYST'
  | 'CORAL_SAPPHIRE'
  | 'CYAN_VIOLET'
  | 'AMBER_SKY'
  | 'PINK_AQUA'
  | 'CUSTOM';

export interface CustomRetailerChartColors {
  amazonHex: string;
  flipkartHex: string;
}

export const DEFAULT_CUSTOM_RETAILER_CHART_COLORS: CustomRetailerChartColors = {
  amazonHex: '#FF9900',
  flipkartHex: '#2874F0',
};

export type MovementDefaultRetailer = 'ALL' | 'AMAZON' | 'FLIPKART';

export type MovementLayout = 'OVERVIEW_FIRST' | 'PRODUCTS_FIRST';

export type MovementProductSort =
  | 'LATEST_CHANGE'
  | 'RUPEE_CHANGE'
  | 'PERCENTAGE_CHANGE'
  | 'PRODUCT_NAME';

export type MovementDirectionFilter = 'BOTH' | 'INCREASES' | 'DECREASES';

export type MovementProductGraphState = 'EXPANDED' | 'COLLAPSED';

export interface InsightCustomization {
  contrastMode: AppContrastMode;
  surfaceStyle: AppSurfaceStyle;
  reduceTransparency: boolean;
  priceEmphasis: PriceEmphasis;
  shopOverviewStartState: SectionStartState;
  breakdownStartState: SectionStartState;
  breakdownLayout: BreakdownLayout;
  breakdownValueMode: BreakdownValueMode;
  prioritiesStartState: SectionStartState;
  priorityProductLimit: PriorityProductLimit;
  prioritySortMode: PrioritySortMode;
  priorityRowStyle: PriorityRowStyle;
  advancedInfoStartState: SectionStartState;
  advancedInfoLevel: AdvancedInfoLevel;
  priceHistoryRange: PriceHistoryRange;
  historyGraphStyle: HistoryGraphStyle;
  graphSize: GraphSize;
  graphPointMode: GraphPointMode;
  retailerChartPalette: RetailerChartPalette;
  customRetailerChartColors: CustomRetailerChartColors;
  movementDefaultRetailer: MovementDefaultRetailer;
  movementLayout: MovementLayout;
  movementProductSort: MovementProductSort;
  movementDirectionFilter: MovementDirectionFilter;
  movementGraphStyle: HistoryGraphStyle;
  movementProductGraphState: MovementProductGraphState;
}

export const DEFAULT_INSIGHT_CUSTOMIZATION: InsightCustomization = {
  contrastMode: 'STANDARD',
  surfaceStyle: 'SOFT',
  reduceTransparency: false,
  priceEmphasis: 'NORMAL',
  shopOverviewStartState: 'COLLAPSED',
  breakdownStartState: 'COLLAPSED',
  breakdownLayout: 'KPI_CARDS',
  breakdownValueMode: 'COUNTS',
  prioritiesStartState: 'COLLAPSED',
  priorityProductLimit: 5,
  prioritySortMode: 'RUPEE_GAP',
  priorityRowStyle: 'DETAILED',
  advancedInfoStartState: 'COLLAPSED',
  advancedInfoLevel: 'STANDARD',
  priceHistoryRange: 'THIRTY_DAYS',
  historyGraphStyle: 'LINE',
  graphSize: 'STANDARD',
  graphPointMode: 'TAP_ONLY',
  retailerChartPalette: 'ORIGINAL',
  customRetailerChartColors: DEFAULT_CUSTOM_RETAILER_CHART_COLORS,
  movementDefaultRetailer: 'ALL',
  movementLayout: 'OVERVIEW_FIRST',
  movementProductSort: 'LATEST_CHANGE',
  movementDirectionFilter: 'BOTH',
  movementGraphStyle: 'LINE',
  movementProductGraphState: 'EXPANDED',
};

export interface SavedPersonalizationPreset {
  name: string;
  themeMode: AppThemeMode;
  advancedModeEnabled: boolean;
  priceChangeNotificationsEnabled: boolean;
  customizationProfile: string;
}

export interface AppCustomization {
  accentColor: AppAccentColor;
  appColorPalette: AppColorPalette;
  customColorPalette: CustomAppColorPalette;
  fontStyle: AppFontStyle;
  textSize: AppTextSize;
  displayDensity: AppDisplayDensity;
  motionPreference: AppMotionPreference;
  launchTileIconPreferences: LaunchTileIconPreferences;
  hapticsEnabled: boolean;
  automaticPriceChecksEnabled: boolean;
  dashboardCardStyle: DashboardCardStyle;
  dashboardDefaultSort: DashboardDefaultSort;
  dashboardPageSize: DashboardPageSize;
  priceMovementDefaultRange: PriceMovementDefaultRange;
  priceAlertDirection: PriceAlertDirection;
  priceAlertThreshold: PriceAlertThreshold;
  insightCustomization: InsightCustomization;
  savedPersonalizationPresets: SavedPersonalizationPreset[];
}

export const DEFAULT_APP_CUSTOMIZATION: AppCustomization = {
  accentColor: 'SUPREME',
  appColorPalette: 'SUPREME_HARMONY',
  customColorPalette: DEFAULT_CUSTOM_PALETTE,
  fontStyle: 'SYSTEM',
  textSize: 'STANDARD',
  displayDensity: 'COMFORTABLE',
  motionPreference: 'SYSTEM',
  launchTileIconPreferences: DEFAULT_TILE_ICONS,
  hapticsEnabled: true,
  automaticPriceChecksEnabled: true,
  dashboardCardStyle: 'DETAILED',
  dashboardDefaultSort: 'MOST_VIEWED',
  dashboardPageSize: 10,
  priceMovementDefaultRange: 'THIRTY_DAYS',
  priceAlertDirection: 'BOTH',
  priceAlertThreshold: 'ANY',
  insightCustomization: DEFAULT_INSIGHT_CUSTOMIZATION,
  savedPersonalizationPresets: [],
};

export type PersonalizationPreset =
  | 'SUPREME'
  | 'QUICK_SHOP'
  | 'ANALYST'
  | 'COMFORTABLE';

export interface PresetInfo {
  id: PersonalizationPreset;
  displayName: string;
  description: string;
}

export const PRESETS_INFO: PresetInfo[] = [
  {
    id: 'SUPREME',
    displayName: 'Supreme Default',
    description: 'Balanced original appearance and information.',
  },
  {
    id: 'QUICK_SHOP',
    displayName: 'Quick Shop',
    description: 'Compact cards with immediate shop decisions.',
  },
  {
    id: 'ANALYST',
    displayName: 'Analyst',
    description: 'Full history and detailed price movement.',
  },
  {
    id: 'COMFORTABLE',
    displayName: 'Comfortable',
    description: 'Large text, solid surfaces and high contrast.',
  },
];

export function personalizationForPreset(preset: PersonalizationPreset): AppCustomization {
  switch (preset) {
    case 'SUPREME':
      return { ...DEFAULT_APP_CUSTOMIZATION };

    case 'QUICK_SHOP':
      return {
        ...DEFAULT_APP_CUSTOMIZATION,
        displayDensity: 'COMPACT',
        dashboardCardStyle: 'COMPACT',
        dashboardPageSize: 15,
        insightCustomization: {
          ...DEFAULT_INSIGHT_CUSTOMIZATION,
          priceEmphasis: 'BOLD',
          shopOverviewStartState: 'EXPANDED',
          breakdownStartState: 'EXPANDED',
          breakdownLayout: 'COMPACT_STRIP',
          prioritiesStartState: 'EXPANDED',
          priorityProductLimit: 3,
          priorityRowStyle: 'COMPACT',
          priceHistoryRange: 'SEVEN_DAYS',
          graphSize: 'COMPACT',
          graphPointMode: 'HIDDEN',
          movementProductGraphState: 'COLLAPSED',
        },
      };

    case 'ANALYST':
      return {
        ...DEFAULT_APP_CUSTOMIZATION,
        dashboardDefaultSort: 'BEST_SAVING',
        dashboardPageSize: 20,
        insightCustomization: {
          ...DEFAULT_INSIGHT_CUSTOMIZATION,
          shopOverviewStartState: 'EXPANDED',
          breakdownStartState: 'EXPANDED',
          breakdownValueMode: 'COUNTS_AND_PERCENTAGES',
          prioritiesStartState: 'EXPANDED',
          priorityProductLimit: 10,
          prioritySortMode: 'PERCENTAGE_GAP',
          advancedInfoStartState: 'EXPANDED',
          advancedInfoLevel: 'FULL',
          historyGraphStyle: 'AREA',
          graphSize: 'LARGE',
          graphPointMode: 'ALWAYS_LATEST',
          movementProductSort: 'PERCENTAGE_CHANGE',
          movementGraphStyle: 'AREA',
        },
      };

    case 'COMFORTABLE':
      return {
        ...DEFAULT_APP_CUSTOMIZATION,
        textSize: 'LARGE',
        displayDensity: 'COMFORTABLE',
        insightCustomization: {
          ...DEFAULT_INSIGHT_CUSTOMIZATION,
          contrastMode: 'HIGH',
          surfaceStyle: 'SOLID',
          reduceTransparency: true,
          priceEmphasis: 'BOLD',
          priorityProductLimit: 3,
          graphSize: 'LARGE',
        },
      };
  }
}

export function normalizePaletteHex(value: string): string | null {
  const rawValue = value.trim().replace(/^#/, '');
  if (rawValue.length !== 6) return null;
  const isHex = /^[0-9a-fA-F]{6}$/.test(rawValue);
  return isHex ? `#${rawValue.toUpperCase()}` : null;
}

export function writeAppCustomization(customization: AppCustomization): string {
  const parts = [
    PROFILE_VERSION,
    customization.accentColor,
    customization.fontStyle,
    customization.textSize,
    customization.displayDensity,
    customization.motionPreference,
    String(customization.hapticsEnabled),
    customization.dashboardCardStyle,
    customization.dashboardDefaultSort,
    String(customization.dashboardPageSize),
    customization.priceMovementDefaultRange,
    customization.priceAlertDirection,
    customization.priceAlertThreshold,
    writeInsightCustomization(customization.insightCustomization),
    customization.appColorPalette,
    writeCustomColorPalette(customization.customColorPalette),
    '', // legacy savedColorPreset
    '', // legacy savedPersonalizationPreset
    writeSavedPresetsList(customization.savedPersonalizationPresets),
    String(customization.automaticPriceChecksEnabled),
    customization.launchTileIconPreferences.insights,
    writeTileIcons(customization.launchTileIconPreferences),
  ];
  return parts.join('|');
}

export function readAppCustomization(storedValue?: string | null): AppCustomization {
  if (!storedValue) return { ...DEFAULT_APP_CUSTOMIZATION };
  const parts = storedValue.split('|');
  if (parts[0] !== PROFILE_VERSION) return { ...DEFAULT_APP_CUSTOMIZATION };

  const legacyIcon = (parts[20] as LaunchTileIconStyle) || 'CLEAN';
  const tileIcons = readTileIcons(parts[21], legacyIcon);

  return {
    accentColor: (parts[1] as AppAccentColor) || DEFAULT_APP_CUSTOMIZATION.accentColor,
    fontStyle: (parts[2] as AppFontStyle) || DEFAULT_APP_CUSTOMIZATION.fontStyle,
    textSize: (parts[3] as AppTextSize) || DEFAULT_APP_CUSTOMIZATION.textSize,
    displayDensity: (parts[4] as AppDisplayDensity) || DEFAULT_APP_CUSTOMIZATION.displayDensity,
    motionPreference: (parts[5] as AppMotionPreference) || DEFAULT_APP_CUSTOMIZATION.motionPreference,
    hapticsEnabled: parts[6] === 'true',
    dashboardCardStyle: (parts[7] as DashboardCardStyle) || DEFAULT_APP_CUSTOMIZATION.dashboardCardStyle,
    dashboardDefaultSort: (parts[8] as DashboardDefaultSort) || DEFAULT_APP_CUSTOMIZATION.dashboardDefaultSort,
    dashboardPageSize: (Number(parts[9]) as DashboardPageSize) || DEFAULT_APP_CUSTOMIZATION.dashboardPageSize,
    priceMovementDefaultRange: (parts[10] as PriceMovementDefaultRange) || DEFAULT_APP_CUSTOMIZATION.priceMovementDefaultRange,
    priceAlertDirection: (parts[11] as PriceAlertDirection) || DEFAULT_APP_CUSTOMIZATION.priceAlertDirection,
    priceAlertThreshold: (parts[12] as PriceAlertThreshold) || DEFAULT_APP_CUSTOMIZATION.priceAlertThreshold,
    insightCustomization: readInsightCustomization(parts[13]),
    appColorPalette: (parts[14] as AppColorPalette) || DEFAULT_APP_CUSTOMIZATION.appColorPalette,
    customColorPalette: readCustomColorPalette(parts[15]),
    savedPersonalizationPresets: readSavedPresetsList(parts[18]),
    automaticPriceChecksEnabled: parts[19] !== 'false',
    launchTileIconPreferences: tileIcons,
  };
}

function writeTileIcons(prefs: LaunchTileIconPreferences): string {
  return `i1;${prefs.insights};${prefs.inventory};${prefs.priceMovement};${prefs.quickCompare}`;
}

function readTileIcons(val?: string, legacy: LaunchTileIconStyle = 'CLEAN'): LaunchTileIconPreferences {
  if (!val) return { insights: legacy, inventory: legacy, priceMovement: legacy, quickCompare: legacy };
  const parts = val.split(';');
  if (parts[0] !== 'i1') return { insights: legacy, inventory: legacy, priceMovement: legacy, quickCompare: legacy };
  return {
    insights: (parts[1] as LaunchTileIconStyle) || legacy,
    inventory: (parts[2] as LaunchTileIconStyle) || legacy,
    priceMovement: (parts[3] as LaunchTileIconStyle) || legacy,
    quickCompare: (parts[4] as LaunchTileIconStyle) || legacy,
  };
}

function writeCustomColorPalette(p: CustomAppColorPalette): string {
  return `c1;${p.primaryHex};${p.secondaryHex};${p.competitiveHex};${p.warningHex};${p.reviewHex}`;
}

function readCustomColorPalette(val?: string): CustomAppColorPalette {
  if (!val) return { ...DEFAULT_CUSTOM_PALETTE };
  const parts = val.split(';');
  if (parts[0] !== 'c1') return { ...DEFAULT_CUSTOM_PALETTE };
  return {
    primaryHex: parts[1] || DEFAULT_CUSTOM_PALETTE.primaryHex,
    secondaryHex: parts[2] || DEFAULT_CUSTOM_PALETTE.secondaryHex,
    competitiveHex: parts[3] || DEFAULT_CUSTOM_PALETTE.competitiveHex,
    warningHex: parts[4] || DEFAULT_CUSTOM_PALETTE.warningHex,
    reviewHex: parts[5] || DEFAULT_CUSTOM_PALETTE.reviewHex,
  };
}

function writeSavedPresetsList(presets: SavedPersonalizationPreset[]): string {
  if (!presets || presets.length === 0) return '';
  return presets
    .map((p) => `${encodeURIComponent(p.name)}:${p.themeMode}:${p.advancedModeEnabled}:${p.priceChangeNotificationsEnabled}:${encodeURIComponent(p.customizationProfile)}`)
    .join('~');
}

function readSavedPresetsList(val?: string): SavedPersonalizationPreset[] {
  if (!val) return [];
  return val
    .split('~')
    .filter(Boolean)
    .map((chunk) => {
      const [rawName, themeMode, adv, notif, rawProf] = chunk.split(':');
      return {
        name: decodeURIComponent(rawName || 'Saved setup'),
        themeMode: (themeMode as AppThemeMode) || 'DARK',
        advancedModeEnabled: adv === 'true',
        priceChangeNotificationsEnabled: notif === 'true',
        customizationProfile: decodeURIComponent(rawProf || ''),
      };
    });
}

function writeInsightCustomization(c: InsightCustomization): string {
  return [
    INSIGHT_PROFILE_VERSION,
    c.contrastMode,
    c.surfaceStyle,
    String(c.reduceTransparency),
    c.priceEmphasis,
    c.shopOverviewStartState,
    c.breakdownStartState,
    c.breakdownLayout,
    c.breakdownValueMode,
    c.prioritiesStartState,
    String(c.priorityProductLimit),
    c.prioritySortMode,
    c.priorityRowStyle,
    c.advancedInfoStartState,
    c.advancedInfoLevel,
    c.priceHistoryRange,
    c.historyGraphStyle,
    c.graphSize,
    c.graphPointMode,
    c.movementDefaultRetailer,
    c.movementLayout,
    c.movementProductSort,
    c.movementDirectionFilter,
    c.movementGraphStyle,
    c.movementProductGraphState,
    c.retailerChartPalette,
    `${c.customRetailerChartColors.amazonHex};${c.customRetailerChartColors.flipkartHex}`,
  ].join(',');
}

function readInsightCustomization(val?: string): InsightCustomization {
  if (!val) return { ...DEFAULT_INSIGHT_CUSTOMIZATION };
  const parts = val.split(',');
  if (parts[0] !== INSIGHT_PROFILE_VERSION) return { ...DEFAULT_INSIGHT_CUSTOMIZATION };

  const [amz, flip] = (parts[26] || '').split(';');

  return {
    contrastMode: (parts[1] as AppContrastMode) || DEFAULT_INSIGHT_CUSTOMIZATION.contrastMode,
    surfaceStyle: (parts[2] as AppSurfaceStyle) || DEFAULT_INSIGHT_CUSTOMIZATION.surfaceStyle,
    reduceTransparency: parts[3] === 'true',
    priceEmphasis: (parts[4] as PriceEmphasis) || DEFAULT_INSIGHT_CUSTOMIZATION.priceEmphasis,
    shopOverviewStartState: (parts[5] as SectionStartState) || DEFAULT_INSIGHT_CUSTOMIZATION.shopOverviewStartState,
    breakdownStartState: (parts[6] as SectionStartState) || DEFAULT_INSIGHT_CUSTOMIZATION.breakdownStartState,
    breakdownLayout: (parts[7] as BreakdownLayout) || DEFAULT_INSIGHT_CUSTOMIZATION.breakdownLayout,
    breakdownValueMode: (parts[8] as BreakdownValueMode) || DEFAULT_INSIGHT_CUSTOMIZATION.breakdownValueMode,
    prioritiesStartState: (parts[9] as SectionStartState) || DEFAULT_INSIGHT_CUSTOMIZATION.prioritiesStartState,
    priorityProductLimit: (Number(parts[10]) as PriorityProductLimit) || DEFAULT_INSIGHT_CUSTOMIZATION.priorityProductLimit,
    prioritySortMode: (parts[11] as PrioritySortMode) || DEFAULT_INSIGHT_CUSTOMIZATION.prioritySortMode,
    priorityRowStyle: (parts[12] as PriorityRowStyle) || DEFAULT_INSIGHT_CUSTOMIZATION.priorityRowStyle,
    advancedInfoStartState: (parts[13] as SectionStartState) || DEFAULT_INSIGHT_CUSTOMIZATION.advancedInfoStartState,
    advancedInfoLevel: (parts[14] as AdvancedInfoLevel) || DEFAULT_INSIGHT_CUSTOMIZATION.advancedInfoLevel,
    priceHistoryRange: (parts[15] as PriceHistoryRange) || DEFAULT_INSIGHT_CUSTOMIZATION.priceHistoryRange,
    historyGraphStyle: (parts[16] as HistoryGraphStyle) || DEFAULT_INSIGHT_CUSTOMIZATION.historyGraphStyle,
    graphSize: (parts[17] as GraphSize) || DEFAULT_INSIGHT_CUSTOMIZATION.graphSize,
    graphPointMode: (parts[18] as GraphPointMode) || DEFAULT_INSIGHT_CUSTOMIZATION.graphPointMode,
    movementDefaultRetailer: (parts[19] as MovementDefaultRetailer) || DEFAULT_INSIGHT_CUSTOMIZATION.movementDefaultRetailer,
    movementLayout: (parts[20] as MovementLayout) || DEFAULT_INSIGHT_CUSTOMIZATION.movementLayout,
    movementProductSort: (parts[21] as MovementProductSort) || DEFAULT_INSIGHT_CUSTOMIZATION.movementProductSort,
    movementDirectionFilter: (parts[22] as MovementDirectionFilter) || DEFAULT_INSIGHT_CUSTOMIZATION.movementDirectionFilter,
    movementGraphStyle: (parts[23] as HistoryGraphStyle) || DEFAULT_INSIGHT_CUSTOMIZATION.movementGraphStyle,
    movementProductGraphState: (parts[24] as MovementProductGraphState) || DEFAULT_INSIGHT_CUSTOMIZATION.movementProductGraphState,
    retailerChartPalette: (parts[25] as RetailerChartPalette) || DEFAULT_INSIGHT_CUSTOMIZATION.retailerChartPalette,
    customRetailerChartColors: {
      amazonHex: amz || DEFAULT_CUSTOM_RETAILER_CHART_COLORS.amazonHex,
      flipkartHex: flip || DEFAULT_CUSTOM_RETAILER_CHART_COLORS.flipkartHex,
    },
  };
}
