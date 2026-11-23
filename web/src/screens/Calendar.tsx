import { useMemo, useState } from 'react';
import { longLabel, monthLong, parts, weekdayShort, type LocalDate } from '../core/dates';
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
  };
  const isThisMonth = month.y === parts(today).y && month.m === parts(today).m;
  const headers = Array.from({ length: 7 }, (_, i) => ((weekStart - 1 + i) % 7) + 1);
  const doneCount = dayItems.filter((i) => i.done).length;

  return (
    <div className="flex flex-col">
      <ScreenHeader title="Calendar" right={topBar} />
      <div className="flex flex-col gap-4 px-4 pb-32">
        <section className="-mx-2 flex flex-col gap-2 rounded-[20px] bg-surface p-2 sm:mx-0 sm:p-3">
          <div className="flex items-center gap-1">
            <button type="button" aria-label="Previous month" onClick={() => shift(-1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronLeft" size={18} />
            </button>
            <h2 className="m-0 flex-1 text-center text-base font-semibold" aria-live="polite">
              {monthLong(month.m)} {month.y}
            </h2>
            <button type="button" aria-label="Next month" onClick={() => shift(1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
              <Icon name="chevronRight" size={18} />
            </button>
          </div>
          {!isThisMonth && (
            <button type="button" onClick={goToday} className="mx-auto h-9 rounded-full px-4 text-sm font-semibold text-primary hover:bg-surface2">
              Back to today
            </button>
          )}
          <div className="grid grid-cols-7 overflow-hidden rounded-xl border border-divider" role="grid" aria-label={`${monthLong(month.m)} ${month.y}`}>
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
                        <span className={`overflow-hidden whitespace-nowrap sm:text-ellipsis ${i.done ? 'line-through' : ''}`}>{i.task.name}</span>
                      </span>
                    );
                  })}
                  {more > 0 && <span className="px-1 text-[10px] font-semibold text-text2 sm:text-xs">+{more} more</span>}
                </button>
              );
            })}
          </div>
        </section>

        <section className="flex flex-col gap-2" aria-labelledby="cal-day">
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
        </section>
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
