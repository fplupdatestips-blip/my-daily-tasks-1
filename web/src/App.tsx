import { useCallback, useEffect, useRef, useState } from 'react';
import { addDays, logicalToday } from './core/dates';
import type { Task } from './core/model';
import { Calendar } from './screens/Calendar';
import { Detail } from './screens/Detail';
import { Editor } from './screens/Editor';
import { Progress, SettingsScreen, SignIn, TasksList } from './screens/OtherScreens';
import { Today } from './screens/Today';
import { useAppState, useMedia, useNow, useStore, useTheme } from './ui/hooks';
import { Icon } from './ui/icons';

type Tab = 'today' | 'calendar' | 'tasks' | 'progress' | 'settings';
type Panel = { kind: 'detail'; id: string } | { kind: 'edit'; task: Task | null; preset?: Partial<Task> } | null;

const TABS: { key: Tab; label: string; icon: string }[] = [
  { key: 'today', label: 'Today', icon: 'today' },
  { key: 'calendar', label: 'Calendar', icon: 'calendar' },
  { key: 'tasks', label: 'Tasks', icon: 'list' },
  { key: 'progress', label: 'Progress', icon: 'chart' },
];

export function App() {
  const store = useStore();
  const state = useAppState();
  const { theme, setTheme, dark } = useTheme();
  const now = useNow();
  const today = logicalToday(now, state.settings.dayEndsAt);
  const [date, setDate] = useState(today);
  const [tab, setTab] = useState<Tab>('today');
  const [panel, setPanel] = useState<Panel>(null);
  const [snack, setSnack] = useState<{ text: string; undo: () => void } | null>(null);
  const snackTimer = useRef<number | undefined>(undefined);
  const phone = !useMedia('(min-width: 600px)');
  const desktop = useMedia('(min-width: 1200px)');

  // Follow the calendar when the day rolls over and the user was on today.
  const lastToday = useRef(today);
  useEffect(() => {
    if (lastToday.current !== today) {
      setDate((d) => (d === lastToday.current ? today : d));
      lastToday.current = today;
    }
  }, [today]);

  const showUndo = useCallback((text: string, undo: () => void) => {
    window.clearTimeout(snackTimer.current);
    setSnack({ text, undo });
    snackTimer.current = window.setTimeout(() => setSnack(null), 5000);
  }, []);

  const openEditor = useCallback((task: Task | null, preset?: Partial<Task>) => setPanel({ kind: 'edit', task, preset }), []);
  const openDetail = useCallback((t: Task) => setPanel({ kind: 'detail', id: t.id }), []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = e.target as HTMLElement;
      if (el.closest('input, textarea, select, [contenteditable]') || e.metaKey || e.ctrlKey || e.altKey) return;
      if (e.key === 'Escape') setPanel(null);
      else if (e.key === 'n' || e.key === 'N') {
        e.preventDefault();
        openEditor(null);
      } else if (e.key === 't' || e.key === 'T') {
        setTab('today');
        setDate(today);
      } else if (e.key === 'ArrowLeft' && tab === 'today') setDate((d) => addDays(d, -1));
      else if (e.key === 'ArrowRight' && tab === 'today') setDate((d) => addDays(d, 1));
      else if (e.key === '1') setTab('today');
      else if (e.key === '2') setTab('calendar');
      else if (e.key === '3') setTab('tasks');
      else if (e.key === '4') setTab('progress');
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [openEditor, tab, today]);

  if (!state.ready) return <div className="min-h-dvh" aria-busy="true" />;
  if (!state.user) return <SignIn />;

  const syncIcon = { synced: 'cloud', syncing: 'cloudSync', offline: 'cloudOff', local: 'device' }[state.sync];
  const syncLabel = {
    synced: 'All changes synced',
    syncing: 'Syncing…',
    offline: "Offline. Changes will sync when you're back online",
    local: 'Test mode: saved in this browser only',
  }[state.sync];

  const topBar = (
    <>
      <span title={syncLabel} aria-label={syncLabel} role="img" className={`flex size-12 items-center justify-center text-text2 ${state.sync === 'syncing' ? 'animate-pulse' : ''}`}>
        <Icon name={syncIcon} size={22} strokeWidth={1.8} />
      </span>
      {phone && (
        <button type="button" aria-label="Settings" onClick={() => setTab('settings')} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
          <Icon name="settings" size={22} strokeWidth={1.8} />
        </button>
      )}
    </>
  );

  const nextSort = state.tasks.reduce((m, t) => Math.max(m, t.sortOrder), 0) + 1;
  const detailTask = panel?.kind === 'detail' ? state.tasks.find((t) => t.id === panel.id) : undefined;
  const panelContent =
    panel?.kind === 'edit' ? (
      <Editor
        key={panel.task?.id ?? `new-${panel.preset?.name ?? ''}`}
        task={panel.task}
        preset={panel.preset}
        today={today}
        dark={dark}
        nextSortOrder={nextSort}
        onCancel={() => setPanel(panel.task ? { kind: 'detail', id: panel.task.id } : null)}
        onSave={(t) => {
          void store.saveTask(t);
          setPanel(desktop ? { kind: 'detail', id: t.id } : null);
        }}
      />
    ) : detailTask ? (
      <Detail key={detailTask.id} task={detailTask} today={today} dark={dark} showBack={phone} onClose={() => setPanel(null)} onEdit={() => openEditor(detailTask)} />
    ) : null;

  const main = (
    <>
      {tab === 'today' && (
        <Today today={today} date={date} setDate={setDate} dark={dark} wide={!phone} selectedId={panel?.kind === 'detail' ? panel.id : null} onOpen={openDetail} onEdit={openEditor} onUndo={showUndo} topBar={topBar} />
      )}
      {tab === 'calendar' && (
        <Calendar
          today={today}
          dark={dark}
          onOpen={openDetail}
          onOpenDay={(d) => {
            setDate(d);
            setTab('today');
          }}
          topBar={topBar}
        />
      )}
      {tab === 'tasks' && <TasksList today={today} dark={dark} onOpen={openDetail} topBar={topBar} />}
      {tab === 'progress' && <Progress today={today} topBar={topBar} />}
      {tab === 'settings' && <SettingsScreen theme={theme} setTheme={setTheme} showBack={phone} onClose={() => setTab('today')} />}
    </>
  );

  return (
    <div className="flex min-h-dvh">
      <a href="#tasks" className="skip-link">
        Skip to tasks
      </a>

      {!phone && (
        <nav aria-label="Main" className="sticky top-0 flex h-dvh w-20 flex-none flex-col items-center gap-3 bg-surface2 py-4">
          <button type="button" aria-label="New task (N)" title="New task (N)" onClick={() => openEditor(null)} className="mb-4 flex size-14 items-center justify-center rounded-2xl bg-primary-container text-on-primary-container">
            <Icon name="plus" size={24} strokeWidth={2.2} />
          </button>
          {[...TABS, { key: 'settings' as Tab, label: 'Settings', icon: 'settings' }].map((t) => (
            <button key={t.key} type="button" aria-current={tab === t.key ? 'page' : undefined} onClick={() => setTab(t.key)} className="flex flex-col items-center gap-1 text-xs font-medium">
              <span className={`flex h-8 w-14 items-center justify-center rounded-2xl ${tab === t.key ? 'bg-primary-container text-on-primary-container' : 'text-text2'}`}>
                <Icon name={t.icon} size={22} />
              </span>
              <span className={tab === t.key ? 'font-semibold' : 'text-text2'}>{t.label}</span>
            </button>
          ))}
        </nav>
      )}

      <div className={`flex min-w-0 flex-1 justify-center ${desktop ? 'gap-0' : ''}`}>
        <div className={`w-full ${tab === 'calendar' ? 'max-w-[1100px]' : 'max-w-[720px]'}`}>{main}</div>
        {desktop && (
          <aside aria-label="Details" className="sticky top-0 h-dvh w-[420px] flex-none overflow-hidden border-l border-divider">
            {panelContent ?? (
              <div className="flex h-full flex-col items-center justify-center gap-2 p-8 text-center text-text2">
                <Icon name="today" size={32} />
                <p className="m-0">Select a task to see its streak and history, or press N to add one.</p>
              </div>
            )}
          </aside>
        )}
      </div>

      {!desktop && panelContent && (
        <div className="fixed inset-0 z-30 flex justify-end" role="dialog" aria-modal="true">
          {!phone && <button type="button" aria-label="Close" className="absolute inset-0 bg-[var(--scrim)]" onClick={() => setPanel(null)} />}
          <div className={`relative h-full overflow-hidden ${phone ? 'w-full' : 'w-[480px] rounded-l-[28px] shadow-2xl'}`}>{panelContent}</div>
        </div>
      )}

      {phone && tab === 'today' && !panelContent && (
        <button type="button" aria-label="Add task" onClick={() => openEditor(null)} className="fixed bottom-[100px] right-4 z-10 flex size-16 items-center justify-center rounded-[22px] bg-primary text-on-primary shadow-lg">
          <Icon name="plus" size={26} strokeWidth={2.2} />
        </button>
      )}

      {phone && (
        <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-10 grid h-20 grid-cols-4 bg-surface2">
          {TABS.map((t) => (
            <button key={t.key} type="button" aria-current={tab === t.key ? 'page' : undefined} onClick={() => setTab(t.key)} className="flex flex-col items-center justify-center gap-1 text-xs">
              <span className={`flex h-8 w-14 items-center justify-center rounded-2xl ${tab === t.key ? 'bg-primary-container text-on-primary-container' : 'text-text2'}`}>
                <Icon name={t.icon} size={22} />
              </span>
              <span className={tab === t.key ? 'font-semibold' : 'text-text2'}>{t.label}</span>
            </button>
          ))}
        </nav>
      )}

      {snack && (
        <div role="status" className={`fixed left-1/2 z-40 flex -translate-x-1/2 items-center gap-4 rounded-xl bg-text py-2 pl-4 pr-2 text-sm text-bg shadow-lg ${phone ? 'bottom-24' : 'bottom-6'}`}>
          <span>{snack.text}</span>
          <button
            type="button"
            onClick={() => {
              snack.undo();
              setSnack(null);
            }}
            className="h-10 rounded-full px-3 font-semibold text-primary-container"
          >
            Undo
          </button>
        </div>
      )}
    </div>
  );
}
