// Local calendar dates as "YYYY-MM-DD" strings. All arithmetic goes through
// "epoch days" (days since 1970-01-01) computed in UTC, so daylight saving
// never shifts a date.

export type LocalDate = string;

export function toEpochDay(d: LocalDate): number {
  const [y, m, day] = d.split('-').map(Number);
  return Math.floor(Date.UTC(y, m - 1, day) / 86_400_000);
}

export function fromEpochDay(n: number): LocalDate {
  const dt = new Date(n * 86_400_000);
  return ymd(dt.getUTCFullYear(), dt.getUTCMonth() + 1, dt.getUTCDate());
}

export function ymd(y: number, m: number, d: number): LocalDate {
  return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

export function parts(d: LocalDate): { y: number; m: number; d: number } {
  const [y, m, day] = d.split('-').map(Number);
  return { y, m, d: day };
}

export function addDays(d: LocalDate, n: number): LocalDate {
  return fromEpochDay(toEpochDay(d) + n);
}

export function daysBetween(from: LocalDate, to: LocalDate): number {
  return toEpochDay(to) - toEpochDay(from);
}

/** ISO weekday: 1 = Monday ... 7 = Sunday. */
export function isoWeekday(d: LocalDate): number {
  // 1970-01-01 was a Thursday (ISO 4).
  const n = toEpochDay(d);
  return ((((n + 3) % 7) + 7) % 7) + 1;
}

export function isLeapYear(y: number): boolean {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
}

export function daysInMonth(y: number, m: number): number {
  return [31, isLeapYear(y) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1];
}

/** First day of the week containing d. weekStart is an ISO weekday. */
export function startOfWeek(d: LocalDate, weekStart: number): LocalDate {
  const back = (isoWeekday(d) - weekStart + 7) % 7;
  return addDays(d, -back);
}

/**
 * The calendar date "today" counts as. With dayEndsAtHour = 3, 1:30 AM
 * still belongs to yesterday.
 */
export function logicalToday(now: Date = new Date(), dayEndsAtHour = 0): LocalDate {
  const shifted = new Date(now.getTime() - dayEndsAtHour * 3_600_000);
  return ymd(shifted.getFullYear(), shifted.getMonth() + 1, shifted.getDate());
}

const WEEKDAY_SHORT = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const WEEKDAY_LONG = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const MONTH_LONG = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

export const weekdayShort = (iso: number) => WEEKDAY_SHORT[iso - 1];
export const weekdayLong = (iso: number) => WEEKDAY_LONG[iso - 1];
export const monthLong = (m: number) => MONTH_LONG[m - 1];

/** "Friday, 20 November" */
export function longLabel(d: LocalDate): string {
  const p = parts(d);
  return `${weekdayLong(isoWeekday(d))}, ${p.d} ${monthLong(p.m)}`;
}

/** "Thu" if within the last week, else "12 Nov". */
export function shortLabel(d: LocalDate, today: LocalDate): string {
  const diff = daysBetween(d, today);
  if (diff >= 1 && diff < 7) return weekdayShort(isoWeekday(d));
  const p = parts(d);
  return `${p.d} ${monthLong(p.m).slice(0, 3)}`;
}
