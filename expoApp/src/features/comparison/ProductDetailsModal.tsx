import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
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

import type { BannerTone } from '../../components/BottomBanner';
import { comparePrices, retailerDisplayName, savedRetailerPrice } from '../../domain/comparison';
import { formatRelativeTime, formatRupees } from '../../domain/formatting';
import type { InventoryProduct, PriceObservation, PriceRetailer } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { fetchRetailerPrice } from '../../network/retailerPriceClient';
import { colors, radius, spacing, type } from '../../theme/tokens';

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
        if (!controller.signal.aborted) {
          setState({ phase: 'failed', message: messageFrom(error) });
        }
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

  const historySummary = useMemo(() => {
    const amazon = history.filter((entry) => entry.retailer === 'AMAZON').length;
    const flipkart = history.filter((entry) => entry.retailer === 'FLIPKART').length;
    return { amazon, flipkart };
  }, [history]);

  return (
    <Modal
      visible={product != null}
      transparent={Platform.OS !== 'ios'}
      presentationStyle={Platform.OS === 'ios' ? 'pageSheet' : 'overFullScreen'}
      animationType="slide"
      onRequestClose={close}
    >
      <SafeAreaView style={[styles.modalRoot, Platform.OS !== 'ios' && styles.backdrop]}>
        <View style={styles.sheet}>
          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <Text style={styles.eyebrow}>PRODUCT DETAILS</Text>
              <Text style={styles.headerTitle}>Compare saved and live prices</Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close product details"
              onPress={close}
              style={({ pressed }) => [styles.closeButton, pressed && styles.pressed]}
            >
              <Ionicons name="close" size={27} color={colors.text} />
            </Pressable>
          </View>

          {currentProduct ? (
            <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
              <View style={styles.productCard}>
                <View style={styles.imageShell}>
                  {currentProduct.imageUrl ? (
                    <Image source={{ uri: currentProduct.imageUrl }} style={styles.image} contentFit="contain" transition={160} />
                  ) : (
                    <Ionicons name="cube-outline" size={46} color={colors.textMuted} />
                  )}
                </View>
                <View style={styles.productCopy}>
                  <Text style={styles.productName}>{currentProduct.productName}</Text>
                  <Text style={styles.shopLabel}>SHOP PRICE</Text>
                  <Text style={styles.shopPrice}>{formatRupees(currentProduct.shopPrice)}</Text>
                </View>
              </View>

              <View style={styles.retailerGrid}>
                <RetailerPanel
                  retailer="AMAZON"
                  product={currentProduct}
                  state={amazonState}
                  observationCount={historySummary.amazon}
                  onOpen={() => openRetailer(currentProduct.amazonUrl, 'AMAZON')}
                />
                <RetailerPanel
                  retailer="FLIPKART"
                  product={currentProduct}
                  state={flipkartState}
                  observationCount={historySummary.flipkart}
                  onOpen={() => openRetailer(currentProduct.flipkartUrl, 'FLIPKART')}
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
                  <ActivityIndicator color={colors.background} />
                ) : (
                  <Ionicons name="refresh" size={22} color={colors.background} />
                )}
                <Text style={styles.refreshText}>{refreshing ? 'Checking retailers…' : 'Refresh live prices'}</Text>
              </Pressable>

              <View style={styles.safetyNote}>
                <Ionicons name="shield-checkmark-outline" size={22} color={colors.primary} />
                <Text style={styles.safetyText}>
                  A failed live check never erases a valid saved price. Successful checks are added to bounded local history.
                </Text>
              </View>
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
  onOpen,
}: {
  retailer: PriceRetailer;
  product: InventoryProduct;
  state: RetailerState;
  observationCount: number;
  onOpen: () => void;
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

  return (
    <View style={[styles.retailerCard, { borderColor: withAlpha(tone, '88') }]}>
      <View style={styles.retailerHeader}>
        <Text style={styles.retailerName}>{retailerDisplayName(retailer)}</Text>
        <View style={[styles.sourceChip, { backgroundColor: withAlpha(tone, '18') }]}>
          <Text style={[styles.sourceText, { color: tone }]}>{source}</Text>
        </View>
      </View>

      {state.phase === 'checking' ? (
        <View style={styles.checkingRow}>
          <ActivityIndicator color={colors.warning} size="small" />
          <Text style={styles.checkingText}>Checking live price…</Text>
        </View>
      ) : visiblePrice != null ? (
        <>
          <Text style={styles.retailerPrice}>{formatRupees(visiblePrice)}</Text>
          <Text style={[styles.relationshipText, { color: tone }]}>
            {relationshipText(product.shopPrice, visiblePrice)}
          </Text>
        </>
      ) : (
        <Text style={styles.unavailablePrice}>{linked ? 'Not checked yet' : 'No retailer link'}</Text>
      )}

      {state.phase === 'failed' ? <Text style={styles.failureText}>{state.message}</Text> : null}
      <Text style={styles.historyText}>
        {observationCount} saved {observationCount === 1 ? 'observation' : 'observations'}
      </Text>
      <Pressable
        accessibilityRole="link"
        accessibilityLabel={`Open ${retailerDisplayName(retailer)}`}
        onPress={onOpen}
        style={({ pressed }) => [styles.openButton, pressed && styles.pressed]}
      >
        <Ionicons name="globe-outline" size={18} color={linked ? colors.text : colors.textMuted} />
        <Text style={[styles.openText, !linked && styles.muted]}>Open {retailerDisplayName(retailer)}</Text>
      </Pressable>
    </View>
  );
}

function retailerSourceLabel(
  state: RetailerState,
  savedCheckedAt: number | null,
  linked: boolean,
): string {
  if (state.phase === 'live') return 'LIVE';
  if (state.phase === 'checking') return 'CHECKING';
  if (state.phase === 'failed' && savedCheckedAt != null) return 'SAVED • CHECK FAILED';
  if (state.phase === 'failed') return 'FAILED';
  if (savedCheckedAt != null) return `SAVED • ${formatRelativeTime(savedCheckedAt).toLocaleUpperCase()}`;
  return linked ? 'NOT CHECKED' : 'UNAVAILABLE';
}

function relationshipText(shopPrice: number, onlinePrice: number): string {
  const relationship = comparePrices(shopPrice, onlinePrice);
  const difference = Math.abs(shopPrice - onlinePrice);
  if (relationship === 'ONLINE_LOWER') return `${formatRupees(difference)} lower than shop • review`;
  if (relationship === 'SHOP_LOWER') return `${formatRupees(difference)} higher than shop • competitive`;
  return 'Matches shop price • competitive';
}

function withAlpha(hex: string, alpha: string): string {
  return hex.length === 7 ? `${hex}${alpha}` : hex;
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  modalRoot: { flex: 1, backgroundColor: colors.surface },
  backdrop: { backgroundColor: 'rgba(0,0,0,0.76)', justifyContent: 'flex-end' },
  sheet: {
    flex: Platform.OS === 'ios' ? 1 : undefined,
    maxHeight: Platform.OS === 'ios' ? '100%' : '94%',
    minHeight: Platform.OS === 'ios' ? undefined : '82%',
    overflow: 'hidden',
    backgroundColor: colors.surface,
    borderTopLeftRadius: Platform.OS === 'ios' ? 0 : radius.lg,
    borderTopRightRadius: Platform.OS === 'ios' ? 0 : radius.lg,
  },
  header: { flexDirection: 'row', alignItems: 'center', padding: spacing.xl, borderBottomWidth: 1, borderBottomColor: colors.border },
  headerCopy: { flex: 1, minWidth: 0, paddingRight: spacing.md },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  headerTitle: { color: colors.text, fontFamily: type.bold, fontSize: 21, marginTop: spacing.xs },
  closeButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border },
  content: { padding: spacing.xl, gap: spacing.lg, paddingBottom: 56 },
  productCard: { flexDirection: 'row', borderWidth: 1, borderColor: colors.border, borderRadius: radius.lg, backgroundColor: colors.background, padding: spacing.md },
  imageShell: { width: 132, height: 132, alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: radius.md, backgroundColor: '#F4F4F2' },
  image: { width: '100%', height: '100%' },
  productCopy: { flex: 1, minWidth: 0, justifyContent: 'center', paddingLeft: spacing.lg },
  productName: { color: colors.text, fontFamily: type.bold, fontSize: 18, lineHeight: 24 },
  shopLabel: { color: colors.textMuted, fontFamily: type.bold, fontSize: 10, letterSpacing: 1, marginTop: spacing.lg },
  shopPrice: { color: colors.text, fontFamily: type.bold, fontSize: 27, marginTop: spacing.xs },
  retailerGrid: { gap: spacing.md },
  retailerCard: { borderWidth: 1, borderRadius: radius.md, backgroundColor: colors.background, padding: spacing.lg },
  retailerHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  retailerName: { color: colors.text, fontFamily: type.bold, fontSize: 18 },
  sourceChip: { maxWidth: '68%', borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: 6 },
  sourceText: { fontFamily: type.bold, fontSize: 9, letterSpacing: 0.6, textAlign: 'center' },
  retailerPrice: { color: colors.text, fontFamily: type.bold, fontSize: 28, marginTop: spacing.lg },
  relationshipText: { fontFamily: type.semibold, fontSize: 13, marginTop: spacing.xs },
  unavailablePrice: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 17, marginTop: spacing.lg },
  checkingRow: { minHeight: 56, flexDirection: 'row', alignItems: 'center', marginTop: spacing.sm },
  checkingText: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 14, marginLeft: spacing.md },
  failureText: { color: colors.danger, fontFamily: type.regular, fontSize: 12, lineHeight: 17, marginTop: spacing.sm },
  historyText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12, marginTop: spacing.lg },
  openButton: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, marginTop: spacing.md },
  openText: { color: colors.text, fontFamily: type.semibold, fontSize: 14, marginLeft: spacing.sm },
  muted: { color: colors.textMuted },
  refreshButton: { minHeight: 58, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.primary },
  refreshText: { color: colors.background, fontFamily: type.bold, fontSize: 16, marginLeft: spacing.sm },
  safetyNote: { flexDirection: 'row', alignItems: 'flex-start', borderRadius: radius.md, backgroundColor: colors.primaryMuted, padding: spacing.lg },
  safetyText: { flex: 1, color: colors.textMuted, fontFamily: type.regular, fontSize: 13, lineHeight: 19, marginLeft: spacing.md },
  pressed: { opacity: 0.7 },
  disabled: { opacity: 0.58 },
});
