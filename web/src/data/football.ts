import { useMemo, useSyncExternalStore } from 'react';
import { footballTasks, type FootballFeed } from '../core/football';
import type { Settings, Task } from '../core/model';

// Loads football.json (published next to the web app and refreshed by the
// Build workflow) and keeps a copy in localStorage for offline starts.

const KEY = 'dt.football.v1';
const URL = `${import.meta.env.BASE_URL}football.json`;
const REFRESH_MS = 60 * 60 * 1000;

function cached(): FootballFeed | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as FootballFeed) : null;
  } catch {
    return null;
  }
}

let feed: FootballFeed | null = cached();
let lastFetch = 0;
const listeners = new Set<() => void>();

async function refresh() {
  if (Date.now() - lastFetch < REFRESH_MS / 2) return;
  lastFetch = Date.now();
  try {
    const r = await fetch(URL, { cache: 'no-cache' });
    if (!r.ok) return;
    const next = (await r.json()) as FootballFeed;
    if (!next || !Array.isArray(next.matches)) return;
    if (feed && next.updatedAt === feed.updatedAt) return;
    feed = next;
    try {
      localStorage.setItem(KEY, JSON.stringify(next));
    } catch {
      /* storage full: keep it in memory */
    }
    listeners.forEach((l) => l());
  } catch {
    /* offline: keep the cached copy */
  }
}

function subscribe(l: () => void) {
  listeners.add(l);
  void refresh();
  const id = window.setInterval(() => void refresh(), REFRESH_MS);
  const vis = () => document.visibilityState === 'visible' && void refresh();
  document.addEventListener('visibilitychange', vis);
  return () => {
    listeners.delete(l);
    window.clearInterval(id);
    document.removeEventListener('visibilitychange', vis);
  };
}

export function useFootballFeed(): FootballFeed | null {
  return useSyncExternalStore(subscribe, () => feed);
}

/** The person's tasks plus the FPL deadline and Match Coverage tasks. */
export function useAllTasks(tasks: Task[], settings: Settings): Task[] {
  const f = useFootballFeed();
  const auto = useMemo(() => footballTasks(f, settings), [f, settings.fplDeadlines, settings.matchCoverage]); // eslint-disable-line react-hooks/exhaustive-deps
  return useMemo(() => (auto.length ? [...tasks, ...auto] : tasks), [tasks, auto]);
}
