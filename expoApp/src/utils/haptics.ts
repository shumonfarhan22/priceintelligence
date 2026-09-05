import { Platform } from 'react-native';
import * as Haptics from 'expo-haptics';

/**
 * Universal tactile feedback utility that safely executes on supported native devices
 * and smoothly falls back to no-op on web or unsupported hardware.
 */
export async function triggerSuccessHaptic(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
  } catch {
    // Non-fatal if device has no haptics hardware
  }
}

export async function triggerWarningHaptic(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
  } catch {
    // Non-fatal
  }
}

export async function triggerErrorHaptic(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
  } catch {
    // Non-fatal
  }
}

export async function triggerLightImpact(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  } catch {
    // Non-fatal
  }
}

export async function triggerMediumImpact(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
  } catch {
    // Non-fatal
  }
}

export async function triggerHeavyImpact(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
  } catch {
    // Non-fatal
  }
}

export async function triggerSelectionHaptic(): Promise<void> {
  if (Platform.OS === 'web') return;
  try {
    await Haptics.selectionAsync();
  } catch {
    // Non-fatal
  }
}
