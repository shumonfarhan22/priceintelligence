export function formatRupees(value: number): string {
  return `₹${value.toLocaleString('en-IN', {
    minimumFractionDigits: Number.isInteger(value) ? 0 : 2,
    maximumFractionDigits: 2,
  })}`;
}

export function formatRelativeTime(timestamp: number | null, now = Date.now()): string {
  if (timestamp == null || !Number.isFinite(timestamp) || timestamp <= 0) return 'time unknown';
  const elapsed = Math.max(0, now - timestamp);
  const minutes = Math.floor(elapsed / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} ${days === 1 ? 'day' : 'days'} ago`;
  return new Date(timestamp).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}
