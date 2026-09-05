import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  NativeSyntheticEvent,
  NativeScrollEvent,
  BackHandler,
  Easing,
  FlatList,
  Keyboard,
  KeyboardAvoidingView,
  LayoutAnimation,
  Platform,
  Pressable,
  RefreshControl,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  PixelRatio,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { BannerTone } from '../../components/BottomBanner';
import type { ComparisonSort } from '../../domain/comparison';
import { summarizeProductComparison } from '../../domain/comparison';
import { formatRupees } from '../../domain/formatting';
import type { InventoryProduct } from '../../domain/models';
import { InventoryRepository } from '../../data/inventoryRepository';
import { useCustomization } from '../../theme/CustomizationContext';
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
  const { customization, colors } = useCustomization();
  const pageSize = customization.dashboardPageSize || 10;
  const defaultSort: ComparisonSort = customization.dashboardDefaultSort || 'MOST_VIEWED';
  const isCompact = customization.dashboardCardStyle === 'COMPACT';

  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchFocused, setSearchFocused] = useState(true);
  const [isClosingSearch, setIsClosingSearch] = useState(false);
  const [catalogRevealed, setCatalogRevealed] = useState(false);
  const [scannerVisible, setScannerVisible] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<InventoryProduct | null>(null);
  const catalogFadeAnim = useRef(new Animated.Value(1)).current;
  const insets = useSafeAreaInsets();
  const restingBottom = Math.max(insets.bottom, 20);
  const keyboardOffsetAnim = useRef(new Animated.Value(0)).current;
  const keyboardHeightVal = useRef(0);
  const isSearchClosing = useRef(false);
  if (Platform.OS === 'web') {
    (window as any).__selectProduct = (p: InventoryProduct) => setSelectedProduct(p);
    (window as any).__products = products;
  }
  const requestId = useRef(0);
  const searchRef = useRef<TextInput>(null);
  const searchProgress = useRef(new Animated.Value(1)).current;
  const keyboardVisible = useRef(false);
  const closeAfterKeyboard = useRef(false);
  const { width: windowWidth } = useWindowDimensions();
  const expandedSearchWidth = Math.max(288, windowWidth - (spacing.lg * 2));
  const fontScale = PixelRatio.getFontScale();
  const twoColumnCardWidth = (windowWidth - 44) / 2;
  const minimumTwoColumnCardWidth = fontScale >= 1.30 ? 175 : 145;
  const singleColumn = twoColumnCardWidth < minimumTwoColumnCardWidth;

  const [searchBarVisible, setSearchBarVisible] = useState(true);
  const searchBarVisibleRef = useRef(true);
  const searchBarAnim = useRef(new Animated.Value(1)).current;
  const lastOffsetY = useRef(0);
  const accumulatedDown = useRef(0);

  const updateSearchBarVisibility = useCallback((visible: boolean) => {
    if (searchBarVisibleRef.current === visible) return;
    searchBarVisibleRef.current = visible;
    setSearchBarVisible(visible);
    if (customization.motionPreference === 'REDUCED') {
      searchBarAnim.setValue(visible ? 1 : 0);
    } else {
      Animated.timing(searchBarAnim, {
        toValue: visible ? 1 : 0,
        duration: 180,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: Platform.OS !== 'web',
      }).start();
    }
  }, [customization.motionPreference, searchBarAnim]);

  const handleScroll = useCallback((e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const currentY = e.nativeEvent.contentOffset.y;
    const deltaY = currentY - lastOffsetY.current;

    if (currentY <= 12) {
      accumulatedDown.current = 0;
      updateSearchBarVisibility(true);
    } else if (deltaY > 0) {
      accumulatedDown.current += deltaY;
      if (accumulatedDown.current >= 24) {
        updateSearchBarVisibility(false);
        accumulatedDown.current = 0;
      }
    } else if (deltaY < -4) {
      accumulatedDown.current = 0;
      updateSearchBarVisibility(true);
    }
    lastOffsetY.current = currentY;
  }, [updateSearchBarVisibility]);

  const openProduct = useCallback(async (product: InventoryProduct) => {
    searchRef.current?.blur();
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
  }, [repository, searchProgress]);

  const checkExactMatchAndAutoOpen = useCallback((searchQuery: string, productList: InventoryProduct[]) => {
    const trimmed = searchQuery.trim().toLowerCase();
    if (trimmed.length < 2) return;
    const match = productList.find((p) =>
      p.productName.toLowerCase() === trimmed ||
      (p.barcode && p.barcode.toLowerCase() === trimmed) ||
      (p.amazonUrl && p.amazonUrl.toLowerCase() === trimmed) ||
      (p.flipkartUrl && p.flipkartUrl.toLowerCase() === trimmed)
    );
    if (match) {
      openProduct(match);
    }
  }, [openProduct]);

  const loadProducts = useCallback(async (
    search: string,
    selectedSort: ComparisonSort,
    selectedPage: number,
  ) => {
    const id = ++requestId.current;
    const result = await repository.listComparisonProducts(search, selectedSort, selectedPage, pageSize);
    if (requestId.current !== id) return;
    setProducts(result.products);
    setTotalPages(result.totalPages);
    if (result.page !== selectedPage) setPage(result.page);
    setLoading(false);
    if (search.trim()) {
      checkExactMatchAndAutoOpen(search, result.products);
    }
  }, [checkExactMatchAndAutoOpen, pageSize, repository]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      loadProducts(query, defaultSort, page).catch((error) => {
        setLoading(false);
        showBanner(messageFrom(error), 'error');
      });
    }, query.trim() ? 180 : 0);
    return () => clearTimeout(timeout);
  }, [defaultSort, loadProducts, page, query, showBanner]);

  const leave = useCallback(() => {
    searchRef.current?.blur();
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
    searchRef.current?.blur();
    Keyboard.dismiss();
    setRefreshing(true);
    setQuery('');
    setPage(1);
    closeAfterKeyboard.current = false;
    setSearchFocused(false);
    searchProgress.setValue(0);
    try {
      await loadProducts('', defaultSort, 1);
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setRefreshing(false);
    }
  };

  const updateProduct = (updated: InventoryProduct) => {
    setSelectedProduct(updated);
    setProducts((current) => current.map((entry) => entry.id === updated.id ? updated : entry));
    onComparisonChanged();
    loadProducts(query, defaultSort, page).catch(() => undefined);
  };

  const suggestions = useMemo(
    () => searchFocused && !isClosingSearch && query.trim() ? products.slice(0, 4) : [],
    [isClosingSearch, products, query, searchFocused],
  );

  const collapseSearch = useCallback((onFinished?: () => void) => {
    if (isSearchClosing.current) return;
    isSearchClosing.current = true;
    setIsClosingSearch(true);
    closeAfterKeyboard.current = false;
    searchRef.current?.blur();
    Keyboard.dismiss();

    const finishCollapse = () => {
      // Phase 2: Now that search bar has physically arrived and docked at the bottom,
      // smoothly collapse horizontally from search pill into the circular FAB in the corner
      Animated.timing(searchProgress, {
        toValue: 0,
        duration: 220,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: false,
      }).start(() => {
        setSearchFocused(false);
        setIsClosingSearch(false);
        isSearchClosing.current = false;
        onFinished?.();
      });
    };

    // Phase 1: Always glide search bar down to resting bottom position first
    Animated.timing(keyboardOffsetAnim, {
      toValue: 0,
      duration: Platform.OS === 'ios' ? 240 : 200,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: Platform.OS !== 'web',
    }).start(() => {
      // Phase 2: ONLY AFTER arriving at resting bottom position, collapse horizontally into FAB
      finishCollapse();
    });
  }, [keyboardOffsetAnim, searchProgress]);

  const dismissSearch = useCallback(() => {
    collapseSearch(() => {
      setCatalogRevealed(true);
      catalogFadeAnim.setValue(0.35);
      Animated.timing(catalogFadeAnim, {
        toValue: 1,
        duration: 250,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start();
    });
  }, [collapseSearch, catalogFadeAnim]);

  const submitSearch = useCallback(() => {
    collapseSearch(() => {
      setCatalogRevealed(true);
      catalogFadeAnim.setValue(0.35);
      Animated.timing(catalogFadeAnim, {
        toValue: 1,
        duration: 250,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start();
      if (query.trim()) {
        setLoading(true);
        loadProducts(query, defaultSort, 1).catch(() => undefined);
      }
    });
  }, [collapseSearch, defaultSort, loadProducts, query, catalogFadeAnim]);

  const openSearch = useCallback(() => {
    if (searchFocused && !isSearchClosing.current) return;
    isSearchClosing.current = false;
    setIsClosingSearch(false);
    if (catalogRevealed) {
      setQuery('');
      setPage(1);
    }
    setCatalogRevealed(false);
    setSearchFocused(true);

    // Focus input immediately in parallel with horizontal expansion
    searchRef.current?.focus();

    Animated.timing(searchProgress, {
      toValue: 1,
      duration: 200,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    }).start();
  }, [catalogRevealed, searchFocused, searchProgress]);

  // Auto-activate search bar when entering quick compare screen (strictly once on mount)
  const hasAutoActivated = useRef(false);
  useEffect(() => {
    if (!hasAutoActivated.current) {
      hasAutoActivated.current = true;
      const frame = requestAnimationFrame(() => {
        searchRef.current?.focus();
      });
      const timer = setTimeout(() => {
        searchRef.current?.focus();
      }, 30);
      return () => {
        cancelAnimationFrame(frame);
        clearTimeout(timer);
      };
    }
  }, []);

  if (Platform.OS === 'web') {
    (window as any).__openSearch = openSearch;
    (window as any).__submitSearch = submitSearch;
    (window as any).__dismissSearch = dismissSearch;
    (window as any).__setQuery = setQuery;
    (window as any).__query = query;
    (window as any).__searchFocused = searchFocused;
    (window as any).__catalogRevealed = catalogRevealed;
    (window as any).__setSelectedProduct = setSelectedProduct;
  }

  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';

    const shown = Keyboard.addListener(showEvent, (e) => {
      keyboardVisible.current = true;
      const h = e.endCoordinates ? e.endCoordinates.height : 0;
      keyboardHeightVal.current = h;
      const targetOffset = Math.max(0, h + spacing.sm - restingBottom);
      Animated.timing(keyboardOffsetAnim, {
        toValue: targetOffset,
        duration: e.duration || 220,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: Platform.OS !== 'web',
      }).start();
    });
    const hidden = Keyboard.addListener(hideEvent, (e) => {
      keyboardVisible.current = false;
      keyboardHeightVal.current = 0;
      if (isSearchClosing.current) {
        // collapseSearch is already in flight and controlling the descent & FAB collapse
        return;
      }
      if (searchFocused) {
        dismissSearch();
      }
    });
    return () => {
      shown.remove();
      hidden.remove();
    };
  }, [dismissSearch, keyboardOffsetAnim, restingBottom, searchFocused]);
  const header = (
    <View style={styles.header}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Back to dashboard"
        onPress={leave}
        style={({ pressed }) => [styles.backButton, pressed && styles.pressed]}
      >
        <Ionicons name="arrow-back" size={26} color={colors.text} />
      </Pressable>
      <View style={[styles.headerIcon, { backgroundColor: withAlpha(colors.primary, '18') }]}>
        <Ionicons name="search" size={22} color={colors.primary} />
      </View>
      <View style={styles.headerCopy}>
        <Text style={[styles.eyebrow, { color: colors.primary }]}>QUICK COMPARE</Text>
        <Text style={[styles.title, { color: colors.text }]}>Open one comparison quickly</Text>
      </View>
    </View>
  );

  const renderItem = useCallback(
    ({ item }: { item: InventoryProduct }) => (
      <View style={singleColumn ? styles.cardSlotSingle : styles.cardSlot}>
        <ComparisonCard
          product={item}
          onPress={() => openProduct(item)}
          isCompact={isCompact}
          isSingleColumn={singleColumn}
        />
      </View>
    ),
    [isCompact, openProduct, singleColumn],
  );

  return (
    <View style={[styles.screen, { backgroundColor: colors.background }]}>
      {header}
      <Animated.View style={[{ flex: 1 }, { opacity: catalogFadeAnim }]}>
        <FlatList
          key={singleColumn ? 'single-column' : 'two-column'}
          data={catalogRevealed ? products : []}
          numColumns={singleColumn ? 1 : 2}
          keyExtractor={(product) => product.id.toString()}
          renderItem={renderItem}
          columnWrapperStyle={singleColumn ? undefined : styles.cardRow}
          contentContainerStyle={styles.content}
          ListHeaderComponent={null}
          ListEmptyComponent={
            !catalogRevealed || loading ? (
              <LoadingState singleColumn={singleColumn} />
            ) : (
              <EmptyState query={query} />
            )
          }
          ListFooterComponent={catalogRevealed && products.length > 0 ? (
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
              tintColor={colors.primary}
              colors={[colors.primary]}
              progressBackgroundColor={colors.surface}
            />
          )}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
          onScroll={handleScroll}
          scrollEventThrottle={16}
          onScrollBeginDrag={() => {
            Keyboard.dismiss();
          }}
          scrollEnabled={catalogRevealed}
          showsVerticalScrollIndicator={false}
          initialNumToRender={6}
          maxToRenderPerBatch={6}
          windowSize={5}
          removeClippedSubviews={Platform.OS !== 'web'}
        />
      </Animated.View>

      <Animated.View
        pointerEvents={searchFocused ? 'auto' : 'none'}
        style={[
          styles.searchDimmer,
          {
            opacity: searchProgress.interpolate({
              inputRange: [0, 1],
              outputRange: [0, 0.58],
            }),
          },
        ]}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Dismiss search and view products"
          onPress={dismissSearch}
          style={StyleSheet.absoluteFill}
        />
      </Animated.View>

      <Animated.View
        style={[
          styles.searchAvoider,
          {
            paddingBottom: restingBottom,
            opacity: searchBarAnim,
            transform: [
              {
                translateY: Animated.add(
                  Animated.multiply(keyboardOffsetAnim, -1),
                  searchBarAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [90, 0],
                  })
                ),
              },
            ],
          },
        ]}
        pointerEvents={searchBarVisible || searchFocused ? 'box-none' : 'none'}
      >
        <View style={styles.searchArea}>
          {suggestions.length > 0 ? (
            <View style={[styles.suggestions, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}>
              {suggestions.map((product) => (
                <Pressable
                  key={product.id}
                  accessibilityRole="button"
                  onPress={() => openProduct(product)}
                  style={({ pressed }) => [styles.suggestionRow, { borderBottomColor: colors.border }, pressed && styles.pressed]}
                >
                  <Ionicons name="cube-outline" size={18} color={colors.primary} />
                  <Text style={[styles.suggestionText, { color: colors.text }]} numberOfLines={1}>{product.productName}</Text>
                  <Text style={[styles.suggestionPrice, { color: colors.textMuted }]}>{formatRupees(product.shopPrice)}</Text>
                </Pressable>
              ))}
            </View>
          ) : null}

          <Animated.View
            style={[
              styles.searchShell,
              {
                width: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [56, expandedSearchWidth] }),
                borderRadius: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [28, 16] }),
                backgroundColor: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [colors.primary, colors.surface] }),
                borderColor: searchProgress.interpolate({ inputRange: [0, 1], outputRange: [colors.primary, colors.border] }),
              },
            ]}
          >
            <Animated.View
              pointerEvents={searchFocused && !isSearchClosing.current ? "auto" : "none"}
              style={[
                styles.searchContent,
                {
                  opacity: searchProgress.interpolate({ inputRange: [0, 0.45, 1], outputRange: [0, 0, 1] }),
                },
              ]}
            >
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Submit search"
                onPress={submitSearch}
                style={styles.searchSubmitButton}
              >
                <Ionicons name="search" size={20} color={colors.primary} />
              </Pressable>

              <TextInput
                ref={searchRef}
                autoFocus={true}
                value={query}
                onChangeText={(value) => {
                  setQuery(value);
                  setPage(1);
                }}
                onFocus={() => {
                  if (!searchFocused) openSearch();
                }}
                onSubmitEditing={submitSearch}
                placeholder="Search…"
                placeholderTextColor={colors.textMuted}
                selectionColor={colors.primary}
                cursorColor={colors.primary}
                returnKeyType="search"
                autoCapitalize="none"
                autoCorrect={false}
                style={[styles.searchInput, { color: colors.text }]}
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
                    <Ionicons name="close-circle" size={18} color={colors.textMuted} />
                  </Pressable>
                ) : null}
              </View>

              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Scan barcode to find and compare product"
                onPress={() => {
                  Keyboard.dismiss();
                  setScannerVisible(true);
                }}
                style={styles.searchCamera}
              >
                <Ionicons name="barcode-outline" size={22} color={colors.primary} />
              </Pressable>
            </Animated.View>

            <Animated.View
              pointerEvents={!searchFocused ? "auto" : "none"}
              style={[
                styles.searchFabLayer,
                {
                  opacity: searchProgress.interpolate({ inputRange: [0, 0.35, 1], outputRange: [1, 0, 0] }),
                },
              ]}
            >
              <Pressable accessibilityRole="button" accessibilityLabel="Open product search" onPress={openSearch} style={styles.searchFabButton}>
                <Ionicons name="search" size={25} color="#0B0F14" />
              </Pressable>
            </Animated.View>
          </Animated.View>
        </View>
      </Animated.View>

      <BarcodeScannerModal
        visible={scannerVisible}
        onClose={() => setScannerVisible(false)}
        onScanned={(value) => {
          setScannerVisible(false);
          setLoading(true);
          setQuery(value);
          setPage(1);
          repository.listComparisonProducts(value, DEFAULT_SORT, 1, PAGE_SIZE).then((res) => {
            setProducts(res.products);
            setTotalPages(res.totalPages);
            setLoading(false);
            checkExactMatchAndAutoOpen(value, res.products);
          }).catch(() => undefined);
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


const ComparisonCard = memo(function ComparisonCard({
  product,
  onPress,
  isCompact,
  isSingleColumn,
}: {
  product: InventoryProduct;
  onPress: () => void;
  isCompact?: boolean;
  isSingleColumn?: boolean;
}) {
  const { customization, colors } = useCustomization();
  const summary = summarizeProductComparison(product);
  const isCompared = product.amazonLastPrice != null || product.flipkartLastPrice != null;
  const isBold = customization.insightCustomization?.priceEmphasis === 'BOLD';
  const tone = !isCompared
    ? colors.warning
    : summary.position === 'REVIEW'
      ? colors.danger
      : colors.primary;

  const label = !isCompared
    ? 'Needs check'
    : summary.position === 'REVIEW'
      ? 'Review • saved'
      : summary.position === 'COMPETITIVE'
        ? 'Competitive • saved'
        : 'Matched • saved';

  const iconName: keyof typeof Ionicons.glyphMap = !isCompared
    ? 'search-outline'
    : summary.position === 'REVIEW'
      ? 'alert-circle'
      : 'trophy';

  if (isSingleColumn) {
    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${product.productName}, shop price ${formatRupees(product.shopPrice)}, ${label}`}
        onPress={onPress}
        style={({ pressed }) => [
          styles.cardSingle,
          { backgroundColor: colors.surface, borderColor: colors.border },
          pressed && styles.cardPressed,
        ]}
      >
        <View style={styles.cardImageShellSingle}>
          {product.imageUrl ? (
            <Image source={{ uri: product.imageUrl }} style={styles.cardImage} contentFit="contain" transition={150} />
          ) : (
            <View style={styles.imagePlaceholder}>
              <Text style={styles.imagePlaceholderLetter}>
                {product.productName.trim().charAt(0).toUpperCase() || 'P'}
              </Text>
            </View>
          )}
        </View>
        <View style={styles.cardBodySingle}>
          <Text style={[styles.cardName, { color: colors.text }]} numberOfLines={2}>
            {product.productName}
          </Text>
          <View style={styles.singlePriceRow}>
            <View>
              <Text style={[styles.shopPriceLabel, { color: colors.textMuted }]}>SHOP PRICE</Text>
              <Text
                style={[
                  styles.cardPrice,
                  { color: colors.text },
                  isBold && { fontFamily: type.bold, fontWeight: '800', fontSize: 17 },
                ]}
              >
                {formatRupees(product.shopPrice)}
              </Text>
            </View>
            <View style={[styles.statusChip, { backgroundColor: withAlpha(tone, '18') }]}>
              <Ionicons name={iconName} size={13} color={tone} />
              <Text style={[styles.statusText, { color: tone }]} numberOfLines={1}>{label}</Text>
            </View>
          </View>
        </View>
      </Pressable>
    );
  }

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${product.productName}, shop price ${formatRupees(product.shopPrice)}, ${label}`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: colors.surface, borderColor: colors.border },
        isCompact && styles.cardCompact,
        pressed && styles.cardPressed,
      ]}
    >
      <View style={[styles.cardImageShell, isCompact && styles.cardImageShellCompact]}>
        {product.imageUrl ? (
          <Image source={{ uri: product.imageUrl }} style={styles.cardImage} contentFit="contain" transition={150} />
        ) : (
          <View style={styles.imagePlaceholder}>
            <Text style={styles.imagePlaceholderLetter}>
              {product.productName.trim().charAt(0).toUpperCase() || 'P'}
            </Text>
          </View>
        )}
      </View>
      <View style={[styles.cardBody, isCompact && styles.cardBodyCompact]}>
        <Text style={[styles.cardName, { color: colors.text }, isCompact && styles.cardNameCompact]} numberOfLines={isCompact ? 1 : 2}>
          {product.productName}
        </Text>
        <Text style={[styles.shopPriceLabel, { color: colors.textMuted }]}>SHOP PRICE</Text>
        <Text
          style={[
            styles.cardPrice,
            { color: colors.text },
            isBold && { fontFamily: type.bold, fontWeight: '800', fontSize: isCompact ? 16 : 18 },
            isCompact && styles.cardPriceCompact,
          ]}
        >
          {formatRupees(product.shopPrice)}
        </Text>
        <View style={[styles.statusChip, isCompact && styles.statusChipCompact, { backgroundColor: withAlpha(tone, '18') }]}>
          <Ionicons name={iconName} size={isCompact ? 11 : 13} color={tone} />
          <Text style={[styles.statusText, isCompact && styles.statusTextCompact, { color: tone }]} numberOfLines={1}>{label}</Text>
        </View>
      </View>
    </Pressable>
  );
});

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
  const { colors } = useCustomization();

  return (
    <View style={styles.pagination}>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: page <= 1 }}
        disabled={page <= 1}
        onPress={onPrevious}
        style={({ pressed }) => [
          styles.pageButton,
          { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
          pressed && styles.pressed,
          page <= 1 && styles.disabled,
        ]}
      >
        <Ionicons name="chevron-back" size={20} color={colors.text} />
        <Text style={[styles.pageButtonText, { color: colors.text }]}>Previous</Text>
      </Pressable>
      <View style={[styles.pageBadge, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}>
        <Text style={[styles.pageBadgeText, { color: colors.textMuted }]}>{page} / {totalPages}</Text>
      </View>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: page >= totalPages }}
        disabled={page >= totalPages}
        onPress={onNext}
        style={({ pressed }) => [
          styles.pageButton,
          { backgroundColor: colors.surfaceRaised, borderColor: colors.border },
          pressed && styles.pressed,
          page >= totalPages && styles.disabled,
        ]}
      >
        <Text style={[styles.pageButtonText, { color: colors.text }]}>Next</Text>
        <Ionicons name="chevron-forward" size={20} color={colors.text} />
      </Pressable>
    </View>
  );
}

const ChatGPTSkeletonCard = memo(function ChatGPTSkeletonCard({
  isSingleColumn,
  pulse1,
  pulse2,
}: {
  isSingleColumn?: boolean;
  pulse1: Animated.Value;
  pulse2: Animated.Value;
}) {
  const lineOpacity1 = pulse1.interpolate({
    inputRange: [0, 1],
    outputRange: [0.35, 0.90],
  });
  const lineOpacity2 = pulse2.interpolate({
    inputRange: [0, 1],
    outputRange: [0.35, 0.90],
  });

  if (isSingleColumn) {
    return (
      <View style={styles.skeletonCardSingle}>
        <View style={styles.skeletonImageSingle}>
          <Ionicons name="image-outline" size={28} color="rgba(255, 255, 255, 0.08)" />
        </View>
        <View style={styles.skeletonBodySingle}>
          <Animated.View style={[styles.skeletonLineTag, { opacity: lineOpacity1 }]} />
          <Animated.View style={[styles.skeletonLineTitle1, { opacity: lineOpacity2 }]} />
          <Animated.View style={[styles.skeletonLineTitle2, { opacity: lineOpacity1 }]} />
          <View style={styles.skeletonBottomRow}>
            <Animated.View style={[styles.skeletonPriceBlock, { opacity: lineOpacity2 }]} />
            <View style={styles.skeletonRetailerRow}>
              <Animated.View style={[styles.skeletonRetailerChip, { opacity: lineOpacity1 }]} />
              <Animated.View style={[styles.skeletonRetailerChip, { opacity: lineOpacity2 }]} />
            </View>
          </View>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.skeletonCard}>
      <View style={styles.skeletonImage}>
        <Ionicons name="image-outline" size={36} color="rgba(255, 255, 255, 0.08)" />
      </View>
      <View style={styles.skeletonBody}>
        <Animated.View style={[styles.skeletonLineTitle1, { opacity: lineOpacity1 }]} />
        <Animated.View style={[styles.skeletonLineTitle2, { opacity: lineOpacity2 }]} />
        <Animated.View style={[styles.skeletonLineEyebrow, { opacity: lineOpacity1 }]} />
        <Animated.View style={[styles.skeletonPriceBlock, { opacity: lineOpacity2 }]} />
        <Animated.View style={[styles.skeletonStatusChip, { opacity: lineOpacity1 }]} />
      </View>
    </View>
  );
});

const LoadingState = memo(function LoadingState({ singleColumn }: { singleColumn?: boolean }) {
  const pulse1 = useRef(new Animated.Value(0)).current;
  const pulse2 = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const loop1 = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse1, {
          toValue: 1,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulse1, {
          toValue: 0,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    );

    const loop2 = Animated.loop(
      Animated.sequence([
        Animated.delay(200),
        Animated.timing(pulse2, {
          toValue: 1,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulse2, {
          toValue: 0,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    );

    loop1.start();
    loop2.start();
    return () => {
      loop1.stop();
      loop2.stop();
    };
  }, [pulse1, pulse2]);

  if (singleColumn) {
    return (
      <View style={styles.skeletonList} accessibilityLabel="Loading comparisons">
        {[0, 1, 2, 3].map((index) => (
          <View key={index} style={styles.cardSlotSingle}>
            <ChatGPTSkeletonCard isSingleColumn pulse1={pulse1} pulse2={pulse2} />
          </View>
        ))}
      </View>
    );
  }

  // Two-column grid matching FlatList row layout exactly
  const pairs = [
    [0, 1],
    [2, 3],
    [4, 5],
  ];

  return (
    <View style={styles.skeletonList} accessibilityLabel="Loading comparisons">
      {pairs.map(([first, second], rowIndex) => (
        <View key={rowIndex} style={styles.cardRow}>
          <View style={styles.cardSlot}>
            <ChatGPTSkeletonCard pulse1={pulse1} pulse2={pulse2} />
          </View>
          <View style={styles.cardSlot}>
            <ChatGPTSkeletonCard pulse1={pulse1} pulse2={pulse2} />
          </View>
        </View>
      ))}
    </View>
  );
});


function EmptyState({ query }: { query: string }) {
  const { colors } = useCustomization();

  return (
    <View style={[styles.stateCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      <Ionicons name={query.trim() ? 'search-outline' : 'cube-outline'} size={38} color={colors.textMuted} />
      <Text style={[styles.stateTitle, { color: colors.text }]}>{query.trim() ? 'No matching products' : 'Inventory is empty'}</Text>
      <Text style={[styles.stateBody, { color: colors.textMuted }]}>
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
  headerClip: { overflow: 'hidden' },
  content: { padding: spacing.lg, paddingTop: spacing.sm, paddingBottom: 112 },
  header: { minHeight: 70, flexDirection: 'row', alignItems: 'center', marginBottom: spacing.sm },
  backButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center', marginLeft: -spacing.sm },
  headerIcon: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 13,
    backgroundColor: 'rgba(16,185,129,0.12)',
    marginHorizontal: spacing.sm,
  },
  headerCopy: { flex: 1, minWidth: 0 },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 20, lineHeight: 24, marginTop: 2 },
  guideCard: {
    padding: 14,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
    gap: 8,
  },
  guideTitle: {
    color: colors.text,
    fontFamily: type.bold,
    fontSize: 14,
    fontWeight: '700',
  },
  guideDescription: {
    color: colors.textMuted,
    fontFamily: type.regular,
    fontSize: 11,
    lineHeight: 16,
  },
  guideChipsRow: {
    flexDirection: 'row',
    gap: 7,
    marginTop: 2,
  },
  guideChip: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 12,
    backgroundColor: 'rgba(16,185,129,0.09)',
  },
  guideChipText: {
    color: colors.primary,
    fontFamily: type.bold,
    fontSize: 10,
    fontWeight: '700',
  },
  searchDimmer: { ...StyleSheet.absoluteFill, zIndex: 10, backgroundColor: 'rgba(0,0,0,0.58)' },
  searchAvoider: { position: 'absolute', zIndex: 20, left: 0, top: 0, right: 0, bottom: 0, justifyContent: 'flex-end', pointerEvents: 'box-none' },
  searchAvoiderContent: { flex: 1, justifyContent: 'flex-end' },
  searchArea: { alignItems: 'flex-end', paddingHorizontal: spacing.lg, pointerEvents: 'box-none' },
  searchShell: { height: 56, overflow: 'hidden', borderWidth: 1, borderColor: colors.border },
  searchContent: { ...StyleSheet.absoluteFill, flexDirection: 'row', alignItems: 'center', paddingLeft: spacing.lg },
  searchSubmitButton: { width: 36, height: 44, alignItems: 'center', justifyContent: 'center' },
  searchSubmitChip: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: radius.sm,
    backgroundColor: colors.primary,
    marginRight: spacing.xs,
  },
  searchSubmitChipText: {
    color: '#0B0F14',
    fontFamily: type.bold,
    fontSize: 12,
    fontWeight: '700',
  },
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
  cardRow: { flexDirection: 'row', gap: spacing.md },
  cardSlot: { flex: 1, maxWidth: '49%', marginBottom: spacing.md },
  cardSlotSingle: { width: '100%', marginBottom: spacing.md },
  card: { overflow: 'hidden', borderWidth: 1, borderColor: colors.border, borderRadius: 20, backgroundColor: colors.surface },
  cardSingle: { flexDirection: 'row', overflow: 'hidden', borderWidth: 1, borderColor: colors.border, borderRadius: 20, backgroundColor: colors.surface, padding: spacing.sm },
  cardCompact: { borderRadius: 16 },
  cardPressed: { opacity: 0.82, transform: [{ scale: 0.99 }] },
  cardImageShell: {
    width: '100%',
    aspectRatio: 1.05,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    overflow: 'hidden',
    padding: 10,
  },
  cardImageShellSingle: {
    width: 88,
    height: 88,
    borderRadius: 16,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
    padding: 6,
  },
  cardImageShellCompact: { aspectRatio: 1.15, padding: 6 },
  cardImage: { width: '100%', height: '100%' },
  imagePlaceholder: { flex: 1, width: '100%', height: '100%', alignItems: 'center', justifyContent: 'center', backgroundColor: '#FFFFFF' },
  imagePlaceholderLetter: { color: '#94A3B8', fontFamily: type.bold, fontSize: 28, fontWeight: '800' },
  cardBody: { flex: 1, padding: spacing.md },
  cardBodySingle: { flex: 1, marginLeft: spacing.md, justifyContent: 'space-between', paddingVertical: 2 },
  cardBodyCompact: { padding: spacing.sm, paddingTop: spacing.xs },
  cardName: { minHeight: 36, color: colors.text, fontFamily: type.bold, fontSize: 13, lineHeight: 18 },
  cardNameCompact: { minHeight: 18, fontSize: 12, lineHeight: 16 },
  singlePriceRow: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 4 },
  shopPriceLabel: { color: colors.textMuted, fontFamily: type.bold, fontSize: 8, letterSpacing: 0.7, marginTop: 5 },
  cardPrice: { color: colors.primary, fontFamily: type.bold, fontSize: 15, fontWeight: '800', marginTop: 1 },
  cardPriceCompact: { fontSize: 14 },
  statusChip: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', borderRadius: radius.pill, paddingHorizontal: 9, paddingVertical: 6, marginTop: 7 },
  statusChipCompact: { paddingHorizontal: 6, paddingVertical: 3, marginTop: 4 },
  statusText: { maxWidth: 118, fontFamily: type.bold, fontSize: 9, marginLeft: 5 },
  statusTextCompact: { fontSize: 8, maxWidth: 100 },
  skeletonGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md },
  skeletonList: { flexDirection: 'column', gap: spacing.md },
  skeletonCard: {
    width: '100%',
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    borderRadius: 20,
    backgroundColor: '#11161D',
  },
  skeletonCardSingle: {
    width: '100%',
    flexDirection: 'row',
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    borderRadius: 20,
    backgroundColor: '#11161D',
    padding: spacing.sm,
  },
  skeletonImage: {
    width: '100%',
    aspectRatio: 1.08,
    backgroundColor: '#151C25',
    alignItems: 'center',
    justifyContent: 'center',
  },
  skeletonImageSingle: {
    width: 88,
    height: 88,
    borderRadius: 14,
    backgroundColor: '#151C25',
    alignItems: 'center',
    justifyContent: 'center',
  },
  skeletonBody: {
    padding: spacing.md,
    gap: 7,
  },
  skeletonBodySingle: {
    flex: 1,
    marginLeft: spacing.md,
    justifyContent: 'space-between',
    paddingVertical: 2,
  },
  skeletonLineTag: {
    width: '32%',
    height: 10,
    borderRadius: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.22)',
  },
  skeletonLineTitle1: {
    width: '92%',
    height: 14,
    borderRadius: 5,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
  },
  skeletonLineTitle2: {
    width: '64%',
    height: 14,
    borderRadius: 5,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
  },
  skeletonLineEyebrow: {
    width: '36%',
    height: 9,
    borderRadius: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.18)',
    marginTop: 4,
  },
  skeletonPriceBlock: {
    width: '46%',
    height: 20,
    borderRadius: 6,
    backgroundColor: 'rgba(0, 230, 153, 0.35)',
    marginTop: 2,
  },
  skeletonStatusChip: {
    width: '68%',
    height: 24,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(255, 255, 255, 0.20)',
    marginTop: 4,
  },
  skeletonBottomRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  skeletonRetailerRow: {
    flexDirection: 'row',
    gap: 4,
  },
  skeletonRetailerChip: {
    width: 36,
    height: 18,
    borderRadius: radius.pill,
    backgroundColor: 'rgba(255, 255, 255, 0.20)',
  },
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
