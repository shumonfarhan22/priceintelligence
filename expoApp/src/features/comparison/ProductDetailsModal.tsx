import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Linking,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { BannerTone } from '../../components/BottomBanner';
import { comparePrices, retailerDisplayName, savedRetailerPrice } from '../../domain/comparison';
import { formatRelativeTime, formatRupees } from '../../domain/formatting';
import type { InventoryProduct, PriceObservation, PriceRetailer } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { fetchRetailerPrice } from '../../network/retailerPriceClient';
import { colors, radius, spacing, type } from '../../theme/tokens';

const AMAZON_LOGO = require('../../../assets/brand/logo_amazon.png');
const FLIPKART_LOGO = require('../../../assets/brand/logo_flipkart.png');

type RetailerState =
  | { phase: 'idle' }
  | { phase: 'checking' }
  | { phase: 'live'; price: number; checkedAt: number }
  | { phase: 'failed'; message: string };

type ShowBanner = (message: string, tone?: BannerTone) => void;

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
  const [currentProduct, setCurrentProduct] = useState<InventoryProduct | null>(product);
  const [amazonState, setAmazonState] = useState<RetailerState>({ phase: 'idle' });
  const [flipkartState, setFlipkartState] = useState<RetailerState>({ phase: 'idle' });
  const [refreshing, setRefreshing] = useState(false);
  const [history, setHistory] = useState<PriceObservation[]>([]);
  const requestRef = useRef<AbortController | null>(null);

  useEffect(() => {
    requestRef.current?.abort();
    requestRef.current = null;
    setCurrentProduct(product);
    setAmazonState({ phase: 'idle' });
    setFlipkartState({ phase: 'idle' });
    setRefreshing(false);
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
  }, [product?.id, repository]);

  const close = () => {
    requestRef.current?.abort();
    requestRef.current = null;
    setRefreshing(false);
    onClose();
  };

  const refreshLivePrices = async () => {
    if (!currentProduct || refreshing) return;
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
    setRefreshing(true);
    if (currentProduct.amazonUrl) setAmazonState({ phase: 'checking' });
    if (currentProduct.flipkartUrl) setFlipkartState({ phase: 'checking' });

    let successCount = 0;
    await Promise.all(linkedRetailers.map(async ({ retailer, url }) => {
      const setState = retailer === 'AMAZON' ? setAmazonState : setFlipkartState;
      try {
        const result = await fetchRetailerPrice(url, retailer, controller.signal);
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
    setRefreshing(false);
    if (successCount === linkedRetailers.length) {
      showBanner(`${successCount === 1 ? 'Live price' : 'Both live prices'} saved successfully.`, 'success');
    } else if (successCount > 0) {
      showBanner('One live price was saved; the other retailer could not be checked.', 'info');
    } else {
      showBanner('Live prices could not be checked. Any previous saved prices are still available.', 'error');
    }
  };

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
      <SafeAreaView style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <Text style={styles.lastChecked}>
                {refreshing
                  ? 'Checking live prices…'
                  : lastChecked
                    ? `Last checked: ${formatRelativeTime(lastChecked)}`
                    : 'Last checked: not yet'}
              </Text>
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
                <View style={styles.productPanel}>
                  <View style={styles.imageShell}>
                    {currentProduct.imageUrl ? (
                      <Image
                        source={{ uri: currentProduct.imageUrl }}
                        style={styles.image}
                        contentFit="contain"
                        transition={160}
                      />
                    ) : (
                      <Ionicons name="cube-outline" size={42} color="#626A76" />
                    )}
                    <View style={styles.imageShade} />
                  </View>
                  <Text style={styles.productName} numberOfLines={3}>{currentProduct.productName}</Text>
                  <View style={styles.productSpacer} />
                  <Text style={styles.shopLabel}>Supreme Price</Text>
                  <Text style={styles.shopPrice}>{formatRupees(currentProduct.shopPrice)}</Text>
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
                style={({ pressed }) => [styles.refreshButton, pressed && styles.pressed, refreshing && styles.disabled]}
              >
                {refreshing ? (
                  <ActivityIndicator color={colors.textMuted} size="small" />
                ) : (
                  <Ionicons name="refresh" size={20} color={colors.primary} />
                )}
                <Text style={styles.refreshText}>{refreshing ? 'Checking prices…' : 'Refresh Live Prices'}</Text>
              </Pressable>

            </ScrollView>
          ) : null}
        </View>
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
  const source = retailerSourceLabel(state, saved?.checkedAt ?? null, linked);
  const direction = relationship === 'ONLINE_LOWER'
    ? 'LOWER'
    : relationship === 'SHOP_LOWER'
      ? 'HIGHER'
      : relationship === 'MATCHED'
        ? 'MATCHED'
        : '';
  const directionIcon = relationship === 'ONLINE_LOWER'
    ? 'caret-down'
    : relationship === 'SHOP_LOWER'
      ? 'caret-up'
      : 'remove';

  return (
    <View style={[styles.retailerCard, { borderColor: withAlpha(tone, '88') }]}>
      <View style={[styles.logoShell, retailer === 'AMAZON' && styles.amazonLogoShell]}>
        <Image
          source={retailer === 'AMAZON' ? AMAZON_LOGO : FLIPKART_LOGO}
          style={styles.retailerLogo}
          contentFit="contain"
        />
      </View>

      {state.phase === 'checking' ? (
        <View style={styles.checkingState}>
          <ActivityIndicator color={colors.warning} size="small" />
          <Text style={styles.checkingText}>Checking…</Text>
        </View>
      ) : visiblePrice != null ? (
        <>
          <View style={styles.priceRow}>
            <Ionicons name={directionIcon} size={20} color={tone} />
            <Text style={[styles.retailerPrice, { color: tone }]} numberOfLines={1} adjustsFontSizeToFit>
              {formatRupees(visiblePrice)}
            </Text>
          </View>
          <Text style={[styles.sourceText, { color: tone }]} numberOfLines={2}>
            {source}{direction ? ` • ${direction}` : ''}
          </Text>
          <Text style={styles.differenceText} numberOfLines={2}>
            {relationshipText(product.shopPrice, visiblePrice)}
          </Text>
        </>
      ) : (
        <>
          <Text style={styles.unavailablePrice}>{linked ? 'Not checked' : 'No link'}</Text>
          <Text style={styles.sourceText}>{source}</Text>
        </>
      )}

      {state.phase === 'failed' ? <Text style={styles.failureText} numberOfLines={2}>{state.message}</Text> : null}
      <Text style={styles.observationText}>{observationCount} saved</Text>
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
  return (
    <Pressable
      accessibilityRole="link"
      accessibilityLabel={`Open ${retailerDisplayName(retailer)}`}
      onPress={onPress}
      style={({ pressed }) => [styles.linkButton, pressed && styles.pressed]}
    >
      <Ionicons name="globe-outline" size={18} color={colors.textMuted} />
      <Text style={[styles.linkText, !linked && styles.muted]}>{retailerDisplayName(retailer)}</Text>
    </Pressable>
  );
}

function retailerSourceLabel(state: RetailerState, savedCheckedAt: number | null, linked: boolean): string {
  if (state.phase === 'live') return 'LIVE';
  if (state.phase === 'checking') return 'CHECKING';
  if (state.phase === 'failed' && savedCheckedAt != null) return 'SAVED';
  if (state.phase === 'failed') return 'FAILED';
  if (savedCheckedAt != null) return 'SAVED';
  return linked ? 'NOT CHECKED' : 'UNAVAILABLE';
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
    backgroundColor: 'rgba(0,0,0,0.78)',
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.xl,
  },
  sheet: {
    width: '100%',
    maxWidth: 560,
    maxHeight: '94%',
    overflow: 'hidden',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
  },
  header: {
    minHeight: 62,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: spacing.xl,
    paddingRight: spacing.md,
  },
  headerCopy: { flex: 1, minWidth: 0 },
  lastChecked: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13 },
  closeButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  content: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xl, gap: spacing.md },
  bentoRow: { minHeight: 382, flexDirection: 'row', alignItems: 'stretch', gap: 10 },
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
    borderRadius: radius.sm,
    backgroundColor: '#F8FAFC',
  },
  image: { width: '100%', height: '100%' },
  imageShade: { ...StyleSheet.absoluteFill, pointerEvents: 'none', backgroundColor: 'rgba(0,0,0,0.10)' },
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
  amazonLogoShell: { backgroundColor: '#F2F2F0' },
  retailerLogo: { width: '88%', height: 31 },
  priceRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: spacing.md },
  retailerPrice: { flexShrink: 1, fontFamily: type.bold, fontSize: 21, marginLeft: 2 },
  sourceText: { color: colors.textMuted, fontFamily: type.bold, fontSize: 9, lineHeight: 13, textAlign: 'center', marginTop: spacing.sm },
  differenceText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, lineHeight: 15, textAlign: 'center', marginTop: spacing.sm },
  unavailablePrice: { color: colors.textMuted, fontFamily: type.bold, fontSize: 14, textAlign: 'center', marginTop: spacing.lg },
  checkingState: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  checkingText: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 11, marginTop: spacing.sm },
  failureText: { color: colors.danger, fontFamily: type.regular, fontSize: 9, lineHeight: 12, textAlign: 'center', marginTop: spacing.xs },
  observationText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 9, marginTop: 'auto', paddingTop: spacing.xs },
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
    minHeight: 56,
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
});
