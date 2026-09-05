import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import { triggerSuccessHaptic } from '../../utils/haptics';
import { useEffect, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Linking,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  Vibration,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Defs, Line, Mask, Rect } from 'react-native-svg';

import { useCustomization } from '../../theme/CustomizationContext';
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
  onError,
  embedded = false,
}: {
  visible: boolean;
  onClose: () => void;
  onScanned: (value: string) => void;
  onError?: (message: string) => void;
  embedded?: boolean;
}) {
  const [permission, requestPermission] = useCameraPermissions();
  const [handled, setHandled] = useState(false);
  const [torchEnabled, setTorchEnabled] = useState(false);
  const [permissionRecoveryVisible, setPermissionRecoveryVisible] = useState(false);
  const handledRef = useRef(false);
  const permissionRequestStartedRef = useRef(false);

  const insets = useSafeAreaInsets();
  const { width, height } = useWindowDimensions();

  let hapticsEnabled = true;
  let isReducedMotion = false;
  try {
    const ctx = useCustomization();
    hapticsEnabled = ctx.customization.hapticsEnabled;
    isReducedMotion = ctx.customization.motionPreference === 'REDUCED';
  } catch {
    // fallback if used without context
  }

  // Viewfinder geometry matching Compose: 70% width square cutout
  const cutoutWidth = Math.round(Math.min(width * 0.70, 320));
  const cutoutHeight = cutoutWidth;
  const cutoutLeft = Math.round((width - cutoutWidth) / 2);
  const cutoutTop = Math.round((height - cutoutHeight) / 2 - 20);
  const cutoutRight = cutoutLeft + cutoutWidth;
  const cutoutBottom = cutoutTop + cutoutHeight;

  // Oscillating laser sweep animation
  const laserAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      handledRef.current = false;
      setHandled(false);
      setTorchEnabled(false);
      setPermissionRecoveryVisible(false);
    } else {
      permissionRequestStartedRef.current = false;
    }
  }, [visible]);

  // Request permission automatically when opened
  useEffect(() => {
    if (
      !visible ||
      !permission ||
      permission.granted ||
      permissionRequestStartedRef.current
    ) {
      return;
    }

    if (!permission.canAskAgain && !permission.granted) {
      setPermissionRecoveryVisible(true);
      return;
    }

    permissionRequestStartedRef.current = true;
    requestPermission()
      .then((res) => {
        if (!res.granted) {
          setPermissionRecoveryVisible(true);
        }
      })
      .catch(() => {
        setPermissionRecoveryVisible(true);
      });
  }, [permission, requestPermission, visible]);

  // Laser sweep animation loop
  useEffect(() => {
    if (!visible || handled || isReducedMotion) {
      laserAnim.setValue(0.5);
      return;
    }

    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(laserAnim, {
          toValue: 1,
          duration: 1800,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(laserAnim, {
          toValue: 0,
          duration: 1800,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ])
    );

    animation.start();
    return () => animation.stop();
  }, [visible, handled, isReducedMotion, laserAnim]);

  const laserTranslateY = laserAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [cutoutTop + 10, cutoutBottom - 14],
  });

  const handleScan = (result: BarcodeScanningResult) => {
    const value = result.data.trim();
    if (!value || handledRef.current) return;
    handledRef.current = true;
    setHandled(true);

    if (hapticsEnabled) {
      void triggerSuccessHaptic();
    }

    onScanned(value);
  };

  const bracketLength = 36;
  const bracketStroke = 4;
  const bracketColor = '#10B981';

  const scannerContent = (
    <View style={styles.fullscreen}>
      {/* 1. Camera View */}
      {permission?.granted ? (
        <CameraView
          style={StyleSheet.absoluteFill}
          facing="back"
          enableTorch={torchEnabled}
          barcodeScannerSettings={{ barcodeTypes: [...PRODUCT_BARCODE_TYPES] }}
          onBarcodeScanned={handled ? undefined : handleScan}
        />
      ) : (
        <View style={[StyleSheet.absoluteFill, styles.cameraPlaceholder]} />
      )}

      {/* 2. Cutout Canvas Overlay with 65% Black scrim and rounded cutout */}
      <Svg style={StyleSheet.absoluteFill} width={width} height={height}>
        <Defs>
          <Mask id="cutoutMask" x="0" y="0" width={width} height={height}>
            <Rect x="0" y="0" width={width} height={height} fill="#FFFFFF" />
            <Rect
              x={cutoutLeft}
              y={cutoutTop}
              width={cutoutWidth}
              height={cutoutHeight}
              rx={16}
              ry={16}
              fill="#000000"
            />
          </Mask>
        </Defs>

        {/* 65% Black Scrim punched out by mask */}
        <Rect
          x="0"
          y="0"
          width={width}
          height={height}
          fill="rgba(0,0,0,0.65)"
          mask="url(#cutoutMask)"
        />

        {/* 4 Emerald Corner Brackets matching Compose */}
        {/* Top-Left */}
        <Line
          x1={cutoutLeft}
          y1={cutoutTop}
          x2={cutoutLeft + bracketLength}
          y2={cutoutTop}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />
        <Line
          x1={cutoutLeft}
          y1={cutoutTop}
          x2={cutoutLeft}
          y2={cutoutTop + bracketLength}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />

        {/* Top-Right */}
        <Line
          x1={cutoutRight}
          y1={cutoutTop}
          x2={cutoutRight - bracketLength}
          y2={cutoutTop}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />
        <Line
          x1={cutoutRight}
          y1={cutoutTop}
          x2={cutoutRight}
          y2={cutoutTop + bracketLength}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />

        {/* Bottom-Left */}
        <Line
          x1={cutoutLeft}
          y1={cutoutBottom}
          x2={cutoutLeft + bracketLength}
          y2={cutoutBottom}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />
        <Line
          x1={cutoutLeft}
          y1={cutoutBottom}
          x2={cutoutLeft}
          y2={cutoutBottom - bracketLength}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />

        {/* Bottom-Right */}
        <Line
          x1={cutoutRight}
          y1={cutoutBottom}
          x2={cutoutRight - bracketLength}
          y2={cutoutBottom}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />
        <Line
          x1={cutoutRight}
          y1={cutoutBottom}
          x2={cutoutRight}
          y2={cutoutBottom - bracketLength}
          stroke={bracketColor}
          strokeWidth={bracketStroke}
          strokeLinecap="round"
        />
      </Svg>

      {/* 3. Animated Oscillating Laser Sweep Bar */}
      {!handled && !isReducedMotion && (
        <Animated.View
          style={[
            styles.laserBar,
            {
              left: cutoutLeft + 14,
              width: cutoutWidth - 28,
              transform: [{ translateY: laserTranslateY }],
            },
          ]}
        />
      )}

      {/* 4. Top Close Button (matching Compose padding and 52dp hit box) */}
      <View
        style={[
          styles.closeButtonContainer,
          { top: Math.max(insets.top + 8, 20) },
        ]}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Close scanner"
          onPress={onClose}
          style={({ pressed }) => [
            styles.closeIconButton,
            pressed && styles.pressed,
          ]}
        >
          <MaterialIcons name="close" size={30} color="#FFFFFF" />
        </Pressable>
      </View>

      {/* 5. Bottom Controls (Torch & Instruction text matching Compose) */}
      <View
        style={[
          styles.bottomControls,
          { bottom: Math.max(insets.bottom, 24) + 16 },
        ]}
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={torchEnabled ? 'Turn flashlight off' : 'Turn flashlight on'}
          onPress={() => setTorchEnabled((prev) => !prev)}
          style={({ pressed }) => [
            styles.flashlightButton,
            torchEnabled && styles.flashlightButtonActive,
            pressed && styles.pressed,
          ]}
        >
          <MaterialIcons
            name="flashlight-on"
            size={25}
            color={torchEnabled ? '#FFD700' : '#FFFFFF'}
          />
        </Pressable>

        <Text style={styles.instructionText}>
          Point the camera at a barcode
        </Text>
      </View>

      {/* 6. Permission Recovery Dialog */}
      {permissionRecoveryVisible && (
        <View style={styles.dialogBackdrop}>
          <View style={styles.dialogCard}>
            <View style={styles.dialogIconContainer}>
              <MaterialIcons name="videocam-off" size={28} color={colors.warning} />
            </View>
            <Text style={styles.dialogTitle}>Camera access is off</Text>
            <Text style={styles.dialogExplanation}>
              Allow camera access in Settings to scan product barcodes. Manual barcode entry remains available.
            </Text>
            <View style={styles.dialogActions}>
              <Pressable
                accessibilityRole="button"
                onPress={() => {
                  setPermissionRecoveryVisible(false);
                  onClose();
                }}
                style={({ pressed }) => [
                  styles.dialogSecondaryButton,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.dialogSecondaryButtonText}>Not now</Text>
              </Pressable>

              <Pressable
                accessibilityRole="button"
                onPress={async () => {
                  setPermissionRecoveryVisible(false);
                  try {
                    await Linking.openSettings();
                  } catch {}
                  onClose();
                }}
                style={({ pressed }) => [
                  styles.dialogPrimaryButton,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.dialogPrimaryButtonText}>Open Settings</Text>
              </Pressable>
            </View>
          </View>
        </View>
      )}
    </View>
  );

  if (embedded) return visible ? scannerContent : null;

  return (
    <Modal
      visible={visible}
      animationType="fade"
      presentationStyle="fullScreen"
      onRequestClose={onClose}
    >
      {scannerContent}
    </Modal>
  );
}

const styles = StyleSheet.create({
  fullscreen: {
    flex: 1,
    backgroundColor: '#000000',
  },
  cameraPlaceholder: {
    backgroundColor: '#0A0E14',
  },
  laserBar: {
    position: 'absolute',
    height: 3,
    backgroundColor: '#10B981',
    borderRadius: 2,
    shadowColor: '#10B981',
    shadowOpacity: 0.9,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 0 },
    elevation: 4,
  },
  closeButtonContainer: {
    position: 'absolute',
    left: 14,
    zIndex: 10,
  },
  closeIconButton: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(0, 0, 0, 0.40)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  bottomControls: {
    position: 'absolute',
    left: 0,
    right: 0,
    alignItems: 'center',
    zIndex: 10,
  },
  flashlightButton: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: 'rgba(0, 0, 0, 0.42)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.24)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 26,
  },
  flashlightButtonActive: {
    backgroundColor: 'rgba(255, 215, 0, 0.20)',
    borderColor: '#FFD700',
    borderWidth: 2,
  },
  instructionText: {
    color: '#FFFFFF',
    fontFamily: type.semibold,
    fontSize: 15,
    fontWeight: '500',
    textAlign: 'center',
    textShadowColor: 'rgba(0, 0, 0, 0.75)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 3,
  },
  dialogBackdrop: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.xl,
    zIndex: 50,
  },
  dialogCard: {
    width: '100%',
    maxWidth: 380,
    backgroundColor: '#161B22',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#30363D',
    padding: spacing.xl,
    alignItems: 'center',
  },
  dialogIconContainer: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: 'rgba(245, 158, 11, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  dialogTitle: {
    color: '#F0F6FC',
    fontFamily: type.bold,
    fontSize: 19,
    textAlign: 'center',
    marginBottom: spacing.sm,
  },
  dialogExplanation: {
    color: '#8B949E',
    fontFamily: type.regular,
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  dialogActions: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    width: '100%',
    gap: spacing.md,
  },
  dialogSecondaryButton: {
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    borderRadius: radius.md,
  },
  dialogSecondaryButtonText: {
    color: '#8B949E',
    fontFamily: type.semibold,
    fontSize: 14,
  },
  dialogPrimaryButton: {
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl,
    backgroundColor: '#10B981',
    borderRadius: radius.md,
  },
  dialogPrimaryButtonText: {
    color: '#0D1117',
    fontFamily: type.bold,
    fontSize: 14,
  },
  pressed: {
    opacity: 0.7,
  },
});
