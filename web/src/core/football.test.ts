import { describe, expect, it } from 'vitest';
import cases from '../../../shared/football-cases.json';
import { footballTasks, type FootballFeed } from './football';
import { itemsForDay } from './today';

describe('football feed to tasks', () => {
  for (const z of cases.zones) {
    it(z.zone, () => {
      const tasks = footballTasks(cases.feed as FootballFeed, {}, z.zone);
      expect(tasks.map((t) => ({ id: t.id, name: t.name, date: t.repeat.dueDate, time: t.time, timeOfDay: t.timeOfDay, icon: t.icon }))).toEqual(z.expect);
    });
  }
  it('settings turn each kind off', () => {
    const feed = cases.feed as FootballFeed;
    expect(footballTasks(feed, { fplDeadlines: false }, 'UTC').every((t) => t.auto === 'MATCH')).toBe(true);
    expect(footballTasks(feed, { matchCoverage: false }, 'UTC').every((t) => t.auto === 'FPL_DEADLINE')).toBe(true);
    expect(footballTasks(null, {}, 'UTC')).toEqual([]);
  });
  it('matches stay on their day and are not moved to the next one', () => {
    const tasks = footballTasks(cases.feed as FootballFeed, {}, 'Europe/London');
    expect(itemsForDay(tasks, [], '2026-11-24', '2026-11-25', 1).map((i) => i.task.id)).toEqual(['match-401915295']);
    expect(itemsForDay(tasks, [], '2026-11-25', '2026-11-25', 1)).toEqual([]);
  });
});
