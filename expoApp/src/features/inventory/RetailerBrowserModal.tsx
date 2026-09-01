import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Linking, Modal, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';

import { normalizeRetailerUrl } from '../../data/backup';
import type { PriceRetailer } from '../../domain/models';
import { colors, radius, spacing, type } from '../../theme/tokens';

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
  const details = SITES[site];
  const [currentUrl, setCurrentUrl] = useState(details.startUrl);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const webViewRef = useRef<WebView>(null);

  useEffect(() => {
    if (!visible) return;
    setCurrentUrl(details.startUrl);
    setLoading(Platform.OS !== 'web');
    setLoadError(null);
  }, [details.startUrl, visible]);

  const acceptedUrl = useMemo(
    () => normalizeRetailerUrl(currentUrl, details.retailer),
    [currentUrl, details.retailer],
  );

  const content = (
    <SafeAreaView style={styles.root} edges={['top', 'bottom']}>
      <View style={styles.dragHandle} />
      <View style={styles.header}>
        <Pressable accessibilityRole="button" accessibilityLabel={`Close ${details.name} browser`} onPress={onClose} style={styles.headerButton}>
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
          accessibilityState={{ disabled: !acceptedUrl }}
          disabled={!acceptedUrl}
          onPress={() => acceptedUrl && onUseLink(acceptedUrl)}
          style={[styles.headerButton, !acceptedUrl && styles.disabled]}
        >
          <Ionicons name="checkmark" size={27} color={acceptedUrl ? colors.primary : colors.textMuted} />
        </Pressable>
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
          <Text style={[styles.linkStatus, acceptedUrl ? styles.linkReady : styles.linkWaiting]}>
            {acceptedUrl ? 'Product link ready to use' : `Open a ${details.name} product page to continue`}
          </Text>
        </View>
      ) : (
        <>
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
        </>
      )}
    </SafeAreaView>
  );

  if (embedded) return visible ? content : null;
  return (
    <Modal visible={visible} animationType="slide" presentationStyle="fullScreen" onRequestClose={onClose}>
      {content}
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  dragHandle: { width: 42, height: 4, alignSelf: 'center', borderRadius: radius.pill, backgroundColor: 'rgba(148,163,184,0.55)', marginTop: 8 },
  header: { minHeight: 58, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: colors.border, backgroundColor: colors.surface, paddingHorizontal: spacing.sm },
  headerButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center' },
  headerCopy: { flex: 1, minWidth: 0, paddingHorizontal: 6 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 14 },
  url: { color: colors.primary, fontFamily: type.semibold, fontSize: 9, marginTop: 3 },
  loader: { marginHorizontal: 6 },
  disabled: { opacity: 0.42 },
  webView: { flex: 1, backgroundColor: '#FFFFFF' },
  errorBanner: { minHeight: 48, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: colors.border, backgroundColor: 'rgba(244,63,94,0.10)', paddingHorizontal: spacing.md },
  errorText: { flex: 1, color: colors.danger, fontFamily: type.regular, fontSize: 12, marginHorizontal: spacing.sm },
  retryButton: { minHeight: 38, justifyContent: 'center', paddingHorizontal: spacing.sm },
  retryText: { color: colors.primary, fontFamily: type.bold, fontSize: 12 },
  webFallback: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.xl },
  webTitle: { color: colors.text, fontFamily: type.bold, fontSize: 20, marginTop: spacing.lg },
  webBody: { maxWidth: 460, color: colors.textMuted, fontFamily: type.regular, fontSize: 14, lineHeight: 21, textAlign: 'center', marginTop: spacing.sm },
  openRetailerButton: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.primary, paddingHorizontal: spacing.xl, marginTop: spacing.xl },
  openRetailerText: { color: colors.background, fontFamily: type.bold, fontSize: 14, marginLeft: spacing.sm },
  webUrlInput: { width: '100%', maxWidth: 520, minHeight: 56, color: colors.text, fontFamily: type.regular, fontSize: 14, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.surface, paddingHorizontal: spacing.lg, marginTop: spacing.lg },
  linkStatus: { fontFamily: type.semibold, fontSize: 12, marginTop: spacing.sm },
  linkReady: { color: colors.primary },
  linkWaiting: { color: colors.textMuted },
});
