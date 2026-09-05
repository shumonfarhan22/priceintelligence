import Ionicons from '@expo/vector-icons/Ionicons';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import React, { useState } from 'react';
import {
  Image,
  LayoutChangeEvent,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import Svg, { Path } from 'react-native-svg';

import type {
  AppCustomization,
} from '../../domain/customization';
import type { DynamicColors } from '../../theme/dynamicTheme';
import { withAlpha } from '../../theme/dynamicTheme';
import { radius, type } from '../../theme/tokens';

export type PreviewTarget =
  | 'LAUNCH_HUB'
  | 'QUICK_COMPARE'
  | 'PRODUCT_DETAILS'
  | 'PRICE_MOVEMENT'
  | 'ALERTS';

export const PREVIEW_TARGETS: {
  id: PreviewTarget;
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
}[] = [
  { id: 'LAUNCH_HUB', label: 'Launch hub', icon: 'home' },
  { id: 'QUICK_COMPARE', label: 'Quick Compare', icon: 'search' },
  { id: 'PRODUCT_DETAILS', label: 'Product details', icon: 'information-circle' },
  { id: 'PRICE_MOVEMENT', label: 'Price movement', icon: 'trending-up' },
  { id: 'ALERTS', label: 'Alerts', icon: 'notifications' },
];

export function AdaptivePersonalizationPreview({
  customization,
  colors,
  target,
  editorOpen = false,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
  target: PreviewTarget;
  editorOpen?: boolean;
}) {
  const [containerSize, setContainerSize] = useState<{ width: number; height: number }>({
    width: 0,
    height: 0,
  });

  const onContainerLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width > 0 && height > 0) {
      setContainerSize({ width, height });
    }
  };

  const BASE_WIDTH = 390;
  const BASE_HEIGHT = 820;

  // In Compose: previewScale = minOf(maxWidth / 390f, maxHeight / 820f, 0.92f)
  const defaultScale = editorOpen ? 0.46 : 0.68;
  const previewScale =
    containerSize.width > 0 && containerSize.height > 0
      ? Math.min(containerSize.width / BASE_WIDTH, containerSize.height / BASE_HEIGHT, 0.92)
      : defaultScale;

  const scaledWidth = Math.round(BASE_WIDTH * previewScale);
  const scaledHeight = Math.round(BASE_HEIGHT * previewScale);

  return (
    <View style={styles.container} onLayout={onContainerLayout}>
      <View
        style={{
          width: scaledWidth,
          height: scaledHeight,
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden',
        }}
      >
        <View
          style={[
            styles.deviceFrame,
            {
              width: BASE_WIDTH,
              height: BASE_HEIGHT,
              transform: [{ scale: previewScale }],
              backgroundColor: colors.background,
              borderColor: withAlpha(colors.primary, '8C'), // 1.5dp primary with ~55% alpha matching Compose
            },
          ]}
        >
          <View style={styles.screenInner}>
            {target === 'LAUNCH_HUB' && (
              <LaunchHubPreview customization={customization} colors={colors} />
            )}
            {target === 'QUICK_COMPARE' && (
              <QuickComparePreview customization={customization} colors={colors} />
            )}
            {target === 'PRODUCT_DETAILS' && (
              <ProductDetailsPreview customization={customization} colors={colors} />
            )}
            {target === 'PRICE_MOVEMENT' && (
              <PriceMovementPreview customization={customization} colors={colors} />
            )}
            {target === 'ALERTS' && (
              <AlertsPreview customization={customization} colors={colors} />
            )}
          </View>
        </View>
      </View>
    </View>
  );
}

/**
 * Realistic Launch Hub preview screen matching Compose LaunchHubScreen:
 * - Logo + Supreme Price Intelligence header + Online status pill + mini settings gear
 * - Shop Overview card: '41 products compared', proportional green/pink bar, 'Competitive 38', 'Review 3'
 * - Top priorities (3) card
 * - 2x2 Launch Tiles grid: Insights (emerald), Inventory (purple), Price Movement (indigo), Quick Compare (amber)
 * - Empty handset bottom space matching Compose 390x820 phone frame
 */
function LaunchHubPreview({
  customization,
  colors,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
}) {
  const tilePrefs = customization.launchTileIconPreferences;

  return (
    <View style={styles.previewContent}>
      {/* Top Header */}
      <View style={styles.miniHeader}>
        <View style={styles.miniHeaderLeft}>
          <Image
            source={require('../../../assets/brand/app_logo.png')}
            style={styles.miniLogoImage}
            resizeMode="contain"
          />
          <View style={styles.brandTitleCol}>
            <Text style={[styles.miniTitle, { color: colors.primary }]}>SUPREME</Text>
            <Text style={[styles.miniSubtitle, { color: colors.text }]}>
              PRICE INTELLIGENCE
            </Text>
            <View style={styles.onlineRow}>
              <MaterialIcons name="wifi" size={11} color={colors.competitive} />
              <Text style={[styles.onlineText, { color: colors.textMuted }]}>Online</Text>
            </View>
          </View>
        </View>

        <View
          style={[
            styles.miniGearBtn,
            { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
          ]}
        >
          <MaterialIcons name="settings" size={18} color={colors.text} />
        </View>
      </View>

      {/* Shop Overview Card */}
      <View
        style={[
          styles.miniCard,
          { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
        ]}
      >
        <Text style={[styles.miniCardEyebrow, { color: colors.primary }]}>SHOP OVERVIEW</Text>
        <Text style={[styles.miniCardTitle, { color: colors.text }]}>41 products compared</Text>

        {/* Proportional track bar */}
        <View style={styles.miniTrack}>
          <View
            style={{
              flex: 38,
              height: 8,
              backgroundColor: colors.competitive,
              borderTopLeftRadius: 4,
              borderBottomLeftRadius: 4,
            }}
          />
          <View
            style={{
              flex: 3,
              height: 8,
              backgroundColor: colors.danger,
              borderTopRightRadius: 4,
              borderBottomRightRadius: 4,
              marginLeft: 2,
            }}
          />
        </View>

        <View style={styles.miniMetaRow}>
          <View style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: colors.competitive }]} />
            <Text style={[styles.legendLabel, { color: colors.textMuted }]}>Competitive</Text>
            <Text style={[styles.legendValue, { color: colors.text }]}>38</Text>
          </View>
          <View style={[styles.legendItem, { marginLeft: 14 }]}>
            <View style={[styles.legendDot, { backgroundColor: colors.danger }]} />
            <Text style={[styles.legendLabel, { color: colors.textMuted }]}>Review</Text>
            <Text style={[styles.legendValue, { color: colors.text }]}>3</Text>
          </View>
        </View>
      </View>

      {/* Top Priorities Collapsed Accordion */}
      <View
        style={[
          styles.miniPriorityCard,
          { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
        ]}
      >
        <View style={styles.miniPriorityHeader}>
          <Text style={[styles.miniPriorityTitle, { color: colors.textMuted }]}>
            Top priorities (3)
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textMuted} />
        </View>
      </View>

      {/* 2x2 Launch Tiles Grid */}
      <View style={styles.tileGrid2x2}>
        {/* Row 1: Insights & Inventory */}
        <View style={styles.tileGridRow}>
          {/* Insights (Emerald) */}
          <View
            style={[
              styles.launchTile,
              {
                backgroundColor: colors.surfaceRaised,
                borderColor: withAlpha(colors.primary, '38'),
              },
            ]}
          >
            <MaterialIcons
              name={
                tilePrefs.insights === 'CLASSIC'
                  ? 'dashboard'
                  : tilePrefs.insights === 'BUSINESS'
                  ? 'assessment'
                  : tilePrefs.insights === 'PRODUCT'
                  ? 'lightbulb'
                  : tilePrefs.insights === 'DATA'
                  ? 'insights'
                  : 'analytics'
              }
              size={32}
              color={colors.primary}
            />
            <Text style={[styles.launchTileLabel, { color: colors.text }]}>Insights</Text>
          </View>

          {/* Inventory (Purple) */}
          <View
            style={[
              styles.launchTile,
              {
                backgroundColor: colors.surfaceRaised,
                borderColor: withAlpha('#C084FC', '38'),
              },
            ]}
          >
            <MaterialIcons
              name={
                tilePrefs.inventory === 'CLASSIC'
                  ? 'inventory'
                  : tilePrefs.inventory === 'BUSINESS'
                  ? 'warehouse'
                  : tilePrefs.inventory === 'PRODUCT'
                  ? 'category'
                  : tilePrefs.inventory === 'DATA'
                  ? 'all-inbox'
                  : 'inventory-2'
              }
              size={32}
              color="#C084FC"
            />
            <Text style={[styles.launchTileLabel, { color: colors.text }]}>Inventory</Text>
          </View>
        </View>

        {/* Row 2: Price Movement & Quick Compare */}
        <View style={styles.tileGridRow}>
          {/* Price Movement (Indigo) */}
          <View
            style={[
              styles.launchTile,
              {
                backgroundColor: colors.surfaceRaised,
                borderColor: withAlpha('#818CF8', '38'),
              },
            ]}
          >
            <MaterialIcons
              name={
                tilePrefs.priceMovement === 'CLASSIC'
                  ? 'show-chart'
                  : tilePrefs.priceMovement === 'BUSINESS'
                  ? 'timeline'
                  : tilePrefs.priceMovement === 'PRODUCT'
                  ? 'trending-up'
                  : tilePrefs.priceMovement === 'DATA'
                  ? 'stacked-line-chart'
                  : 'query-stats'
              }
              size={32}
              color="#818CF8"
            />
            <Text style={[styles.launchTileLabel, { color: colors.text }]}>Price Movement</Text>
          </View>

          {/* Quick Compare (Amber) */}
          <View
            style={[
              styles.launchTile,
              {
                backgroundColor: colors.surfaceRaised,
                borderColor: withAlpha('#FBBF24', '38'),
              },
            ]}
          >
            <MaterialIcons
              name={
                tilePrefs.quickCompare === 'CLASSIC'
                  ? 'search'
                  : tilePrefs.quickCompare === 'BUSINESS'
                  ? 'compare-arrows'
                  : tilePrefs.quickCompare === 'PRODUCT'
                  ? 'travel-explore'
                  : tilePrefs.quickCompare === 'DATA'
                  ? 'price-check'
                  : 'manage-search'
              }
              size={32}
              color="#FBBF24"
            />
            <Text style={[styles.launchTileLabel, { color: colors.text }]}>Quick Compare</Text>
          </View>
        </View>
      </View>
    </View>
  );
}

function QuickComparePreview({
  customization,
  colors,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
}) {
  const isBold = customization.insightCustomization.priceEmphasis === 'BOLD';
  return (
    <View style={styles.previewContent}>
      {/* Search Header */}
      <View style={[styles.miniSearchBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <Ionicons name="search" size={13} color={colors.textMuted} />
        <Text style={[styles.miniSearchText, { color: colors.textMuted }]}>Search catalog...</Text>
      </View>

      {/* Main Card */}
      <View style={[styles.miniCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View style={[styles.miniImageThumb, { backgroundColor: colors.surfaceRaised }]}>
            <Text style={{ fontSize: 13, fontFamily: type.bold, color: colors.text }}>H</Text>
          </View>
          <View style={{ flex: 1, marginLeft: 8 }}>
            <Text style={[styles.miniProdTitle, { color: colors.text }]} numberOfLines={1}>
              Hawkins Futura 3L Cooker
            </Text>
            <Text style={{ fontSize: 8.5, color: colors.textMuted, marginTop: 1 }}>SHOP PRICE</Text>
            <Text
              style={{
                fontSize: 14,
                fontFamily: isBold ? type.bold : type.semibold,
                color: colors.primary,
                fontWeight: isBold ? '800' : '600',
              }}
            >
              ₹1,850
            </Text>
          </View>
        </View>
        <View
          style={[
            styles.miniStatusBadge,
            { backgroundColor: withAlpha(colors.competitive, '22'), marginTop: 7 },
          ]}
        >
          <Ionicons name="trophy" size={10} color={colors.competitive} />
          <Text
            style={{
              fontSize: 9.5,
              fontFamily: type.bold,
              color: colors.competitive,
              marginLeft: 4,
            }}
          >
            Competitive • ₹49 below Amazon
          </Text>
        </View>
      </View>

      {/* Retailers row */}
      <View style={{ flexDirection: 'row', gap: 6 }}>
        <View
          style={[
            styles.miniRetailerCard,
            { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 },
          ]}
        >
          <Text style={{ fontSize: 8.5, fontFamily: type.bold, color: '#FF9900' }}>Amazon</Text>
          <Text style={{ fontSize: 11.5, fontFamily: type.bold, color: colors.danger, marginTop: 2 }}>
            ₹1,899
          </Text>
        </View>
        <View
          style={[
            styles.miniRetailerCard,
            { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 },
          ]}
        >
          <Text style={{ fontSize: 8.5, fontFamily: type.bold, color: '#2874F0' }}>Flipkart</Text>
          <Text style={{ fontSize: 11.5, fontFamily: type.bold, color: colors.danger, marginTop: 2 }}>
            ₹1,875
          </Text>
        </View>
      </View>
    </View>
  );
}

function ProductDetailsPreview({
  customization,
  colors,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
}) {
  const graphStyle = customization.insightCustomization.historyGraphStyle;
  return (
    <View style={styles.previewContent}>
      <View style={{ flexDirection: 'row', gap: 6 }}>
        <View
          style={[
            styles.miniRetailerCard,
            { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 },
          ]}
        >
          <Text style={{ fontSize: 8.5, fontFamily: type.bold, color: '#FF9900' }}>amazon.in</Text>
          <Text style={{ fontSize: 11.5, fontFamily: type.bold, color: colors.danger, marginTop: 2 }}>
            ▲ ₹1,899
          </Text>
          <Text style={{ fontSize: 7.5, color: colors.textMuted }}>SAVED • HIGHER</Text>
        </View>
        <View
          style={[
            styles.miniRetailerCard,
            { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 },
          ]}
        >
          <Text style={{ fontSize: 8.5, fontFamily: type.bold, color: '#2874F0' }}>Flipkart</Text>
          <Text style={{ fontSize: 11.5, fontFamily: type.bold, color: colors.danger, marginTop: 2 }}>
            ▲ ₹1,875
          </Text>
          <Text style={{ fontSize: 7.5, color: colors.textMuted }}>SAVED • HIGHER</Text>
        </View>
      </View>

      {/* Mini Graph */}
      <View
        style={[
          styles.miniCard,
          { backgroundColor: colors.surface, borderColor: colors.border, marginTop: 6, padding: 8 },
        ]}
      >
        <Text style={{ fontSize: 8.5, fontFamily: type.bold, color: colors.textMuted, marginBottom: 4 }}>
          PRICE HISTORY ({graphStyle})
        </Text>
        <Svg width="100%" height="48" viewBox="0 0 200 48">
          {graphStyle === 'AREA' ? (
            <Path
              d="M 0,38 Q 50,15 100,28 T 200,8 L 200,48 L 0,48 Z"
              fill={withAlpha(colors.primary, '33')}
            />
          ) : null}
          <Path
            d={
              graphStyle === 'STEP'
                ? 'M 0,38 L 50,38 L 50,22 L 110,22 L 110,30 L 160,30 L 160,12 L 200,12'
                : 'M 0,38 Q 50,15 100,28 T 200,8'
            }
            stroke={colors.primary}
            strokeWidth="2.4"
            fill="none"
          />
        </Svg>
      </View>
    </View>
  );
}

function PriceMovementPreview({
  customization,
  colors,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
}) {
  return (
    <View style={styles.previewContent}>
      <View
        style={[
          styles.miniCard,
          { backgroundColor: colors.surface, borderColor: colors.border, padding: 9 },
        ]}
      >
        <Text style={{ fontSize: 10.5, fontFamily: type.bold, color: colors.text }}>Net Market Trend</Text>
        <Text style={{ fontSize: 8.5, color: colors.textMuted, marginTop: 1 }}>Past 30 days price delta</Text>
        <Svg width="100%" height="50" viewBox="0 0 200 50">
          <Path
            d="M 0,26 C 40,42 80,10 120,32 C 160,40 180,14 200,20"
            stroke={colors.accent}
            strokeWidth="2.6"
            fill="none"
          />
        </Svg>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 4 }}>
          <Text style={{ fontSize: 7.5, color: colors.textMuted }}>May 1</Text>
          <Text style={{ fontSize: 7.5, color: colors.competitive, fontFamily: type.bold }}>
            ▲ +1.8% average
          </Text>
          <Text style={{ fontSize: 7.5, color: colors.textMuted }}>Today</Text>
        </View>
      </View>
    </View>
  );
}

function AlertsPreview({
  customization,
  colors,
}: {
  customization: AppCustomization;
  colors: DynamicColors;
}) {
  return (
    <View style={styles.previewContent}>
      <View
        style={[
          styles.miniCard,
          { backgroundColor: colors.surface, borderColor: colors.border, padding: 10 },
        ]}
      >
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View
            style={{
              width: 26,
              height: 26,
              borderRadius: 13,
              backgroundColor: withAlpha(colors.danger, '22'),
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Ionicons name="notifications" size={13} color={colors.danger} />
          </View>
          <View style={{ flex: 1, marginLeft: 8 }}>
            <Text style={{ fontSize: 11, fontFamily: type.bold, color: colors.text }}>
              Price Drop Detected
            </Text>
            <Text style={{ fontSize: 8.5, color: colors.textMuted }}>Prestige Deluxe 3L • Amazon</Text>
          </View>
        </View>
        <Text style={{ fontSize: 9.5, color: colors.textMuted, marginTop: 7, lineHeight: 13 }}>
          Amazon dropped price from ₹2,250 to ₹2,099 (₹151 change).
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    width: '100%',
    alignSelf: 'stretch',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
  },
  deviceFrame: {
    borderRadius: 22,
    borderWidth: 1.5,
    padding: 16,
    overflow: 'hidden',
  },
  screenInner: {
    flex: 1,
  },
  previewContent: {
    gap: 12,
  },
  miniHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingBottom: 4,
  },
  miniHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  miniLogoImage: {
    width: 32,
    height: 32,
    borderRadius: 8,
  },
  brandTitleCol: {
    marginLeft: 8,
    justifyContent: 'center',
  },
  miniTitle: {
    fontSize: 13,
    fontFamily: type.bold,
    letterSpacing: 1.2,
    lineHeight: 15,
  },
  miniSubtitle: {
    fontSize: 8.5,
    fontFamily: type.bold,
    letterSpacing: 1.0,
    marginTop: 1,
  },
  onlineRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 2,
  },
  onlineText: {
    fontSize: 9.5,
  },
  miniGearBtn: {
    width: 36,
    height: 36,
    borderRadius: 10,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  miniCard: {
    borderRadius: 16,
    borderWidth: 1,
    padding: 14,
  },
  miniCardEyebrow: {
    fontSize: 9,
    fontFamily: type.bold,
    letterSpacing: 1.0,
  },
  miniCardTitle: {
    fontSize: 14,
    fontFamily: type.bold,
    marginTop: 4,
  },
  miniTrack: {
    flexDirection: 'row',
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
    marginTop: 10,
  },
  miniMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  legendDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  legendLabel: {
    fontSize: 10.5,
    marginLeft: 5,
  },
  legendValue: {
    fontSize: 11,
    fontFamily: type.bold,
    marginLeft: 4,
  },
  miniPriorityCard: {
    borderRadius: 14,
    borderWidth: 1,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  miniPriorityHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  miniPriorityTitle: {
    fontSize: 11.5,
    fontFamily: type.bold,
  },
  tileGrid2x2: {
    gap: 10,
  },
  tileGridRow: {
    flexDirection: 'row',
    gap: 10,
  },
  launchTile: {
    flex: 1,
    height: 105,
    borderRadius: 18,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 8,
    gap: 8,
  },
  launchTileLabel: {
    fontSize: 11.5,
    fontFamily: type.bold,
    textAlign: 'center',
  },
  miniSearchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: radius.pill,
    borderWidth: 1,
    gap: 5,
  },
  miniSearchText: {
    fontSize: 9,
  },
  miniImageThumb: {
    width: 34,
    height: 34,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  miniProdTitle: {
    fontSize: 10.5,
    fontFamily: type.bold,
  },
  miniStatusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    paddingHorizontal: 6,
    paddingVertical: 2.5,
    borderRadius: radius.pill,
  },
  miniRetailerCard: {
    borderRadius: 9,
    borderWidth: 1,
    padding: 7,
  },
});
