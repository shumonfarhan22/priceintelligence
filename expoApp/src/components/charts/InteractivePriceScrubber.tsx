import React, { useState } from 'react';
import {
  PanResponder,
  StyleSheet,
  Text,
  View,
  type GestureResponderEvent,
  type PanResponderGestureState,
} from 'react-native';
import { useCustomization } from '../../theme/CustomizationContext';
import { formatRupees } from '../../domain/formatting';
import { radius, spacing, type as typography } from '../../theme/tokens';
import { triggerLightImpact } from '../../utils/haptics';

export interface ScrubberPoint {
  x: number;
  y: number;
  price: number;
  timestamp: number;
  label?: string;
  retailer?: string;
}

export interface InteractivePriceScrubberProps {
  width: number;
  height: number;
  points: ScrubberPoint[];
  onPointSelected?: (point: ScrubberPoint | null) => void;
}

export function InteractivePriceScrubber({
  width,
  height,
  points,
  onPointSelected,
}: InteractivePriceScrubberProps) {
  const { colors } = useCustomization();
  const [activePoint, setActivePoint] = useState<ScrubberPoint | null>(null);

  const updateClosestPoint = (touchX: number) => {
    if (points.length === 0) return;
    let closest = points[0];
    let minDiff = Math.abs(points[0].x - touchX);

    for (let i = 1; i < points.length; i++) {
      const diff = Math.abs(points[i].x - touchX);
      if (diff < minDiff) {
        minDiff = diff;
        closest = points[i];
      }
    }

    if (!activePoint || activePoint.timestamp !== closest.timestamp) {
      void triggerLightImpact();
    }
    setActivePoint(closest);
    onPointSelected?.(closest);
  };

  const panResponder = PanResponder.create({
    onStartShouldSetPanResponder: () => true,
    onMoveShouldSetPanResponder: () => true,
    onPanResponderGrant: (evt: GestureResponderEvent) => {
      updateClosestPoint(evt.nativeEvent.locationX);
    },
    onPanResponderMove: (evt: GestureResponderEvent, _gestureState: PanResponderGestureState) => {
      updateClosestPoint(evt.nativeEvent.locationX);
    },
    onPanResponderRelease: () => {
      setActivePoint(null);
      onPointSelected?.(null);
    },
    onPanResponderTerminate: () => {
      setActivePoint(null);
      onPointSelected?.(null);
    },
  });

  return (
    <View style={[StyleSheet.absoluteFill, { width, height }]} {...panResponder.panHandlers}>
      {activePoint && (
        <>
          {/* Vertical Indicator Line */}
          <View
            style={[
              styles.scrubberLine,
              {
                left: activePoint.x,
                height,
                backgroundColor: colors.primary,
              },
            ]}
          />

          {/* Focal Target Circle */}
          <View
            style={[
              styles.focalCircle,
              {
                left: activePoint.x - 7,
                top: activePoint.y - 7,
                borderColor: colors.primary,
                backgroundColor: colors.surface,
              },
            ]}
          />

          {/* Floating Tooltip */}
          <View
            style={[
              styles.tooltip,
              {
                left: Math.max(12, Math.min(activePoint.x - 55, width - 120)),
                top: Math.max(8, activePoint.y - 48),
                backgroundColor: colors.surfaceRaised,
                borderColor: colors.border,
              },
            ]}
          >
            <Text style={[styles.tooltipPrice, { fontFamily: typography.bold, color: colors.primary }]}>
              {formatRupees(activePoint.price)}
            </Text>
            {activePoint.retailer && (
              <Text style={[styles.tooltipSub, { fontFamily: typography.regular, color: colors.textMuted }]}>
                {activePoint.retailer}
              </Text>
            )}
          </View>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  scrubberLine: {
    position: 'absolute',
    top: 0,
    width: 1.5,
    opacity: 0.75,
  },
  focalCircle: {
    position: 'absolute',
    width: 14,
    height: 14,
    borderRadius: 7,
    borderWidth: 2.5,
  },
  tooltip: {
    position: 'absolute',
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
    borderRadius: radius.sm,
    borderWidth: 1,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 4,
    alignItems: 'center',
  },
  tooltipPrice: {
    fontWeight: '700',
  },
  tooltipSub: {
    fontSize: 10,
    marginTop: 1,
  },
});
