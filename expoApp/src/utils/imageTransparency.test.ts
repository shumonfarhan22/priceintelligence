import { describe, expect, it, vi } from 'vitest';

vi.mock('react-native', () => ({
  Platform: { OS: 'web' },
}));

import { processImagePixels } from './imageTransparency';

describe('imageTransparency', () => {
  it('detects white perimeter and flood-fills exterior to transparent', () => {
    const w = 100;
    const h = 100;
    const buffer = new Uint8ClampedArray(w * h * 4);

    // Fill entirely with white
    for (let i = 0; i < buffer.length; i += 4) {
      buffer[i] = 255;
      buffer[i + 1] = 255;
      buffer[i + 2] = 255;
      buffer[i + 3] = 255;
    }

    // Draw non-white product in center (x: 25..75, y: 25..75)
    for (let y = 25; y < 75; y++) {
      for (let x = 25; x < 75; x++) {
        const idx = (y * w + x) * 4;
        buffer[idx] = 100; // steel grey
        buffer[idx + 1] = 100;
        buffer[idx + 2] = 100;
        buffer[idx + 3] = 255;
      }
    }

    // Inside the product, draw an internal white brand logo (x: 40..50, y: 40..50)
    for (let y = 40; y < 50; y++) {
      for (let x = 40; x < 50; x++) {
        const idx = (y * w + x) * 4;
        buffer[idx] = 255;
        buffer[idx + 1] = 255;
        buffer[idx + 2] = 255;
        buffer[idx + 3] = 255;
      }
    }

    const modified = processImagePixels(buffer, w, h);
    expect(modified).toBe(true);

    // Exterior corner should now be transparent (alpha = 0)
    const cornerAlpha = buffer[3];
    expect(cornerAlpha).toBe(0);

    // Product center should still be opaque (alpha = 255)
    const productCenterIdx = (30 * w + 30) * 4;
    expect(buffer[productCenterIdx + 3]).toBe(255);

    // Internal white brand logo must NOT be erased (alpha must remain 255)
    const logoIdx = (45 * w + 45) * 4;
    expect(buffer[logoIdx + 3]).toBe(255);
    expect(buffer[logoIdx]).toBe(255);
  });

  it('leaves lifestyle / non-white background photos untouched', () => {
    const w = 50;
    const h = 50;
    const buffer = new Uint8ClampedArray(w * h * 4);

    // Fill with blue/dark backdrop
    for (let i = 0; i < buffer.length; i += 4) {
      buffer[i] = 20;
      buffer[i + 1] = 40;
      buffer[i + 2] = 80;
      buffer[i + 3] = 255;
    }

    const modified = processImagePixels(buffer, w, h);
    expect(modified).toBe(false);
    expect(buffer[3]).toBe(255);
  });
});
