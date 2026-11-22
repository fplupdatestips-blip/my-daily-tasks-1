import { useMemo, useState } from 'react';
import { addDays, daysInMonth, isoWeekday, longLabel, monthLong, parts, ymd, type LocalDate } from '../core/dates';
import { describeRepeat, formatTime } from '../core/describe';
import type { Task } from '../core/model';
import { dueInfo, isScheduled, streak } from '../core/repeat';
import { marksByTask } from '../core/today';
import { toggleDay } from '../data/actions';
import { useAppState, useStore } from '../ui/hooks';
import { Icon } from '../ui/icons';
import { TaskIcon } from '../ui/TaskIcon';

const CALENDAR = new Set(['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']);
export const PAUSED_FOREVER = '9999-12-31';

interface Props {
  task: Task;
  today: LocalDate;
  dark: boolean;
  onClose: () => void;
  onEdit: () => void;
  showBack: boolean;
}

export function Detail({ task, today, dark, onClose, onEdit, showBack }: Props) {
  const store = useStore();
  const { completions, settings } = useAppState();
  const [month, setMonth] = useState(() => today.slice(0, 7));
  const [confirmDelete, setConfirmDelete] = useState(false);
  const marks = useMemo(() => marksByTask(completions).get(task.id) ?? [], [completions, task.id]);
  const byDate = useMemo(() => new Map(marks.map((m) => [m.date, m.status])), [marks]);
  const s = streak(task, today, marks);
  const calendarRule = CALENDAR.has(task.repeat.type);

  const rate = useMemo(() => {
    let sched = 0;
    let done = 0;
    for (let i = 0; i < 30; i++) {
      const d = addDays(today, -i);
      if (task.repeat.startDate && d < task.repeat.startDate) break;
      const st = byDate.get(d);
      if (calendarRule) {
        if (!isScheduled(task.repeat, d) || st === 'SKIPPED' || (d === today && st !== 'DONE')) continue;
        sched++;
      }
      if (st === 'DONE') done++;
    }
    return calendarRule ? (sched ? `${Math.round((done / sched) * 100)}%` : '–') : String(done);
  }, [byDate, task, today, calendarRule]);

  const [y, m] = month.split('-').map(Number);
  const first = ymd(y, m, 1);
  const lead = (isoWeekday(first) - settings.firstDayOfWeek + 7) % 7;
  const cells: (LocalDate | null)[] = [...Array(lead).fill(null), ...Array.from({ length: daysInMonth(y, m) }, (_, i) => ymd(y, m, i + 1))];
  const shiftMonth = (d: number) => {
    const mm = m + d;
    const ny = y + Math.floor((mm - 1) / 12);
    const nm = ((((mm - 1) % 12) + 12) % 12) + 1;
    setMonth(ymd(ny, nm, 1).slice(0, 7));
  };
  const weekdayHeads = Array.from({ length: 7 }, (_, i) => ((settings.firstDayOfWeek - 1 + i) % 7) + 1);
  const history = [...marks].filter((x) => x.status !== 'PARTIAL').sort((a, b) => (a.date < b.date ? 1 : -1)).slice(0, 20);
  const paused = Boolean(task.pausedUntil && task.pausedUntil >= today);

  const dayState = (d: LocalDate) => {
    const st = byDate.get(d);
    if (st === 'DONE') return 'done';
    if (st === 'SKIPPED') return 'skip';
    if (d > today) return 'future';
    const scheduled = calendarRule ? isScheduled(task.repeat, d) : dueInfo({ repeat: task.repeat }, d, marks.filter((x) => x.date < d), settings.firstDayOfWeek).due;
    if (d === today) return 'today';
    return scheduled ? 'missed' : 'off';
  };

  return (
    <div className="flex h-full flex-col bg-bg">
      <header className="flex h-16 flex-none items-center justify-between px-1">
        <button type="button" aria-label={showBack ? 'Back' : 'Close details'} onClick={onClose} className="flex size-12 items-center justify-center rounded-full hover:bg-surface2">
          <Icon name={showBack ? 'back' : 'close'} size={22} />
        </button>
        <div className="flex">
          <button type="button" aria-label="Edit" onClick={onEdit} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name="edit" />
          </button>
          <button type="button" aria-label={paused ? 'Resume' : 'Pause'} title={paused ? 'Resume' : 'Pause'} onClick={() => void store.saveTask({ ...task, pausedUntil: paused ? null : PAUSED_FOREVER })} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name={paused ? 'play' : 'pause'} />
          </button>
          <button type="button" aria-label={task.archived ? 'Unarchive' : 'Archive'} title={task.archived ? 'Unarchive' : 'Archive'} onClick={() => void store.saveTask({ ...task, archived: !task.archived })} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name="archive" />
          </button>
          <button type="button" aria-label="Delete" onClick={() => setConfirmDelete(true)} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name="trash" />
          </button>
        </div>
      </header>

      <div className="flex flex-1 flex-col gap-4 overflow-y-auto px-4 pb-6">
        {confirmDelete && (
          <div role="alert" className="flex flex-wrap items-center gap-2 rounded-2xl bg-surface p-4">
            <span className="flex-1 text-sm">Delete “{task.name}” and its history? This can’t be undone.</span>
            <button type="button" onClick={() => setConfirmDelete(false)} className="h-10 rounded-full px-4 text-sm font-semibold hover:bg-surface2">
              Cancel
            </button>
            <button type="button" onClick={() => { void store.deleteTask(task.id); onClose(); }} className="h-10 rounded-full bg-missed px-4 text-sm font-semibold text-surface">
              Delete
            </button>
          </div>
        )}
        <div className="flex items-center gap-3.5">
          <TaskIcon task={task} dark={dark} size={56} iconSize={26} radius="1rem" />
          <div className="flex flex-col gap-0.5">
            <h1 className="m-0 text-2xl font-semibold">{task.name}</h1>
            <span className="text-sm text-text2">
              {[describeRepeat(task.repeat), task.timeOfDay.charAt(0) + task.timeOfDay.slice(1).toLowerCase(), task.time ? formatTime(task.time) : null].filter(Boolean).join(' · ')}
              {paused && ' · Paused'}
              {task.archived && ' · Archived'}
            </span>
          </div>
        </div>
        {task.description && <p className="m-0 text-[15px] leading-relaxed">{task.description}</p>}

        <div className="grid grid-cols-3 gap-2">
          {calendarRule ? (
            <>
              <Stat value={<span className="inline-flex items-center gap-1 text-streak"><Icon name="flame" strokeWidth={2} />{s.current}</span>} label="Current streak" />
              <Stat value={s.best} label="Best streak" />
              <Stat value={<span className="text-done">{rate}</span>} label="Last 30 days" />
            </>
          ) : (
            <>
              <Stat value={rate} label="Done in 30 days" />
              <Stat value={marks.filter((x) => x.status === 'DONE').length} label="Done in total" />
              <Stat value={task.subtasks.length} label="Subtasks" />
            </>
          )}
        </div>

        {task.subtasks.length > 0 && (
          <section className="rounded-2xl bg-surface px-4 py-2">
            <h2 className="mb-1 mt-1 text-[13px] font-semibold uppercase tracking-wider text-text2">Subtasks</h2>
            <ul className="m-0 list-disc pl-5 text-[15px] leading-8">
              {task.subtasks.map((x) => <li key={x.id}>{x.title}</li>)}
            </ul>
          </section>
        )}

        <section aria-label={`${monthLong(m)} ${y}`} className="flex flex-col gap-2 rounded-[20px] bg-surface p-3">
          <div className="flex items-center justify-between">
            <button type="button" aria-label="Previous month" onClick={() => shiftMonth(-1)} className="flex size-11 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronLeft" size={18} />
            </button>
            <h2 className="m-0 text-base font-semibold">{monthLong(m)} {y}</h2>
            <button type="button" aria-label="Next month" onClick={() => shiftMonth(1)} className="flex size-11 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronRight" size={18} />
            </button>
          </div>
          <div className="grid grid-cols-7 gap-1.5 text-center text-xs font-medium text-text2">
            {weekdayHeads.map((d) => <span key={d}>{['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][d - 1]}</span>)}
          </div>
          <div className="grid grid-cols-7 gap-1.5">
            {cells.map((d, i) => {
              if (!d) return <span key={`e${i}`} />;
              const st = dayState(d);
              const cls = {
                done: 'bg-done text-on-done',
                skip: 'bg-divider text-text2',
                missed: 'text-missed',
                today: 'border-2 border-primary text-primary',
                future: 'text-outline',
                off: 'text-text2',
              }[st];
              const label = `${longLabel(d)}, ${{ done: 'done', skip: 'skipped', missed: 'missed', today: 'today, not done yet', future: 'upcoming', off: 'not scheduled' }[st]}`;
              return (
                <button key={d} type="button" aria-label={label} disabled={d > today} onClick={() => toggleDay(store, task, d)} className={`h-10 rounded-[10px] p-0 text-[13px] font-semibold ${cls} ${d <= today ? 'hover:ring-2 hover:ring-outline' : ''}`}>
                  {parts(d).d}
                  {st === 'missed' && <span aria-hidden="true">×</span>}
                </button>
              );
            })}
          </div>
          <div className="flex flex-wrap gap-4 px-1 pt-1 text-xs text-text2">
            <span className="flex items-center gap-1.5"><span className="size-3 rounded-[3px] bg-done" />Done</span>
            <span className="flex items-center gap-1.5"><span className="size-3 rounded-[3px] bg-divider" />Skipped</span>
            <span className="flex items-center gap-1.5"><span className="text-missed">×</span>Missed</span>
            <span>Tap a past day to change it</span>
          </div>
        </section>

        <section className="flex flex-col gap-1">
          <h2 className="mx-1 mb-1 mt-0 text-[13px] font-semibold uppercase tracking-wider text-text2">History</h2>
          <div className="rounded-2xl bg-surface px-4 py-1">
            {history.length === 0 && <p className="text-sm text-text2">Nothing yet.</p>}
            {history.map((h) => (
              <div key={h.date} className="flex min-h-11 items-center justify-between border-b border-divider text-[15px] last:border-0">
                <span>{longLabel(h.date)}</span>
                <span className={`text-[13px] ${h.status === 'DONE' ? 'text-done' : 'text-text2'}`}>{h.status === 'DONE' ? 'Done' : 'Skipped'}</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function Stat({ value, label }: { value: React.ReactNode; label: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-2xl bg-surface p-3">
      <span className="text-2xl font-bold">{value}</span>
      <span className="text-[13px] text-text2">{label}</span>
    </div>
  );
}
