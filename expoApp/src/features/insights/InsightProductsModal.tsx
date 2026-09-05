import Ionicons from '@expo/vector-icons/Ionicons';
import React, { useMemo, useState } from 'react';
import {
  FlatList,
  Image,
  Modal,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {
  InsightGroup,
  InsightProduct,
  PricingInsightsSnapshot,
  getGroupTitle,
  getProductBasicReason,
  insightBrand,
  productMatchesGroup,
} from '../../domain/insights';
import { colors, radius, spacing, type } from '../../theme/tokens';
import { InsightProductAnalysisModal } from './InsightProductAnalysisModal';
import { formatRupees } from '../../domain/formatting';
import { useCustomization } from '../../theme/CustomizationContext';
import type { DynamicColors } from '../../theme/dynamicTheme';

function getModalTokens(colors: DynamicColors) {
  return {
    primary: colors.primary,
    warning: colors.warning,
    danger: colors.danger,
    panelStrong: colors.surfaceRaised,
    panel: colors.surface,
    border: colors.border,
    text: colors.text,
    textMuted: colors.textMuted,
  };
}
type ModalTokens = ReturnType<typeof getModalTokens>;

export function InsightProductsModal({
  group,
  brand,
  snapshot,
  repository,
  showBanner,
  onClose,
}: {
  group: InsightGroup | null;
  brand: string | null;
  snapshot: PricingInsightsSnapshot;
  repository: any;
  showBanner: (msg: string, tone?: 'info' | 'success' | 'error') => void;
  onClose: () => void;
}) {
  const { colors } = useCustomization();
  const tokens = useMemo(() => getModalTokens(colors), [colors]);
  const styles = useMemo(() => createStyles(tokens), [tokens]);
  const [selectedProduct, setSelectedProduct] = useState<InsightProduct | null>(null);

  const products = useMemo(() => {
    if (brand) {
      return snapshot.products.filter((p) => insightBrand(p.item) === brand);
    }
    if (group) {
      return snapshot.products.filter((p) => productMatchesGroup(p, group));
    }
    return [];
  }, [snapshot, group, brand]);

  const title = brand ? `${brand} brand health` : getGroupTitle(group);

  const renderProductRow = ({ item: prod }: { item: InsightProduct }) => {
    const item = prod.item;
    const isCompetitive = prod.position === 'COMPETITIVE';
    const isReview = prod.position === 'REVIEW';
    const reasonColor = isCompetitive
      ? tokens.primary
      : isReview
      ? tokens.danger
      : tokens.textMuted;

    return (
      <Pressable
        testID="insight-product-item"
        accessibilityLabel={`View analysis for ${item.productName}`}
        style={({ pressed }) => [styles.productRow, pressed && styles.pressed]}
        onPress={() => setSelectedProduct(prod)}
      >
        <View style={styles.imageBox}>
          {item.imageUrl ? (
            <Image source={{ uri: item.imageUrl }} style={styles.image} resizeMode="contain" />
          ) : (
            <Text style={styles.initialFallback}>
              {item.productName.trim().charAt(0).toUpperCase() || 'P'}
            </Text>
          )}
        </View>

        <View style={styles.productTextCol}>
          <Text style={styles.productName} numberOfLines={3}>
            {item.productName}
          </Text>
          <Text style={[styles.productReason, { color: reasonColor }]} numberOfLines={1}>
            {getProductBasicReason(prod)}
          </Text>
        </View>

        <Ionicons name="chevron-forward" size={18} color={tokens.textMuted} style={styles.chevron} />
      </Pressable>
    );
  };

  return (
    <Modal visible animationType="fade" transparent onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <SafeAreaView style={styles.modalSafeContainer}>
          {selectedProduct ? (
            <InsightProductAnalysisModal
              isEmbedded
              product={selectedProduct}
              group={group}
              brand={brand}
              groupSize={products.length}
              repository={repository}
              showBanner={showBanner}
              onBack={() => setSelectedProduct(null)}
              onClose={onClose}
            />
          ) : (
            <View style={styles.sheet}>
              {/* Header */}
              <View style={styles.header}>
                <View style={styles.headerTitles}>
                  <Text style={styles.title} numberOfLines={2}>
                    {title}
                  </Text>
                  <Text style={styles.countSubtitle}>
                    {products.length === 1 ? '1 product' : `${products.length} products`}
                  </Text>
                </View>
                <Pressable
                  onPress={onClose}
                  accessibilityLabel="Close product group"
                  style={({ pressed }) => [styles.closeBtn, pressed && styles.pressed]}
                >
                  <Ionicons name="close" size={24} color={tokens.text} />
                </Pressable>
              </View>

              <View style={styles.divider} />

              {/* List / Empty */}
              {products.length === 0 ? (
                <View style={styles.emptyContainer}>
                  <Text style={styles.emptyText}>No products are in this group.</Text>
                </View>
              ) : (
                <FlatList
                  data={products}
                  keyExtractor={(p) => p.item.id.toString()}
                  renderItem={renderProductRow}
                  contentContainerStyle={styles.listContent}
                  ItemSeparatorComponent={() => <View style={styles.divider} />}
                  showsVerticalScrollIndicator={false}
                />
              )}
            </View>
          )}
        </SafeAreaView>
      </View>
    </Modal>
  );
}

function createStyles(tokens: ModalTokens) {
  return StyleSheet.create({
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.65)',
      justifyContent: 'center',
      alignItems: 'center',
    },
    modalSafeContainer: {
      width: '96%',
      maxHeight: '94%',
      alignItems: 'center',
      justifyContent: 'center',
    },
    sheet: {
      width: '100%',
      height: '100%',
      backgroundColor: tokens.panelStrong,
      borderRadius: 24,
      borderWidth: 1,
      borderColor: tokens.border,
      overflow: 'hidden',
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 18,
      paddingTop: 12,
      paddingBottom: 10,
    },
    headerTitles: {
      flex: 1,
      marginRight: 8,
    },
    title: {
      color: tokens.text,
      fontSize: 16,
      fontFamily: type.bold,
    },
    countSubtitle: {
      color: tokens.textMuted,
      fontSize: 11,
      fontFamily: type.regular,
      marginTop: 2,
    },
    closeBtn: {
      width: 40,
      height: 40,
      justifyContent: 'center',
      alignItems: 'center',
    },
    divider: {
      height: 1,
      backgroundColor: tokens.border,
    },
    listContent: {
      paddingHorizontal: 16,
      paddingVertical: 8,
      paddingBottom: 28,
    },
    productRow: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingVertical: 10,
    },
    imageBox: {
      width: 76,
      height: 76,
      borderRadius: 16,
      backgroundColor: '#F8FAFC',
      alignItems: 'center',
      justifyContent: 'center',
      overflow: 'hidden',
      padding: 6,
    },
    image: {
      width: '100%',
      height: '100%',
    },
    initialFallback: {
      fontSize: 26,
      color: '#475569',
      fontFamily: type.bold,
    },
    productTextCol: {
      flex: 1,
      marginLeft: 12,
      justifyContent: 'center',
    },
    productName: {
      color: tokens.text,
      fontSize: 14,
      fontFamily: type.bold,
      lineHeight: 19,
      marginBottom: 4,
    },
    productReason: {
      fontSize: 11,
      fontFamily: type.regular,
      lineHeight: 15,
    },
    chevron: {
      marginLeft: 6,
    },
    emptyContainer: {
      padding: 32,
      alignItems: 'center',
      justifyContent: 'center',
    },
    emptyText: {
      color: tokens.textMuted,
      fontSize: 13,
      fontFamily: type.regular,
    },
    pressed: {
      opacity: 0.75,
    },
  });
}
