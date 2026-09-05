import { Platform } from 'react-native';

export interface UpdateStatus {
  isAvailable: boolean;
  isDownloading: boolean;
  manifest?: Record<string, unknown>;
  error?: string;
}

/**
 * Checks for Over-The-Air (OTA) updates using expo-updates when available.
 * Safely falls back to no-op in development or web environments.
 */
export async function checkForOtaUpdate(): Promise<UpdateStatus> {
  if (Platform.OS === 'web' || __DEV__) {
    return { isAvailable: false, isDownloading: false };
  }

  try {
    // Dynamic import to prevent bundler errors when expo-updates native module isn't linked
    const moduleName = 'expo-updates';
    const Updates: any = await import(moduleName);
    if (!Updates?.isEnabled) {
      return { isAvailable: false, isDownloading: false };
    }

    const update = await Updates.checkForUpdateAsync();
    if (update?.isAvailable) {
      await Updates.fetchUpdateAsync();
      return {
        isAvailable: true,
        isDownloading: false,
        manifest: update.manifest as Record<string, unknown>,
      };
    }
    return { isAvailable: false, isDownloading: false };
  } catch (error) {
    return {
      isAvailable: false,
      isDownloading: false,
      error: error instanceof Error ? error.message : 'Unknown OTA check error',
    };
  }
}

/**
 * Reloads the app to apply downloaded OTA JavaScript / asset bundle.
 */
export async function applyOtaUpdate(): Promise<boolean> {
  if (Platform.OS === 'web' || __DEV__) {
    return false;
  }

  try {
    const moduleName = 'expo-updates';
    const Updates: any = await import(moduleName);
    if (Updates?.isEnabled) {
      await Updates.reloadAsync();
      return true;
    }
    return false;
  } catch {
    return false;
  }
}
