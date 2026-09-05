export const colors = {
  background: '#0B0F14',
  surface: '#14181D',
  surfaceRaised: '#1E2128',
  border: '#2A313C',
  primary: '#10B981',
  primaryMuted: '#123B32',
  accent: '#8B7CF6',
  warning: '#F59E0B',
  danger: '#F87171',
  text: '#F8FAFC',
  textMuted: '#7C8794',
  textLight: '#5B6472',
  scrim: 'rgba(0, 0, 0, 0.55)',
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const radius = {
  sm: 10,
  md: 16,
  lg: 24,
  pill: 999,
} as const;

export const type = {
  regular: 'Lato',
  semibold: 'Lato-Semibold',
  bold: 'Lato-Bold',
} as const;
