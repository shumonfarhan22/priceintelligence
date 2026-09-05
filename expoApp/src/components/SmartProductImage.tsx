import React from 'react';
import { StyleProp, View, Text, StyleSheet } from 'react-native';
import { Image, ImageStyle, ImageContentFit } from 'expo-image';

export interface SmartProductImageProps {
  uri: string | null | undefined;
  style?: StyleProp<ImageStyle>;
  contentFit?: ImageContentFit;
  transition?: number;
  recyclingKey?: string;
  accessibilityLabel?: string;
  fallbackLetter?: string;
  placeholderStyle?: StyleProp<any>;
  fallbackLetterStyle?: StyleProp<any>;
}

export const SmartProductImage = React.memo(function SmartProductImage({
  uri,
  style,
  contentFit = 'contain',
  transition = 120,
  recyclingKey,
  accessibilityLabel,
  fallbackLetter,
  placeholderStyle,
  fallbackLetterStyle,
}: SmartProductImageProps) {
  if (!uri) {
    if (fallbackLetter) {
      return (
        <View style={[styles.placeholder, placeholderStyle]}>
          <Text style={[styles.fallbackLetter, fallbackLetterStyle]}>
            {fallbackLetter.toUpperCase()}
          </Text>
        </View>
      );
    }
    return null;
  }

  return (
    <Image
      source={{ uri }}
      style={style}
      contentFit={contentFit}
      transition={transition}
      recyclingKey={recyclingKey ?? uri}
      accessibilityLabel={accessibilityLabel}
      cachePolicy="memory-disk"
    />
  );
});

const styles = StyleSheet.create({
  placeholder: {
    width: '100%',
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
  },
  fallbackLetter: {
    fontSize: 28,
    fontWeight: '800',
    color: '#94A3B8',
  },
});
