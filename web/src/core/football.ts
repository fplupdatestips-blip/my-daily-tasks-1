import type { LocalDate } from './dates';
import type { Settings, Task, TimeOfDay } from './model';

// Turns the football feed (football.json, built by scripts/football-feed.mjs)
// into read-only tasks: one per FPL gameweek deadline and one "Match
// Coverage" per day on which an English Premier League team plays. They are made
// fresh on every load and never saved, so a re-sync can't duplicate them;
// ticks are normal completions keyed by the stable ids below.
// Kotlin twin: android/core/src/main/kotlin/app/dailytasks/core/Football.kt

export interface FeedTeam {
  id: string;
  name: string;
  short: string;
  abbr: string;
}

export interface FeedMatch {
  id: string;
  kickoff: string;
  timeValid: boolean;
  home: FeedTeam;
  away: FeedTeam;
  competition: string;
  venue: string;
  state: string;
  statusName: string;
}

export interface FootballFeed {
  version: number;
  updatedAt: string;
  deadlines: { gw: number; name: string; deadline: string }[];
  matches: FeedMatch[];
}

export const deadlineTaskId = (gw: number) => `fpl-gw-${gw}`;
/** One Match Coverage task per local day. */
export const matchDayTaskId = (date: LocalDate) => `match-day-${date}`;

/** Local date and "HH:mm" of an instant, in the device's zone (or `timeZone`). */
export function localDateTime(iso: string, timeZone?: string): { date: LocalDate; time: string } {
  const f = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
  const p = Object.fromEntries(f.formatToParts(new Date(iso)).map((x) => [x.type, x.value]));
  return { date: `${p.year}-${p.month}-${p.day}`, time: `${p.hour}:${p.minute}` };
}

export function timeOfDayFor(time: string | null): TimeOfDay {
  if (!time) return 'ANYTIME';
  const h = Number(time.slice(0, 2));
  return h < 12 ? 'MORNING' : h < 17 ? 'AFTERNOON' : 'EVENING';
}

function base(id: string, auto: Task['auto']): Omit<Task, 'name' | 'description' | 'icon' | 'color' | 'timeOfDay' | 'time' | 'repeat'> {
  return { id, auto, subtasks: [], reminders: [], sortOrder: 0, createdAt: 0 };
}

export function footballTasks(feed: FootballFeed | null, settings: Pick<Settings, 'fplDeadlines' | 'matchCoverage'>, timeZone?: string): Task[] {
  if (!feed) return [];
  const out: Task[] = [];
  if (settings.fplDeadlines !== false) {
    for (const d of feed.deadlines ?? []) {
      const { date, time } = localDateTime(d.deadline, timeZone);
      out.push({
        ...base(deadlineTaskId(d.gw), 'FPL_DEADLINE'),
        name: `FPL GW${d.gw} deadline`,
        shortName: `GW${d.gw} deadline`,
        description: `${d.name} deadline at ${time}. Transfers, captain and chips locked in by then.`,
        icon: 'deadline',
        color: 'purple',
        time,
        timeOfDay: timeOfDayFor(time),
        repeat: { type: 'NONE', dueDate: date, fixed: true },
      });
    }
  }
  if (settings.matchCoverage !== false) {
    // One task per day, listing that day's matches in kick-off order.
    const byDay = new Map<LocalDate, { time: string | null; line: string }[]>();
    for (const m of [...(feed.matches ?? [])].sort((x, y) => (x.kickoff < y.kickoff ? -1 : x.kickoff > y.kickoff ? 1 : 0))) {
      const { date, time } = localDateTime(m.kickoff, timeZone);
      const t = m.timeValid ? time : null;
      const list = byDay.get(date) ?? [];
      list.push({ time: t, line: `${t ?? 'TBC'}  ${m.home.short} v ${m.away.short} · ${m.competition}` });
      byDay.set(date, list);
    }
    for (const [date, list] of byDay) {
      const first = list.find((x) => x.time)?.time ?? null;
      out.push({
        ...base(matchDayTaskId(date), 'MATCH'),
        name: 'Match Coverage',
        shortName: `${list.length} ${list.length === 1 ? 'match' : 'matches'}`,
        description: `${list.length} ${list.length === 1 ? 'match' : 'matches'}:\n${list.map((x) => x.line).join('\n')}`,
        icon: 'football',
        color: 'green',
        time: first,
        timeOfDay: timeOfDayFor(first),
        repeat: { type: 'NONE', dueDate: date, fixed: true },
      });
    }
  }
  return out;
}
