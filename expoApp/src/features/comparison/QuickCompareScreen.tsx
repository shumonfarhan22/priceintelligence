import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  BackHandler,
  Easing,
  FlatList,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { BannerTone } from '../../components/BottomBanner';
import type { ComparisonSort } from '../../domain/comparison';
import { summarizeProductComparison } from '../../domain/comparison';
import { formatRupees } from '../../domain/formatting';
import type { InventoryProduct } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { BarcodeScannerModal } from '../inventory/BarcodeScannerModal';
import { ProductDetailsModal } from './ProductDetailsModal';

const PAGE_SIZE = 10;

const DEFAULT_SORT: ComparisonSort = 'MOST_VIEWED';

type ShowBanner = (message: string, tone?: BannerTone) => void;

export function QuickCompareScreen({
  repository,
  onBack,
  onComparisonChanged,
  showBanner,
}: {
  repository: InventoryRepository;
  onBack: () => void;
  onComparisonChanged: () => void;
  showBanner: ShowBanner;
}) {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);
  const [scannerVisible, setScannerVisible] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<InventoryProduct | null>(null);
  const requestId = useRef(0);
  const searchRef = useRef<TextInput>(null);
  const searchProgress = useRef(new Animated.Value(0)).current;
  const keyboardVisible = useRef(false);
  const closeAfterKeyboard = useRef(false);
  const insets = useSafeAreaInsets();
  const { width: windowWidth } = useWindowDimensions();
  const expandedSearchWidth = Math.max(288, windowWidth - (spacing.lg * 2));

  const loadProducts = useCallback(async (
    search: string,
    selectedSort: ComparisonSort,
    selectedPage: number,
  ) => {
    const id = ++requestId.current;
    const result = await repository.listComparisonProducts(search, selectedSort, selectedPage, PAGE_SIZE);
    if (requestId.current !== id) return;
    setProducts(result.products);
    setTotalPages(result.totalPages);
    if (result.page !== selectedPage) setPage(result.page);
    setLoading(false);
  }, [repository]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      loadProducts(query, DEFAULT_SORT, page).catch((error) => {
        setLoading(false);
        showBanner(messageFrom(error), 'error');
      });
    }, query.trim() ? 180 : 0);
    return () => clearTimeout(timeout);
  }, [loadProducts, page, query, showBanner]);

  const leave = useCallback(() => {
    Keyboard.dismiss();
    onBack();
  }, [onBack]);

  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (scannerVisible || selectedProduct) return false;
      leave();
      return true;
    });
    return () => subscription.remove();
  }, [leave, scannerVisible, selectedProduct]);

  const refresh = async () => {
    Keyboard.dismiss();
    setRefreshing(true);
    setQuery('');
    setPage(1);
    closeAfterKeyboard.current = false;
    setSearchFocused(false);
    searchProgress.setValue(0);
    try {
      await loadProducts('', DEFAULT_SORT, 1);
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setRefreshing(false);
    }
  };

  const openProduct = async (product: InventoryProduct) => {
    Keyboard.dismiss();
    setSearchFocused(false);
    searchProgress.setValue(0);
    setSelectedProduct(product);
    try {
      await repository.incrementProductView(product.id);
      setProducts((current) => current.map((entry) => (
        entry.id === product.id ? { ...entry, searchCount: entry.searchCount + 1 } : entry
      )));
    } catch {
      // Viewing a locally available product should still work if popularity tracking fails.
    }
  };

  const updateProduct = (updated: InventoryProduct) => {
    setSelectedProduct(updated);
    setProducts((current) => current.map((entry) => entry.id === updated.id ? updated : entry));
    onComparisonChanged();
    loadProducts(query, DEFAULT_SORT, page).catch(() => undefined);
  };

  const suggestions = useMemo(
    () => searchFocused && query.trim() ? products.slice(0, 4) : [],
    [products, query, searchFocused],
  );

  const collapseSearch = useCallback(() => {
    closeAfterKeyboard.current = false;
    setSearchFocused(false);
    Animated.timing(searchProgress, {
      toValue: 0,
      duration: 125,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    }).start();
  }, [searchProgress]);

  const dismissSearch = useCallback(() => {
    closeAfterKeyboard.current = true;
    Keyboard.dismiss();
    if (!keyboardVisible.current) collapseSearch();
  }, [collapseSearch]);

  const openSearch = useCallback(() => {
    if (searchFocused) return;
    setSearchFocused(true);
    Animated.timing(searchProgress, {
      toValue: 1,
      duration: 125,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    }).start(({ finished }) => {
      if (finished) searchRef.current?.focus();
    });
  }, [searchFocused, searchProgress]);

  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = 'keyboardDidHide';
    const shown = Keyboard.addListener(showEvent, () => {
      keyboardVisible.current = true;
    });
    const hidden = Keyboard.addListener(hideEvent, () => {
      keyboardVisible.current = false;
      if (searchFocused) collapseSearch();
    });
    return () => {
      shown.remove();
      hidden.remove();
    };
  }, [collapseSearch, searchFocused]);

  const header = (
    <View>
      <View style={styles.header}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Back to dashboard"
          onPress={leave}
          style={({ pressed }) => [styles.backButton, pressed && styles.pressed]}
        >
          <Ionicons name="arrow-back" size={28} color={colors.text} />
        </Pressable>
        <View style={styles.headerIcon}>
          <Ionicons name="search" size={26} color={colors.warning} />
        </View>
        <View style={styles.headerCopy}>
          <Text style={styles.eyebrow}>QUICK COMPARE</Text>
          <Text style={styles.title}>Open one comparison quickly</Text>
        </View>
      </View>

    </View>
  );

  return (
    <View style={styles.screen}>
      <FlatList
        data={products}
        numColumns={2}
        keyExtractor={(product) => product.id.toString()}
        renderItem={({ item }) => (
          <View style={styles.cardSlot}>
            <ComparisonCard product={item} onPress={() => openProduct(item)} />
          </View>
        )}
        columnWrapperStyle={styles.cardRow}
        contentContainerStyle={styles.content}
        ListHeaderComponent={header}
        ListEmptyComponent={loading ? <LoadingState /> : <EmptyState query={query} />}
        ListFooterComponent={products.length > 0 ? (
          <Pagination
            page={page}
            totalPages={totalPages}
            onPrevious={() => {
              setLoading(true);
              setPage((current) => Math.max(1, current - 1));
            }}
            onNext={() => {
              setLoading(true);
              setPage((current) => Math.min(totalPages, current + 1));
            }}
          />
        ) : null}
        refreshControl={(
          <RefreshControl
            refreshing={refreshing}
            onRefresh={refresh}
            tintColor={colors.warning}
            colors={[colors.warning]}
            progressBackgroundColor={colors.surface}
          />
        )}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        onScrollBeginDrag={() => {
          Keyboard.dismiss();
        }}
        scrollEnabled={!searchFocused}
        showsVerticalScrollIndicator={false}
      />

      {searchFocused ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Close comparison search"
          onPress={dismissSearch}
          style={styles.searchDimmer}
        />
      ) : null}

      <KeyboardAvoidingView
        behavior="padding"
        keyboardVerticalOffset={0}
        style={styles.searchAvoider}
        contentContainerStyle={styles.searchAvoiderContent}
      >
        <View
          style={[
            styles.searchArea,
            { paddingBottom: searchFocused ? spacing.sm : Math.max(insets.bottom, 20) },
          ]}
        >
          {suggestions.length > 0 ? (
            <View style={styles.suggestions}>
              {suggestions.map((product) => (
                <Pressable
                  key={product.id}
                  accessibilityRole="button"
                  onPress={() => openProduct(product)}
                  style={({ pressed }) => [styles.suggestionRow, pressed && styles.pressed]}
                >
                  <Ionicons name="cube-outline" size={18} color={colors.warning} />
                  <Text style={styles.suggestionText} numberOfLines={1}>{product.productName}</Text>
                  <Text style={styles.suggestionPrice}>{formatRupees(product.shopPrice)}</Text>
                </Pressable>
              ))}
            </View>
          ) : null}

          <Animated.View
            style={[
              styles.searchShell,
              {
                width: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [56, expandedSearchWidth] }),
                borderRadius: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [28, 12] }),
                backgroundColor: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [colors.warning, '#11161D'] }),
              },
            ]}
          >
            {searchFocused ? (
              <Animated.View
                style={[
                  styles.searchContent,
                  {
                    opacity: searchProgress.interpolate({ inputRange: [0, 0.55, 1], outputRange: [0, 0, 1] }),
                  },
                ]}
              >
                <Ionicons name="search" size={20} color={colors.warning} />
                <TextInput
                  ref={searchRef}
                  value={query}
                  onChangeText={(value) => {
                    setLoading(true);
                    setQuery(value);
                    setPage(1);
                  }}
                  onFocus={() => setSearchFocused(true)}
                  onSubmitEditing={dismissSearch}
                  placeholder="Search…"
                  placeholderTextColor={colors.textMuted}
                  selectionColor={colors.warning}
                  cursorColor={colors.warning}
                  returnKeyType="search"
                  autoCapitalize="none"
                  autoCorrect={false}
                  style={styles.searchInput}
                />
                <View style={styles.searchClearSlot}>
                  {query ? (
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Clear comparison search"
                      hitSlop={10}
                      onPress={() => {
                        setQuery('');
                        setPage(1);
                        searchRef.current?.focus();
                      }}
                      style={styles.searchAction}
                    >
                      <Ionicons name="close" size={20} color={colors.textMuted} />
                    </Pressable>
                  ) : null}
                </View>
                <View style={styles.searchDivider} />
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Scan barcode to compare"
                  hitSlop={10}
                  onPress={() => {
                    dismissSearch();
                    setTimeout(() => setScannerVisible(true), Platform.OS === 'ios' ? 260 : 100);
                  }}
                  style={styles.searchCamera}
                >
                  <Ionicons name="camera" size={22} color={colors.warning} />
                </Pressable>
              </Animated.View>
            ) : (
              <Animated.View
                style={[
                  styles.searchFabLayer,
                  {
                    opacity: searchProgress.interpolate({ inputRange: [0, 0.35, 1], outputRange: [1, 0, 0] }),
                  },
                ]}
              >
                <Pressable accessibilityRole="button" accessibilityLabel="Open product search" onPress={openSearch} style={styles.searchFabButton}>
                  <Ionicons name="search" size={25} color={colors.background} />
                </Pressable>
              </Animated.View>
            )}
          </Animated.View>
        </View>
      </KeyboardAvoidingView>

      <BarcodeScannerModal
        visible={scannerVisible}
        onClose={() => setScannerVisible(false)}
        onScanned={(value) => {
          setScannerVisible(false);
          setLoading(true);
          setQuery(value);
          setPage(1);
        }}
      />
      <ProductDetailsModal
        product={selectedProduct}
        repository={repository}
        showBanner={showBanner}
        onClose={() => setSelectedProduct(null)}
        onProductUpdated={updateProduct}
      />
    </View>
  );
}

function ComparisonCard({ product, onPress }: { product: InventoryProduct; onPress: () => void }) {
  const summary = summarizeProductComparison(product);
  const tone = summary.position === 'REVIEW'
    ? colors.danger
    : summary.position === 'COMPETITIVE'
      ? colors.primary
      : colors.textMuted;
  const label = summary.position === 'REVIEW'
    ? 'Review • saved'
    : summary.position === 'COMPETITIVE'
      ? 'Competitive • saved'
      : 'Not checked';
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${product.productName}, shop price ${formatRupees(product.shopPrice)}, ${label}`}
      onPress={onPress}
      style={({ pressed }) => [styles.card, { borderColor: withAlpha(tone, '88') }, pressed && styles.cardPressed]}
    >
      <View style={styles.cardImageShell}>
        {product.imageUrl ? (
          <Image source={{ uri: product.imageUrl }} style={styles.cardImage} contentFit="contain" transition={150} />
        ) : (
          <View style={styles.imagePlaceholder}>
            <Ionicons name="cube-outline" size={39} color={colors.textMuted} />
            <Text style={styles.imagePlaceholderText}>PRODUCT</Text>
          </View>
        )}
      </View>
      <View style={styles.cardBody}>
        <Text style={styles.cardName} numberOfLines={2}>{product.productName}</Text>
        <Text style={styles.shopPriceLabel}>SHOP PRICE</Text>
        <Text style={styles.cardPrice}>{formatRupees(product.shopPrice)}</Text>
        <View style={[styles.statusChip, { backgroundColor: withAlpha(tone, '18') }]}>
          <Ionicons
            name={summary.position === 'REVIEW' ? 'alert-circle' : summary.position === 'COMPETITIVE' ? 'trophy' : 'time-outline'}
            size={14}
            color={tone}
          />
          <Text style={[styles.statusText, { color: tone }]} numberOfLines={1}>{label}</Text>
        </View>
      </View>
    </Pressable>
  );
}

function Pagination({
  page,
  totalPages,
  onPrevious,
  onNext,
}: {
  page: number;
  totalPages: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <View style={styles.pagination}>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: page <= 1 }}
        disabled={page <= 1}
        onPress={onPrevious}
        style={({ pressed }) => [styles.pageButton, pressed && styles.pressed, page <= 1 && styles.disabled]}
      >
        <Ionicons name="chevron-back" size={20} color={colors.text} />
        <Text style={styles.pageButtonText}>Previous</Text>
      </Pressable>
      <View style={styles.pageBadge}>
        <Text style={styles.pageBadgeText}>{page} / {totalPages}</Text>
      </View>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: page >= totalPages }}
        disabled={page >= totalPages}
        onPress={onNext}
        style={({ pressed }) => [styles.pageButton, pressed && styles.pressed, page >= totalPages && styles.disabled]}
      >
        <Text style={styles.pageButtonText}>Next</Text>
        <Ionicons name="chevron-forward" size={20} color={colors.text} />
      </Pressable>
    </View>
  );
}

function LoadingState() {
  const shimmer = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animation = Animated.loop(Animated.timing(shimmer, {
      toValue: 1,
      duration: 1150,
      easing: Easing.linear,
      useNativeDriver: Platform.OS !== 'web',
    }));
    animation.start();
    return () => animation.stop();
  }, [shimmer]);

  return (
    <View style={styles.skeletonGrid} accessibilityLabel="Loading comparisons">
      {[0, 1, 2, 3].map((index) => (
        <View key={index} style={styles.skeletonCard}>
          <View style={styles.skeletonImage} />
          <View style={styles.skeletonBody}>
            <View style={styles.skeletonLineWide} />
            <View style={styles.skeletonLineMedium} />
            <View style={styles.skeletonLineShort} />
            <View style={styles.skeletonChip} />
          </View>
          <Animated.View
            style={[
              styles.shimmerBand,
              {
                transform: [
                  { translateX: shimmer.interpolate({ inputRange: [0, 1], outputRange: [-220, 260] }) },
                  { skewX: '-14deg' },
                ],
              },
            ]}
          />
        </View>
      ))}
    </View>
  );
}

function EmptyState({ query }: { query: string }) {
  return (
    <View style={styles.stateCard}>
      <Ionicons name={query.trim() ? 'search-outline' : 'cube-outline'} size={38} color={colors.textMuted} />
      <Text style={styles.stateTitle}>{query.trim() ? 'No matching products' : 'Inventory is empty'}</Text>
      <Text style={styles.stateBody}>
        {query.trim()
          ? 'Try a shorter product name, barcode, Amazon link, or Flipkart link.'
          : 'Add products in Inventory before opening a comparison.'}
      </Text>
    </View>
  );
}

function withAlpha(hex: string, alpha: string): string {
  return hex.length === 7 ? `${hex}${alpha}` : hex;
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.lg, paddingTop: spacing.sm, paddingBottom: 112 },
  header: { minHeight: 70, flexDirection: 'row', alignItems: 'center', marginBottom: spacing.sm },
  backButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center', marginLeft: -spacing.sm },
  headerIcon: { width: 54, height: 54, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: withAlpha(colors.warning, '16'), marginHorizontal: spacing.sm },
  headerCopy: { flex: 1, minWidth: 0 },
  eyebrow: { color: colors.textMuted, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 20, lineHeight: 24, marginTop: 2 },
  searchDimmer: { ...StyleSheet.absoluteFill, zIndex: 10, backgroundColor: 'rgba(0,0,0,0.58)' },
  searchAvoider: { position: 'absolute', zIndex: 20, left: 0, top: 0, right: 0, bottom: 0, justifyContent: 'flex-end', pointerEvents: 'box-none' },
  searchAvoiderContent: { flex: 1, justifyContent: 'flex-end' },
  searchArea: { alignItems: 'flex-end', paddingHorizontal: spacing.lg, pointerEvents: 'box-none' },
  searchShell: { height: 56, overflow: 'hidden', borderWidth: 1, borderColor: colors.border },
  searchContent: { ...StyleSheet.absoluteFill, flexDirection: 'row', alignItems: 'center', paddingLeft: spacing.lg },
  searchInput: { flex: 1, minWidth: 0, height: 54, color: colors.text, fontFamily: type.regular, fontSize: 16, paddingHorizontal: spacing.sm },
  searchClearSlot: { width: 42, height: 48, alignItems: 'center', justifyContent: 'center' },
  searchAction: { width: 38, height: 44, alignItems: 'center', justifyContent: 'center' },
  searchDivider: { width: 1, height: 24, backgroundColor: colors.border },
  searchCamera: { width: 56, height: 56, alignItems: 'center', justifyContent: 'center' },
  searchFabLayer: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center' },
  searchFabButton: { width: 56, height: 56, alignItems: 'center', justifyContent: 'center' },
  suggestions: { width: '100%', marginBottom: spacing.sm, overflow: 'hidden', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.surfaceRaised },
  suggestionRow: { minHeight: 52, flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.lg, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border },
  suggestionText: { flex: 1, color: colors.text, fontFamily: type.semibold, fontSize: 14, marginHorizontal: spacing.md },
  suggestionPrice: { color: colors.textMuted, fontFamily: type.bold, fontSize: 13 },
  cardRow: { gap: spacing.md },
  cardSlot: { flex: 1, maxWidth: '49%', marginBottom: spacing.md },
  card: { overflow: 'hidden', borderWidth: 1, borderRadius: 20, backgroundColor: colors.surface },
  cardPressed: { opacity: 0.82, transform: [{ scale: 0.99 }] },
  cardImageShell: { width: '100%', aspectRatio: 1.08, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F4F4F2' },
  cardImage: { width: '100%', height: '100%' },
  imagePlaceholder: { alignItems: 'center', justifyContent: 'center' },
  imagePlaceholderText: { color: '#626A76', fontFamily: type.bold, fontSize: 9, letterSpacing: 1.1, marginTop: spacing.sm },
  cardBody: { flex: 1, padding: spacing.md },
  cardName: { minHeight: 36, color: colors.text, fontFamily: type.bold, fontSize: 13, lineHeight: 18 },
  shopPriceLabel: { color: colors.textMuted, fontFamily: type.bold, fontSize: 8, letterSpacing: 0.7, marginTop: 5 },
  cardPrice: { color: colors.text, fontFamily: type.bold, fontSize: 15, marginTop: 1 },
  statusChip: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', borderRadius: radius.pill, paddingHorizontal: 9, paddingVertical: 6, marginTop: 7 },
  statusText: { maxWidth: 118, fontFamily: type.bold, fontSize: 9, marginLeft: 5 },
  skeletonGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md },
  skeletonCard: { width: '47.8%', overflow: 'hidden', borderWidth: 1, borderColor: colors.border, borderRadius: 20, backgroundColor: colors.surface },
  skeletonImage: { width: '100%', aspectRatio: 1.08, backgroundColor: colors.surfaceRaised },
  skeletonBody: { padding: spacing.md, gap: spacing.sm },
  skeletonLineWide: { width: '92%', height: 14, borderRadius: radius.pill, backgroundColor: '#6C737E' },
  skeletonLineMedium: { width: '72%', height: 12, borderRadius: radius.pill, backgroundColor: '#59616D' },
  skeletonLineShort: { width: '46%', height: 10, borderRadius: radius.pill, backgroundColor: '#59616D' },
  skeletonChip: { width: '64%', height: 32, borderRadius: radius.pill, backgroundColor: '#59616D', marginTop: spacing.xs },
  shimmerBand: { position: 'absolute', top: -30, bottom: -30, width: 72, backgroundColor: 'rgba(255,255,255,0.14)' },
  pagination: { minHeight: 82, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: spacing.md },
  pageButton: { minHeight: 48, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, paddingHorizontal: spacing.md },
  pageButtonText: { color: colors.text, fontFamily: type.semibold, fontSize: 13 },
  pageBadge: { minWidth: 58, height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: radius.pill, backgroundColor: colors.surfaceRaised },
  pageBadgeText: { color: colors.textMuted, fontFamily: type.bold, fontSize: 12 },
  stateCard: { minHeight: 260, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.lg, backgroundColor: colors.surface, padding: spacing.xl, marginTop: spacing.md },
  stateTitle: { color: colors.text, fontFamily: type.bold, fontSize: 18, textAlign: 'center', marginTop: spacing.lg },
  stateBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 14, lineHeight: 20, textAlign: 'center', marginTop: spacing.sm },
  pressed: { opacity: 0.68 },
  disabled: { opacity: 0.38 },
});
