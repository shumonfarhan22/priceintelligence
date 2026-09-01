import Ionicons from '@expo/vector-icons/Ionicons';
import { Image } from 'expo-image';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  BackHandler,
  Keyboard,
  PanResponder,
  Platform,
  Pressable,
  RefreshControl,
  SectionList,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { BannerTone } from '../../components/BottomBanner';
import type { InventoryProduct } from '../../domain/models';
import type { ValidatedInventoryInput } from '../../domain/inventoryValidation';
import { InventoryRepository } from '../../data/inventoryRepository';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { BarcodeScannerModal } from './BarcodeScannerModal';
import { ProductEditorModal } from './ProductEditorModal';

interface InventorySection {
  title: string;
  count: number;
  data: InventoryProduct[];
}

type ShowBanner = (
  message: string,
  tone?: BannerTone,
  action?: { label: string; onPress: () => void },
) => void;

export function InventoryScreen({
  repository,
  productCount,
  onBack,
  onProductCountChanged,
  showBanner,
}: {
  repository: InventoryRepository;
  productCount: number;
  onBack: () => void;
  onProductCountChanged: (count: number) => void;
  showBanner: ShowBanner;
}) {
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [editorVisible, setEditorVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<InventoryProduct | null>(null);
  const [scannerVisible, setScannerVisible] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<InventoryProduct[] | null>(null);
  const insets = useSafeAreaInsets();
  const requestId = useRef(0);
  const deleteTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingDeleteRef = useRef<InventoryProduct[] | null>(null);
  const searchRef = useRef<TextInput>(null);

  const loadProducts = useCallback(async (search = query) => {
    const id = ++requestId.current;
    const nextProducts = await repository.listProducts(search);
    if (id !== requestId.current) return;
    setProducts(nextProducts);
    const visibleIds = new Set(nextProducts.map((product) => product.id));
    setSelectedIds((current) => new Set([...current].filter((selectedId) => visibleIds.has(selectedId))));
    setLoading(false);
    if (!search.trim()) onProductCountChanged(nextProducts.length);
  }, [onProductCountChanged, query, repository]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      loadProducts(query).catch((error) => {
        setLoading(false);
        showBanner(messageFrom(error), 'error');
      });
    }, query ? 160 : 0);
    return () => clearTimeout(timeout);
  }, [query, loadProducts, showBanner]);

  useEffect(() => () => {
    if (deleteTimer.current) clearTimeout(deleteTimer.current);
    const pending = pendingDeleteRef.current;
    if (pending) repository.deleteProducts(pending.map((product) => product.id)).catch(() => undefined);
  }, [repository]);

  const visibleProducts = useMemo(() => {
    const pendingIds = new Set(pendingDelete?.map((product) => product.id) ?? []);
    return products.filter((product) => !pendingIds.has(product.id));
  }, [pendingDelete, products]);

  const sections = useMemo<InventorySection[]>(() => {
    const grouped = new Map<string, InventoryProduct[]>();
    for (const product of visibleProducts) {
      const group = product.productName.trim().split(/\s+/)[0]?.toLocaleUpperCase() || 'OTHER';
      const list = grouped.get(group) ?? [];
      list.push(product);
      grouped.set(group, list);
    }
    return [...grouped.entries()]
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([title, groupProducts]) => ({
        title,
        count: groupProducts.length,
        data: query.trim() || expandedGroups.has(title) ? groupProducts : [],
      }));
  }, [expandedGroups, query, visibleProducts]);

  const allShownIds = visibleProducts.map((product) => product.id);
  const allShownSelected = allShownIds.length > 0 && allShownIds.every((id) => selectedIds.has(id));

  const toggleSelection = (id: number) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const commitPendingDelete = useCallback(async () => {
    const pending = pendingDeleteRef.current;
    if (!pending) return;
    if (deleteTimer.current) clearTimeout(deleteTimer.current);
    deleteTimer.current = null;
    pendingDeleteRef.current = null;
    setPendingDelete(null);
    try {
      await repository.deleteProducts(pending.map((product) => product.id));
      await loadProducts(query);
      onProductCountChanged(await repository.countProducts());
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    }
  }, [loadProducts, onProductCountChanged, query, repository, showBanner]);

  const leaveInventory = useCallback(() => {
    Keyboard.dismiss();
    commitPendingDelete().finally(onBack);
  }, [commitPendingDelete, onBack]);

  useEffect(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      leaveInventory();
      return true;
    });
    return () => subscription.remove();
  }, [leaveInventory]);

  const queueDelete = useCallback((items: InventoryProduct[]) => {
    if (items.length === 0) return;
    if (pendingDeleteRef.current) {
      commitPendingDelete().catch(() => undefined);
    }
    pendingDeleteRef.current = items;
    setPendingDelete(items);
    setSelectedIds(new Set());
    deleteTimer.current = setTimeout(() => {
      commitPendingDelete().catch(() => undefined);
    }, 5200);
    const label = items.length === 1 ? 'Product removed' : `${items.length} products removed`;
    showBanner(label, 'info', {
      label: 'UNDO',
      onPress: () => {
        if (deleteTimer.current) clearTimeout(deleteTimer.current);
        deleteTimer.current = null;
        pendingDeleteRef.current = null;
        setPendingDelete(null);
      },
    });
  }, [commitPendingDelete, showBanner]);

  const refresh = async () => {
    setRefreshing(true);
    Keyboard.dismiss();
    setQuery('');
    setExpandedGroups(new Set());
    setSelectedIds(new Set());
    try {
      await loadProducts('');
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setRefreshing(false);
    }
  };

  const saveProduct = async (input: ValidatedInventoryInput, editingId: number | null) => {
    await repository.saveProduct(input, editingId);
    const nextQuery = editingId == null ? '' : query;
    if (editingId == null) {
      setQuery('');
      const brand = input.productName.trim().split(/\s+/)[0]?.toLocaleUpperCase();
      if (brand) setExpandedGroups((current) => new Set(current).add(brand));
    }
    await loadProducts(nextQuery);
    const count = await repository.countProducts();
    onProductCountChanged(count);
    showBanner(editingId == null ? 'Product added' : 'Product updated', 'success');
  };

  const openNewProduct = () => {
    Keyboard.dismiss();
    setEditingProduct(null);
    setEditorVisible(true);
  };

  const openEditor = (product: InventoryProduct) => {
    Keyboard.dismiss();
    setEditingProduct(product);
    setEditorVisible(true);
  };

  return (
    <View style={styles.screen}>
      <SectionList
        sections={sections}
        keyExtractor={(item) => item.id.toString()}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        stickySectionHeadersEnabled={false}
        contentContainerStyle={styles.listContent}
        refreshControl={(
          <RefreshControl
            refreshing={refreshing}
            onRefresh={refresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
            progressBackgroundColor={colors.surface}
          />
        )}
        ListHeaderComponent={(
          <View>
            <InventoryHeader
              total={productCount}
              selectionCount={selectedIds.size}
              allShownSelected={allShownSelected}
              onBack={leaveInventory}
              onRefresh={refresh}
              onSelectAll={() => setSelectedIds(allShownSelected ? new Set() : new Set(allShownIds))}
              onClearSelection={() => setSelectedIds(new Set())}
              onDeleteSelection={() => queueDelete(visibleProducts.filter((product) => selectedIds.has(product.id)))}
            />
            <View style={styles.searchShell}>
              <Ionicons name="search" size={23} color={colors.textMuted} />
              <TextInput
                ref={searchRef}
                value={query}
                onChangeText={setQuery}
                placeholder="Search inventory…"
                placeholderTextColor={colors.textMuted}
                selectionColor={colors.primary}
                cursorColor={colors.primary}
                returnKeyType="search"
                autoCapitalize="none"
                autoCorrect={false}
                clearButtonMode="never"
                onSubmitEditing={Keyboard.dismiss}
                style={styles.searchInput}
                accessibilityLabel="Search inventory"
              />
              {query ? (
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Clear inventory search"
                  hitSlop={10}
                  onPress={() => {
                    setQuery('');
                    searchRef.current?.focus();
                  }}
                  style={styles.searchAction}
                >
                  <Ionicons name="close" size={20} color={colors.textMuted} />
                </Pressable>
              ) : null}
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Scan inventory barcode"
                onPress={() => {
                  Keyboard.dismiss();
                  setScannerVisible(true);
                }}
                style={styles.searchAction}
              >
                <Ionicons name="camera" size={23} color={colors.textMuted} />
              </Pressable>
            </View>
          </View>
        )}
        renderSectionHeader={({ section }) => (
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ expanded: query.trim() ? true : expandedGroups.has(section.title) }}
            onPress={() => {
              if (query.trim()) return;
              setExpandedGroups((current) => {
                const next = new Set(current);
                if (next.has(section.title)) next.delete(section.title); else next.add(section.title);
                return next;
              });
            }}
            style={({ pressed }) => [styles.groupHeader, pressed && styles.pressed]}
          >
            <Text style={styles.groupName}>{section.title}</Text>
            <View style={styles.groupMeta}>
              <Text style={styles.groupCount}>{section.count} item{section.count === 1 ? '' : 's'}</Text>
              <Ionicons
                name={(query.trim() || expandedGroups.has(section.title)) ? 'chevron-up' : 'chevron-down'}
                size={19}
                color={colors.textMuted}
              />
            </View>
          </Pressable>
        )}
        renderItem={({ item }) => (
          <ProductRow
            product={item}
            selected={selectedIds.has(item.id)}
            selectionMode={selectedIds.size > 0}
            onPress={() => selectedIds.size > 0 ? toggleSelection(item.id) : openEditor(item)}
            onLongPress={() => toggleSelection(item.id)}
            onEdit={() => openEditor(item)}
            onDelete={() => queueDelete([item])}
          />
        )}
        ListEmptyComponent={!loading ? (
          <View style={styles.emptyState}>
            <Ionicons name={query ? 'search-outline' : 'cube-outline'} size={42} color={colors.textMuted} />
            <Text style={styles.emptyTitle}>{query ? 'No matching products' : 'Your inventory is empty'}</Text>
            <Text style={styles.emptyBody}>
              {query ? 'Try a product name, barcode, Amazon link, or Flipkart link.' : 'Add the first product to start comparing prices.'}
            </Text>
          </View>
        ) : null}
        ListFooterComponent={<View style={{ height: 100 + insets.bottom }} />}
      />

      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Add new product"
        onPress={openNewProduct}
        style={({ pressed }) => [styles.fab, { bottom: spacing.xl + insets.bottom }, pressed && styles.pressed]}
      >
        <Ionicons name="add" size={31} color={colors.background} />
      </Pressable>

      <ProductEditorModal
        visible={editorVisible}
        product={editingProduct}
        onClose={() => setEditorVisible(false)}
        onSave={saveProduct}
        onScanComplete={() => showBanner('Barcode scanned', 'success')}
      />
      <BarcodeScannerModal
        visible={scannerVisible}
        onClose={() => setScannerVisible(false)}
        onScanned={(value) => {
          setScannerVisible(false);
          setQuery(value);
          showBanner('Code scanned. Inventory filtered.', 'success');
        }}
      />
    </View>
  );
}

function InventoryHeader({
  total,
  selectionCount,
  allShownSelected,
  onBack,
  onRefresh,
  onSelectAll,
  onClearSelection,
  onDeleteSelection,
}: {
  total: number;
  selectionCount: number;
  allShownSelected: boolean;
  onBack: () => void;
  onRefresh: () => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onDeleteSelection: () => void;
}) {
  const selectionMode = selectionCount > 0;
  return (
    <View style={styles.header}>
      <Pressable accessibilityRole="button" accessibilityLabel={selectionMode ? 'Clear selection' : 'Back'} onPress={selectionMode ? onClearSelection : onBack} style={styles.headerIcon}>
        <Ionicons name={selectionMode ? 'close' : 'arrow-back'} size={26} color={colors.text} />
      </Pressable>
      <View style={styles.headerBadge}>
        <Ionicons name="archive" size={25} color={colors.primary} />
      </View>
      <View style={styles.headerText}>
        <Text style={styles.headerEyebrow} numberOfLines={1}>
          {selectionMode ? 'INVENTORY SELECTION' : 'SUPREME INVENTORY'}
        </Text>
        <Text style={styles.headerTitle}>
          {selectionMode ? `${selectionCount} selected` : `${total} Total ${total === 1 ? 'Product' : 'Products'}`}
        </Text>
      </View>
      {selectionMode ? (
        <>
          <Pressable accessibilityRole="button" accessibilityLabel={allShownSelected ? 'Deselect all shown' : 'Select all shown'} onPress={onSelectAll} style={styles.headerIcon}>
            <Ionicons name={allShownSelected ? 'checkbox' : 'square-outline'} size={24} color={colors.text} />
          </Pressable>
          <Pressable accessibilityRole="button" accessibilityLabel="Delete selected products" onPress={onDeleteSelection} style={styles.headerIcon}>
            <Ionicons name="trash-outline" size={24} color={colors.danger} />
          </Pressable>
        </>
      ) : (
        <Pressable accessibilityRole="button" accessibilityLabel="Refresh inventory" onPress={onRefresh} style={styles.headerIcon}>
          <Ionicons name="refresh" size={25} color={colors.text} />
        </Pressable>
      )}
    </View>
  );
}

function ProductRow({
  product,
  selected,
  selectionMode,
  onPress,
  onLongPress,
  onEdit,
  onDelete,
}: {
  product: InventoryProduct;
  selected: boolean;
  selectionMode: boolean;
  onPress: () => void;
  onLongPress: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const longPressHandled = useRef(false);
  const swipeX = useRef(new Animated.Value(0)).current;
  const swipeOpen = useRef(false);

  const settleSwipe = (open: boolean) => {
    swipeOpen.current = open;
    Animated.spring(swipeX, {
      toValue: open ? -76 : 0,
      useNativeDriver: Platform.OS !== 'web',
      damping: 22,
      stiffness: 240,
      mass: 0.65,
    }).start();
  };

  const panResponder = useMemo(() => PanResponder.create({
    onMoveShouldSetPanResponder: (_, gesture) => (
      !selectionMode && Math.abs(gesture.dx) > 7 && Math.abs(gesture.dx) > Math.abs(gesture.dy)
    ),
    onPanResponderMove: (_, gesture) => {
      const base = swipeOpen.current ? -76 : 0;
      swipeX.setValue(Math.max(-92, Math.min(0, base + gesture.dx)));
    },
    onPanResponderRelease: (_, gesture) => settleSwipe((swipeOpen.current ? -76 : 0) + gesture.dx < -42),
    onPanResponderTerminate: () => settleSwipe(swipeOpen.current),
  }), [selectionMode, swipeX]);

  return (
    <View style={styles.swipeContainer}>
      <View style={styles.deleteUnderlay}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Delete ${product.productName}`}
          onPress={() => {
            settleSwipe(false);
            onDelete();
          }}
          style={styles.deleteAction}
        >
          <Ionicons name="trash" size={22} color={colors.text} />
          <Text style={styles.deleteText}>Delete</Text>
        </Pressable>
      </View>
      <Animated.View style={{ transform: [{ translateX: swipeX }] }} {...panResponder.panHandlers}>
        <View style={[styles.productRow, selected && styles.productSelected]}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${selectionMode ? 'Select' : 'Open'} ${product.productName}`}
            accessibilityState={{ selected }}
            onPress={() => {
              if (longPressHandled.current) {
                longPressHandled.current = false;
                return;
              }
              if (swipeOpen.current) {
                settleSwipe(false);
                return;
              }
              onPress();
            }}
            onLongPress={() => {
              longPressHandled.current = true;
              settleSwipe(false);
              onLongPress();
            }}
            style={({ pressed }) => [styles.productMain, pressed && styles.pressed]}
          >
            <View style={styles.productImageShell}>
              {product.imageUrl ? (
                <Image source={product.imageUrl} style={styles.productImage} contentFit="contain" transition={120} />
              ) : (
                <Ionicons name="cube-outline" size={25} color={colors.textMuted} />
              )}
            </View>
            <View style={styles.productContent}>
              <Text style={styles.productName} numberOfLines={2}>{product.productName}</Text>
              <View style={styles.pricePill}>
                <Ionicons name="pricetag" size={13} color={colors.primary} />
                <Text style={styles.priceValue} numberOfLines={1}>SUPREME • {formatInr(product.shopPrice)}</Text>
              </View>
            </View>
            {selectionMode ? (
              <Ionicons name={selected ? 'checkmark-circle' : 'ellipse-outline'} size={25} color={selected ? colors.primary : colors.textMuted} />
            ) : null}
          </Pressable>
          {!selectionMode ? (
            <Pressable accessibilityRole="button" accessibilityLabel={`Edit ${product.productName}`} onPress={onEdit} hitSlop={8} style={styles.rowAction}>
              <Ionicons name="pencil" size={20} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </View>
      </Animated.View>
    </View>
  );
}

function formatInr(value: number): string {
  return `₹${new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value)}`;
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  listContent: { paddingHorizontal: spacing.lg, paddingTop: spacing.sm },
  header: { minHeight: 60, flexDirection: 'row', alignItems: 'center' },
  headerIcon: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  headerBadge: { width: 44, height: 44, borderRadius: 13, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.primaryMuted, marginLeft: spacing.xs },
  headerText: { flex: 1, marginLeft: 10 },
  headerEyebrow: { color: colors.text, fontFamily: type.bold, fontSize: 13, letterSpacing: 0.2 },
  headerTitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, marginTop: 2 },
  searchShell: { height: 56, flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, borderRadius: 12, paddingLeft: 14, paddingRight: spacing.xs, marginVertical: spacing.md },
  searchInput: { flex: 1, minHeight: 54, color: colors.text, fontFamily: type.regular, fontSize: 16, paddingHorizontal: spacing.md },
  searchAction: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  groupHeader: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderWidth: 1, borderRadius: 10, paddingHorizontal: 14, marginBottom: spacing.sm },
  groupName: { color: colors.primary, fontFamily: type.bold, fontSize: 15, letterSpacing: 0.3 },
  groupMeta: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  groupCount: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13 },
  swipeContainer: { overflow: 'hidden', borderRadius: 13, marginBottom: 10 },
  deleteUnderlay: { ...StyleSheet.absoluteFill, alignItems: 'flex-end', justifyContent: 'center', borderRadius: 13, backgroundColor: colors.danger },
  deleteAction: { width: 76, height: '100%', alignItems: 'center', justifyContent: 'center' },
  deleteText: { color: colors.text, fontFamily: type.bold, fontSize: 10, marginTop: 2 },
  productRow: { minHeight: 84, flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, borderRadius: 13, padding: 9 },
  productMain: { flex: 1, minWidth: 0, flexDirection: 'row', alignItems: 'center' },
  productSelected: { borderColor: colors.primary, backgroundColor: colors.primaryMuted },
  productImageShell: { width: 64, height: 64, borderRadius: radius.sm, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceRaised, overflow: 'hidden' },
  productImage: { width: '100%', height: '100%', backgroundColor: '#F8FAFC' },
  productContent: { flex: 1, marginLeft: 11, marginRight: spacing.sm },
  productName: { color: colors.text, fontFamily: type.bold, fontSize: 14, lineHeight: 18 },
  pricePill: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', borderWidth: 1, borderColor: 'rgba(16,185,129,0.70)', borderRadius: 7, paddingHorizontal: spacing.sm, paddingVertical: spacing.xs, marginTop: 6 },
  priceValue: { flexShrink: 1, color: colors.primary, fontFamily: type.bold, fontSize: 10, marginLeft: 6 },
  rowAction: { width: 42, height: 42, alignItems: 'center', justifyContent: 'center' },
  emptyState: { alignItems: 'center', paddingHorizontal: spacing.xl, paddingVertical: 72 },
  emptyTitle: { color: colors.text, fontFamily: type.bold, fontSize: 20, marginTop: spacing.lg },
  emptyBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 14, lineHeight: 21, textAlign: 'center', marginTop: spacing.sm },
  fab: {
    position: 'absolute',
    right: 14,
    width: 56,
    height: 56,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primary,
    ...Platform.select({
      web: { boxShadow: '0 5px 10px rgba(0, 0, 0, 0.35)' },
      default: {
        shadowColor: '#000',
        shadowOpacity: 0.35,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 5 },
        elevation: 9,
      },
    }),
  },
  pressed: { opacity: 0.72 },
});
