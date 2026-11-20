import { describe, expect, it } from 'vitest';
import cases from '../../../shared/repeat-cases.json';
import { dueInfo, streak, type DayMark } from './repeat';
import type { Repeat } from './model';
import { isoWeekday } from './dates';

type Marks = { date: string; status: string }[] | undefined;
const marks = (c: Marks) => (c ?? []) as DayMark[];

describe('dates', () => {
  it('knows 20 Nov 2026 is a Friday', () => expect(isoWeekday('2026-11-20')).toBe(5));
});

describe('shared due cases', () => {
  for (const c of cases.dueCases as Array<{ name: string; repeat: Repeat; date: string; completions?: Marks; archived?: boolean; pausedUntil?: string; expect: { due: boolean; movedFrom?: string; overdueDays?: number } }>) {
    it(c.name, () => {
      const r = dueInfo({ repeat: c.repeat, archived: c.archived, pausedUntil: c.pausedUntil }, c.date, marks(c.completions), cases.weekStart);
      expect(r.due).toBe(c.expect.due);
      if (c.expect.due) {
        expect(r.movedFrom ?? null).toBe(c.expect.movedFrom ?? null);
        expect(r.overdueDays).toBe(c.expect.overdueDays ?? 0);
      }
    });
  }
});

describe('shared streak cases', () => {
  for (const c of cases.streakCases as Array<{ name: string; repeat: Repeat; today: string; completions?: Marks; expect: { current: number; best: number } }>) {
    it(c.name, () => expect(streak({ repeat: c.repeat }, c.today, marks(c.completions))).toEqual(c.expect));
  }
});
