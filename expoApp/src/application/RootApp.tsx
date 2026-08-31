import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { MAX_BACKUP_BYTES } from '../data/backup';
import { InventoryRepository } from '../data/inventoryRepository';
import { colors, radius, spacing, type } from '../theme/tokens';

interface RootAppProps {
  fontFallback: boolean;
}

type LoadState =
  | { phase: 'loading' }
  | { phase: 'ready'; repository: InventoryRepository; productCount: number }
  | { phase: 'error'; message: string };

export function RootApp({ fontFallback }: RootAppProps) {
  const [state, setState] = useState<LoadState>({ phase: 'loading' });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    InventoryRepository.create()
      .then(async (repository) => {
        const productCount = await repository.countProducts();
        if (active) setState({ phase: 'ready', repository, productCount });
      })
      .catch((error: unknown) => {
        if (active) setState({ phase: 'error', message: messageFrom(error) });
      });
    return () => {
      active = false;
    };
  }, []);

  if (state.phase === 'loading') {
    return <CenteredStatus label="Preparing secure local storage…" />;
  }
  if (state.phase === 'error') {
    return <CenteredStatus label={state.message} error />;
  }

  const refreshCount = async () => {
    const productCount = await state.repository.countProducts();
    setState({ ...state, productCount });
  };

  const importBackup = async () => {
    const selection = await DocumentPicker.getDocumentAsync({
      type: ['application/json', 'text/json', 'text/plain'],
      copyToCacheDirectory: true,
      multiple: false,
    });
    if (selection.canceled) return;
    const asset = selection.assets[0];
    if (asset.size != null && asset.size > MAX_BACKUP_BYTES) {
      Alert.alert('Backup not imported', 'This backup is too large to import safely.');
      return;
    }

    setBusy(true);
    try {
      const contents = await new File(asset.uri).text();
      const result = await state.repository.importBackupJson(contents);
      await refreshCount();
      Alert.alert(
        'Backup import complete',
        `${result.addedCount} added · ${result.duplicateCount} already present · ${result.invalidCount} invalid`,
      );
    } catch (error) {
      Alert.alert('Backup not imported', messageFrom(error));
    } finally {
      setBusy(false);
    }
  };

  const exportBackup = async () => {
    setBusy(true);
    try {
      const contents = await state.repository.createBackupJson();
      const file = new File(Paths.cache, `price-intelligence-backup-${Date.now()}.json`);
      file.create({ overwrite: true, intermediates: true });
      file.write(contents);
      if (!(await Sharing.isAvailableAsync())) {
        throw new Error('Sharing is not available on this device.');
      }
      await Sharing.shareAsync(file.uri, {
        dialogTitle: 'Save Price Intelligence backup',
        mimeType: 'application/json',
        UTI: 'public.json',
      });
    } catch (error) {
      Alert.alert('Backup not exported', messageFrom(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.brandRow}>
          <Image
            source={require('../../assets/brand/app_logo.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Supreme Price Intelligence"
          />
          <View style={styles.brandText}>
            <Text style={styles.brandTitle}>SUPREME</Text>
            <Text style={styles.brandSubtitle}>PRICE INTELLIGENCE V2</Text>
          </View>
        </View>

        <View style={styles.heroCard}>
          <View style={styles.statusPill}>
            <View style={styles.statusDot} />
            <Text style={styles.statusText}>LOCAL DATABASE READY</Text>
          </View>
          <Text style={styles.heroTitle}>A safer foundation for daily work</Text>
          <Text style={styles.heroBody}>
            The new app is isolated from the current installation. Its first contract is preserving
            inventory and price history through the existing backup format.
          </Text>
          <View style={styles.countRow}>
            <Text style={styles.countNumber}>{state.productCount}</Text>
            <Text style={styles.countLabel}>products in the v2 database</Text>
          </View>
        </View>

        <Text style={styles.sectionTitle}>SAFE MIGRATION</Text>
        <ActionButton
          title="Import current app backup"
          subtitle="Merge valid products; keep duplicates and invalid rows out"
          onPress={importBackup}
          disabled={busy}
          primary
        />
        <ActionButton
          title="Export v2 backup"
          subtitle="Verify that migrated inventory remains portable"
          onPress={exportBackup}
          disabled={busy}
        />

        <View style={styles.contractCard}>
          <Text style={styles.contractTitle}>Foundation guarantees</Text>
          <ContractRow text="Separate Android and iPhone app identity during testing" />
          <ContractRow text="SQLite storage with foreign keys, WAL, and explicit migrations" />
          <ContractRow text="Backup versions 1 and 2 accepted without opening the Room database" />
          <ContractRow text="Newest 60 observations retained per retailer" />
          <ContractRow text="Supreme assets, typography, and dark palette reused exactly" />
        </View>

        {fontFallback ? (
          <Text style={styles.warning}>Bundled typography could not load; system text is being used.</Text>
        ) : null}
      </ScrollView>
      {busy ? (
        <View style={styles.busyOverlay} accessibilityLiveRegion="polite">
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.busyText}>Working safely…</Text>
        </View>
      ) : null}
    </SafeAreaView>
  );
}

function ActionButton({
  title,
  subtitle,
  onPress,
  disabled,
  primary = false,
}: {
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
      style={({ pressed }) => [
        styles.actionButton,
        primary && styles.actionButtonPrimary,
        pressed && styles.pressed,
        disabled && styles.disabled,
      ]}
    >
      <Text style={styles.actionTitle}>{title}</Text>
      <Text style={styles.actionSubtitle}>{subtitle}</Text>
    </Pressable>
  );
}

function ContractRow({ text }: { text: string }) {
  return (
    <View style={styles.contractRow}>
      <Text style={styles.check}>✓</Text>
      <Text style={styles.contractText}>{text}</Text>
    </View>
  );
}

function CenteredStatus({ label, error = false }: { label: string; error?: boolean }) {
  return (
    <SafeAreaView style={styles.centered}>
      {!error ? <ActivityIndicator color={colors.primary} size="large" /> : null}
      <Text style={[styles.centeredLabel, error && styles.error]}>{label}</Text>
    </SafeAreaView>
  );
}

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'Something unexpected happened.';
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.xl, paddingBottom: 48, gap: spacing.lg },
  brandRow: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.sm },
  logo: { width: 56, height: 56 },
  brandText: { marginLeft: spacing.md },
  brandTitle: { color: colors.text, fontFamily: type.bold, fontSize: 24, letterSpacing: 1.3 },
  brandSubtitle: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 12, letterSpacing: 1.2 },
  heroCard: { backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, borderRadius: radius.lg, padding: spacing.xl },
  statusPill: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', backgroundColor: colors.primaryMuted, borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  statusDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.primary, marginRight: spacing.sm },
  statusText: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 0.8 },
  heroTitle: { color: colors.text, fontFamily: type.bold, fontSize: 27, lineHeight: 32, marginTop: spacing.lg },
  heroBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 16, lineHeight: 23, marginTop: spacing.md },
  countRow: { flexDirection: 'row', alignItems: 'baseline', marginTop: spacing.xl },
  countNumber: { color: colors.primary, fontFamily: type.bold, fontSize: 38 },
  countLabel: { color: colors.text, fontFamily: type.semibold, fontSize: 15, marginLeft: spacing.md },
  sectionTitle: { color: colors.textMuted, fontFamily: type.bold, fontSize: 12, letterSpacing: 1.4, marginTop: spacing.sm },
  actionButton: { minHeight: 76, justifyContent: 'center', backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, borderRadius: radius.md, paddingHorizontal: spacing.lg, paddingVertical: spacing.md },
  actionButtonPrimary: { borderColor: colors.primary, backgroundColor: colors.primaryMuted },
  actionTitle: { color: colors.text, fontFamily: type.bold, fontSize: 17 },
  actionSubtitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13, lineHeight: 18, marginTop: spacing.xs },
  pressed: { opacity: 0.75, transform: [{ scale: 0.99 }] },
  disabled: { opacity: 0.55 },
  contractCard: { backgroundColor: colors.surfaceRaised, borderColor: colors.border, borderWidth: 1, borderRadius: radius.md, padding: spacing.lg, marginTop: spacing.sm },
  contractTitle: { color: colors.text, fontFamily: type.bold, fontSize: 18, marginBottom: spacing.md },
  contractRow: { flexDirection: 'row', alignItems: 'flex-start', marginVertical: spacing.sm },
  check: { color: colors.primary, fontFamily: type.bold, fontSize: 17, width: 28 },
  contractText: { flex: 1, color: colors.textMuted, fontFamily: type.regular, fontSize: 14, lineHeight: 20 },
  warning: { color: colors.warning, fontFamily: type.regular, fontSize: 13 },
  busyOverlay: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.scrim },
  busyText: { color: colors.text, fontFamily: type.semibold, fontSize: 16, marginTop: spacing.md },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background, padding: spacing.xl },
  centeredLabel: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 16, textAlign: 'center', marginTop: spacing.lg },
  error: { color: colors.danger },
});
