import { shortLabel, type LocalDate } from './dates';
import { lateLabel } from './describe';
import { completionId, type Completion, type Task, type TimeOfDay } from './model';
import { dueInfo, streak, type DayMark } from './repeat';

export interface TodayItem {
  task: Task;
  done: boolean;
  skipped: boolean;
  late: string | null;
  overdueDays: number;
  streak: number;
  subtasksDone: string[];
}

export const GROUPS: { key: TimeOfDay; label: string }[] = [
  { key: 'MORNING', label: 'Morning' },
  { key: 'AFTERNOON', label: 'Afternoon' },
  { key: 'EVENING', label: 'Evening' },
  { key: 'ANYTIME', label: 'Anytime' },
];

export function marksByTask(completions: Completion[]): Map<string, DayMark[]> {
  const m = new Map<string, DayMark[]>();
  for (const c of completions) {
    const list = m.get(c.taskId) ?? [];
    list.push({ date: c.date, status: c.status });
    m.set(c.taskId, list);
  }
  return m;
}

/** Everything that belongs on a given day, in display order. */
export function itemsForDay(tasks: Task[], completions: Completion[], date: LocalDate, today: LocalDate, weekStart: number): TodayItem[] {
  const marks = marksByTask(completions);
  const byId = new Map(completions.map((c) => [c.id, c]));
  const items: TodayItem[] = [];
  for (const task of tasks) {
    if (task.deleted) continue;
    const tm = marks.get(task.id) ?? [];
    const info = dueInfo(task, date, tm, weekStart);
    if (!info.due) continue;
    const c = byId.get(completionId(task.id, date));
    const oneOff = task.repeat.type === 'NONE';
    items.push({
      task,
      done: c?.status === 'DONE',
      skipped: c?.status === 'SKIPPED',
      late: lateLabel(info.movedFrom, info.overdueDays, (d) => shortLabel(d, today)),
      overdueDays: info.overdueDays,
      streak: streak(task, today, tm).current,
      subtasksDone: oneOff ? task.subtasks.filter((s) => s.done).map((s) => s.id) : (c?.subtasksDone ?? []),
    });
  }
  // Late one-offs first in their group, then by exact time, then manual order.
  items.sort((a, b) => {
    if ((a.late ? 0 : 1) !== (b.late ? 0 : 1)) return (a.late ? 0 : 1) - (b.late ? 0 : 1);
    const at = a.task.time ?? '99:99';
    const bt = b.task.time ?? '99:99';
    if (at !== bt) return at < bt ? -1 : 1;
    return a.task.sortOrder - b.task.sortOrder;
  });
  return items;
}
