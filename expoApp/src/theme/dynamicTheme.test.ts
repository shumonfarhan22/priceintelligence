import { describe, expect, it } from 'vitest';
import { DEFAULT_APP_CUSTOMIZATION } from '../domain/customization';
import { fontScaleForTextSize, resolveColors, withAlpha } from './dynamicTheme';

describe('dynamicTheme', () => {
  describe('withAlpha', () => {
    it('appends alpha hex to 7-character hex code', () => {
      expect(withAlpha('#10B981', '26')).toBe('#10B98126');
    });

    it('returns original color if not 7-character hex', () => {
      expect(withAlpha('rgb(0,0,0)', '26')).toBe('rgb(0,0,0)');
    });
  });

  describe('resolveColors', () => {
    it('resolves dark theme tokens correctly', () => {
      const colors = resolveColors(DEFAULT_APP_CUSTOMIZATION, 'DARK');
      expect(colors.isDark).toBe(true);
      expect(colors.background).toBe('#0B0F14');
      expect(colors.surface).toBe('#14181D');
      expect(colors.surfaceRaised).toBe('#1E2128');
      expect(colors.border).toBe('#2A313C');
      expect(colors.primary).toBe('#10B981');
      expect(colors.text).toBe('#F8FAFC');
    });

    it('resolves Supreme Light theme tokens correctly', () => {
      const colors = resolveColors(DEFAULT_APP_CUSTOMIZATION, 'LIGHT');
      expect(colors.isDark).toBe(false);
      expect(colors.background).toBe('#F2EDE4');
      expect(colors.surface).toBe('#FFFFFF');
      expect(colors.surfaceRaised).toBe('#EAE3D5');
      expect(colors.border).toBe('#D6CEC0');
      expect(colors.primary).toBe('#10B981');
      expect(colors.text).toBe('#1C1917');
    });

    it('resolves SYSTEM theme mode based on systemIsDark', () => {
      const darkColors = resolveColors(DEFAULT_APP_CUSTOMIZATION, 'SYSTEM', true);
      expect(darkColors.isDark).toBe(true);
      expect(darkColors.background).toBe('#0B0F14');

      const lightColors = resolveColors(DEFAULT_APP_CUSTOMIZATION, 'SYSTEM', false);
      expect(lightColors.isDark).toBe(false);
      expect(lightColors.background).toBe('#F2EDE4');
    });

    it('supports high contrast mode in both dark and light modes', () => {
      const highContrastCustomization = {
        ...DEFAULT_APP_CUSTOMIZATION,
        insightCustomization: {
          ...DEFAULT_APP_CUSTOMIZATION.insightCustomization,
          contrastMode: 'HIGH' as const,
        },
      };

      const darkHigh = resolveColors(highContrastCustomization, 'DARK');
      expect(darkHigh.border).toBe('#475569');
      expect(darkHigh.text).toBe('#FFFFFF');

      const lightHigh = resolveColors(highContrastCustomization, 'LIGHT');
      expect(lightHigh.border).toBe('#78716C');
      expect(lightHigh.text).toBe('#000000');
    });

    it('applies OCEAN_COPPER palette', () => {
      const custom = {
        ...DEFAULT_APP_CUSTOMIZATION,
        appColorPalette: 'OCEAN_COPPER' as const,
      };
      const colors = resolveColors(custom, 'LIGHT');
      expect(colors.primary).toBe('#3B82F6');
      expect(colors.accent).toBe('#E08A5B');
    });

    it('applies ROYAL_AMETHYST palette', () => {
      const custom = {
        ...DEFAULT_APP_CUSTOMIZATION,
        appColorPalette: 'ROYAL_AMETHYST' as const,
      };
      const colors = resolveColors(custom, 'DARK');
      expect(colors.primary).toBe('#A855F7');
      expect(colors.accent).toBe('#D6A63D');
    });

    it('applies CUSTOM palette with user-defined hexes', () => {
      const custom = {
        ...DEFAULT_APP_CUSTOMIZATION,
        appColorPalette: 'CUSTOM' as const,
        customColorPalette: {
          primaryHex: '#FF5722',
          secondaryHex: '#00BCD4',
          competitiveHex: '#4CAF50',
          warningHex: '#FF9800',
          reviewHex: '#E91E63',
        },
      };
      const colors = resolveColors(custom, 'LIGHT');
      expect(colors.primary).toBe('#FF5722');
      expect(colors.accent).toBe('#00BCD4');
      expect(colors.competitive).toBe('#4CAF50');
      expect(colors.warning).toBe('#FF9800');
      expect(colors.danger).toBe('#E91E63');
    });
  });

  describe('fontScaleForTextSize', () => {
    it('returns expected scale factors', () => {
      expect(fontScaleForTextSize('STANDARD')).toBe(1.0);
      expect(fontScaleForTextSize('COMFORTABLE')).toBe(1.08);
      expect(fontScaleForTextSize('LARGE')).toBe(1.16);
    });
  });
});
