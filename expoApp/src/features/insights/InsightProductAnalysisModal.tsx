import Ionicons from '@expo/vector-icons/Ionicons';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Dimensions,
  Image,
  LayoutChangeEvent,
  Modal,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import Svg, {
  Circle,
  Defs,
  Line,
  LinearGradient as SvgLinearGradient,
  Path,
  Stop,
} from 'react-native-svg';

import { InsightGroup, InsightProduct } from '../../domain/insights';
import {
  AnalysisTone,
  buildAnalysisMessage,
  lowestOnline,
  summarizePriceHistory,
} from '../../domain/insightAnalysis';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { formatRelativeTime, formatRupees } from '../../domain/formatting';
import { PriceHistoryEntry, PriceRetailer } from '../../domain/models';
import { LinearGradient } from 'expo-linear-gradient';
import { useNetworkThroughput } from '../../hooks/useNetworkThroughput';
import { useCustomization } from '../../theme/CustomizationContext';

import { DynamicColors } from '../../theme/dynamicTheme';

function getAnalysisTokens(colors: DynamicColors) {
  return {
    primary: colors.primary,
    warning: colors.warning,
    danger: colors.danger,
    amazon: '#FF9900',
    flipkart: '#2874F0',
    panel: colors.surface,
    panelStrong: colors.surfaceRaised,
    border: colors.border,
    text: colors.text,
    textMuted: colors.textMuted,
    gridLine: colors.isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(148, 163, 184, 0.28)',
    isDark: colors.isDark,
  };
}
type AnalysisTokens = ReturnType<typeof getAnalysisTokens>;

interface SelectedHistoryPoint {
  retailer: 'Amazon' | 'Flipkart';
  price: number;
  previousPrice: number | null;
  checkedAt: number;
  color: string;
}

export interface InsightProductAnalysisModalProps {
  product: InsightProduct;
  group: InsightGroup | null;
  brand: string | null;
  groupSize: number;
  repository: any;
  showBanner: (msg: string, tone?: 'info' | 'success' | 'error') => void;
  onBack?: () => void;
  onClose: () => void;
  isEmbedded?: boolean;
}

function MetricTile({
  label,
  value,
  valueColor,
  tokens,
  styles,
  style,
}: {
  label: string;
  value: string;
  valueColor?: string;
  tokens: AnalysisTokens;
  styles: any;
  style?: any;
}) {
  return (
    <View style={[styles.metricTile, style]}>
      <Text style={styles.metricLabel} numberOfLines={1}>
        {label}
      </Text>
      <Text style={[styles.metricValue, { color: valueColor ?? tokens.text }]} numberOfLines={1}>
        {value}
      </Text>
    </View>
  );
}

function AnalysisSection({
  title,
  children,
  styles,
}: {
  title: string;
  children: React.ReactNode;
  styles: any;
}) {
  return (
    <View style={styles.sectionCard}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={{ height: 10 }} />
      {children}
    </View>
  );
}

function ProductAnalysisLineChart({
  entries,
  tokens,
  styles,
  height = 150,
}: {
  entries: PriceHistoryEntry[];
  tokens: AnalysisTokens;
  styles: any;
  height?: number;
}) {
  const [canvasWidth, setCanvasWidth] = useState(0);
  const [selectedPoint, setSelectedPoint] = useState<SelectedHistoryPoint | null>(null);

  const series = useMemo(() => {
    const list: Array<{
      retailer: 'Amazon' | 'Flipkart';
      color: string;
      points: Array<{ price: number; checkedAt: number }>;
    }> = [];

    const azPoints = entries
      .filter((e) => e.retailer === 'AMAZON' && Number.isFinite(e.price) && e.price > 0)
      .sort((a, b) => a.checkedAt - b.checkedAt);
    if (azPoints.length > 0) {
      list.push({ retailer: 'Amazon', color: tokens.amazon, points: azPoints });
    }

    const fkPoints = entries
      .filter((e) => e.retailer === 'FLIPKART' && Number.isFinite(e.price) && e.price > 0)
      .sort((a, b) => a.checkedAt - b.checkedAt);
    if (fkPoints.length > 0) {
      list.push({ retailer: 'Flipkart', color: tokens.flipkart, points: fkPoints });
    }

    return list;
  }, [entries]);

  const allPoints = useMemo(() => series.flatMap((s) => s.points), [series]);
  if (allPoints.length === 0) return null;

  const minPrice = Math.min(...allPoints.map((p) => p.price));
  const maxPrice = Math.max(...allPoints.map((p) => p.price));
  const midPrice = minPrice + (maxPrice - minPrice) / 2;

  const minTime = Math.min(...allPoints.map((p) => p.checkedAt));
  const maxTime = Math.max(...allPoints.map((p) => p.checkedAt));
  const timeRange = Math.max(1, maxTime - minTime);
  const priceRange = Math.max(1, maxPrice - minPrice);

  const horizontalPadding = 10;
  const verticalPadding = 12;
  const chartW = Math.max(1, canvasWidth - horizontalPadding * 2);
  const chartH = Math.max(1, height - verticalPadding * 2);
  const chartBottom = height - verticalPadding;

  const getOffset = (p: { price: number; checkedAt: number }) => {
    const x =
      minTime === maxTime
        ? horizontalPadding + chartW / 2
        : horizontalPadding + chartW * ((p.checkedAt - minTime) / timeRange);

    const normPrice = (p.price - minPrice) / priceRange;
    const y =
      maxPrice - minPrice <= 0.01
        ? verticalPadding + chartH / 2
        : verticalPadding + chartH * (1 - normPrice);

    return { x, y };
  };

  const handleTap = (evt: any) => {
    const touchX = evt.nativeEvent?.locationX ?? 0;
    const touchY = evt.nativeEvent?.locationY ?? 0;

    let nearest: SelectedHistoryPoint | null = null;
    let minDistanceSq = 32 * 32;

    series.forEach((s) => {
      s.points.forEach((pt, idx) => {
        const offset = getOffset(pt);
        const dx = offset.x - touchX;
        const dy = offset.y - touchY;
        const distSq = dx * dx + dy * dy;
        if (distSq <= minDistanceSq) {
          minDistanceSq = distSq;
          nearest = {
            retailer: s.retailer,
            price: pt.price,
            previousPrice: idx > 0 ? s.points[idx - 1].price : null,
            checkedAt: pt.checkedAt,
            color: s.color,
          };
        }
      });
    });

    setSelectedPoint(nearest);
  };

  return (
    <View style={styles.chartWrapper}>
      {/* Legend */}
      <View style={styles.chartLegend}>
        {series.map((s) => (
          <View key={s.retailer} style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: s.color }]} />
            <Text style={styles.legendLabel}>{s.retailer}</Text>
          </View>
        ))}
      </View>

      {/* Selected Point Tooltip Callout */}
      {selectedPoint && (
        <View style={[styles.tooltipCard, { borderColor: `${selectedPoint.color}80` }]}>
          <View style={styles.tooltipHeader}>
            <View style={[styles.legendDot, { backgroundColor: selectedPoint.color }]} />
            <Text style={[styles.tooltipRetailer, { color: selectedPoint.color }]}>
              {selectedPoint.retailer}
            </Text>
            <Text style={styles.tooltipTime}>
              {formatRelativeTime(selectedPoint.checkedAt)}
            </Text>
          </View>
          <View style={styles.tooltipBody}>
            <Text style={styles.tooltipPrice}>{formatRupees(selectedPoint.price)}</Text>
            {selectedPoint.previousPrice != null && (
              <Text
                style={[
                  styles.tooltipChange,
                  {
                    color:
                      selectedPoint.price < selectedPoint.previousPrice
                        ? tokens.danger
                        : selectedPoint.price > selectedPoint.previousPrice
                        ? tokens.primary
                        : tokens.textMuted,
                  },
                ]}
              >
                {selectedPoint.price < selectedPoint.previousPrice
                  ? `▼ -${formatRupees(selectedPoint.previousPrice - selectedPoint.price)}`
                  : selectedPoint.price > selectedPoint.previousPrice
                  ? `▲ +${formatRupees(selectedPoint.price - selectedPoint.previousPrice)}`
                  : '— unchanged'}
              </Text>
            )}
          </View>
        </View>
      )}

      {/* Chart Canvas */}
      <View style={[styles.chartRow, { height }]}>
        {/* Y Axis */}
        <View style={styles.yAxisCol}>
          <Text style={styles.axisLabel}>{formatRupees(maxPrice)}</Text>
          {maxPrice - minPrice > 0.01 && (
            <Text style={styles.axisLabel}>{formatRupees(midPrice)}</Text>
          )}
          <Text style={styles.axisLabel}>{formatRupees(minPrice)}</Text>
        </View>

        {/* SVG Area */}
        <Pressable
          style={styles.svgContainer}
          onLayout={(e: LayoutChangeEvent) => {
            const w = e.nativeEvent.layout.width;
            if (w > 0) setCanvasWidth(w);
          }}
          onPress={handleTap}
        >
          {canvasWidth > 0 && (
            <Svg width={canvasWidth} height={height}>
              <Defs>
                {series.map((s) => (
                  <SvgLinearGradient
                    key={`grad_${s.retailer}`}
                    id={`grad_${s.retailer}`}
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <Stop offset="0%" stopColor={s.color} stopOpacity="0.25" />
                    <Stop offset="90%" stopColor={s.color} stopOpacity="0.02" />
                    <Stop offset="100%" stopColor={s.color} stopOpacity="0" />
                  </SvgLinearGradient>
                ))}
              </Defs>

              {/* 3 Grid lines */}
              {[0, 1, 2].map((i) => {
                const y = verticalPadding + (chartH * i) / 2;
                return (
                  <Line
                    key={`grid_${i}`}
                    x1={horizontalPadding}
                    y1={y}
                    x2={canvasWidth - horizontalPadding}
                    y2={y}
                    stroke={tokens.gridLine}
                    strokeWidth={1}
                    strokeDasharray="4, 4"
                  />
                );
              })}

              {/* Series Curves and Area */}
              {series.map((s) => {
                const offsets = s.points.map(getOffset);
                if (offsets.length === 0) return null;

                let lineD = `M ${offsets[0].x.toFixed(1)} ${offsets[0].y.toFixed(1)}`;
                for (let i = 1; i < offsets.length; i++) {
                  const prev = offsets[i - 1];
                  const curr = offsets[i];
                  const midX = ((prev.x + curr.x) / 2).toFixed(1);
                  lineD += ` C ${midX} ${prev.y.toFixed(1)}, ${midX} ${curr.y.toFixed(1)}, ${curr.x.toFixed(1)} ${curr.y.toFixed(1)}`;
                }

                const areaD = `${lineD} L ${offsets[offsets.length - 1].x.toFixed(1)} ${chartBottom} L ${offsets[0].x.toFixed(1)} ${chartBottom} Z`;

                return (
                  <React.Fragment key={`series_${s.retailer}`}>
                    <Path d={areaD} fill={`url(#grad_${s.retailer})`} />
                    {offsets.length > 1 && (
                      <>
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={8}
                          strokeOpacity={0.12}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={4}
                          strokeOpacity={0.3}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={2}
                          strokeOpacity={1.0}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </>
                    )}
                    {offsets.map((pt, pIdx) => (
                      <Circle
                        key={`pt_${s.retailer}_${pIdx}`}
                        cx={pt.x}
                        cy={pt.y}
                        r={s.points.length === 1 ? 5 : 3.5}
                        fill={s.color}
                        stroke={tokens.panelStrong}
                        strokeWidth={1.5}
                      />
                    ))}
                  </React.Fragment>
                );
              })}
            </Svg>
          )}
        </Pressable>
      </View>
    </View>
  );
}

export function InsightProductAnalysisModal({
  product: initialProduct,
  group,
  brand,
  groupSize,
  repository,
  showBanner,
  onBack,
  onClose,
  isEmbedded = false,
}: InsightProductAnalysisModalProps) {
  const [product, setProduct] = useState<InsightProduct>(initialProduct);
  const [history, setHistory] = useState<PriceHistoryEntry[]>([]);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { colors, customization } = useCustomization();
  const tokens = useMemo(() => getAnalysisTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);
  const throughput = useNetworkThroughput(isRefreshing);

  // Spotlight animation value (from -0.18 to 1.18)
  const spotlightAnim = useRef(new Animated.Value(-0.18)).current;
  const spotlightLoop = useRef<Animated.CompositeAnimation | null>(null);

  useEffect(() => {
    setProduct(initialProduct);
  }, [initialProduct]);

  // Load price history
  const loadHistory = async () => {
    setIsHistoryLoading(true);
    try {
      const hist = await repository.getPriceHistory(product.item.id);
      setHistory(hist);
    } catch (e) {
      console.error(e);
    } finally {
      setIsHistoryLoading(false);
    }
  };

  useEffect(() => {
    loadHistory();
  }, [product.item.id]);

  // Handle Spotlight animation loop
  useEffect(() => {
    if (isRefreshing) {
      if (customization.motionPreference === 'REDUCED') {
        spotlightAnim.setValue(0.12);
        return;
      }
      spotlightLoop.current = Animated.loop(
        Animated.sequence([
          Animated.timing(spotlightAnim, {
            toValue: 1.18,
            duration: 1500,
            useNativeDriver: true,
          }),
          Animated.timing(spotlightAnim, {
            toValue: -0.18,
            duration: 1500,
            useNativeDriver: true,
          }),
        ]),
      );
      spotlightLoop.current.start();
    } else {
      spotlightLoop.current?.stop();
      spotlightAnim.setValue(-0.18);
    }
    return () => spotlightLoop.current?.stop();
  }, [customization.motionPreference, isRefreshing, spotlightAnim]);


  const analysis = useMemo(
    () => buildAnalysisMessage(product, group, brand, groupSize),
    [product, group, brand, groupSize],
  );

  const getToneColor = (tone: AnalysisTone) => {
    switch (tone) {
      case 'GOOD':
        return tokens.primary;
      case 'BAD':
        return tokens.danger;
      case 'WARNING':
        return tokens.warning;
      case 'NEUTRAL':
      default:
        return tokens.primary;
    }
  };
  const toneColor = getToneColor(analysis.tone);

  const item = product.item;
  const online = lowestOnline(item);
  const difference = online ? item.shopPrice - online.price : null;
  const percentDiff =
    difference != null && item.shopPrice > 0
      ? (Math.abs(difference) / item.shopPrice) * 100
      : null;

  // Margin calculation
  const cost = item.purchaseCost && item.purchaseCost > 0 ? item.purchaseCost : null;
  const profit = cost != null && item.shopPrice > 0 ? item.shopPrice - cost : null;
  const marginPercent = profit != null && item.shopPrice > 0 ? (profit / item.shopPrice) * 100 : null;

  // Confidence calculation
  const linkedCount = [item.amazonUrl, item.flipkartUrl].filter((u) => !!u?.trim()).length;
  const freshCount = [product.amazonFresh, product.flipkartFresh].filter(Boolean).length;
  const confidence =
    linkedCount === 2 && freshCount === 2
      ? 'High confidence'
      : (item.amazonLastPrice ?? 0) > 0 || (item.flipkartLastPrice ?? 0) > 0
      ? 'Medium confidence'
      : 'Low confidence';

  const confidenceColor =
    confidence === 'High confidence'
      ? tokens.primary
      : confidence === 'Medium confidence'
      ? tokens.warning
      : tokens.danger;

  const confidenceDesc =
    confidence === 'High confidence'
      ? 'Both retailers have fresh saved prices.'
      : confidence === 'Medium confidence'
      ? 'Some usable evidence exists, but it is incomplete or due for checking.'
      : 'There is not enough retailer data for a reliable comparison.';

  const hasRetailerLink = !!item.amazonUrl?.trim() || !!item.flipkartUrl?.trim();

  const handleRefresh = async () => {
    if (isRefreshing || !hasRetailerLink) return;
    setIsRefreshing(true);
    try {
      const result = await repository.refreshProductPrices(product.item.id);
      if (result.success) {
        showBanner('Price refreshed successfully', 'success');
        const updated = await repository.getProduct(product.item.id);
        if (updated) {
          setProduct((prev) => ({
            ...prev,
            item: updated,
            amazonAlert: (updated.amazonLastPrice ?? 0) > 0 && updated.shopPrice > (updated.amazonLastPrice ?? 0),
            flipkartAlert: (updated.flipkartLastPrice ?? 0) > 0 && updated.shopPrice > (updated.flipkartLastPrice ?? 0),
          }));
        }
        await loadHistory();
      } else {
        showBanner(result.message || 'Failed to refresh prices', 'error');
      }
    } catch (err: any) {
      showBanner(err.message || 'Network error during price refresh', 'error');
    } finally {
      setIsRefreshing(false);
    }
  };

  // Interpolate spotlight translation
  const dialogHeight = Dimensions.get('window').height * 0.9;
  const translateY = spotlightAnim.interpolate({
    inputRange: [-0.18, 1.18],
    outputRange: [-dialogHeight * 0.18, dialogHeight * 1.18],
  });

  const content = (
    <View style={styles.sheet}>
      {/* Animated Gold Spotlight Scan Bar */}
      {isRefreshing && (
        <Animated.View
          pointerEvents="none"
          style={[
            styles.spotlightSweep,
            {
              transform: [{ translateY }],
            },
          ]}
        >
          <LinearGradient
            colors={[
              'transparent',
              'rgba(245, 158, 11, 0.08)',
              'rgba(251, 191, 36, 0.32)',
              'rgba(245, 158, 11, 0.08)',
              'transparent',
            ]}
            start={{ x: 0, y: 0 }}
            end={{ x: 0, y: 1 }}
            style={StyleSheet.absoluteFill}
          />
        </Animated.View>
      )}


      {/* Header */}
      <View style={styles.header}>
        {onBack ? (
          <Pressable
            onPress={onBack}
            accessibilityLabel="Back to product list"
            style={({ pressed }) => [styles.iconBtn, pressed && styles.pressed]}
          >
            <Ionicons name="arrow-back" size={24} color={tokens.text} />
          </Pressable>
        ) : (
          <View style={{ width: 44 }} />
        )}
        <View style={styles.headerTitles}>
          <Text style={styles.headerEyebrow}>PRODUCT ANALYSIS</Text>
          <Text style={styles.headerSubtitle} numberOfLines={1}>
            {analysis.label}
          </Text>
        </View>
        <Pressable
          onPress={onClose}
          accessibilityLabel="Close product analysis"
          style={({ pressed }) => [styles.iconBtn, pressed && styles.pressed]}
        >
          <Ionicons name="close" size={24} color={tokens.text} />
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Product Identity */}
        <View style={styles.identityRow}>
          <View style={styles.imageBox}>
            {item.imageUrl ? (
              <Image source={{ uri: item.imageUrl }} style={styles.image} resizeMode="contain" />
            ) : (
              <Text style={styles.initialFallback}>
                {item.productName.trim().charAt(0).toUpperCase() || 'P'}
              </Text>
            )}
          </View>
          <View style={styles.identityTextCol}>
            <Text style={styles.productName} numberOfLines={4}>
              {item.productName}
            </Text>
            <View style={[styles.statusBadge, { backgroundColor: `${toneColor}1A` }]}>
              <Text style={[styles.statusBadgeText, { color: toneColor }]}>
                {product.position === 'COMPETITIVE'
                  ? 'Shop price is competitive'
                  : product.position === 'REVIEW'
                  ? 'Online price is lower'
                  : 'No usable online comparison'}
                {product.needsCheck ? ' • Price check due' : ' • Price is fresh'}
              </Text>
            </View>
          </View>
        </View>

        {/* Verdict Card */}
        <View style={[styles.verdictCard, { backgroundColor: `${toneColor}14`, borderColor: `${toneColor}55` }]}>
          <View style={[styles.verdictIconCircle, { backgroundColor: `${toneColor}22` }]}>
            <Ionicons
              name={
                analysis.tone === 'GOOD'
                  ? 'trending-up'
                  : analysis.tone === 'BAD'
                  ? 'alert-circle'
                  : 'time'
              }
              size={20}
              color={toneColor}
            />
          </View>
          <View style={styles.verdictContent}>
            <Text style={[styles.verdictHeadline, { color: toneColor }]}>
              {analysis.headline}
            </Text>
            <Text style={styles.verdictExplanation}>{analysis.explanation}</Text>
          </View>
        </View>

        {/* Price Evidence Card */}
        <AnalysisSection title="PRICE EVIDENCE" styles={styles}>
          <View style={styles.metricsGrid}>
            <MetricTile
              label="SHOP"
              value={item.shopPrice > 0 ? formatRupees(item.shopPrice) : 'Not set'}
              tokens={tokens}
              styles={styles}
              style={{ flex: 1 }}
            />
            <MetricTile
              label="BEST ONLINE"
              value={online ? formatRupees(online.price) : 'Unavailable'}
              tokens={tokens}
              styles={styles}
              style={{ flex: 1, marginLeft: 8 }}
            />
          </View>
          <View style={{ height: 8 }} />
          <View style={styles.metricsGrid}>
            <MetricTile
              label="AMAZON • SAVED"
              value={(item.amazonLastPrice ?? 0) > 0 ? formatRupees(item.amazonLastPrice!) : 'Unavailable'}
              tokens={tokens}
              styles={styles}
              style={{ flex: 1 }}
            />
            <MetricTile
              label="FLIPKART • SAVED"
              value={(item.flipkartLastPrice ?? 0) > 0 ? formatRupees(item.flipkartLastPrice!) : 'Unavailable'}
              tokens={tokens}
              styles={styles}
              style={{ flex: 1, marginLeft: 8 }}
            />
          </View>

          <View style={{ height: 12 }} />

          {/* Comparative Summary Text */}
          <Text
            style={[
              styles.evidenceSummary,
              {
                color:
                  difference == null
                    ? tokens.textMuted
                    : difference > 0.01
                    ? tokens.danger
                    : tokens.primary,
              },
            ]}
          >
            {difference == null
              ? 'No usable online price is available for comparison.'
              : difference > 0.01
              ? `Shop is ${formatRupees(difference)} higher than ${online?.retailer} (${percentDiff?.toFixed(1)}%).`
              : difference < -0.01
              ? `Shop is ${formatRupees(Math.abs(difference))} lower than ${online?.retailer} (${percentDiff?.toFixed(1)}%).`
              : 'The shop price matches the best saved online price.'}
          </Text>

          <View style={styles.divider} />

          {/* Retailer Detail Rows */}
          <View style={styles.retailerEvidenceRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.retailerName}>Amazon</Text>
              <Text
                style={[
                  styles.retailerStatus,
                  {
                    color: product.amazonFresh
                      ? tokens.primary
                      : item.amazonUrl
                      ? tokens.textMuted
                      : tokens.warning,
                  },
                ]}
              >
                {!item.amazonUrl
                  ? 'Retailer link missing'
                  : !(item.amazonLastPrice && item.amazonLastPrice > 0)
                  ? 'No usable saved price'
                  : product.amazonFresh
                  ? `Saved • ${formatRelativeTime(item.amazonLastChecked)} • fresh`
                  : `Saved • ${formatRelativeTime(item.amazonLastChecked)} • check due`}
              </Text>
            </View>
            <Text style={styles.retailerPrice}>
              {(item.amazonLastPrice ?? 0) > 0 ? formatRupees(item.amazonLastPrice!) : '—'}
            </Text>
          </View>

          <View style={{ height: 10 }} />

          <View style={styles.retailerEvidenceRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.retailerName}>Flipkart</Text>
              <Text
                style={[
                  styles.retailerStatus,
                  {
                    color: product.flipkartFresh
                      ? tokens.primary
                      : item.flipkartUrl
                      ? tokens.textMuted
                      : tokens.warning,
                  },
                ]}
              >
                {!item.flipkartUrl
                  ? 'Retailer link missing'
                  : !(item.flipkartLastPrice && item.flipkartLastPrice > 0)
                  ? 'No usable saved price'
                  : product.flipkartFresh
                  ? `Saved • ${formatRelativeTime(item.flipkartLastChecked)} • fresh`
                  : `Saved • ${formatRelativeTime(item.flipkartLastChecked)} • check due`}
              </Text>
            </View>
            <Text style={styles.retailerPrice}>
              {(item.flipkartLastPrice ?? 0) > 0 ? formatRupees(item.flipkartLastPrice!) : '—'}
            </Text>
          </View>
        </AnalysisSection>

        {/* Margin & Confidence Card */}
        <AnalysisSection title="MARGIN & CONFIDENCE" styles={styles}>
          {cost == null ? (
            <Text style={styles.warningNotice}>
              Purchase cost is missing, so profit and margin cannot be calculated safely.
            </Text>
          ) : (
            <>
              <View style={styles.metricsGrid}>
                <MetricTile
                  label="PURCHASE COST"
                  value={formatRupees(cost)}
                  tokens={tokens}
                  styles={styles}
                  style={{ flex: 1 }}
                />
                <MetricTile
                  label="PROFIT"
                  value={profit != null ? formatRupees(profit) : '—'}
                  valueColor={(profit ?? 0) >= 0 ? tokens.primary : tokens.danger}
                  tokens={tokens}
                  styles={styles}
                  style={{ flex: 1, marginLeft: 8 }}
                />
              </View>
              <View style={{ height: 10 }} />
              <Text
                style={[
                  styles.marginText,
                  { color: (marginPercent ?? 0) >= 0 ? tokens.primary : tokens.danger },
                ]}
              >
                Current shop margin: {marginPercent != null ? `${marginPercent.toFixed(1)}%` : '—'}
              </Text>
            </>
          )}

          <View style={styles.divider} />

          <Text style={[styles.confidenceHeading, { color: confidenceColor }]}>
            {confidence}
          </Text>
          <Text style={styles.confidenceDesc}>{confidenceDesc}</Text>
        </AnalysisSection>

        {/* Price History Section */}
        <AnalysisSection title="PRICE HISTORY • 30 DAYS" styles={styles}>
          {isHistoryLoading ? (
            <View style={styles.loadingHistoryRow}>
              <ActivityIndicator size="small" color={tokens.primary} />
              <Text style={styles.loadingHistoryText}>Loading saved price history…</Text>
            </View>
          ) : history.length === 0 ? (
            <Text style={styles.emptyHistoryText}>
              No saved observations are available in this period. The graph will appear after successful price checks.
            </Text>
          ) : (
            <>
              <Text style={styles.historyHelper}>
                Tap a point to see its retailer, date, price and change.
              </Text>
              <ProductAnalysisLineChart entries={history} height={150} tokens={tokens} styles={styles} />
            </>
          )}
        </AnalysisSection>

        {/* Recommended Next Step */}
        <AnalysisSection title="RECOMMENDED NEXT STEP" styles={styles}>
          <Text style={styles.recommendationText}>{analysis.recommendation}</Text>
        </AnalysisSection>

        {/* Refresh Live Prices Button */}
        <Pressable
          style={({ pressed }) => [
            styles.refreshButton,
            isRefreshing
              ? styles.refreshBtnActive
              : hasRetailerLink
              ? styles.refreshBtnEnabled
              : styles.refreshBtnDisabled,
            pressed && styles.pressed,
          ]}
          onPress={handleRefresh}
          disabled={isRefreshing || !hasRetailerLink}
        >
          {isRefreshing ? (
            <>
              <ActivityIndicator color={tokens.warning} size="small" />
              <Text style={[styles.refreshButtonText, { color: tokens.warning }]}>
                Checking live prices…{throughput ? ` • ${throughput}` : ''}
              </Text>
            </>
          ) : (

            <>
              <Ionicons
                name="refresh"
                size={19}
                color={hasRetailerLink ? (colors.isDark ? '#022c22' : '#FFFFFF') : tokens.textMuted}
              />
              <Text
                style={[
                  styles.refreshButtonText,
                  { color: hasRetailerLink ? (colors.isDark ? '#022c22' : '#FFFFFF') : tokens.textMuted },
                ]}
              >
                {hasRetailerLink ? 'Refresh live prices' : 'Retailer link required'}
              </Text>
            </>
          )}
        </Pressable>
      </ScrollView>
    </View>
  );

  if (isEmbedded) {
    return content;
  }

  return (
    <Modal visible animationType="fade" transparent onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <SafeAreaView style={styles.modalSafeContainer}>{content}</SafeAreaView>
      </View>
    </Modal>
  );
}

function createStyles(tokens: AnalysisTokens) {
  return StyleSheet.create({
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.65)',
      justifyContent: 'center',
      alignItems: 'center',
    },
    modalSafeContainer: {
      width: '96%',
      maxHeight: '94%',
      alignItems: 'center',
      justifyContent: 'center',
    },
    sheet: {
      width: '100%',
      height: '100%',
      backgroundColor: tokens.panelStrong,
      borderRadius: 24,
      borderWidth: 1,
      borderColor: tokens.border,
      overflow: 'hidden',
    },
    spotlightSweep: {
      position: 'absolute',
      left: 0,
      right: 0,
      top: 0,
      height: 240,
      zIndex: 10,
    },

    header: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: 12,
      paddingVertical: 10,
      borderBottomWidth: 1,
      borderBottomColor: tokens.border,
    },
    headerTitles: {
      flex: 1,
      marginHorizontal: 8,
    },
    headerEyebrow: {
      color: tokens.primary,
      fontSize: 11,
      fontFamily: type.bold,
      letterSpacing: 0.8,
    },
    headerSubtitle: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.regular,
    },
    iconBtn: {
      width: 44,
      height: 44,
      justifyContent: 'center',
      alignItems: 'center',
    },
    scrollContent: {
      padding: 16,
      paddingBottom: 36,
    },
    identityRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: 14,
    },
    imageBox: {
      width: 104,
      height: 104,
      borderRadius: 18,
      backgroundColor: tokens.isDark ? '#14181D' : '#F8FAFC',
      borderWidth: 1,
      borderColor: tokens.border,
      alignItems: 'center',
      justifyContent: 'center',
      overflow: 'hidden',
      padding: 8,
    },
    image: {
      width: '100%',
      height: '100%',
    },
    initialFallback: {
      fontSize: 32,
      color: tokens.textMuted,
      fontFamily: type.bold,
    },
    identityTextCol: {
      flex: 1,
      marginLeft: 14,
    },
    productName: {
      color: tokens.text,
      fontSize: 15,
      fontFamily: type.bold,
      lineHeight: 20,
      marginBottom: 8,
    },
    statusBadge: {
      alignSelf: 'flex-start',
      paddingHorizontal: 10,
      paddingVertical: 5,
      borderRadius: radius.pill,
    },
    statusBadgeText: {
      fontSize: 10,
      fontFamily: type.bold,
    },
    verdictCard: {
      flexDirection: 'row',
      padding: 14,
      borderRadius: 18,
      borderWidth: 1,
      marginBottom: 12,
      alignItems: 'flex-start',
    },
    verdictIconCircle: {
      width: 38,
      height: 38,
      borderRadius: 19,
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 11,
    },
    verdictContent: {
      flex: 1,
    },
    verdictHeadline: {
      fontSize: 14,
      fontFamily: type.bold,
      lineHeight: 19,
      marginBottom: 4,
    },
    verdictExplanation: {
      color: tokens.text,
      fontSize: 12,
      fontFamily: type.regular,
      lineHeight: 18,
    },
    sectionCard: {
      backgroundColor: tokens.panel,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 14,
      marginBottom: 12,
    },
    sectionTitle: {
      color: tokens.primary,
      fontSize: 10,
      fontFamily: type.bold,
      letterSpacing: 0.8,
    },
    metricsGrid: {
      flexDirection: 'row',
    },
    metricTile: {
      backgroundColor: tokens.panelStrong,
      borderRadius: 13,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 10,
    },
    metricLabel: {
      color: tokens.textMuted,
      fontSize: 9,
      fontFamily: type.bold,
      marginBottom: 4,
    },
    metricValue: {
      fontSize: 13,
      fontFamily: type.bold,
    },
    evidenceSummary: {
      fontSize: 12,
      fontFamily: type.semibold,
      lineHeight: 17,
    },
    divider: {
      height: 1,
      backgroundColor: tokens.border,
      marginVertical: 12,
    },
    retailerEvidenceRow: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
    },
    retailerName: {
      color: tokens.text,
      fontSize: 12,
      fontFamily: type.bold,
      marginBottom: 2,
    },
    retailerStatus: {
      fontSize: 10,
      fontFamily: type.regular,
    },
    retailerPrice: {
      color: tokens.text,
      fontSize: 13,
      fontFamily: type.bold,
    },
    warningNotice: {
      color: tokens.warning,
      fontSize: 12,
      lineHeight: 18,
      fontFamily: type.regular,
    },
    marginText: {
      fontSize: 12,
      fontFamily: type.bold,
    },
    confidenceHeading: {
      fontSize: 12,
      fontFamily: type.bold,
      marginBottom: 3,
    },
    confidenceDesc: {
      color: tokens.textMuted,
      fontSize: 11,
      lineHeight: 16,
      fontFamily: type.regular,
    },
    loadingHistoryRow: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      paddingVertical: 16,
    },
    loadingHistoryText: {
      color: tokens.textMuted,
      fontSize: 12,
      marginLeft: 9,
      fontFamily: type.regular,
    },
    emptyHistoryText: {
      color: tokens.textMuted,
      fontSize: 12,
      lineHeight: 18,
      fontFamily: type.regular,
    },
    historyHelper: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
      marginBottom: 10,
    },
    chartWrapper: {
      width: '100%',
    },
    chartLegend: {
      flexDirection: 'row',
      marginBottom: 8,
    },
    legendItem: {
      flexDirection: 'row',
      alignItems: 'center',
      marginRight: 14,
    },
    legendDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
      marginRight: 6,
    },
    legendLabel: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.bold,
    },
    tooltipCard: {
      backgroundColor: tokens.panelStrong,
      borderWidth: 1,
      borderColor: tokens.border,
      borderRadius: 8,
      padding: 8,
      marginBottom: 8,
    },
    tooltipHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: 4,
    },
    tooltipRetailer: {
      fontSize: 11,
      fontFamily: type.bold,
      marginRight: 8,
    },
    tooltipTime: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
    },
    tooltipBody: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    tooltipPrice: {
      color: tokens.text,
      fontSize: 13,
      fontFamily: type.bold,
      marginRight: 8,
    },
    tooltipChange: {
      fontSize: 11,
      fontFamily: type.bold,
    },
    chartRow: {
      flexDirection: 'row',
      width: '100%',
    },
    yAxisCol: {
      width: 54,
      height: '100%',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      paddingVertical: 10,
    },
    axisLabel: {
      color: tokens.textMuted,
      fontSize: 9,
      fontFamily: type.regular,
    },
    svgContainer: {
      flex: 1,
      height: '100%',
    },
    recommendationText: {
      color: tokens.text,
      fontSize: 13,
      lineHeight: 19,
      fontFamily: type.regular,
    },
    refreshButton: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      paddingVertical: 14,
      borderRadius: 16,
      marginTop: 6,
    },
    refreshBtnEnabled: {
      backgroundColor: tokens.primary,
    },
    refreshBtnActive: {
      backgroundColor: 'rgba(245, 158, 11, 0.16)',
      borderWidth: 1,
      borderColor: 'rgba(251, 191, 36, 0.70)',
    },
    refreshBtnDisabled: {
      backgroundColor: tokens.panel,
      borderWidth: 1,
      borderColor: tokens.border,
    },
    refreshButtonText: {
      fontSize: 13,
      fontFamily: type.bold,
      marginLeft: 8,
    },
    pressed: {
      opacity: 0.75,
    },
  });
}

