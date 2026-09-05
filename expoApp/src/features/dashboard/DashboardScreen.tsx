import Ionicons from '@expo/vector-icons/Ionicons';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  LayoutAnimation,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  UIManager,
  View,
} from 'react-native';

import type {
  ComparisonOverview,
  InventoryRepository,
  PriorityProductSummary,
} from '../../data/inventoryRepository';
import type { InventoryProduct } from '../../domain/models';
import { formatRupees } from '../../domain/formatting';
import { useCustomization } from '../../theme/CustomizationContext';
import { withAlpha } from '../../theme/dynamicTheme';
import { radius, spacing, type } from '../../theme/tokens';
import { resolveLaunchTileIcons } from './launchTileIcons';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

export function DashboardScreen({
  overview,
  repository,
  onOpenInventory,
  onOpenCompare,
  onOpenTools,
  onOpenPriceMovement,
  onOpenInsights,
  onSelectProduct,
}: {
  overview: ComparisonOverview;
  repository?: InventoryRepository;
  onOpenInventory: () => void;
  onOpenCompare: () => void;
  onOpenTools: () => void;
  onOpenPriceMovement: () => void;
  onOpenInsights: (filter?: 'COMPETITIVE' | 'REVIEW') => void;
  onSelectProduct?: (product: InventoryProduct) => void;
}) {
  const { customization, colors } = useCustomization();
  const [priorities, setPriorities] = useState<PriorityProductSummary[]>([]);
  const startExpanded =
    customization.insightCustomization?.prioritiesStartState === 'EXPANDED';
  const [prioritiesExpanded, setPrioritiesExpanded] = useState(startExpanded);
  const chevronAnim = useRef(new Animated.Value(startExpanded ? 1 : 0)).current;

  const reduceMotion = customization.motionPreference === 'REDUCED';
  const tileIcons = useMemo(
    () => resolveLaunchTileIcons(customization.launchTileIconPreferences),
    [customization.launchTileIconPreferences],
  );

  const priorityLimit = customization.insightCustomization?.priorityProductLimit || 3;
  const prioritySortMode = customization.insightCustomization?.prioritySortMode || 'RUPEE_GAP';
  const priorityRowStyle = customization.insightCustomization?.priorityRowStyle || 'DETAILED';

  useEffect(() => {
    if (!repository) return;
    let active = true;
    repository
      .getTopPriorityProducts(priorityLimit, prioritySortMode)
      .then((items) => {
        if (active) setPriorities(items);
      })
      .catch(() => {
        if (active) setPriorities([]);
      });
    return () => {
      active = false;
    };
  }, [repository, overview.productCount, priorityLimit, prioritySortMode]);

  const togglePriorities = useCallback(() => {
    if (reduceMotion) {
      setPrioritiesExpanded((v) => !v);
      return;
    }
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setPrioritiesExpanded((prev) => {
      const next = !prev;
      Animated.timing(chevronAnim, {
        toValue: next ? 1 : 0,
        duration: 200,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start();
      return next;
    });
  }, [chevronAnim, reduceMotion]);

  const total = Math.max(1, overview.productCount);
  const competitiveCount = overview.competitiveCount;
  const reviewCount = overview.reviewCount;
  const uncheckedCount = overview.uncheckedCount;

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.contentContainer}
      showsVerticalScrollIndicator={false}
    >
      {/* ── Brand Header ── */}
      <View style={styles.brandRow}>
        <Image
          source={require('../../../assets/brand/app_logo.png')}
          style={styles.logo}
          contentFit="contain"
        />
        <View style={styles.brandText}>
          <Text style={[styles.brandTitle, { color: colors.primary }]}>SUPREME</Text>
          <Text style={[styles.brandSubtitle, { color: colors.text }]}>PRICE INTELLIGENCE</Text>
          <View style={styles.onlineRow}>
            <MaterialIcons name="wifi" size={13} color={colors.competitive} />
            <Text style={[styles.onlineText, { color: colors.textMuted }]}>Online</Text>
          </View>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Open settings and customization tools"
          onPress={onOpenTools}
          style={({ pressed }) => [
            styles.settingsButton,
            { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
            pressed && styles.pressed,
          ]}
        >
          <MaterialIcons name="settings" size={24} color={colors.text} />
        </Pressable>
      </View>

      {/* ── Shop Overview Decision Meter Card ── */}
      <View
        style={[
          styles.overviewCard,
          { backgroundColor: colors.surface, borderColor: colors.border },
        ]}
      >
        <Text style={[styles.eyebrow, { color: colors.primary }]}>SHOP OVERVIEW</Text>
        <Text style={[styles.overviewTitle, { color: colors.text }]}>
          {overview.productCount} {overview.productCount === 1 ? 'product' : 'products'} compared
        </Text>

        {/* Proportional Segment Track */}
        <View style={[styles.progressTrack, { backgroundColor: colors.surfaceRaised }]}>
          {competitiveCount > 0 ? (
            <View
              style={[
                styles.progressSegment,
                {
                  flex: competitiveCount,
                  backgroundColor: colors.competitive,
                  borderTopLeftRadius: radius.pill,
                  borderBottomLeftRadius: radius.pill,
                  borderTopRightRadius: reviewCount === 0 && uncheckedCount === 0 ? radius.pill : 0,
                  borderBottomRightRadius: reviewCount === 0 && uncheckedCount === 0 ? radius.pill : 0,
                },
              ]}
            />
          ) : null}
          {reviewCount > 0 ? (
            <View
              style={[
                styles.progressSegment,
                {
                  flex: reviewCount,
                  backgroundColor: colors.danger,
                  marginLeft: competitiveCount > 0 ? 2 : 0,
                  borderTopLeftRadius: competitiveCount === 0 ? radius.pill : 0,
                  borderBottomLeftRadius: competitiveCount === 0 ? radius.pill : 0,
                  borderTopRightRadius: uncheckedCount === 0 ? radius.pill : 0,
                  borderBottomRightRadius: uncheckedCount === 0 ? radius.pill : 0,
                },
              ]}
            />
          ) : null}
          {uncheckedCount > 0 ? (
            <View
              style={[
                styles.progressSegment,
                {
                  flex: uncheckedCount,
                  backgroundColor: withAlpha(colors.textMuted, '40'),
                  marginLeft: competitiveCount > 0 || reviewCount > 0 ? 2 : 0,
                  borderTopRightRadius: radius.pill,
                  borderBottomRightRadius: radius.pill,
                  borderTopLeftRadius: competitiveCount === 0 && reviewCount === 0 ? radius.pill : 0,
                  borderBottomLeftRadius: competitiveCount === 0 && reviewCount === 0 ? radius.pill : 0,
                },
              ]}
            />
          ) : null}
        </View>

        {/* Clickable Legend Pills */}
        <View style={styles.overviewMeta}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Filter by competitive products (${competitiveCount})`}
            onPress={() => onOpenInsights('COMPETITIVE')}
            style={({ pressed }) => [styles.metaItem, pressed && styles.pressed]}
          >
            <View style={[styles.metaDot, { backgroundColor: colors.competitive }]} />
            <Text style={[styles.metaText, { color: colors.textMuted }]}>
              Competitive <Text style={{ color: colors.text, fontFamily: type.bold }}>{competitiveCount}</Text>
            </Text>
          </Pressable>

          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Filter by review products (${reviewCount})`}
            onPress={() => onOpenInsights('REVIEW')}
            style={({ pressed }) => [styles.metaItem, pressed && styles.pressed]}
          >
            <View style={[styles.metaDot, { backgroundColor: colors.danger }]} />
            <Text style={[styles.metaText, { color: colors.textMuted }]}>
              Review <Text style={{ color: colors.text, fontFamily: type.bold }}>{reviewCount}</Text>
            </Text>
          </Pressable>
        </View>

        {uncheckedCount > 0 ? (
          <Text style={[styles.uncheckedText, { color: colors.textMuted }]}>
            {uncheckedCount} awaiting a first retailer check
          </Text>
        ) : null}
      </View>

      {/* ── Top Priorities (3) Collapsible Accordion ── */}
      {priorities.length > 0 ? (
        <View
          style={[
            styles.priorityCard,
            { backgroundColor: colors.surface, borderColor: colors.border },
          ]}
        >
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Toggle top priorities (${priorities.length})`}
            onPress={togglePriorities}
            style={styles.priorityHeader}
          >
            <View style={styles.priorityHeaderLeft}>
              <MaterialIcons name="priority-high" size={18} color={colors.danger} />
              <Text style={[styles.priorityHeaderTitle, { color: colors.text }]}>
                Top priorities ({priorities.length})
              </Text>
            </View>
            <Animated.View
              style={{
                transform: [
                  {
                    rotate: chevronAnim.interpolate({
                      inputRange: [0, 1],
                      outputRange: ['0deg', '180deg'],
                    }),
                  },
                ],
              }}
            >
              <Ionicons name="chevron-down" size={18} color={colors.textMuted} />
            </Animated.View>
          </Pressable>

          {prioritiesExpanded ? (
            <View style={styles.priorityList}>
              {priorities.map((item, index) => {
                const isRisk = item.marginRisk;
                return (
                  <Pressable
                    key={item.id}
                    accessibilityRole="button"
                    accessibilityLabel={`Product: ${item.productName}, gap ${formatRupees(item.gap)}`}
                    onPress={() => onSelectProduct?.(item.item)}
                    style={({ pressed }) => [
                      styles.priorityRow,
                      {
                        backgroundColor: colors.surfaceRaised,
                        borderColor: withAlpha(colors.danger, '35'),
                      },
                      pressed && styles.pressed,
                    ]}
                  >
                    {priorityRowStyle === 'COMPACT' ? (
                      <View style={styles.priorityCompactRow}>
                        <View style={styles.priorityCompactLeft}>
                          <View style={[styles.priorityBadge, { backgroundColor: withAlpha(isRisk ? colors.danger : colors.competitive, '22') }]}>
                            <Text
                              style={[
                                styles.priorityBadgeText,
                                { color: isRisk ? colors.danger : colors.competitive },
                              ]}
                            >
                              {isRisk ? 'RISK' : `#${index + 1}`}
                            </Text>
                          </View>
                          <Text style={[styles.priorityNameCompact, { color: colors.text }]} numberOfLines={1}>
                            {item.productName}
                          </Text>
                        </View>
                        <View style={styles.priorityCompactRight}>
                          <Text style={[styles.priorityCompactPrice, { color: colors.text }]}>
                            {formatRupees(item.shopPrice)} <Text style={{ color: colors.danger, fontSize: 10 }}>vs {formatRupees(item.onlinePrice)}</Text>
                          </Text>
                          <Text style={[styles.priorityCompactGap, { color: colors.danger }]}>
                            -{formatRupees(item.gap)}
                          </Text>
                        </View>
                      </View>
                    ) : (
                      <>
                        <View style={styles.priorityRowHeader}>
                          <View style={styles.priorityBadge}>
                            <Text
                              style={[
                                styles.priorityBadgeText,
                                { color: isRisk ? colors.danger : colors.competitive },
                              ]}
                            >
                              {isRisk ? 'MARGIN RISK' : `#${index + 1} PRIORITY`}
                            </Text>
                          </View>
                          <Text style={[styles.priorityGap, { color: colors.danger }]}>
                            -{formatRupees(item.gap)} gap
                          </Text>
                        </View>

                        <Text style={[styles.priorityName, { color: colors.text }]} numberOfLines={2}>
                          {item.productName}
                        </Text>

                        <View style={styles.priorityPriceRow}>
                          <View style={[styles.pricePill, { backgroundColor: colors.isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)' }]}>
                            <Text style={[styles.pricePillLabel, { color: colors.textMuted }]}>Shop</Text>
                            <Text style={[styles.pricePillVal, { color: colors.text }]}>
                              {formatRupees(item.shopPrice)}
                            </Text>
                          </View>
                          <View style={[styles.pricePill, { backgroundColor: colors.isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)' }]}>
                            <Text style={[styles.pricePillLabel, { color: colors.textMuted }]}>
                              {item.onlineRetailer}
                            </Text>
                            <Text style={[styles.pricePillVal, { color: colors.danger }]}>
                              {formatRupees(item.onlinePrice)}
                            </Text>
                          </View>
                          {item.purchaseCost != null ? (
                            <View style={[styles.pricePill, { backgroundColor: colors.isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)' }]}>
                              <Text style={[styles.pricePillLabel, { color: colors.textMuted }]}>Cost</Text>
                              <Text style={[styles.pricePillVal, { color: colors.textMuted }]}>
                                {formatRupees(item.purchaseCost)}
                              </Text>
                            </View>
                          ) : null}
                        </View>
                      </>
                    )}
                  </Pressable>
                );
              })}
            </View>
          ) : null}
        </View>
      ) : null}

      {/* ── 2x2 Launch Destination Tiles with Spring Physics ── */}
      <View style={styles.tileGrid}>
        <View style={styles.tileRow}>
          <LaunchTile
            title="Insights"
            icon={tileIcons.insights}
            accent={colors.primary}
            textColor={colors.text}
            onPress={() => onOpenInsights()}
            reduceMotion={reduceMotion}
            panelColor={colors.surface}
          />
          <LaunchTile
            title="Inventory"
            icon={tileIcons.inventory}
            accent="#C084FC"
            textColor={colors.text}
            onPress={onOpenInventory}
            reduceMotion={reduceMotion}
            panelColor={colors.surface}
          />
        </View>
        <View style={styles.tileRow}>
          <LaunchTile
            title="Price Movement"
            icon={tileIcons.priceMovement}
            accent={colors.accent || '#10B981'}
            textColor={colors.text}
            onPress={onOpenPriceMovement}
            reduceMotion={reduceMotion}
            panelColor={colors.surface}
          />
          <LaunchTile
            title="Quick Compare"
            icon={tileIcons.quickCompare}
            accent={colors.warning}
            textColor={colors.text}
            onPress={onOpenCompare}
            reduceMotion={reduceMotion}
            panelColor={colors.surface}
          />
        </View>
      </View>
    </ScrollView>
  );
}

function LaunchTile({
  title,
  icon,
  accent,
  textColor,
  onPress,
  reduceMotion,
  panelColor,
}: {
  title: string;
  icon: React.ComponentProps<typeof MaterialIcons>['name'];
  accent: string;
  textColor?: string;
  onPress: () => void;
  reduceMotion: boolean;
  panelColor: string;
}) {
  const pressAnim = useRef(new Animated.Value(0)).current;

  const handlePressIn = () => {
    if (reduceMotion) return;
    Animated.timing(pressAnim, {
      toValue: 1,
      duration: 65,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start();
  };

  const handlePressOut = () => {
    if (reduceMotion) return;
    Animated.spring(pressAnim, {
      toValue: 0,
      damping: 14,
      stiffness: 240,
      useNativeDriver: true,
    }).start();
  };

  const tileScale = pressAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [1, 0.965],
  });

  const iconScale = pressAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [1, 1.06],
  });

  const gradStart = withAlpha(accent, '2A');
  const gradMid = withAlpha(accent, '0C');
  const gradEnd = withAlpha(panelColor, '00');
  const borderColor = withAlpha(accent, '50');

  return (
    <Animated.View style={[styles.tileWrapper, { transform: [{ scale: tileScale }] }]}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Navigate to ${title}`}
        onPress={onPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        style={[styles.tilePressable, { borderColor, backgroundColor: panelColor }]}
      >
        <LinearGradient
          colors={[gradStart, gradMid, gradEnd]}
          start={{ x: 0.5, y: 0 }}
          end={{ x: 0.5, y: 1 }}
          style={styles.tileGradient}
        >
          <Animated.View style={{ transform: [{ scale: iconScale }] }}>
            <MaterialIcons name={icon} size={46} color={accent} />
          </Animated.View>
          <Text style={[styles.tileTitle, textColor ? { color: textColor } : null]} numberOfLines={2}>
            {title}
          </Text>
        </LinearGradient>
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  contentContainer: {
    padding: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: 36,
  },
  brandRow: {
    minHeight: 60,
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
  },
  logo: { width: 50, height: 50 },
  brandText: { flex: 1, marginLeft: 10 },
  brandTitle: {
    fontFamily: type.bold,
    fontSize: 22,
    lineHeight: 24,
    letterSpacing: 0.8,
  },
  brandSubtitle: {
    fontFamily: type.semibold,
    fontSize: 11,
    letterSpacing: 0.75,
  },
  onlineRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 5,
  },
  onlineText: {
    fontFamily: type.regular,
    fontSize: 10,
    marginLeft: 5,
  },
  settingsButton: {
    width: 46,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 15,
    borderWidth: 1,
  },
  overviewCard: {
    marginTop: spacing.md,
    borderWidth: 1,
    borderRadius: 18,
    padding: spacing.lg,
  },
  eyebrow: {
    fontFamily: type.bold,
    fontSize: 10,
    letterSpacing: 1,
  },
  overviewTitle: {
    fontFamily: type.bold,
    fontSize: 17,
    marginTop: 2,
  },
  progressTrack: {
    height: 11,
    flexDirection: 'row',
    overflow: 'hidden',
    borderRadius: radius.pill,
    marginTop: 14,
  },
  progressSegment: { height: '100%' },
  overviewMeta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
  },
  metaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 2,
    paddingHorizontal: 4,
  },
  metaDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: spacing.sm,
  },
  metaText: {
    fontFamily: type.regular,
    fontSize: 12,
  },
  uncheckedText: {
    fontFamily: type.regular,
    fontSize: 11,
    marginTop: spacing.sm,
  },
  priorityCard: {
    marginTop: spacing.md,
    borderWidth: 1,
    borderRadius: 18,
    overflow: 'hidden',
  },
  priorityHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.lg,
  },
  priorityHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  priorityHeaderTitle: {
    fontFamily: type.bold,
    fontSize: 14,
  },
  priorityList: {
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.lg,
    gap: spacing.sm,
  },
  priorityRow: {
    borderWidth: 1,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  priorityCompactRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.sm,
  },
  priorityCompactLeft: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    minWidth: 0,
  },
  priorityNameCompact: {
    flex: 1,
    fontFamily: type.semibold,
    fontSize: 12,
  },
  priorityCompactRight: {
    alignItems: 'flex-end',
  },
  priorityCompactPrice: {
    fontFamily: type.bold,
    fontSize: 11,
  },
  priorityCompactGap: {
    fontFamily: type.bold,
    fontSize: 10,
  },
  priorityRowHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  priorityBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  priorityBadgeText: {
    fontFamily: type.bold,
    fontSize: 9,
    letterSpacing: 0.8,
  },
  priorityGap: {
    fontFamily: type.bold,
    fontSize: 12,
  },
  priorityName: {
    fontFamily: type.semibold,
    fontSize: 13,
    lineHeight: 18,
  },
  priorityPriceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.sm,
  },
  pricePill: {
    backgroundColor: 'rgba(255,255,255,0.04)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  pricePillLabel: {
    fontSize: 9,
    fontFamily: type.regular,
  },
  pricePillVal: {
    fontSize: 11,
    fontFamily: type.bold,
  },
  tileGrid: {
    marginTop: spacing.md,
    gap: 12,
  },
  tileRow: {
    flexDirection: 'row',
    gap: 12,
  },
  tileWrapper: {
    flex: 1,
    minHeight: 174,
  },
  tilePressable: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 26,
    overflow: 'hidden',
  },
  tileGradient: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 14,
  },
  tileTitle: {
    color: '#FFFFFF',
    fontFamily: type.bold,
    fontSize: 15,
    lineHeight: 19,
    textAlign: 'center',
    marginTop: 16,
    width: '100%',
  },
  pressed: { opacity: 0.75 },
});
