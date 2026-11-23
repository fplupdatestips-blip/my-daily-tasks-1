import type { LocalDate } from './dates';
import type { Settings, Task, TimeOfDay } from './model';

// Turns the football feed (football.json, built by scripts/football-feed.mjs)
// into read-only tasks: one per FPL gameweek deadline and one "Match
// Coverage" per match an English Premier League team plays. They are made
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
export const matchTaskId = (matchId: string) => `match-${matchId}`;

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
    for (const m of feed.matches ?? []) {
      const { date, time } = localDateTime(m.kickoff, timeZone);
      const t = m.timeValid ? time : null;
      const bits = [m.competition, t ? `kick-off ${t}` : 'kick-off time to be confirmed', m.venue].filter(Boolean);
      out.push({
        ...base(matchTaskId(m.id), 'MATCH'),
        name: `Match Coverage: ${m.home.short} v ${m.away.short}`,
        description: `${m.home.name} v ${m.away.name} · ${bits.join(' · ')}`,
        icon: 'football',
        color: 'green',
        time: t,
        timeOfDay: timeOfDayFor(t),
        repeat: { type: 'NONE', dueDate: date, fixed: true },
      });
    }
  }
  return out;
}
