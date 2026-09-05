import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { RootApp } from './src/application/RootApp';
import { colors } from './src/theme/tokens';

import { CustomizationProvider, useCustomization } from './src/theme/CustomizationContext';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function App() {
  const [fontsLoaded, fontError] = useFonts({
    'Lato': require('./assets/brand/lato_regular.ttf'),
    'Lato-Semibold': require('./assets/brand/lato_semibold.ttf'),
    'Lato-Bold': require('./assets/brand/lato_bold.ttf'),
  });

  useEffect(() => {
    if (fontsLoaded || fontError) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [fontsLoaded, fontError]);

  if (!fontsLoaded && !fontError) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <CustomizationProvider>
        <AppContent fontFallback={!!fontError} />
      </CustomizationProvider>
    </SafeAreaProvider>
  );
}

function AppContent({ fontFallback }: { fontFallback: boolean }) {
  const { colors } = useCustomization();

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar style={colors.isDark ? 'light' : 'dark'} />
      <RootApp fontFallback={fontFallback} />
    </View>
  );
}
