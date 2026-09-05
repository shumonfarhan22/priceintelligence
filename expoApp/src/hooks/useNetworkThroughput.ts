import { useEffect, useState } from 'react';

export function formatNetworkSpeed(bytesPerSecond: number): string {
  if (bytesPerSecond >= 1024 * 1024) {
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(1)} MB/s`;
  }
  if (bytesPerSecond >= 1024) {
    return `${Math.round(bytesPerSecond / 1024)} KB/s`;
  }
  return `${Math.max(0, Math.round(bytesPerSecond))} B/s`;
}

type ThroughputListener = (bytesPerSec: number) => void;
const listeners = new Set<ThroughputListener>();
let currentSpeedBytesPerSec = 0;

export function reportThroughputBytes(bytes: number, elapsedMs: number) {
  if (elapsedMs <= 0) return;
  const speed = (bytes / elapsedMs) * 1000;
  currentSpeedBytesPerSec = speed;
  listeners.forEach((listener) => listener(speed));
}

export function resetThroughput() {
  currentSpeedBytesPerSec = 0;
  listeners.forEach((listener) => listener(0));
}

export function useNetworkThroughput(isActive: boolean): string | null {
  const [speedText, setSpeedText] = useState<string | null>(null);

  useEffect(() => {
    if (!isActive) {
      setSpeedText(null);
      return;
    }

    setSpeedText(currentSpeedBytesPerSec > 0 ? formatNetworkSpeed(currentSpeedBytesPerSec) : '0 KB/s');

    const listener: ThroughputListener = (speed) => {
      setSpeedText(formatNetworkSpeed(speed));
    };

    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }, [isActive]);

  return speedText;
}
