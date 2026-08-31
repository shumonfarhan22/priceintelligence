import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useState } from 'react';
import { Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radius, spacing, type } from '../../theme/tokens';

export type PriceTarget = 'purchaseCost' | 'shopPrice';
type Operator = '+' | '−' | '×' | '÷';

export function PriceCalculatorModal({
  visible,
  target,
  initialValue,
  onTargetChange,
  onUseValue,
  onClose,
  embedded = false,
}: {
  visible: boolean;
  target: PriceTarget;
  initialValue: string;
  onTargetChange: (target: PriceTarget) => void;
  onUseValue: (target: PriceTarget, value: string) => void;
  onClose: () => void;
  embedded?: boolean;
}) {
  const [display, setDisplay] = useState('0');
  const [storedValue, setStoredValue] = useState<number | null>(null);
  const [operator, setOperator] = useState<Operator | null>(null);
  const [waitingForOperand, setWaitingForOperand] = useState(false);

  useEffect(() => {
    if (!visible) return;
    const parsed = Number(initialValue);
    setDisplay(Number.isFinite(parsed) && parsed > 0 ? formatNumber(parsed) : '0');
    setStoredValue(null);
    setOperator(null);
    setWaitingForOperand(false);
  }, [visible]);

  const inputDigit = (digit: string) => {
    if (waitingForOperand || display === '0') {
      setDisplay(digit);
      setWaitingForOperand(false);
    } else if (display.length < 14) {
      setDisplay(`${display}${digit}`);
    }
  };

  const inputDecimal = () => {
    if (waitingForOperand) {
      setDisplay('0.');
      setWaitingForOperand(false);
    } else if (!display.includes('.')) {
      setDisplay(`${display}.`);
    }
  };

  const chooseOperator = (nextOperator: Operator) => {
    const inputValue = Number(display);
    if (!Number.isFinite(inputValue)) return;
    if (operator && storedValue != null && !waitingForOperand) {
      const result = calculate(storedValue, inputValue, operator);
      setDisplay(formatNumber(result));
      setStoredValue(result);
    } else {
      setStoredValue(inputValue);
    }
    setOperator(nextOperator);
    setWaitingForOperand(true);
  };

  const equals = () => {
    if (!operator || storedValue == null) return;
    const result = calculate(storedValue, Number(display), operator);
    setDisplay(formatNumber(result));
    setStoredValue(null);
    setOperator(null);
    setWaitingForOperand(true);
  };

  const clear = () => {
    setDisplay('0');
    setStoredValue(null);
    setOperator(null);
    setWaitingForOperand(false);
  };

  const backspace = () => {
    if (waitingForOperand) return;
    setDisplay((current) => current.length <= 1 ? '0' : current.slice(0, -1));
  };

  const calculatorContent = (
    <SafeAreaView style={[styles.root, Platform.OS === 'android' && styles.backdrop]}>
        <View style={styles.sheet}>
          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <Text style={styles.eyebrow}>PRICE CALCULATOR</Text>
              <Text style={styles.title} numberOfLines={1}>Calculate shop prices</Text>
            </View>
            <Pressable accessibilityRole="button" accessibilityLabel="Close calculator" onPress={onClose} style={styles.closeButton}>
              <Ionicons name="close" size={25} color={colors.text} />
            </Pressable>
          </View>

          <ScrollView
            contentContainerStyle={styles.content}
            showsVerticalScrollIndicator={false}
          >
            <View style={styles.targetRow}>
              <TargetButton label="Purchase Cost" selected={target === 'purchaseCost'} onPress={() => onTargetChange('purchaseCost')} />
              <TargetButton label="Selling Price" selected={target === 'shopPrice'} onPress={() => onTargetChange('shopPrice')} />
            </View>

            <View style={styles.displayCard}>
              <Text style={styles.pending}>{storedValue != null && operator ? `${formatNumber(storedValue)} ${operator}` : fieldName(target)}</Text>
              <Text style={styles.display} numberOfLines={1} adjustsFontSizeToFit>₹ {display}</Text>
            </View>

            <View style={styles.keypad}>
              <View style={styles.keyRow}>
                <CalculatorKey label="C" tone="danger" onPress={clear} />
                <CalculatorKey label="⌫" tone="muted" onPress={backspace} />
                <CalculatorKey label="÷" tone="accent" onPress={() => chooseOperator('÷')} />
                <CalculatorKey label="×" tone="accent" onPress={() => chooseOperator('×')} />
              </View>
              <View style={styles.keyRow}>
                {['7', '8', '9'].map((digit) => <CalculatorKey key={digit} label={digit} onPress={() => inputDigit(digit)} />)}
                <CalculatorKey label="−" tone="accent" onPress={() => chooseOperator('−')} />
              </View>
              <View style={styles.keyRow}>
                {['4', '5', '6'].map((digit) => <CalculatorKey key={digit} label={digit} onPress={() => inputDigit(digit)} />)}
                <CalculatorKey label="+" tone="accent" onPress={() => chooseOperator('+')} />
              </View>
              <View style={styles.keyRow}>
                {['1', '2', '3'].map((digit) => <CalculatorKey key={digit} label={digit} onPress={() => inputDigit(digit)} />)}
                <CalculatorKey label="." onPress={inputDecimal} />
              </View>
              <View style={styles.keyRow}>
                <CalculatorKey label="0" wide onPress={() => inputDigit('0')} />
                <CalculatorKey label="=" wide tone="primary" onPress={equals} />
              </View>
            </View>

            <Pressable
              accessibilityRole="button"
              onPress={() => {
                const value = Number(display);
                if (Number.isFinite(value) && value > 0) onUseValue(target, trimNumber(value));
              }}
              style={({ pressed }) => [styles.useButton, pressed && styles.pressed]}
            >
              <Text style={styles.useButtonText}>USE FOR {fieldName(target).toLocaleUpperCase()}</Text>
            </Pressable>
          </ScrollView>
        </View>
    </SafeAreaView>
  );

  if (embedded) return visible ? calculatorContent : null;

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={Platform.OS !== 'ios'}
      presentationStyle={Platform.OS === 'ios' ? 'pageSheet' : 'overFullScreen'}
      onRequestClose={onClose}
    >
      {calculatorContent}
    </Modal>
  );
}

function TargetButton({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={({ pressed }) => [styles.targetButton, selected && styles.targetSelected, pressed && styles.pressed]}
    >
      <Text style={[styles.targetText, selected && styles.targetTextSelected]}>{label}</Text>
    </Pressable>
  );
}

function CalculatorKey({
  label,
  onPress,
  tone = 'default',
  wide = false,
}: {
  label: string;
  onPress: () => void;
  tone?: 'default' | 'muted' | 'accent' | 'danger' | 'primary';
  wide?: boolean;
}) {
  const keyColor = tone === 'primary' ? colors.primary : tone === 'danger' ? colors.danger : tone === 'accent' ? colors.accent : colors.text;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label === '⌫' ? 'Backspace' : label}
      onPress={onPress}
      style={({ pressed }) => [
        styles.key,
        wide && styles.keyWide,
        tone === 'primary' && styles.keyPrimary,
        tone === 'muted' && styles.keyMuted,
        pressed && styles.pressed,
      ]}
    >
      <Text style={[styles.keyText, { color: tone === 'primary' ? colors.background : keyColor }]}>{label}</Text>
    </Pressable>
  );
}

function calculate(left: number, right: number, operator: Operator): number {
  const result = operator === '+' ? left + right
    : operator === '−' ? left - right
      : operator === '×' ? left * right
        : right === 0 ? left : left / right;
  return Number.isFinite(result) ? result : left;
}

function formatNumber(value: number): string {
  return trimNumber(Math.round(value * 100) / 100);
}

function trimNumber(value: number): string {
  return Number(value.toFixed(2)).toString();
}

function fieldName(target: PriceTarget): string {
  return target === 'purchaseCost' ? 'Purchase cost' : 'Selling price';
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.surface },
  backdrop: { backgroundColor: 'rgba(0,0,0,0.72)', justifyContent: 'flex-end' },
  sheet: { flex: Platform.OS === 'ios' ? 1 : undefined, backgroundColor: colors.surface, borderTopLeftRadius: Platform.OS === 'ios' ? 0 : radius.lg, borderTopRightRadius: Platform.OS === 'ios' ? 0 : radius.lg, overflow: 'hidden' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: spacing.xl, borderBottomWidth: 1, borderBottomColor: colors.border },
  headerCopy: { flex: 1, minWidth: 0, paddingRight: spacing.md },
  eyebrow: { color: colors.accent, fontFamily: type.bold, fontSize: 11, letterSpacing: 1.2 },
  title: { color: colors.text, fontFamily: type.bold, fontSize: 21, marginTop: spacing.xs },
  closeButton: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: radius.md, borderWidth: 1, borderColor: colors.border },
  content: { padding: spacing.xl, paddingBottom: spacing.xxl },
  targetRow: { flexDirection: 'row', gap: spacing.md },
  targetButton: { flex: 1, minHeight: 50, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, backgroundColor: colors.background },
  targetSelected: { borderColor: colors.accent, backgroundColor: 'rgba(139,124,246,0.14)' },
  targetText: { color: colors.textMuted, fontFamily: type.semibold, fontSize: 14 },
  targetTextSelected: { color: colors.text },
  displayCard: { alignItems: 'flex-end', justifyContent: 'center', minHeight: 112, backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md, padding: spacing.lg, marginTop: spacing.lg },
  pending: { color: colors.textMuted, fontFamily: type.regular, fontSize: 13 },
  display: { width: '100%', color: colors.text, fontFamily: type.bold, fontSize: 38, textAlign: 'right', marginTop: spacing.sm },
  keypad: { gap: spacing.sm, marginTop: spacing.lg },
  keyRow: { flexDirection: 'row', gap: spacing.sm },
  key: { flex: 1, height: 64, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border, borderRadius: radius.md },
  keyWide: { flex: 2 },
  keyPrimary: { backgroundColor: colors.primary, borderColor: colors.primary },
  keyMuted: { backgroundColor: colors.surfaceRaised },
  keyText: { fontFamily: type.bold, fontSize: 22 },
  useButton: { minHeight: 56, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.accent, borderRadius: radius.md, marginTop: spacing.lg },
  useButtonText: { color: colors.background, fontFamily: type.bold, fontSize: 14, letterSpacing: 0.5 },
  pressed: { opacity: 0.67 },
});
