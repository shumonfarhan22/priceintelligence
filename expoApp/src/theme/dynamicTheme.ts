import type { AppCustomization, AppThemeMode } from '../domain/customization';

export interface DynamicColors {
  background: string;
  surface: string;
  surfaceRaised: string;
  border: string;
  primary: string;
  primaryMuted: string;
  accent: string;
  competitive: string;
  warning: string;
  danger: string;
  text: string;
  textMuted: string;
  textLight: string;
  scrim: string;
  isDark: boolean;
}

export function withAlpha(hexColor: string, hexAlpha: string): string {
  if (hexColor.startsWith('#') && hexColor.length === 7) {
    return `${hexColor}${hexAlpha}`;
  }
  return hexColor;
}

export function resolveColors(
  customization: AppCustomization,
  themeMode: AppThemeMode,
  systemIsDark: boolean = true,
): DynamicColors {
  const isDark = themeMode === 'SYSTEM' ? systemIsDark : themeMode === 'DARK';
  const contrast = customization.insightCustomization.contrastMode;

  let primary = '#10B981';
  let secondary = '#8B7CF6';
  let competitive = '#34D399';
  let warning = '#F59E0B';
  let danger = '#FB7185';

  switch (customization.appColorPalette) {
    case 'SUPREME_HARMONY':
      primary = '#10B981';
      secondary = '#8B7CF6';
      competitive = '#34D399';
      warning = '#F59E0B';
      danger = '#FB7185';
      break;

    case 'OCEAN_COPPER':
      primary = '#3B82F6';
      secondary = '#E08A5B';
      competitive = '#14B8A6';
      warning = '#D6A63D';
      danger = '#F43F5E';
      break;

    case 'ROYAL_AMETHYST':
      primary = '#A855F7';
      secondary = '#D6A63D';
      competitive = '#22C55E';
      warning = '#F97316';
      danger = '#E11D48';
      break;

    case 'CUSTOM': {
      const p = customization.customColorPalette;
      primary = p.primaryHex || '#10B981';
      secondary = p.secondaryHex || '#8B7CF6';
      competitive = p.competitiveHex || '#34D399';
      warning = p.warningHex || '#F59E0B';
      danger = p.reviewHex || '#FB7185';
      break;
    }
  }

  const primaryMuted = isDark ? withAlpha(primary, '26') : withAlpha(primary, '1E');

  if (isDark) {
    return {
      background: '#0B0F14',
      surface: '#14181D',
      surfaceRaised: '#1E2128',
      border: contrast === 'HIGH' ? '#475569' : '#2A313C',
      primary,
      primaryMuted,
      accent: secondary,
      competitive,
      warning,
      danger,
      text: contrast === 'HIGH' ? '#FFFFFF' : '#F8FAFC',
      textMuted: '#7C8794',
      textLight: '#5B6472',
      scrim: 'rgba(0, 0, 0, 0.65)',
      isDark: true,
    };
  }

  return {
    background: '#F2EDE4',
    surface: '#FFFFFF',
    surfaceRaised: '#EAE3D5',
    border: contrast === 'HIGH' ? '#78716C' : '#D6CEC0',
    primary,
    primaryMuted,
    accent: secondary,
    competitive,
    warning,
    danger,
    text: contrast === 'HIGH' ? '#000000' : '#1C1917',
    textMuted: '#78716C',
    textLight: '#A8A29E',
    scrim: 'rgba(0, 0, 0, 0.45)',
    isDark: false,
  };
}

export function fontScaleForTextSize(textSize: AppCustomization['textSize']): number {
  switch (textSize) {
    case 'STANDARD':
      return 1.0;
    case 'COMFORTABLE':
      return 1.08;
    case 'LARGE':
      return 1.16;
  }
}
