import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  BackHandler,
  FlatList,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { BannerTone } from '../../components/BottomBanner';
import type { ComparisonSort } from '../../domain/comparison';
import { summarizeProductComparison } from '../../domain/comparison';
import { formatRelativeTime, formatRupees } from '../../domain/formatting';
import type { InventoryProduct } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { BarcodeScannerModal } from '../inventory/BarcodeScannerModal';
import { ProductDetailsModal } from './ProductDetailsModal';

const PAGE_SIZE = 10;

const SORT_OPTIONS: Array<{ value: ComparisonSort; label: string }> = [
  { value: 'MOST_VIEWED', label: 'Most viewed' },
  { value: 'ALPHABETICAL', label: 'A–Z' },
  { value: 'RECENT', label: 'Recent' },
  { value: 'BEST_SAVING', label: 'Best saving' },
];

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
  const [sort, setSort] = useState<ComparisonSort>('MOST_VIEWED');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);
  const [scannerVisible, setScannerVisible] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<InventoryProduct | null>(null);
  const requestId = useRef(0);
  const searchRef = useRef<TextInput>(null);
  const insets = useSafeAreaInsets();

  const loadProducts = useCallback(async (
    search: string,
    selectedSort: ComparisonSort,
    selectedPage: number,
  ) => {
    const id = ++requestId.current;
    const result = await repository.listComparisonProducts(search, selectedSort, selectedPage, PAGE_SIZE);
    if (requestId.current !== id) return;
    setProducts(result.products);
    setTotal(result.total);
    setTotalPages(result.totalPages);
    if (result.page !== selectedPage) setPage(result.page);
    setLoading(false);
  }, [repository]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      loadProducts(query, sort, page).catch((error) => {
        setLoading(false);
        showBanner(messageFrom(error), 'error');
      });
    }, query.trim() ? 180 : 0);
    return () => clearTimeout(timeout);
  }, [loadProducts, page, query, showBanner, sort]);

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
    setSort('MOST_VIEWED');
    setPage(1);
    setSearchFocused(false);
    try {
      await loadProducts('', 'MOST_VIEWED', 1);
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setRefreshing(false);
    }
  };

  const openProduct = async (product: InventoryProduct) => {
    Keyboard.dismiss();
    setSearchFocused(false);
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
    loadProducts(query, sort, page).catch(() => undefined);
  };

  const suggestions = useMemo(
    () => searchFocused && query.trim() ? products.slice(0, 4) : [],
    [products, query, searchFocused],
  );

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

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.sortRow}
        keyboardShouldPersistTaps="handled"
      >
        {SORT_OPTIONS.map((option) => {
          const selected = option.value === sort;
          return (
            <Pressable
              key={option.value}
              accessibilityRole="button"
              accessibilityState={{ selected }}
              onPress={() => {
                setLoading(true);
                setSort(option.value);
                setPage(1);
              }}
              style={({ pressed }) => [styles.sortChip, selected && styles.sortChipSelected, pressed && styles.pressed]}
            >
              <Text style={[styles.sortText, selected && styles.sortTextSelected]}>{option.label}</Text>
            </Pressable>
          );
        })}
      </ScrollView>

      <View style={styles.resultRow}>
        <Text style={styles.resultText}>
          {total} {total === 1 ? 'product' : 'products'}{query.trim() ? ' found' : ''}
        </Text>
        <Text style={styles.pageText}>Page {Math.min(page, totalPages)} of {totalPages}</Text>
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
          setSearchFocused(false);
        }}
        scrollEnabled={!searchFocused}
        showsVerticalScrollIndicator={false}
      />

      {searchFocused ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Close comparison search"
          onPress={() => {
            Keyboard.dismiss();
            setSearchFocused(false);
          }}
          style={styles.searchDimmer}
        />
      ) : null}

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'position' : undefined}
        keyboardVerticalOffset={0}
        style={styles.searchAvoider}
        contentContainerStyle={styles.searchAvoiderContent}
      >
        <View style={[styles.searchArea, { paddingBottom: Math.max(insets.bottom, spacing.md) }]}>
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

          <View style={[styles.searchShell, searchFocused && styles.searchFocused]}>
            <Ionicons name="search" size={22} color={searchFocused ? colors.warning : colors.textMuted} />
            <TextInput
              ref={searchRef}
              value={query}
              onChangeText={(value) => {
                setLoading(true);
                setQuery(value);
                setPage(1);
              }}
              onFocus={() => setSearchFocused(true)}
              onBlur={() => setSearchFocused(false)}
              onSubmitEditing={() => {
                setSearchFocused(false);
                Keyboard.dismiss();
              }}
              placeholder="Search…"
              placeholderTextColor={colors.textMuted}
              selectionColor={colors.warning}
              cursorColor={colors.warning}
              returnKeyType="search"
              autoCapitalize="none"
              autoCorrect={false}
              style={styles.searchInput}
            />
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
                <Ionicons name="close-circle" size={22} color={colors.textMuted} />
              </Pressable>
            ) : null}
            <View style={styles.searchDivider} />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Scan barcode to compare"
              hitSlop={10}
              onPress={() => {
                Keyboard.dismiss();
                setSearchFocused(false);
                setScannerVisible(true);
              }}
              style={styles.searchAction}
            >
              <Ionicons name="camera" size={23} color={colors.textMuted} />
            </Pressable>
          </View>
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
  const latestChecked = Math.max(product.amazonLastChecked ?? 0, product.flipkartLastChecked ?? 0) || null;

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
        <Text style={styles.cardName} numberOfLines={3}>{product.productName}</Text>
        <Text style={styles.shopPriceLabel}>SHOP PRICE</Text>
        <Text style={styles.cardPrice}>{formatRupees(product.shopPrice)}</Text>
        <View style={[styles.statusChip, { backgroundColor: withAlpha(tone, '18') }]}>
          <Ionicons
            name={summary.position === 'REVIEW' ? 'alert-circle' : summary.position === 'COMPETITIVE' ? 'trophy' : 'time-outline'}
            size={16}
            color={tone}
          />
          <Text style={[styles.statusText, { color: tone }]} numberOfLines={1}>{label}</Text>
        </View>
        {latestChecked ? <Text style={styles.checkedText}>Checked {formatRelativeTime(latestChecked)}</Text> : null}
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
  return (
    <View style={styles.stateCard}>
      <ActivityIndicator color={colors.warning} size="large" />
      <Text style={styles.stateTitle}>Loading comparisons…</Text>
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
  content: { padding: spacing.lg, paddingBottom: 132 },
  header: { minHeight: 84, flexDirection: 'row', alignItems: 'center' },
  backButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center', marginLeft: -spacing.sm },
  headerIcon: { width: 58, height: 58, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, backgroundColor: withAlpha(colors.warning, '16'), marginHorizontal: spacing.sm },
  headerCopy: { flex: 1, minWidth: 0 },
  eyebrow: { color: colors.textMuted, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 22, lineHeight: 27, marginTop: spacing.xs },
  searchDimmer: { ...StyleSheet.absoluteFill, zIndex: 10, backgroundColor: 'rgba(0,0,0,0.58)' },
  searchAvoider: { position: 'absolute', zIndex: 20, left: 0, right: 0, bottom: 0, pointerEvents: 'box-none' },
  searchAvoiderContent: { width: '100%' },
  searchArea: { paddingHorizontal: spacing.lg },
  searchShell: { minHeight: 64, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: '#11161D', paddingHorizontal: spacing.lg },
  searchFocused: { borderColor: colors.warning },
  searchInput: { flex: 1, minWidth: 0, color: colors.text, fontFamily: type.regular, fontSize: 16, paddingHorizontal: spacing.md, paddingVertical: spacing.md },
  searchAction: { width: 38, height: 44, alignItems: 'center', justifyContent: 'center' },
  searchDivider: { width: 1, height: 30, backgroundColor: colors.border, marginHorizontal: spacing.xs },
  suggestions: { marginBottom: spacing.sm, overflow: 'hidden', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.surfaceRaised },
  suggestionRow: { minHeight: 52, flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.lg, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border },
  suggestionText: { flex: 1, color: colors.text, fontFamily: type.semibold, fontSize: 14, marginHorizontal: spacing.md },
  suggestionPrice: { color: colors.textMuted, fontFamily: type.bold, fontSize: 13 },
  sortRow: { gap: spacing.sm, paddingVertical: spacing.lg },
  sortChip: { minHeight: 42, justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.pill, backgroundColor: colors.surface, paddingHorizontal: spacing.lg },
  sortChipSelected: { borderColor: colors.warning, backgroundColor: withAlpha(colors.warning, '16') },
  sortText: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 13 },
  sortTextSelected: { color: colors.warning },
  resultRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.md },
  resultText: { color: colors.text, fontFamily: type.bold, fontSize: 15 },
  pageText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12 },
  cardRow: { gap: spacing.md },
  cardSlot: { flex: 1, maxWidth: '49%', marginBottom: spacing.md },
  card: { minHeight: 360, overflow: 'hidden', borderWidth: 1, borderRadius: radius.lg, backgroundColor: colors.surface },
  cardPressed: { opacity: 0.82, transform: [{ scale: 0.99 }] },
  cardImageShell: { height: 174, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F4F4F2' },
  cardImage: { width: '100%', height: '100%' },
  imagePlaceholder: { alignItems: 'center', justifyContent: 'center' },
  imagePlaceholderText: { color: '#626A76', fontFamily: type.bold, fontSize: 9, letterSpacing: 1.1, marginTop: spacing.sm },
  cardBody: { flex: 1, padding: spacing.md },
  cardName: { minHeight: 61, color: colors.text, fontFamily: type.bold, fontSize: 15, lineHeight: 20 },
  shopPriceLabel: { color: colors.textMuted, fontFamily: type.bold, fontSize: 9, letterSpacing: 0.9, marginTop: spacing.sm },
  cardPrice: { color: colors.text, fontFamily: type.bold, fontSize: 21, marginTop: spacing.xs },
  statusChip: { minHeight: 38, alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', borderRadius: radius.pill, paddingHorizontal: spacing.md, marginTop: spacing.md },
  statusText: { maxWidth: 118, fontFamily: type.bold, fontSize: 11, marginLeft: 6 },
  checkedText: { color: colors.textMuted, fontFamily: type.regular, fontSize: 10, marginTop: spacing.sm },
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
