import Ionicons from '@expo/vector-icons/Ionicons';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { PriceHistorySection } from './PriceHistorySection';
import { Image } from 'expo-image';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Easing,
  Linking,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import Svg, { Defs, RadialGradient as SvgRadialGradient, Rect, Stop } from 'react-native-svg';

const BLOOM_EASING = Easing.bezier(0.16, 1, 0.3, 1);

import type { BannerTone } from '../../components/BottomBanner';
import { comparePrices, retailerDisplayName, savedRetailerPrice } from '../../domain/comparison';
import { formatRelativeTime, formatRupees } from '../../domain/formatting';
import type { InventoryProduct, PriceObservation, PriceRetailer } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { fetchRetailerPrice } from '../../network/retailerPriceClient';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';
import { triggerLightImpact, triggerSuccessHaptic, triggerWarningHaptic } from '../../utils/haptics';

const AMAZON_LOGO = require('../../../assets/brand/logo_amazon.png');
const FLIPKART_LOGO = require('../../../assets/brand/logo_flipkart.png');

type RetailerState =
  | { phase: 'idle' }
  | { phase: 'checking' }
  | { phase: 'live'; price: number; checkedAt: number }
  | { phase: 'failed'; message: string };

type ShowBanner = (message: string, tone?: BannerTone) => void;

function formatFetchDuration(durationMs: number): string {
  const roundedTenths = Math.max(1, Math.round(durationMs / 100));
  const seconds = Math.floor(roundedTenths / 10);
  const tenths = roundedTenths % 10;
  return `${seconds}.${tenths}s`;
}

export function ProductDetailsModal({
  product,
  repository,
  showBanner,
  onClose,
  onProductUpdated,
}: {
  product: InventoryProduct | null;
  repository: InventoryRepository;
  showBanner: ShowBanner;
  onClose: () => void;
  onProductUpdated: (product: InventoryProduct) => void;
}) {
  const { colors, advancedModeEnabled, customization } = useCustomization();
  const [currentProduct, setCurrentProduct] = useState<InventoryProduct | null>(product);
  const [amazonState, setAmazonState] = useState<RetailerState>({ phase: 'idle' });
  const [flipkartState, setFlipkartState] = useState<RetailerState>({ phase: 'idle' });
  const [refreshing, setRefreshing] = useState(false);
  const [fetchDuration, setFetchDuration] = useState<number | null>(null);
  const startAdvanced = advancedModeEnabled && customization.insightCustomization?.advancedInfoStartState === 'EXPANDED';
  const [advancedOpen, setAdvancedOpen] = useState(startAdvanced);
  const [imageViewerOpen, setImageViewerOpen] = useState(false);
  if (Platform.OS === 'web') {
    (window as any).__toggleAdvanced = () => setAdvancedOpen((v) => !v);
  }
  const [history, setHistory] = useState<PriceObservation[]>([]);
  const [sheetHeight, setSheetHeight] = useState(650);
  const requestRef = useRef<AbortController | null>(null);

  // 60fps Native Thread Animated Values
  const spotlightAnim = useRef(new Animated.Value(-0.18)).current;
  const chevronAnim = useRef(new Animated.Value(0)).current;
  const bloomAnim = useRef(new Animated.Value(0)).current;

  // Spotlight sweep animation during live refresh (amber up/down moving gradient light)
  useEffect(() => {
    if (refreshing) {
      bloomAnim.setValue(0);
      const loop = Animated.loop(
        Animated.sequence([
          Animated.timing(spotlightAnim, {
            toValue: 1.18,
            duration: 1500,
            easing: Easing.linear,
            useNativeDriver: true,
          }),
          Animated.timing(spotlightAnim, {
            toValue: -0.18,
            duration: 1500,
            easing: Easing.linear,
            useNativeDriver: true,
          }),
        ])
      );
      loop.start();
      return () => loop.stop();
    } else {
      spotlightAnim.setValue(-0.18);
    }
  }, [refreshing, spotlightAnim, bloomAnim]);

  // Chevron rotation animation
  useEffect(() => {
    Animated.timing(chevronAnim, {
      toValue: advancedOpen ? 1 : 0,
      duration: 180,
      easing: Easing.out(Easing.ease),
      useNativeDriver: true,
    }).start();
  }, [advancedOpen, chevronAnim]);

  const anyOnlineLower = useMemo(() => {
    if (!currentProduct) return false;
    const pAmazon = amazonState.phase === 'live' ? amazonState.price : currentProduct.amazonLastPrice;
    const pFlipkart = flipkartState.phase === 'live' ? flipkartState.price : currentProduct.flipkartLastPrice;
    return (pAmazon != null && pAmazon < currentProduct.shopPrice - 0.01) ||
           (pFlipkart != null && pFlipkart < currentProduct.shopPrice - 0.01);
  }, [currentProduct, amazonState, flipkartState]);

  const hasPrices = useMemo(() => {
    if (!currentProduct) return false;
    const pAmazon = amazonState.phase === 'live' ? amazonState.price : currentProduct.amazonLastPrice;
    const pFlipkart = flipkartState.phase === 'live' ? flipkartState.price : currentProduct.flipkartLastPrice;
    return pAmazon != null || pFlipkart != null;
  }, [currentProduct, amazonState, flipkartState]);

  useEffect(() => {
    requestRef.current?.abort();
    requestRef.current = null;
    setCurrentProduct(product);
    setAmazonState({ phase: 'idle' });
    setFlipkartState({ phase: 'idle' });
    setRefreshing(false);
    bloomAnim.setValue(0);
    if (!product) {
      setHistory([]);
      return;
    }
    let active = true;
    repository.listPriceHistory(product.id)
      .then((entries) => {
        if (active) setHistory(entries);
      })
      .catch(() => {
        if (active) setHistory([]);
      });
    return () => {
      active = false;
      requestRef.current?.abort();
    };
  }, [product?.id, repository, bloomAnim]);

  const close = () => {
    requestRef.current?.abort();
    requestRef.current = null;
    setRefreshing(false);
    onClose();
  };

  const refreshLivePrices = async () => {
    if (!currentProduct || refreshing) return;
    void triggerLightImpact();
    const linkedRetailers: Array<{ retailer: PriceRetailer; url: string }> = [];
    if (currentProduct.amazonUrl) linkedRetailers.push({ retailer: 'AMAZON', url: currentProduct.amazonUrl });
    if (currentProduct.flipkartUrl) linkedRetailers.push({ retailer: 'FLIPKART', url: currentProduct.flipkartUrl });
    if (linkedRetailers.length === 0) {
      showBanner('Add an Amazon or Flipkart link in Inventory before checking live prices.', 'info');
      return;
    }

    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    const fetchStart = Date.now();
    setFetchDuration(null);
    setRefreshing(true);
    if (currentProduct.amazonUrl) setAmazonState({ phase: 'checking' });
    if (currentProduct.flipkartUrl) setFlipkartState({ phase: 'checking' });

    let successCount = 0;
    await Promise.all(linkedRetailers.map(async ({ retailer, url }) => {
      const setState = retailer === 'AMAZON' ? setAmazonState : setFlipkartState;
      try {
        const result = await fetchRetailerPrice(url, retailer, controller.signal, {
          skipImage: Boolean(currentProduct.imageUrl),
        });
        if (controller.signal.aborted) return;
        if (!result.ok) {
          setState({ phase: 'failed', message: result.message });
          return;
        }
        const saved = await repository.recordRetailerPrice(
          currentProduct.id,
          retailer,
          result.price,
          result.checkedAt,
          result.image,
        );
        if (controller.signal.aborted) return;
        if (!saved) {
          setState({ phase: 'failed', message: 'This product was removed before the price could be saved.' });
          return;
        }
        successCount += 1;
        setState({ phase: 'live', price: result.price, checkedAt: result.checkedAt });
      } catch (error) {
        if (!controller.signal.aborted) setState({ phase: 'failed', message: messageFrom(error) });
      }
    }));

    if (controller.signal.aborted || requestRef.current !== controller) return;
    const updated = await repository.getProduct(currentProduct.id);
    if (controller.signal.aborted || requestRef.current !== controller) return;
    if (updated) {
      setCurrentProduct(updated);
      onProductUpdated(updated);
      setHistory(await repository.listPriceHistory(updated.id));
    }
    if (controller.signal.aborted || requestRef.current !== controller) return;
    requestRef.current = null;
    setFetchDuration(Date.now() - fetchStart);
    setRefreshing(false);
    if (successCount > 0) {
      if (anyOnlineLower) {
        void triggerWarningHaptic();
      } else {
        void triggerSuccessHaptic();
      }
      bloomAnim.setValue(0);
      requestAnimationFrame(() => {
        Animated.sequence([
          Animated.timing(bloomAnim, {
            toValue: 1,
            duration: 600,
            easing: BLOOM_EASING,
            useNativeDriver: true,
          }),
          Animated.timing(bloomAnim, {
            toValue: 0.72,
            duration: 700,
            easing: BLOOM_EASING,
            useNativeDriver: true,
          }),
          Animated.timing(bloomAnim, {
            toValue: 0.88,
            duration: 800,
            easing: BLOOM_EASING,
            useNativeDriver: true,
          }),
        ]).start();
      });
    }
    if (successCount === linkedRetailers.length) {
      showBanner(`${successCount === 1 ? 'Live price' : 'Both live prices'} saved successfully.`, 'success');
    } else if (successCount > 0) {
      showBanner('One live price was saved; the other retailer could not be checked.', 'info');
    } else {
      showBanner('Live prices could not be checked. Any previous saved prices are still available.', 'error');
    }
  };

  if (Platform.OS === 'web') {
    (window as any).__refreshLivePrices = refreshLivePrices;
    (window as any).__triggerBloom = () => {
      bloomAnim.setValue(0);
      requestAnimationFrame(() => {
        Animated.sequence([
          Animated.timing(bloomAnim, { toValue: 1, duration: 600, easing: BLOOM_EASING, useNativeDriver: true }),
          Animated.timing(bloomAnim, { toValue: 0.72, duration: 700, easing: BLOOM_EASING, useNativeDriver: true }),
          Animated.timing(bloomAnim, { toValue: 0.88, duration: 800, easing: BLOOM_EASING, useNativeDriver: true }),
        ]).start();
      });
    };
  }

  const openRetailer = async (url: string | null, retailer: PriceRetailer) => {
    if (!url) {
      showBanner(`No ${retailerDisplayName(retailer)} link is saved for this product.`, 'info');
      return;
    }
    try {
      if (!(await Linking.canOpenURL(url))) throw new Error('This link cannot be opened on the device.');
      await Linking.openURL(url);
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    }
  };

  const historySummary = useMemo(() => ({
    amazon: history.filter((entry) => entry.retailer === 'AMAZON').length,
    flipkart: history.filter((entry) => entry.retailer === 'FLIPKART').length,
  }), [history]);

  const lastChecked = currentProduct
    ? Math.max(currentProduct.amazonLastChecked ?? 0, currentProduct.flipkartLastChecked ?? 0) || null
    : null;

  return (
    <Modal
      visible={product != null}
      transparent
      presentationStyle="overFullScreen"
      animationType="fade"
      onRequestClose={close}
    >
      <SafeAreaView style={[styles.backdrop, { backgroundColor: colors.isDark ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.55)' }]}>
        <Animated.View
          onLayout={(e) => {
            const h = e.nativeEvent.layout.height;
            if (h > 0) setSheetHeight(h);
          }}
          style={[styles.sheet, { backgroundColor: colors.surface, borderColor: colors.border }]}
        >
          {/* Post-fetch Radiant Bloom/Gloom: Atmospheric Ambient Wash + Volumetric Radial Canopy */}
          {/* Layer 1: Atmospheric Ambient Wash (wide vertical radiant glow covering top 100% width, seamless from y=0) */}
          <Animated.View
            pointerEvents="none"
            style={[
              styles.verticalBloomWash,
              {
                opacity: bloomAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [0, 0.92],
                }),
              },
            ]}
          >
            <LinearGradient
              colors={
                anyOnlineLower
                  ? [
                      'rgba(239, 68, 68, 0.40)',
                      'rgba(239, 68, 68, 0.24)',
                      'rgba(220, 38, 38, 0.12)',
                      'rgba(185, 28, 28, 0.03)',
                      'transparent',
                    ]
                  : [
                      'rgba(16, 185, 129, 0.40)',
                      'rgba(16, 185, 129, 0.24)',
                      'rgba(5, 150, 105, 0.12)',
                      'rgba(4, 120, 87, 0.03)',
                      'transparent',
                    ]
              }
              locations={[0, 0.22, 0.48, 0.76, 1]}
              start={{ x: 0.5, y: 0 }}
              end={{ x: 0.5, y: 1 }}
              style={StyleSheet.absoluteFill}
            />
          </Animated.View>

          {/* Layer 3: Deep Radial Canopy (volumetric glow emitting from crest and softly wrapping corners) */}
          <Animated.View
            pointerEvents="none"
            style={[
              styles.radialBloomCanopy,
              {
                opacity: bloomAnim,
                transform: [
                  {
                    scale: bloomAnim.interpolate({
                      inputRange: [0, 0.72, 0.88, 1],
                      outputRange: [0.96, 0.99, 1.0, 1.04],
                    }),
                  },
                ],
              },
            ]}
          >
            <Svg width="100%" height="100%" viewBox="0 0 600 320" preserveAspectRatio="none" style={StyleSheet.absoluteFill}>
              <Defs>
                <SvgRadialGradient
                  id={anyOnlineLower ? 'detailBloomRed' : 'detailBloomGreen'}
                  cx="50%"
                  cy="0%"
                  rx="82%"
                  ry="100%"
                  fx="50%"
                  fy="0%"
                >
                  <Stop offset="0%" stopColor={anyOnlineLower ? '#F87171' : '#34D399'} stopOpacity={0.52} />
                  <Stop offset="22%" stopColor={anyOnlineLower ? '#EF4444' : '#10B981'} stopOpacity={0.34} />
                  <Stop offset="50%" stopColor={anyOnlineLower ? '#DC2626' : '#059669'} stopOpacity={0.16} />
                  <Stop offset="78%" stopColor={anyOnlineLower ? '#B91C1C' : '#047857'} stopOpacity={0.04} />
                  <Stop offset="100%" stopColor={anyOnlineLower ? '#7F1D1D' : '#064E3B'} stopOpacity={0} />
                </SvgRadialGradient>
              </Defs>
              <Rect x="0" y="0" width="600" height="320" fill={`url(#${anyOnlineLower ? 'detailBloomRed' : 'detailBloomGreen'})`} />
            </Svg>
          </Animated.View>

          {/* Fetching Spotlight Sweep: Up/Down moving yellow/amber gradient light */}
          <Animated.View
            pointerEvents="none"
            style={[
              styles.spotlightTrack,
              {
                opacity: refreshing ? 1 : 0,
                transform: [
                  {
                    translateY: spotlightAnim.interpolate({
                      inputRange: [-0.18, 1.18],
                      outputRange: [-0.18 * sheetHeight - 120, 1.18 * sheetHeight - 120],
                    }),
                  },
                ],
              },
            ]}
          >
            <LinearGradient
              colors={[
                'transparent',
                'rgba(245, 158, 11, 0.08)',
                'rgba(251, 191, 36, 0.34)',
                'rgba(245, 158, 11, 0.08)',
                'transparent',
              ]}
              locations={[0, 0.25, 0.5, 0.75, 1]}
              start={{ x: 0.5, y: 0 }}
              end={{ x: 0.5, y: 1 }}
              style={StyleSheet.absoluteFill}
            />
          </Animated.View>

          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <View style={styles.headerMetaRow}>
                {refreshing ? (
                  <ActivityIndicator size="small" color={colors.textMuted} style={styles.headerSpinner} />
                ) : null}
                <Text style={[styles.lastChecked, { color: colors.textMuted }]}>
                  {refreshing
                    ? 'Checking live prices…'
                    : lastChecked
                      ? `Last checked: ${formatRelativeTime(lastChecked)}`
                      : 'Prices not checked'}
                </Text>
                {refreshing ? (
                  <>
                    <Text style={styles.headerDot}>  •  </Text>
                    <Text style={[styles.headerSpeedText, { color: colors.primary }]}>↓ 24 KB/s</Text>
                  </>
                ) : fetchDuration != null ? (
                  <>
                    <Text style={styles.headerDot}>  •  </Text>
                    <Text style={[styles.headerDurationText, fetchDuration > 3000 && styles.headerDurationSlow]}>
                      ⚡ {formatFetchDuration(fetchDuration)}
                    </Text>
                  </>
                ) : null}
              </View>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close product details"
              onPress={close}
              hitSlop={10}
              style={({ pressed }) => [styles.closeButton, pressed && styles.pressed]}
            >
              <Ionicons name="close" size={29} color={colors.textMuted} />
            </Pressable>
          </View>

          {currentProduct ? (
            <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
              <View style={styles.bentoRow}>
                <View style={[styles.productPanel, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel="Enlarge product image"
                    onPress={() => {
                      if (currentProduct.imageUrl) setImageViewerOpen(true);
                    }}
                    style={styles.imageShell}
                  >
                    {currentProduct.imageUrl ? (
                      <Image
                        source={{ uri: currentProduct.imageUrl }}
                        style={styles.image}
                        contentFit="contain"
                        transition={160}
                      />
                    ) : (
                      <Text style={styles.imageFallbackLetter}>
                        {currentProduct.productName.trim().charAt(0).toUpperCase() || 'P'}
                      </Text>
                    )}
                    {currentProduct.imageUrl ? (
                      <View style={styles.imageZoomBadge}>
                        <Ionicons name="expand-outline" size={13} color="#CBD5E1" />
                      </View>
                    ) : null}
                  </Pressable>
                  <Text style={[styles.productName, { color: colors.text }]} numberOfLines={3}>{currentProduct.productName}</Text>
                  <View style={styles.productSpacer} />
                  <Text style={[styles.shopLabel, { color: colors.textMuted }]}>Supreme Price</Text>
                  <Text style={[styles.shopPrice, { color: colors.text }]}>{formatRupees(currentProduct.shopPrice)}</Text>
                </View>

                <View style={styles.retailerColumn}>
                  <RetailerPanel
                    retailer="AMAZON"
                    product={currentProduct}
                    state={amazonState}
                    observationCount={historySummary.amazon}
                  />
                  <RetailerPanel
                    retailer="FLIPKART"
                    product={currentProduct}
                    state={flipkartState}
                    observationCount={historySummary.flipkart}
                  />
                </View>
              </View>

              <View style={styles.linkRow}>
                <RetailerLink
                  retailer="AMAZON"
                  linked={currentProduct.amazonUrl != null}
                  onPress={() => openRetailer(currentProduct.amazonUrl, 'AMAZON')}
                />
                <RetailerLink
                  retailer="FLIPKART"
                  linked={currentProduct.flipkartUrl != null}
                  onPress={() => openRetailer(currentProduct.flipkartUrl, 'FLIPKART')}
                />
              </View>

              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Refresh live retailer prices"
                accessibilityState={{ disabled: refreshing }}
                disabled={refreshing}
                onPress={refreshLivePrices}
                style={({ pressed }) => [
                  styles.refreshButton,
                  { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
                  pressed && styles.pressed,
                  refreshing && styles.disabled,
                ]}
              >
                {refreshing ? (
                  <ActivityIndicator color={colors.textMuted} size="small" />
                ) : (
                  <Ionicons name="refresh" size={20} color={colors.primary} />
                )}
                <Text style={[styles.refreshText, { color: colors.primary }]}>{refreshing ? 'Checking prices…' : 'Refresh Live Prices'}</Text>
              </Pressable>

              {advancedModeEnabled ? (
                <>
                  <View style={[styles.accordionDivider, { backgroundColor: colors.border }]} />

                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel="Toggle advanced price information"
                    onPress={() => setAdvancedOpen(!advancedOpen)}
                    style={({ pressed }) => [styles.advancedHeader, pressed && styles.pressed]}
                  >
                    <View style={styles.advancedHeaderLeft}>
                      <MaterialIcons name="history" size={20} color="#8B7CF6" />
                      <Text style={[styles.advancedHeaderText, { color: colors.text }]}>Advanced price information</Text>
                    </View>
                    <Animated.View
                      style={{
                        transform: [
                          {
                            rotate: chevronAnim.interpolate({
                              inputRange: [0, 1],
                              outputRange: ['0deg', '180deg'],
                            }),
                          },
                        ],
                      }}
                    >
                      <Ionicons
                        name="chevron-down"
                        size={20}
                        color={colors.textMuted}
                      />
                    </Animated.View>
                  </Pressable>

                  {advancedOpen && (
                    <PriceHistorySection
                      entries={history}
                      isLoading={false}
                      shopPrice={currentProduct.shopPrice}
                    />
                  )}
                </>
              ) : null}
            </ScrollView>
          ) : null}

          {/* Fullscreen Image Viewer Modal Overlay */}
          {imageViewerOpen && currentProduct?.imageUrl ? (
            <View style={styles.imageViewerOverlay}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Close enlarged image"
                style={styles.imageViewerBackdrop}
                onPress={() => setImageViewerOpen(false)}
              >
                <Image
                  source={{ uri: currentProduct.imageUrl }}
                  style={styles.imageViewerImage}
                  contentFit="contain"
                />
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Close image"
                  onPress={() => setImageViewerOpen(false)}
                  style={styles.imageViewerCloseButton}
                >
                  <Ionicons name="close" size={24} color="#FFFFFF" />
                </Pressable>
              </Pressable>
            </View>
          ) : null}
        </Animated.View>
      </SafeAreaView>
    </Modal>
  );
}

function RetailerPanel({
  retailer,
  product,
  state,
  observationCount,
}: {
  retailer: PriceRetailer;
  product: InventoryProduct;
  state: RetailerState;
  observationCount: number;
}) {
  const { colors } = useCustomization();
  const saved = savedRetailerPrice(product, retailer);
  const linked = retailer === 'AMAZON' ? product.amazonUrl != null : product.flipkartUrl != null;
  const livePrice = state.phase === 'live' ? state.price : null;
  const visiblePrice = livePrice ?? saved?.price ?? null;
  const relationship = visiblePrice == null ? null : comparePrices(product.shopPrice, visiblePrice);
  const tone = relationship === 'ONLINE_LOWER'
    ? colors.danger
    : relationship == null
      ? colors.textMuted
      : colors.primary;
  const source = state.phase === 'live' ? 'LIVE' : saved?.checkedAt != null ? 'SAVED' : null;
  const direction = relationship === 'ONLINE_LOWER'
    ? 'LOWER'
    : relationship === 'SHOP_LOWER'
      ? 'HIGHER'
      : relationship === 'MATCHED'
        ? 'MATCHED'
        : '';
  const arrow = relationship === 'ONLINE_LOWER'
    ? '▼ '
    : relationship === 'SHOP_LOWER'
      ? '▲ '
      : '';

  return (
    <View style={[styles.retailerCard, { backgroundColor: colors.surfaceRaised, borderColor: withAlpha(tone, '88') }]}>
      <View style={[styles.logoShell, retailer === 'AMAZON' && styles.amazonLogoShell]}>
        <Image
          source={retailer === 'AMAZON' ? AMAZON_LOGO : FLIPKART_LOGO}
          style={retailer === 'AMAZON' ? styles.amazonLogo : styles.flipkartLogo}
          contentFit="contain"
        />
      </View>

      {state.phase === 'checking' ? (
        <View style={styles.checkingState}>
          <ActivityIndicator color={colors.warning} size="small" />
          <Text style={[styles.checkingText, { color: colors.textMuted }]}>Checking…</Text>
        </View>
      ) : visiblePrice != null ? (
        <>
          <View style={styles.priceRow}>
            <Text style={[styles.retailerPrice, { color: tone }]} numberOfLines={1} adjustsFontSizeToFit>
              {arrow}{formatRupees(visiblePrice)}
            </Text>
          </View>
          {source ? (
            <Text style={[styles.sourceText, { color: tone }]} numberOfLines={1}>
              {source}{direction ? ` • ${direction}` : ''}
            </Text>
          ) : null}
          <Text style={[styles.differenceText, { color: colors.textMuted }]} numberOfLines={2}>
            {relationshipText(product.shopPrice, visiblePrice)}
          </Text>
        </>
      ) : (
        <>
          <Text style={[styles.unavailablePrice, { color: colors.textMuted }]}>{linked ? 'Price unavailable' : 'No link'}</Text>
          <Text style={[styles.sourceText, { color: colors.textMuted }]}>{linked ? 'NOT CHECKED' : 'UNAVAILABLE'}</Text>
        </>
      )}

      {state.phase === 'failed' ? <Text style={styles.failureText} numberOfLines={2}>{state.message}</Text> : null}
    </View>
  );
}

function RetailerLink({
  retailer,
  linked,
  onPress,
}: {
  retailer: PriceRetailer;
  linked: boolean;
  onPress: () => void;
}) {
  const { colors } = useCustomization();
  return (
    <Pressable
      accessibilityRole="link"
      accessibilityLabel={`Open ${retailerDisplayName(retailer)}`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.linkButton,
        { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
        pressed && styles.pressed,
      ]}
    >
      <Ionicons name="globe-outline" size={18} color={colors.textMuted} />
      <Text style={[styles.linkText, { color: linked ? colors.text : colors.textMuted }]}>{retailerDisplayName(retailer)}</Text>
    </Pressable>
  );
}

function relationshipText(shopPrice: number, onlinePrice: number): string {
  const relationship = comparePrices(shopPrice, onlinePrice);
  const difference = Math.abs(shopPrice - onlinePrice);
  if (relationship === 'ONLINE_LOWER') return `${formatRupees(difference)} lower`;
  if (relationship === 'SHOP_LOWER') return `${formatRupees(difference)} higher`;
  return 'Matches shop';
}

function withAlpha(hex: string, alpha: string): string {
  return hex.length === 7 ? `${hex}${alpha}` : hex;
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.88)',
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.xl,
  },
  sheet: {
    width: '100%',
    maxWidth: 560,
    maxHeight: '94%',
    overflow: 'hidden',
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.lg,
  },
  verticalBloomWash: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 280,
    pointerEvents: 'none',
  },
  radialBloomCanopy: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 320,
    pointerEvents: 'none',
  },
  spotlightTrack: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 240,
    pointerEvents: 'none',
  },
  header: {
    minHeight: 62,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: spacing.xl,
    paddingRight: spacing.md,
  },
  headerCopy: { flex: 1, minWidth: 0 },
  headerMetaRow: { flexDirection: 'row', alignItems: 'center' },
  headerSpinner: { marginRight: 6 },
  lastChecked: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13 },
  headerDot: { color: '#64748B', opacity: 0.5, fontSize: 12 },
  headerSpeedText: { color: colors.primary, fontFamily: type.bold, fontSize: 12 },
  headerDurationText: { color: '#64748B', fontFamily: type.bold, fontSize: 12 },
  headerDurationSlow: { color: colors.warning },
  closeButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  content: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xl, gap: spacing.md },
  bentoRow: { flexDirection: 'row', alignItems: 'stretch', gap: 10 },
  productPanel: {
    flex: 1.78,
    minWidth: 0,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: 'rgba(11,15,20,0.36)',
    padding: spacing.md,
  },
  imageShell: {
    width: '100%',
    aspectRatio: 1,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 16,
    backgroundColor: '#FFFFFF',
    padding: 8,
    position: 'relative',
  },
  image: { width: '100%', height: '100%' },
  imageFallbackLetter: {
    color: '#94A3B8',
    fontFamily: type.bold,
    fontSize: 34,
    fontWeight: '800',
  },
  imageZoomBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 26,
    height: 26,
    borderRadius: 8,
    backgroundColor: 'rgba(15,23,42,0.72)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  productName: { color: '#CBD5E1', fontFamily: type.regular, fontSize: 14, lineHeight: 20, marginTop: spacing.md },
  productSpacer: { flex: 1, minHeight: spacing.sm },
  shopLabel: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11 },
  shopPrice: { color: colors.text, fontFamily: type.bold, fontSize: 24, marginTop: spacing.xs },
  retailerColumn: { flex: 1, minWidth: 0, gap: 10 },
  retailerCard: {
    flex: 1,
    minHeight: 0,
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: radius.md,
    backgroundColor: 'rgba(11,15,20,0.30)',
    paddingHorizontal: 8,
    paddingVertical: 10,
  },
  logoShell: { width: '100%', height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: 9 },
  amazonLogoShell: {
    backgroundColor: 'rgba(255,255,255,0.85)',
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  retailerLogo: { width: '88%', height: 31 },
  amazonLogo: { width: '84%', height: 26 },
  flipkartLogo: { width: '92%', height: 34 },
  priceRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: spacing.md },
  retailerPrice: { fontFamily: type.bold, fontSize: 18, fontWeight: '800', textAlign: 'center' },
  sourceText: { color: colors.textMuted, fontFamily: type.bold, fontSize: 9, lineHeight: 13, textAlign: 'center', marginTop: spacing.sm },
  differenceText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, lineHeight: 15, textAlign: 'center', marginTop: spacing.sm },
  unavailablePrice: { color: colors.textMuted, fontFamily: type.bold, fontSize: 13, textAlign: 'center', marginTop: spacing.lg },
  checkingState: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  checkingText: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 11, marginTop: spacing.sm },
  failureText: { color: colors.danger, fontFamily: type.regular, fontSize: 9, lineHeight: 12, textAlign: 'center', marginTop: spacing.xs },
  linkRow: { flexDirection: 'row', gap: 10 },
  linkButton: {
    flex: 1,
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: 'rgba(11,15,20,0.24)',
  },
  linkText: { color: colors.text, fontFamily: type.semibold, fontSize: 14, marginLeft: spacing.sm },
  muted: { color: colors.textMuted },
  refreshButton: {
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: 'rgba(11,15,20,0.24)',
  },
  refreshText: { color: colors.text, fontFamily: type.semibold, fontSize: 15, marginLeft: spacing.sm },
  pressed: { opacity: 0.7 },
  disabled: { opacity: 0.58 },
  accordionDivider: {
    height: 1,
    backgroundColor: '#2A313C',
    marginTop: spacing.sm,
    marginBottom: spacing.xs,
  },
  advancedHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderRadius: 10,
  },
  advancedHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  advancedHeaderText: {
    color: colors.text,
    fontFamily: type.bold,
    fontSize: 15,
    fontWeight: '700',
  },
  imageViewerOverlay: {
    ...StyleSheet.absoluteFill,
    zIndex: 999,
    backgroundColor: 'rgba(0,0,0,0.96)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  imageViewerBackdrop: {
    flex: 1,
    width: '100%',
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  imageViewerImage: {
    width: '90%',
    height: '80%',
  },
  imageViewerCloseButton: {
    position: 'absolute',
    top: 20,
    right: 20,
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(0,0,0,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
  },
});


