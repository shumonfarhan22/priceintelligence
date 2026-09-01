import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { appendCalculatorKey, evaluatePriceExpression, formatCalculatorValue } from '../../domain/priceCalculator';
import { colors, radius, spacing, type } from '../../theme/tokens';

export type PriceTarget = 'purchaseCost' | 'shopPrice';

const KEYS = [
  ['AC', 'BACKSPACE', '%', '÷'],
  ['7', '8', '9', '×'],
  ['4', '5', '6', '−'],
  ['1', '2', '3', '+'],
  ['00', '0', '.', '='],
] as const;

export function PriceCalculatorModal({
  visible,
  target,
  purchaseCost,
  sellingPrice,
  onTargetChange,
  onUseValue,
  onClose,
  embedded = false,
}: {
  visible: boolean;
  target: PriceTarget;
  purchaseCost: string;
  sellingPrice: string;
  onTargetChange: (target: PriceTarget) => void;
  onUseValue: (target: PriceTarget, value: string) => void;
  onClose: () => void;
  embedded?: boolean;
}) {
  const [expression, setExpression] = useState('0');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!visible) return;
    const initial = target === 'purchaseCost' ? purchaseCost : sellingPrice;
    setExpression(initial.trim() || '0');
    setError(null);
  }, [visible]);

  const calculate = () => {
    try {
      const value = evaluatePriceExpression(expression);
      if (value <= 0) throw new Error('Price must be greater than zero');
      setError(null);
      return value;
    } catch (calculationError) {
      setError(calculationError instanceof Error ? calculationError.message : 'Check the calculation');
      return null;
    }
  };

  const pressKey = (key: string) => {
    if (key === 'AC') setExpression('0');
    else if (key === 'BACKSPACE') setExpression((current) => current.slice(0, -1) || '0');
    else if (key === '=') {
      const value = calculate();
      if (value != null) setExpression(formatCalculatorValue(value));
    } else setExpression((current) => appendCalculatorKey(current, key));
    setError(null);
  };

  const content = (
    <SafeAreaView style={styles.root}>
      <View style={styles.sheet}>
        <View style={styles.header}>
          <View style={styles.headerCopy}>
            <Text style={styles.eyebrow}>PRICE CALCULATOR</Text>
            <Text style={styles.subtitle}>Calculate without leaving the product form</Text>
          </View>
          <Pressable accessibilityRole="button" accessibilityLabel="Close calculator" onPress={onClose} style={styles.closeButton}>
            <Ionicons name="close" size={25} color={colors.text} />
          </Pressable>
        </View>

        <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
          <View style={styles.targetRow}>
            <TargetButton label="Purchase Cost" value={purchaseCost} selected={target === 'purchaseCost'} onPress={() => onTargetChange('purchaseCost')} />
            <TargetButton label="Selling Price" value={sellingPrice} selected={target === 'shopPrice'} onPress={() => onTargetChange('shopPrice')} />
          </View>

          <View style={styles.displayCard}>
            <Text style={styles.display} numberOfLines={1} adjustsFontSizeToFit>{expression || '0'}</Text>
          </View>

          {error ? <Text style={styles.error} accessibilityLiveRegion="assertive">{error}</Text> : null}

          <View style={styles.keypad}>
            {KEYS.map((row) => (
              <View key={row.join('')} style={styles.keyRow}>
                {row.map((key) => <CalculatorKey key={key} label={key} onPress={() => pressKey(key)} />)}
              </View>
            ))}
          </View>

          <Pressable
            accessibilityRole="button"
            onPress={() => {
              const value = calculate();
              if (value == null) return;
              onUseValue(target, formatCalculatorValue(value));
              onClose();
            }}
            style={({ pressed }) => [styles.useButton, pressed && styles.pressed]}
          >
            <Ionicons name="checkmark" size={21} color={colors.background} />
            <Text style={styles.useButtonText}>Use result for {fieldName(target)}</Text>
          </Pressable>
        </ScrollView>
      </View>
    </SafeAreaView>
  );

  if (embedded) return visible ? content : null;
  return (
    <Modal visible={visible} animationType="fade" transparent presentationStyle="overFullScreen" onRequestClose={onClose}>
      {content}
    </Modal>
  );
}

function TargetButton({ label, value, selected, onPress }: { label: string; value: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[styles.targetButton, selected && styles.targetSelected]}>
      <Text style={styles.targetLabel} numberOfLines={1}>{label}</Text>
      <Text style={styles.targetValue} numberOfLines={1}>₹ {value.trim() || '0'}</Text>
    </Pressable>
  );
}

function CalculatorKey({ label, onPress }: { label: string; onPress: () => void }) {
  const operator = ['%', '÷', '×', '−', '+', '='].includes(label);
  const equals = label === '=';
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label === 'BACKSPACE' ? 'Delete last calculator character' : label}
      onPress={onPress}
      style={({ pressed }) => [styles.key, operator && styles.operatorKey, equals && styles.equalsKey, pressed && styles.pressed]}
    >
      {label === 'BACKSPACE' ? (
        <Ionicons name="backspace-outline" size={21} color={colors.text} />
      ) : (
        <Text style={[styles.keyText, equals && styles.equalsText]}>{label}</Text>
      )}
    </Pressable>
  );
}

function fieldName(target: PriceTarget): string {
  return target === 'purchaseCost' ? 'Purchase Cost' : 'Selling Price';
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.78)', paddingHorizontal: spacing.md, paddingVertical: spacing.lg },
  sheet: { width: '100%', maxWidth: 560, maxHeight: '96%', overflow: 'hidden', backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.border, borderRadius: radius.lg },
  header: { minHeight: 62, flexDirection: 'row', alignItems: 'center', paddingLeft: spacing.lg, paddingRight: spacing.sm, paddingTop: spacing.sm },
  headerCopy: { flex: 1, minWidth: 0 },
  eyebrow: { color: colors.primary, fontFamily: type.bold, fontSize: 11, letterSpacing: 0.8 },
  subtitle: { color: colors.textMuted, fontFamily: type.regular, fontSize: 11, marginTop: 3 },
  closeButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  content: { padding: spacing.lg, paddingTop: spacing.sm, gap: spacing.md },
  targetRow: { flexDirection: 'row', gap: 10 },
  targetButton: { flex: 1, minWidth: 0, minHeight: 62, justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: 14, backgroundColor: colors.surfaceRaised, padding: spacing.md },
  targetSelected: { borderWidth: 1.5, borderColor: colors.primary, backgroundColor: 'rgba(16,185,129,0.14)' },
  targetLabel: { color: colors.textMuted, fontFamily: type.regular, fontSize: 10 },
  targetValue: { color: colors.text, fontFamily: type.bold, fontSize: 16, marginTop: 3 },
  displayCard: { height: 78, alignItems: 'flex-end', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.background, paddingHorizontal: spacing.lg },
  display: { width: '100%', color: colors.text, fontFamily: type.bold, fontSize: 27, textAlign: 'right' },
  error: { color: colors.danger, fontFamily: type.semibold, fontSize: 12 },
  keypad: { gap: spacing.sm },
  keyRow: { flexDirection: 'row', gap: spacing.sm },
  key: { flex: 1, height: 50, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: 14, backgroundColor: colors.surfaceRaised },
  operatorKey: { borderColor: 'rgba(16,185,129,0.38)', backgroundColor: 'rgba(16,185,129,0.14)' },
  equalsKey: { borderColor: colors.primary, backgroundColor: colors.primary },
  keyText: { color: colors.text, fontFamily: type.bold, fontSize: 18 },
  equalsText: { color: colors.background },
  useButton: { minHeight: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: 14, backgroundColor: colors.primary },
  useButtonText: { color: colors.background, fontFamily: type.bold, fontSize: 14, marginLeft: spacing.sm },
  pressed: { opacity: 0.68 },
});
