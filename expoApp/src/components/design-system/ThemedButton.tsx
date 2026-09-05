import React, { useRef } from 'react';
import {
  ActivityIndicator,
  Animated,
  Pressable,
  StyleSheet,
  Text,
  type GestureResponderEvent,
  type TextStyle,
  type ViewStyle,
} from 'react-native';
import { useCustomization } from '../../theme/CustomizationContext';
import { radius as defaultRadius, spacing, type as typography } from '../../theme/tokens';
import { triggerLightImpact } from '../../utils/haptics';

export type ButtonVariant = 'primary' | 'secondary' | 'accent' | 'outline' | 'ghost';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ThemedButtonProps {
  label: string;
  onPress?: (event: GestureResponderEvent) => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  disabled?: boolean;
  icon?: React.ReactNode;
  hapticFeedback?: boolean;
  style?: ViewStyle | ViewStyle[];
  labelStyle?: TextStyle;
}

export function ThemedButton({
  label,
  onPress,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  icon,
  hapticFeedback = true,
  style,
  labelStyle,
}: ThemedButtonProps) {
  const { colors } = useCustomization();
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => {
    Animated.timing(scaleAnim, {
      toValue: 0.97,
      duration: 100,
      useNativeDriver: true,
    }).start();
  };

  const handlePressOut = () => {
    Animated.timing(scaleAnim, {
      toValue: 1,
      duration: 120,
      useNativeDriver: true,
    }).start();
  };

  const handlePress = (e: GestureResponderEvent) => {
    if (disabled || loading) return;
    if (hapticFeedback) {
      void triggerLightImpact();
    }
    onPress?.(e);
  };

  // Resolve Variant Colors
  let bg = colors.primary;
  let textCol = '#FFFFFF';
  let borderCol = 'transparent';

  switch (variant) {
    case 'primary':
      bg = colors.primary;
      textCol = colors.isDark ? '#000000' : '#FFFFFF';
      break;
    case 'secondary':
      bg = colors.surfaceRaised;
      textCol = colors.text;
      borderCol = colors.border;
      break;
    case 'accent':
      bg = colors.accent;
      textCol = '#FFFFFF';
      break;
    case 'outline':
      bg = 'transparent';
      textCol = colors.primary;
      borderCol = colors.primary;
      break;
    case 'ghost':
      bg = 'transparent';
      textCol = colors.text;
      break;
  }

  // Resolve Size Dimensions
  const sizeStyles = {
    sm: { paddingVertical: spacing.xs, paddingHorizontal: spacing.sm, minHeight: 34, fontSize: 13 },
    md: { paddingVertical: spacing.sm, paddingHorizontal: spacing.md, minHeight: 44, fontSize: 15 },
    lg: { paddingVertical: spacing.md, paddingHorizontal: spacing.lg, minHeight: 52, fontSize: 16 },
  }[size];

  return (
    <Animated.View style={[{ transform: [{ scale: scaleAnim }] }, style]}>
      <Pressable
        onPress={handlePress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        disabled={disabled || loading}
        style={[
          styles.buttonBase,
          {
            backgroundColor: bg,
            borderColor: borderCol,
            borderWidth: variant === 'outline' || variant === 'secondary' ? 1 : 0,
            borderRadius: defaultRadius.md,
            paddingVertical: sizeStyles.paddingVertical,
            paddingHorizontal: sizeStyles.paddingHorizontal,
            minHeight: sizeStyles.minHeight,
            opacity: disabled ? 0.45 : 1,
          },
        ]}
      >
        {loading ? (
          <ActivityIndicator size="small" color={textCol} />
        ) : (
          <>
            {icon && <Animated.View style={styles.iconContainer}>{icon}</Animated.View>}
            <Text
              style={[
                styles.labelText,
                { fontFamily: typography.semibold, color: textCol, fontSize: sizeStyles.fontSize },
                labelStyle,
              ]}
            >
              {label}
            </Text>
          </>
        )}
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  buttonBase: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  labelText: {
    fontWeight: '600',
  },
  iconContainer: {
    marginRight: 6,
  },
});
