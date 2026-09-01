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
          <Ionicons name="settings" size={27} color={colors.text} />
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

      <View style={styles.milestoneCard}>
        <View style={styles.milestoneCopy}>
          <Text style={styles.milestoneTitle}>Comparison milestone active</Text>
          <Text style={styles.milestoneBody}>Search, scan, open saved evidence, and refresh Amazon or Flipkart prices safely.</Text>
        </View>
        <Ionicons name="shield-checkmark-outline" size={25} color={colors.primary} />
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
      <View style={[styles.iconHalo, { backgroundColor: withAlpha(color, '18') }]}>
        <View style={[styles.iconShell, { borderColor: withAlpha(color, '77'), backgroundColor: withAlpha(color, '20') }]}>
          <Ionicons name={icon} size={31} color={color} />
        </View>
      </View>
      <Text style={styles.tileLabel}>{label}</Text>
      {disabled ? <Text style={styles.tileStatus}>NEXT MILESTONE</Text> : <Text style={[styles.tileStatus, { color }]}>READY</Text>}
    </Pressable>
  );
}

function withAlpha(hex: string, alpha: string): string {
  return hex.length === 7 ? `${hex}${alpha}` : hex;
}

const styles = StyleSheet.create({
  content: { padding: spacing.xl, paddingBottom: 48 },
  brandRow: { minHeight: 92, flexDirection: 'row', alignItems: 'center' },
  logo: { width: 62, height: 62 },
  brandText: { flex: 1, marginLeft: spacing.md },
  brandTitle: { color: colors.text, fontFamily: type.bold, fontSize: 25, letterSpacing: 1.2 },
  brandSubtitle: { color: colors.text, fontFamily: type.semibold, fontSize: 11, letterSpacing: 0.75 },
  onlineRow: { flexDirection: 'row', alignItems: 'center', marginTop: spacing.sm },
  onlineText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, marginLeft: spacing.xs },
  settingsButton: { width: 58, height: 58, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, backgroundColor: colors.surface },
  overviewCard: { marginTop: spacing.lg, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.border, borderRadius: radius.lg, padding: spacing.xl },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  overviewTitle: { color: colors.text, fontFamily: type.bold, fontSize: 22, marginTop: spacing.sm },
  progressTrack: { height: 13, flexDirection: 'row', overflow: 'hidden', borderRadius: radius.pill, backgroundColor: colors.surfaceRaised, marginTop: spacing.xl },
  progressSegment: { height: '100%' },
  overviewMeta: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: spacing.lg },
  metaItem: { flexDirection: 'row', alignItems: 'center' },
  metaDot: { width: 8, height: 8, borderRadius: 4, marginRight: spacing.sm },
  metaText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13 },
  uncheckedText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12, marginTop: spacing.md },
  milestoneCard: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, padding: spacing.lg, marginTop: spacing.lg },
  milestoneCopy: { flex: 1, minWidth: 0, paddingRight: spacing.md },
  milestoneTitle: { color: colors.text, fontFamily: type.bold, fontSize: 15 },
  milestoneBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12, lineHeight: 17, marginTop: spacing.xs },
  tileGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', marginTop: spacing.lg, rowGap: spacing.lg },
  tile: { width: '48%', minHeight: 214, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderRadius: radius.lg, padding: spacing.lg },
  tileDisabled: { opacity: 0.43 },
  iconHalo: { width: 104, height: 104, alignItems: 'center', justifyContent: 'center', borderRadius: 34 },
  iconShell: { width: 72, height: 72, alignItems: 'center', justifyContent: 'center', borderRadius: 23, borderWidth: 1 },
  tileLabel: { color: colors.text, fontFamily: type.bold, fontSize: 17, textAlign: 'center', marginTop: spacing.lg },
  tileStatus: { color: colors.textMuted, fontFamily: type.bold, fontSize: 9, letterSpacing: 0.8, marginTop: spacing.sm },
  pressed: { opacity: 0.72, transform: [{ scale: 0.99 }] },
});
