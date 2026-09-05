import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Platform, useColorScheme } from 'react-native';

import type {
  AppCustomization,
  AppThemeMode,
  PersonalizationPreset,
  SavedPersonalizationPreset,
} from '../domain/customization';
import {
  DEFAULT_APP_CUSTOMIZATION,
  MAX_SAVED_PERSONALIZATION_PRESETS,
  MAX_SAVED_PRESET_NAME_LENGTH,
  personalizationForPreset,
  readAppCustomization,
  writeAppCustomization,
} from '../domain/customization';
import type { DynamicColors } from './dynamicTheme';
import { fontScaleForTextSize, resolveColors } from './dynamicTheme';

const PREFS_STORAGE_KEY = 'supreme_price_intelligence_customization';
const THEME_STORAGE_KEY = 'supreme_price_intelligence_theme_mode';
const ADVANCED_STORAGE_KEY = 'supreme_price_intelligence_advanced_mode';
const NOTIF_STORAGE_KEY = 'supreme_price_intelligence_notif_enabled';

export interface CustomizationContextType {
  customization: AppCustomization;
  themeMode: AppThemeMode;
  advancedModeEnabled: boolean;
  priceChangeNotificationsEnabled: boolean;
  colors: DynamicColors;
  fontScale: number;
  updateCustomization: (updater: (prev: AppCustomization) => AppCustomization) => void;
  setThemeMode: (mode: AppThemeMode) => void;
  setAdvancedModeEnabled: (enabled: boolean) => void;
  setPriceChangeNotificationsEnabled: (enabled: boolean) => void;
  applyPreset: (preset: PersonalizationPreset) => void;
  saveNamedSetup: (name: string) => boolean;
  applyNamedSetup: (setup: SavedPersonalizationPreset) => void;
  renameNamedSetup: (oldName: string, newName: string) => boolean;
  deleteNamedSetup: (name: string) => void;
  resetAll: () => void;
}

const CustomizationContext = createContext<CustomizationContextType | null>(null);

export function CustomizationProvider({ children }: { children: React.ReactNode }) {
  const existing = useContext(CustomizationContext);
  if (existing) {
    return <>{children}</>;
  }

  const systemColorScheme = useColorScheme();
  const systemIsDark = systemColorScheme !== 'light';

  const [customization, setCustomization] = useState<AppCustomization>(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        const stored = window.localStorage.getItem(PREFS_STORAGE_KEY);
        if (stored) return readAppCustomization(stored);
      } catch {}
    }
    return DEFAULT_APP_CUSTOMIZATION;
  });

  const [themeMode, setThemeModeState] = useState<AppThemeMode>(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
        if (stored === 'LIGHT' || stored === 'DARK' || stored === 'SYSTEM') return stored;
      } catch {}
    }
    return 'DARK';
  });

  const [advancedModeEnabled, setAdvancedModeState] = useState<boolean>(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        const stored = window.localStorage.getItem(ADVANCED_STORAGE_KEY);
        if (stored != null) return stored === 'true';
      } catch {}
    }
    return true;
  });

  const [priceChangeNotificationsEnabled, setNotificationsState] = useState<boolean>(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        const stored = window.localStorage.getItem(NOTIF_STORAGE_KEY);
        if (stored != null) return stored === 'true';
      } catch {}
    }
    return false;
  });

  // Save changes to localStorage on web
  useEffect(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(PREFS_STORAGE_KEY, writeAppCustomization(customization));
      } catch {}
    }
  }, [customization]);

  const setThemeMode = useCallback((mode: AppThemeMode) => {
    setThemeModeState(mode);
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(THEME_STORAGE_KEY, mode);
      } catch {}
    }
  }, []);

  const setAdvancedModeEnabled = useCallback((enabled: boolean) => {
    setAdvancedModeState(enabled);
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(ADVANCED_STORAGE_KEY, String(enabled));
      } catch {}
    }
  }, []);

  const setPriceChangeNotificationsEnabled = useCallback((enabled: boolean) => {
    setNotificationsState(enabled);
    if (Platform.OS === 'web' && typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(NOTIF_STORAGE_KEY, String(enabled));
      } catch {}
    }
  }, []);

  const updateCustomization = useCallback((updater: (prev: AppCustomization) => AppCustomization) => {
    setCustomization((prev) => updater(prev));
  }, []);

  const applyPreset = useCallback((preset: PersonalizationPreset) => {
    const next = personalizationForPreset(preset);
    setCustomization((prev) => ({
      ...next,
      savedPersonalizationPresets: prev.savedPersonalizationPresets,
    }));
  }, []);

  const customizationRef = useRef(customization);
  customizationRef.current = customization;

  const saveNamedSetup = useCallback((name: string): boolean => {
    const cleanName = name.trim().slice(0, MAX_SAVED_PRESET_NAME_LENGTH);
    if (!cleanName) return false;

    const current = customizationRef.current;
    const exists = current.savedPersonalizationPresets.some(
      (p) => p.name.toLowerCase() === cleanName.toLowerCase()
    );
    if (exists || current.savedPersonalizationPresets.length >= MAX_SAVED_PERSONALIZATION_PRESETS) {
      return false;
    }

    const setup: SavedPersonalizationPreset = {
      name: cleanName,
      themeMode,
      advancedModeEnabled,
      priceChangeNotificationsEnabled,
      customizationProfile: writeAppCustomization(current),
    };

    setCustomization((prev) => ({
      ...prev,
      savedPersonalizationPresets: [...prev.savedPersonalizationPresets, setup],
    }));
    return true;
  }, [advancedModeEnabled, priceChangeNotificationsEnabled, themeMode]);

  const applyNamedSetup = useCallback((setup: SavedPersonalizationPreset) => {
    const restored = readAppCustomization(setup.customizationProfile);
    setThemeMode(setup.themeMode);
    setAdvancedModeEnabled(setup.advancedModeEnabled);
    setPriceChangeNotificationsEnabled(setup.priceChangeNotificationsEnabled);
    setCustomization((prev) => ({
      ...restored,
      savedPersonalizationPresets: prev.savedPersonalizationPresets,
    }));
  }, [setAdvancedModeEnabled, setNotificationsState, setThemeMode]);

  const renameNamedSetup = useCallback((oldName: string, newName: string): boolean => {
    const cleanName = newName.trim().slice(0, MAX_SAVED_PRESET_NAME_LENGTH);
    if (!cleanName) return false;

    const current = customizationRef.current;
    const exists = current.savedPersonalizationPresets.some(
      (p) => p.name.toLowerCase() === cleanName.toLowerCase() && p.name.toLowerCase() !== oldName.toLowerCase()
    );
    if (exists) return false;

    setCustomization((prev) => ({
      ...prev,
      savedPersonalizationPresets: prev.savedPersonalizationPresets.map((p) =>
        p.name === oldName ? { ...p, name: cleanName } : p
      ),
    }));
    return true;
  }, []);

  const deleteNamedSetup = useCallback((name: string) => {
    setCustomization((prev) => ({
      ...prev,
      savedPersonalizationPresets: prev.savedPersonalizationPresets.filter((p) => p.name !== name),
    }));
  }, []);

  const resetAll = useCallback(() => {
    setThemeMode('DARK');
    setAdvancedModeEnabled(true);
    setPriceChangeNotificationsEnabled(false);
    setCustomization((prev) => ({
      ...DEFAULT_APP_CUSTOMIZATION,
      savedPersonalizationPresets: prev.savedPersonalizationPresets,
    }));
  }, [setAdvancedModeEnabled, setNotificationsState, setThemeMode]);

  const colors = useMemo(() => {
    return resolveColors(customization, themeMode, systemIsDark);
  }, [customization, systemIsDark, themeMode]);

  const fontScale = useMemo(() => {
    return fontScaleForTextSize(customization.textSize);
  }, [customization.textSize]);

  const value = useMemo(
    () => ({
      customization,
      themeMode,
      advancedModeEnabled,
      priceChangeNotificationsEnabled,
      colors,
      fontScale,
      updateCustomization,
      setThemeMode,
      setAdvancedModeEnabled,
      setPriceChangeNotificationsEnabled,
      applyPreset,
      saveNamedSetup,
      applyNamedSetup,
      renameNamedSetup,
      deleteNamedSetup,
      resetAll,
    }),
    [
      customization,
      themeMode,
      advancedModeEnabled,
      priceChangeNotificationsEnabled,
      colors,
      fontScale,
      updateCustomization,
      setThemeMode,
      setAdvancedModeEnabled,
      setPriceChangeNotificationsEnabled,
      applyPreset,
      saveNamedSetup,
      applyNamedSetup,
      renameNamedSetup,
      deleteNamedSetup,
      resetAll,
    ]
  );

  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    (window as any).__customization = value;
  }

  useEffect(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      (window as any).__customization = value;
    }
  }, [value]);

  return (
    <CustomizationContext.Provider value={value}>
      {children}
    </CustomizationContext.Provider>
  );
}

export function useCustomization(): CustomizationContextType {
  const ctx = useContext(CustomizationContext);
  if (!ctx) {
    throw new Error('useCustomization must be used within a CustomizationProvider');
  }
  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    (window as any).__customization = ctx;
  }
  return ctx;
}
