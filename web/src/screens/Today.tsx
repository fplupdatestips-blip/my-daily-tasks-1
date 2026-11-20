import { useMemo, useState } from 'react';
import { addDays, isoWeekday, longLabel, parts, startOfWeek, weekdayShort, type LocalDate } from '../core/dates';
import type { Task } from '../core/model';
import { GROUPS, itemsForDay, type TodayItem } from '../core/today';
import { skip, toggleDone, toggleSubtask } from '../data/actions';
import { useAppState, useStore } from '../ui/hooks';
import { Icon } from '../ui/icons';
import { TEMPLATES } from './templates';
import { TaskCard } from './TaskCard';

interface Props {
  today: LocalDate;
  date: LocalDate;
  setDate: (d: LocalDate) => void;
  dark: boolean;
  wide: boolean;
  selectedId: string | null;
  onOpen: (t: Task) => void;
  onEdit: (t: Task | null, preset?: Partial<Task>) => void;
  onUndo: (text: string, undo: () => void) => void;
  topBar: React.ReactNode;
}

export function Today({ today, date, setDate, dark, wide, selectedId, onOpen, onEdit, onUndo, topBar }: Props) {
  const store = useStore();
  const { tasks, completions, settings } = useAppState();
  const [showDone, setShowDone] = useState(false);
  const [quick, setQuick] = useState('');
  const items = useMemo(() => itemsForDay(tasks, completions, date, today, settings.firstDayOfWeek), [tasks, completions, date, today, settings.firstDayOfWeek]);
  const open = items.filter((i) => !i.done && !i.skipped);
  const finished = items.filter((i) => i.done || i.skipped);
  const doneCount = items.filter((i) => i.done).length;

  const missedYesterday = useMemo(() => {
    if (date !== today) return 0;
    const y = addDays(today, -1);
    return itemsForDay(tasks, completions, y, today, settings.firstDayOfWeek).filter((i) => !i.done && !i.skipped && i.task.repeat.type !== 'NONE').length;
  }, [tasks, completions, date, today, settings.firstDayOfWeek]);
  const dismissKey = `dt.missed.${today}`;
  const [dismissed, setDismissed] = useState(() => {
    try {
      return localStorage.getItem(dismissKey) === '1';
    } catch {
      return false;
    }
  });

  const act = (item: TodayItem) => {
    const undo = toggleDone(store, item, date);
    if (!item.done) {
      navigator.vibrate?.(10);
      onUndo(`Done: ${item.task.name}`, undo);
    }
  };
  const doSkip = (item: TodayItem) => onUndo(item.skipped ? `Unskipped ${item.task.name}` : `Skipped ${item.task.name}`, skip(store, item, date));
  const doSub = (item: TodayItem, id: string) => {
    const r = toggleSubtask(store, item, id, date);
    if (r.completedTask) onUndo(`Done: ${item.task.name}`, r.undo);
  };

  const weekStart = startOfWeek(date, settings.firstDayOfWeek);
  const week = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  const quickAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!quick.trim()) return;
    onEdit(null, { name: quick.trim() });
    setQuick('');
  };

  const card = (i: TodayItem) => (
    <TaskCard
      key={i.task.id}
      item={i}
      dark={dark}
      selected={selectedId === i.task.id}
      onToggle={() => act(i)}
      onSubtask={(id) => doSub(i, id)}
      onOpen={() => onOpen(i.task)}
      onSkip={() => doSkip(i)}
      onEdit={() => onEdit(i.task)}
    />
  );

  const ringC = 2 * Math.PI * 26;
  const frac = items.length ? doneCount / items.length : 0;
  const isToday = date === today;

  return (
    <div className="flex flex-col">
      <header className="flex items-start justify-between px-4 pb-2 pt-5">
        <div className="flex flex-col gap-0.5">
          <span className="text-sm font-medium text-text2">{longLabel(date)}</span>
          <h1 className="m-0 text-[28px] font-semibold tracking-tight">{isToday ? 'Today' : date < today ? 'Past day' : 'Upcoming'}</h1>
        </div>
        <div className="flex items-center gap-1">
          {!isToday && (
            <button type="button" onClick={() => setDate(today)} className="h-10 rounded-full px-4 text-sm font-semibold text-primary hover:bg-surface2">
              Back to today
            </button>
          )}
          {topBar}
        </div>
      </header>

      <nav aria-label="Week" className="grid grid-cols-[auto_repeat(7,minmax(0,1fr))_auto] items-center gap-1 px-3 pb-3">
        <button type="button" aria-label="Previous week" onClick={() => setDate(addDays(date, -7))} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
          <Icon name="chevronLeft" size={18} />
        </button>
        {week.map((d) => {
          const sel = d === date;
          const its = itemsForDay(tasks, completions, d, today, settings.firstDayOfWeek);
          const all = its.length > 0 && its.every((i) => i.done || i.skipped);
          return (
            <button
              key={d}
              type="button"
              onClick={() => setDate(d)}
              aria-label={`${d === today ? 'Today, ' : ''}${longLabel(d)}${all && d <= today ? ', all done' : ''}`}
              aria-current={sel ? 'date' : undefined}
              className={`flex h-[60px] flex-col items-center justify-center gap-0.5 rounded-2xl ${sel ? 'bg-primary text-on-primary' : d === today ? 'text-primary' : 'text-text2 hover:bg-surface2'}`}
            >
              <span className="text-xs font-medium">{weekdayShort(isoWeekday(d)).slice(0, 1)}</span>
              <span className="text-base font-semibold">{parts(d).d}</span>
              <span className={`size-[5px] rounded-full ${all && d <= today && !sel ? 'bg-done' : 'bg-transparent'}`} />
            </button>
          );
        })}
        <button type="button" aria-label="Next week" onClick={() => setDate(addDays(date, 7))} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
          <Icon name="chevronRight" size={18} />
        </button>
      </nav>

      <main id="tasks" className="flex flex-col gap-3 px-4 pb-32">
        {items.length > 0 && (
          <section className="flex items-center gap-4 rounded-[20px] bg-primary-container px-4 py-2.5 text-on-primary-container">
            <svg width="52" height="52" viewBox="0 0 64 64" role="img" aria-label={`${doneCount} of ${items.length} tasks done`}>
              <circle cx="32" cy="32" r="26" fill="none" stroke="var(--ring-track)" strokeWidth="8" />
              {frac > 0 && <circle cx="32" cy="32" r="26" fill="none" stroke="var(--primary)" strokeWidth="8" strokeLinecap="round" strokeDasharray={`${ringC * frac} ${ringC}`} transform="rotate(-90 32 32)" style={{ transition: 'stroke-dasharray 300ms' }} />}
              <text x="32" y="37" textAnchor="middle" fontSize="16" fontWeight="700" fill="currentColor">
                {doneCount}/{items.length}
              </text>
            </svg>
            <div className="flex flex-col gap-1">
              <span className="text-lg font-semibold">{open.length === 0 ? 'All done for the day' : `${doneCount} of ${items.length} done`}</span>
              <span className="text-sm">{open.length === 0 ? 'Nice work.' : `${open.length} left`}</span>
            </div>
          </section>
        )}

        {missedYesterday > 0 && !dismissed && (
          <section className="flex items-center gap-3 rounded-2xl bg-surface px-4 py-3">
            <span className="flex-1 text-sm">
              You missed {missedYesterday} {missedYesterday === 1 ? 'task' : 'tasks'} yesterday.
            </span>
            <button type="button" className="h-10 rounded-full px-3 text-sm font-semibold text-primary hover:bg-surface2" onClick={() => setDate(addDays(today, -1))}>
              Review
            </button>
            <button
              type="button"
              aria-label="Dismiss"
              className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2"
              onClick={() => {
                setDismissed(true);
                try {
                  localStorage.setItem(dismissKey, '1');
                } catch {
                  /* ignore */
                }
              }}
            >
              <Icon name="close" size={16} />
            </button>
          </section>
        )}

        {wide && (
          <form onSubmit={quickAdd} className="flex items-center gap-2 rounded-2xl bg-surface px-3">
            <Icon name="plus" className="text-text2" />
            <label htmlFor="quick-add" className="sr-only">
              Add a task
            </label>
            <input id="quick-add" value={quick} onChange={(e) => setQuick(e.target.value)} placeholder="Add a task, press Enter" className="h-12 flex-1 bg-transparent outline-none placeholder:text-text2" />
          </form>
        )}

        {items.length === 0 && tasks.length === 0 && (
          <section className="flex flex-col items-center gap-4 rounded-3xl bg-surface px-6 py-10 text-center">
            <span className="flex size-16 items-center justify-center rounded-3xl bg-primary-container text-on-primary-container">
              <Icon name="today" size={30} />
            </span>
            <h2 className="m-0 text-xl font-semibold">Add your first daily task</h2>
            <p className="m-0 max-w-xs text-sm text-text2">Start from one of these, or tap the add button to make your own.</p>
            <div className="flex flex-wrap justify-center gap-2">
              {TEMPLATES.map((t) => (
                <button key={t.name} type="button" onClick={() => onEdit(null, t)} className="flex h-10 items-center gap-1.5 rounded-xl border border-outline px-3 text-sm hover:bg-surface2">
                  <Icon name={t.icon ?? 'check'} size={16} />
                  {t.name}
                </button>
              ))}
            </div>
          </section>
        )}
        {items.length === 0 && tasks.length > 0 && (
          <p className="py-10 text-center text-text2">Nothing due {isToday ? 'today' : `on ${longLabel(date)}`}.</p>
        )}

        {GROUPS.map((g) => {
          const list = open.filter((i) => i.task.timeOfDay === g.key);
          if (!list.length) return null;
          return (
            <section key={g.key} className="flex flex-col gap-2" aria-labelledby={`g-${g.key}`}>
              <h2 id={`g-${g.key}`} className="mx-1 mb-0 mt-1 text-[13px] font-semibold uppercase tracking-wider text-text2">
                {g.label}
              </h2>
              {list.map(card)}
            </section>
          );
        })}

        {finished.length > 0 && (
          <section className="flex flex-col gap-2">
            <button type="button" aria-expanded={showDone} onClick={() => setShowDone(!showDone)} className="flex h-[52px] items-center gap-3 rounded-2xl bg-surface px-4 text-[15px] font-medium text-text2">
              <span className="flex size-6 items-center justify-center rounded-full bg-done text-on-done">
                <Icon name="check" size={14} strokeWidth={3} />
              </span>
              <span className="flex-1 text-left">Done · {finished.length}</span>
              <Icon name="chevronDown" className={`transition-transform ${showDone ? 'rotate-180' : ''}`} />
            </button>
            {showDone && finished.map(card)}
          </section>
        )}
      </main>
    </div>
  );
}
