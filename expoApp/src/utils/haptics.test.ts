import { describe, expect, it, vi } from 'vitest';

vi.mock('react-native', () => ({
  Platform: { OS: 'android' },
}));

vi.mock('expo-haptics', () => ({
  notificationAsync: vi.fn(),
  impactAsync: vi.fn(),
  selectionAsync: vi.fn(),
  NotificationFeedbackType: {
    Success: 'success',
    Warning: 'warning',
    Error: 'error',
  },
  ImpactFeedbackStyle: {
    Light: 'light',
    Medium: 'medium',
    Heavy: 'heavy',
  },
}));

import * as Haptics from 'expo-haptics';
import {
  triggerSuccessHaptic,
  triggerWarningHaptic,
  triggerErrorHaptic,
  triggerLightImpact,
  triggerMediumImpact,
  triggerHeavyImpact,
  triggerSelectionHaptic,
} from './haptics';

describe('haptics utility', () => {
  it('calls notificationAsync with Success on triggerSuccessHaptic', async () => {
    await triggerSuccessHaptic();
    expect(Haptics.notificationAsync).toHaveBeenCalledWith('success');
  });

  it('calls notificationAsync with Warning on triggerWarningHaptic', async () => {
    await triggerWarningHaptic();
    expect(Haptics.notificationAsync).toHaveBeenCalledWith('warning');
  });

  it('calls notificationAsync with Error on triggerErrorHaptic', async () => {
    await triggerErrorHaptic();
    expect(Haptics.notificationAsync).toHaveBeenCalledWith('error');
  });

  it('calls impactAsync with Light on triggerLightImpact', async () => {
    await triggerLightImpact();
    expect(Haptics.impactAsync).toHaveBeenCalledWith('light');
  });

  it('calls impactAsync with Medium on triggerMediumImpact', async () => {
    await triggerMediumImpact();
    expect(Haptics.impactAsync).toHaveBeenCalledWith('medium');
  });

  it('calls impactAsync with Heavy on triggerHeavyImpact', async () => {
    await triggerHeavyImpact();
    expect(Haptics.impactAsync).toHaveBeenCalledWith('heavy');
  });

  it('calls selectionAsync on triggerSelectionHaptic', async () => {
    await triggerSelectionHaptic();
    expect(Haptics.selectionAsync).toHaveBeenCalled();
  });
});
