import { useEffect, useRef, useState } from 'react';
import { isoWeekday, monthLong, parts, weekdayLong, weekdayShort, type LocalDate } from '../core/dates';
import { describeRepeat } from '../core/describe';
import { newId, type Repeat, type RepeatType, type Subtask, type Task, type TimeOfDay } from '../core/model';
import { Icon, TASK_ICONS } from '../ui/icons';
import { TASK_COLORS, taskColors } from '../ui/palette';

interface Props {
  task: Task | null;
  preset?: Partial<Task>;
  today: LocalDate;
  dark: boolean;
  nextSortOrder: number;
  onSave: (t: Task) => void;
  onCancel: () => void;
}

type Preset = { key: string; label: string; make: (today: LocalDate) => Repeat };

const PRESETS: Preset[] = [
  { key: 'someday', label: 'No date yet', make: () => ({ type: 'SOMEDAY' }) },
  { key: 'none', label: 'Does not repeat', make: (t) => ({ type: 'NONE', dueDate: t }) },
  { key: 'daily', label: 'Every day', make: (t) => ({ type: 'DAILY', interval: 1, startDate: t }) },
  { key: 'weekdays', label: 'Weekdays', make: (t) => ({ type: 'WEEKLY', daysOfWeek: [1, 2, 3, 4, 5], startDate: t }) },
  { key: 'weekly', label: 'Specific days', make: (t) => ({ type: 'WEEKLY', daysOfWeek: [isoWeekday(t)], startDate: t }) },
  { key: 'everyN', label: 'Every few days', make: (t) => ({ type: 'DAILY', interval: 3, startDate: t }) },
  { key: 'quota', label: 'Times a week', make: (t) => ({ type: 'QUOTA', count: 3, period: 'WEEK', startDate: t }) },
  { key: 'monthly', label: 'Monthly', make: (t) => ({ type: 'MONTHLY', dayOfMonth: parts(t).d, startDate: t }) },
  { key: 'yearly', label: 'Yearly', make: (t) => ({ type: 'YEARLY', month: parts(t).m, dayOfMonth: parts(t).d, startDate: t }) },
  { key: 'after', label: 'After last done', make: (t) => ({ type: 'AFTER_DONE', interval: 7, startDate: t }) },
];

function presetKey(r: Repeat): string {
  switch (r.type) {
    case 'SOMEDAY':
      return 'someday';
    case 'NONE':
      return 'none';
    case 'DAILY':
      return (r.interval ?? 1) <= 1 ? 'daily' : 'everyN';
    case 'WEEKLY':
      return [...(r.daysOfWeek ?? [])].sort().join() === '1,2,3,4,5' ? 'weekdays' : 'weekly';
    case 'QUOTA':
      return 'quota';
    case 'MONTHLY':
      return 'monthly';
    case 'YEARLY':
      return 'yearly';
    case 'AFTER_DONE':
      return 'after';
  }
}

const TIMES: { key: TimeOfDay; label: string }[] = [
  { key: 'MORNING', label: 'Morning' },
  { key: 'AFTERNOON', label: 'Afternoon' },
  { key: 'EVENING', label: 'Evening' },
  { key: 'ANYTIME', label: 'Anytime' },
];

const field = 'box-border rounded-xl border border-outline bg-surface px-3.5 outline-none focus:border-2 focus:border-primary';
const chip = (on: boolean) => `h-9 rounded-[10px] px-3.5 text-sm ${on ? 'bg-primary-container font-semibold text-on-primary-container' : 'border border-outline hover:bg-surface2'}`;

export function Editor({ task, preset, today, dark, nextSortOrder, onSave, onCancel }: Props) {
  const init: Task = task ?? {
    id: newId(),
    name: '',
    description: '',
    icon: 'check',
    color: 'teal',
    timeOfDay: 'ANYTIME',
    time: null,
    repeat: { type: 'DAILY', interval: 1, startDate: today },
    subtasks: [],
    reminders: [],
    sortOrder: nextSortOrder,
    createdAt: Date.now(),
    ...preset,
  };
  if (!task && preset?.repeat) init.repeat = { startDate: today, ...preset.repeat };
  const [t, setT] = useState<Task>(init);
  const [iconsOpen, setIconsOpen] = useState(false);
  const [more, setMore] = useState(Boolean(init.repeat.endDate));
  const [newSub, setNewSub] = useState('');
  const nameRef = useRef<HTMLInputElement>(null);
  useEffect(() => nameRef.current?.focus(), []);

  const set = (p: Partial<Task>) => setT((x) => ({ ...x, ...p }));
  const setR = (p: Partial<Repeat>) => setT((x) => ({ ...x, repeat: { ...x.repeat, ...p } }));
  const r = t.repeat;
  const { tint, fg } = taskColors(t.color, dark);

  const addSub = () => {
    if (!newSub.trim()) return;
    set({ subtasks: [...t.subtasks, { id: newId(), title: newSub.trim() }] });
    setNewSub('');
  };
  const moveSub = (i: number, d: number) => {
    const s = [...t.subtasks];
    const j = i + d;
    if (j < 0 || j >= s.length) return;
    [s[i], s[j]] = [s[j], s[i]];
    set({ subtasks: s });
  };
  const editSub = (i: number, p: Partial<Subtask>) => set({ subtasks: t.subtasks.map((s, k) => (k === i ? { ...s, ...p } : s)) });

  const save = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!t.name.trim()) return;
    onSave({ ...t, name: t.name.trim(), description: t.description.trim(), subtasks: t.subtasks.filter((s) => s.title.trim()) });
  };

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') onCancel();
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) save();
  };

  return (
    <form onSubmit={save} onKeyDown={onKey} className="flex h-full flex-col bg-surface" aria-label={task ? 'Edit task' : 'New task'}>
      <header className="flex h-16 flex-none items-center gap-1 border-b border-divider px-2">
        <button type="button" aria-label="Close" onClick={onCancel} className="flex size-12 items-center justify-center rounded-full text-text2 hover:bg-surface2">
          <Icon name="close" size={22} />
        </button>
        <h1 className="m-0 flex-1 text-xl font-semibold">{task ? 'Edit task' : 'New task'}</h1>
        <button type="submit" disabled={!t.name.trim()} className="mr-2 h-10 rounded-full bg-primary px-5 text-sm font-semibold text-on-primary disabled:opacity-40">
          Save
        </button>
      </header>

      <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-4 py-5">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="task-name" className="text-[13px] font-semibold text-primary">
            Name
          </label>
          <input id="task-name" ref={nameRef} value={t.name} onChange={(e) => set({ name: e.target.value })} className={`${field} h-[52px] text-[17px]`} autoComplete="off" />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="task-desc" className="text-[13px] font-semibold text-text2">
            Description
          </label>
          <textarea id="task-desc" rows={2} value={t.description} onChange={(e) => set({ description: e.target.value })} className={`${field} resize-none py-3 text-[15px] leading-snug`} />
          <span className="text-xs text-text2">Shown under the task name on Today</span>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <button type="button" aria-label={`Choose icon, currently ${t.icon}`} aria-expanded={iconsOpen} onClick={() => setIconsOpen(!iconsOpen)} className="flex size-14 items-center justify-center rounded-2xl" style={{ background: tint, color: fg }}>
              <Icon name={t.icon} size={26} />
            </button>
            {iconsOpen && (
              <div className="absolute left-0 top-16 z-20 grid w-64 grid-cols-6 gap-1 rounded-2xl bg-surface p-2 shadow-lg ring-1 ring-divider">
                {TASK_ICONS.map((i) => (
                  <button key={i} type="button" aria-label={i} aria-pressed={t.icon === i} onClick={() => { set({ icon: i }); setIconsOpen(false); }} className={`flex size-10 items-center justify-center rounded-xl ${t.icon === i ? 'bg-primary-container text-on-primary-container' : 'hover:bg-surface2'}`}>
                    <Icon name={i} />
                  </button>
                ))}
              </div>
            )}
          </div>
          <div role="radiogroup" aria-label="Color" className="flex gap-2">
            {Object.entries(TASK_COLORS).map(([k, c]) => (
              <button key={k} type="button" role="radio" aria-checked={t.color === k} aria-label={c.name} onClick={() => set({ color: k })} className="size-11 rounded-full" style={{ background: c.swatch, boxShadow: t.color === k ? '0 0 0 3px var(--surface), 0 0 0 5px var(--text)' : undefined }} />
            ))}
          </div>
        </div>

        <section className="flex flex-col gap-2.5">
          <h2 className="m-0 text-[13px] font-semibold text-text2">Repeats</h2>
          <div className="flex flex-wrap gap-2">
            {PRESETS.map((p) => {
              const on = presetKey(r) === p.key;
              return (
                <button key={p.key} type="button" aria-pressed={on} className={chip(on)} onClick={() => !on && set({ repeat: { ...p.make(today), startDate: p.key === 'none' || p.key === 'someday' ? undefined : (r.startDate ?? today) } })}>
                  {p.label}
                </button>
              );
            })}
          </div>
          <RuleFields r={r} setR={setR} />
          <p className="m-0 text-sm text-text2" aria-live="polite">
            {describeRepeat(r)}.{' '}
            {r.type === 'NONE' && 'If it is not done, it moves to the next day until it is.'}
            {r.type === 'SOMEDAY' && 'It waits in the Someday list (Tasks tab) until you give it a date.'}
            {r.type === 'AFTER_DONE' && 'The next one is due that many days after you last tick it.'}
          </p>
        </section>

        <section className="flex flex-col gap-1.5">
          <h2 className="m-0 text-[13px] font-semibold text-text2">Subtasks</h2>
          <ul className="m-0 flex list-none flex-col rounded-xl border border-divider p-0">
            {t.subtasks.map((s, i) => (
              <li key={s.id} className="flex min-h-12 items-center gap-1 border-b border-divider pl-3 pr-1">
                <label className="sr-only" htmlFor={`sub-${s.id}`}>
                  Subtask {i + 1}
                </label>
                <input id={`sub-${s.id}`} value={s.title} onChange={(e) => editSub(i, { title: e.target.value })} className="h-11 flex-1 bg-transparent text-[15px] outline-none" />
                <button type="button" aria-label={`Move ${s.title} up`} disabled={i === 0} onClick={() => moveSub(i, -1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2 disabled:opacity-30">
                  <Icon name="chevronDown" size={16} className="rotate-180" />
                </button>
                <button type="button" aria-label={`Move ${s.title} down`} disabled={i === t.subtasks.length - 1} onClick={() => moveSub(i, 1)} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2 disabled:opacity-30">
                  <Icon name="chevronDown" size={16} />
                </button>
                <button type="button" aria-label={`Remove ${s.title}`} onClick={() => set({ subtasks: t.subtasks.filter((_, k) => k !== i) })} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
                  <Icon name="close" size={16} />
                </button>
              </li>
            ))}
            <li className="flex min-h-12 items-center gap-2 pl-3 pr-1">
              <Icon name="plus" size={18} className="text-primary" />
              <label htmlFor="new-sub" className="sr-only">
                Add subtask
              </label>
              <input
                id="new-sub"
                value={newSub}
                placeholder="Add subtask"
                onChange={(e) => setNewSub(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addSub();
                  }
                }}
                onBlur={addSub}
                className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:font-semibold placeholder:text-primary"
              />
            </li>
          </ul>
          {t.subtasks.length > 0 && <span className="text-xs text-text2">Ticking the last subtask completes the task.{r.type !== 'NONE' && r.type !== 'SOMEDAY' && ' Subtasks reset each day.'}</span>}
        </section>

        <section className="flex flex-col gap-2.5">
          <h2 className="m-0 text-[13px] font-semibold text-text2">Time of day</h2>
          <div role="radiogroup" aria-label="Time of day" className="grid grid-cols-4 overflow-hidden rounded-xl border border-outline">
            {TIMES.map((x, i) => (
              <button key={x.key} type="button" role="radio" aria-checked={t.timeOfDay === x.key} onClick={() => set({ timeOfDay: x.key })} className={`h-12 text-sm ${i < 3 ? 'border-r border-outline' : ''} ${t.timeOfDay === x.key ? 'bg-primary-container font-semibold text-on-primary-container' : 'hover:bg-surface2'}`}>
                {x.label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-3">
            <label htmlFor="task-time" className="text-sm">
              Exact time (optional)
            </label>
            <input id="task-time" type="time" value={t.time ?? ''} onChange={(e) => set({ time: e.target.value || null })} className={`${field} h-11`} />
          </div>
        </section>

        <section className="flex flex-col gap-2 rounded-2xl bg-surface2 p-4">
          <div className="flex items-center gap-3">
            <Icon name="bell" className="text-text2" />
            <div className="flex flex-1 flex-col">
              <span className="text-base">Reminders</span>
              <span className="text-[13px] text-text2">They ring on your phone, not in the browser</span>
            </div>
          </div>
          {t.reminders.map((rm, i) => (
            <div key={i} className="flex items-center gap-2 pl-9">
              <label className="sr-only" htmlFor={`rem-${i}`}>
                Reminder {i + 1}
              </label>
              <input id={`rem-${i}`} type="time" value={rm.localTime} onChange={(e) => set({ reminders: t.reminders.map((x, k) => (k === i ? { ...x, localTime: e.target.value } : x)) })} className={`${field} h-11`} />
              <button type="button" aria-label="Remove reminder" onClick={() => set({ reminders: t.reminders.filter((_, k) => k !== i) })} className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface">
                <Icon name="close" size={16} />
              </button>
            </div>
          ))}
          <button type="button" onClick={() => set({ reminders: [...t.reminders, { localTime: t.time ?? '09:00', enabled: true }] })} className="flex min-h-12 items-center gap-2 pl-9 text-left text-sm font-semibold text-primary">
            <Icon name="plus" size={18} strokeWidth={2.2} />
            Add reminder
          </button>
        </section>

        <section className="flex flex-col gap-3">
          <button type="button" aria-expanded={more} onClick={() => setMore(!more)} className="flex min-h-12 items-center justify-between text-left">
            <span className="flex flex-col">
              <span className="text-[15px] font-medium">More options</span>
              <span className="text-[13px] text-text2">Start and end date</span>
            </span>
            <Icon name="chevronDown" className={`text-text2 transition-transform ${more ? 'rotate-180' : ''}`} />
          </button>
          {more && r.type !== 'NONE' && r.type !== 'SOMEDAY' && (
            <div className="flex flex-wrap gap-4">
              <label className="flex flex-col gap-1 text-[13px] font-semibold text-text2">
                Starts
                <input type="date" value={r.startDate ?? ''} onChange={(e) => setR({ startDate: e.target.value || undefined })} className={`${field} h-11 font-normal text-text`} />
              </label>
              <label className="flex flex-col gap-1 text-[13px] font-semibold text-text2">
                Ends (optional)
                <input type="date" value={r.endDate ?? ''} onChange={(e) => setR({ endDate: e.target.value || undefined })} className={`${field} h-11 font-normal text-text`} />
              </label>
            </div>
          )}
          {more && r.type === 'NONE' && <p className="m-0 text-sm text-text2">One-off tasks only need a due date.</p>}
        </section>
      </div>
    </form>
  );
}

function RuleFields({ r, setR }: { r: Repeat; setR: (p: Partial<Repeat>) => void }) {
  const num = (value: number | undefined, onChange: (n: number) => void, label: string, min = 1, max = 365) => (
    <input type="number" aria-label={label} min={min} max={max} value={value ?? min} onChange={(e) => onChange(Math.max(min, Math.min(max, Number(e.target.value) || min)))} className={`${field} h-11 w-20`} />
  );
  const type: RepeatType = r.type;
  if (type === 'NONE')
    return (
      <label className="flex items-center gap-3 text-sm">
        Due
        <input type="date" value={r.dueDate ?? ''} onChange={(e) => e.target.value && setR({ dueDate: e.target.value })} className={`${field} h-11`} />
      </label>
    );
  if (type === 'DAILY' && (r.interval ?? 1) > 1)
    return <div className="flex items-center gap-2 text-sm">Every {num(r.interval, (n) => setR({ interval: Math.max(2, n) }), 'Number of days', 2)} days</div>;
  if (type === 'AFTER_DONE') return <div className="flex items-center gap-2 text-sm">{num(r.interval, (n) => setR({ interval: n }), 'Days after last done')} days after I last do it</div>;
  if (type === 'WEEKLY')
    return (
      <div className="flex flex-wrap gap-1.5" role="group" aria-label="Days of the week">
        {[1, 2, 3, 4, 5, 6, 7].map((d) => {
          const on = (r.daysOfWeek ?? []).includes(d);
          return (
            <button key={d} type="button" aria-pressed={on} aria-label={weekdayLong(d)} onClick={() => {
              const s = new Set(r.daysOfWeek ?? []);
              if (on) s.delete(d);
              else s.add(d);
              if (s.size) setR({ daysOfWeek: [...s].sort() });
            }} className={`size-11 rounded-full text-sm ${on ? 'bg-primary font-semibold text-on-primary' : 'border border-outline hover:bg-surface2'}`}>
              {weekdayShort(d).slice(0, 2)}
            </button>
          );
        })}
      </div>
    );
  if (type === 'QUOTA')
    return (
      <div className="flex items-center gap-2 text-sm">
        {num(r.count, (n) => setR({ count: n }), 'Times', 1, 31)} times a
        <select aria-label="Period" value={r.period ?? 'WEEK'} onChange={(e) => setR({ period: e.target.value as 'WEEK' | 'MONTH' })} className={`${field} h-11`}>
          <option value="WEEK">week</option>
          <option value="MONTH">month</option>
        </select>
        <span className="text-text2">on any days</span>
      </div>
    );
  if (type === 'MONTHLY') {
    const byWeekday = Boolean(r.ordinal && r.weekday);
    return (
      <div className="flex flex-col gap-2 text-sm">
        <div className="flex gap-2">
          <button type="button" className={chip(!byWeekday)} aria-pressed={!byWeekday} onClick={() => setR({ ordinal: undefined, weekday: undefined, dayOfMonth: r.dayOfMonth ?? 1 })}>By date</button>
          <button type="button" className={chip(byWeekday)} aria-pressed={byWeekday} onClick={() => setR({ ordinal: 1, weekday: 1 })}>By weekday</button>
        </div>
        {byWeekday ? (
          <div className="flex items-center gap-2">
            The
            <select aria-label="Which" value={r.ordinal} onChange={(e) => setR({ ordinal: Number(e.target.value) })} className={`${field} h-11`}>
              <option value={1}>first</option>
              <option value={2}>second</option>
              <option value={3}>third</option>
              <option value={4}>fourth</option>
              <option value={-1}>last</option>
            </select>
            <select aria-label="Weekday" value={r.weekday} onChange={(e) => setR({ weekday: Number(e.target.value) })} className={`${field} h-11`}>
              {[1, 2, 3, 4, 5, 6, 7].map((d) => <option key={d} value={d}>{weekdayLong(d)}</option>)}
            </select>
          </div>
        ) : (
          <div className="flex items-center gap-2">On day {num(r.dayOfMonth, (n) => setR({ dayOfMonth: n }), 'Day of month', 1, 31)} <span className="text-text2">(short months use their last day)</span></div>
        )}
      </div>
    );
  }
  if (type === 'YEARLY')
    return (
      <div className="flex items-center gap-2 text-sm">
        On {num(r.dayOfMonth, (n) => setR({ dayOfMonth: n }), 'Day', 1, 31)}
        <select aria-label="Month" value={r.month ?? 1} onChange={(e) => setR({ month: Number(e.target.value) })} className={`${field} h-11`}>
          {Array.from({ length: 12 }, (_, i) => <option key={i} value={i + 1}>{monthLong(i + 1)}</option>)}
        </select>
      </div>
    );
  return null;
}
