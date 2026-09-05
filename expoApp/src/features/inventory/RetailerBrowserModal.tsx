import Ionicons from '@expo/vector-icons/Ionicons';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Easing,
  Linking,
  Modal,
  PanResponder,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';

import { normalizeRetailerUrl } from '../../data/backup';
import type { PriceRetailer } from '../../domain/models';
import { buildCanonicalProductUrl } from '../../network/retailerRequestStrategy';
import { radius, spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';
import type { DynamicColors } from '../../theme/dynamicTheme';

export type RetailerBrowserSite = 'AMAZON' | 'FLIPKART';

const SITES: Record<RetailerBrowserSite, { name: string; startUrl: string; retailer: PriceRetailer }> = {
  AMAZON: { name: 'Amazon India', startUrl: 'https://www.amazon.in/', retailer: 'AMAZON' },
  FLIPKART: { name: 'Flipkart', startUrl: 'https://www.flipkart.com/', retailer: 'FLIPKART' },
};

export function RetailerBrowserModal({
  visible,
  site,
  onUseLink,
  onClose,
  embedded = false,
}: {
  visible: boolean;
  site: RetailerBrowserSite;
  onUseLink: (url: string) => void;
  onClose: () => void;
  embedded?: boolean;
}) {
  const { colors } = useCustomization();
  const styles = useMemo(() => createStyles(colors), [colors]);
  const details = SITES[site];
  const [currentUrl, setCurrentUrl] = useState(details.startUrl);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const webViewRef = useRef<WebView>(null);
  const insets = useSafeAreaInsets();
  const { height: windowHeight } = useWindowDimensions();
  const bannerAnim = useRef(new Animated.Value(0)).current;
  const translateY = useRef(new Animated.Value(windowHeight)).current;
  const isClosingRef = useRef(false);

  // Safe area top padding ensuring notch / dynamic island never overlap buttons
  const safeTopPadding = Math.max(insets.top, Platform.OS === 'ios' ? 48 : 24);

  const dismissModal = useCallback((callback?: () => void) => {
    if (isClosingRef.current) return;
    isClosingRef.current = true;
    Animated.timing(translateY, {
      toValue: windowHeight,
      duration: 220,
      easing: Easing.in(Easing.cubic),
      useNativeDriver: true,
    }).start(() => {
      isClosingRef.current = false;
      if (callback) {
        callback();
      } else {
        onClose();
      }
    });
  }, [onClose, translateY, windowHeight]);

  useEffect(() => {
    if (!visible) return;
    isClosingRef.current = false;
    setCurrentUrl(details.startUrl);
    setLoading(Platform.OS !== 'web');
    setLoadError(null);
    translateY.setValue(windowHeight);
    Animated.timing(translateY, {
      toValue: 0,
      duration: 280,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start();
  }, [details.startUrl, translateY, visible, windowHeight]);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_, gesture) => gesture.dy > 6 && Math.abs(gesture.dx) < 30,
      onPanResponderMove: (_, gesture) => {
        if (gesture.dy > 0) {
          translateY.setValue(gesture.dy);
        }
      },
      onPanResponderRelease: (_, gesture) => {
        if (gesture.dy > 120 || gesture.vy > 0.5) {
          dismissModal();
        } else {
          Animated.spring(translateY, {
            toValue: 0,
            damping: 22,
            stiffness: 240,
            useNativeDriver: true,
          }).start();
        }
      },
    })
  ).current;

  const acceptedUrl = useMemo(
    () => normalizeRetailerUrl(currentUrl, details.retailer),
    [currentUrl, details.retailer],
  );

  const canonicalProductUrl = useMemo(
    () => buildCanonicalProductUrl(currentUrl, details.retailer),
    [currentUrl, details.retailer],
  );

  const bestUrlToUse = canonicalProductUrl || acceptedUrl;

  useEffect(() => {
    if (canonicalProductUrl) {
      Animated.spring(bannerAnim, {
        toValue: 1,
        damping: 18,
        stiffness: 220,
        useNativeDriver: true,
      }).start();
    } else {
      Animated.timing(bannerAnim, {
        toValue: 0,
        duration: 160,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start();
    }
  }, [bannerAnim, canonicalProductUrl]);

  const content = (
    <Animated.View
      style={[
        styles.root,
        {
          paddingTop: safeTopPadding,
          transform: [{ translateY }],
        },
      ]}
    >
      <View style={styles.topArea} {...panResponder.panHandlers}>
        <View style={styles.dragHandleTouchArea}>
          <View style={styles.dragHandle} />
        </View>
        <View style={styles.header}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Close ${details.name} browser`}
            onPress={() => dismissModal()}
            style={styles.headerButton}
          >
            <Ionicons name="close" size={26} color={colors.text} />
          </Pressable>

          <View style={styles.headerCopy}>
            <Text style={styles.title}>{details.name}</Text>
            <Text style={styles.url} numberOfLines={1}>INCOGNITO  •  {currentUrl}</Text>
          </View>

          {loading ? <ActivityIndicator size="small" color={colors.primary} style={styles.loader} /> : null}
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Use this ${details.name} link`}
            accessibilityState={{ disabled: !bestUrlToUse }}
            disabled={!bestUrlToUse}
            onPress={() => bestUrlToUse && dismissModal(() => onUseLink(bestUrlToUse))}
            style={[styles.headerButton, !bestUrlToUse && styles.disabled]}
          >
            <Ionicons name="checkmark" size={27} color={bestUrlToUse ? colors.primary : colors.textMuted} />
          </Pressable>
        </View>
      </View>

      {Platform.OS === 'web' ? (
        <View style={styles.webFallback}>
          <Ionicons name="globe-outline" size={42} color={colors.primary} />
          <Text style={styles.webTitle}>Retailer browser preview</Text>
          <Text style={styles.webBody}>The secure in-app browser runs on Android and iPhone. For browser testing, open the retailer and paste its product link here.</Text>
          <Pressable accessibilityRole="link" onPress={() => Linking.openURL(details.startUrl)} style={styles.openRetailerButton}>
            <Ionicons name="open-outline" size={19} color={colors.background} />
            <Text style={styles.openRetailerText}>Open {details.name}</Text>
          </Pressable>
          <TextInput
            value={currentUrl}
            onChangeText={setCurrentUrl}
            autoCapitalize="none"
            autoCorrect={false}
            inputMode="url"
            selectionColor={colors.primary}
            style={styles.webUrlInput}
            accessibilityLabel={`${details.name} product link`}
          />
          <Text style={[styles.linkStatus, bestUrlToUse ? styles.linkReady : styles.linkWaiting]}>
            {canonicalProductUrl
              ? `Canonical product link detected: ${canonicalProductUrl}`
              : bestUrlToUse
              ? 'Product link ready to use'
              : `Open a ${details.name} product page to continue`}
          </Text>
          {bestUrlToUse ? (
            <Pressable
              accessibilityRole="button"
              onPress={() => onUseLink(bestUrlToUse)}
              style={styles.useWebLinkButton}
            >
              <Ionicons name="checkmark-circle" size={18} color="#0B0F14" style={{ marginRight: 6 }} />
              <Text style={styles.useWebLinkText}>Use this product link</Text>
            </Pressable>
          ) : null}
        </View>
      ) : (
        <View style={styles.webViewContainer}>
          {loadError ? (
            <View style={styles.errorBanner}>
              <Ionicons name="warning-outline" size={19} color={colors.danger} />
              <Text style={styles.errorText}>{loadError}</Text>
              <Pressable accessibilityRole="button" onPress={() => webViewRef.current?.reload()} style={styles.retryButton}>
                <Text style={styles.retryText}>Retry</Text>
              </Pressable>
            </View>
          ) : null}
          <WebView
            ref={webViewRef}
            source={{ uri: details.startUrl }}
            style={styles.webView}
            originWhitelist={['https://*']}
            incognito
            cacheEnabled={false}
            sharedCookiesEnabled={false}
            thirdPartyCookiesEnabled={false}
            allowsBackForwardNavigationGestures
            pullToRefreshEnabled
            setSupportMultipleWindows={false}
            onShouldStartLoadWithRequest={(request) => /^https:\/\//i.test(request.url) || request.url === 'about:blank'}
            onNavigationStateChange={(navigation) => {
              if (/^https:\/\//i.test(navigation.url)) setCurrentUrl(navigation.url);
              setLoading(navigation.loading);
            }}
            onLoadStart={() => {
              setLoading(true);
              setLoadError(null);
            }}
            onLoadEnd={() => setLoading(false)}
            onError={() => {
              setLoading(false);
              setLoadError(`${details.name} could not be loaded. Check your connection and try again.`);
            }}
          />
        </View>
      )}

      {canonicalProductUrl ? (
        <Animated.View
          style={[
            styles.detectedDock,
            {
              bottom: Math.max(insets.bottom, 12) + 8,
              opacity: bannerAnim,
              transform: [
                {
                  translateY: bannerAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [60, 0],
                  }),
                },
              ],
            },
          ]}
        >
          <View style={styles.detectedDockContent}>
            <View style={styles.detectedIconShell}>
              <Ionicons name="checkmark-circle" size={24} color={colors.primary} />
            </View>
            <View style={styles.detectedCopy}>
              <Text style={styles.detectedTitle}>Product page detected</Text>
              <Text style={styles.detectedSub} numberOfLines={1}>
                {canonicalProductUrl}
              </Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Use this product URL"
              onPress={() => onUseLink(canonicalProductUrl)}
              style={({ pressed }) => [styles.detectedButton, pressed && styles.pressed]}
            >
              <Ionicons name="checkmark" size={18} color="#0B0F14" style={{ marginRight: 4 }} />
              <Text style={styles.detectedButtonText}>Use link</Text>
            </Pressable>
          </View>
        </Animated.View>
      ) : null}
    </Animated.View>
  );

  if (embedded) return visible ? content : null;
  return (
    <Modal visible={visible} transparent animationType="none" presentationStyle="overFullScreen" onRequestClose={() => dismissModal()}>
      {content}
    </Modal>
  );
}

const createStyles = (colors: DynamicColors) =>
  StyleSheet.create({
    root: { flex: 1, backgroundColor: colors.background },
    topArea: {
      backgroundColor: colors.surface,
      borderTopLeftRadius: 18,
      borderTopRightRadius: 18,
    },
    dragHandleTouchArea: {
      paddingVertical: 10,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.surface,
    },
    dragHandle: {
      width: 44,
      height: 5,
      alignSelf: 'center',
      borderRadius: radius.pill,
      backgroundColor: 'rgba(148,163,184,0.65)',
    },
    header: { minHeight: 58, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: colors.border, backgroundColor: colors.surface, paddingHorizontal: spacing.sm },
    headerButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center' },
    headerCopy: { flex: 1, minWidth: 0, paddingHorizontal: 6 },
    title: { color: colors.text, fontFamily: type.bold, fontSize: 14 },
    url: { color: colors.primary, fontFamily: type.semibold, fontSize: 9, marginTop: 3 },
    loader: { marginHorizontal: 6 },
    disabled: { opacity: 0.42 },
    pressed: { opacity: 0.85, transform: [{ scale: 0.98 }] },
    webViewContainer: { flex: 1, position: 'relative' },
    webView: { flex: 1, backgroundColor: '#FFFFFF' },
    errorBanner: { minHeight: 48, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: colors.border, backgroundColor: 'rgba(244,63,94,0.10)', paddingHorizontal: spacing.md },
    errorText: { flex: 1, color: colors.danger, fontFamily: type.regular, fontSize: 12, marginHorizontal: spacing.sm },
    retryButton: { minHeight: 38, justifyContent: 'center', paddingHorizontal: spacing.sm },
    retryText: { color: colors.primary, fontFamily: type.bold, fontSize: 12 },
    webFallback: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.xl },
    webTitle: { color: colors.text, fontFamily: type.bold, fontSize: 20, marginTop: spacing.lg },
    webBody: { maxWidth: 460, color: colors.textMuted, fontFamily: type.regular, fontSize: 14, lineHeight: 21, textAlign: 'center', marginTop: spacing.sm },
    openRetailerButton: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.primary, paddingHorizontal: spacing.xl, marginTop: spacing.xl },
    openRetailerText: { color: colors.isDark ? '#022c22' : '#FFFFFF', fontFamily: type.bold, fontSize: 14, marginLeft: spacing.sm },
    webUrlInput: { width: '100%', maxWidth: 520, minHeight: 56, color: colors.text, fontFamily: type.regular, fontSize: 14, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.surface, paddingHorizontal: spacing.lg, marginTop: spacing.lg },
    linkStatus: { fontFamily: type.semibold, fontSize: 12, marginTop: spacing.sm, textAlign: 'center' },
    linkReady: { color: colors.primary },
    linkWaiting: { color: colors.textMuted },
    useWebLinkButton: { minHeight: 44, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.primary, paddingHorizontal: spacing.xl, marginTop: spacing.md },
    useWebLinkText: { color: colors.isDark ? '#022c22' : '#FFFFFF', fontFamily: type.bold, fontSize: 14 },
    detectedDock: { position: 'absolute', left: spacing.md, right: spacing.md, zIndex: 100 },
    detectedDockContent: {
      flexDirection: 'row',
      alignItems: 'center',
      borderRadius: 18,
      borderWidth: 1.5,
      borderColor: 'rgba(16,185,129,0.45)',
      backgroundColor: colors.isDark ? '#131821' : colors.surfaceRaised,
      paddingVertical: 10,
      paddingHorizontal: 12,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: 0.35,
      shadowRadius: 10,
      elevation: 8,
    },
    detectedIconShell: { width: 34, height: 34, borderRadius: 17, backgroundColor: 'rgba(16,185,129,0.14)', alignItems: 'center', justifyContent: 'center', marginRight: 10 },
    detectedCopy: { flex: 1, minWidth: 0, marginRight: 8 },
    detectedTitle: { color: colors.text, fontFamily: type.bold, fontSize: 13, fontWeight: '700' },
    detectedSub: { color: colors.primary, fontFamily: type.regular, fontSize: 10, marginTop: 1 },
    detectedButton: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.primary,
      paddingHorizontal: 14,
      paddingVertical: 9,
      borderRadius: radius.pill,
    },
    detectedButtonText: { color: colors.isDark ? '#022c22' : '#FFFFFF', fontFamily: type.bold, fontSize: 12, fontWeight: '700' },
  });

