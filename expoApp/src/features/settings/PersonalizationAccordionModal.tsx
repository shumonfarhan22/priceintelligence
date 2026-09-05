import Ionicons from '@expo/vector-icons/Ionicons';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import React, { useState, useEffect } from 'react';
import {
  LayoutChangeEvent,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';

import type {
  AppColorPalette,
  AppContrastMode,
  AppFontStyle,
  AppTextSize,
  AppThemeMode,
  DashboardDefaultSort,
  DashboardPageSize,
  HistoryGraphStyle,
  AdvancedInfoLevel,
  PriceHistoryRange,
  MovementDefaultRetailer,
  MovementProductSort,
  PriceAlertDirection,
  PriceAlertThreshold,
  PriceEmphasis,
  PriceMovementDefaultRange,
  PriorityProductLimit,
  PriorityRowStyle,
  PrioritySortMode,
  SectionStartState,
} from '../../domain/customization';
import {
  normalizePaletteHex,
} from '../../domain/customization';
import { useCustomization } from '../../theme/CustomizationContext';
import { withAlpha } from '../../theme/dynamicTheme';
import { radius, spacing, type } from '../../theme/tokens';
import type { PreviewTarget } from './AdaptivePersonalizationPreview';
import { AdaptivePersonalizationPreview, PREVIEW_TARGETS } from './AdaptivePersonalizationPreview';

export type AccordionSection =
  | 'APPEARANCE'
  | 'QUICK_COMPARE'
  | 'SHOP_SUMMARY'
  | 'PRODUCT_DETAILS'
  | 'PRICE_MOVEMENT'
  | 'ALERTS_BEHAVIOUR';

interface SectionMeta {
  id: AccordionSection;
  title: string;
  icon: keyof typeof Ionicons.glyphMap;
  editorLabel: string;
}

const SECTIONS_META: SectionMeta[] = [
  { id: 'APPEARANCE', title: 'Appearance', icon: 'color-palette', editorLabel: 'Appearance' },
  { id: 'QUICK_COMPARE', title: 'Quick Compare', icon: 'search', editorLabel: 'Quick Compare' },
  { id: 'SHOP_SUMMARY', title: 'Shop Summary', icon: 'storefront', editorLabel: 'Shop Summary' },
  { id: 'PRODUCT_DETAILS', title: 'Product Details & analysis', icon: 'information-circle', editorLabel: 'Product Details' },
  { id: 'PRICE_MOVEMENT', title: 'Price Movement', icon: 'trending-up', editorLabel: 'Price Movement' },
  { id: 'ALERTS_BEHAVIOUR', title: 'Alerts & automatic checks', icon: 'notifications', editorLabel: 'Alerts' },
];

export function PersonalizationAccordionModal({
  visible,
  onClose,
}: {
  visible: boolean;
  onClose: () => void;
}) {
  const insets = useSafeAreaInsets();
  const {
    customization,
    themeMode,
    advancedModeEnabled,
    priceChangeNotificationsEnabled,
    colors,
    updateCustomization,
    setThemeMode,
    setAdvancedModeEnabled,
    setPriceChangeNotificationsEnabled,
    saveNamedSetup,
    applyNamedSetup,
    deleteNamedSetup,
    resetAll,
  } = useCustomization();

  const [expandedSection, setExpandedSection] = useState<AccordionSection | null>(null);
  const [previewTarget, setPreviewTarget] = useState<PreviewTarget>('LAUNCH_HUB');
  const [customPaletteOpen, setCustomPaletteOpen] = useState(false);
  const [saveSetupInput, setSaveSetupInput] = useState('');
  const [showSaveDialog, setShowSaveDialog] = useState(false);

  // Available height measurement for dynamic 58% vs 100% split
  const [bodyHeight, setBodyHeight] = useState(600);

  const editorOpen = expandedSection !== null;

  const onBodyLayout = (e: LayoutChangeEvent) => {
    const { height } = e.nativeEvent.layout;
    if (height > 0) {
      setBodyHeight(height);
    }
  };

  const currentSectionMeta = SECTIONS_META.find((s) => s.id === expandedSection);

  const getSummaryForSection = (sectionId: AccordionSection): string => {
    switch (sectionId) {
      case 'APPEARANCE': {
        const fontName = customization.fontStyle === 'SYSTEM' ? 'System' : customization.fontStyle.charAt(0) + customization.fontStyle.slice(1).toLowerCase();
        const textSizeName = customization.textSize === 'STANDARD' ? 'Standard' : customization.textSize === 'COMFORTABLE' ? 'Comfortable' : 'Large';
        return `${themeMode.charAt(0) + themeMode.slice(1).toLowerCase()} • ${
          customization.appColorPalette === 'SUPREME_HARMONY' ? 'Supreme Harmony' :
          customization.appColorPalette === 'OCEAN_COPPER' ? 'Ocean Copper' :
          customization.appColorPalette === 'ROYAL_AMETHYST' ? 'Royal Amethyst' : 'Custom'
        } • ${fontName} • ${textSizeName} • Individual tile icons`;
      }
      case 'QUICK_COMPARE':
        return `${customization.dashboardDefaultSort} • ${customization.dashboardPageSize} per page`;
      case 'SHOP_SUMMARY':
        return `${customization.insightCustomization.priorityProductLimit} • ${customization.insightCustomization.prioritySortMode} • ${customization.insightCustomization.priorityRowStyle}`;
      case 'PRODUCT_DETAILS':
        return `${customization.insightCustomization.advancedInfoLevel} • ${customization.insightCustomization.priceHistoryRange} • ${customization.insightCustomization.historyGraphStyle}`;
      case 'PRICE_MOVEMENT':
        return `${customization.priceMovementDefaultRange} • ${customization.insightCustomization.movementDefaultRetailer} • ${customization.insightCustomization.movementProductSort}`;
      case 'ALERTS_BEHAVIOUR':
        return `Daily checks ${customization.automaticPriceChecksEnabled ? 'on' : 'off'} • Alerts ${priceChangeNotificationsEnabled ? 'on' : 'off'}`;
    }
  };

  // Target-associated section
  const targetSection: AccordionSection =
    previewTarget === 'LAUNCH_HUB' ? 'SHOP_SUMMARY' :
    previewTarget === 'QUICK_COMPARE' ? 'QUICK_COMPARE' :
    previewTarget === 'PRODUCT_DETAILS' ? 'PRODUCT_DETAILS' :
    previewTarget === 'PRICE_MOVEMENT' ? 'PRICE_MOVEMENT' :
    'ALERTS_BEHAVIOUR';

  if (!visible) return null;

  return (
    <Modal
      visible={visible}
      animationType="fade"
      transparent
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        {/* Floating Rounded Dialog Frame (95% width, 97% height, 24dp radius) */}
        <View
          style={[
            styles.dialogSurface,
            {
              backgroundColor: colors.surface,
              borderColor: colors.border,
              marginTop: Math.max(insets.top + 8, 48),
              marginBottom: Math.max(insets.bottom + 8, 16),
            },
          ]}
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.headerLeft}>
              <MaterialIcons name="settings" size={21} color={colors.primary} />
              <Text style={[styles.headerTitle, { color: colors.text }]}>Personalization</Text>
            </View>

            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close personalization"
              onPress={onClose}
              style={styles.closeIconButton}
            >
              <Ionicons name="close" size={22} color={colors.text} />
            </Pressable>
          </View>

          {/* Body with dynamic vertical split */}
          <View style={styles.dialogBody} onLayout={onBodyLayout}>
            {/* Top Viewport: Live Preview */}
            <View
              style={[
                styles.previewContainer,
                {
                  height: editorOpen ? Math.round(bodyHeight * 0.58) : undefined,
                  flex: editorOpen ? undefined : 1,
                  justifyContent: 'center',
                  alignItems: 'center',
                },
              ]}
            >
              <AdaptivePersonalizationPreview
                customization={customization}
                colors={colors}
                target={previewTarget}
                editorOpen={editorOpen}
              />
            </View>

            {/* Bottom Viewport: CLOSED STATE (Pills + Target Selector) */}
            {!editorOpen && (
              <View style={styles.closedControlsContainer}>
                {/* Quick Editor Buttons (PersonalizationEditorZones) */}
                {previewTarget === 'LAUNCH_HUB' ? (
                  <View style={styles.editorZonesRow}>
                    <Pressable
                      onPress={() => setExpandedSection('APPEARANCE')}
                      style={[styles.editorZoneBtn, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}
                    >
                      <MaterialIcons name="palette" size={17} color={colors.primary} />
                      <Text style={[styles.editorZoneBtnText, { color: colors.text }]}>Appearance</Text>
                    </Pressable>

                    <Pressable
                      onPress={() => setExpandedSection('SHOP_SUMMARY')}
                      style={[styles.editorZoneBtn, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}
                    >
                      <MaterialIcons name="edit" size={17} color={colors.primary} />
                      <Text style={[styles.editorZoneBtnText, { color: colors.text }]}>Shop Summary</Text>
                    </Pressable>
                  </View>
                ) : (
                  <View style={styles.editorZonesRow}>
                    <Pressable
                      onPress={() => setExpandedSection(targetSection)}
                      style={[styles.editorZoneBtn, { backgroundColor: colors.surfaceRaised, borderColor: colors.border }]}
                    >
                      <MaterialIcons
                        name={
                          previewTarget === 'QUICK_COMPARE' ? 'search' :
                          previewTarget === 'PRODUCT_DETAILS' ? 'info' :
                          previewTarget === 'PRICE_MOVEMENT' ? 'show-chart' : 'notifications'
                        }
                        size={17}
                        color={colors.primary}
                      />
                      <Text style={[styles.editorZoneBtnText, { color: colors.text }]}>
                        {PREVIEW_TARGETS.find((p) => p.id === previewTarget)?.label || 'Edit Section'}
                      </Text>
                    </Pressable>
                  </View>
                )}

                {/* 5-Icon Target Navigation Bar (PersonalizationPreviewTargetSelector) */}
                <View style={styles.targetSelectorRow}>
                  {PREVIEW_TARGETS.map((item) => {
                    const isSelected = item.id === previewTarget;
                    return (
                      <Pressable
                        key={item.id}
                        accessibilityRole="tab"
                        accessibilityLabel={item.label}
                        testID={`target-tab-${item.id}`}
                        onPress={() => {
                          setPreviewTarget(item.id);
                          setExpandedSection(null);
                        }}
                        style={[
                          styles.targetIconBtn,
                          {
                            backgroundColor: isSelected ? withAlpha(colors.primary, '28') : colors.surfaceRaised,
                            borderColor: isSelected ? colors.primary : colors.border,
                          },
                        ]}
                      >
                        <MaterialIcons
                          name={
                            item.id === 'LAUNCH_HUB' ? 'home' :
                            item.id === 'QUICK_COMPARE' ? 'search' :
                            item.id === 'PRODUCT_DETAILS' ? 'info' :
                            item.id === 'PRICE_MOVEMENT' ? 'show-chart' : 'notifications'
                          }
                          size={20}
                          color={isSelected ? colors.primary : colors.textMuted}
                        />
                      </Pressable>
                    );
                  })}
                </View>
              </View>
            )}

            {/* Bottom Viewport: OPEN STATE (Dedicated Editor Sheet with Floating Circular Close Button) */}
            {editorOpen && currentSectionMeta && (
              <View style={[styles.openEditorContainer, { height: Math.round(bodyHeight * 0.42) }]}>
                {/* Floating circular close button (top-right of bottom panel) */}
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Close editor"
                  onPress={() => setExpandedSection(null)}
                  style={[
                    styles.floatingCloseBtn,
                    {
                      backgroundColor: colors.surfaceRaised,
                      borderColor: colors.border,
                    },
                  ]}
                >
                  <Ionicons name="close" size={20} color={colors.text} />
                </Pressable>

                <ScrollView
                  contentContainerStyle={styles.editorScrollContent}
                  showsVerticalScrollIndicator={false}
                >
                  {/* AccordionSectionCard */}
                  <View
                    style={[
                      styles.sectionCard,
                      {
                        backgroundColor: colors.surfaceRaised,
                        borderColor: withAlpha(colors.primary, '80'),
                      },
                    ]}
                  >
                    {/* Header Row */}
                    <View style={styles.sectionCardHeader}>
                      <View style={[styles.sectionIconBadge, { backgroundColor: withAlpha(colors.primary, '22') }]}>
                        <Ionicons name={currentSectionMeta.icon} size={20} color={colors.primary} />
                      </View>
                      <View style={styles.sectionHeaderCol}>
                        <Text style={[styles.sectionCardTitle, { color: colors.text }]}>
                          {currentSectionMeta.title}
                        </Text>
                        <Text style={[styles.sectionCardSummary, { color: colors.textMuted }]} numberOfLines={2}>
                          {getSummaryForSection(currentSectionMeta.id)}
                        </Text>
                      </View>
                    </View>

                    {/* Section Options Content */}
                    <View style={styles.sectionOptionsContent}>
                      {expandedSection === 'APPEARANCE' && (
                        <AppearanceEditor
                          themeMode={themeMode}
                          customization={customization}
                          colors={colors}
                          customPaletteOpen={customPaletteOpen}
                          saveSetupInput={saveSetupInput}
                          showSaveDialog={showSaveDialog}
                          onThemeChange={setThemeMode}
                          onCustomizationChange={updateCustomization}
                          onToggleCustomPalette={() => setCustomPaletteOpen(!customPaletteOpen)}
                          onSetSaveInput={setSaveSetupInput}
                          onToggleSaveDialog={() => setShowSaveDialog(!showSaveDialog)}
                          onSaveSetup={() => {
                            if (saveSetupInput.trim()) {
                              saveNamedSetup(saveSetupInput.trim());
                              setSaveSetupInput('');
                              setShowSaveDialog(false);
                            }
                          }}
                          onApplyNamedSetup={applyNamedSetup}
                          onDeleteNamedSetup={deleteNamedSetup}
                        />
                      )}

                      {expandedSection === 'QUICK_COMPARE' && (
                        <QuickCompareEditor
                          customization={customization}
                          colors={colors}
                          onChange={updateCustomization}
                        />
                      )}

                      {expandedSection === 'SHOP_SUMMARY' && (
                        <ShopSummaryEditor
                          customization={customization}
                          colors={colors}
                          onChange={updateCustomization}
                        />
                      )}

                      {expandedSection === 'PRODUCT_DETAILS' && (
                        <ProductDetailsEditor
                          customization={customization}
                          colors={colors}
                          advancedMode={advancedModeEnabled}
                          onAdvancedModeChange={setAdvancedModeEnabled}
                          onChange={updateCustomization}
                        />
                      )}

                      {expandedSection === 'PRICE_MOVEMENT' && (
                        <PriceMovementEditor
                          customization={customization}
                          colors={colors}
                          onChange={updateCustomization}
                        />
                      )}

                      {expandedSection === 'ALERTS_BEHAVIOUR' && (
                        <AlertsEditor
                          customization={customization}
                          colors={colors}
                          notificationsEnabled={priceChangeNotificationsEnabled}
                          onNotificationsChange={setPriceChangeNotificationsEnabled}
                          onChange={updateCustomization}
                        />
                      )}
                    </View>
                  </View>
                </ScrollView>
              </View>
            )}
          </View>

          {/* Docked Footer */}
          <View style={[styles.footer, { backgroundColor: colors.surface }]}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Reset all"
              onPress={resetAll}
              style={[styles.resetBtn, { borderColor: colors.border }]}
            >
              <MaterialIcons name="restart-alt" size={18} color={colors.text} />
              <Text style={[styles.resetBtnText, { color: colors.text }]}>Reset all</Text>
            </Pressable>

            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Done"
              onPress={onClose}
              style={[styles.doneBtn, { backgroundColor: colors.primary }]}
            >
              <Text style={styles.doneBtnText}>Done</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

// --------------------------------------------------------------------------
// 2-Column ChoiceGrid and ChoiceGroup
// --------------------------------------------------------------------------

function ChoiceGroup<T extends string | number>({
  title,
  options,
  selected,
  onSelect,
  labelMap,
  colors,
}: {
  title: string;
  options: readonly T[] | T[];
  selected: T;
  onSelect: (val: T) => void;
  labelMap?: Partial<Record<string | number, string>>;
  colors: any;
}) {
  // 2 columns chunking matching Compose
  const rows: T[][] = [];
  for (let i = 0; i < options.length; i += 2) {
    rows.push(options.slice(i, i + 2));
  }

  return (
    <View style={styles.choiceGroup}>
      <Text style={[styles.choiceGroupTitle, { color: colors.textMuted }]}>{title}</Text>
      <View style={styles.choiceGrid}>
        {rows.map((row, rIdx) => (
          <View key={rIdx} style={styles.choiceGridRow}>
            {row.map((opt) => {
              const isSelected = opt === selected;
              const displayLabel = labelMap?.[opt] || String(opt).replace(/_/g, ' ');
              return (
                <Pressable
                  key={String(opt)}
                  onPress={() => onSelect(opt)}
                  style={[
                    styles.choiceTile,
                    {
                      backgroundColor: isSelected ? withAlpha(colors.primary, '22') : colors.surfaceRaised,
                      borderColor: isSelected ? colors.primary : colors.border,
                      borderWidth: isSelected ? 1.5 : 1,
                    },
                  ]}
                >
                  <Text
                    style={[
                      styles.choiceTileText,
                      {
                        color: isSelected ? colors.primary : colors.textMuted,
                        fontFamily: isSelected ? type.bold : type.regular,
                      },
                    ]}
                    numberOfLines={1}
                  >
                    {displayLabel}
                  </Text>
                </Pressable>
              );
            })}
            {row.length === 1 && <View style={styles.choiceTilePlaceholder} />}
          </View>
        ))}
      </View>
    </View>
  );
}

function HexColorInput({
  value,
  role,
  colors,
  onCommit,
}: {
  value: string;
  role: string;
  colors: any;
  onCommit: (val: string) => void;
}) {
  const [localText, setLocalText] = useState(value);

  useEffect(() => {
    setLocalText(value);
  }, [value]);

  const handleChange = (text: string) => {
    let formatted = text.trim();
    if (!formatted.startsWith('#') && formatted.length > 0) {
      formatted = '#' + formatted;
    }
    setLocalText(formatted);
    const norm = normalizePaletteHex(formatted);
    if (norm) {
      onCommit(norm);
    }
  };

  const handleBlur = () => {
    const norm = normalizePaletteHex(localText);
    if (norm) {
      onCommit(norm);
    } else {
      setLocalText(value);
    }
  };

  return (
    <TextInput
      value={localText}
      onChangeText={handleChange}
      onBlur={handleBlur}
      style={[styles.customHexInput, { color: colors.text, borderColor: colors.border }]}
      placeholder="#10B981"
      placeholderTextColor={colors.textMuted}
      autoCapitalize="characters"
      autoCorrect={false}
    />
  );
}

// --------------------------------------------------------------------------
// Appearance Editor
// --------------------------------------------------------------------------

function AppearanceEditor({
  themeMode,
  customization,
  colors,
  customPaletteOpen,
  saveSetupInput,
  showSaveDialog,
  onThemeChange,
  onCustomizationChange,
  onToggleCustomPalette,
  onSetSaveInput,
  onToggleSaveDialog,
  onSaveSetup,
  onApplyNamedSetup,
  onDeleteNamedSetup,
}: any) {
  const insight = customization.insightCustomization;

  const palettes: { id: AppColorPalette; name: string; desc: string; bars: string[] }[] = [
    {
      id: 'SUPREME_HARMONY',
      name: 'Supreme Harmony',
      desc: 'Balanced emerald, gold and ocean tones',
      bars: ['#10B981', '#D6A63D', '#8B7CF6', '#38BDF8'],
    },
    {
      id: 'OCEAN_COPPER',
      name: 'Ocean Copper',
      desc: 'Teal, copper and ocean blue',
      bars: ['#38BDF8', '#E08A5B', '#2DD4BF', '#10B981'],
    },
    {
      id: 'ROYAL_AMETHYST',
      name: 'Royal Amethyst',
      desc: 'Purple, sapphire and gold accents',
      bars: ['#C084FC', '#60A5FA', '#8B7CF6', '#D6A63D'],
    },
    {
      id: 'CUSTOM',
      name: 'Custom',
      desc: 'User specified 5-color palette',
      bars: [
        customization.customColorPalette?.primaryHex || '#10B981',
        customization.customColorPalette?.secondaryHex || '#8B7CF6',
        customization.customColorPalette?.competitiveHex || '#34D399',
        customization.customColorPalette?.warningHex || '#F59E0B',
      ],
    },
  ];

  return (
    <View style={styles.editorSectionBody}>
      {/* Theme: 2 columns -> System, Light, Dark */}
      <ChoiceGroup<AppThemeMode>
        title="Theme"
        options={['SYSTEM', 'LIGHT', 'DARK']}
        selected={themeMode}
        onSelect={onThemeChange}
        labelMap={{ SYSTEM: 'System', LIGHT: 'Light', DARK: 'Dark' }}
        colors={colors}
      />

      {/* App colour palette: 2 columns with 4 bars each */}
      <View style={styles.choiceGroup}>
        <Text style={[styles.choiceGroupTitle, { color: colors.textMuted }]}>App colour palette</Text>
        <View style={styles.paletteGrid}>
          <View style={styles.paletteGridRow}>
            {palettes.slice(0, 2).map((p) => {
              const isSelected = customization.appColorPalette === p.id;
              return (
                <Pressable
                  key={p.id}
                  onPress={() => onCustomizationChange((c: any) => ({ ...c, appColorPalette: p.id }))}
                  style={[
                    styles.paletteTile,
                    {
                      backgroundColor: isSelected ? withAlpha(colors.primary, '22') : colors.surfaceRaised,
                      borderColor: isSelected ? colors.primary : colors.border,
                      borderWidth: isSelected ? 2 : 1,
                    },
                  ]}
                >
                  <View style={styles.paletteBarsRow}>
                    {p.bars.map((bar, i) => (
                      <View key={i} style={[styles.paletteBar, { backgroundColor: bar }]} />
                    ))}
                  </View>
                  <Text style={[styles.paletteTileTitle, { color: colors.text }]}>{p.name}</Text>
                  <Text style={[styles.paletteTileDesc, { color: colors.textMuted }]} numberOfLines={2}>
                    {p.desc}
                  </Text>
                </Pressable>
              );
            })}
          </View>
          <View style={styles.paletteGridRow}>
            {palettes.slice(2, 4).map((p) => {
              const isSelected = customization.appColorPalette === p.id;
              return (
                <Pressable
                  key={p.id}
                  onPress={() => onCustomizationChange((c: any) => ({ ...c, appColorPalette: p.id }))}
                  style={[
                    styles.paletteTile,
                    {
                      backgroundColor: isSelected ? withAlpha(colors.primary, '22') : colors.surfaceRaised,
                      borderColor: isSelected ? colors.primary : colors.border,
                      borderWidth: isSelected ? 2 : 1,
                    },
                  ]}
                >
                  <View style={styles.paletteBarsRow}>
                    {p.bars.map((bar, i) => (
                      <View key={i} style={[styles.paletteBar, { backgroundColor: bar }]} />
                    ))}
                  </View>
                  <Text style={[styles.paletteTileTitle, { color: colors.text }]}>{p.name}</Text>
                  <Text style={[styles.paletteTileDesc, { color: colors.textMuted }]} numberOfLines={2}>
                    {p.desc}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* Custom palette trigger */}
        {customization.appColorPalette === 'CUSTOM' && (
          <Pressable
            onPress={onToggleCustomPalette}
            style={[styles.editCustomPaletteBtn, { borderColor: colors.border }]}
          >
            <Ionicons name="create-outline" size={16} color={colors.primary} />
            <Text style={[styles.editCustomPaletteBtnText, { color: colors.primary }]}>
              {customPaletteOpen ? 'Hide custom colour editor' : 'Edit custom colours here'}
            </Text>
          </Pressable>
        )}

        {customPaletteOpen && (
          <View style={[styles.customColorEditorBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            {(['primaryHex', 'secondaryHex', 'competitiveHex', 'warningHex', 'reviewHex'] as const).map((role) => (
              <View key={role} style={styles.customColorEditorRow}>
                <View
                  style={[
                    styles.customColorThumb,
                    { backgroundColor: customization.customColorPalette?.[role] || colors.primary },
                  ]}
                />
                <Text style={[styles.customColorRoleLabel, { color: colors.text }]}>
                  {role.replace('Hex', '').charAt(0).toUpperCase() + role.replace('Hex', '').slice(1)}
                </Text>
                <HexColorInput
                  value={customization.customColorPalette?.[role] || ''}
                  role={role}
                  colors={colors}
                  onCommit={(norm) => {
                    onCustomizationChange((c: any) => ({
                      ...c,
                      customColorPalette: { ...c.customColorPalette, [role]: norm },
                    }));
                  }}
                />
              </View>
            ))}
          </View>
        )}
      </View>

      {/* Font style (2 columns) */}
      <ChoiceGroup<AppFontStyle>
        title="Font style"
        options={['SYSTEM', 'ROBOTO', 'INTER', 'OPEN_SANS', 'LATO', 'MONTSERRAT']}
        selected={customization.fontStyle}
        onSelect={(val) => onCustomizationChange((c: any) => ({ ...c, fontStyle: val }))}
        labelMap={{
          SYSTEM: 'System',
          ROBOTO: 'Roboto',
          INTER: 'Inter',
          OPEN_SANS: 'Open Sans',
          LATO: 'Lato',
          MONTSERRAT: 'Montserrat',
        }}
        colors={colors}
      />

      {/* Text size (2 columns) */}
      <ChoiceGroup<AppTextSize>
        title="Text size"
        options={['STANDARD', 'COMFORTABLE', 'LARGE']}
        selected={customization.textSize}
        onSelect={(val) => onCustomizationChange((c: any) => ({ ...c, textSize: val }))}
        labelMap={{
          STANDARD: 'Standard',
          COMFORTABLE: 'Comfortable',
          LARGE: 'Large',
        }}
        colors={colors}
      />

      {/* Contrast */}
      <ChoiceGroup<AppContrastMode>
        title="Contrast"
        options={['STANDARD', 'HIGH']}
        selected={insight.contrastMode}
        onSelect={(val) =>
          onCustomizationChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, contrastMode: val },
          }))
        }
        labelMap={{ STANDARD: 'Standard', HIGH: 'High contrast' }}
        colors={colors}
      />

      {/* Switches */}
      <View style={styles.switchRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.switchTitle, { color: colors.text }]}>Reduce transparency</Text>
          <Text style={[styles.switchSubtitle, { color: colors.textMuted }]}>
            Uses solid surfaces for clearer text and cards.
          </Text>
        </View>
        <Switch
          value={insight.reduceTransparency}
          onValueChange={(v) =>
            onCustomizationChange((c: any) => ({
              ...c,
              insightCustomization: { ...c.insightCustomization, reduceTransparency: v },
            }))
          }
          trackColor={{ false: colors.border, true: colors.primary }}
          thumbColor="#FFFFFF"
        />
      </View>

      <View style={styles.switchRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.switchTitle, { color: colors.text }]}>Haptic feedback</Text>
          <Text style={[styles.switchSubtitle, { color: colors.textMuted }]}>
            Feedback for barcode scans and action confirmations.
          </Text>
        </View>
        <Switch
          value={customization.hapticsEnabled}
          onValueChange={(v) => onCustomizationChange((c: any) => ({ ...c, hapticsEnabled: v }))}
          trackColor={{ false: colors.border, true: colors.primary }}
          thumbColor="#FFFFFF"
        />
      </View>

      {/* Section reset */}
      <Pressable
        onPress={() => {
          onThemeChange('DARK');
          onCustomizationChange((c: any) => ({
            ...c,
            appColorPalette: 'SUPREME_HARMONY',
            fontStyle: 'SYSTEM',
            textSize: 'STANDARD',
            insightCustomization: {
              ...c.insightCustomization,
              contrastMode: 'STANDARD',
              reduceTransparency: false,
            },
          }));
        }}
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>

      {/* Saved Setups Manager */}
      <View style={[styles.savedSetupsCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <View style={styles.savedSetupsHeaderRow}>
          <Text style={[styles.savedSetupsTitle, { color: colors.primary }]}>MY SAVED PRESETS</Text>
          <Pressable
            onPress={onToggleSaveDialog}
            style={[styles.saveNewBtn, { backgroundColor: withAlpha(colors.primary, '22'), borderColor: colors.primary }]}
          >
            <Ionicons name="add" size={13} color={colors.primary} />
            <Text style={[styles.saveNewBtnText, { color: colors.primary }]}>Save current</Text>
          </Pressable>
        </View>

        {showSaveDialog && (
          <View style={styles.saveInputWrap}>
            <TextInput
              value={saveSetupInput}
              onChangeText={onSetSaveInput}
              placeholder="Preset name..."
              placeholderTextColor={colors.textMuted}
              style={[styles.saveInput, { color: colors.text, borderColor: colors.border }]}
            />
            <Pressable onPress={onSaveSetup} style={[styles.confirmSaveBtn, { backgroundColor: colors.primary }]}>
              <Text style={styles.confirmSaveBtnText}>Save</Text>
            </Pressable>
          </View>
        )}

        {customization.savedPersonalizationPresets.length === 0 ? (
          <Text style={[styles.emptyPresetsText, { color: colors.textMuted }]}>
            No saved presets. Saved setups remain available after Reset All.
          </Text>
        ) : (
          customization.savedPersonalizationPresets.map((preset: any) => (
            <View key={preset.name} style={[styles.savedPresetRow, { borderColor: colors.border }]}>
              <View style={{ flex: 1 }}>
                <Text style={[styles.savedPresetName, { color: colors.text }]}>{preset.name}</Text>
                <Text style={[styles.savedPresetMeta, { color: colors.textMuted }]}>
                  {preset.themeMode} • {preset.customizationProfile?.slice(0, 16)}...
                </Text>
              </View>
              <Pressable
                onPress={() => onApplyNamedSetup(preset)}
                style={[styles.presetActionPill, { backgroundColor: withAlpha(colors.primary, '22') }]}
              >
                <Text style={[styles.presetActionPillText, { color: colors.primary }]}>Apply</Text>
              </Pressable>
              <Pressable
                onPress={() => onDeleteNamedSetup(preset.name)}
                style={[styles.presetActionPill, { backgroundColor: withAlpha(colors.danger, '22'), marginLeft: 6 }]}
              >
                <Text style={[styles.presetActionPillText, { color: colors.danger }]}>Delete</Text>
              </Pressable>
            </View>
          ))
        )}
      </View>
    </View>
  );
}

// --------------------------------------------------------------------------
// Quick Compare Editor
// --------------------------------------------------------------------------

function QuickCompareEditor({ customization, colors, onChange }: any) {
  return (
    <View style={styles.editorSectionBody}>
      <ChoiceGroup<DashboardDefaultSort>
        title="Default sorting"
        options={['MOST_VIEWED', 'BEST_SAVING', 'ALPHABETICAL', 'RECENT']}
        selected={customization.dashboardDefaultSort}
        onSelect={(val) => onChange((c: any) => ({ ...c, dashboardDefaultSort: val }))}
        labelMap={{
          MOST_VIEWED: 'Most viewed',
          BEST_SAVING: 'Best saving',
          ALPHABETICAL: 'Alphabetical',
          RECENT: 'Recent',
        }}
        colors={colors}
      />

      <ChoiceGroup<DashboardPageSize>
        title="Products per page"
        options={[10, 15, 20]}
        selected={customization.dashboardPageSize}
        onSelect={(val) => onChange((c: any) => ({ ...c, dashboardPageSize: val }))}
        labelMap={{
          10: '10 products',
          15: '15 products',
          20: '20 products',
        }}
        colors={colors}
      />

      <ChoiceGroup<PriceEmphasis>
        title="Shop price emphasis"
        options={['NORMAL', 'BOLD']}
        selected={customization.insightCustomization.priceEmphasis}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, priceEmphasis: val },
          }))
        }
        labelMap={{ NORMAL: 'Normal weight', BOLD: 'Bold & prominent' }}
        colors={colors}
      />

      <Pressable
        onPress={() =>
          onChange((c: any) => ({
            ...c,
            dashboardDefaultSort: 'MOST_VIEWED',
            dashboardPageSize: 15,
            insightCustomization: { ...c.insightCustomization, priceEmphasis: 'BOLD' },
          }))
        }
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>
    </View>
  );
}

// --------------------------------------------------------------------------
// Shop Summary Editor
// --------------------------------------------------------------------------

function ShopSummaryEditor({ customization, colors, onChange }: any) {
  const insight = customization.insightCustomization;

  return (
    <View style={styles.editorSectionBody}>
      <ChoiceGroup<SectionStartState>
        title="Top Priorities starts"
        options={['EXPANDED', 'COLLAPSED']}
        selected={insight.prioritiesStartState}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, prioritiesStartState: val },
          }))
        }
        labelMap={{ EXPANDED: 'Expanded', COLLAPSED: 'Collapsed' }}
        colors={colors}
      />

      <ChoiceGroup<PriorityProductLimit>
        title="Products shown"
        options={[3, 5, 10]}
        selected={insight.priorityProductLimit}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, priorityProductLimit: val },
          }))
        }
        labelMap={{ 3: '3 products', 5: '5 products', 10: '10 products' }}
        colors={colors}
      />

      <ChoiceGroup<PrioritySortMode>
        title="Priority ranking"
        options={['RUPEE_GAP', 'PERCENTAGE_GAP']}
        selected={insight.prioritySortMode}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, prioritySortMode: val },
          }))
        }
        labelMap={{
          RUPEE_GAP: 'Rupee gap',
          PERCENTAGE_GAP: 'Percentage gap',
        }}
        colors={colors}
      />

      <ChoiceGroup<PriorityRowStyle>
        title="Priority row style"
        options={['COMPACT', 'DETAILED']}
        selected={insight.priorityRowStyle}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, priorityRowStyle: val },
          }))
        }
        labelMap={{ COMPACT: 'Compact', DETAILED: 'Detailed' }}
        colors={colors}
      />

      <Pressable
        onPress={() =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: {
              ...c.insightCustomization,
              prioritiesStartState: 'COLLAPSED',
              priorityProductLimit: 5,
              prioritySortMode: 'RUPEE_GAP',
              priorityRowStyle: 'COMPACT',
            },
          }))
        }
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>
    </View>
  );
}

// --------------------------------------------------------------------------
// Product Details Editor
// --------------------------------------------------------------------------

function ProductDetailsEditor({ customization, colors, advancedMode, onAdvancedModeChange, onChange }: any) {
  const insight = customization.insightCustomization;

  return (
    <View style={styles.editorSectionBody}>
      <View style={styles.switchRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.switchTitle, { color: colors.text }]}>Advanced mode</Text>
          <Text style={[styles.switchSubtitle, { color: colors.textMuted }]}>
            Shows price history and deeper comparison information.
          </Text>
        </View>
        <Switch
          value={advancedMode}
          onValueChange={onAdvancedModeChange}
          trackColor={{ false: colors.border, true: colors.primary }}
          thumbColor="#FFFFFF"
        />
      </View>

      <ChoiceGroup<PriceHistoryRange>
        title="Price-history period"
        options={['SEVEN_DAYS', 'FOURTEEN_DAYS', 'THIRTY_DAYS']}
        selected={insight.priceHistoryRange}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, priceHistoryRange: val },
          }))
        }
        labelMap={{
          SEVEN_DAYS: '7 Days',
          FOURTEEN_DAYS: '14 Days',
          THIRTY_DAYS: '30 Days',
        }}
        colors={colors}
      />

      <ChoiceGroup<HistoryGraphStyle>
        title="Graph appearance"
        options={['AREA', 'LINE', 'STEP']}
        selected={insight.historyGraphStyle}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, historyGraphStyle: val },
          }))
        }
        labelMap={{ AREA: 'Area fill', LINE: 'Line only', STEP: 'Step-line' }}
        colors={colors}
      />

      <ChoiceGroup<AdvancedInfoLevel>
        title="Information level"
        options={['ESSENTIAL', 'STANDARD', 'FULL']}
        selected={insight.advancedInfoLevel}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, advancedInfoLevel: val },
          }))
        }
        labelMap={{ ESSENTIAL: 'Essential', STANDARD: 'Standard', FULL: 'Full expert' }}
        colors={colors}
      />

      <Pressable
        onPress={() => {
          onAdvancedModeChange(true);
          onChange((c: any) => ({
            ...c,
            insightCustomization: {
              ...c.insightCustomization,
              priceHistoryRange: 'THIRTY_DAYS',
              historyGraphStyle: 'AREA',
              advancedInfoLevel: 'STANDARD',
            },
          }));
        }}
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>
    </View>
  );
}

// --------------------------------------------------------------------------
// Price Movement Editor
// --------------------------------------------------------------------------

function PriceMovementEditor({ customization, colors, onChange }: any) {
  const insight = customization.insightCustomization;

  return (
    <View style={styles.editorSectionBody}>
      <ChoiceGroup<PriceMovementDefaultRange>
        title="Default period"
        options={['SEVEN_DAYS', 'FOURTEEN_DAYS', 'THIRTY_DAYS']}
        selected={customization.priceMovementDefaultRange}
        onSelect={(val) => onChange((c: any) => ({ ...c, priceMovementDefaultRange: val }))}
        labelMap={{
          SEVEN_DAYS: '7 Days',
          FOURTEEN_DAYS: '14 Days',
          THIRTY_DAYS: '30 Days',
        }}
        colors={colors}
      />

      <ChoiceGroup<MovementDefaultRetailer>
        title="Default retailer"
        options={['ALL', 'AMAZON', 'FLIPKART']}
        selected={insight.movementDefaultRetailer}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, movementDefaultRetailer: val },
          }))
        }
        labelMap={{ ALL: 'All retailers', AMAZON: 'Amazon only', FLIPKART: 'Flipkart only' }}
        colors={colors}
      />

      <ChoiceGroup<MovementProductSort>
        title="Changed-product sorting"
        options={['LATEST_CHANGE', 'RUPEE_CHANGE', 'PERCENTAGE_CHANGE', 'PRODUCT_NAME']}
        selected={insight.movementProductSort}
        onSelect={(val) =>
          onChange((c: any) => ({
            ...c,
            insightCustomization: { ...c.insightCustomization, movementProductSort: val },
          }))
        }
        labelMap={{
          LATEST_CHANGE: 'Latest change',
          RUPEE_CHANGE: 'Rupee change',
          PERCENTAGE_CHANGE: 'Percentage change',
          PRODUCT_NAME: 'Product name',
        }}
        colors={colors}
      />

      <Pressable
        onPress={() =>
          onChange((c: any) => ({
            ...c,
            priceMovementDefaultRange: 'THIRTY_DAYS',
            insightCustomization: {
              ...c.insightCustomization,
              movementDefaultRetailer: 'ALL',
              movementProductSort: 'LATEST_CHANGE',
            },
          }))
        }
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>
    </View>
  );
}

// --------------------------------------------------------------------------
// Alerts Editor
// --------------------------------------------------------------------------

function AlertsEditor({ customization, colors, notificationsEnabled, onNotificationsChange, onChange }: any) {
  return (
    <View style={styles.editorSectionBody}>
      <View style={styles.switchRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.switchTitle, { color: colors.text }]}>Automatic daily price checks</Text>
          <Text style={[styles.switchSubtitle, { color: colors.textMuted }]}>
            Checks each linked product once per day to keep rolling history fresh.
          </Text>
        </View>
        <Switch
          value={customization.automaticPriceChecksEnabled}
          onValueChange={(v) => onChange((c: any) => ({ ...c, automaticPriceChecksEnabled: v }))}
          trackColor={{ false: colors.border, true: colors.primary }}
          thumbColor="#FFFFFF"
        />
      </View>

      <View style={styles.switchRow}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.switchTitle, { color: colors.text }]}>Price change alerts</Text>
          <Text style={[styles.switchSubtitle, { color: colors.textMuted }]}>
            Notify only when price changes meet the selected threshold.
          </Text>
        </View>
        <Switch
          value={notificationsEnabled}
          onValueChange={onNotificationsChange}
          trackColor={{ false: colors.border, true: colors.primary }}
          thumbColor="#FFFFFF"
        />
      </View>

      {notificationsEnabled && (
        <>
          <ChoiceGroup<PriceAlertDirection>
            title="Alert direction"
            options={['BOTH', 'DECREASES_ONLY', 'INCREASES_ONLY']}
            selected={customization.priceAlertDirection}
            onSelect={(val) => onChange((c: any) => ({ ...c, priceAlertDirection: val }))}
            labelMap={{
              BOTH: 'Any change',
              DECREASES_ONLY: 'Price drops only',
              INCREASES_ONLY: 'Increases only',
            }}
            colors={colors}
          />

          <ChoiceGroup<PriceAlertThreshold>
            title="Minimum change"
            options={['ANY', 'RUPEES_50', 'PERCENT_2', 'PERCENT_5']}
            selected={customization.priceAlertThreshold}
            onSelect={(val) => onChange((c: any) => ({ ...c, priceAlertThreshold: val }))}
            labelMap={{
              ANY: 'Any change (₹1+)',
              RUPEES_50: '₹50 change',
              PERCENT_2: '2% change',
              PERCENT_5: '5% change',
            }}
            colors={colors}
          />
        </>
      )}

      <Pressable
        onPress={() => {
          onNotificationsChange(false);
          onChange((c: any) => ({
            ...c,
            automaticPriceChecksEnabled: true,
            priceAlertDirection: 'DECREASES_ONLY',
            priceAlertThreshold: 'ANY',
          }));
        }}
        style={[styles.sectionResetBtn, { borderColor: colors.border }]}
      >
        <Ionicons name="refresh" size={15} color={colors.textMuted} />
        <Text style={[styles.sectionResetBtnText, { color: colors.text }]}>Reset this section</Text>
      </Pressable>
    </View>
  );
}

// --------------------------------------------------------------------------
// Stylesheet matching Jetpack Compose
// --------------------------------------------------------------------------

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dialogSurface: {
    width: '95%',
    flex: 1,
    maxHeight: '97%',
    borderRadius: 24,
    borderWidth: 1,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 16,
  },
  header: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingLeft: 16,
    paddingRight: 8,
    borderBottomWidth: 0,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
  },
  headerTitle: {
    fontSize: 16,
    fontFamily: type.bold,
  },
  closeIconButton: {
    padding: 6,
    borderRadius: radius.pill,
  },
  dialogBody: {
    flex: 1,
  },
  previewContainer: {
    width: '100%',
    alignSelf: 'stretch',
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  closedControlsContainer: {
    paddingHorizontal: 14,
    paddingBottom: 10,
    gap: 7,
  },
  editorZonesRow: {
    flexDirection: 'row',
    gap: 7,
  },
  editorZoneBtn: {
    flex: 1,
    height: 42,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 14,
    borderWidth: 1,
    gap: 7,
    paddingHorizontal: 10,
  },
  editorZoneBtnText: {
    fontSize: 10.5,
    fontFamily: type.bold,
  },
  targetSelectorRow: {
    flexDirection: 'row',
    gap: 7,
  },
  targetIconBtn: {
    flex: 1,
    height: 42,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 14,
    borderWidth: 1,
  },
  openEditorContainer: {
    width: '100%',
    position: 'relative',
  },
  floatingCloseBtn: {
    position: 'absolute',
    top: 8,
    right: 14,
    width: 42,
    height: 42,
    borderRadius: 21,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 4,
  },
  editorScrollContent: {
    paddingHorizontal: 14,
    paddingTop: 8,
    paddingBottom: 16,
  },
  sectionCard: {
    borderRadius: 18,
    borderWidth: 1,
    padding: 14,
    gap: 14,
  },
  sectionCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingRight: 46, // Leave room for floating circular close button
    gap: 11,
  },
  sectionIconBadge: {
    width: 38,
    height: 38,
    borderRadius: 19,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sectionHeaderCol: {
    flex: 1,
  },
  sectionCardTitle: {
    fontSize: 14,
    fontFamily: type.bold,
  },
  sectionCardSummary: {
    fontSize: 10,
    marginTop: 2,
    lineHeight: 13,
  },
  sectionOptionsContent: {
    gap: 14,
  },
  editorSectionBody: {
    gap: 14,
  },
  choiceGroup: {
    gap: 7,
  },
  choiceGroupTitle: {
    fontSize: 11,
    fontFamily: type.bold,
  },
  choiceGrid: {
    gap: 7,
  },
  choiceGridRow: {
    flexDirection: 'row',
    gap: 7,
  },
  choiceTile: {
    flex: 1,
    minHeight: 48,
    borderRadius: 12,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  choiceTilePlaceholder: {
    flex: 1,
  },
  choiceTileText: {
    fontSize: 11,
    textAlign: 'center',
  },
  paletteGrid: {
    gap: 8,
  },
  paletteGridRow: {
    flexDirection: 'row',
    gap: 8,
  },
  paletteTile: {
    flex: 1,
    minHeight: 94,
    borderRadius: 14,
    padding: 11,
    gap: 6,
    justifyContent: 'center',
  },
  paletteBarsRow: {
    flexDirection: 'row',
    height: 9,
    gap: 3,
    marginBottom: 2,
  },
  paletteBar: {
    flex: 1,
    height: 9,
    borderRadius: 2,
  },
  paletteTileTitle: {
    fontSize: 11,
    fontFamily: type.bold,
  },
  paletteTileDesc: {
    fontSize: 9,
    lineHeight: 12,
  },
  editCustomPaletteBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 38,
    borderRadius: 12,
    borderWidth: 1,
    gap: 7,
    marginTop: 4,
  },
  editCustomPaletteBtnText: {
    fontSize: 11,
    fontFamily: type.bold,
  },
  customColorEditorBox: {
    borderRadius: 14,
    borderWidth: 1,
    padding: 10,
    gap: 8,
    marginTop: 4,
  },
  customColorEditorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  customColorThumb: {
    width: 22,
    height: 22,
    borderRadius: 11,
  },
  customColorRoleLabel: {
    flex: 1,
    fontSize: 11,
    fontFamily: type.bold,
  },
  customHexInput: {
    width: 80,
    height: 30,
    borderRadius: 6,
    borderWidth: 1,
    textAlign: 'center',
    fontSize: 11,
    fontFamily: type.bold,
    paddingHorizontal: 4,
  },
  switchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    minHeight: 48,
    paddingVertical: 2,
  },
  switchTitle: {
    fontSize: 12.5,
    fontFamily: type.bold,
  },
  switchSubtitle: {
    fontSize: 9.5,
    marginTop: 2,
  },
  sectionResetBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 40,
    borderRadius: 12,
    borderWidth: 1,
    gap: 6,
    marginTop: 4,
  },
  sectionResetBtnText: {
    fontSize: 11.5,
    fontFamily: type.bold,
  },
  savedSetupsCard: {
    borderRadius: 14,
    borderWidth: 1,
    padding: 12,
    gap: 10,
    marginTop: 4,
  },
  savedSetupsHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  savedSetupsTitle: {
    fontSize: 10,
    fontFamily: type.bold,
    letterSpacing: 0.8,
  },
  saveNewBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: radius.pill,
    borderWidth: 1,
    gap: 4,
  },
  saveNewBtnText: {
    fontSize: 10,
    fontFamily: type.bold,
  },
  saveInputWrap: {
    flexDirection: 'row',
    gap: 8,
  },
  saveInput: {
    flex: 1,
    height: 36,
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    fontSize: 12,
  },
  confirmSaveBtn: {
    paddingHorizontal: 14,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmSaveBtnText: {
    color: '#0B0F14',
    fontSize: 12,
    fontFamily: type.bold,
  },
  emptyPresetsText: {
    fontSize: 10,
    fontStyle: 'italic',
  },
  savedPresetRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
    borderBottomWidth: 1,
  },
  savedPresetName: {
    fontSize: 12,
    fontFamily: type.bold,
  },
  savedPresetMeta: {
    fontSize: 9,
    marginTop: 1,
  },
  presetActionPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: radius.pill,
  },
  presetActionPillText: {
    fontSize: 10,
    fontFamily: type.bold,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderTopWidth: 0,
    gap: 10,
  },
  resetBtn: {
    flex: 1,
    height: 44,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    borderWidth: 1,
    gap: 6,
  },
  resetBtnText: {
    fontSize: 13,
    fontFamily: type.bold,
  },
  doneBtn: {
    flex: 1,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
  },
  doneBtnText: {
    color: '#0B0F14',
    fontSize: 14,
    fontFamily: type.bold,
  },
});
