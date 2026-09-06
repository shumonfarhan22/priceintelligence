import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, Easing, Image, StyleSheet, Text, View } from 'react-native';
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
  const [splashVisible, setSplashVisible] = useState(true);

  const handleSplashFinished = useCallback(() => {
    SplashScreen.hideAsync().catch(() => {});
    setSplashVisible(false);
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar style={splashVisible ? 'light' : colors.isDark ? 'light' : 'dark'} />
      <RootApp fontFallback={fontFallback} />
      {splashVisible ? (
        <SplashOverlay onFinished={handleSplashFinished} />
      ) : null}
    </View>
  );
}

function SplashOverlay({ onFinished }: { onFinished: () => void }) {
  const contentOpacity = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    const timer = setTimeout(() => {
      Animated.timing(contentOpacity, {
        toValue: 0,
        duration: 250,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start(() => {
        onFinished();
      });
    }, 1300);

    return () => clearTimeout(timer);
  }, [contentOpacity, onFinished]);

  return (
    <Animated.View
      pointerEvents="none"
      style={[
        StyleSheet.absoluteFill,
        styles.splashContainer,
        { opacity: contentOpacity },
      ]}
    >
      <View style={styles.splashCenter} />

      <View style={styles.splashBottom}>
        <Image
          source={require('./assets/brand/splash_branding.png')}
          style={styles.splashBranding}
          resizeMode="contain"
          accessibilityLabel="Price Intelligence Branding"
        />
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  splashContainer: {
    backgroundColor: 'transparent',
    zIndex: 999999,
  },
  splashCenter: {
    flex: 1,
  },
  splashBottom: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingBottom: 40,
  },
  splashBranding: {
    width: 220,
    height: 88,
  },
});

