import * as Haptics from 'expo-haptics';
import { useEffect } from 'react';
import {
  AccessibilityInfo,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, radius, spacing, type } from '../theme/tokens';

export type BannerTone = 'success' | 'info' | 'error';

export interface BannerNotice {
  id: number;
  message: string;
  tone: BannerTone;
  actionLabel?: string;
  onAction?: () => void;
}

export function BottomBanner({
  notice,
  onDismiss,
}: {
  notice: BannerNotice | null;
  onDismiss: () => void;
}) {
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (!notice) return;
    AccessibilityInfo.announceForAccessibility(notice.message);
    const feedback = notice.tone === 'error'
      ? Haptics.NotificationFeedbackType.Error
      : notice.tone === 'success'
        ? Haptics.NotificationFeedbackType.Success
        : Haptics.NotificationFeedbackType.Warning;
    Haptics.notificationAsync(feedback).catch(() => undefined);
  }, [notice?.id]);

  if (!notice) return null;
  const accent = notice.tone === 'error'
    ? colors.danger
    : notice.tone === 'success'
      ? colors.primary
      : colors.warning;

  return (
    <View
      accessibilityLiveRegion="assertive"
      style={[styles.wrapper, { bottom: Math.max(insets.bottom, spacing.md) }]}
    >
      <View style={[styles.banner, { borderColor: accent }]}>
        <View style={[styles.dot, { backgroundColor: accent }]} />
        <Text style={styles.message} numberOfLines={3}>{notice.message}</Text>
        {notice.actionLabel && notice.onAction ? (
          <Pressable
            accessibilityRole="button"
            onPress={() => {
              notice.onAction?.();
              onDismiss();
            }}
            style={({ pressed }) => [styles.action, pressed && styles.pressed]}
          >
            <Text style={[styles.actionText, { color: accent }]}>{notice.actionLabel}</Text>
          </Pressable>
        ) : (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Dismiss notification"
            onPress={onDismiss}
            hitSlop={12}
          >
            <Text style={styles.close}>×</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    left: spacing.lg,
    right: spacing.lg,
    zIndex: 100,
  },
  banner: {
    minHeight: 58,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceRaised,
    borderWidth: 1,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    shadowColor: '#000',
    shadowOpacity: 0.35,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 5 },
    elevation: 10,
  },
  dot: { width: 9, height: 9, borderRadius: 5, marginRight: spacing.md },
  message: { flex: 1, color: colors.text, fontFamily: type.semibold, fontSize: 14, lineHeight: 19 },
  action: { minHeight: 44, justifyContent: 'center', paddingLeft: spacing.lg },
  actionText: { fontFamily: type.bold, fontSize: 14, letterSpacing: 0.4 },
  close: { color: colors.textMuted, fontFamily: type.regular, fontSize: 28, lineHeight: 30, paddingLeft: spacing.md },
  pressed: { opacity: 0.65 },
});
