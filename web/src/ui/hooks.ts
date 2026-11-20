import { createContext, useContext, useEffect, useState, useSyncExternalStore } from 'react';
import type { Theme } from '../core/model';
import type { AppState, AppStore } from '../data/store';

export const StoreContext = createContext<AppStore | null>(null);

export function useStore(): AppStore {
  const s = useContext(StoreContext);
  if (!s) throw new Error('StoreContext missing');
  return s;
}

export function useAppState(): AppState {
  const store = useStore();
  return useSyncExternalStore(store.subscribe, store.getState);
}

export function useMedia(q: string): boolean {
  const [m, setM] = useState(() => window.matchMedia(q).matches);
  useEffect(() => {
    const mq = window.matchMedia(q);
    const on = () => setM(mq.matches);
    mq.addEventListener('change', on);
    return () => mq.removeEventListener('change', on);
  }, [q]);
  return m;
}

function readTheme(): Theme {
  try {
    const t = localStorage.getItem('dt.theme');
    if (t === 'light' || t === 'dark' || t === 'system') return t;
  } catch {
    /* ignore */
  }
  return 'system';
}

/** Theme is per device, so it lives in localStorage, not in sync. */
export function useTheme(): { theme: Theme; setTheme: (t: Theme) => void; dark: boolean } {
  const [theme, setThemeState] = useState<Theme>(readTheme);
  const systemDark = useMedia('(prefers-color-scheme: dark)');
  const dark = theme === 'dark' || (theme === 'system' && systemDark);
  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#0E1514' : '#F7FAF9');
  }, [dark]);
  const setTheme = (t: Theme) => {
    setThemeState(t);
    try {
      localStorage.setItem('dt.theme', t);
    } catch {
      /* ignore */
    }
  };
  return { theme, setTheme, dark };
}

/** Re-renders when the logical day changes (checked every minute). */
export function useNow(): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 60_000);
    const vis = () => document.visibilityState === 'visible' && setNow(new Date());
    document.addEventListener('visibilitychange', vis);
    return () => {
      window.clearInterval(id);
      document.removeEventListener('visibilitychange', vis);
    };
  }, []);
  return now;
}
