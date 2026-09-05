import Ionicons from '@expo/vector-icons/Ionicons';
import React, { useEffect, useMemo, useState, useRef, useCallback } from 'react';
import {
  Dimensions,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
  Platform,
  SafeAreaView,
  LayoutAnimation,
  UIManager,
  ActivityIndicator,
  Animated,
  NativeSyntheticEvent,
  NativeScrollEvent,
  LayoutChangeEvent,
} from 'react-native';
import Svg, {
  Path,
  Line,
  Circle,
  Defs,
  LinearGradient as SvgLinearGradient,
  Stop,
} from 'react-native-svg';

import type {
  InventoryRepository,
  ShopPriceMovementSnapshot,
  ShopProductMovement,
  ShopPriceChange,
  ShopPricePoint,
} from '../../data/inventoryRepository';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';
import type { DynamicColors } from '../../theme/dynamicTheme';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

// ── Compose-exact Design Tokens adapted dynamically to theme ──
function getTokens(themeColors: DynamicColors) {
  return {
    primary: themeColors.primary,
    primaryContainer: themeColors.isDark ? '#123626' : '#DCFCE7',
    panel: themeColors.surface,
    panelMuted: themeColors.surfaceRaised,
    border: themeColors.border,
    text: themeColors.text,
    textMuted: themeColors.textMuted,
    lower: '#FB7185',     // Retailer lowered price (rose error)
    higher: '#10B981',    // Retailer increased price (competitive emerald)
    amazon: '#FF9900',    // Amazon amber
    flipkart: '#2874F0',  // Flipkart blue
    gridLine: themeColors.isDark ? 'rgba(124, 135, 148, 0.22)' : 'rgba(100, 116, 139, 0.22)',
    rootBackground: themeColors.background,
    lowerBoxBg: themeColors.isDark ? 'rgba(251, 113, 133, 0.10)' : 'rgba(251, 113, 133, 0.15)',
    lowerBoxBorder: themeColors.isDark ? 'rgba(251, 113, 133, 0.28)' : 'rgba(251, 113, 133, 0.40)',
    higherBoxBg: themeColors.isDark ? 'rgba(16, 185, 129, 0.10)' : 'rgba(16, 185, 129, 0.15)',
    higherBoxBorder: themeColors.isDark ? 'rgba(16, 185, 129, 0.28)' : 'rgba(16, 185, 129, 0.40)',
  };
}
type TokensType = ReturnType<typeof getTokens>;

type Range = '1D' | '7D' | '14D' | '30D';
type Retailer = 'All' | 'Amazon' | 'Flipkart';

const DEFAULT_RANGE_MAP: Record<string, Range> = {
  SEVEN_DAYS: '7D',
  FOURTEEN_DAYS: '14D',
  THIRTY_DAYS: '30D',
};

const RANGE_DAYS: Record<Range, number> = {
  '1D': 1,
  '7D': 7,
  '14D': 14,
  '30D': 30,
};

const RANGE_BUCKETS: Record<Range, number> = {
  '1D': 6,
  '7D': 7,
  '14D': 14,
  '30D': 15,
};

// ── Formatting Helpers matching Kotlin implementation ──
function formatIndianPrice(price: number): string {
  return `₹${Math.round(price).toLocaleString('en-IN')}`;
}

function formatAxisPrice(price: number): string {
  if (price >= 100000) {
    return `₹${(price / 100000).toFixed(1).replace(/\.0$/, '')}L`;
  }
  if (price >= 1000) {
    return `₹${(price / 1000).toFixed(1).replace(/\.0$/, '')}K`;
  }
  return `₹${Math.round(price)}`;
}

function formatShortDate(timestamp: number): string {
  const d = new Date(timestamp);
  return `${d.getDate()} ${d.toLocaleString('default', { month: 'short' })}`;
}

function formatTime(timestamp: number): string {
  const d = new Date(timestamp);
  let hours = d.getHours();
  const minutes = d.getMinutes().toString().padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12 || 12;
  return `${hours}:${minutes} ${ampm}`;
}

function formatFullDate(timestamp: number): string {
  const d = new Date(timestamp);
  return `${d.getDate()} ${d.toLocaleString('default', { month: 'short' })} ${d.getFullYear()} • ${formatTime(timestamp)}`;
}

function formatRelativeTime(checkedAt: number, now: number): string {
  const diffMs = Math.max(0, now - checkedAt);
  const diffHrs = Math.floor(diffMs / (1000 * 60 * 60));
  if (diffHrs < 1) return 'just now';
  if (diffHrs < 24) return `${diffHrs}h ago`;
  const diffDays = Math.floor(diffHrs / 24);
  return `${diffDays}d ago`;
}

function formatMovementPercent(percentage: number): string {
  const rounded = Math.round(percentage * 10) / 10;
  const displayed = rounded % 1 === 0 ? rounded.toString() : rounded.toFixed(1);
  return `${displayed}%`;
}

function formatAxisTick(price: number, maxPrice: number, minPrice: number): string {
  const maxStr = formatAxisPrice(maxPrice);
  const minStr = formatAxisPrice(minPrice);
  if (maxStr === minStr && maxPrice !== minPrice) {
    return formatIndianPrice(price);
  }
  return formatAxisPrice(price);
}

// ── Aggregate Movement Chart (SVG Paired Rounded Bars) ──
interface MovementBucket {
  lowerCount: number;
  higherCount: number;
}

function AggregateMovementChart({
  changes,
  range,
  generatedAt,
}: {
  changes: ShopPriceChange[];
  range: Range;
  generatedAt: number;
}) {
  const { colors } = useCustomization();
  const tokens = useMemo(() => getTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);
  const [canvasWidth, setCanvasWidth] = useState(0);

  const days = RANGE_DAYS[range];
  const bucketCount = RANGE_BUCKETS[range];
  const windowMillis = days * 24 * 60 * 60 * 1000;
  const cutoff = Math.max(0, generatedAt - windowMillis);
  const bucketSize = Math.max(1, Math.floor(windowMillis / bucketCount));

  const buckets = useMemo(() => {
    const lower = new Array(bucketCount).fill(0);
    const higher = new Array(bucketCount).fill(0);

    changes.forEach((c) => {
      const idx = Math.min(
        bucketCount - 1,
        Math.max(0, Math.floor((c.checkedAt - cutoff) / bucketSize))
      );
      if (c.direction === 'LOWER') {
        lower[idx]++;
      } else {
        higher[idx]++;
      }
    });

    const result: MovementBucket[] = [];
    for (let i = 0; i < bucketCount; i++) {
      result.push({ lowerCount: lower[i], higherCount: higher[i] });
    }
    return result;
  }, [changes, bucketCount, cutoff, bucketSize]);

  const maxCount = useMemo(() => {
    let m = 1;
    buckets.forEach((b) => {
      m = Math.max(m, b.lowerCount, b.higherCount);
    });
    return m;
  }, [buckets]);

  const midCount = Math.floor((maxCount + 1) / 2);
  const startTime = cutoff;
  const midTime = cutoff + Math.floor(windowMillis / 2);

  const formatBoundLabel = (t: number) => {
    return range === '1D' ? formatTime(t) : formatShortDate(t);
  };

  const chartHeight = 112;
  const topPadding = 7;
  const baseline = chartHeight - 7;
  const usableHeight = baseline - topPadding;

  return (
    <View style={styles.aggregateChartWrapper}>
      <Text style={styles.aggregateChartEyebrow}>MOVEMENT ACTIVITY</Text>
      <Text style={styles.aggregateChartHelper}>Y: number of changes • X: time</Text>

      <View style={styles.aggregateChartRow}>
        {/* Y-axis Ticks */}
        <View style={styles.aggregateYAxisCol}>
          <Text style={styles.axisText}>{maxCount}</Text>
          <Text style={styles.axisText}>{midCount}</Text>
          <Text style={styles.axisText}>0</Text>
        </View>

        {/* SVG Canvas */}
        <View
          style={styles.aggregateCanvasContainer}
          onLayout={(e: LayoutChangeEvent) => {
            const w = e.nativeEvent.layout.width;
            if (w > 0) setCanvasWidth(w);
          }}
        >
          {canvasWidth > 0 && (
            <Svg width={canvasWidth} height={chartHeight}>
              {/* 3 Horizontal Reference Lines */}
              {[0, 1, 2].map((i) => {
                const y = topPadding + (usableHeight * i) / 2;
                return (
                  <Line
                    key={`grid_${i}`}
                    x1={0}
                    y1={y}
                    x2={canvasWidth}
                    y2={y}
                    stroke={tokens.gridLine}
                    strokeWidth={1}
                    strokeDasharray="4, 4"
                  />
                );
              })}

              {/* Paired Rounded Bars */}
              {buckets.map((b, i) => {
                const groupWidth = canvasWidth / buckets.length;
                const barWidth = Math.max(2, Math.min(groupWidth * 0.22, 8));
                const centerX = groupWidth * (i + 0.5);

                const lowerH = (usableHeight * b.lowerCount) / maxCount;
                const higherH = (usableHeight * b.higherCount) / maxCount;

                return (
                  <React.Fragment key={`bucket_${i}`}>
                    {b.lowerCount > 0 && (
                      <Line
                        x1={centerX - barWidth * 0.65}
                        y1={baseline}
                        x2={centerX - barWidth * 0.65}
                        y2={baseline - lowerH}
                        stroke={tokens.lower}
                        strokeWidth={barWidth}
                        strokeLinecap="round"
                      />
                    )}
                    {b.higherCount > 0 && (
                      <Line
                        x1={centerX + barWidth * 0.65}
                        y1={baseline}
                        x2={centerX + barWidth * 0.65}
                        y2={baseline - higherH}
                        stroke={tokens.higher}
                        strokeWidth={barWidth}
                        strokeLinecap="round"
                      />
                    )}
                  </React.Fragment>
                );
              })}
            </Svg>
          )}
        </View>
      </View>

      {/* Time Bounds Row */}
      <View style={styles.aggregateTimeRow}>
        <Text style={styles.axisText}>{formatBoundLabel(startTime)}</Text>
        <Text style={styles.axisText}>{formatBoundLabel(midTime)}</Text>
        <Text style={styles.axisText}>Now</Text>
      </View>

      {/* Legend */}
      <View style={styles.chartLegendRow}>
        <View style={styles.legendItem}>
          <View style={[styles.legendCircle, { backgroundColor: tokens.lower }]} />
          <Text style={styles.legendLabel}>Lower</Text>
        </View>
        <View style={[styles.legendItem, { marginLeft: 16 }]}>
          <View style={[styles.legendCircle, { backgroundColor: tokens.higher }]} />
          <Text style={styles.legendLabel}>Higher</Text>
        </View>
      </View>
    </View>
  );
}

// ── Interactive Product Line Chart (Dual Line SVG with Glow Strokes) ──
interface SelectedPoint {
  retailer: 'Amazon' | 'Flipkart';
  price: number;
  previousPrice: number | null;
  checkedAt: number;
  color: string;
}

function ProductMovementLineChart({
  amazonHistory,
  flipkartHistory,
  graphHeight = 150,
}: {
  amazonHistory: ShopPricePoint[];
  flipkartHistory: ShopPricePoint[];
  graphHeight?: number;
}) {
  const { colors } = useCustomization();
  const tokens = useMemo(() => getTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);
  const [canvasWidth, setCanvasWidth] = useState(0);
  const [selectedPoint, setSelectedPoint] = useState<SelectedPoint | null>(null);

  const series = useMemo(() => {
    const list: Array<{
      retailer: 'Amazon' | 'Flipkart';
      color: string;
      points: ShopPricePoint[];
    }> = [];

    if (amazonHistory.length > 0) {
      list.push({
        retailer: 'Amazon',
        color: tokens.amazon,
        points: [...amazonHistory].sort((a, b) => a.checkedAt - b.checkedAt),
      });
    }
    if (flipkartHistory.length > 0) {
      list.push({
        retailer: 'Flipkart',
        color: tokens.flipkart,
        points: [...flipkartHistory].sort((a, b) => a.checkedAt - b.checkedAt),
      });
    }
    return list;
  }, [amazonHistory, flipkartHistory, tokens]);

  const allPoints = useMemo(() => series.flatMap((s) => s.points), [series]);

  if (allPoints.length === 0) return null;

  const minPrice = Math.min(...allPoints.map((p) => p.price));
  const maxPrice = Math.max(...allPoints.map((p) => p.price));
  const midPrice = minPrice + (maxPrice - minPrice) / 2;

  const minTime = Math.min(...allPoints.map((p) => p.checkedAt));
  const maxTime = Math.max(...allPoints.map((p) => p.checkedAt));
  const timeRange = Math.max(1, maxTime - minTime);
  const priceRange = Math.max(1, maxPrice - minPrice);

  const horizontalPadding = 8;
  const verticalPadding = 9;
  const chartW = Math.max(1, canvasWidth - horizontalPadding * 2);
  const chartH = Math.max(1, graphHeight - verticalPadding * 2);
  const chartBottom = graphHeight - verticalPadding;

  const getOffset = (point: ShopPricePoint) => {
    const x =
      minTime === maxTime
        ? horizontalPadding + chartW / 2
        : horizontalPadding + chartW * ((point.checkedAt - minTime) / timeRange);

    const normPrice = (point.price - minPrice) / priceRange;
    const y =
      maxPrice - minPrice <= 0.01
        ? verticalPadding + chartH / 2
        : verticalPadding + chartH * (1 - normPrice);

    return { x, y };
  };

  const handleTap = (evt: any) => {
    const rect = evt.currentTarget?.getBoundingClientRect?.();
    const touchX =
      evt.nativeEvent?.locationX ??
      evt.nativeEvent?.offsetX ??
      (rect && evt.clientX != null ? evt.clientX - rect.left : 0);
    const touchY =
      evt.nativeEvent?.locationY ??
      evt.nativeEvent?.offsetY ??
      (rect && evt.clientY != null ? evt.clientY - rect.top : 0);

    let nearest: SelectedPoint | null = null;
    let minDistanceSq = 35 * 35; // 35dp touch radius

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
    <View style={styles.productLineChartBox}>
      {/* Subtitle */}
      <Text style={styles.productLineChartHelper}>
        Y: saved price • X: date • Tap a point for details
      </Text>

      {/* Legend */}
      <View style={styles.productLineLegendRow}>
        {series.map((s) => (
          <View key={s.retailer} style={styles.legendItem}>
            <View style={[styles.legendCircle, { backgroundColor: s.color }]} />
            <Text style={styles.legendLabel}>{s.retailer}</Text>
          </View>
        ))}
      </View>

      {/* Chart Row */}
      <View style={[styles.productLineChartRow, { height: graphHeight }]}>
        {/* Y-axis Ticks */}
        <View style={styles.productLineYAxisCol}>
          <Text style={styles.axisText}>{formatAxisTick(maxPrice, maxPrice, minPrice)}</Text>
          {maxPrice - minPrice > 0.01 && (
            <Text style={styles.axisText}>{formatAxisTick(midPrice, maxPrice, minPrice)}</Text>
          )}
          <Text style={styles.axisText}>{formatAxisTick(minPrice, maxPrice, minPrice)}</Text>
        </View>

        {/* SVG Area */}
        <Pressable
          style={styles.productLineCanvasContainer}
          onLayout={(e: LayoutChangeEvent) => {
            const w = e.nativeEvent.layout.width;
            if (w > 0) setCanvasWidth(w);
          }}
          onPress={handleTap}
        >
          {canvasWidth > 0 && (
            <Svg width={canvasWidth} height={graphHeight}>
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
                    <Stop offset="0%" stopColor={s.color} stopOpacity="0.22" />
                    <Stop offset="85%" stopColor={s.color} stopOpacity="0.015" />
                    <Stop offset="100%" stopColor={s.color} stopOpacity="0" />
                  </SvgLinearGradient>
                ))}
              </Defs>

              {/* 3 Horizontal Reference Lines */}
              {[0, 1, 2].map((i) => {
                const y = verticalPadding + (chartH * i) / 2;
                return (
                  <Line
                    key={`grid_h_${i}`}
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

              {/* Draw Series Paths and Points */}
              {series.map((s) => {
                const offsets = s.points.map(getOffset);
                if (offsets.length === 0) return null;

                // Bezier Line Path
                let lineD = `M ${offsets[0].x.toFixed(1)} ${offsets[0].y.toFixed(1)}`;
                for (let i = 1; i < offsets.length; i++) {
                  const prev = offsets[i - 1];
                  const curr = offsets[i];
                  const midX = ((prev.x + curr.x) / 2).toFixed(1);
                  lineD += ` C ${midX} ${prev.y.toFixed(1)}, ${midX} ${curr.y.toFixed(1)}, ${curr.x.toFixed(1)} ${curr.y.toFixed(1)}`;
                }

                // Gradient Area Path
                const areaD = `${lineD} L ${offsets[offsets.length - 1].x.toFixed(1)} ${chartBottom} L ${offsets[0].x.toFixed(1)} ${chartBottom} Z`;

                const selectedInSeries =
                  selectedPoint?.retailer === s.retailer
                    ? s.points.find((p) => p.checkedAt === selectedPoint.checkedAt)
                    : null;
                const selectedOffset = selectedInSeries ? getOffset(selectedInSeries) : null;

                return (
                  <React.Fragment key={`series_${s.retailer}`}>
                    {/* Area Fill */}
                    <Path d={areaD} fill={`url(#grad_${s.retailer})`} />

                    {/* Glow Strokes (Triple Pass) */}
                    {offsets.length > 1 && (
                      <>
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={10}
                          strokeOpacity={0.11}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={4.8}
                          strokeOpacity={0.28}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <Path
                          d={lineD}
                          fill="none"
                          stroke={s.color}
                          strokeWidth={2.4}
                          strokeOpacity={1.0}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </>
                    )}

                    {/* Vertical Selection Guide */}
                    {selectedOffset && (
                      <Line
                        x1={selectedOffset.x}
                        y1={verticalPadding}
                        x2={selectedOffset.x}
                        y2={chartBottom}
                        stroke={s.color}
                        strokeOpacity={0.34}
                        strokeWidth={1}
                        strokeDasharray="4, 4"
                      />
                    )}

                    {/* Data Points */}
                    {offsets.map((off, idx) => {
                      const pt = s.points[idx];
                      const isSel =
                        selectedPoint?.retailer === s.retailer &&
                        selectedPoint.checkedAt === pt.checkedAt;
                      const isLatest = idx === offsets.length - 1;

                      if (isSel) {
                        return (
                          <React.Fragment key={`point_${s.retailer}_${idx}`}>
                            <Circle cx={off.x} cy={off.y} r={10} fill={s.color} fillOpacity={0.22} />
                            <Circle cx={off.x} cy={off.y} r={5.8} fill={tokens.panelMuted} />
                            <Circle cx={off.x} cy={off.y} r={4.5} fill={s.color} />
                          </React.Fragment>
                        );
                      }

                      if (isLatest) {
                        return (
                          <React.Fragment key={`point_${s.retailer}_${idx}`}>
                            <Circle cx={off.x} cy={off.y} r={8} fill={s.color} fillOpacity={0.18} />
                            <Circle cx={off.x} cy={off.y} r={4.6} fill={tokens.panelMuted} />
                            <Circle cx={off.x} cy={off.y} r={3.3} fill={s.color} />
                          </React.Fragment>
                        );
                      }

                      return (
                        <React.Fragment key={`point_${s.retailer}_${idx}`}>
                          <Circle cx={off.x} cy={off.y} r={3.7} fill={tokens.panelMuted} />
                          <Circle cx={off.x} cy={off.y} r={2.5} fill={s.color} />
                        </React.Fragment>
                      );
                    })}
                  </React.Fragment>
                );
              })}
            </Svg>
          )}
        </Pressable>
      </View>

      {/* Date Axis Row */}
      <View style={styles.productLineDateRow}>
        <Text style={styles.axisText}>{formatShortDate(minTime)}</Text>
        {maxTime - minTime > 86400000 && (
          <Text style={styles.axisText}>{formatShortDate(minTime + timeRange / 2)}</Text>
        )}
        <Text style={styles.axisText}>{formatShortDate(maxTime)}</Text>
      </View>

      {/* Selected Point Details Pill */}
      {selectedPoint && (
        <View
          style={[
            styles.selectedPointCard,
            {
              backgroundColor: `${selectedPoint.color}18`,
              borderColor: `${selectedPoint.color}60`,
            },
          ]}
        >
          <View style={styles.selectedPointHeaderRow}>
            <View style={[styles.selectedPointDot, { backgroundColor: selectedPoint.color }]} />
            <Text style={styles.selectedPointRetailer}>{selectedPoint.retailer}</Text>
            <View style={{ flex: 1 }} />
            <Text style={[styles.selectedPointPrice, { color: selectedPoint.color }]}>
              {formatIndianPrice(selectedPoint.price)}
            </Text>
          </View>

          <Text style={styles.selectedPointDate}>{formatFullDate(selectedPoint.checkedAt)}</Text>

          {selectedPoint.previousPrice != null && (
            <Text style={styles.selectedPointDesc}>
              {selectedPoint.price < selectedPoint.previousPrice
                ? `Lower by ${formatIndianPrice(selectedPoint.previousPrice - selectedPoint.price)} (-${(((selectedPoint.previousPrice - selectedPoint.price) / selectedPoint.previousPrice) * 100).toFixed(1)}%) from ${formatIndianPrice(selectedPoint.previousPrice)}`
                : selectedPoint.price > selectedPoint.previousPrice
                ? `Higher by ${formatIndianPrice(selectedPoint.price - selectedPoint.previousPrice)} (+${(((selectedPoint.price - selectedPoint.previousPrice) / selectedPoint.previousPrice) * 100).toFixed(1)}%) from ${formatIndianPrice(selectedPoint.previousPrice)}`
                : `Unchanged from ${formatIndianPrice(selectedPoint.previousPrice)}`}
            </Text>
          )}
        </View>
      )}
    </View>
  );
}

// ── Main Screen Component ──
export function PriceMovementScreen({
  repository,
  onClose,
}: {
  repository: InventoryRepository;
  onClose: () => void;
}) {
  const { colors, customization } = useCustomization();
  const tokens = useMemo(() => getTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);

  const initialRange: Range = customization.priceMovementDefaultRange
    ? (DEFAULT_RANGE_MAP[customization.priceMovementDefaultRange] || '30D')
    : '30D';
  const [range, setRange] = useState<Range>(initialRange);
  const [retailer, setRetailer] = useState<Retailer>(
    (customization.insightCustomization?.movementDefaultRetailer as Retailer) || 'All'
  );
  const [collapsedIds, setCollapsedIds] = useState<Set<number>>(new Set());

  const lastScrollY = useRef(0);
  const accumulatedScroll = useRef(0);
  const headerProgress = useRef(new Animated.Value(1)).current;
  const [headerVisible, setHeaderVisible] = useState(true);

  useEffect(() => {
    Animated.timing(headerProgress, {
      toValue: headerVisible ? 1 : 0,
      duration: 180,
      useNativeDriver: false,
    }).start();
  }, [headerProgress, headerVisible]);

  const onScroll = useCallback((event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const offset = Math.max(0, event.nativeEvent.contentOffset.y);
    const delta = offset - lastScrollY.current;

    if ((delta > 0 && accumulatedScroll.current < 0) || (delta < 0 && accumulatedScroll.current > 0)) {
      accumulatedScroll.current = 0;
    }
    accumulatedScroll.current += delta;

    if (offset < 8) {
      setHeaderVisible(true);
      accumulatedScroll.current = 0;
    } else if (accumulatedScroll.current > 24) {
      setHeaderVisible(false);
      accumulatedScroll.current = 0;
    } else if (accumulatedScroll.current < -24) {
      setHeaderVisible(true);
      accumulatedScroll.current = 0;
    }
    lastScrollY.current = offset;
  }, []);

  const [snapshot, setSnapshot] = useState<ShopPriceMovementSnapshot | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchSnapshot = async () => {
    setLoading(true);
    try {
      const data = await repository.getPriceMovementSnapshot(30);
      setSnapshot(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSnapshot();
  }, [repository]);

  const toggleExpand = (id: number) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // ── Filtered Products (Kotlin buildShopPriceMovementView match) ──
  const { filteredProducts, allFilteredChanges } = useMemo(() => {
    if (!snapshot) return { filteredProducts: [], allFilteredChanges: [] };
    const now = snapshot.generatedAt;
    const days = RANGE_DAYS[range];
    const cutoff = now - days * 24 * 60 * 60 * 1000;
    const sortMode = customization.insightCustomization?.movementProductSort || 'LATEST_CHANGE';

    const list = snapshot.products
      .map((p) => {
        let filteredChanges = p.changes.filter((c) => c.checkedAt >= cutoff);
        if (retailer !== 'All') {
          filteredChanges = filteredChanges.filter(
            (c) => c.retailer.toUpperCase() === retailer.toUpperCase()
          );
        }

        const withBaseline = (history: ShopPricePoint[]) => {
          const sorted = [...history].sort((a, b) => a.checkedAt - b.checkedAt);
          const prev = sorted.filter((pt) => pt.checkedAt < cutoff).pop();
          const visible = sorted.filter((pt) => pt.checkedAt >= cutoff);
          return prev ? [prev, ...visible] : visible;
        };

        return {
          ...p,
          changes: filteredChanges,
          amazonHistory: retailer === 'Flipkart' ? [] : withBaseline(p.amazonHistory),
          flipkartHistory: retailer === 'Amazon' ? [] : withBaseline(p.flipkartHistory),
        };
      })
      .filter((p) => p.changes.length > 0)
      .sort((a, b) => {
        if (sortMode === 'PRODUCT_NAME') {
          return a.item.productName.localeCompare(b.item.productName);
        }
        if (sortMode === 'RUPEE_CHANGE') {
          const aDiff = a.changes[0] ? Math.abs(a.changes[0].newPrice - a.changes[0].oldPrice) : 0;
          const bDiff = b.changes[0] ? Math.abs(b.changes[0].newPrice - b.changes[0].oldPrice) : 0;
          return bDiff - aDiff;
        }
        if (sortMode === 'PERCENTAGE_CHANGE') {
          const aPct = a.changes[0] ? Math.abs(a.changes[0].percentage) : 0;
          const bPct = b.changes[0] ? Math.abs(b.changes[0].percentage) : 0;
          return bPct - aPct;
        }
        const aTime = a.changes.length ? Math.max(...a.changes.map((c) => c.checkedAt)) : 0;
        const bTime = b.changes.length ? Math.max(...b.changes.map((c) => c.checkedAt)) : 0;
        return bTime - aTime;
      });

    const allChanges = list.flatMap((p) => p.changes);
    return { filteredProducts: list, allFilteredChanges: allChanges };
  }, [snapshot, range, retailer, customization.insightCustomization?.movementProductSort]);

  const lowerCount = useMemo(
    () => allFilteredChanges.filter((c) => c.direction === 'LOWER').length,
    [allFilteredChanges]
  );
  const higherCount = useMemo(
    () => allFilteredChanges.filter((c) => c.direction === 'HIGHER').length,
    [allFilteredChanges]
  );

  // ── Render Header / Overview in ListHeaderComponent ──
  const renderListHeader = () => (
    <View>
      {/* Filters Panel */}
      <View style={styles.filtersCard}>
        <Text style={styles.filterGroupTitle}>Time period</Text>
        <View style={styles.filterBtnRow}>
          {(['1D', '7D', '14D', '30D'] as Range[]).map((r) => {
            const isSel = range === r;
            return (
              <Pressable
                key={r}
                onPress={() => setRange(r)}
                style={[styles.filterBtn, isSel && styles.filterBtnSelected]}
              >
                <Text style={[styles.filterBtnText, isSel && styles.filterBtnTextSelected]}>
                  {r}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <Text style={[styles.filterGroupTitle, { marginTop: 10 }]}>Retailer</Text>
        <View style={styles.filterBtnRow}>
          {(['All', 'Amazon', 'Flipkart'] as Retailer[]).map((ret) => {
            const isSel = retailer === ret;
            return (
              <Pressable
                key={ret}
                onPress={() => setRetailer(ret)}
                style={[styles.filterBtn, isSel && styles.filterBtnSelected]}
              >
                <Text style={[styles.filterBtnText, isSel && styles.filterBtnTextSelected]}>
                  {ret}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </View>

      {/* Movement Overview Card */}
      <View style={styles.overviewCard}>
        {/* Top summary row */}
        <View style={styles.overviewHeaderRow}>
          <View style={styles.overviewIconCircle}>
            <Ionicons name="trending-up" size={20} color={tokens.primary} />
          </View>
          <View style={styles.overviewHeaderTitles}>
            <Text style={styles.overviewTotalChanges}>
              {allFilteredChanges.length} price changes
            </Text>
            <Text style={styles.overviewChangedProducts}>
              {filteredProducts.length} changed products
            </Text>
          </View>
        </View>

        {/* Side-by-side metric boxes */}
        <View style={styles.metricsRow}>
          <View style={styles.lowerMetricBox}>
            <Ionicons name="trending-down" size={20} color={tokens.lower} />
            <View style={styles.metricTextWrapper}>
              <Text style={styles.metricValue}>{lowerCount}</Text>
              <Text style={[styles.metricLabel, { color: tokens.lower }]}>Lower</Text>
            </View>
          </View>

          <View style={styles.higherMetricBox}>
            <Ionicons name="trending-up" size={20} color={tokens.higher} />
            <View style={styles.metricTextWrapper}>
              <Text style={styles.metricValue}>{higherCount}</Text>
              <Text style={[styles.metricLabel, { color: tokens.higher }]}>Higher</Text>
            </View>
          </View>
        </View>

        {/* Aggregate Movement Chart */}
        {allFilteredChanges.length > 0 && snapshot && (
          <AggregateMovementChart
            changes={allFilteredChanges}
            range={range}
            generatedAt={snapshot.generatedAt}
          />
        )}
      </View>

      {/* Changed Products Title */}
      <View style={styles.listHeaderSection}>
        <Text style={styles.listSectionTitle}>CHANGED PRODUCTS</Text>
        <Text style={styles.listSectionSubtitle}>
          {filteredProducts.length} {filteredProducts.length === 1 ? 'product' : 'products'}
        </Text>
      </View>
    </View>
  );

  // ── Render Product Card ──
  const renderProductItem = ({ item }: { item: ShopProductMovement }) => {
    const changes = item.changes;
    if (changes.length === 0) return null;

    const latest = changes[0];
    const isDown = latest.direction === 'LOWER';
    const movementColor = isDown ? tokens.lower : tokens.higher;
    const isExpanded = !collapsedIds.has(item.item.id);

    const amzLatest =
      item.amazonHistory.length > 0
        ? item.amazonHistory[item.amazonHistory.length - 1].price
        : 0;
    const flipLatest =
      item.flipkartHistory.length > 0
        ? item.flipkartHistory[item.flipkartHistory.length - 1].price
        : 0;

    const retailerLabel =
      latest.retailer.charAt(0).toUpperCase() + latest.retailer.slice(1).toLowerCase();

    return (
      <View style={styles.productCard}>
        <Text style={styles.productTitle} numberOfLines={2}>
          {item.item.productName}
        </Text>

        {/* Status Row */}
        <View style={styles.statusRow}>
          <Ionicons
            name={isDown ? 'trending-down' : 'trending-up'}
            size={18}
            color={movementColor}
          />
          <Text style={[styles.statusText, { color: movementColor }]}>
            {retailerLabel} {isDown ? 'lowered' : 'increased'}
          </Text>
          <Text style={styles.statusTime}>
            {formatRelativeTime(latest.checkedAt, snapshot?.generatedAt || Date.now())}
          </Text>
        </View>

        {/* Price Transition Text */}
        <Text style={styles.transitionText}>
          {formatIndianPrice(latest.oldPrice)} → {formatIndianPrice(latest.newPrice)} •{' '}
          {formatMovementPercent(latest.percentage)}
        </Text>

        {/* Price Graph Accordion Header */}
        <Pressable
          style={styles.accordionHeader}
          onPress={() => toggleExpand(item.item.id)}
          accessibilityRole="button"
        >
          <Text style={styles.accordionTitle}>Price graph</Text>
          <Ionicons
            name={isExpanded ? 'chevron-up' : 'chevron-down'}
            size={19}
            color={tokens.textMuted}
          />
        </Pressable>

        {/* Expanded Chart Section */}
        {isExpanded && (
          <View style={styles.accordionContent}>
            {/* Latest Price Badges */}
            <View style={styles.latestPricesRow}>
              {amzLatest > 0 && (
                <View style={styles.legendItem}>
                  <View style={[styles.legendCircle, { backgroundColor: tokens.amazon }]} />
                  <Text style={styles.latestPriceText}>
                    Amazon {formatIndianPrice(amzLatest)}
                  </Text>
                </View>
              )}
              {flipLatest > 0 && (
                <View style={[styles.legendItem, amzLatest > 0 && { marginLeft: 12 }]}>
                  <View style={[styles.legendCircle, { backgroundColor: tokens.flipkart }]} />
                  <Text style={styles.latestPriceText}>
                    Flipkart {formatIndianPrice(flipLatest)}
                  </Text>
                </View>
              )}
            </View>

            {/* Line Chart */}
            <ProductMovementLineChart
              amazonHistory={item.amazonHistory}
              flipkartHistory={item.flipkartHistory}
              graphHeight={150}
            />
          </View>
        )}
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.root}>
      {/* Scroll-Aware Movement Header */}
      <Animated.View
        style={[
          styles.headerClip,
          {
            height: headerProgress.interpolate({
              inputRange: [0, 1],
              outputRange: [0, 64],
            }),
            opacity: headerProgress,
          },
        ]}
      >
        <View style={styles.header}>
          <Pressable onPress={onClose} style={styles.headerBackBtn} accessibilityRole="button">
            <Ionicons name="arrow-back" size={24} color={tokens.text} />
          </Pressable>

          <View style={styles.headerIconBadge}>
            <Ionicons name="stats-chart" size={20} color={tokens.primary} />
          </View>

          <View style={styles.headerTitleWrap}>
            <Text style={styles.headerEyebrow}>PRICE MOVEMENT</Text>
            <Text style={styles.headerTitle}>Amazon and Flipkart changes</Text>
          </View>

          <Pressable
            style={styles.headerRefreshBtn}
            onPress={fetchSnapshot}
            accessibilityRole="button"
            disabled={loading}
          >
            <Ionicons name="refresh" size={24} color={tokens.primary} />
          </Pressable>
        </View>
      </Animated.View>

      {/* Main Content */}
      {loading ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={tokens.primary} />
        </View>
      ) : (
        <FlatList
          data={filteredProducts}
          keyExtractor={(item) => item.item.id.toString()}
          renderItem={renderProductItem}
          contentContainerStyle={styles.listContent}
          ListHeaderComponent={renderListHeader}
          onScroll={onScroll}
          scrollEventThrottle={16}
          showsVerticalScrollIndicator={false}
        />
      )}
    </SafeAreaView>
  );
}

// ── Stylesheet Factory ──
function createStyles(tokens: TokensType) {
  return StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: tokens.rootBackground,
    },
    headerClip: {
      overflow: 'hidden',
      borderBottomWidth: 1,
      borderBottomColor: tokens.border,
    },
    header: {
      height: 64,
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: 8,
    },
    headerBackBtn: {
      padding: 8,
      marginRight: 4,
    },
    headerIconBadge: {
      width: 38,
      height: 38,
      borderRadius: 13,
      backgroundColor: 'rgba(16, 185, 129, 0.12)',
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 10,
    },
    headerTitleWrap: {
      flex: 1,
    },
    headerEyebrow: {
      color: tokens.primary,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '800',
      letterSpacing: 1,
    },
    headerTitle: {
      color: tokens.text,
      fontSize: 17,
      fontFamily: type.bold,
      fontWeight: '700',
      marginTop: 1,
    },
    headerRefreshBtn: {
      padding: 8,
    },
    centered: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
    },
    listContent: {
      paddingHorizontal: 16,
      paddingTop: 16,
      paddingBottom: 40,
    },

    // ── Filters Card ──
    filtersCard: {
      backgroundColor: tokens.panel,
      borderRadius: 16,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 12,
      marginBottom: 12,
    },
    filterGroupTitle: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '700',
      marginBottom: 6,
    },
    filterBtnRow: {
      flexDirection: 'row',
      gap: 7,
    },
    filterBtn: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      paddingVertical: 10,
      borderRadius: 10,
      backgroundColor: tokens.panelMuted,
      borderWidth: 1,
      borderColor: tokens.border,
    },
    filterBtnSelected: {
      backgroundColor: tokens.primaryContainer,
      borderColor: tokens.primary,
    },
    filterBtnText: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '700',
    },
    filterBtnTextSelected: {
      color: tokens.primary,
    },

    // ── Overview Card ──
    overviewCard: {
      backgroundColor: tokens.panel,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 16,
      marginBottom: 12,
    },
    overviewHeaderRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: 14,
    },
    overviewIconCircle: {
      width: 38,
      height: 38,
      borderRadius: 19,
      backgroundColor: 'rgba(16, 185, 129, 0.14)',
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 10,
    },
    overviewHeaderTitles: {
      flex: 1,
    },
    overviewTotalChanges: {
      color: tokens.text,
      fontSize: 17,
      fontFamily: type.bold,
      fontWeight: '800',
    },
    overviewChangedProducts: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.regular,
      marginTop: 1,
    },
    metricsRow: {
      flexDirection: 'row',
      gap: 8,
    },
    lowerMetricBox: {
      flex: 1,
      flexDirection: 'row',
      alignItems: 'center',
      padding: 11,
      borderRadius: 12,
      backgroundColor: tokens.lowerBoxBg,
      borderWidth: 1,
      borderColor: tokens.lowerBoxBorder,
    },
    higherMetricBox: {
      flex: 1,
      flexDirection: 'row',
      alignItems: 'center',
      padding: 11,
      borderRadius: 12,
      backgroundColor: tokens.higherBoxBg,
      borderWidth: 1,
      borderColor: tokens.higherBoxBorder,
    },
    metricTextWrapper: {
      marginLeft: 8,
    },
    metricValue: {
      color: tokens.text,
      fontSize: 17,
      fontFamily: type.bold,
      fontWeight: '800',
    },
    metricLabel: {
      fontSize: 10,
      fontFamily: type.bold,
      fontWeight: '700',
      marginTop: -2,
    },

    // ── Aggregate Chart ──
    aggregateChartWrapper: {
      marginTop: 16,
    },
    aggregateChartEyebrow: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.bold,
      fontWeight: '700',
      letterSpacing: 0.7,
    },
    aggregateChartHelper: {
      color: tokens.textMuted,
      fontSize: 9,
      fontFamily: type.regular,
      marginTop: 2,
      marginBottom: 9,
    },
    aggregateChartRow: {
      flexDirection: 'row',
      height: 112,
    },
    aggregateYAxisCol: {
      width: 28,
      height: 112,
      paddingVertical: 7,
      justifyContent: 'space-between',
      alignItems: 'flex-end',
      marginRight: 7,
    },
    aggregateCanvasContainer: {
      flex: 1,
      height: 112,
    },
    aggregateTimeRow: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      paddingLeft: 35,
      marginTop: 4,
    },
    chartLegendRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: 9,
    },
    legendItem: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    legendCircle: {
      width: 8,
      height: 8,
      borderRadius: 4,
      marginRight: 6,
    },
    legendLabel: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
    },
    axisText: {
      color: tokens.textMuted,
      fontSize: 9,
      fontFamily: type.regular,
    },

    // ── Changed Products Header ──
    listHeaderSection: {
      marginTop: 8,
      marginBottom: 8,
    },
    listSectionTitle: {
      color: tokens.primary,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '800',
      letterSpacing: 0.9,
    },
    listSectionSubtitle: {
      color: tokens.textMuted,
      fontSize: 12,
      fontFamily: type.regular,
      marginTop: 1,
    },

    // ── Product Card ──
    productCard: {
      backgroundColor: tokens.panel,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 15,
      marginBottom: 12,
    },
    productTitle: {
      color: tokens.text,
      fontSize: 15,
      fontFamily: type.bold,
      fontWeight: '700',
      lineHeight: 21,
    },
    statusRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginTop: 7,
    },
    statusText: {
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '700',
      marginLeft: 6,
      flex: 1,
    },
    statusTime: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
      marginLeft: 8,
    },
    transitionText: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.regular,
      marginTop: 6,
    },
    accordionHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingVertical: 5,
      marginTop: 12,
      borderRadius: 10,
    },
    accordionTitle: {
      color: tokens.text,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '700',
    },
    accordionContent: {
      marginTop: 8,
    },
    latestPricesRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: 10,
    },
    latestPriceText: {
      color: tokens.text,
      fontSize: 11,
      fontFamily: type.bold,
      fontWeight: '700',
    },

    // ── Product Inner Line Chart Box ──
    productLineChartBox: {
      backgroundColor: tokens.panelMuted,
      borderRadius: 16,
      borderWidth: 1,
      borderColor: tokens.border,
      padding: 12,
    },
    productLineChartHelper: {
      color: tokens.textMuted,
      fontSize: 9,
      fontFamily: type.regular,
      marginBottom: 7,
    },
    productLineLegendRow: {
      flexDirection: 'row',
      gap: 16,
      marginBottom: 10,
    },
    productLineChartRow: {
      flexDirection: 'row',
    },
    productLineYAxisCol: {
      width: 57,
      paddingVertical: 9,
      justifyContent: 'space-between',
      alignItems: 'flex-end',
      marginRight: 8,
    },
    productLineCanvasContainer: {
      flex: 1,
      height: '100%',
    },
    productLineDateRow: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      paddingLeft: 65,
      marginTop: 6,
    },

    // ── Selected Point Details Pill ──
    selectedPointCard: {
      backgroundColor: tokens.panel,
      borderColor: tokens.border,
      borderRadius: 12,
      borderWidth: 1,
      padding: 11,
      marginTop: 10,
    },
    selectedPointHeaderRow: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    selectedPointDot: {
      width: 9,
      height: 9,
      borderRadius: 4.5,
      marginRight: 7,
    },
    selectedPointRetailer: {
      color: tokens.text,
      fontSize: 12,
      fontFamily: type.bold,
      fontWeight: '700',
    },
    selectedPointPrice: {
      fontSize: 14,
      fontFamily: type.bold,
      fontWeight: '800',
    },
    selectedPointDate: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
      marginTop: 5,
    },
    selectedPointDesc: {
      color: tokens.textMuted,
      fontSize: 10,
      fontFamily: type.regular,
      marginTop: 4,
    },
  });
}
