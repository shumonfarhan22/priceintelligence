import React, { useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  Pressable,
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
import { LinearGradient } from 'expo-linear-gradient';
import { PriceObservation, PriceRetailer } from '../../domain/models';
import { formatRupees } from '../../domain/formatting';
import { colors, spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';

const RETAILER_COLORS = {
  AMAZON: '#FF9900',
  FLIPKART: '#2874F0',
};

const CHART_SURFACE_COLOR = '#14181D';
const PANEL_BG = 'rgba(20, 24, 29, 0.75)';

function hexToRgba(hex: string, alpha: number): string {
  const clean = hex.replace('#', '');
  const r = parseInt(clean.substring(0, 2), 16);
  const g = parseInt(clean.substring(2, 4), 16);
  const b = parseInt(clean.substring(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

export type PriceMovement = 'LOWER' | 'HIGHER' | 'UNCHANGED' | 'UNKNOWN';

export interface RetailerPriceHistorySummary {
  retailer: PriceRetailer;
  latestPrice: number;
  previousPrice: number | null;
  lowestSavedPrice: number;
  highestSavedPrice: number;
  averagePrice: number;
  latestCheckedAt: number;
  observationCount: number;
  movement: PriceMovement;
  movementAmount: number | null;
  movementPercent: number | null;
}

function summarizeRetailerHistory(
  entries: PriceObservation[],
  retailer: PriceRetailer
): RetailerPriceHistorySummary | null {
  const validEntries = entries
    .filter(
      (e) =>
        e.retailer === retailer &&
        Number.isFinite(e.price) &&
        e.price > 0 &&
        e.checkedAt > 0
    )
    .sort((a, b) => b.checkedAt - a.checkedAt);

  if (validEntries.length === 0) return null;

  const latest = validEntries[0];
  const previous = validEntries[1] ?? null;
  const change = previous ? latest.price - previous.price : null;

  let movement: PriceMovement = 'UNKNOWN';
  if (change !== null) {
    if (Math.abs(change) <= 0.01) movement = 'UNCHANGED';
    else if (change < 0) movement = 'LOWER';
    else movement = 'HIGHER';
  }

  const percent =
    previous && previous.price > 0 && change !== null
      ? (change / previous.price) * 100
      : null;

  const prices = validEntries.map((e) => e.price);
  const sum = prices.reduce((acc, p) => acc + p, 0);

  return {
    retailer,
    latestPrice: latest.price,
    previousPrice: previous ? previous.price : null,
    lowestSavedPrice: Math.min(...prices),
    highestSavedPrice: Math.max(...prices),
    averagePrice: sum / prices.length,
    latestCheckedAt: latest.checkedAt,
    observationCount: validEntries.length,
    movement,
    movementAmount: change !== null ? Math.abs(change) : null,
    movementPercent: percent !== null ? Math.abs(percent) : null,
  };
}

function formatHistoryDate(timestamp: number): string {
  const d = new Date(timestamp);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const mins = String(d.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} at ${hours}:${mins} IST`;
}

function formatTimeAgo(timestamp: number): string {
  const elapsed = Math.max(0, Date.now() - timestamp);
  const minutes = Math.floor(elapsed / 60000);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days} day${days === 1 ? '' : 's'} ago`;
  if (hours > 0) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  if (minutes > 0) return `${minutes} minute${minutes === 1 ? '' : 's'} ago`;
  return 'recently';
}

interface PriceHistorySectionProps {
  entries: PriceObservation[];
  isLoading?: boolean;
  shopPrice: number;
}

export function PriceHistorySection({
  entries,
  isLoading = false,
  shopPrice,
}: PriceHistorySectionProps) {
  const { colors, customization } = useCustomization();
  const historyRange = customization.insightCustomization?.priceHistoryRange || 'THIRTY_DAYS';
  const rangeDays = historyRange === 'SEVEN_DAYS' ? 7 : historyRange === 'FOURTEEN_DAYS' ? 14 : 30;
  const oldestAllowedTimestamp = Date.now() - rangeDays * 86400000;

  const displayEntries = useMemo(() => {
    return entries
      .filter(
        (e) =>
          (e.retailer === 'AMAZON' || e.retailer === 'FLIPKART') &&
          Number.isFinite(e.price) &&
          e.price > 0 &&
          e.checkedAt >= oldestAllowedTimestamp
      )
      .sort((a, b) => a.checkedAt - b.checkedAt);
  }, [entries, oldestAllowedTimestamp]);

  const summaries = useMemo(() => {
    const list: RetailerPriceHistorySummary[] = [];
    const amazonSummary = summarizeRetailerHistory(displayEntries, 'AMAZON');
    if (amazonSummary) list.push(amazonSummary);
    const flipkartSummary = summarizeRetailerHistory(displayEntries, 'FLIPKART');
    if (flipkartSummary) list.push(flipkartSummary);
    return list;
  }, [displayEntries]);

  return (
    <View style={styles.sectionContainer}>
      <Text style={[styles.sectionTitle, { color: colors.text }]}>Price history</Text>
      <Text style={[styles.sectionSubtitle, { color: colors.textMuted }]}>
        Shows the last {rangeDays} days. Up to 60 successful checks per retailer are kept
        on this device.
      </Text>

      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="small" color={colors.textMuted} />
          <Text style={[styles.loadingText, { color: colors.textMuted }]}>Loading saved checks...</Text>
        </View>
      ) : summaries.length === 0 ? (
        <View style={[styles.emptyContainer, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}>
          <Text style={[styles.emptyText, { color: colors.text, fontFamily: type.bold, marginBottom: 4 }]}>No saved checks yet</Text>
          <Text style={[styles.emptyText, { color: colors.textMuted }]}>
            Use “Refresh Live Prices” to fetch and save prices to the rolling
            history.
          </Text>
        </View>
      ) : (
        summaries.map((summary) => (
          <RetailerPriceHistoryCard
            key={summary.retailer}
            summary={summary}
            entries={displayEntries.filter(
              (e) => e.retailer === summary.retailer
            )}
            shopPrice={shopPrice}
          />
        ))
      )}
    </View>
  );
}

function RetailerPriceHistoryCard({
  summary,
  entries,
  shopPrice,
}: {
  summary: RetailerPriceHistorySummary;
  entries: PriceObservation[];
  shopPrice: number;
}) {
  const { colors } = useCustomization();
  const retailerName = summary.retailer === 'AMAZON' ? 'Amazon' : 'Flipkart';
  const lineColor = RETAILER_COLORS[summary.retailer];

  let movementText = 'First saved check';
  let movementColor: string = colors.textMuted;
  if (summary.movement === 'LOWER') {
    movementText = `Down ${formatRupees(summary.movementAmount || 0)}${
      summary.movementPercent != null
        ? ` (${summary.movementPercent.toFixed(1)}%)`
        : ''
    } since the previous check`;
    movementColor = colors.danger;
  } else if (summary.movement === 'HIGHER') {
    movementText = `Up ${formatRupees(summary.movementAmount || 0)}${
      summary.movementPercent != null
        ? ` (${summary.movementPercent.toFixed(1)}%)`
        : ''
    } since the previous check`;
    movementColor = colors.primary;
  } else if (summary.movement === 'UNCHANGED') {
    movementText = 'Unchanged since the previous check';
    movementColor = colors.textMuted;
  }

  return (
    <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      <View style={styles.cardHeader}>
        <Text style={[styles.retailerName, { color: colors.text }]}>{retailerName}</Text>
        <Text style={[styles.checkCount, { color: colors.textMuted }]}>
          {summary.observationCount === 1
            ? '1 saved check'
            : `${summary.observationCount} saved checks`}
        </Text>
      </View>

      <View style={styles.metricRow}>
        <View style={styles.metricCol}>
          <Text style={[styles.metricLabel, { color: colors.textMuted }]}>Latest</Text>
          <Text style={[styles.metricValue, { color: colors.text }]}>
            {formatRupees(summary.latestPrice)}
          </Text>
        </View>
        <View style={styles.metricCol}>
          <Text style={[styles.metricLabel, { color: colors.textMuted }]}>Lowest saved</Text>
          <Text style={[styles.metricValue, { color: colors.text }]}>
            {formatRupees(summary.lowestSavedPrice)}
          </Text>
        </View>
      </View>

      <View style={styles.metricRow}>
        <View style={styles.metricCol}>
          <Text style={[styles.metricLabel, { color: colors.textMuted }]}>Highest saved</Text>
          <Text style={[styles.metricValue, { color: colors.text }]}>
            {formatRupees(summary.highestSavedPrice)}
          </Text>
        </View>
        <View style={styles.metricCol}>
          <Text style={[styles.metricLabel, { color: colors.textMuted }]}>Average</Text>
          <Text style={[styles.metricValue, { color: colors.text }]}>
            {formatRupees(Math.round(summary.averagePrice))}
          </Text>
        </View>
      </View>

      <Text style={[styles.movementText, { color: movementColor }]}>
        {movementText}
      </Text>

      <PriceHistoryLineGraph
        entries={entries}
        retailer={summary.retailer}
        shopPrice={shopPrice}
        lineColor={lineColor}
      />

      <Text style={[styles.latestSavedFooter, { color: colors.textMuted }]}>
        Latest saved {formatTimeAgo(summary.latestCheckedAt)}
      </Text>
    </View>
  );
}

function PriceHistoryLineGraph({
  entries,
  retailer,
  shopPrice,
  lineColor,
}: {
  entries: PriceObservation[];
  retailer: PriceRetailer;
  shopPrice: number;
  lineColor: string;
}) {
  const { colors, customization } = useCustomization();
  const graphStyle = customization.insightCustomization?.historyGraphStyle || 'AREA';
  const [selectedPoint, setSelectedPoint] = useState<PriceObservation | null>(
    null
  );
  const [layoutWidth, setLayoutWidth] = useState(280);

  const points = useMemo(() => {
    return entries
      .filter((e) => Number.isFinite(e.price) && e.price > 0 && e.checkedAt > 0)
      .sort((a, b) => a.checkedAt - b.checkedAt);
  }, [entries]);

  if (points.length === 0) return null;

  const height = 126;
  const horizontalInset = 12;
  const verticalInset = 14;
  const graphWidth = Math.max(1, layoutWidth - horizontalInset * 2);
  const graphHeight = Math.max(1, height - verticalInset * 2);
  const chartBottom = verticalInset + graphHeight;

  const validShopPrice =
    Number.isFinite(shopPrice) && shopPrice > 0 ? shopPrice : null;

  const allPrices = points.map((p) => p.price);
  if (validShopPrice !== null) allPrices.push(validShopPrice);

  const rawMin = Math.min(...allPrices);
  const rawMax = Math.max(...allPrices);
  const rawRange = rawMax - rawMin;
  const pricePadding = Math.max(rawRange * 0.12, rawMax * 0.01, 1.0);
  const chartMin = Math.max(0, rawMin - pricePadding);
  const chartMax = rawMax + pricePadding;
  const chartRange = Math.max(1, chartMax - chartMin);

  const firstTimestamp = points[0].checkedAt;
  const lastTimestamp = points[points.length - 1].checkedAt;
  const timestampRange = Math.max(1, lastTimestamp - firstTimestamp);

  const xPosition = (entry: PriceObservation) => {
    if (points.length === 1) return horizontalInset + graphWidth / 2;
    const fraction = (entry.checkedAt - firstTimestamp) / timestampRange;
    return horizontalInset + graphWidth * fraction;
  };

  const yPosition = (price: number) => {
    const fraction = (price - chartMin) / chartRange;
    return verticalInset + graphHeight * (1 - fraction);
  };

  const pointOffsets = points.map((p) => ({
    x: xPosition(p),
    y: yPosition(p.price),
    entry: p,
  }));

  let linePath = '';
  let areaPath = '';

  if (pointOffsets.length > 1) {
    if (graphStyle === 'STEP') {
      linePath = `M ${pointOffsets[0].x.toFixed(1)} ${pointOffsets[0].y.toFixed(1)}`;
      for (let i = 1; i < pointOffsets.length; i++) {
        const prev = pointOffsets[i - 1];
        const curr = pointOffsets[i];
        linePath += ` L ${curr.x.toFixed(1)} ${prev.y.toFixed(1)} L ${curr.x.toFixed(1)} ${curr.y.toFixed(1)}`;
      }
    } else {
      linePath = `M ${pointOffsets[0].x.toFixed(1)} ${pointOffsets[0].y.toFixed(1)}`;
      for (let i = 1; i < pointOffsets.length; i++) {
        const prev = pointOffsets[i - 1];
        const curr = pointOffsets[i];
        const midX = ((prev.x + curr.x) / 2).toFixed(1);
        linePath += ` C ${midX} ${prev.y.toFixed(1)}, ${midX} ${curr.y.toFixed(
          1
        )}, ${curr.x.toFixed(1)} ${curr.y.toFixed(1)}`;
      }
    }
    const lastX = pointOffsets[pointOffsets.length - 1].x.toFixed(1);
    const firstX = pointOffsets[0].x.toFixed(1);
    areaPath = `${linePath} L ${lastX} ${chartBottom} L ${firstX} ${chartBottom} Z`;
  }

  const onTouchGraph = (e: any) => {
    const touchX = e.nativeEvent.locationX;
    let closest = points[0];
    let minDiff = Infinity;
    points.forEach((p) => {
      const px = xPosition(p);
      const diff = Math.abs(px - touchX);
      if (diff < minDiff) {
        minDiff = diff;
        closest = p;
      }
    });
    setSelectedPoint(closest);
  };

  const gradId = `gradient_${retailer}`;

  return (
    <View
      style={[
        styles.graphBox,
        {
          backgroundColor: colors.surfaceRaised,
          borderColor: hexToRgba(lineColor, 0.22),
        },
      ]}
      onLayout={(e: LayoutChangeEvent) => {
        const w = e.nativeEvent.layout.width;
        if (w > 0) setLayoutWidth(w);
      }}
    >
      <LinearGradient
        colors={[
          hexToRgba(lineColor, 0.05),
          'rgba(255, 255, 255, 0.02)',
          'rgba(255, 255, 255, 0.035)',
        ]}
        locations={[0, 0.35, 1]}
        style={StyleSheet.absoluteFill}
        start={{ x: 0.5, y: 0 }}
        end={{ x: 0.5, y: 1 }}
      />

      <Pressable onPress={onTouchGraph} style={{ width: '100%', height }}>
        <Svg width={layoutWidth} height={height}>
          <Defs>
            <SvgLinearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
              <Stop offset="0" stopColor={lineColor} stopOpacity="0.08" />
              <Stop offset="0.6" stopColor={lineColor} stopOpacity="0.015" />
              <Stop offset="1" stopColor={lineColor} stopOpacity="0" />
            </SvgLinearGradient>
          </Defs>

          {/* 3 Horizontal Grid Lines */}
          {[0, 1, 2].map((idx) => {
            const y = verticalInset + (graphHeight * idx) / 2;
            return (
              <Line
                key={idx}
                x1={horizontalInset}
                y1={y}
                x2={horizontalInset + graphWidth}
                y2={y}
                stroke={colors.border}
                strokeWidth={1}
              />
            );
          })}

          {/* Dashed Shop Price Line */}
          {validShopPrice !== null && (
            <Line
              x1={horizontalInset}
              y1={yPosition(validShopPrice)}
              x2={horizontalInset + graphWidth}
              y2={yPosition(validShopPrice)}
              stroke={colors.textMuted}
              strokeWidth={1.5}
              strokeDasharray="7, 5"
            />
          )}

          {/* Area under line */}
          {graphStyle === 'AREA' && areaPath !== '' && <Path d={areaPath} fill={`url(#${gradId})`} />}

          {/* 3 glow/line passes */}
          {linePath !== '' && (
            <>
              <Path
                d={linePath}
                fill="none"
                stroke={lineColor}
                strokeWidth={9}
                opacity={0.10}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <Path
                d={linePath}
                fill="none"
                stroke={lineColor}
                strokeWidth={4.2}
                opacity={0.24}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <Path
                d={linePath}
                fill="none"
                stroke={lineColor}
                strokeWidth={2.4}
                opacity={1}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </>
          )}

          {/* Selected point vertical dashed line */}
          {selectedPoint && (
            <Line
              x1={xPosition(selectedPoint)}
              y1={verticalInset}
              x2={xPosition(selectedPoint)}
              y2={chartBottom}
              stroke={`${lineColor}55`}
              strokeWidth={1}
              strokeDasharray="4, 4"
            />
          )}

          {/* Data Points */}
          {pointOffsets.map((pt, idx) => {
            const isLatest = idx === pointOffsets.length - 1;
            const isSelected = selectedPoint === pt.entry;
            return (
              <React.Fragment key={idx}>
                {isLatest && !isSelected && (
                  <Circle
                    cx={pt.x}
                    cy={pt.y}
                    r={7.5}
                    fill={hexToRgba(lineColor, 0.16)}
                  />
                )}
                <Circle
                  cx={pt.x}
                  cy={pt.y}
                  r={isLatest ? 4.6 : 3.6}
                  fill={CHART_SURFACE_COLOR}
                />
                <Circle
                  cx={pt.x}
                  cy={pt.y}
                  r={isLatest ? 3.0 : 2.2}
                  fill={lineColor}
                />
              </React.Fragment>
            );
          })}

          {/* Selected Point Highlight */}
          {selectedPoint && (
            <>
              <Circle
                cx={xPosition(selectedPoint)}
                cy={yPosition(selectedPoint.price)}
                r={13}
                fill={hexToRgba(lineColor, 0.16)}
              />
              <Circle
                cx={xPosition(selectedPoint)}
                cy={yPosition(selectedPoint.price)}
                r={6.5}
                fill={CHART_SURFACE_COLOR}
              />
              <Circle
                cx={xPosition(selectedPoint)}
                cy={yPosition(selectedPoint.price)}
                r={4.2}
                fill={lineColor}
              />
            </>
          )}
        </Svg>
      </Pressable>

      {/* Selected Point Details Pill */}
      {selectedPoint && (
        <View
          style={[
            styles.selectedPointCard,
            {
              backgroundColor: `${lineColor}22`,
              borderColor: `${lineColor}70`,
            },
          ]}
        >
          <View>
            <Text style={styles.selectedPointDate}>
              Saved on {formatHistoryDate(selectedPoint.checkedAt)}
            </Text>
            <Text style={styles.selectedPointTimeAgo}>
              {formatTimeAgo(selectedPoint.checkedAt)}
            </Text>
          </View>
          <Text style={[styles.selectedPointPrice, { color: lineColor }]}>
            {formatRupees(selectedPoint.price)}
          </Text>
        </View>
      )}

      {/* Older / Latest axis labels */}
      <View style={styles.axisRow}>
        <Text style={styles.axisText}>OLDER</Text>
        <Text style={styles.axisText}>LATEST</Text>
      </View>

      {/* Supreme price dashed legend */}
      {validShopPrice !== null && (
        <Text style={styles.legendText}>
          Dashed line: Supreme price {formatRupees(validShopPrice)}
        </Text>
      )}

      {points.length === 1 && (
        <Text style={styles.legendText}>
          One check saved. The trend line will appear after another successful
          check.
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: spacing.md,
    gap: 10,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontFamily: type.bold,
    fontWeight: '800',
  },
  sectionSubtitle: {
    color: colors.textMuted,
    fontSize: 12,
    fontFamily: type.regular,
    lineHeight: 17,
  },
  loadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.lg,
    gap: spacing.sm,
  },
  loadingText: {
    color: colors.textMuted,
    fontSize: 12,
    fontFamily: type.regular,
  },
  emptyContainer: {
    backgroundColor: PANEL_BG,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 14,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: 12,
    fontFamily: type.regular,
  },
  card: {
    backgroundColor: PANEL_BG,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 14,
    gap: 8,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  retailerName: {
    color: colors.text,
    fontSize: 14,
    fontFamily: type.bold,
    fontWeight: '800',
  },
  checkCount: {
    color: colors.textMuted,
    fontSize: 10,
    fontFamily: type.bold,
  },
  metricRow: {
    flexDirection: 'row',
    gap: 16,
  },
  metricCol: {
    flex: 1,
  },
  metricLabel: {
    color: colors.textMuted,
    fontSize: 10,
    fontFamily: type.regular,
  },
  metricValue: {
    color: colors.text,
    fontSize: 15,
    fontFamily: type.bold,
    fontWeight: '800',
    marginTop: 1,
  },
  movementText: {
    fontSize: 12,
    fontFamily: type.bold,
    marginTop: 2,
  },
  graphBox: {
    borderRadius: 14,
    borderWidth: 1,
    overflow: 'hidden',
    padding: 10,
    gap: 5,
    marginTop: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.035)',
  },
  selectedPointCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
    marginTop: 4,
  },
  selectedPointDate: {
    color: colors.text,
    fontSize: 11,
    fontFamily: type.bold,
  },
  selectedPointTimeAgo: {
    color: colors.textMuted,
    fontSize: 10,
    fontFamily: type.regular,
    marginTop: 1,
  },
  selectedPointPrice: {
    fontSize: 15,
    fontFamily: type.bold,
    fontWeight: '800',
  },
  axisRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 2,
  },
  axisText: {
    color: colors.textMuted,
    fontSize: 9,
    fontFamily: type.bold,
  },
  legendText: {
    color: colors.textMuted,
    fontSize: 10,
    fontFamily: type.regular,
    lineHeight: 14,
  },
  latestSavedFooter: {
    color: colors.textMuted,
    fontSize: 10,
    fontFamily: type.regular,
    marginTop: 2,
  },
});
