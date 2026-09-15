const MINUTE = 60;
const HOUR = MINUTE * 60;
const DAY = HOUR * 24;

/** Short, timeline-style ages: 12s, 5m, 3h, 2d, then an absolute date. */
export function relativeTime(iso: string): string {
  const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);

  if (seconds < MINUTE) {
    return `${Math.floor(seconds)}s`;
  }
  if (seconds < HOUR) {
    return `${Math.floor(seconds / MINUTE)}m`;
  }
  if (seconds < DAY) {
    return `${Math.floor(seconds / HOUR)}h`;
  }
  if (seconds < DAY * 7) {
    return `${Math.floor(seconds / DAY)}d`;
  }
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
