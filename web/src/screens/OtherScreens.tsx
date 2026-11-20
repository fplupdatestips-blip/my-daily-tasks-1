import { useMemo, useRef, useState } from 'react';
import { addDays, isoWeekday, startOfWeek, weekdayShort, type LocalDate } from '../core/dates';
import { describeRepeat } from '../core/describe';
import type { Task, Theme } from '../core/model';
import { streak } from '../core/repeat';
import { itemsForDay, marksByTask } from '../core/today';
import { useAppState, useStore } from '../ui/hooks';
import { Icon } from '../ui/icons';
import { taskColors } from '../ui/palette';

function ScreenHeader({ title, right }: { title: string; right?: React.ReactNode }) {
  return (
    <header className="flex items-center justify-between px-4 pb-3 pt-5">
      <h1 className="m-0 text-[28px] font-semibold tracking-tight">{title}</h1>
      <div className="flex items-center gap-1">{right}</div>
    </header>
  );
}

export function TasksList({ today, dark, onOpen, topBar }: { today: LocalDate; dark: boolean; onOpen: (t: Task) => void; topBar: React.ReactNode }) {
  const store = useStore();
  const { tasks } = useAppState();
  const [filter, setFilter] = useState<'active' | 'paused' | 'archived'>('active');
  const list = tasks
    .filter((t) => {
      const paused = Boolean(t.pausedUntil && t.pausedUntil >= today);
      if (filter === 'archived') return t.archived;
      if (filter === 'paused') return !t.archived && paused;
      return !t.archived && !paused;
    })
    .sort((a, b) => a.sortOrder - b.sortOrder);

  const move = (i: number, d: number) => {
    const j = i + d;
    if (j < 0 || j >= list.length) return;
    const a = list[i];
    const b = list[j];
    void store.saveTask({ ...a, sortOrder: b.sortOrder });
    void store.saveTask({ ...b, sortOrder: a.sortOrder });
  };

  return (
    <div className="flex flex-col">
      <ScreenHeader title="Tasks" right={topBar} />
      <div className="flex gap-2 px-4 pb-3" role="group" aria-label="Filter">
        {(['active', 'paused', 'archived'] as const).map((f) => (
          <button key={f} type="button" aria-pressed={filter === f} onClick={() => setFilter(f)} className={`h-9 rounded-[10px] px-3.5 text-sm capitalize ${filter === f ? 'bg-primary-container font-semibold text-on-primary-container' : 'border border-outline'}`}>
            {f}
          </button>
        ))}
      </div>
      <ul className="m-0 flex list-none flex-col gap-2 px-4 pb-32">
        {list.length === 0 && <li className="py-10 text-center text-text2">No {filter} tasks.</li>}
        {list.map((t, i) => {
          const { tint, fg } = taskColors(t.color, dark);
          return (
            <li key={t.id} className="flex items-center gap-2 rounded-2xl bg-surface py-2 pl-3 pr-1">
              <span className="flex size-10 flex-none items-center justify-center rounded-xl" style={{ background: tint, color: fg }}>
                <Icon name={t.icon} />
              </span>
              <button type="button" onClick={() => onOpen(t)} className="flex min-w-0 flex-1 flex-col text-left">
                <span className="truncate text-base font-medium">{t.name}</span>
                <span className="truncate text-[13px] text-text2">{describeRepeat(t.repeat)}{t.subtasks.length ? ` · ${t.subtasks.length} subtasks` : ''}</span>
              </button>
              {filter === 'active' && (
                <>
                  <button type="button" aria-label={`Move ${t.name} up`} disabled={i === 0} onClick={() => move(i, -1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2 disabled:opacity-30">
                    <Icon name="chevronDown" size={16} className="rotate-180" />
                  </button>
                  <button type="button" aria-label={`Move ${t.name} down`} disabled={i === list.length - 1} onClick={() => move(i, 1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2 disabled:opacity-30">
                    <Icon name="chevronDown" size={16} />
                  </button>
                </>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export function Progress({ today, topBar }: { today: LocalDate; topBar: React.ReactNode }) {
  const { tasks, completions, settings } = useAppState();
  const week = useMemo(() => {
    const start = startOfWeek(today, settings.firstDayOfWeek);
    return Array.from({ length: 7 }, (_, i) => {
      const d = addDays(start, i);
      const items = d <= today ? itemsForDay(tasks, completions, d, today, settings.firstDayOfWeek) : [];
      return { d, total: items.length, done: items.filter((x) => x.done).length };
    });
  }, [tasks, completions, today, settings.firstDayOfWeek]);
  const total = week.reduce((a, b) => a + b.total, 0);
  const done = week.reduce((a, b) => a + b.done, 0);
  const marks = marksByTask(completions);
  const streaks = tasks
    .filter((t) => !t.archived)
    .map((t) => ({ t, s: streak(t, today, marks.get(t.id) ?? []) }))
    .filter((x) => x.s.current > 0)
    .sort((a, b) => b.s.current - a.s.current)
    .slice(0, 5);
  const max = Math.max(1, ...week.map((w) => w.total));

  return (
    <div className="flex flex-col">
      <ScreenHeader title="Progress" right={topBar} />
      <div className="flex flex-col gap-4 px-4 pb-32">
        <section className="flex flex-col gap-3 rounded-[20px] bg-surface p-4">
          <div className="flex items-baseline justify-between">
            <h2 className="m-0 text-base font-semibold">This week</h2>
            <span className="text-sm text-text2">{total ? `${Math.round((done / total) * 100)}% · ${done} of ${total}` : 'No tasks yet'}</span>
          </div>
          <div className="grid h-40 grid-cols-7 items-end gap-2" role="img" aria-label={week.map((w) => `${weekdayShort(isoWeekday(w.d))} ${w.done} of ${w.total}`).join(', ')}>
            {week.map((w) => (
              <div key={w.d} className="flex h-full flex-col items-center justify-end gap-1">
                <span className="text-xs font-semibold text-text2">{w.total ? w.done : ''}</span>
                <div className="relative w-full max-w-10 overflow-hidden rounded-lg bg-ring-track/50" style={{ height: `${(w.total / max) * 100}%`, minHeight: 4 }}>
                  <div className="absolute bottom-0 w-full rounded-lg bg-done" style={{ height: w.total ? `${(w.done / w.total) * 100}%` : 0 }} />
                </div>
                <span className={`text-xs ${w.d === today ? 'font-bold text-primary' : 'text-text2'}`}>{weekdayShort(isoWeekday(w.d)).slice(0, 1)}</span>
              </div>
            ))}
          </div>
        </section>
        <section className="flex flex-col gap-1 rounded-[20px] bg-surface p-4">
          <h2 className="m-0 mb-2 text-base font-semibold">Longest active streaks</h2>
          {streaks.length === 0 && <p className="m-0 text-sm text-text2">Tick a repeating task a few days in a row to start a streak.</p>}
          {streaks.map(({ t, s }) => (
            <div key={t.id} className="flex min-h-11 items-center justify-between border-b border-divider last:border-0">
              <span>{t.name}</span>
              <span className="inline-flex items-center gap-1 font-semibold text-streak">
                <Icon name="flame" size={16} strokeWidth={2} />
                {s.current}
              </span>
            </div>
          ))}
        </section>
      </div>
    </div>
  );
}

export function SettingsScreen({ theme, setTheme, onClose, showBack }: { theme: Theme; setTheme: (t: Theme) => void; onClose: () => void; showBack: boolean }) {
  const store = useStore();
  const { settings, user, tasks, completions } = useAppState();
  const fileRef = useRef<HTMLInputElement>(null);
  const [msg, setMsg] = useState('');
  const local = store.backend.mode === 'local';

  const exportJson = () => {
    const blob = new Blob([JSON.stringify({ version: 1, exportedAt: new Date().toISOString(), settings, tasks, completions }, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `daily-tasks-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(a.href);
  };
  const importJson = async (f: File) => {
    try {
      const data = JSON.parse(await f.text());
      for (const t of data.tasks ?? []) await store.saveTask(t);
      for (const c of data.completions ?? []) await store.saveCompletion(c);
      setMsg(`Imported ${(data.tasks ?? []).length} tasks.`);
    } catch {
      setMsg('That file could not be read.');
    }
  };

  const row = 'flex min-h-14 items-center justify-between gap-3 border-b border-divider py-2 last:border-0';
  const sel = 'h-11 rounded-xl border border-outline bg-surface px-3';

  return (
    <div className="flex flex-col">
      <header className="flex items-center gap-1 px-2 pb-3 pt-4">
        {showBack && (
          <button type="button" aria-label="Back" onClick={onClose} className="flex size-12 items-center justify-center rounded-full hover:bg-surface2">
            <Icon name="back" size={22} />
          </button>
        )}
        <h1 className={`m-0 text-[28px] font-semibold tracking-tight ${showBack ? '' : 'px-2'}`}>Settings</h1>
      </header>
      <div className="flex max-w-xl flex-col gap-4 px-4 pb-32">
        <section className="rounded-[20px] bg-surface px-4">
          <h2 className="mb-0 mt-3 text-[13px] font-semibold uppercase tracking-wider text-text2">Appearance</h2>
          <div className={row}>
            <label htmlFor="theme">Theme on this device</label>
            <select id="theme" value={theme} onChange={(e) => setTheme(e.target.value as Theme)} className={sel}>
              <option value="system">System</option>
              <option value="light">Light</option>
              <option value="dark">Dark</option>
            </select>
          </div>
        </section>
        <section className="rounded-[20px] bg-surface px-4">
          <h2 className="mb-0 mt-3 text-[13px] font-semibold uppercase tracking-wider text-text2">Day</h2>
          <div className={row}>
            <label htmlFor="fdow">First day of the week</label>
            <select id="fdow" value={settings.firstDayOfWeek} onChange={(e) => void store.saveSettings({ ...settings, firstDayOfWeek: Number(e.target.value) })} className={sel}>
              <option value={1}>Monday</option>
              <option value={6}>Saturday</option>
              <option value={7}>Sunday</option>
            </select>
          </div>
          <div className={row}>
            <label htmlFor="dayend" className="flex flex-col">
              My day ends at
              <span className="text-[13px] text-text2">Ticks before this time count for the day before</span>
            </label>
            <select id="dayend" value={settings.dayEndsAt} onChange={(e) => void store.saveSettings({ ...settings, dayEndsAt: Number(e.target.value) })} className={sel}>
              {[0, 1, 2, 3, 4].map((h) => <option key={h} value={h}>{h === 0 ? 'Midnight' : `${h}:00 AM`}</option>)}
            </select>
          </div>
        </section>
        <section className="rounded-[20px] bg-surface px-4">
          <h2 className="mb-0 mt-3 text-[13px] font-semibold uppercase tracking-wider text-text2">Data</h2>
          <div className={row}>
            <span className="flex flex-col">
              Export
              <span className="text-[13px] text-text2">All tasks and history as a JSON file</span>
            </span>
            <button type="button" onClick={exportJson} className="flex h-10 items-center gap-1.5 rounded-full px-4 text-sm font-semibold text-primary hover:bg-surface2">
              <Icon name="download" size={18} />
              Export
            </button>
          </div>
          <div className={row}>
            <span className="flex flex-col">
              Import
              <span className="text-[13px] text-text2">Adds tasks from an exported file</span>
            </span>
            <button type="button" onClick={() => fileRef.current?.click()} className="flex h-10 items-center gap-1.5 rounded-full px-4 text-sm font-semibold text-primary hover:bg-surface2">
              <Icon name="upload" size={18} />
              Import
            </button>
            <input ref={fileRef} type="file" accept="application/json" className="hidden" onChange={(e) => e.target.files?.[0] && void importJson(e.target.files[0])} />
          </div>
          {msg && <p role="status" className="text-sm text-text2">{msg}</p>}
        </section>
        <section className="rounded-[20px] bg-surface px-4">
          <h2 className="mb-0 mt-3 text-[13px] font-semibold uppercase tracking-wider text-text2">Account</h2>
          <div className={row}>
            <span className="flex flex-col">
              {user?.name}
              <span className="text-[13px] text-text2">{local ? 'Test mode: saved in this browser only, not synced' : user?.email}</span>
            </span>
            <button type="button" onClick={() => void store.backend.signOut()} className="flex h-10 items-center gap-1.5 rounded-full px-4 text-sm font-semibold text-primary hover:bg-surface2">
              <Icon name="logout" size={18} />
              Sign out
            </button>
          </div>
        </section>
        <section className="rounded-[20px] bg-surface px-4 py-3 text-sm text-text2">
          <h2 className="mb-2 mt-0 text-[13px] font-semibold uppercase tracking-wider">Keyboard shortcuts</h2>
          <p className="m-0 leading-7">
            <kbd>N</kbd> new task · <kbd>T</kbd> today · <kbd>←</kbd> <kbd>→</kbd> change day · <kbd>1</kbd> <kbd>2</kbd> <kbd>3</kbd> switch tab · <kbd>Esc</kbd> close
          </p>
        </section>
      </div>
    </div>
  );
}

export function SignIn() {
  const store = useStore();
  const { error } = useAppState();
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const local = store.backend.mode === 'local';
  return (
    <main className="flex min-h-dvh items-center justify-center p-4">
      <div className="flex w-full max-w-sm flex-col items-center gap-5 rounded-3xl bg-surface p-8 text-center">
        <span className="flex size-16 items-center justify-center rounded-3xl bg-primary text-on-primary">
          <Icon name="check" size={32} strokeWidth={3} />
        </span>
        <h1 className="m-0 text-2xl font-semibold">Daily Tasks</h1>
        <p className="m-0 text-text2">Your daily tasks, the same on your phone and here.</p>
        <button
          type="button"
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            setErr('');
            try {
              await store.backend.signIn();
            } catch (e) {
              setErr(e instanceof Error ? e.message : 'Sign-in failed');
            } finally {
              setBusy(false);
            }
          }}
          className="flex h-12 w-full items-center justify-center gap-3 rounded-full bg-primary px-6 font-semibold text-on-primary disabled:opacity-60"
        >
          {local ? 'Try it in test mode' : 'Continue with Google'}
        </button>
        {local && <p className="m-0 text-[13px] text-text2">Firebase is not set up yet, so tasks are saved in this browser only.</p>}
        {(err || error) && <p role="alert" className="m-0 text-sm text-missed">{err || error}</p>}
      </div>
    </main>
  );
}
