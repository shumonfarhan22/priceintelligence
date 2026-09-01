import Ionicons from '@expo/vector-icons/Ionicons';
import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import { readAsStringAsync } from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BottomBanner, type BannerNotice, type BannerTone } from '../components/BottomBanner';
import { MAX_BACKUP_BYTES } from '../data/backup';
import { InventoryRepository, type ComparisonOverview } from '../data/inventoryRepository';
import { QuickCompareScreen } from '../features/comparison/QuickCompareScreen';
import { DashboardScreen } from '../features/dashboard/DashboardScreen';
import { InventoryScreen } from '../features/inventory/InventoryScreen';
import { colors, radius, spacing, type } from '../theme/tokens';

interface RootAppProps {
  fontFallback: boolean;
}

type LoadState =
  | { phase: 'loading' }
  | { phase: 'ready'; repository: InventoryRepository; overview: ComparisonOverview }
  | { phase: 'error'; message: string };

type Route = 'dashboard' | 'inventory' | 'comparison';

export function RootApp({ fontFallback }: RootAppProps) {
  const [state, setState] = useState<LoadState>({ phase: 'loading' });
  const [route, setRoute] = useState<Route>('dashboard');
  const [toolsVisible, setToolsVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<BannerNotice | null>(null);
  const noticeCounter = useRef(0);
  const noticeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const repositoryRef = useRef<InventoryRepository | null>(null);

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
      setNotice((current) => current?.id === nextNotice.id ? null : current);
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

  if (state.phase === 'loading') return <CenteredStatus label="Preparing secure local storage…" />;
  if (state.phase === 'error') return <CenteredStatus label={state.message} error />;

  const importBackup = async () => {
    const selection = await DocumentPicker.getDocumentAsync({
      type: ['application/json', 'text/json', 'text/plain'],
      copyToCacheDirectory: true,
      multiple: false,
    });
    if (selection.canceled) return;
    const asset = selection.assets[0];
    setToolsVisible(false);
    if (asset.size != null && asset.size > MAX_BACKUP_BYTES) {
      showBanner('This backup is too large to import safely.', 'error');
      return;
    }

    setBusy(true);
    try {
      // DocumentPicker copies Android documents into our cache, but the SDK 57
      // File object can still reject that URI during its newer permission check.
      // The legacy reader is the supported SAF/file-URI bridge and works for the
      // picker result on Android as well as the temporary copy returned on iOS.
      const contents = await readAsStringAsync(asset.uri);
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
    <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
      {route === 'dashboard' ? (
        <DashboardScreen
          overview={state.overview}
          onOpenInventory={() => setRoute('inventory')}
          onOpenCompare={() => setRoute('comparison')}
          onOpenTools={() => setToolsVisible(true)}
        />
      ) : route === 'inventory' ? (
        <InventoryScreen
          repository={state.repository}
          productCount={state.overview.productCount}
          onBack={() => setRoute('dashboard')}
          onProductCountChanged={updateProductCount}
          showBanner={showBanner}
        />
      ) : (
        <QuickCompareScreen
          repository={state.repository}
          onBack={() => setRoute('dashboard')}
          onComparisonChanged={() => refreshOverview(state.repository).catch(() => undefined)}
          showBanner={showBanner}
        />
      )}

      <MigrationToolsModal
        visible={toolsVisible}
        productCount={state.overview.productCount}
        busy={busy}
        fontFallback={fontFallback}
        onClose={() => setToolsVisible(false)}
        onImport={importBackup}
        onExport={exportBackup}
      />
      <BottomBanner
        notice={notice}
        onDismiss={() => setNotice(null)}
        bottomOffset={route === 'inventory' ? 94 : 0}
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
}: {
  visible: boolean;
  productCount: number;
  busy: boolean;
  fontFallback: boolean;
  onClose: () => void;
  onImport: () => void;
  onExport: () => void;
}) {
  return (
    <Modal
      visible={visible}
      transparent={Platform.OS !== 'ios'}
      presentationStyle={Platform.OS === 'ios' ? 'pageSheet' : 'overFullScreen'}
      animationType="slide"
      onRequestClose={onClose}
    >
      <SafeAreaView style={[styles.toolsRoot, Platform.OS === 'android' && styles.toolsBackdrop]}>
        <View style={styles.toolsSheet}>
          <View style={styles.toolsHeader}>
            <View>
              <Text style={styles.toolsEyebrow}>DATA SAFETY</Text>
              <Text style={styles.toolsTitle}>Backup & migration</Text>
            </View>
            <Pressable accessibilityRole="button" accessibilityLabel="Close tools" onPress={onClose} style={styles.closeButton}>
              <Ionicons name="close" size={26} color={colors.text} />
            </Pressable>
          </View>
          <ScrollView contentContainerStyle={styles.toolsContent}>
            <View style={styles.databaseCard}>
              <Ionicons name="shield-checkmark" size={25} color={colors.primary} />
              <View style={styles.databaseText}>
                <Text style={styles.databaseTitle}>
                  {productCount} {productCount === 1 ? 'product' : 'products'} in V2
                </Text>
                <Text style={styles.databaseBody}>Stored locally in the isolated SQLite database.</Text>
              </View>
            </View>
            <ToolButton
              icon="download-outline"
              title="Import current app backup"
              subtitle="Safely merge products and price history"
              onPress={onImport}
              disabled={busy}
              primary
            />
            <ToolButton
              icon="share-outline"
              title="Export V2 backup"
              subtitle="Save or share a portable JSON backup"
              onPress={onExport}
              disabled={busy}
            />
            <View style={styles.safetyNote}>
              <Text style={styles.safetyTitle}>The current app stays protected</Text>
              <Text style={styles.safetyBody}>
                V2 never opens or modifies the Room inventory database. Migration only reads a backup file you choose.
              </Text>
            </View>
            {fontFallback ? <Text style={styles.fontWarning}>Bundled typography could not load; system text is being used.</Text> : null}
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
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [styles.toolButton, primary && styles.toolButtonPrimary, pressed && styles.pressed, disabled && styles.disabled]}
    >
      <View style={styles.toolIcon}>
        <Ionicons name={icon} size={23} color={primary ? colors.primary : colors.textMuted} />
      </View>
      <View style={styles.toolText}>
        <Text style={styles.toolTitle}>{title}</Text>
        <Text style={styles.toolSubtitle}>{subtitle}</Text>
      </View>
      <Ionicons name="chevron-forward" size={20} color={colors.textMuted} />
    </Pressable>
  );
}

function CenteredStatus({ label, error = false }: { label: string; error?: boolean }) {
  return (
    <SafeAreaView style={styles.centered}>
      {!error ? <ActivityIndicator color={colors.primary} size="large" /> : <Ionicons name="alert-circle" size={38} color={colors.danger} />}
      <Text style={[styles.centeredLabel, error && styles.error]}>{label}</Text>
    </SafeAreaView>
  );
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.background },
  busyOverlay: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.62)', zIndex: 200 },
  busyText: { color: colors.text, fontFamily: type.semibold, fontSize: 16, marginTop: spacing.md },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background, padding: spacing.xl },
  centeredLabel: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 16, textAlign: 'center', marginTop: spacing.lg },
  error: { color: colors.danger },
  toolsRoot: { flex: 1, backgroundColor: colors.surface },
  toolsBackdrop: { backgroundColor: 'rgba(0,0,0,0.72)', justifyContent: 'flex-end' },
  toolsSheet: { flex: Platform.OS === 'ios' ? 1 : undefined, maxHeight: Platform.OS === 'ios' ? '100%' : '90%', backgroundColor: colors.surface, borderTopLeftRadius: Platform.OS === 'ios' ? 0 : radius.lg, borderTopRightRadius: Platform.OS === 'ios' ? 0 : radius.lg, overflow: 'hidden' },
  toolsHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: spacing.xl, borderBottomWidth: 1, borderBottomColor: colors.border },
  toolsEyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  toolsTitle: { color: colors.text, fontFamily: type.bold, fontSize: 24, marginTop: spacing.xs },
  closeButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border },
  toolsContent: { padding: spacing.xl, gap: spacing.lg, paddingBottom: 48 },
  databaseCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.primaryMuted, borderWidth: 1, borderColor: 'rgba(16,185,129,0.45)', borderRadius: radius.md, padding: spacing.lg },
  databaseText: { flex: 1, marginLeft: spacing.md },
  databaseTitle: { color: colors.text, fontFamily: type.bold, fontSize: 17 },
  databaseBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, marginTop: spacing.xs },
  toolButton: { minHeight: 76, flexDirection: 'row', alignItems: 'center', backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, padding: spacing.md },
  toolButtonPrimary: { borderColor: colors.primary },
  toolIcon: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceRaised, borderRadius: radius.sm },
  toolText: { flex: 1, marginHorizontal: spacing.md },
  toolTitle: { color: colors.text, fontFamily: type.bold, fontSize: 16 },
  toolSubtitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, marginTop: spacing.xs },
  safetyNote: { backgroundColor: colors.surfaceRaised, borderRadius: radius.md, padding: spacing.lg },
  safetyTitle: { color: colors.text, fontFamily: type.bold, fontSize: 15 },
  safetyBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, lineHeight: 19, marginTop: spacing.sm },
  fontWarning: { color: colors.warning, fontFamily: type.regular, fontSize: 13 },
  pressed: { opacity: 0.68 },
  disabled: { opacity: 0.5 },
});
