import * as Haptics from 'expo-haptics';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useEffect, useRef, useState } from 'react';
import {
  AccessibilityInfo,
  Animated,
  Easing,
  LayoutChangeEvent,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, radius, spacing, type } from '../theme/tokens';
import { useCustomization } from '../theme/CustomizationContext';

export type BannerTone = 'success' | 'info' | 'error';

export interface BannerNotice {
  id: number;
  message: string;
  tone: BannerTone;
  actionLabel?: string;
  onAction?: () => void;
}

export interface UndoNotice {
  id: number;
  itemCount: number;
  onUndo: () => void;
}

export interface CoordinatedBannersProps {
  notice: BannerNotice | null;
  onDismissNotice: () => void;
  undoNotice?: UndoNotice | null;
  onUndo?: () => void;
  bottomOffset?: number;
}

export function BottomBanner({
  notice,
  onDismiss,
  bottomOffset = 0,
  undoNotice = null,
  onUndo,
}: {
  notice: BannerNotice | null;
  onDismiss: () => void;
  bottomOffset?: number;
  undoNotice?: UndoNotice | null;
  onUndo?: () => void;
}) {
  const { colors } = useCustomization();
  const insets = useSafeAreaInsets();
  const [measuredUndoHeight, setMeasuredUndoHeight] = useState(58);
  const stackOffsetAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!notice) return;
    AccessibilityInfo.announceForAccessibility(notice.message);
    const feedback =
      notice.tone === 'error'
        ? Haptics.NotificationFeedbackType.Error
        : notice.tone === 'success'
        ? Haptics.NotificationFeedbackType.Success
        : Haptics.NotificationFeedbackType.Warning;
    Haptics.notificationAsync(feedback).catch(() => undefined);
  }, [notice?.id]);

  useEffect(() => {
    if (!undoNotice) return;
    const countText = `${undoNotice.itemCount} ${undoNotice.itemCount === 1 ? 'product' : 'products'} deleted`;
    AccessibilityInfo.announceForAccessibility(countText);
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning).catch(() => undefined);
  }, [undoNotice?.id]);

  useEffect(() => {
    Animated.timing(stackOffsetAnim, {
      toValue: undoNotice ? measuredUndoHeight + 8 : 0,
      duration: 180,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    }).start();
  }, [undoNotice, measuredUndoHeight, stackOffsetAnim]);

  const baseBottom = Math.max(insets.bottom, spacing.md) + bottomOffset;

  return (
    <>
      {/* Undo Banner (bottom layer) */}
      {undoNotice ? (
        <View
          accessibilityLiveRegion="polite"
          onLayout={(e: LayoutChangeEvent) => {
            const h = e.nativeEvent.layout.height;
            if (h > 0 && Math.abs(h - measuredUndoHeight) > 1) {
              setMeasuredUndoHeight(h);
            }
          }}
          style={[styles.wrapper, { bottom: baseBottom, zIndex: 101 }]}
        >
          <View style={[styles.banner, styles.undoBanner, { backgroundColor: colors.surfaceRaised, borderColor: 'rgba(239, 68, 68, 0.4)' }]}>
            <View style={styles.undoIconBox}>
              <MaterialIcons name="delete-outline" size={20} color={colors.danger} />
            </View>
            <Text style={[styles.message, { color: colors.text }]} numberOfLines={2}>
              {undoNotice.itemCount} {undoNotice.itemCount === 1 ? 'product' : 'products'} deleted
            </Text>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Undo deletion"
              onPress={() => {
                onUndo?.();
                undoNotice.onUndo?.();
              }}
              style={({ pressed }) => [styles.undoAction, pressed && styles.pressed]}
            >
              <Text style={styles.undoActionText}>UNDO</Text>
            </Pressable>
          </View>
        </View>
      ) : null}

      {/* Status Notice Banner (top layer, floats above Undo banner when both active) */}
      {notice ? (
        <Animated.View
          accessibilityLiveRegion="polite"
          style={[
            styles.wrapper,
            {
              bottom: Animated.add(new Animated.Value(baseBottom), stackOffsetAnim),
              zIndex: 102,
            },
          ]}
        >
          <StatusBannerContent notice={notice} onDismiss={onDismiss} />
        </Animated.View>
      ) : null}
    </>
  );
}

function StatusBannerContent({
  notice,
  onDismiss,
}: {
  notice: BannerNotice;
  onDismiss: () => void;
}) {
  const { colors } = useCustomization();
  const accent =
    notice.tone === 'error'
      ? colors.danger
      : notice.tone === 'success'
      ? colors.primary
      : colors.warning;

  return (
    <View style={[styles.banner, { backgroundColor: colors.surfaceRaised, borderColor: accent }]}>
      <View style={[styles.dot, { backgroundColor: accent }]} />
      <Text style={[styles.message, { color: colors.text }]} numberOfLines={3}>
        {notice.message}
      </Text>
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
          <Text style={[styles.close, { color: colors.textMuted }]}>×</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    left: spacing.lg,
    right: spacing.lg,
  },
  banner: {
    minHeight: 56,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceRaised,
    borderWidth: 1,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    ...Platform.select({
      web: { boxShadow: '0 5px 12px rgba(0, 0, 0, 0.35)' },
      default: {
        shadowColor: '#000',
        shadowOpacity: 0.35,
        shadowRadius: 12,
        shadowOffset: { width: 0, height: 5 },
        elevation: 10,
      },
    }),
  },
  undoBanner: {
    borderColor: 'rgba(239, 68, 68, 0.4)',
    backgroundColor: '#161B22',
  },
  undoIconBox: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(239, 68, 68, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  dot: { width: 9, height: 9, borderRadius: 5, marginRight: spacing.md },
  message: { flex: 1, color: colors.text, fontFamily: type.semibold, fontSize: 14, lineHeight: 19 },
  action: { minHeight: 44, justifyContent: 'center', paddingLeft: spacing.lg },
  actionText: { fontFamily: type.bold, fontSize: 14, letterSpacing: 0.4 },
  undoAction: {
    minHeight: 40,
    paddingHorizontal: 14,
    borderRadius: 8,
    backgroundColor: 'rgba(16, 185, 129, 0.15)',
    borderWidth: 1,
    borderColor: 'rgba(16, 185, 129, 0.35)',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: spacing.sm,
  },
  undoActionText: {
    color: colors.primary,
    fontFamily: type.bold,
    fontSize: 13,
    letterSpacing: 0.8,
  },
  close: { color: colors.textMuted, fontFamily: type.regular, fontSize: 28, lineHeight: 30, paddingLeft: spacing.md },
  pressed: { opacity: 0.65 },
});
