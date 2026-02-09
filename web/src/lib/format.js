/**
 * Format milliseconds to a human-readable string like "2h 15m" or "45m 30s".
 */
export function formatMs(ms) {
  if (!ms || ms <= 0) return '0s';
  const totalSec = Math.floor(ms / 1000);
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;

  const parts = [];
  if (h > 0) parts.push(`${h}h`);
  if (m > 0) parts.push(`${m}m`);
  if (parts.length === 0) parts.push(`${s}s`);
  return parts.join(' ');
}
