import { useState } from 'react';
import { describeRepeat, formatTime } from '../core/describe';
import type { TodayItem } from '../core/today';
import { Icon } from '../ui/icons';
import { TaskIcon } from '../ui/TaskIcon';

interface Props {
  item: TodayItem;
  dark: boolean;
  selected?: boolean;
  onToggle: () => void;
  onSubtask: (id: string) => void;
  onOpen: () => void;
  onSkip: () => void;
  onEdit: () => void;
}

export function TaskCard({ item, dark, selected, onToggle, onSubtask, onOpen, onSkip, onEdit }: Props) {
  const { task, done, skipped } = item;
  const subs = task.subtasks;
  const subsDone = subs.filter((s) => item.subtasksDone.includes(s.id)).length;
  const [open, setOpen] = useState(subsDone > 0 && !done);
  const [menu, setMenu] = useState(false);
  const meta = [describeRepeat(task.repeat, true), task.time ? formatTime(task.time) : null].filter(Boolean).join(' · ');
  const stateLabel = done ? 'done' : skipped ? 'skipped' : 'not done';

  return (
    <div
      className={`group relative rounded-2xl bg-surface ${selected ? 'ring-2 ring-primary' : ''}`}
      onContextMenu={(e) => {
        e.preventDefault();
        setMenu(true);
      }}
    >
      <div className="flex items-center gap-3 py-3 pl-3 pr-2">
        <TaskIcon task={task} dark={dark} />
        <button type="button" onClick={onOpen} className="flex min-w-0 flex-1 flex-col gap-0.5 text-left">
          {item.late && !done && (
            <span className="self-start rounded-md bg-amber-tint px-1.5 py-0.5 text-[11px] font-semibold tracking-wide text-streak">{item.late}</span>
          )}
          <span className={`text-base font-medium ${done ? 'text-text2 line-through' : ''}`}>{task.name}</span>
          {task.description && <span className="line-clamp-3 text-[13px] leading-snug text-text2">{task.description}</span>}
          <span className="flex flex-wrap items-center gap-x-2.5 text-[13px] text-text2">
            <span>{skipped ? 'Skipped' : meta}</span>
            {subs.length > 0 && (
              <span className="inline-flex items-center gap-0.5 font-semibold text-text">
                <Icon name="subtasks" size={14} strokeWidth={2} />
                {subsDone} of {subs.length}
              </span>
            )}
            {item.streak > 0 && (
              <span className="inline-flex items-center gap-0.5 font-semibold text-streak" aria-label={`${item.streak} day streak`}>
                <Icon name="flame" size={14} strokeWidth={2} />
                {item.streak}
              </span>
            )}
          </span>
        </button>
        <div className="hidden flex-none items-center [@media(hover:hover)]:group-hover:flex">
          <button type="button" onClick={onSkip} aria-label={skipped ? `Unskip ${task.name}` : `Skip ${task.name} today`} title="Skip (S)" className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name="skip" size={18} />
          </button>
          {!task.auto && <button type="button" onClick={onEdit} aria-label={`Edit ${task.name}`} title="Edit (E)" className="flex size-10 items-center justify-center rounded-full text-text2 hover:bg-surface2">
            <Icon name="edit" size={18} />
          </button>}
        </div>
        <button
          type="button"
          onClick={onToggle}
          aria-pressed={done}
          aria-label={`${task.name}, ${stateLabel}. ${done ? 'Mark not done' : 'Mark done'}`}
          className="flex size-12 flex-none items-center justify-center rounded-full"
        >
          <span
            className={`flex size-8 items-center justify-center rounded-full transition-colors duration-200 ${done ? 'bg-done text-on-done' : 'border-2 border-outline'}`}
          >
            {done && <Icon name="check" size={18} strokeWidth={3} />}
          </span>
        </button>
      </div>
      {subs.length > 0 && (
        <button
          type="button"
          onClick={() => setOpen(!open)}
          aria-expanded={open}
          aria-label={`${open ? 'Hide' : 'Show'} subtasks of ${task.name}, ${subsDone} of ${subs.length} done`}
          className="-mt-2 ml-16 flex h-9 items-center gap-1 rounded-full px-2 text-[13px] font-semibold text-primary hover:bg-surface2"
        >
          {open ? 'Hide subtasks' : 'Show subtasks'}
          <Icon name="chevronDown" size={16} className={`transition-transform ${open ? 'rotate-180' : ''}`} />
        </button>
      )}
      {subs.length > 0 && open && (
        <ul aria-label={`Subtasks of ${task.name}`} className="flex flex-col pb-2 pl-16 pr-2">
          {subs.map((s) => {
            const d = item.subtasksDone.includes(s.id);
            return (
              <li key={s.id} className="flex min-h-10 items-center gap-2.5 border-t border-divider">
                <button type="button" onClick={() => onSubtask(s.id)} aria-pressed={d} aria-label={`${d ? 'Mark not done' : 'Mark done'}: ${s.title}`} className="flex size-10 flex-none items-center justify-center">
                  <span className={`flex size-[22px] items-center justify-center rounded-md ${d ? 'bg-done text-on-done' : 'border-2 border-outline'}`}>
                    {d && <Icon name="check" size={13} strokeWidth={3} />}
                  </span>
                </button>
                <span className={`text-sm ${d ? 'text-text2 line-through' : ''}`}>{s.title}</span>
              </li>
            );
          })}
        </ul>
      )}
      {menu && (
        <div className="absolute right-2 top-12 z-20 flex min-w-40 flex-col rounded-xl bg-surface py-1 shadow-lg ring-1 ring-divider" role="menu" onMouseLeave={() => setMenu(false)}>
          {[
            [done ? 'Mark not done' : 'Mark done', onToggle],
            [skipped ? 'Unskip' : 'Skip today', onSkip],
            ...(task.auto ? [] : [['Edit', onEdit]]),
            ['Details', onOpen],
          ].map(([label, fn]) => (
            <button key={label as string} role="menuitem" type="button" className="px-4 py-2.5 text-left text-sm hover:bg-surface2" onClick={() => { setMenu(false); (fn as () => void)(); }}>
              {label as string}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
