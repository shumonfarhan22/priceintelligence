import React from 'react';
import { StyleSheet, View, type ViewProps, type ViewStyle } from 'react-native';
import { useCustomization } from '../../theme/CustomizationContext';
import { radius as defaultRadius, spacing } from '../../theme/tokens';

export type CardVariant = 'elevated' | 'outlined' | 'flat';
export type CardPadding = 'none' | 'sm' | 'md' | 'lg';
export type CardRadius = 'sm' | 'md' | 'lg';

export interface ThemedCardProps extends ViewProps {
  variant?: CardVariant;
  padding?: CardPadding;
  cardRadius?: CardRadius;
  style?: ViewStyle | ViewStyle[];
  children?: React.ReactNode;
}

export function ThemedCard({
  variant = 'outlined',
  padding = 'md',
  cardRadius = 'md',
  style,
  children,
  ...rest
}: ThemedCardProps) {
  const { colors } = useCustomization();

  const paddingVal = {
    none: 0,
    sm: spacing.sm,
    md: spacing.md,
    lg: spacing.lg,
  }[padding];

  const radiusVal = {
    sm: defaultRadius.sm,
    md: defaultRadius.md,
    lg: defaultRadius.lg,
  }[cardRadius];

  const variantStyle: ViewStyle = {
    backgroundColor: variant === 'elevated' ? colors.surfaceRaised : colors.surface,
    borderColor: variant === 'outlined' ? colors.border : 'transparent',
    borderWidth: variant === 'outlined' ? 1 : 0,
  };

  return (
    <View
      style={[
        styles.base,
        variantStyle,
        { padding: paddingVal, borderRadius: radiusVal },
        variant === 'elevated' && styles.shadow,
        style,
      ]}
      {...rest}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    overflow: 'hidden',
  },
  shadow: {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    elevation: 3,
  },
});
