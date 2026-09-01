import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import type { ComparisonOverview } from '../../data/inventoryRepository';
import { colors, radius, spacing, type } from '../../theme/tokens';

export function DashboardScreen({
  overview,
  onOpenInventory,
  onOpenCompare,
  onOpenTools,
}: {
  overview: ComparisonOverview;
  onOpenInventory: () => void;
  onOpenCompare: () => void;
  onOpenTools: () => void;
}) {
  const total = Math.max(1, overview.productCount);
  const competitiveWidth = `${(overview.competitiveCount / total) * 100}%` as `${number}%`;
  const reviewWidth = `${(overview.reviewCount / total) * 100}%` as `${number}%`;
  const uncheckedWidth = `${(overview.uncheckedCount / total) * 100}%` as `${number}%`;
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View style={styles.brandRow}>
        <Image source={require('../../../assets/brand/app_logo.png')} style={styles.logo} contentFit="contain" />
        <View style={styles.brandText}>
          <Text style={styles.brandTitle}>SUPREME</Text>
          <Text style={styles.brandSubtitle}>PRICE INTELLIGENCE</Text>
          <View style={styles.onlineRow}>
            <Ionicons name="shield-checkmark" size={14} color={colors.primary} />
            <Text style={styles.onlineText}>Local data ready</Text>
          </View>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Open backup and migration tools"
          onPress={onOpenTools}
          style={({ pressed }) => [styles.settingsButton, pressed && styles.pressed]}
        >
          <Ionicons name="settings" size={24} color={colors.text} />
        </Pressable>
      </View>

      <View style={styles.overviewCard}>
        <Text style={styles.eyebrow}>SHOP OVERVIEW</Text>
        <Text style={styles.overviewTitle}>
          {overview.productCount} {overview.productCount === 1 ? 'product' : 'products'} compared
        </Text>
        <View style={styles.progressTrack}>
          <View style={[styles.progressSegment, { width: competitiveWidth, backgroundColor: colors.primary }]} />
          <View style={[styles.progressSegment, { width: reviewWidth, backgroundColor: colors.danger }]} />
          <View style={[styles.progressSegment, { width: uncheckedWidth, backgroundColor: colors.textMuted }]} />
        </View>
        <View style={styles.overviewMeta}>
          <View style={styles.metaItem}>
            <View style={[styles.metaDot, { backgroundColor: colors.primary }]} />
            <Text style={styles.metaText}>Competitive {overview.competitiveCount}</Text>
          </View>
          <View style={styles.metaItem}>
            <View style={[styles.metaDot, { backgroundColor: colors.danger }]} />
            <Text style={styles.metaText}>Review {overview.reviewCount}</Text>
          </View>
        </View>
        {overview.uncheckedCount > 0 ? (
          <Text style={styles.uncheckedText}>{overview.uncheckedCount} awaiting a first retailer check</Text>
        ) : null}
      </View>

      <View style={styles.tileGrid}>
        <DashboardTile label="Insights" icon="grid" color={colors.primary} disabled />
        <DashboardTile label="Inventory" icon="archive" color="#4F8DF7" onPress={onOpenInventory} />
        <DashboardTile label="Price Movement" icon="trending-up" color={colors.accent} disabled />
        <DashboardTile label="Quick Compare" icon="search" color={colors.warning} onPress={onOpenCompare} />
      </View>
    </ScrollView>
  );
}

function DashboardTile({
  label,
  icon,
  color,
  onPress,
  disabled = false,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  color: string;
  onPress?: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={disabled ? `${label}, not available in this milestone` : label}
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.tile,
        { borderColor: withAlpha(color, '66'), backgroundColor: withAlpha(color, '12') },
        disabled && styles.tileDisabled,
        pressed && styles.pressed,
      ]}
    >
      <Ionicons name={icon} size={46} color={color} />
      <Text style={styles.tileLabel}>{label}</Text>
    </Pressable>
  );
}

function withAlpha(hex: string, alpha: string): string {
  return hex.length === 7 ? `${hex}${alpha}` : hex;
}

const styles = StyleSheet.create({
  content: { padding: spacing.lg, paddingTop: spacing.md, paddingBottom: 28 },
  brandRow: { minHeight: 60, flexDirection: 'row', alignItems: 'center' },
  logo: { width: 50, height: 50 },
  brandText: { flex: 1, marginLeft: 10 },
  brandTitle: { color: colors.text, fontFamily: type.bold, fontSize: 22, lineHeight: 24, letterSpacing: 0.8 },
  brandSubtitle: { color: colors.text, fontFamily: type.semibold, fontSize: 11, letterSpacing: 0.75 },
  onlineRow: { flexDirection: 'row', alignItems: 'center', marginTop: 5 },
  onlineText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 10, marginLeft: 5 },
  settingsButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: 15, borderWidth: 1, borderColor: colors.border, backgroundColor: colors.surface },
  overviewCard: { marginTop: spacing.md, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.border, borderRadius: 18, padding: spacing.lg },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 10, letterSpacing: 1 },
  overviewTitle: { color: colors.text, fontFamily: type.bold, fontSize: 17, marginTop: 2 },
  progressTrack: { height: 11, flexDirection: 'row', overflow: 'hidden', borderRadius: radius.pill, backgroundColor: colors.surfaceRaised, marginTop: 14 },
  progressSegment: { height: '100%' },
  overviewMeta: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 },
  metaItem: { flexDirection: 'row', alignItems: 'center' },
  metaDot: { width: 8, height: 8, borderRadius: 4, marginRight: spacing.sm },
  metaText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12 },
  uncheckedText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, marginTop: spacing.sm },
  tileGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginTop: spacing.md, rowGap: spacing.md },
  tile: { width: '48.2%', minHeight: 174, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderRadius: 26, padding: 14 },
  tileDisabled: { opacity: 0.5 },
  tileLabel: { width: '100%', color: colors.text, fontFamily: type.bold, fontSize: 15, lineHeight: 19, textAlign: 'center', marginTop: spacing.lg },
  pressed: { opacity: 0.72, transform: [{ scale: 0.99 }] },
});
