import Ionicons from '@expo/vector-icons/Ionicons';
import * as Clipboard from 'expo-clipboard';
import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  InputAccessoryView,
  Keyboard,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { InventoryProduct } from '../../domain/models';
import {
  emptyInventoryDraft,
  type InventoryDraft,
  type ValidatedInventoryInput,
  validateInventoryDraft,
} from '../../domain/inventoryValidation';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { BarcodeScannerModal } from './BarcodeScannerModal';
import { PriceCalculatorModal, type PriceTarget } from './PriceCalculatorModal';

const ACCESSORY_ID = 'price-intelligence-product-editor-accessory';
type DraftField = keyof InventoryDraft;
const FIELD_ORDER: DraftField[] = [
  'productName',
  'purchaseCost',
  'shopPrice',
  'barcode',
  'amazonUrl',
  'flipkartUrl',
];

export function ProductEditorModal({
  visible,
  product,
  onClose,
  onSave,
  onScanComplete,
}: {
  visible: boolean;
  product: InventoryProduct | null;
  onClose: () => void;
  onSave: (input: ValidatedInventoryInput, editingId: number | null) => Promise<void>;
  onScanComplete?: () => void;
}) {
  const [draft, setDraft] = useState<InventoryDraft>(emptyInventoryDraft);
  const [error, setError] = useState<{ field: DraftField; message: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [focusedField, setFocusedField] = useState<DraftField | null>(null);
  const [clipboardAvailable, setClipboardAvailable] = useState(false);
  const [scannerVisible, setScannerVisible] = useState(false);
  const [calculatorVisible, setCalculatorVisible] = useState(false);
  const [calculatorTarget, setCalculatorTarget] = useState<PriceTarget>('purchaseCost');

  const nameRef = useRef<TextInput>(null);
  const purchaseRef = useRef<TextInput>(null);
  const sellingRef = useRef<TextInput>(null);
  const barcodeRef = useRef<TextInput>(null);
  const amazonRef = useRef<TextInput>(null);
  const flipkartRef = useRef<TextInput>(null);
  const refs: Record<DraftField, React.RefObject<TextInput | null>> = {
    productName: nameRef,
    purchaseCost: purchaseRef,
    shopPrice: sellingRef,
    barcode: barcodeRef,
    amazonUrl: amazonRef,
    flipkartUrl: flipkartRef,
  };

  useEffect(() => {
    if (!visible) return;
    setDraft(product ? draftFromProduct(product) : emptyInventoryDraft);
    setError(null);
    setSaving(false);
    setFocusedField(null);
    setClipboardAvailable(false);
    setCalculatorVisible(false);
  }, [visible, product?.id]);

  useEffect(() => {
    const subscription = Keyboard.addListener('keyboardDidHide', () => setFocusedField(null));
    return () => subscription.remove();
  }, []);

  const changeField = (field: DraftField, value: string) => {
    setDraft((current) => ({ ...current, [field]: value }));
    if (error?.field === field) setError(null);
  };

  const handleFocus = (field: DraftField) => {
    setFocusedField(field);
    if (field === 'productName') {
      Clipboard.hasStringAsync().then(setClipboardAvailable).catch(() => setClipboardAvailable(false));
    }
  };

  const pasteProductName = async () => {
    const value = await Clipboard.getStringAsync();
    if (value) {
      changeField('productName', value);
      nameRef.current?.focus();
    }
  };

  const submit = async () => {
    const validation = validateInventoryDraft(draft);
    if (!validation.valid) {
      setError({ field: validation.field, message: validation.message });
      refs[validation.field].current?.focus();
      return;
    }

    setSaving(true);
    try {
      await onSave(validation.input, product?.id ?? null);
      Keyboard.dismiss();
      onClose();
    } catch (saveError) {
      setError({
        field: 'productName',
        message: saveError instanceof Error ? saveError.message : 'The product could not be saved.',
      });
    } finally {
      setSaving(false);
    }
  };

  const close = () => {
    Keyboard.dismiss();
    onClose();
  };

  const focusedIndex = focusedField ? FIELD_ORDER.indexOf(focusedField) : -1;
  const focusRelativeField = (offset: number) => {
    const nextField = FIELD_ORDER[focusedIndex + offset];
    if (nextField) refs[nextField].current?.focus();
  };

  return (
    <>
      <Modal
        visible={visible}
        animationType="slide"
        transparent={Platform.OS !== 'ios'}
        presentationStyle={Platform.OS === 'ios' ? 'pageSheet' : 'overFullScreen'}
        onRequestClose={close}
      >
        <SafeAreaView style={[styles.modalRoot, Platform.OS === 'android' && styles.androidBackdrop]}>
          <Pressable style={styles.dismissArea} onPress={Keyboard.dismiss}>
            <View style={[styles.sheet, Platform.OS === 'ios' && styles.iosSheet]} onStartShouldSetResponder={() => true}>
              <View style={styles.header}>
                <View>
                  <Text style={styles.eyebrow}>{product ? 'EDIT INVENTORY' : 'NEW INVENTORY ITEM'}</Text>
                  <Text style={styles.title}>{product ? 'Edit Product' : 'Add New Product'}</Text>
                </View>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Close product editor"
                  onPress={close}
                  style={({ pressed }) => [styles.closeButton, pressed && styles.pressed]}
                >
                  <Ionicons name="close" size={25} color={colors.text} />
                </Pressable>
              </View>

              <ScrollView
                contentContainerStyle={styles.form}
                keyboardShouldPersistTaps="handled"
                keyboardDismissMode={Platform.OS === 'ios' ? 'interactive' : 'on-drag'}
                automaticallyAdjustKeyboardInsets
                showsVerticalScrollIndicator={false}
              >
                {error ? (
                  <View style={styles.errorBanner} accessibilityLiveRegion="assertive">
                    <Ionicons name="alert-circle" size={18} color={colors.danger} />
                    <Text style={styles.errorText}>{error.message}</Text>
                  </View>
                ) : null}

                <Field
                  label="Product Name"
                  value={draft.productName}
                  onChangeText={(value) => changeField('productName', value)}
                  onFocus={() => handleFocus('productName')}
                  onBlur={() => setTimeout(() => {
                    if (!nameRef.current?.isFocused()) {
                      setFocusedField((field) => field === 'productName' ? null : field);
                    }
                  }, 180)}
                  placeholder="e.g., Hawkins 3.5L Cooker"
                  inputRef={nameRef}
                  returnKeyType="next"
                  onSubmitEditing={() => purchaseRef.current?.focus()}
                  error={error?.field === 'productName'}
                  accessory={focusedField === 'productName' && clipboardAvailable ? (
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Paste product name"
                      onPress={pasteProductName}
                      style={({ pressed }) => [styles.inlineAction, pressed && styles.pressed]}
                    >
                      <Ionicons name="clipboard-outline" size={17} color={colors.primary} />
                      <Text style={styles.inlineActionText}>Paste</Text>
                    </Pressable>
                  ) : null}
                />

                <View style={styles.sectionHeadingRow}>
                  <Text style={styles.sectionLabel}>PRICE DETAILS</Text>
                  <Pressable
                    accessibilityRole="button"
                    onPress={() => {
                      Keyboard.dismiss();
                      setCalculatorVisible(true);
                    }}
                    style={({ pressed }) => [styles.calculatorButton, pressed && styles.pressed]}
                  >
                    <Ionicons name="calculator" size={17} color={colors.accent} />
                    <Text style={styles.calculatorButtonText}>Calculator</Text>
                  </Pressable>
                </View>
                <View style={styles.priceRow}>
                  <Field
                    compact
                    label="Purchase Cost"
                    value={draft.purchaseCost}
                    onChangeText={(value) => changeField('purchaseCost', value)}
                    onFocus={() => handleFocus('purchaseCost')}
                    placeholder="₹ 0.00"
                    inputMode="decimal"
                    inputRef={purchaseRef}
                    returnKeyType="next"
                    onSubmitEditing={() => sellingRef.current?.focus()}
                    error={error?.field === 'purchaseCost'}
                  />
                  <Field
                    compact
                    label="Selling Price"
                    value={draft.shopPrice}
                    onChangeText={(value) => changeField('shopPrice', value)}
                    onFocus={() => handleFocus('shopPrice')}
                    placeholder="₹ 0.00"
                    inputMode="decimal"
                    inputRef={sellingRef}
                    returnKeyType="next"
                    onSubmitEditing={() => barcodeRef.current?.focus()}
                    error={error?.field === 'shopPrice'}
                  />
                </View>

                <Field
                  label="Barcode"
                  value={draft.barcode}
                  onChangeText={(value) => changeField('barcode', value)}
                  onFocus={() => handleFocus('barcode')}
                  placeholder="Scan or enter barcode"
                  inputMode="text"
                  inputRef={barcodeRef}
                  returnKeyType="next"
                  onSubmitEditing={() => amazonRef.current?.focus()}
                  error={error?.field === 'barcode'}
                  accessory={(
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Scan barcode"
                      onPress={() => {
                        Keyboard.dismiss();
                        setScannerVisible(true);
                      }}
                      style={({ pressed }) => [styles.iconAction, pressed && styles.pressed]}
                    >
                      <Ionicons name="camera" size={22} color={colors.textMuted} />
                    </Pressable>
                  )}
                />

                <Text style={styles.sectionLabel}>OPTIONAL RETAILER LINKS</Text>
                <Field
                  label="Amazon"
                  value={draft.amazonUrl}
                  onChangeText={(value) => changeField('amazonUrl', value)}
                  onFocus={() => handleFocus('amazonUrl')}
                  placeholder="https://amazon.in/..."
                  inputMode="url"
                  autoCapitalize="none"
                  inputRef={amazonRef}
                  returnKeyType="next"
                  onSubmitEditing={() => flipkartRef.current?.focus()}
                  error={error?.field === 'amazonUrl'}
                />
                <Field
                  label="Flipkart"
                  value={draft.flipkartUrl}
                  onChangeText={(value) => changeField('flipkartUrl', value)}
                  onFocus={() => handleFocus('flipkartUrl')}
                  placeholder="https://flipkart.com/..."
                  inputMode="url"
                  autoCapitalize="none"
                  inputRef={flipkartRef}
                  returnKeyType="done"
                  onSubmitEditing={Keyboard.dismiss}
                  error={error?.field === 'flipkartUrl'}
                />

                <View style={styles.actions}>
                  <Pressable
                    accessibilityRole="button"
                    disabled={saving}
                    onPress={() => {
                      setDraft(emptyInventoryDraft);
                      setError(null);
                      nameRef.current?.focus();
                    }}
                    style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressed]}
                  >
                    <Text style={styles.secondaryButtonText}>Clear Form</Text>
                  </Pressable>
                  <Pressable
                    accessibilityRole="button"
                    disabled={saving}
                    onPress={submit}
                    style={({ pressed }) => [styles.primaryButton, pressed && styles.pressed, saving && styles.disabled]}
                  >
                    {saving ? (
                      <ActivityIndicator color={colors.background} />
                    ) : (
                      <Text style={styles.primaryButtonText}>{product ? 'SAVE CHANGES' : 'SAVE ITEM'}</Text>
                    )}
                  </Pressable>
                </View>
              </ScrollView>
            </View>
          </Pressable>
        </SafeAreaView>
        {Platform.OS === 'ios' ? (
          <InputAccessoryView nativeID={ACCESSORY_ID}>
            <View style={styles.keyboardAccessory}>
              <Text style={styles.keyboardFieldName}>{fieldLabel(focusedField)}</Text>
              <View style={styles.keyboardActions}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Previous field"
                  accessibilityState={{ disabled: focusedIndex <= 0 }}
                  disabled={focusedIndex <= 0}
                  onPress={() => focusRelativeField(-1)}
                  style={[styles.keyboardStep, focusedIndex <= 0 && styles.disabled]}
                >
                  <Ionicons name="chevron-up" size={21} color={colors.text} />
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Next field"
                  accessibilityState={{ disabled: focusedIndex < 0 || focusedIndex >= FIELD_ORDER.length - 1 }}
                  disabled={focusedIndex < 0 || focusedIndex >= FIELD_ORDER.length - 1}
                  onPress={() => focusRelativeField(1)}
                  style={[styles.keyboardStep, (focusedIndex < 0 || focusedIndex >= FIELD_ORDER.length - 1) && styles.disabled]}
                >
                  <Ionicons name="chevron-down" size={21} color={colors.text} />
                </Pressable>
                <Pressable accessibilityRole="button" onPress={Keyboard.dismiss} style={styles.doneButton}>
                  <Text style={styles.doneButtonText}>Done</Text>
                </Pressable>
              </View>
            </View>
          </InputAccessoryView>
        ) : null}
      </Modal>
      <BarcodeScannerModal
        visible={scannerVisible}
        onClose={() => setScannerVisible(false)}
        onScanned={(value) => {
          changeField('barcode', value);
          setScannerVisible(false);
          onScanComplete?.();
        }}
      />
      <PriceCalculatorModal
        visible={calculatorVisible}
        target={calculatorTarget}
        initialValue={draft[calculatorTarget]}
        onTargetChange={setCalculatorTarget}
        onUseValue={(target, value) => {
          changeField(target, value);
          setCalculatorVisible(false);
        }}
        onClose={() => setCalculatorVisible(false)}
      />
    </>
  );
}

function Field({
  label,
  value,
  onChangeText,
  onFocus,
  onBlur,
  placeholder,
  inputRef,
  inputMode = 'text',
  returnKeyType,
  onSubmitEditing,
  autoCapitalize = 'sentences',
  accessory,
  compact = false,
  error = false,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  onFocus: () => void;
  onBlur?: () => void;
  placeholder: string;
  inputRef: React.RefObject<TextInput | null>;
  inputMode?: 'text' | 'decimal' | 'url';
  returnKeyType: 'next' | 'done';
  onSubmitEditing: () => void;
  autoCapitalize?: 'none' | 'sentences';
  accessory?: React.ReactNode;
  compact?: boolean;
  error?: boolean;
}) {
  return (
    <View style={[styles.fieldGroup, compact && styles.compactField]}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={[styles.inputShell, error && styles.inputError]}>
        <TextInput
          ref={inputRef}
          value={value}
          onChangeText={onChangeText}
          onFocus={onFocus}
          onBlur={onBlur}
          placeholder={placeholder}
          placeholderTextColor={colors.textMuted}
          selectionColor={colors.primary}
          cursorColor={colors.primary}
          style={styles.input}
          inputMode={inputMode}
          autoCapitalize={autoCapitalize}
          autoCorrect={inputMode === 'text'}
          returnKeyType={returnKeyType}
          blurOnSubmit={false}
          onSubmitEditing={onSubmitEditing}
          inputAccessoryViewID={Platform.OS === 'ios' ? ACCESSORY_ID : undefined}
        />
        {accessory}
      </View>
    </View>
  );
}

function draftFromProduct(product: InventoryProduct): InventoryDraft {
  return {
    productName: product.productName,
    purchaseCost: product.purchaseCost?.toString() ?? '',
    shopPrice: product.shopPrice.toString(),
    barcode: product.barcode ?? '',
    amazonUrl: product.amazonUrl ?? '',
    flipkartUrl: product.flipkartUrl ?? '',
  };
}

function fieldLabel(field: DraftField | null): string {
  switch (field) {
    case 'productName': return 'Product name';
    case 'purchaseCost': return 'Purchase cost';
    case 'shopPrice': return 'Selling price';
    case 'barcode': return 'Barcode';
    case 'amazonUrl': return 'Amazon link';
    case 'flipkartUrl': return 'Flipkart link';
    default: return 'Product details';
  }
}

const styles = StyleSheet.create({
  modalRoot: { flex: 1, backgroundColor: colors.surface },
  androidBackdrop: { backgroundColor: 'rgba(0,0,0,0.72)', justifyContent: 'flex-end' },
  dismissArea: { flex: 1, justifyContent: 'flex-end' },
  sheet: { maxHeight: '96%', backgroundColor: colors.surface, borderTopLeftRadius: radius.lg, borderTopRightRadius: radius.lg, borderWidth: 1, borderBottomWidth: 0, borderColor: colors.border, overflow: 'hidden' },
  iosSheet: { flex: 1, maxHeight: '100%', borderWidth: 0, borderRadius: 0 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: spacing.xl, paddingVertical: spacing.lg, borderBottomWidth: 1, borderBottomColor: colors.border },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 24, marginTop: spacing.xs },
  closeButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border },
  form: { padding: spacing.xl, paddingBottom: 48, gap: spacing.lg },
  errorBanner: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(244,63,94,0.10)', borderColor: 'rgba(244,63,94,0.45)', borderWidth: 1, borderRadius: radius.md, padding: spacing.md },
  errorText: { flex: 1, color: colors.danger, fontFamily: type.semibold, fontSize: 14, lineHeight: 19, marginLeft: spacing.sm },
  sectionLabel: { color: colors.textMuted, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2, marginTop: spacing.sm },
  sectionHeadingRow: { minHeight: 46, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  calculatorButton: { minHeight: 42, flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.md, borderWidth: 1, borderColor: colors.border, borderRadius: radius.sm, backgroundColor: colors.background },
  calculatorButtonText: { color: colors.text, fontFamily: type.semibold, fontSize: 13, marginLeft: spacing.sm },
  priceRow: { flexDirection: 'row', gap: spacing.md },
  fieldGroup: { width: '100%' },
  compactField: { flex: 1 },
  fieldLabel: { color: colors.text, fontFamily: type.semibold, fontSize: 14, marginBottom: spacing.sm },
  inputShell: { minHeight: 58, flexDirection: 'row', alignItems: 'center', backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, paddingLeft: spacing.lg, paddingRight: spacing.sm },
  inputError: { borderColor: colors.danger },
  input: { flex: 1, minHeight: 56, color: colors.text, fontFamily: type.regular, fontSize: 16, paddingVertical: spacing.md },
  inlineAction: { minHeight: 44, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingHorizontal: spacing.md, borderRadius: radius.sm, backgroundColor: colors.primaryMuted },
  inlineActionText: { color: colors.primary, fontFamily: type.bold, fontSize: 13, marginLeft: spacing.xs },
  iconAction: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center' },
  actions: { flexDirection: 'row', gap: spacing.md, marginTop: spacing.sm },
  secondaryButton: { flex: 1, minHeight: 56, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md },
  secondaryButtonText: { color: colors.text, fontFamily: type.bold, fontSize: 14 },
  primaryButton: { flex: 1, minHeight: 56, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.primary, borderRadius: radius.md },
  primaryButtonText: { color: colors.background, fontFamily: type.bold, fontSize: 14, letterSpacing: 0.4 },
  keyboardAccessory: { minHeight: 48, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.surfaceRaised, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.border, paddingHorizontal: spacing.lg },
  keyboardFieldName: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 13 },
  keyboardActions: { flexDirection: 'row', alignItems: 'center' },
  keyboardStep: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  doneButton: { minWidth: 60, minHeight: 44, alignItems: 'flex-end', justifyContent: 'center' },
  doneButtonText: { color: colors.primary, fontFamily: type.bold, fontSize: 16 },
  pressed: { opacity: 0.68 },
  disabled: { opacity: 0.55 },
});
