import type { LocalDate } from './dates';

// Mirrors the Firestore documents under users/{uid}/... and the Kotlin
// classes in android/app/src/main/java/app/dailytasks/core/Model.kt.
// Keep field names identical on both sides.

export type RepeatType = 'NONE' | 'SOMEDAY' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY' | 'QUOTA' | 'AFTER_DONE';

export interface Repeat {
  type: RepeatType;
  /** NONE: the day the one-off task is due. SOMEDAY has no date yet and is never due. */
  dueDate?: LocalDate;
  /** NONE only: shown on dueDate and never moved to the next day (matches, deadlines). */
  fixed?: boolean;
  /** DAILY: every N days. AFTER_DONE: N days after the last completion. */
  interval?: number;
  /** WEEKLY: ISO weekdays, 1 = Monday ... 7 = Sunday. */
  daysOfWeek?: number[];
  /** MONTHLY by date, and YEARLY. */
  dayOfMonth?: number;
  /** MONTHLY by weekday: 1-4, or -1 for the last one. */
  ordinal?: number;
  /** MONTHLY by weekday: ISO weekday. */
  weekday?: number;
  /** YEARLY: 1-12. */
  month?: number;
  /** QUOTA: how many times per period. */
  count?: number;
  period?: 'WEEK' | 'MONTH';
  startDate?: LocalDate;
  endDate?: LocalDate;
}

export type TimeOfDay = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'ANYTIME';

export interface Subtask {
  id: string;
  title: string;
  /** Only used on one-off tasks, whose progress carries over between days. */
  done?: boolean;
}

export interface Reminder {
  /** Local wall-clock time "HH:mm". */
  localTime: string;
  enabled: boolean;
}

export interface Task {
  id: string;
  name: string;
  description: string;
  icon: string;
  /** Optional picture the person uploaded: a small (128px) data: URL. Shown instead of the icon. */
  image?: string | null;
  color: string;
  timeOfDay: TimeOfDay;
  /** Optional exact time "HH:mm". */
  time?: string | null;
  repeat: Repeat;
  subtasks: Subtask[];
  reminders: Reminder[];
  sortOrder: number;
  createdAt: number;
  archived?: boolean;
  pausedUntil?: LocalDate | null;
  updatedAt?: unknown;
  deleted?: boolean;
  /** Made by the app from the football feed, never saved: FPL deadline or match. */
  auto?: 'FPL_DEADLINE' | 'MATCH';
}

export type CompletionStatus = 'DONE' | 'SKIPPED' | 'PARTIAL';

export interface Completion {
  /** `${taskId}_${date}` so the same tick on two devices is one record. */
  id: string;
  taskId: string;
  date: LocalDate;
  status: CompletionStatus;
  /** Subtask ids ticked that day (repeating tasks reset every day). */
  subtasksDone?: string[];
  completedAt: number;
  deviceId?: string;
  updatedAt?: unknown;
}

export interface Settings {
  /** ISO weekday the week starts on. */
  firstDayOfWeek: number;
  /** 0-4: hour after midnight when "today" ends. */
  dayEndsAt: number;
  /** Show FPL gameweek deadlines in Today and the calendar (default on). */
  fplDeadlines?: boolean;
  /** Show "Match Coverage" for every Premier League team's match (default on). */
  matchCoverage?: boolean;
}

/** Device-only, not synced: each device can have its own theme. */
export type Theme = 'system' | 'light' | 'dark';

export const DEFAULT_SETTINGS: Settings = { firstDayOfWeek: 1, dayEndsAt: 0 };

export const completionId = (taskId: string, date: LocalDate) => `${taskId}_${date}`;

export function newId(): string {
  const a = new Uint8Array(10);
  crypto.getRandomValues(a);
  return Array.from(a, (b) => b.toString(36).padStart(2, '0')).join('').slice(0, 20);
}
