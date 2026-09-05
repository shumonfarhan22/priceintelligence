import { Platform } from 'react-native';

const transparentImageCache = new Map<string, string>();
const pendingPromises = new Map<string, Promise<string>>();

/**
 * Fast sub-10ms boundary flood-fill algorithm that detects exterior white
 * backgrounds and soft studio contact shadows from the perimeter inwards,
 * converting them into transparent alpha / soft dark shadows.
 *
 * All interior whites (brand logos, dials, buttons, metallic specular highlights)
 * remain 100% opaque.
 */
export function processImagePixels(data: Uint8ClampedArray, w: number, h: number): boolean {
  if (w <= 0 || h <= 0 || data.length < w * h * 4) return false;

  // 1. Check if the image perimeter is predominantly white / near-white.
  // If corners and borders are colored (e.g. lifestyle photo or non-white backdrop),
  // do not touch the image.
  let whiteBorderCount = 0;
  let totalBorderCount = 0;
  const step = Math.max(1, Math.floor(Math.min(w, h) / 80));

  for (let x = 0; x < w; x += step) {
    for (const y of [0, h - 1]) {
      const idx = (y * w + x) * 4;
      totalBorderCount++;
      const r = data[idx];
      const g = data[idx + 1];
      const b = data[idx + 2];
      const lum = (r + g + b) / 3;
      const diff = Math.max(r, g, b) - Math.min(r, g, b);
      if (lum >= 232 && diff < 20) {
        whiteBorderCount++;
      }
    }
  }

  for (let y = 0; y < h; y += step) {
    for (const x of [0, w - 1]) {
      const idx = (y * w + x) * 4;
      totalBorderCount++;
      const r = data[idx];
      const g = data[idx + 1];
      const b = data[idx + 2];
      const lum = (r + g + b) / 3;
      const diff = Math.max(r, g, b) - Math.min(r, g, b);
      if (lum >= 232 && diff < 20) {
        whiteBorderCount++;
      }
    }
  }

  // If less than 50% of the perimeter is white/near-white, this is not an e-commerce white-backdrop image.
  if (totalBorderCount > 0 && whiteBorderCount / totalBorderCount < 0.5) {
    return false;
  }

  // 2. Identify exterior background vs product
  const isBackground = (idx: number): boolean => {
    const r = data[idx];
    const g = data[idx + 1];
    const b = data[idx + 2];
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const diff = max - min;
    const lum = (r + g + b) / 3;

    // Clear white / near-white backdrop
    if (lum >= 235 && diff < 22) return true;
    // Light studio contact shadow (neutral grey connected to backdrop)
    if (lum >= 180 && diff < 12) return true;
    // Medium studio contact shadow
    if (lum >= 145 && diff < 8) return true;

    return false;
  };

  const visited = new Uint8Array(w * h);
  const queue = new Int32Array(w * h);
  let head = 0;
  let tail = 0;

  // Enqueue perimeter borders
  for (let x = 0; x < w; x++) {
    let p = 0 * w + x;
    if (isBackground(p * 4)) { visited[p] = 1; queue[tail++] = p; }
    p = (h - 1) * w + x;
    if (isBackground(p * 4)) { visited[p] = 1; queue[tail++] = p; }
  }
  for (let y = 0; y < h; y++) {
    let p = y * w + 0;
    if (!visited[p] && isBackground(p * 4)) { visited[p] = 1; queue[tail++] = p; }
    p = y * w + (w - 1);
    if (!visited[p] && isBackground(p * 4)) { visited[p] = 1; queue[tail++] = p; }
  }

  // Flood fill connected exterior background
  while (head < tail) {
    const curr = queue[head++];
    const cx = curr % w;
    const cy = Math.floor(curr / w);

    const idx = curr * 4;
    const r = data[idx];
    const g = data[idx + 1];
    const b = data[idx + 2];
    const lum = (r + g + b) / 3;

    if (lum > 228) {
      data[idx + 3] = 0; // Completely transparent
    } else {
      // Soft studio drop shadow: convert white-grey shadow into natural dark alpha shadow
      const shadowDarkness = Math.min(255, Math.round((228 - lum) * 1.5));
      data[idx] = 0;
      data[idx + 1] = 0;
      data[idx + 2] = 0;
      data[idx + 3] = shadowDarkness;
    }

    if (cy > 0) {
      const n = curr - w;
      if (!visited[n] && isBackground(n * 4)) { visited[n] = 1; queue[tail++] = n; }
    }
    if (cy < h - 1) {
      const n = curr + w;
      if (!visited[n] && isBackground(n * 4)) { visited[n] = 1; queue[tail++] = n; }
    }
    if (cx > 0) {
      const n = curr - 1;
      if (!visited[n] && isBackground(n * 4)) { visited[n] = 1; queue[tail++] = n; }
    }
    if (cx < w - 1) {
      const n = curr + 1;
      if (!visited[n] && isBackground(n * 4)) { visited[n] = 1; queue[tail++] = n; }
    }
  }

  // 3. Boundary anti-aliasing feathering
  for (let y = 1; y < h - 1; y++) {
    for (let x = 1; x < w - 1; x++) {
      const p = y * w + x;
      const idx = p * 4;
      if (data[idx + 3] > 0 && !visited[p]) {
        const hasTransparentNeighbor =
          visited[p - 1] || visited[p + 1] || visited[p - w] || visited[p + w];

        if (hasTransparentNeighbor) {
          const lum = (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
          const diff = Math.max(data[idx], data[idx + 1], data[idx + 2]) - Math.min(data[idx], data[idx + 1], data[idx + 2]);
          if (lum > 218 && diff < 22) {
            const alphaFactor = Math.max(0.08, (255 - lum) / 37);
            data[idx + 3] = Math.round(data[idx + 3] * alphaFactor);
          }
        }
      }
    }
  }

  return true;
}

/**
 * Returns a transparent version of the product image if it contains an exterior white background.
 * Uses memoized caching so images are processed only once.
 */
export async function getTransparentImageUri(uri: string | null | undefined): Promise<string | null> {
  if (!uri || typeof uri !== 'string') return null;
  const trimmed = uri.trim();
  if (!trimmed) return null;

  if (transparentImageCache.has(trimmed)) {
    return transparentImageCache.get(trimmed)!;
  }

  // If already a data URI or local transparent file, return directly
  if (trimmed.startsWith('data:image/png')) {
    return trimmed;
  }

  // If already running processing for this URI, reuse existing promise
  if (pendingPromises.has(trimmed)) {
    return pendingPromises.get(trimmed)!;
  }

  const task = (async () => {
    // In Web environment or browsers where Canvas is natively available
    if (Platform.OS === 'web' && typeof document !== 'undefined') {
      try {
        const result = await processImageViaCanvas(trimmed);
        transparentImageCache.set(trimmed, result);
        return result;
      } catch {
        transparentImageCache.set(trimmed, trimmed);
        return trimmed;
      }
    }

    // On native mobile platforms, remote images will render with contain
    transparentImageCache.set(trimmed, trimmed);
    return trimmed;
  })();

  pendingPromises.set(trimmed, task);
  try {
    const result = await task;
    return result;
  } finally {
    pendingPromises.delete(trimmed);
  }
}

function processImageViaCanvas(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';

    img.onload = () => {
      try {
        const w = img.naturalWidth || 500;
        const h = img.naturalHeight || 500;
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(url);
          return;
        }

        ctx.drawImage(img, 0, 0, w, h);
        const imgData = ctx.getImageData(0, 0, w, h);
        const modified = processImagePixels(imgData.data, w, h);

        if (!modified) {
          resolve(url);
          return;
        }

        ctx.putImageData(imgData, 0, 0);
        const transparentDataUrl = canvas.toDataURL('image/png');
        resolve(transparentDataUrl);
      } catch (e) {
        reject(e);
      }
    };

    img.onerror = () => {
      resolve(url);
    };

    img.src = url;
  });
}

export function clearTransparentImageCache(): void {
  transparentImageCache.clear();
  pendingPromises.clear();
}
