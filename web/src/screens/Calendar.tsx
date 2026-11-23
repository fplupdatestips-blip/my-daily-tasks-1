import { useMemo, useState } from 'react';
import { addDays, isoWeekday, longLabel, monthLong, parts, startOfWeek, weekdayShort, type LocalDate } from '../core/dates';
import { describeRepeat, formatTime } from '../core/describe';
import type { Task } from '../core/model';
import { calendarItems, monthGrid } from '../core/calendar';
import type { TodayItem } from '../core/today';
import { useAllTasks } from '../data/football';
import { useAppState, useMedia } from '../ui/hooks';
import { Icon } from '../ui/icons';
import { taskColors } from '../ui/palette';
import { ScreenHeader } from './OtherScreens';
import { TaskIcon } from '../ui/TaskIcon';

interface Props {
  today: LocalDate;
  dark: boolean;
  onOpen: (t: Task) => void;
  onOpenDay: (d: LocalDate) => void;
  topBar: React.ReactNode;
}

export function Calendar({ today, dark, onOpen, onOpenDay, topBar }: Props) {
  const { tasks: ownTasks, completions, settings } = useAppState();
  const tasks = useAllTasks(ownTasks, settings);
  const weekStart = settings.firstDayOfWeek;
  const wide = useMedia('(min-width: 600px)');
  const [month, setMonth] = useState(() => ({ y: parts(today).y, m: parts(today).m }));
  const [selected, setSelected] = useState<LocalDate>(today);
  const [view, setViewState] = useState<'week' | 'month'>(() => {
    try {
      return localStorage.getItem('dt.calView') === 'month' ? 'month' : 'week';
    } catch {
      return 'week';
    }
  });
  const setView = (v: 'week' | 'month') => {
    setViewState(v);
    try {
      localStorage.setItem('dt.calView', v);
    } catch {
      /* ignore */
    }
  };
  const [weekOf, setWeekOf] = useState<LocalDate>(() => startOfWeek(today, weekStart));
  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(startOfWeek(weekOf, weekStart), i)), [weekOf, weekStart]);
  const weekItems = useMemo(() => weekDays.map((d) => calendarItems(tasks, completions, d, today, weekStart)), [weekDays, tasks, completions, today, weekStart]);

  const rows = useMemo(() => monthGrid(month.y, month.m, weekStart), [month, weekStart]);
  const byDay = useMemo(() => {
    const m = new Map<LocalDate, TodayItem[]>();
    for (const row of rows) for (const d of row) if (d) m.set(d, calendarItems(tasks, completions, d, today, weekStart));
    return m;
  }, [rows, tasks, completions, today, weekStart]);
  const dayItems = useMemo(
    () => byDay.get(selected) ?? calendarItems(tasks, completions, selected, today, weekStart),
    [byDay, selected, tasks, completions, today, weekStart],
  );

  const shift = (n: number) =>
    setMonth(({ y, m }) => {
      const t = y * 12 + (m - 1) + n;
      return { y: Math.floor(t / 12), m: (t % 12) + 1 };
    });
  const goToday = () => {
    setMonth({ y: parts(today).y, m: parts(today).m });
    setSelected(today);
    setWeekOf(startOfWeek(today, weekStart));
  };
  const isThisMonth = month.y === parts(today).y && month.m === parts(today).m;
  const isThisWeek = weekDays.includes(today);
  const move = (n: number) => (view === 'week' ? setWeekOf((w) => addDays(w, 7 * n)) : shift(n));
  const unit = view === 'week' ? 'week' : 'month';
  const first = weekDays[0];
  const last = weekDays[6];
  const mShort = (d: LocalDate) => monthLong(parts(d).m).slice(0, 3);
  const weekLabel =
    parts(first).m === parts(last).m
      ? `${mShort(first)} ${parts(first).d} – ${parts(last).d}`
      : `${mShort(first)} ${parts(first).d} – ${mShort(last)} ${parts(last).d}`;
  const headers = Array.from({ length: 7 }, (_, i) => ((weekStart - 1 + i) % 7) + 1);
  const doneCount = dayItems.filter((i) => i.done).length;

  return (
    <div className="flex flex-col">
      <ScreenHeader title="Calendar" right={topBar} />
      <div className="flex flex-col gap-4 px-4 pb-32">
        <section className="-mx-2 flex flex-col gap-2 rounded-[20px] bg-surface p-2 sm:mx-0 sm:p-3">
          <div className="flex flex-wrap items-center gap-1">
            <button type="button" onClick={goToday} disabled={view === 'week' ? isThisWeek : isThisMonth} className="h-10 rounded-xl border border-outline px-4 text-sm font-semibold hover:bg-surface2 disabled:opacity-50">
              Today
            </button>
            <button type="button" aria-label={`Previous ${unit}`} onClick={() => move(-1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronLeft" size={18} />
            </button>
            <button type="button" aria-label={`Next ${unit}`} onClick={() => move(1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronRight" size={18} />
            </button>
            <h2 className="order-last m-0 w-full truncate pl-1 pt-1 text-base font-semibold sm:order-none sm:w-auto sm:flex-1 sm:pt-0 sm:text-lg" aria-live="polite">
              {view === 'week' ? weekLabel : `${monthLong(month.m)} ${month.y}`}
            </h2>
            <div role="group" aria-label="Calendar view" className="ml-auto flex rounded-xl bg-surface2 p-1">
              {(['week', 'month'] as const).map((v) => (
                <button
                  key={v}
                  type="button"
                  aria-pressed={view === v}
                  onClick={() => setView(v)}
                  className={`h-8 rounded-lg px-3 text-sm font-semibold ${view === v ? 'bg-surface text-text shadow-sm' : 'text-text2 hover:text-text'}`}
                >
                  {v === 'week' ? 'Week' : 'Month'}
                </button>
              ))}
            </div>
          </div>
          {view === 'week' && <WeekGrid days={weekDays} items={weekItems} today={today} dark={dark} onOpen={onOpen} onOpenDay={onOpenDay} />}
          {view === 'month' && <div className="grid grid-cols-7 overflow-hidden rounded-xl border border-divider" role="grid" aria-label={`${monthLong(month.m)} ${month.y}`}>
            {headers.map((w) => (
              <div key={w} role="columnheader" className="border-b border-divider py-1.5 text-center text-xs font-medium text-text2">
                {weekdayShort(w)}
              </div>
            ))}
            {rows.flat().map((d, idx) => {
              const edge = `${idx % 7 !== 6 ? 'border-r' : ''} ${idx < rows.length * 7 - 7 ? 'border-b' : ''} border-divider`;
              if (!d) return <div key={`e${idx}`} className={`bg-surface2/60 ${edge}`} />;
              const its = byDay.get(d) ?? [];
              const allDone = its.length > 0 && its.every((i) => i.done || i.skipped) && d <= today;
              const sel = d === selected;
              const isToday = d === today;
              const max = wide ? 4 : 3;
              const shown = its.length > max ? its.slice(0, max - 1) : its;
              const more = its.length - shown.length;
              return (
                <button
                  key={d}
                  type="button"
                  onClick={() => setSelected(d)}
                  aria-label={`${isToday ? 'Today, ' : ''}${longLabel(d)}, ${its.length} ${its.length === 1 ? 'task' : 'tasks'}${allDone ? ', all done' : ''}: ${its.map((i) => i.task.name).join(', ')}`}
                  aria-pressed={sel}
                  className={`flex min-h-[92px] min-w-0 flex-col gap-[3px] p-[3px] text-left sm:min-h-[112px] sm:gap-1 sm:p-1.5 ${edge} ${
                    sel ? 'bg-primary-container/60 outline-2 -outline-offset-2 outline-primary' : 'hover:bg-surface2'
                  }`}
                >
                  <span className="flex items-center justify-end gap-1 pb-0.5">
                    {allDone && (
                      <span className="flex size-4 items-center justify-center rounded-full bg-done text-on-done" title="All done">
                        <Icon name="check" size={10} strokeWidth={3.5} />
                      </span>
                    )}
                    <span
                      className={`flex h-6 min-w-6 items-center justify-center rounded-full px-1 text-xs font-semibold sm:text-sm ${
                        isToday ? 'bg-primary text-on-primary' : d < today ? 'text-text2' : ''
                      }`}
                    >
                      {parts(d).d}
                    </span>
                  </span>
                  {shown.map((i) => {
                    const { tint, fg } = taskColors(i.task.color, dark);
                    const finished = i.done || i.skipped;
                    return (
                      <span
                        key={i.task.id}
                        className={`flex min-w-0 items-center gap-1 rounded-md px-1 py-[2px] text-[10px] font-medium leading-tight sm:rounded-lg sm:px-1.5 sm:py-1 sm:text-[13px] ${finished ? 'opacity-50' : ''}`}
                        style={{ background: tint, color: fg }}
                      >
                        {wide && (i.task.image ? <img src={i.task.image} alt="" className="size-4 flex-none rounded object-cover" /> : <Icon name={i.task.icon} size={13} strokeWidth={2.2} className="flex-none" />)}
                        <span className={`overflow-hidden whitespace-nowrap sm:text-ellipsis ${i.done ? 'line-through' : ''}`}>{i.task.shortName ?? i.task.name}</span>
                      </span>
                    );
                  })}
                  {more > 0 && <span className="px-1 text-[10px] font-semibold text-text2 sm:text-xs">+{more} more</span>}
                </button>
              );
            })}
          </div>}
        </section>

        {view === 'month' && <section className="flex flex-col gap-2" aria-labelledby="cal-day">
          <div className="flex items-center gap-2 px-1">
            <div className="flex flex-1 flex-col">
              <h2 id="cal-day" className="m-0 text-lg font-semibold">
                {selected === today ? 'Today' : longLabel(selected)}
              </h2>
              <span className="text-sm text-text2">
                {dayItems.length === 0 ? 'Nothing due' : `${dayItems.length} ${dayItems.length === 1 ? 'task' : 'tasks'}${selected <= today ? ` · ${doneCount} done` : ''}`}
              </span>
            </div>
            <button type="button" onClick={() => onOpenDay(selected)} className="h-10 rounded-full border border-outline px-4 text-sm font-semibold text-primary hover:bg-surface2">
              Open day
            </button>
          </div>
          {dayItems.map((i) => (
            <DayRow key={i.task.id} item={i} date={selected} today={today} dark={dark} onOpen={() => onOpen(i.task)} />
          ))}
        </section>}
      </div>
    </div>
  );
}

function DayRow({ item, date, today, dark, onOpen }: { item: TodayItem; date: LocalDate; today: LocalDate; dark: boolean; onOpen: () => void }) {
  const { task, done, skipped } = item;
  const status = done ? 'Done' : skipped ? 'Skipped' : item.late ? item.late : date < today ? 'Not done' : null;
  const meta = [describeRepeat(task.repeat, true), task.time ? formatTime(task.time) : null].filter(Boolean).join(' · ');
  return (
    <button type="button" onClick={onOpen} className="flex items-center gap-3 rounded-2xl bg-surface py-3 pl-3 pr-4 text-left hover:bg-surface2">
      <TaskIcon task={task} dark={dark} />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className={`truncate text-[15px] font-medium ${done ? 'text-text2 line-through' : ''}`}>{task.name}</span>
        {task.description && <span className="truncate text-sm text-text2">{task.description}</span>}
        {meta && <span className="truncate text-xs text-text2">{meta}</span>}
      </span>
      {status && (
        <span className={`flex-none rounded-full px-2.5 py-1 text-xs font-semibold ${done ? 'bg-done text-on-done' : item.late || status === 'Not done' ? 'bg-amber-tint text-streak' : 'bg-surface2 text-text2'}`}>{status}</span>
      )}
    </button>
  );
}

/** A week as seven columns of cards, one card per task (like a content calendar). */
function WeekGrid({ days, items, today, dark, onOpen, onOpenDay }: { days: LocalDate[]; items: TodayItem[][]; today: LocalDate; dark: boolean; onOpen: (t: Task) => void; onOpenDay: (d: LocalDate) => void }) {
  return (
    <div className="-mx-2 overflow-x-auto px-2 pb-1 sm:mx-0 sm:px-0">
      <div className="grid min-w-[880px] grid-cols-7 gap-2">
        {days.map((d, idx) => {
          const isToday = d === today;
          const its = items[idx];
          return (
            <section key={d} aria-label={`${isToday ? 'Today, ' : ''}${longLabel(d)}, ${its.length} ${its.length === 1 ? 'task' : 'tasks'}`} className={`flex min-h-[420px] min-w-0 flex-col rounded-2xl border bg-bg ${isToday ? 'border-primary' : 'border-divider'}`}>
              <button type="button" onClick={() => onOpenDay(d)} title="Open this day" className="flex flex-col items-center gap-1 border-b border-divider py-2.5 hover:bg-surface2">
                <span className="text-xs font-medium text-text2">{weekdayShort(isoWeekday(d))}</span>
                <span className={`flex size-8 items-center justify-center rounded-full text-base font-semibold ${isToday ? 'bg-primary text-on-primary' : d < today ? 'text-text2' : ''}`}>{parts(d).d}</span>
              </button>
              <div className="flex flex-col gap-1.5 p-1.5">
                {its.map((i) => (
                  <WeekCard key={i.task.id} item={i} date={d} today={today} dark={dark} onOpen={() => onOpen(i.task)} />
                ))}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}

const TOD: Record<string, string> = { MORNING: 'Morning', AFTERNOON: 'Afternoon', EVENING: 'Evening', ANYTIME: 'Any time' };

function WeekCard({ item, date, today, dark, onOpen }: { item: TodayItem; date: LocalDate; today: LocalDate; dark: boolean; onOpen: () => void }) {
  const { task, done, skipped } = item;
  const missed = !done && !skipped && date < today;
  const status = done ? { label: 'Done', icon: 'check', cls: 'bg-done/15 text-done' } : skipped ? { label: 'Skipped', icon: 'skip', cls: 'bg-surface2 text-text2' } : missed ? { label: 'Missed', icon: 'close', cls: 'bg-amber-tint text-streak' } : { label: 'To do', icon: 'bell', cls: 'bg-surface2 text-text2' };
  return (
    <button
      type="button"
      onClick={onOpen}
      title={task.name}
      className={`flex min-w-0 items-start gap-1.5 rounded-xl border p-1.5 text-left shadow-sm transition-colors ${done ? 'border-done/30 bg-done/10' : 'border-divider bg-surface hover:bg-surface2'}`}
    >
      <TaskIcon task={task} dark={dark} size={36} iconSize={18} radius="0.55rem" className="self-start" />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="text-sm font-bold leading-tight">{task.time ?? TOD[task.timeOfDay]}</span>
        <span className={`truncate text-xs text-text2 ${done ? 'line-through' : ''}`}>{task.shortName ?? task.name}</span>
        <span className={`inline-flex items-center gap-1 self-start whitespace-nowrap rounded-md px-1.5 py-0.5 text-[11px] font-semibold ${status.cls}`}>
          <Icon name={status.icon} size={11} strokeWidth={2.6} />
          {status.label}
        </span>
      </span>
    </button>
  );
}
