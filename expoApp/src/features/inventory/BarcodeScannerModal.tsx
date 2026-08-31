import Ionicons from '@expo/vector-icons/Ionicons';
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import * as Haptics from 'expo-haptics';
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radius, spacing, type } from '../../theme/tokens';

const PRODUCT_BARCODE_TYPES = [
  'ean13',
  'ean8',
  'upc_a',
  'upc_e',
  'code39',
  'code93',
  'code128',
  'itf14',
  'codabar',
  'qr',
] as const;

export function BarcodeScannerModal({
  visible,
  onClose,
  onScanned,
  embedded = false,
}: {
  visible: boolean;
  onClose: () => void;
  onScanned: (value: string) => void;
  embedded?: boolean;
}) {
  const [permission, requestPermission] = useCameraPermissions();
  const [handled, setHandled] = useState(false);
  const handledRef = useRef(false);
  const permissionRequestStartedRef = useRef(false);

  useEffect(() => {
    if (visible) {
      handledRef.current = false;
      setHandled(false);
    } else {
      permissionRequestStartedRef.current = false;
    }
  }, [visible]);

  useEffect(() => {
    if (
      !visible
      || !permission
      || permission.granted
      || !permission.canAskAgain
      || permissionRequestStartedRef.current
    ) return;

    permissionRequestStartedRef.current = true;
    requestPermission().catch(() => {
      permissionRequestStartedRef.current = false;
    });
  }, [permission, requestPermission, visible]);

  const handleScan = (result: BarcodeScanningResult) => {
    const value = result.data.trim();
    if (!value || handledRef.current) return;
    handledRef.current = true;
    setHandled(true);
    onScanned(value);
    if (Platform.OS === 'ios') {
      // iOS suppresses Taptic Engine feedback while the camera is active.
      // Wait for the scanner view to unmount before confirming the scan.
      setTimeout(() => {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => undefined);
      }, 280);
    }
  };

  const scannerContent = (
    <SafeAreaView style={styles.screen}>
        <View style={styles.header}>
          <View>
            <Text style={styles.eyebrow}>BARCODE SCANNER</Text>
            <Text style={styles.title}>Place the code inside the frame</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Close scanner"
            onPress={onClose}
            style={({ pressed }) => [styles.closeButton, pressed && styles.pressed]}
          >
            <Ionicons name="close" size={26} color={colors.text} />
          </Pressable>
        </View>

        {!permission ? (
          <View style={styles.permissionState}>
            <ActivityIndicator color={colors.primary} size="large" />
          </View>
        ) : permission.granted ? (
          <View style={styles.cameraFrame}>
            <CameraView
              style={StyleSheet.absoluteFill}
              facing="back"
              barcodeScannerSettings={{ barcodeTypes: [...PRODUCT_BARCODE_TYPES] }}
              onBarcodeScanned={handled ? undefined : handleScan}
            />
            <View pointerEvents="none" style={styles.guide}>
              <View style={styles.scanWindow} />
              <Text style={styles.guideText}>EAN, UPC, Code 128, or QR</Text>
            </View>
          </View>
        ) : (
          <View style={styles.permissionState}>
            <Ionicons name="camera-outline" size={44} color={colors.textMuted} />
            <Text style={styles.permissionTitle}>Camera permission is required</Text>
            <Text style={styles.permissionBody}>
              Price Intelligence only uses the camera while you scan a product code.
            </Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => requestPermission()}
              style={({ pressed }) => [styles.permissionButton, pressed && styles.pressed]}
            >
              <Text style={styles.permissionButtonText}>Allow camera</Text>
            </Pressable>
          </View>
        )}
    </SafeAreaView>
  );

  if (embedded) return visible ? scannerContent : null;

  return (
    <Modal visible={visible} animationType="fade" presentationStyle="fullScreen" onRequestClose={onClose}>
      {scannerContent}
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: spacing.xl },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.3 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 20, marginTop: spacing.xs },
  closeButton: { width: 48, height: 48, borderRadius: radius.md, borderWidth: 1, borderColor: colors.border, alignItems: 'center', justifyContent: 'center' },
  cameraFrame: { flex: 1, overflow: 'hidden', margin: spacing.lg, marginTop: 0, borderRadius: radius.lg, borderWidth: 1, borderColor: colors.border },
  guide: { ...StyleSheet.absoluteFill, alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.12)' },
  scanWindow: { width: '82%', height: 190, borderWidth: 3, borderColor: colors.primary, borderRadius: radius.md, backgroundColor: 'transparent' },
  guideText: { color: colors.text, fontFamily: type.semibold, fontSize: 14, backgroundColor: 'rgba(11,15,20,0.82)', borderRadius: radius.pill, paddingHorizontal: spacing.lg, paddingVertical: spacing.sm, marginTop: spacing.lg },
  permissionState: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.xxl },
  permissionTitle: { color: colors.text, fontFamily: type.bold, fontSize: 21, textAlign: 'center', marginTop: spacing.lg },
  permissionBody: { color: colors.textMuted, fontFamily: type.regular, fontSize: 15, lineHeight: 22, textAlign: 'center', marginTop: spacing.sm },
  permissionButton: { minHeight: 52, justifyContent: 'center', backgroundColor: colors.primary, borderRadius: radius.md, paddingHorizontal: spacing.xl, marginTop: spacing.xl },
  permissionButtonText: { color: colors.background, fontFamily: type.bold, fontSize: 16 },
  pressed: { opacity: 0.7 },
});
