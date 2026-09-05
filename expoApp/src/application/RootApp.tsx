import Ionicons from '@expo/vector-icons/Ionicons';
import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  AppState,
  BackHandler,
  Easing,
  Modal,
  PanResponder,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import {
  BottomBanner,
  type BannerNotice,
  type BannerTone,
  type UndoNotice,
} from '../components/BottomBanner';
import { ErrorBoundary } from '../components/ErrorBoundary';
import { MAX_BACKUP_BYTES } from '../data/backup';
import { InventoryRepository, type ComparisonOverview } from '../data/inventoryRepository';
import type { InventoryProduct } from '../domain/models';
import { priceChangeNotificationNavigation } from '../domain/priceChangeNotifications';
import { ProductDetailsModal } from '../features/comparison/ProductDetailsModal';
import { QuickCompareScreen } from '../features/comparison/QuickCompareScreen';
import { DashboardScreen } from '../features/dashboard/DashboardScreen';
import { PriceMovementScreen } from '../features/dashboard/PriceMovementScreen';
import { PricingInsightsScreen } from '../features/insights/PricingInsightsScreen';
import { InventoryScreen } from '../features/inventory/InventoryScreen';
import { PersonalizationAccordionModal } from '../features/settings/PersonalizationAccordionModal';
import { CustomizationProvider, useCustomization } from '../theme/CustomizationContext';
import { colors, radius, spacing, type } from '../theme/tokens';

interface RootAppProps {
  fontFallback: boolean;
}

type LoadState =
  | { phase: 'loading' }
  | { phase: 'ready'; repository: InventoryRepository; overview: ComparisonOverview }
  | { phase: 'error'; message: string };

type Destination = 'inventory' | 'comparison' | 'priceMovement' | 'insights';

export function RootApp(props: RootAppProps) {
  return (
    <ErrorBoundary>
      <CustomizationProvider>
        <RootAppContent {...props} />
      </CustomizationProvider>
    </ErrorBoundary>
  );
}

function RootAppContent({ fontFallback }: RootAppProps) {
  const { colors: dynamicColors, customization, priceChangeNotificationsEnabled } = useCustomization();
  const customizationRef = useRef(customization);
  customizationRef.current = customization;
  const notifEnabledRef = useRef(priceChangeNotificationsEnabled);
  notifEnabledRef.current = priceChangeNotificationsEnabled;

  const [state, setState] = useState<LoadState>({ phase: 'loading' });

  // ── Navigation Shell State matching Compose App.kt ──
  const [hubVisible, setHubVisible] = useState(true);
  const [destination, setDestination] = useState<Destination | null>(null);
  const [insightsFilter, setInsightsFilter] = useState<'COMPETITIVE' | 'REVIEW' | null>(null);
  const [selectedPriorityProduct, setSelectedPriorityProduct] = useState<InventoryProduct | null>(null);

  const [toolsVisible, setToolsVisible] = useState(false);
  const [personalizationVisible, setPersonalizationVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [quickCompareKey, setQuickCompareKey] = useState(0);

  // ── Coordinated Dual Banners ──
  const [notice, setNotice] = useState<BannerNotice | null>(null);
  const [undoNotice, setUndoNotice] = useState<UndoNotice | null>(null);
  const noticeCounter = useRef(0);
  const noticeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const repositoryRef = useRef<InventoryRepository | null>(null);

  // ── Spring Transition Animated Values ──
  const hubAnim = useRef(new Animated.Value(1)).current;
  const destAnim = useRef(new Animated.Value(0)).current;
  const reduceMotion = customization.motionPreference === 'REDUCED';

  const showBanner = useCallback((
    message: string,
    tone: BannerTone = 'info',
    action?: { label: string; onPress: () => void },
  ) => {
    if (noticeTimer.current) clearTimeout(noticeTimer.current);
    const nextNotice: BannerNotice = {
      id: ++noticeCounter.current,
      message,
      tone,
      actionLabel: action?.label,
      onAction: action?.onPress,
    };
    setNotice(nextNotice);
    noticeTimer.current = setTimeout(() => {
      setNotice((current) => (current?.id === nextNotice.id ? null : current));
    }, action ? 5000 : 4200);
  }, []);

  const refreshOverview = useCallback(async (repository: InventoryRepository) => {
    const overview = await repository.getComparisonOverview();
    setState((current) => {
      if (current.phase !== 'ready' || current.repository !== repository) return current;
      return { ...current, overview };
    });
  }, []);

  const updateProductCount = useCallback((productCount: number) => {
    setState((current) => {
      if (current.phase !== 'ready' || current.overview.productCount === productCount) return current;
      const categorized = Math.min(
        productCount,
        current.overview.competitiveCount + current.overview.reviewCount,
      );
      return {
        ...current,
        overview: {
          ...current.overview,
          productCount,
          uncheckedCount: Math.max(0, productCount - categorized),
        },
      };
    });
    const repository = repositoryRef.current;
    if (repository) refreshOverview(repository).catch(() => undefined);
  }, [refreshOverview]);

  useEffect(() => {
    let active = true;
    InventoryRepository.create()
      .then(async (repository) => {
        const overview = await repository.getComparisonOverview();
        if (active) {
          repositoryRef.current = repository;
          if (Platform.OS === 'web') {
            (window as any).__expoRepository = repository;
            (window as any).__openTools = () => setToolsVisible(true);
            (window as any).__closeTools = () => setToolsVisible(false);
            (window as any).__openPersonalization = () => setPersonalizationVisible(true);
            (window as any).__closePersonalization = () => setPersonalizationVisible(false);
          }
          setState({ phase: 'ready', repository, overview });
        }
      })
      .catch((error: unknown) => {
        if (active) setState({ phase: 'error', message: messageFrom(error) });
      });
    return () => {
      active = false;
      repositoryRef.current = null;
      if (noticeTimer.current) clearTimeout(noticeTimer.current);
    };
  }, []);

  // ── Spring Navigation Transitions ──
  const navigateTo = useCallback((dest: Destination, filter?: 'COMPETITIVE' | 'REVIEW') => {
    setDestination(dest);
    setInsightsFilter(filter || null);
    if (dest === 'comparison') {
      setQuickCompareKey((k) => k + 1);
    }
    if (reduceMotion) {
      hubAnim.setValue(0);
      destAnim.setValue(1);
      setHubVisible(false);
    } else {
      setHubVisible(false);
      Animated.parallel([
        Animated.timing(hubAnim, {
          toValue: 0,
          duration: 150,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }),
        Animated.spring(destAnim, {
          toValue: 1,
          damping: 15,
          stiffness: 220,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [destAnim, hubAnim, reduceMotion]);

  useEffect(() => {
    const unsubscribe = priceChangeNotificationNavigation.subscribe((target) => {
      if (target) {
        navigateTo('priceMovement');
      }
    });
    return unsubscribe;
  }, [navigateTo]);


  const navigateHome = useCallback(() => {
    if (reduceMotion) {
      hubAnim.setValue(1);
      destAnim.setValue(0);
      setHubVisible(true);
    } else {
      setHubVisible(true);
      Animated.parallel([
        Animated.spring(hubAnim, {
          toValue: 1,
          damping: 16,
          stiffness: 240,
          useNativeDriver: true,
        }),
        Animated.timing(destAnim, {
          toValue: 0,
          duration: 140,
          easing: Easing.in(Easing.cubic),
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [destAnim, hubAnim, reduceMotion]);

  useEffect(() => {
    if (Platform.OS === 'web') {
      (window as any).__navigateTo = navigateTo;
      (window as any).__navigateHome = navigateHome;
      (window as any).__setRoute = (r: string) => {
        if (r === 'home') navigateHome();
        else navigateTo(r as Destination);
      };
      (window as any).__setSelectedProduct = (p: InventoryProduct | null) => setSelectedPriorityProduct(p);
    }
  }, [navigateTo, navigateHome]);

  // ── Hardware Back Handler & Interactive Edge Swipe ──
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (selectedPriorityProduct) {
        setSelectedPriorityProduct(null);
        return true;
      }
      if (personalizationVisible) {
        setPersonalizationVisible(false);
        return true;
      }
      if (toolsVisible) {
        setToolsVisible(false);
        return true;
      }
      if (!hubVisible) {
        navigateHome();
        return true;
      }
      return false;
    });
    return () => sub.remove();
  }, [selectedPriorityProduct, personalizationVisible, toolsVisible, hubVisible, navigateHome]);

  const { width: windowWidth } = useWindowDimensions();
  const dragX = useRef(new Animated.Value(0)).current;

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_, gesture) => {
        if (hubVisible) return false;
        const startX = gesture.moveX - gesture.dx;
        return startX <= 48 && gesture.dx > 10 && Math.abs(gesture.dy) < 32;
      },
      onPanResponderMove: (_, gesture) => {
        if (gesture.dx > 0) {
          dragX.setValue(gesture.dx);
        }
      },
      onPanResponderRelease: (_, gesture) => {
        if (gesture.dx > 75 || gesture.vx > 0.4) {
          Animated.timing(dragX, {
            toValue: windowWidth,
            duration: 180,
            easing: Easing.out(Easing.cubic),
            useNativeDriver: true,
          }).start(() => {
            dragX.setValue(0);
            navigateHome();
          });
        } else {
          Animated.spring(dragX, {
            toValue: 0,
            damping: 22,
            stiffness: 260,
            useNativeDriver: true,
          }).start();
        }
      },
      onPanResponderTerminate: () => {
        Animated.spring(dragX, {
          toValue: 0,
          damping: 22,
          stiffness: 260,
          useNativeDriver: true,
        }).start();
      },
    }),
  ).current;

  // ── Price Alert Navigation ──
  useEffect(() => {
    if (state.phase !== 'ready') return;

    const unsubNav = priceChangeNotificationNavigation.subscribe((target) => {
      if (target) {
        navigateTo('priceMovement');
        priceChangeNotificationNavigation.consume(target.requestId);
      }
    });

    return () => {
      unsubNav();
    };
  }, [state.phase, navigateTo]);

  if (state.phase === 'loading') return <CenteredStatus label="Preparing secure local storage…" />;
  if (state.phase === 'error') return <CenteredStatus label={state.message} error />;

  const importBackup = async () => {
    let contents = '';
    if (Platform.OS === 'web') {
      const selection = await DocumentPicker.getDocumentAsync({
        type: ['application/json', 'text/json', 'text/plain'],
        multiple: false,
      });
      if (selection.canceled || !selection.assets[0].file) return;
      const domFile = selection.assets[0].file as any;
      if (domFile.size > MAX_BACKUP_BYTES) {
        showBanner('This backup is too large to import safely.', 'error');
        return;
      }
      setToolsVisible(false);
      setBusy(true);
      try {
        contents = await domFile.text();
      } catch (error) {
        setBusy(false);
        showBanner('Failed to read file on web.', 'error');
        return;
      }
    } else {
      let backupFile: File;
      if (Platform.OS === 'android') {
        const selection = await File.pickFileAsync({
          mimeTypes: ['application/json', 'text/json', 'text/plain'],
          multipleFiles: false,
        });
        if (selection.canceled) return;
        backupFile = selection.result;
      } else {
        const selection = await DocumentPicker.getDocumentAsync({
          type: ['application/json', 'text/json', 'text/plain'],
          copyToCacheDirectory: true,
          multiple: false,
        });
        if (selection.canceled) return;
        backupFile = new File(selection.assets[0].uri);
      }
      setToolsVisible(false);
      if (backupFile.size != null && backupFile.size > MAX_BACKUP_BYTES) {
        showBanner('This backup is too large to import safely.', 'error');
        return;
      }

      setBusy(true);
      try {
        contents = await backupFile.text();
      } catch (error) {
        setBusy(false);
        showBanner('Failed to read backup file.', 'error');
        return;
      }
    }

    try {
      const result = await state.repository.importBackupJson(contents);
      const productCount = await state.repository.countProducts();
      updateProductCount(productCount);
      showBanner(
        `${result.addedCount} added · ${result.duplicateCount} already present · ${result.invalidCount} invalid`,
        result.invalidCount > 0 ? 'info' : 'success',
      );
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  const exportBackup = async () => {
    setToolsVisible(false);
    setBusy(true);
    try {
      const contents = await state.repository.createBackupJson();
      const file = new File(Paths.cache, `price-intelligence-backup-${Date.now()}.json`);
      file.create({ overwrite: true, intermediates: true });
      file.write(contents);
      if (!(await Sharing.isAvailableAsync())) throw new Error('Sharing is not available on this device.');
      await Sharing.shareAsync(file.uri, {
        dialogTitle: 'Save Price Intelligence backup',
        mimeType: 'application/json',
        UTI: 'public.json',
      });
      showBanner('Backup prepared successfully.', 'success');
    } catch (error) {
      showBanner(messageFrom(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: dynamicColors.background }]} edges={['top', 'left', 'right']}>
      <View style={styles.contentShell} {...panResponder.panHandlers}>
        {/* ── Launch Hub Layer ── */}
        <Animated.View
          pointerEvents={hubVisible ? 'auto' : 'none'}
          style={[
            StyleSheet.absoluteFill,
            {
              opacity: hubAnim,
              transform: [
                {
                  scale: hubVisible
                    ? hubAnim.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0.97, 1],
                      })
                    : dragX.interpolate({
                        inputRange: [0, windowWidth],
                        outputRange: [0.96, 1],
                        extrapolate: 'clamp',
                      }),
                },
                {
                  translateY: hubAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [-20, 0],
                  }),
                },
              ],
            },
          ]}
        >
          <DashboardScreen
            overview={state.overview}
            repository={state.repository}
            onOpenInventory={() => navigateTo('inventory')}
            onOpenCompare={() => navigateTo('comparison')}
            onOpenTools={() => setToolsVisible(true)}
            onOpenPriceMovement={() => navigateTo('priceMovement')}
            onOpenInsights={(filter) => navigateTo('insights', filter)}
            onSelectProduct={(product) => setSelectedPriorityProduct(product)}
          />
        </Animated.View>

        {/* ── Swipe-Back Backdrop Dimming ── */}
        {!hubVisible ? (
          <Animated.View
            pointerEvents="none"
            style={[
              StyleSheet.absoluteFill,
              {
                backgroundColor: '#000000',
                opacity: dragX.interpolate({
                  inputRange: [0, windowWidth],
                  outputRange: [0.22, 0],
                  extrapolate: 'clamp',
                }),
              },
            ]}
          />
        ) : null}

        {/* ── Destination Layer with Spring Enter/Exit & Swipe-to-Go-Back ── */}
        <Animated.View
          {...panResponder.panHandlers}
          pointerEvents={!hubVisible ? 'auto' : 'none'}
          style={[
            StyleSheet.absoluteFill,
            {
              opacity: destAnim,
              transform: [
                {
                  scale: destAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [0.93, 1],
                  }),
                },
                {
                  translateY: destAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [30, 0],
                  }),
                },
                {
                  translateX: dragX,
                },
              ],
            },
          ]}
        >
          {destination === 'insights' ? (
            <PricingInsightsScreen
              onBack={navigateHome}
              repository={state.repository}
              showBanner={showBanner}
              initialGroup={
                insightsFilter === 'COMPETITIVE'
                  ? 'SHOP_LOWER'
                  : insightsFilter === 'REVIEW'
                  ? 'ONLINE_LOWER'
                  : null
              }
            />
          ) : destination === 'priceMovement' ? (
            <PriceMovementScreen repository={state.repository} onClose={navigateHome} />
          ) : destination === 'inventory' ? (
            <InventoryScreen
              repository={state.repository}
              productCount={state.overview.productCount}
              onBack={navigateHome}
              onProductCountChanged={updateProductCount}
              showBanner={showBanner}
              bannerVisible={notice != null || undoNotice != null}
              onQueueUndo={(n) =>
                setUndoNotice({
                  id: Date.now(),
                  itemCount: n.itemCount,
                  onUndo: () => {
                    n.onUndo();
                    setUndoNotice(null);
                  },
                })
              }
            />
          ) : destination === 'comparison' ? (
            <QuickCompareScreen
              key={`quick-compare-${quickCompareKey}`}
              repository={state.repository}
              onBack={navigateHome}
              onComparisonChanged={() => refreshOverview(state.repository).catch(() => undefined)}
              showBanner={showBanner}
            />
          ) : null}
        </Animated.View>
      </View>

      {/* ── Priority Product Direct Details Sheet ── */}
      {selectedPriorityProduct ? (
        <ProductDetailsModal
          product={selectedPriorityProduct}
          repository={state.repository}
          showBanner={showBanner}
          onClose={() => setSelectedPriorityProduct(null)}
          onProductUpdated={(updated) => {
            refreshOverview(state.repository).catch(() => undefined);
          }}
        />
      ) : null}

      {/* ── App Tools & Personalization Modals ── */}
      <MigrationToolsModal
        visible={toolsVisible}
        productCount={state.overview.productCount}
        busy={busy}
        fontFallback={fontFallback}
        onClose={() => setToolsVisible(false)}
        onImport={importBackup}
        onExport={exportBackup}
        onPersonalize={() => {
          setToolsVisible(false);
          setPersonalizationVisible(true);
        }}
      />
      <PersonalizationAccordionModal
        visible={personalizationVisible}
        onClose={() => setPersonalizationVisible(false)}
      />

      {/* ── Coordinated Dual Banners (Undo + Status) ── */}
      <BottomBanner
        notice={notice}
        onDismiss={() => setNotice(null)}
        undoNotice={undoNotice}
        onUndo={() => setUndoNotice(null)}
        bottomOffset={0}
      />

      {busy ? (
        <View style={styles.busyOverlay} accessibilityLiveRegion="polite">
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.busyText}>Working safely…</Text>
        </View>
      ) : null}
    </SafeAreaView>
  );
}

function MigrationToolsModal({
  visible,
  productCount,
  busy,
  fontFallback,
  onClose,
  onImport,
  onExport,
  onPersonalize,
}: {
  visible: boolean;
  productCount: number;
  busy: boolean;
  fontFallback: boolean;
  onClose: () => void;
  onImport: () => void;
  onExport: () => void;
  onPersonalize: () => void;
}) {
  const { colors } = useCustomization();

  return (
    <Modal
      visible={visible}
      transparent={Platform.OS !== 'ios'}
      presentationStyle={Platform.OS === 'ios' ? 'pageSheet' : 'overFullScreen'}
      animationType="slide"
      onRequestClose={onClose}
    >
      <SafeAreaView style={[styles.toolsRoot, { backgroundColor: colors.surface }, Platform.OS === 'android' && styles.toolsBackdrop]}>
        <View style={[styles.toolsSheet, { backgroundColor: colors.surface }]}>
          <View style={[styles.toolsHeader, { borderBottomColor: colors.border }]}>
            <View>
              <Text style={[styles.toolsEyebrow, { color: colors.primary }]}>DATA SAFETY</Text>
              <Text style={[styles.toolsTitle, { color: colors.text }]}>App tools</Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close tools"
              onPress={onClose}
              style={[styles.closeButton, { borderColor: colors.border }]}
            >
              <Ionicons name="close" size={26} color={colors.text} />
            </Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.toolsContent}>
            <ToolButton
              icon="color-palette-outline"
              title="Settings & Personalization"
              subtitle="Change colours, layouts, graphs, alerts and motion"
              onPress={onPersonalize}
              disabled={busy}
              primary
            />
            <ToolButton
              icon="download-outline"
              title="Import current app backup"
              subtitle="Safely merge products and price history"
              onPress={onImport}
              disabled={busy}
            />
            <ToolButton
              icon="share-outline"
              title="Export V2 backup"
              subtitle="Save or share a portable JSON backup"
              onPress={onExport}
              disabled={busy}
            />
            <View style={[styles.databaseCard, { backgroundColor: colors.primaryMuted, borderColor: colors.primary }]}>
              <Ionicons name="shield-checkmark" size={25} color={colors.primary} />
              <View style={styles.databaseText}>
                <Text style={[styles.databaseTitle, { color: colors.text }]}>
                  {productCount} {productCount === 1 ? 'product' : 'products'} in V2
                </Text>
                <Text style={[styles.databaseBody, { color: colors.textMuted }]}>Stored locally in the isolated SQLite database.</Text>
              </View>
            </View>
            <View style={[styles.safetyNote, { backgroundColor: colors.surfaceRaised }]}>
              <Text style={[styles.safetyTitle, { color: colors.text }]}>The current app stays protected</Text>
              <Text style={[styles.safetyBody, { color: colors.textMuted }]}>
                V2 never opens or modifies the Room inventory database. Migration only reads a backup file you choose.
              </Text>
            </View>
            {fontFallback ? (
              <Text style={[styles.fontWarning, { color: colors.warning }]}>
                Bundled typography could not load; system text is being used.
              </Text>
            ) : null}
          </ScrollView>
        </View>
      </SafeAreaView>
    </Modal>
  );
}

function ToolButton({
  icon,
  title,
  subtitle,
  onPress,
  disabled,
  primary = false,
}: {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  title: string;
  subtitle: string;
  onPress: () => void;
  disabled: boolean;
  primary?: boolean;
}) {
  const { colors } = useCustomization();

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.toolButton,
        { backgroundColor: colors.background, borderColor: primary ? colors.primary : colors.border },
        pressed && styles.pressed,
        disabled && styles.disabled,
      ]}
    >
      <View style={[styles.toolIcon, { backgroundColor: colors.surfaceRaised }]}>
        <Ionicons name={icon} size={23} color={primary ? colors.primary : colors.textMuted} />
      </View>
      <View style={styles.toolText}>
        <Text style={[styles.toolTitle, { color: colors.text }]}>{title}</Text>
        <Text style={[styles.toolSubtitle, { color: colors.textMuted }]}>{subtitle}</Text>
      </View>
      <Ionicons name="chevron-forward" size={20} color={colors.textMuted} />
    </Pressable>
  );
}

function CenteredStatus({ label, error = false }: { label: string; error?: boolean }) {
  const { colors } = useCustomization();

  return (
    <SafeAreaView style={[styles.centered, { backgroundColor: colors.background }]}>
      {!error ? (
        <ActivityIndicator color={colors.primary} size="large" />
      ) : (
        <Ionicons name="alert-circle" size={38} color={colors.danger} />
      )}
      <Text style={[styles.centeredLabel, { color: colors.textMuted }, error && styles.error]}>{label}</Text>
    </SafeAreaView>
  );
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.background },
  contentShell: { flex: 1, overflow: 'hidden' },
  busyOverlay: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.62)',
    zIndex: 200,
  },
  busyText: { color: colors.text, fontFamily: type.semibold, fontSize: 16, marginTop: spacing.md },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
    padding: spacing.xl,
  },
  centeredLabel: {
    color: colors.textMuted,
    fontFamily: type.semibold,
    fontSize: 16,
    textAlign: 'center',
    marginTop: spacing.lg,
  },
  error: { color: colors.danger },
  toolsRoot: { flex: 1, backgroundColor: colors.surface },
  toolsBackdrop: { backgroundColor: 'rgba(0,0,0,0.72)', justifyContent: 'flex-end' },
  toolsSheet: {
    flex: Platform.OS === 'ios' ? 1 : undefined,
    maxHeight: Platform.OS === 'ios' ? '100%' : '90%',
    backgroundColor: colors.surface,
    borderTopLeftRadius: Platform.OS === 'ios' ? 0 : radius.lg,
    borderTopRightRadius: Platform.OS === 'ios' ? 0 : radius.lg,
    overflow: 'hidden',
  },
  toolsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.xl,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  toolsEyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  toolsTitle: { color: colors.text, fontFamily: type.bold, fontSize: 24, marginTop: spacing.xs },
  closeButton: {
    width: 46,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  toolsContent: { padding: spacing.xl, gap: spacing.lg, paddingBottom: 48 },
  databaseCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primaryMuted,
    borderWidth: 1,
    borderColor: 'rgba(16,185,129,0.45)',
    borderRadius: radius.md,
    padding: spacing.lg,
  },
  databaseText: { flex: 1, marginLeft: spacing.md },
  databaseTitle: { color: colors.text, fontFamily: type.bold, fontSize: 17 },
  databaseBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, marginTop: spacing.xs },
  toolButton: {
    minHeight: 76,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  toolButtonPrimary: { borderColor: colors.primary },
  toolIcon: {
    width: 46,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surfaceRaised,
    borderRadius: radius.sm,
  },
  toolText: { flex: 1, marginHorizontal: spacing.md },
  toolTitle: { color: colors.text, fontFamily: type.bold, fontSize: 16 },
  toolSubtitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, marginTop: spacing.xs },
  safetyNote: { backgroundColor: colors.surfaceRaised, borderRadius: radius.md, padding: spacing.lg },
  safetyTitle: { color: colors.text, fontFamily: type.bold, fontSize: 15 },
  safetyBody: {
    color: colors.textMuted,
    fontFamily: type.regular,
    fontSize: 13,
    lineHeight: 19,
    marginTop: spacing.sm,
  },
  fontWarning: { color: colors.warning, fontFamily: type.regular, fontSize: 13 },
  pressed: { opacity: 0.68 },
  disabled: { opacity: 0.5 },
});
