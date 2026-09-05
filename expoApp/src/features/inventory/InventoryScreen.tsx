import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Image } from 'expo-image';
import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  BackHandler,
  Easing,
  Keyboard,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  LayoutAnimation,
  PanResponder,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  SectionList,
  StyleSheet,
  Text,
  TextInput,
  UIManager,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

import type { BannerTone } from '../../components/BottomBanner';
import type { InventoryProduct } from '../../domain/models';
import type { ValidatedInventoryInput } from '../../domain/inventoryValidation';
import { InventoryRepository } from '../../data/inventoryRepository';
import { spacing, type } from '../../theme/tokens';
import { useCustomization } from '../../theme/CustomizationContext';
import type { DynamicColors } from '../../theme/dynamicTheme';

import { BarcodeScannerModal } from './BarcodeScannerModal';
import { ProductEditorModal } from './ProductEditorModal';

function useInventoryTheme() {
  const { colors } = useCustomization();
  const styles = useMemo(() => createStyles(colors), [colors]);
  return { colors, styles };
}

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
  bannerVisible,
  onQueueUndo,
}: {
  repository: InventoryRepository;
  productCount: number;
  onBack: () => void;
  onProductCountChanged: (count: number) => void;
  showBanner: ShowBanner;
  bannerVisible: boolean;
  onQueueUndo?: (notice: { itemCount: number; onUndo: () => void }) => void;
}) {
  const { colors, styles } = useInventoryTheme();
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());
  const [fullyRenderedGroups, setFullyRenderedGroups] = useState<Set<string>>(new Set());
  const deferTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [editorVisible, setEditorVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<InventoryProduct | null>(null);

  const selectionCount = selectedIds.size;
  const [scannerVisible, setScannerVisible] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<InventoryProduct[] | null>(null);
  const insets = useSafeAreaInsets();
  const requestId = useRef(0);
  const deleteTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingDeleteRef = useRef<InventoryProduct[] | null>(null);
  const searchRef = useRef<TextInput>(null);
  const [isSwiping, setIsSwiping] = useState(false);
  const fabAnim = useRef(new Animated.Value(1)).current;

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
    }, query ? 250 : 0);
    return () => clearTimeout(timeout);
  }, [query, loadProducts, showBanner]);

  useEffect(() => () => {
    if (deleteTimer.current) clearTimeout(deleteTimer.current);
    if (deferTimerRef.current) clearTimeout(deferTimerRef.current);
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
        data: groupProducts,
      }));
  }, [visibleProducts]);

  const toggleGroup = useCallback((title: string) => {
    if (deferTimerRef.current) {
      clearTimeout(deferTimerRef.current);
      deferTimerRef.current = null;
    }

    setExpandedGroups((current) => {
      const isCurrentlyExpanded = current.has(title);
      const next = new Set(current);

      if (isCurrentlyExpanded) {
        LayoutAnimation.configureNext({
          duration: 180,
          update: {
            type: LayoutAnimation.Types.easeInEaseOut,
          },
        });
        next.delete(title);
        setFullyRenderedGroups((prev) => {
          const updated = new Set(prev);
          updated.delete(title);
          return updated;
        });
      } else {
        LayoutAnimation.configureNext({
          duration: 200,
          update: {
            type: LayoutAnimation.Types.easeInEaseOut,
          },
        });
        next.add(title);
        deferTimerRef.current = setTimeout(() => {
          setFullyRenderedGroups((prev) => new Set(prev).add(title));
        }, 90);
      }
      return next;
    });
  }, []);

  const allShownIds = visibleProducts.map((product) => product.id);
  const allShownSelected = allShownIds.length > 0 && allShownIds.every((id) => selectedIds.has(id));
  const selectionMode = selectedIds.size > 0;
  const fabVisible = !selectionMode && !bannerVisible && visibleProducts.length > 0;

  useEffect(() => {
    Animated.timing(fabAnim, {
      toValue: fabVisible ? 1 : 0,
      duration: 200,
      useNativeDriver: true,
    }).start();
  }, [fabAnim, fabVisible]);

  const selectedIdsRef = useRef(selectedIds);
  selectedIdsRef.current = selectedIds;

  const toggleSelection = useCallback((id: number) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }, []);

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
    if (deleteTimer.current) clearTimeout(deleteTimer.current);
    deleteTimer.current = setTimeout(() => {
      commitPendingDelete().catch(() => undefined);
    }, 4000);

    if (onQueueUndo) {
      onQueueUndo({
        itemCount: items.length,
        onUndo: () => {
          if (deleteTimer.current) clearTimeout(deleteTimer.current);
          deleteTimer.current = null;
          pendingDeleteRef.current = null;
          setPendingDelete(null);
        },
      });
    } else {
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
    }
  }, [commitPendingDelete, onQueueUndo, showBanner]);

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

  const openNewProduct = useCallback(() => {
    Keyboard.dismiss();
    setEditingProduct(null);
    setEditorVisible(true);
  }, []);

  const openEditor = useCallback((product: InventoryProduct) => {
    Keyboard.dismiss();
    setEditingProduct(product);
    setEditorVisible(true);
  }, []);

  const handleProductPress = useCallback(
    (product: InventoryProduct) => {
      if (selectedIdsRef.current.size > 0) {
        toggleSelection(product.id);
      } else {
        openEditor(product);
      }
    },
    [openEditor, toggleSelection],
  );

  const handleProductLongPress = useCallback(
    (product: InventoryProduct) => {
      toggleSelection(product.id);
    },
    [toggleSelection],
  );

  const handleProductEdit = useCallback(
    (product: InventoryProduct) => {
      openEditor(product);
    },
    [openEditor],
  );

  const handleProductDelete = useCallback(
    (product: InventoryProduct) => {
      queueDelete([product]);
    },
    [queueDelete],
  );

  return (
    <View style={styles.screen}>
      <View style={styles.directoryChrome}>
        <View style={styles.headerClip}>
          <InventoryHeader
            total={loading ? productCount : visibleProducts.length}
            selectionCount={selectedIds.size}
            allShownSelected={allShownSelected}
            refreshing={refreshing}
            onBack={leaveInventory}
            onRefresh={refresh}
            onSelectAll={() => setSelectedIds(allShownSelected ? new Set() : new Set(allShownIds))}
            onClearSelection={() => setSelectedIds(new Set())}
            onDeleteSelection={() => queueDelete(visibleProducts.filter((product) => selectedIds.has(product.id)))}
          />
        </View>
        <View style={styles.searchShell}>
          <MaterialIcons name="search" size={21} color={colors.textMuted} />
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
          <View style={styles.searchClearSlot}>
            {query ? (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Clear inventory search"
                hitSlop={10}
                onPress={() => {
                  setQuery('');
                  Keyboard.dismiss();
                }}
                style={styles.searchAction}
              >
                <MaterialIcons name="close" size={20} color={colors.textMuted} />
              </Pressable>
            ) : null}
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Scan inventory barcode"
            onPress={() => {
              Keyboard.dismiss();
              setScannerVisible(true);
            }}
            style={styles.searchAction}
          >
            <MaterialIcons name="camera-alt" size={21} color={colors.textMuted} />
          </Pressable>
        </View>
      </View>
      <ScrollView
        scrollEnabled={!isSwiping}
        bounces={true}
        directionalLockEnabled={true}
        style={styles.list}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        contentContainerStyle={[
          styles.listContent,
          {
            paddingBottom: 110 + insets.bottom,
          },
        ]}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.primary} />}
      >
        {sections.map((section) => {
          const isSearching = Boolean(query.trim());
          const isExpanded = isSearching || expandedGroups.has(section.title);
          const isFullyRendered = isSearching || fullyRenderedGroups.has(section.title) || section.data.length <= 6;
          const displayProducts = isFullyRendered ? section.data : section.data.slice(0, 6);

          return (
            <View key={section.title} style={styles.groupContainer}>
              <SectionHeader
                title={section.title}
                count={section.count}
                isExpanded={isExpanded}
                onToggle={toggleGroup}
              />
              {isExpanded ? (
                <View style={styles.groupProducts}>
                  {displayProducts.map((item, index) => (
                    <AnimatedProductRowWrapper
                      key={item.id}
                      index={index}
                      isSearching={isSearching}
                      isDeferred={index >= 6}
                    >
                      <ProductRow
                        product={item}
                        selected={selectedIds.has(item.id)}
                        selectionMode={selectionMode}
                        onPress={handleProductPress}
                        onLongPress={handleProductLongPress}
                        onEdit={handleProductEdit}
                        onDelete={handleProductDelete}
                        onSwipeActiveChange={setIsSwiping}
                      />
                    </AnimatedProductRowWrapper>
                  ))}
                </View>
              ) : null}
            </View>
          );
        })}

        {!loading && visibleProducts.length === 0 ? (
          <CompactInventoryEmptyState isSearching={Boolean(query.trim())} />
        ) : null}
      </ScrollView>

      <Animated.View
        style={[
          styles.fabWrapper,
          {
            bottom: 20 + insets.bottom,
            opacity: fabAnim,
            transform: [
              {
                scale: fabAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [0.6, 1],
                }),
              },
            ],
          },
        ]}
        pointerEvents={fabVisible ? 'auto' : 'none'}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Add new product"
          onPress={openNewProduct}
          style={({ pressed }) => [styles.fab, pressed && styles.pressed]}
        >
          <MaterialIcons name="add" size={27} color="#FFFFFF" />
        </Pressable>
      </Animated.View>

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

const InventoryHeader = memo(function InventoryHeader({
  total,
  selectionCount,
  allShownSelected,
  refreshing,
  onBack,
  onRefresh,
  onSelectAll,
  onClearSelection,
  onDeleteSelection,
}: {
  total: number;
  selectionCount: number;
  allShownSelected: boolean;
  refreshing: boolean;
  onBack: () => void;
  onRefresh: () => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onDeleteSelection: () => void;
}) {
  const { colors, styles } = useInventoryTheme();
  const selectionMode = selectionCount > 0;
  if (selectionMode) {
    return (
      <View style={styles.selectionHeader}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Cancel selection"
          onPress={onClearSelection}
          style={styles.headerIcon}
        >
          <MaterialIcons name="close" size={24} color={colors.text} />
        </Pressable>
        <Text style={styles.selectionTitle}>{selectionCount} selected</Text>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={allShownSelected ? 'Deselect all shown' : 'Select all shown'}
          onPress={onSelectAll}
          style={styles.selectionAllButton}
        >
          <Text style={styles.selectionAllText}>{allShownSelected ? 'None' : 'All'}</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Delete selected products"
          onPress={onDeleteSelection}
          style={styles.headerIcon}
        >
          <MaterialIcons name="delete" size={22} color={colors.danger} />
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.header}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Back"
        onPress={onBack}
        style={styles.headerIcon}
      >
        <MaterialIcons name="arrow-back" size={24} color={colors.primary} />
      </Pressable>
      <View style={styles.headerBadge}>
        <MaterialIcons name="inventory-2" size={22} color={colors.primary} />
      </View>
      <View style={styles.headerText}>
        <View style={styles.headerTitleRow}>
          <Text style={styles.headerBrand}>SUPREME </Text>
          <Text style={styles.headerName}>INVENTORY</Text>
        </View>
        <Text style={styles.headerSubtitle}>
          {total} Total {total === 1 ? 'Product' : 'Products'}
        </Text>
      </View>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Refresh inventory"
        onPress={onRefresh}
        disabled={refreshing}
        style={styles.headerIcon}
      >
        {refreshing ? (
          <ActivityIndicator size={20} color={colors.primary} />
        ) : (
          <MaterialIcons name="refresh" size={24} color={colors.text} />
        )}
      </Pressable>
    </View>
  );
});

const SectionHeader = memo(function SectionHeader({
  title,
  count,
  isExpanded,
  onToggle,
}: {
  title: string;
  count: number;
  isExpanded: boolean;
  onToggle: (title: string) => void;
}) {
  const { colors, styles } = useInventoryTheme();
  const rotation = useRef(new Animated.Value(isExpanded ? 1 : 0)).current;
  const pressScale = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    Animated.timing(rotation, {
      toValue: isExpanded ? 1 : 0,
      duration: 200,
      easing: Easing.bezier(0.4, 0, 0.2, 1),
      useNativeDriver: true,
    }).start();
  }, [isExpanded, rotation]);

  const spin = rotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '180deg'],
  });

  const handlePressIn = () => {
    Animated.spring(pressScale, {
      toValue: 0.985,
      useNativeDriver: true,
      damping: 18,
      stiffness: 300,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(pressScale, {
      toValue: 1,
      useNativeDriver: true,
      damping: 18,
      stiffness: 300,
    }).start();
  };

  return (
    <Animated.View style={{ transform: [{ scale: pressScale }] }}>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ expanded: isExpanded }}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        onPress={() => {
          onToggle(title);
        }}
        style={({ pressed }) => [styles.groupHeader, pressed && styles.groupHeaderPressed]}
      >
        <Text style={styles.groupName}>{title}</Text>
        <View style={styles.groupMeta}>
          <Text style={styles.groupCount}>
            {count === 1 ? '1 item' : `${count} items`}
          </Text>
          <Animated.View style={{ transform: [{ rotate: spin }] }}>
            <MaterialIcons name="expand-more" size={20} color={colors.textMuted} />
          </Animated.View>
        </View>
      </Pressable>
    </Animated.View>
  );
});

const AnimatedProductRowWrapper = memo(function AnimatedProductRowWrapper({
  index,
  isSearching,
  isDeferred,
  children,
}: {
  index: number;
  isSearching: boolean;
  isDeferred?: boolean;
  children: React.ReactNode;
}) {
  const anim = useRef(new Animated.Value(isSearching || isDeferred ? 1 : 0)).current;

  useEffect(() => {
    if (isSearching || isDeferred) {
      anim.setValue(1);
      return;
    }
    Animated.timing(anim, {
      toValue: 1,
      duration: 150,
      delay: Math.min(index * 12, 60),
      useNativeDriver: true,
      easing: Easing.out(Easing.cubic),
    }).start();
  }, [anim, index, isSearching, isDeferred]);

  return (
    <Animated.View
      style={{
        opacity: anim,
        transform: [
          {
            translateY: anim.interpolate({
              inputRange: [0, 1],
              outputRange: [7, 0],
            }),
          },
        ],
      }}
    >
      {children}
    </Animated.View>
  );
});

function MaterialCheckbox({ checked }: { checked: boolean }) {
  const { styles } = useInventoryTheme();
  const checkScale = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.spring(checkScale, {
      toValue: 1,
      damping: 16,
      stiffness: 280,
      useNativeDriver: true,
    }).start();
  }, [checkScale]);

  return (
    <Animated.View
      style={[
        styles.checkbox,
        checked ? styles.checkboxChecked : styles.checkboxUnchecked,
        { transform: [{ scale: checkScale }] },
      ]}
    >
      {checked ? (
        <MaterialIcons name="check" size={13} color="#FFFFFF" />
      ) : null}
    </Animated.View>
  );
}

const ProductRow = memo(function ProductRow({
  product,
  selected,
  selectionMode,
  onPress,
  onLongPress,
  onEdit,
  onDelete,
  onSwipeActiveChange,
}: {
  product: InventoryProduct;
  selected: boolean;
  selectionMode: boolean;
  onPress: (product: InventoryProduct) => void;
  onLongPress: (product: InventoryProduct) => void;
  onEdit: (product: InventoryProduct) => void;
  onDelete: (product: InventoryProduct) => void;
  onSwipeActiveChange?: (active: boolean) => void;
}) {
  const { colors, styles } = useInventoryTheme();
  const swipeX = useRef(new Animated.Value(0)).current;
  const pressScale = useRef(new Animated.Value(1)).current;
  const swipeOpen = useRef(false);
  const isDragging = useRef(false);
  const longPressHandled = useRef(false);

  const settleSwipe = useCallback(
    (open: boolean) => {
      swipeOpen.current = open;
      Animated.spring(swipeX, {
        toValue: open ? -80 : 0,
        useNativeDriver: true,
        bounciness: 0,
        speed: 20,
      }).start();
    },
    [swipeX],
  );

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => false,
        onMoveShouldSetPanResponder: (_, gesture) => {
          if (selectionMode) return false;
          return Math.abs(gesture.dx) > 12 && Math.abs(gesture.dy) < 12;
        },
        onPanResponderGrant: () => {
          isDragging.current = true;
          swipeX.stopAnimation();
          onSwipeActiveChange?.(true);
        },
        onPanResponderMove: (_, gesture) => {
          if (selectionMode) return;
          const base = swipeOpen.current ? -80 : 0;
          const next = Math.max(-100, Math.min(0, base + gesture.dx));
          swipeX.setValue(next);
        },
        onPanResponderRelease: (_, gesture) => {
          isDragging.current = false;
          onSwipeActiveChange?.(false);
          if (selectionMode) {
            settleSwipe(false);
            return;
          }
          const movedLeft = gesture.dx < -30 || (swipeOpen.current && gesture.dx < 20);
          settleSwipe(movedLeft);
        },
        onPanResponderTerminate: () => {
          isDragging.current = false;
          onSwipeActiveChange?.(false);
          settleSwipe(false);
        },
      }),
    [onSwipeActiveChange, selectionMode, settleSwipe, swipeX],
  );

  const handlePressIn = useCallback(() => {
    if (isDragging.current) return;
    Animated.spring(pressScale, {
      toValue: 0.985,
      useNativeDriver: true,
      damping: 18,
      stiffness: 300,
    }).start();
  }, [pressScale]);

  const handlePressOut = useCallback(() => {
    Animated.spring(pressScale, {
      toValue: 1,
      useNativeDriver: true,
      damping: 18,
      stiffness: 300,
    }).start();
  }, [pressScale]);

  useEffect(() => {
    if (!selectionMode && swipeOpen.current) {
      settleSwipe(false);
    }
  }, [selectionMode, settleSwipe]);

  const colorAnim = useRef(new Animated.Value(selected ? 1 : 0)).current;
  const isInitialMount = useRef(true);

  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }
    Animated.timing(colorAnim, {
      toValue: selected ? 1 : 0,
      duration: 250,
      useNativeDriver: false,
    }).start();
  }, [selected, colorAnim]);

  const animatedBackgroundColor = colorAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [colors.surface, colors.isDark ? '#123626' : '#DCFCE7'],
  });
  const animatedBorderColor = colorAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [colors.border, colors.primary],
  });

  return (
    <Animated.View style={{ transform: [{ scale: pressScale }] }}>
      <View style={styles.swipeContainer}>
        {!selectionMode ? (
          <Animated.View
            style={[
              styles.deleteUnderlay,
              {
                opacity: swipeX.interpolate({
                  inputRange: [-80, -10, 0],
                  outputRange: [1, 0.4, 0],
                  extrapolate: 'clamp',
                }),
              },
            ]}
          >
            <Animated.View
              style={[
                styles.deleteAction,
                {
                  transform: [
                    {
                      scale: swipeX.interpolate({
                        inputRange: [-80, -20, 0],
                        outputRange: [1, 0.6, 0],
                        extrapolate: 'clamp',
                      }),
                    },
                  ],
                },
              ]}
            >
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={`Delete ${product.productName}`}
                onPress={() => {
                  settleSwipe(false);
                  onDelete(product);
                }}
                style={styles.deletePressable}
              >
                <MaterialIcons name="delete" size={24} color={colors.danger} />
              </Pressable>
            </Animated.View>
          </Animated.View>
        ) : null}
        <Animated.View
          style={[
            styles.productRow,
            {
              backgroundColor: animatedBackgroundColor as any,
              borderColor: animatedBorderColor as any,
              transform: [{ translateX: swipeX }],
            },
          ]}
          {...panResponder.panHandlers}
        >
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${selectionMode ? 'Select' : 'Open'} ${product.productName}`}
            accessibilityState={{ selected }}
            onPressIn={handlePressIn}
            onPressOut={handlePressOut}
            onPress={() => {
              if (longPressHandled.current) {
                longPressHandled.current = false;
                return;
              }
              if (swipeOpen.current) {
                settleSwipe(false);
                return;
              }
              onPress(product);
            }}
            onLongPress={() => {
              longPressHandled.current = true;
              settleSwipe(false);
              onLongPress(product);
            }}
            style={({ pressed }) => [styles.productMain, pressed && styles.pressed]}
          >
            {selectionMode ? <MaterialCheckbox checked={selected} /> : null}
            <View style={styles.productImageShell}>
              {product.imageUrl ? (
                <Image
                  source={product.imageUrl}
                  style={styles.productImage}
                  contentFit="contain"
                  cachePolicy="memory-disk"
                  recyclingKey={product.imageUrl}
                />
              ) : (
                <MaterialIcons name="camera-alt" size={26} color={colors.textMuted} />
              )}
            </View>
            <View style={styles.productContent}>
              <Text style={styles.productName} numberOfLines={2}>
                {product.productName}
              </Text>
              <View style={styles.pricePill}>
                <MaterialIcons name="sell" size={12} color={colors.primary} />
                <Text style={styles.priceLabel} numberOfLines={1}>
                  Supreme Price:
                </Text>
                <Text style={styles.priceValue} numberOfLines={1}>
                  {formatInr(product.shopPrice)}
                </Text>
              </View>
            </View>
          </Pressable>
          {!selectionMode ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`Edit ${product.productName}`}
              onPress={() => onEdit(product)}
              hitSlop={8}
              style={styles.rowAction}
            >
              <MaterialIcons name="edit" size={20} color={colors.textMuted} />
            </Pressable>
          ) : null}
        </Animated.View>
      </View>
    </Animated.View>
  );
});

function CompactInventoryEmptyState({ isSearching }: { isSearching: boolean }) {
  const { colors, styles } = useInventoryTheme();
  return (
    <View style={styles.emptyState}>
      <MaterialIcons
        name={isSearching ? 'search' : 'inventory-2'}
        size={40}
        color={colors.textMuted}
      />
      <Text style={styles.emptyTitle}>
        {isSearching ? 'No matching products' : 'Inventory is empty'}
      </Text>
      <Text style={styles.emptyBody}>
        {isSearching ? 'Try another search' : 'Tap + to add a product'}
      </Text>
    </View>
  );
}

function formatInr(value: number): string {
  return `\u20B9${new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(value)}`;
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const createStyles = (colors: DynamicColors) =>
  StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.background },
    directoryChrome: { paddingHorizontal: spacing.lg },
    headerClip: { overflow: 'hidden' },
    list: { flex: 1 },
    listContent: { paddingHorizontal: spacing.lg, paddingTop: spacing.xs },
    header: { minHeight: 60, flexDirection: 'row', alignItems: 'center' },
    selectionHeader: { minHeight: 52, flexDirection: 'row', alignItems: 'center' },
    headerIcon: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
    headerBadge: {
      width: 40,
      height: 40,
      borderRadius: 13,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: 'rgba(16, 185, 129, 0.12)',
      marginLeft: spacing.xs,
    },
    headerText: { flex: 1, marginLeft: 10 },
    headerTitleRow: { flexDirection: 'row', alignItems: 'center' },
    headerBrand: { color: colors.primary, fontFamily: type.bold, fontSize: 19, lineHeight: 21 },
    headerName: { color: colors.text, fontFamily: type.bold, fontSize: 19, lineHeight: 21 },
    headerSubtitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, marginTop: 1 },
    selectionTitle: { flex: 1, color: colors.text, fontFamily: type.bold, fontSize: 16, marginLeft: spacing.xs },
    selectionAllButton: { minWidth: 52, minHeight: 44, alignItems: 'center', justifyContent: 'center' },
    selectionAllText: { color: colors.textMuted, fontFamily: type.bold, fontSize: 13 },
    searchShell: {
      height: 56,
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: colors.surface,
      borderColor: colors.border,
      borderWidth: 1,
      borderRadius: 12,
      paddingLeft: 14,
      paddingRight: spacing.xs,
      marginVertical: spacing.md,
    },
    searchInput: {
      flex: 1,
      minHeight: 54,
      color: colors.text,
      fontFamily: type.regular,
      fontSize: 16,
      paddingHorizontal: spacing.md,
    },
    searchClearSlot: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
    searchAction: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
    groupContainer: {
      marginVertical: 4,
    },
    groupProducts: {
      gap: 10,
      marginTop: 8,
      marginBottom: 6,
    },
    groupHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      backgroundColor: colors.isDark ? 'rgba(49, 53, 64, 0.55)' : colors.surfaceRaised,
      borderColor: colors.border,
      borderWidth: 1,
      borderRadius: 8,
      paddingHorizontal: 14,
      paddingVertical: 12,
    },
    groupHeaderPressed: { opacity: 0.72 },
    groupName: {
      color: colors.primary,
      fontFamily: Platform.select({ ios: 'System', default: type.bold }),
      fontWeight: '700',
      fontSize: 15,
      flex: 1,
    },
    groupMeta: { flexDirection: 'row', alignItems: 'center', gap: 8 },
    groupCount: {
      color: colors.textMuted,
      fontFamily: Platform.select({ ios: 'System', default: type.regular }),
      fontWeight: '400',
      fontSize: 13,
    },
    swipeContainer: { overflow: 'hidden', borderRadius: 12 },
    deleteUnderlay: {
      ...StyleSheet.absoluteFill,
      alignItems: 'flex-end',
      justifyContent: 'center',
      borderRadius: 12,
      backgroundColor: 'rgba(239, 68, 68, 0.20)',
      borderWidth: 1,
      borderColor: 'rgba(239, 68, 68, 0.50)',
    },
    deleteAction: { width: 68, height: '100%', alignItems: 'center', justifyContent: 'center' },
    deletePressable: { width: 68, height: '100%', alignItems: 'center', justifyContent: 'center' },
    productRow: {
      flexDirection: 'row',
      alignItems: 'center',
      minHeight: 100,
      backgroundColor: colors.surface,
      borderColor: colors.border,
      borderWidth: 1,
      borderRadius: 12,
      padding: 12,
    },
    productMain: { flex: 1, minWidth: 0, flexDirection: 'row', alignItems: 'center' },
    checkbox: {
      width: 18,
      height: 18,
      borderRadius: 3,
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 10,
    },
    checkboxUnchecked: {
      borderWidth: 1.5,
      borderColor: colors.border,
      backgroundColor: 'transparent',
    },
    checkboxChecked: {
      borderWidth: 1.5,
      borderColor: colors.primary,
      backgroundColor: colors.primary,
    },
    productImageShell: {
      width: 76,
      height: 76,
      borderRadius: 10,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.isDark ? '#14181D' : '#F8FAFC',
      borderWidth: 1,
      borderColor: colors.border,
      overflow: 'hidden',
      padding: 6,
    },
    productImage: { width: '100%', height: '100%' },
    productContent: {
      flex: 1,
      marginLeft: 14,
      marginRight: spacing.xs,
      justifyContent: 'center',
    },
    productName: {
      color: colors.text,
      fontFamily: Platform.select({ ios: 'System', default: type.semibold }),
      fontWeight: '600',
      fontSize: 15,
      lineHeight: 21,
    },
    pricePill: {
      flexDirection: 'row',
      alignItems: 'center',
      alignSelf: 'flex-start',
      borderWidth: 1,
      borderColor: colors.isDark ? 'rgba(16, 185, 129, 0.30)' : 'rgba(16, 185, 129, 0.40)',
      backgroundColor: colors.isDark ? 'transparent' : 'rgba(16, 185, 129, 0.08)',
      borderRadius: 6,
      paddingHorizontal: 8,
      paddingVertical: 4,
      marginTop: 10,
    },
    priceLabel: {
      color: colors.primary,
      fontFamily: Platform.select({ ios: 'System', default: type.regular }),
      fontWeight: '500',
      fontSize: 12,
      marginLeft: 6,
      marginRight: 5,
    },
    priceValue: {
      color: colors.primary,
      fontFamily: Platform.select({ ios: 'System', default: type.bold }),
      fontWeight: '700',
      fontSize: 12,
    },
    rowAction: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center', marginLeft: 4 },
    emptyState: { alignItems: 'center', paddingHorizontal: spacing.xl, paddingVertical: 72 },
    emptyTitle: { color: colors.text, fontFamily: type.bold, fontSize: 16, marginTop: 10 },
    emptyBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 12, textAlign: 'center', marginTop: 4 },
    fabWrapper: {
      position: 'absolute',
      right: 14,
      zIndex: 10,
    },
    fab: {
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
