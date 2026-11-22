import { describe, expect, it } from 'vitest';
import cases from '../../../shared/calendar-cases.json';
import { calendarItems, monthGrid } from './calendar';
import type { Completion, Task } from './model';

const tasks = cases.tasks.map((t, i) => ({
  id: t.id, name: t.id, description: '', icon: 'check', color: 'teal', timeOfDay: 'ANYTIME', repeat: t.repeat,
  subtasks: [], reminders: [], sortOrder: i, createdAt: 0,
})) as Task[];
const completions = cases.completions.map((c) => ({ ...c, id: `${c.taskId}_${c.date}`, completedAt: 0 })) as Completion[];

describe('calendar days', () => {
  for (const d of cases.days) {
    it(d.date, () => {
      const ids = calendarItems(tasks, completions, d.date, cases.today, cases.weekStart).map((i) => i.task.id).sort();
      expect(ids).toEqual([...d.expect].sort());
    });
  }
});

describe('monthGrid', () => {
  it('November 2026 starting Monday', () => {
    const g = monthGrid(2026, 11, 1);
    expect(g[0]).toEqual([null, null, null, null, null, null, '2026-11-01']);
    expect(g.length).toBe(6);
    expect(g[5][0]).toBe('2026-11-30');
  });
});
