import { addDays, daysBetween, daysInMonth, isoWeekday, ymd, type LocalDate } from './dates';
import type { Completion, Task } from './model';
import { itemsForDay, type TodayItem } from './today';

// What the calendar shows on each day. Its Kotlin twin is
// android/core/src/main/kotlin/app/dailytasks/core/CalendarItems.kt.
//
// Built on itemsForDay, with two changes so the month reads well:
// - A late task (a one-off not done yet, or an "N days after done" task)
//   is shown on its due date, on the day it was finished, and on today.
//   It is not repeated on every day in between, or on every future day.
// - Before today, "N days after done" tasks show only on days they were
//   ticked or skipped.
// - "N days after done" tasks are projected forward: after today they show
//   every N days from the next time they are due.

export function calendarItems(tasks: Task[], completions: Completion[], date: LocalDate, today: LocalDate, weekStart: number): TodayItem[] {
  const items = itemsForDay(tasks, completions, date, today, weekStart);
  const markedThatDay = new Set(completions.filter((c) => c.date === date).map((c) => c.taskId));
  const out = items.filter((i) => {
    if (date === today || markedThatDay.has(i.task.id)) return true;
    // Before today, an "N days after done" task only shows where it was ticked.
    if (date < today && i.task.repeat.type === 'AFTER_DONE') return false;
    return i.overdueDays === 0;
  });
  if (date <= today) return out;

  // Future days: add "N days after done" tasks on their projected days.
  for (const task of tasks) {
    if (task.deleted || task.archived || task.repeat.type !== 'AFTER_DONE') continue;
    if (out.some((i) => i.task.id === task.id)) continue;
    if (task.pausedUntil && date <= task.pausedUntil) continue;
    if (task.repeat.endDate && date > task.repeat.endDate) continue;
    const n = Math.max(1, task.repeat.interval ?? 1);
    const todayItem = itemsForDay([task], completions, today, today, weekStart)[0];
    let next: LocalDate;
    if (todayItem && !todayItem.done) next = addDays(today, n); // due now; assume done today
    else {
      // Find the next day it becomes due, looking ahead at most n days.
      next = addDays(today, n);
      for (let d = addDays(today, 1); d <= addDays(today, n); d = addDays(d, 1)) {
        if (itemsForDay([task], completions, d, today, weekStart).length) {
          next = d;
          break;
        }
      }
    }
    const diff = daysBetween(next, date);
    if (diff >= 0 && diff % n === 0) {
      out.push({ task, done: false, skipped: false, late: null, overdueDays: 0, streak: 0, subtasksDone: [] });
    }
  }
  return out;
}

/** The weeks of a month as rows of 7 dates (null = outside the month). */
export function monthGrid(year: number, month: number, weekStart: number): (LocalDate | null)[][] {
  const first = ymd(year, month, 1);
  const lead = (isoWeekday(first) - weekStart + 7) % 7;
  const cells: (LocalDate | null)[] = Array(lead).fill(null);
  for (let d = 1; d <= daysInMonth(year, month); d++) cells.push(ymd(year, month, d));
  while (cells.length % 7) cells.push(null);
  const rows: (LocalDate | null)[][] = [];
  for (let i = 0; i < cells.length; i += 7) rows.push(cells.slice(i, i + 7));
  return rows;
}
