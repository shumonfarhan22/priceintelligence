import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useRef, useState } from 'react';
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
    // Stage 1: Display full splash for 1.3 seconds
    const timer = setTimeout(() => {
      // Stage 2: Fade out splash logo, title, and branding against the solid dark background (250ms)
      Animated.timing(contentOpacity, {
        toValue: 0,
        duration: 250,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start(() => {
        // Stage 3: Logo and title are 100% gone. Now smoothly reveal the launch screen (180ms)
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
      <Animated.View style={[styles.splashContent, { opacity: contentOpacity }]}>
        <View style={styles.splashCenter}>
          <Image
            source={require('./assets/brand/splash_logo.png')}
            style={styles.splashLogo}
            resizeMode="contain"
            accessibilityLabel="Price Intelligence Logo"
          />
        </View>

        <View style={styles.splashBottom}>
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
  splashContent: {
    flex: 1,
  },
  splashCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashLogo: {
    width: 150,
    height: 150,
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

