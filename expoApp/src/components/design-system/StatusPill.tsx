import React from 'react';
import { StyleSheet, Text, View, type TextStyle, type ViewStyle } from 'react-native';
import { useCustomization } from '../../theme/CustomizationContext';
import { radius as defaultRadius, spacing, type as typography } from '../../theme/tokens';

export type StatusTone = 'SAVED_LOWER' | 'SAVED_HIGHER' | 'COMPETITIVE' | 'REVIEW' | 'NEUTRAL';

export interface StatusPillProps {
  label: string;
  tone?: StatusTone;
  icon?: React.ReactNode;
  style?: ViewStyle;
  textStyle?: TextStyle;
}

export function StatusPill({ label, tone = 'NEUTRAL', icon, style, textStyle }: StatusPillProps) {
  const { colors } = useCustomization();

  let bg = 'rgba(100, 116, 139, 0.12)';
  let border = 'rgba(100, 116, 139, 0.28)';
  let text = colors.textMuted;

  switch (tone) {
    case 'SAVED_LOWER':
    case 'COMPETITIVE':
      bg = colors.isDark ? 'rgba(16, 185, 129, 0.14)' : 'rgba(16, 185, 129, 0.16)';
      border = colors.isDark ? 'rgba(16, 185, 129, 0.32)' : 'rgba(16, 185, 129, 0.45)';
      text = colors.isDark ? '#34D399' : '#059669';
      break;
    case 'SAVED_HIGHER':
    case 'REVIEW':
      bg = colors.isDark ? 'rgba(239, 68, 68, 0.14)' : 'rgba(239, 68, 68, 0.16)';
      border = colors.isDark ? 'rgba(239, 68, 68, 0.32)' : 'rgba(239, 68, 68, 0.45)';
      text = colors.isDark ? '#F87171' : '#DC2626';
      break;
    case 'NEUTRAL':
      bg = colors.surfaceRaised;
      border = colors.border;
      text = colors.textMuted;
      break;
  }

  return (
    <View style={[styles.pill, { backgroundColor: bg, borderColor: border }, style]}>
      {icon && <View style={styles.icon}>{icon}</View>}
      <Text style={[styles.label, { fontFamily: typography.bold, color: text }, textStyle]}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderRadius: defaultRadius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
  },
  icon: {
    marginRight: 4,
  },
  label: {
    fontWeight: '700',
    fontSize: 11,
    letterSpacing: 0.3,
    textTransform: 'uppercase',
  },
});
