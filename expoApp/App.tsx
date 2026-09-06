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
  const [splashVisible, setSplashVisible] = useState(true);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar style={splashVisible ? 'light' : colors.isDark ? 'light' : 'dark'} />
      <RootApp fontFallback={fontFallback} />
      {splashVisible ? (
        <SplashOverlay onFinished={() => setSplashVisible(false)} />
      ) : null}
    </View>
  );
}

function SplashOverlay({ onFinished }: { onFinished: () => void }) {
  const contentOpacity = useRef(new Animated.Value(1)).current;
  const containerOpacity = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    // Stage 1: Display full splash (logo + company branding) for 1.3 seconds
    const timer = setTimeout(() => {
      // Stage 2: Fade out logo & company branding against solid background (240ms)
      Animated.timing(contentOpacity, {
        toValue: 0,
        duration: 240,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start(() => {
        // Stage 3: Smoothly reveal the main launch screen (180ms)
        Animated.timing(containerOpacity, {
          toValue: 0,
          duration: 180,
          easing: Easing.out(Easing.ease),
          useNativeDriver: true,
        }).start(() => {
          onFinished();
        });
      });
    }, 1300);

    return () => clearTimeout(timer);
  }, [contentOpacity, containerOpacity, onFinished]);

  return (
    <Animated.View
      pointerEvents="none"
      style={[
        StyleSheet.absoluteFill,
        styles.splashContainer,
        { opacity: containerOpacity },
      ]}
    >
      <Animated.View style={[StyleSheet.absoluteFill, { opacity: contentOpacity }]}>
        {/* Exact Center Logo: Centered in the full screen, identical to native splash coordinates */}
        <View style={styles.centerContainer} pointerEvents="none">
          <Image
            source={require('./assets/brand/splash_logo.png')}
            style={styles.splashLogo}
            resizeMode="contain"
            accessibilityLabel="Price Intelligence Logo"
          />
        </View>

        {/* Bottom Company Branding: Independent layer pinned to bottom (SwiftUI parity) */}
        <View style={styles.bottomContainer} pointerEvents="none">
          <Image
            source={require('./assets/brand/splash_branding.png')}
            style={styles.splashBranding}
            resizeMode="contain"
            accessibilityLabel="Price Intelligence Branding"
          />
        </View>
      </Animated.View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  splashContainer: {
    backgroundColor: '#0B0F14',
    zIndex: 999999,
  },
  centerContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashLogo: {
    width: 150,
    height: 150,
  },
  bottomContainer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashBranding: {
    width: 220,
    height: 88,
  },
});

