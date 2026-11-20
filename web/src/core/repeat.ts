import { addDays, daysBetween, daysInMonth, isLeapYear, isoWeekday, parts, startOfWeek, toEpochDay, type LocalDate } from './dates';
import type { Repeat } from './model';

// The repeat-rule engine. Its Kotlin twin is
// android/app/src/main/java/app/dailytasks/core/RepeatEngine.kt and both are
// tested against shared/repeat-cases.json. Change them together.

export interface RuleTask {
  repeat: Repeat;
  archived?: boolean;
  pausedUntil?: LocalDate | null;
}

export interface DayMark {
  date: LocalDate;
  status: 'DONE' | 'SKIPPED' | 'PARTIAL';
}

export interface DueInfo {
  due: boolean;
  /** One-off tasks shown after their due date: the original due date. */
  movedFrom?: LocalDate;
  /** Days late (one-off or AFTER_DONE). 0 when on time. */
  overdueDays: number;
}

const NOT_DUE: DueInfo = { due: false, overdueDays: 0 };

/**
 * Is the rule scheduled on this date, ignoring completions? Only meaningful
 * for calendar rules (DAILY, WEEKLY, MONTHLY, YEARLY); used for streaks.
 */
export function isScheduled(r: Repeat, date: LocalDate): boolean {
  if (r.startDate && date < r.startDate) return false;
  if (r.endDate && date > r.endDate) return false;
  const p = parts(date);
  switch (r.type) {
    case 'DAILY': {
      const n = Math.max(1, r.interval ?? 1);
      if (n === 1) return true;
      const anchor = r.startDate ?? date;
      return (((daysBetween(anchor, date) % n) + n) % n) === 0;
    }
    case 'WEEKLY':
      return (r.daysOfWeek ?? []).includes(isoWeekday(date));
    case 'MONTHLY': {
      if (r.ordinal && r.weekday) return isNthWeekday(date, r.ordinal, r.weekday);
      const want = Math.min(r.dayOfMonth ?? 1, daysInMonth(p.y, p.m));
      return p.d === want;
    }
    case 'YEARLY': {
      if (p.m !== (r.month ?? 1)) return false;
      let want = r.dayOfMonth ?? 1;
      if (want === 29 && p.m === 2 && !isLeapYear(p.y)) want = 28;
      return p.d === Math.min(want, daysInMonth(p.y, p.m));
    }
    default:
      return false;
  }
}

function isNthWeekday(date: LocalDate, ordinal: number, weekday: number): boolean {
  if (isoWeekday(date) !== weekday) return false;
  const p = parts(date);
  if (ordinal === -1) return p.d + 7 > daysInMonth(p.y, p.m);
  return Math.floor((p.d - 1) / 7) + 1 === ordinal;
}

/**
 * Should the task appear on this day's list? A task completed on `date`
 * still appears (as done). `marks` are this task's completions.
 */
export function dueInfo(task: RuleTask, date: LocalDate, marks: DayMark[], weekStart = 1): DueInfo {
  if (task.archived) return NOT_DUE;
  if (task.pausedUntil && date <= task.pausedUntil) return NOT_DUE;
  const r = task.repeat;

  if (r.type === 'NONE') {
    const dueDate = r.dueDate ?? date;
    if (date < dueDate) return NOT_DUE;
    // Finished (done or skipped) on an earlier day: gone.
    if (marks.some((m) => m.date < date && m.date >= dueDate && m.status !== 'PARTIAL')) return NOT_DUE;
    const late = daysBetween(dueDate, date);
    return late > 0 ? { due: true, movedFrom: dueDate, overdueDays: late } : { due: true, overdueDays: 0 };
  }

  if (r.startDate && date < r.startDate) return NOT_DUE;
  if (r.endDate && date > r.endDate) return NOT_DUE;

  if (r.type === 'QUOTA') {
    const count = Math.max(1, r.count ?? 1);
    const from: LocalDate = r.period === 'MONTH' ? `${date.slice(0, 8)}01` : startOfWeek(date, weekStart);
    const doneBefore = marks.filter((m) => m.status === 'DONE' && m.date >= from && m.date < date).length;
    return doneBefore < count ? { due: true, overdueDays: 0 } : NOT_DUE;
  }

  if (r.type === 'AFTER_DONE') {
    const n = Math.max(1, r.interval ?? 1);
    let last: LocalDate | undefined;
    for (const m of marks) {
      if (m.status === 'DONE' && m.date < date && (!last || m.date > last)) last = m.date;
    }
    const nextDue = last ? addDays(last, n) : (r.startDate ?? date);
    if (date < nextDue) return NOT_DUE;
    return { due: true, overdueDays: daysBetween(nextDue, date) };
  }

  return isScheduled(r, date) ? { due: true, overdueDays: 0 } : NOT_DUE;
}

export interface Streak {
  current: number;
  best: number;
}

/**
 * Streaks count scheduled days only. Skipped days are neutral, and today
 * not done yet does not break the streak. Only calendar rules have streaks.
 */
export function streak(task: RuleTask, today: LocalDate, marks: DayMark[]): Streak {
  const r = task.repeat;
  if (!['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'].includes(r.type)) return { current: 0, best: 0 };
  const byDate = new Map(marks.map((m) => [m.date, m.status]));
  let start = r.startDate ?? today;
  for (const m of marks) if (m.date < start) start = m.date;
  if (toEpochDay(today) - toEpochDay(start) > 3660) start = addDays(today, -3660);

  let run = 0;
  let best = 0;
  for (let d = start; d <= today; d = addDays(d, 1)) {
    if (!isScheduled(r, d)) continue;
    const s = byDate.get(d);
    if (s === 'DONE') {
      run += 1;
      best = Math.max(best, run);
    } else if (s === 'SKIPPED') {
      // neutral
    } else if (d !== today) {
      run = 0;
    }
  }
  return { current: run, best };
}
