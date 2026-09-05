import type { ComponentProps } from 'react';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';

import type {
  LaunchTileIconPreferences,
  LaunchTileIconStyle,
} from '../../domain/customization';

export type MaterialIconName = ComponentProps<typeof MaterialIcons>['name'];

export interface TileIconSet {
  insights: MaterialIconName;
  inventory: MaterialIconName;
  priceMovement: MaterialIconName;
  quickCompare: MaterialIconName;
}

export function getTileIconSetForStyle(style: LaunchTileIconStyle): TileIconSet {
  switch (style) {
    case 'CLEAN':
      return {
        insights: 'analytics',
        inventory: 'inventory-2',
        priceMovement: 'query-stats',
        quickCompare: 'manage-search',
      };
    case 'CLASSIC':
      return {
        insights: 'dashboard',
        inventory: 'inventory',
        priceMovement: 'show-chart',
        quickCompare: 'search',
      };
    case 'BUSINESS':
      return {
        insights: 'assessment',
        inventory: 'warehouse',
        priceMovement: 'timeline',
        quickCompare: 'compare-arrows',
      };
    case 'PRODUCT':
      return {
        insights: 'lightbulb',
        inventory: 'category',
        priceMovement: 'trending-up',
        quickCompare: 'travel-explore',
      };
    case 'DATA':
      return {
        insights: 'insights',
        inventory: 'all-inbox',
        priceMovement: 'stacked-line-chart',
        quickCompare: 'price-check',
      };
    default:
      return {
        insights: 'analytics',
        inventory: 'inventory-2',
        priceMovement: 'query-stats',
        quickCompare: 'manage-search',
      };
  }
}

export function resolveLaunchTileIcons(
  preferences: LaunchTileIconPreferences,
): TileIconSet {
  return {
    insights: getTileIconSetForStyle(preferences.insights).insights,
    inventory: getTileIconSetForStyle(preferences.inventory).inventory,
    priceMovement: getTileIconSetForStyle(preferences.priceMovement).priceMovement,
    quickCompare: getTileIconSetForStyle(preferences.quickCompare).quickCompare,
  };
}
