import Ionicons from '@expo/vector-icons/Ionicons';
import React, { useCallback, useMemo, useRef, useState, useEffect } from 'react';
import {
  Animated,
  Dimensions,
  FlatList,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { useScrollAwareHeader } from '../../hooks/useScrollAwareHeader';
import { InventoryProduct } from '../../domain/models';
import {
  BrandInsight,
  InsightGroup,
  PricingInsightsSnapshot,
  buildPricingInsightsSnapshot,
} from '../../domain/insights';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';
import type { DynamicColors } from '../../theme/dynamicTheme';
import { InsightProductsModal } from './InsightProductsModal';

function getInsightTokens(themeColors: DynamicColors) {
  return {
    primary: themeColors.primary,
    warning: themeColors.warning,
    danger: themeColors.danger,
    amazon: '#FF9900',
    flipkart: '#2874F0',
    panel: themeColors.surface,
    panelStrong: themeColors.surfaceRaised,
    border: themeColors.border,
    text: themeColors.text,
    textMuted: themeColors.textMuted,
    background: themeColors.background,
  };
}
type InsightTokens = ReturnType<typeof getInsightTokens>;

function useInsightTheme() {
  const { colors } = useCustomization();
  const tokens = useMemo(() => getInsightTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);
  return { colors, tokens, styles };
}

function InsightCard({ title, children }: { title: string; children: React.ReactNode }) {
  const { styles } = useInsightTheme();
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>{title}</Text>
      <View style={styles.cardDivider} />
      {children}
    </View>
  );
}

function MatrixColumnLabel({ text, color, style }: { text: string; color: string; style?: any }) {
  const { styles } = useInsightTheme();
  return (
    <Text style={[styles.matrixColLabel, { color }, style]} numberOfLines={1}>
      {text}
    </Text>
  );
}

function MatrixCell({ value, color, label, onClick, style }: { value: number; color: string; label?: string; onClick: () => void; style?: any }) {
  const { styles } = useInsightTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label || `Matrix cell with ${value} items`}
      style={({ pressed }) => [
        styles.matrixCell,
        { backgroundColor: `${color}1A`, borderColor: `${color}70` },
        style,
        pressed && styles.pressed,
      ]}
      onPress={onClick}
    >
      <Text style={[styles.matrixCellValue, { color }]}>{value}</Text>
    </Pressable>
  );
}

function MatrixRow({
  label,
  labelColor,
  firstValue,
  secondValue,
  firstColor,
  secondColor,
  onFirstClick,
  onSecondClick,
}: {
  label: string;
  labelColor: string;
  firstValue: number;
  secondValue: number;
  firstColor: string;
  secondColor: string;
  onFirstClick: () => void;
  onSecondClick: () => void;
}) {
  const { styles } = useInsightTheme();
  return (
    <View style={styles.matrixRow}>
      <Text style={[styles.matrixRowLabel, { color: labelColor }]} numberOfLines={2}>
        {label}
      </Text>
      <MatrixCell label={`${label} Fresh`} value={firstValue} color={firstColor} onClick={onFirstClick} style={{ flex: 1 }} />
      <MatrixCell label={`${label} Check due`} value={secondValue} color={secondColor} onClick={onSecondClick} style={{ flex: 1, marginLeft: spacing.sm }} />
    </View>
  );
}

function DecisionMatrixCard({ snapshot, onOpenGroup }: { snapshot: PricingInsightsSnapshot; onOpenGroup: (g: InsightGroup) => void }) {
  const { tokens, styles } = useInsightTheme();
  return (
    <InsightCard title="DECISION MATRIX">
      <Text style={styles.helperText}>Position × freshness</Text>
      <View style={{ height: 12 }} />
      <View style={styles.matrixHeader}>
        <View style={{ flex: 0.82 }} />
        <MatrixColumnLabel text="Fresh" color={tokens.primary} style={{ flex: 1, textAlign: 'center' }} />
        <MatrixColumnLabel text="Check due" color={tokens.warning} style={{ flex: 1, textAlign: 'center', marginLeft: spacing.sm }} />
      </View>
      <View style={{ height: 6 }} />
      <MatrixRow
        label="Competitive"
        labelColor={tokens.primary}
        firstValue={snapshot.competitiveFresh}
        secondValue={snapshot.competitiveDue}
        firstColor={tokens.primary}
        secondColor={tokens.warning}
        onFirstClick={() => onOpenGroup('COMPETITIVE_FRESH')}
        onSecondClick={() => onOpenGroup('COMPETITIVE_DUE')}
      />
      <View style={{ height: 6 }} />
      <MatrixRow
        label="Review"
        labelColor={tokens.danger}
        firstValue={snapshot.reviewFresh}
        secondValue={snapshot.reviewDue}
        firstColor={tokens.danger}
        secondColor={tokens.warning}
        onFirstClick={() => onOpenGroup('REVIEW_FRESH')}
        onSecondClick={() => onOpenGroup('REVIEW_DUE')}
      />
      {snapshot.noComparison > 0 && (
        <>
          <View style={{ height: 10 }} />
          <Text style={styles.noComparisonText}>
            {snapshot.noComparison} products have no usable online comparison
          </Text>
        </>
      )}
      <View style={{ height: 10 }} />
      <Text style={[styles.helperText, { fontSize: 10 }]}>Tap a cell to see products</Text>
    </InsightCard>
  );
}

function RetailerPressureTile({ name, alertCount, freshCount, accent, onClick, style }: any) {
  const { styles } = useInsightTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${name} pressure ${alertCount} alerts ${freshCount} fresh`}
      style={({ pressed }) => [styles.pressureTile, style, pressed && styles.pressed]}
      onPress={onClick}
    >
      <Text style={[styles.pressureTileName, { color: accent }]} numberOfLines={1}>
        {name}
      </Text>
      <Text style={styles.pressureAlerts} numberOfLines={1}>
        {alertCount} price alerts
      </Text>
      <Text style={styles.pressureFresh} numberOfLines={1}>
        {freshCount} fresh
      </Text>
    </Pressable>
  );
}

function RetailerPressureCard({
  snapshot,
  onAmazonClick,
  onFlipkartClick,
}: {
  snapshot: PricingInsightsSnapshot;
  onAmazonClick: () => void;
  onFlipkartClick: () => void;
}) {
  const { tokens } = useInsightTheme();
  return (
    <InsightCard title="RETAILER PRESSURE">
      <View style={{ flexDirection: 'row' }}>
        <RetailerPressureTile
          name="Amazon"
          alertCount={snapshot.amazonAlerts}
          freshCount={snapshot.amazonFresh}
          accent={tokens.amazon}
          onClick={onAmazonClick}
          style={{ flex: 1, marginRight: spacing.sm }}
        />
        <RetailerPressureTile
          name="Flipkart"
          alertCount={snapshot.flipkartAlerts}
          freshCount={snapshot.flipkartFresh}
          accent={tokens.flipkart}
          onClick={onFlipkartClick}
          style={{ flex: 1 }}
        />
      </View>
    </InsightCard>
  );
}

function GapBarSegment({ weight, color }: { weight: number; color: string }) {
  if (weight <= 0) return null;
  return <View style={{ flex: weight, height: 12, backgroundColor: color, borderRadius: 6, marginHorizontal: 1 }} />;
}

function GapLegendTile({ count, title, subtitle, color, onClick, style }: any) {
  const { styles } = useInsightTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${title} ${count} products`}
      style={[styles.gapLegendTile, style]}
      onPress={onClick}
    >
      <Text style={[styles.gapLegendCount, { color }]}>{count}</Text>
      <Text style={styles.gapLegendTitle} numberOfLines={2}>
        {title}
      </Text>
      <Text style={styles.gapLegendSubtitle} numberOfLines={2}>
        {subtitle}
      </Text>
    </Pressable>
  );
}

function PriceGapDistributionCard({ snapshot, onOnlineLowerClick, onNearMatchClick, onShopLowerClick }: any) {
  const { tokens } = useInsightTheme();
  const comparableCount = Math.max(1, snapshot.onlineLower + snapshot.nearMatch + snapshot.shopLower);
  return (
    <InsightCard title="PRICE GAP DISTRIBUTION">
      <View style={{ flexDirection: 'row', height: 12, width: '100%', marginBottom: 12 }}>
        <GapBarSegment weight={snapshot.onlineLower / comparableCount} color={tokens.danger} />
        <GapBarSegment weight={snapshot.nearMatch / comparableCount} color={tokens.warning} />
        <GapBarSegment weight={snapshot.shopLower / comparableCount} color={tokens.primary} />
      </View>
      <View style={{ flexDirection: 'row' }}>
        <GapLegendTile
          count={snapshot.onlineLower}
          title="Online lower"
          subtitle="Needs review"
          color={tokens.danger}
          onClick={onOnlineLowerClick}
          style={{ flex: 1 }}
        />
        <GapLegendTile
          count={snapshot.nearMatch}
          title="Near match"
          subtitle="Within 5%"
          color={tokens.warning}
          onClick={onNearMatchClick}
          style={{ flex: 1 }}
        />
        <GapLegendTile
          count={snapshot.shopLower}
          title="Shop lower"
          subtitle="Competitive"
          color={tokens.primary}
          onClick={onShopLowerClick}
          style={{ flex: 1 }}
        />
      </View>
    </InsightCard>
  );
}

function BrandHealthBar({ competitive, review, unresolved }: { competitive: number; review: number; unresolved: number }) {
  const { tokens } = useInsightTheme();
  const total = Math.max(1, competitive + review + unresolved);
  return (
    <View style={{ flexDirection: 'row', height: 8, width: 92, marginHorizontal: spacing.sm }}>
      <GapBarSegment weight={competitive / total} color={tokens.primary} />
      <GapBarSegment weight={review / total} color={tokens.danger} />
      <GapBarSegment weight={unresolved / total} color={tokens.warning} />
    </View>
  );
}

function BrandHealthCard({ brands, onBrandClick }: { brands: BrandInsight[]; onBrandClick: (b: string) => void }) {
  const { tokens, styles } = useInsightTheme();
  return (
    <InsightCard title="BRAND HEALTH">
      {brands.length === 0 ? (
        <Text style={styles.helperText}>Add products to see brand health.</Text>
      ) : (
        brands.slice(0, 6).map((brand, index) => (
          <View key={brand.name}>
            {index > 0 && <View style={styles.innerDivider} />}
            <Pressable
              style={({ pressed }) => [styles.brandRow, pressed && styles.pressed]}
              onPress={() => onBrandClick(brand.name)}
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.brandName} numberOfLines={1}>
                  {brand.name}
                </Text>
                <Text style={styles.brandSubtitle}>
                  {brand.competitive} of {brand.total} competitive
                </Text>
              </View>
              <BrandHealthBar competitive={brand.competitive} review={brand.review} unresolved={brand.unresolved} />
              <Ionicons name="chevron-forward" size={18} color={tokens.textMuted} />
            </Pressable>
          </View>
        ))
      )}
    </InsightCard>
  );
}

function DataQualityRow({ icon, text, color, onClick }: any) {
  const { tokens, styles } = useInsightTheme();
  return (
    <Pressable style={({ pressed }) => [styles.qualityRow, pressed && styles.pressed]} onPress={onClick}>
      <View style={[styles.qualityIconCircle, { backgroundColor: `${color}1F` }]}>
        <Ionicons name={icon} size={18} color={color} />
      </View>
      <Text style={styles.qualityText}>{text}</Text>
      <Ionicons name="chevron-forward" size={18} color={tokens.textMuted} />
    </Pressable>
  );
}

function DataQualityCard({ snapshot, onNeedsCheckClick, onMissingLinksClick, onMissingPricesClick, onMissingCostsClick }: any) {
  const { tokens, styles } = useInsightTheme();
  return (
    <InsightCard title="DATA QUALITY">
      <DataQualityRow
        icon="time"
        text={`${snapshot.needsCheck} prices need checking`}
        color={tokens.warning}
        onClick={onNeedsCheckClick}
      />
      <View style={styles.innerDivider} />
      <DataQualityRow
        icon="link-outline"
        text={`${snapshot.missingLinks} missing retailer links`}
        color={tokens.warning}
        onClick={onMissingLinksClick}
      />
      <View style={styles.innerDivider} />
      <DataQualityRow
        icon="alert-circle"
        text={`${snapshot.missingPrices} linked products without a saved price`}
        color={tokens.danger}
        onClick={onMissingPricesClick}
      />
      <View style={styles.innerDivider} />
      <DataQualityRow
        icon="pricetag"
        text={`${snapshot.missingCosts} missing purchase costs`}
        color={tokens.textMuted}
        onClick={onMissingCostsClick}
      />
    </InsightCard>
  );
}

export function PricingInsightsScreen({
  onBack,
  repository,
  showBanner,
  initialGroup = null,
}: {
  onBack: () => void;
  repository: any;
  showBanner: any;
  initialGroup?: InsightGroup | null;
}) {
  const { tokens, styles } = useInsightTheme();
  const [selectedGroup, setSelectedGroup] = useState<InsightGroup | null>(initialGroup);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const nowMillis = useRef(Date.now()).current;

  useEffect(() => {
    if (initialGroup) {
      setSelectedGroup(initialGroup);
    }
  }, [initialGroup]);

  const reloadProducts = useCallback(() => {
    repository.listProducts().then(setProducts).catch(console.error);
  }, [repository]);

  useEffect(() => {
    reloadProducts();
  }, [reloadProducts]);

  const snapshot = useMemo(() => buildPricingInsightsSnapshot(products, nowMillis), [products, nowMillis]);

  const {
    headerVisible,
    onScroll,
  } = useScrollAwareHeader();

  const header = (
    <View
      style={[
        styles.headerClip,
        !headerVisible && { display: 'none' }
      ]}
    >
      <View style={styles.header}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Back to launch page"
          onPress={onBack}
          style={({ pressed }) => [styles.iconButton, pressed && styles.pressed]}
        >
          <Ionicons name="arrow-back" size={24} color={tokens.text} />
        </Pressable>
        <View style={styles.headerIconShell}>
          <Ionicons name="bar-chart" size={20} color={tokens.primary} />
        </View>
        <View style={styles.headerTextCol}>
          <Text style={styles.headerEyebrow}>PRICING INSIGHTS</Text>
          <Text style={styles.headerSubtitle}>Understand your shop position</Text>
        </View>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.root}>
      {header}
      <ScrollView
        onScroll={onScroll}
        scrollEventThrottle={16}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        <DecisionMatrixCard
          snapshot={snapshot}
          onOpenGroup={(g) => setSelectedGroup(g)}
        />
        <RetailerPressureCard
          snapshot={snapshot}
          onAmazonClick={() => setSelectedGroup('AMAZON_PRESSURE')}
          onFlipkartClick={() => setSelectedGroup('FLIPKART_PRESSURE')}
        />
        <PriceGapDistributionCard
          snapshot={snapshot}
          onOnlineLowerClick={() => setSelectedGroup('ONLINE_LOWER')}
          onNearMatchClick={() => setSelectedGroup('NEAR_MATCH')}
          onShopLowerClick={() => setSelectedGroup('SHOP_LOWER')}
        />
        <BrandHealthCard
          brands={snapshot.brands}
          onBrandClick={(b) => setSelectedBrand(b)}
        />
        <DataQualityCard
          snapshot={snapshot}
          onNeedsCheckClick={() => setSelectedGroup('NEEDS_CHECK')}
          onMissingLinksClick={() => setSelectedGroup('MISSING_LINKS')}
          onMissingPricesClick={() => setSelectedGroup('MISSING_PRICES')}
          onMissingCostsClick={() => setSelectedGroup('MISSING_COSTS')}
        />
      </ScrollView>

      {(selectedGroup || selectedBrand) && (
        <InsightProductsModal
          group={selectedGroup}
          brand={selectedBrand}
          snapshot={snapshot}
          repository={repository}
          showBanner={showBanner}
          onClose={() => {
            setSelectedGroup(null);
            setSelectedBrand(null);
            reloadProducts();
          }}
        />
      )}
    </SafeAreaView>
  );
}

function createStyles(tokens: InsightTokens) {
  return StyleSheet.create({
    root: { flex: 1, backgroundColor: tokens.background },
    headerClip: { overflow: 'hidden', borderBottomColor: tokens.border },
    header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.lg, paddingVertical: spacing.md },
    iconButton: { width: 44, height: 44, justifyContent: 'center', alignItems: 'center', marginLeft: -spacing.sm },
    headerIconShell: { backgroundColor: `${tokens.primary}1F`, borderRadius: 13, padding: 9, marginLeft: spacing.xs, marginRight: spacing.md },
    headerTextCol: { flex: 1 },
    headerEyebrow: { color: tokens.primary, fontSize: 12, fontFamily: type.bold, letterSpacing: 1 },
    headerSubtitle: { color: tokens.textMuted, fontSize: 12, fontFamily: type.regular },
    scrollContent: { padding: spacing.lg, paddingBottom: 40 },
    card: { backgroundColor: tokens.panel, borderRadius: 20, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: tokens.border },
    cardTitle: { color: tokens.primary, fontSize: 11, fontFamily: type.bold, letterSpacing: 0.8 },
    cardDivider: { height: 1, backgroundColor: tokens.border, marginVertical: 10 },
    helperText: { color: tokens.textMuted, fontSize: 12, fontFamily: type.regular },
    noComparisonText: { color: tokens.textMuted, fontSize: 11, fontFamily: type.regular, lineHeight: 15 },
    matrixHeader: { flexDirection: 'row', width: '100%' },
    matrixColLabel: { fontSize: 10, fontFamily: type.bold },
    matrixRow: { flexDirection: 'row', alignItems: 'center', width: '100%' },
    matrixRowLabel: { flex: 0.82, fontSize: 11, fontFamily: type.bold },
    matrixCell: { borderRadius: 13, borderWidth: 1, paddingVertical: 15, alignItems: 'center', justifyContent: 'center' },
    matrixCellValue: { fontSize: 22, fontFamily: type.bold },
    pressureTile: { backgroundColor: tokens.panelStrong, borderRadius: 14, borderWidth: 1, borderColor: tokens.border, padding: 12 },
    pressureTileName: { fontSize: 13, fontFamily: type.bold, marginBottom: 7 },
    pressureAlerts: { color: tokens.danger, fontSize: 11, fontFamily: type.regular, marginBottom: 7 },
    pressureFresh: { color: tokens.primary, fontSize: 11, fontFamily: type.regular },
    gapLegendTile: { paddingVertical: 4 },
    gapLegendCount: { fontSize: 20, fontFamily: type.bold, marginBottom: 2 },
    gapLegendTitle: { color: tokens.text, fontSize: 10, fontFamily: type.bold, marginBottom: 2 },
    gapLegendSubtitle: { color: tokens.textMuted, fontSize: 9, fontFamily: type.regular },
    innerDivider: { height: 1, backgroundColor: tokens.border },
    brandRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10 },
    brandName: { color: tokens.text, fontSize: 13, fontFamily: type.bold, marginBottom: 2 },
    brandSubtitle: { color: tokens.textMuted, fontSize: 10, fontFamily: type.regular },
    qualityRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10 },
    qualityIconCircle: { width: 32, height: 32, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
    qualityText: { flex: 1, color: tokens.text, fontSize: 12, fontFamily: type.semibold, marginLeft: 10, lineHeight: 16 },
    pressed: { opacity: 0.75 },
  });
}
