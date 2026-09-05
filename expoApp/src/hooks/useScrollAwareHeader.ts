import { useState, useRef, useCallback } from 'react';
import { LayoutAnimation, NativeSyntheticEvent, NativeScrollEvent } from 'react-native';

const HIDE_THRESHOLD = 30;
const SHOW_THRESHOLD = 30;

export function useScrollAwareHeader() {
  const [visible, setVisible] = useState(true);
  const lastOffsetY = useRef(0);
  const downwardTravel = useRef(0);
  const upwardTravel = useRef(0);

  const onScroll = useCallback((event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const currentOffset = event.nativeEvent.contentOffset.y;
    
    // Always show header at the very top (and handle bounce gracefully)
    if (currentOffset <= 0) {
      if (!visible) {
        LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
        setVisible(true);
      }
      downwardTravel.current = 0;
      upwardTravel.current = 0;
      lastOffsetY.current = currentOffset;
      return;
    }

    const delta = currentOffset - lastOffsetY.current;
    
    if (delta > 0) {
      // Scrolling down
      downwardTravel.current += delta;
      upwardTravel.current = 0;
      
      if (downwardTravel.current > HIDE_THRESHOLD && visible) {
        LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
        setVisible(false);
        downwardTravel.current = 0;
      }
    } else if (delta < 0) {
      // Scrolling up
      upwardTravel.current += Math.abs(delta);
      downwardTravel.current = 0;
      
      if (upwardTravel.current > SHOW_THRESHOLD && !visible) {
        LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
        setVisible(true);
        upwardTravel.current = 0;
      }
    }
    
    lastOffsetY.current = currentOffset;
  }, [visible]);

  return { headerVisible: visible, onScroll };
}
