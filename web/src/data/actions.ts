import type { LocalDate } from '../core/dates';
import { completionId, type Completion, type Task } from '../core/model';
import type { TodayItem } from '../core/today';
import { deviceId, type AppStore } from './store';

// Completing, unticking, skipping and subtask ticks. Each returns an undo
// function that puts things back exactly as they were.

type Undo = () => void;

function snapshot(store: AppStore, task: Task, date: LocalDate): Undo {
  const id = completionId(task.id, date);
  const prevC = store.getState().completions.find((c) => c.id === id);
  const prevTask = store.getState().tasks.find((t) => t.id === task.id);
  return () => {
    if (prevC) void store.saveCompletion(prevC);
    else void store.removeCompletion(id);
    if (prevTask && task.repeat.type === 'NONE') void store.saveTask(prevTask);
  };
}

function completion(task: Task, date: LocalDate, status: Completion['status'], subtasksDone: string[]): Completion {
  return { id: completionId(task.id, date), taskId: task.id, date, status, subtasksDone, completedAt: Date.now(), deviceId: deviceId() };
}

export function toggleDone(store: AppStore, item: TodayItem, date: LocalDate): Undo {
  const undo = snapshot(store, item.task, date);
  const t = item.task;
  const oneOff = t.repeat.type === 'NONE';
  if (item.done) {
    void store.removeCompletion(completionId(t.id, date));
    if (oneOff && t.subtasks.length) void store.saveTask({ ...t, subtasks: t.subtasks.map((s) => ({ ...s, done: false })) });
  } else {
    void store.saveCompletion(completion(t, date, 'DONE', t.subtasks.map((s) => s.id)));
    if (oneOff && t.subtasks.length) void store.saveTask({ ...t, subtasks: t.subtasks.map((s) => ({ ...s, done: true })) });
  }
  return undo;
}

export function skip(store: AppStore, item: TodayItem, date: LocalDate): Undo {
  const undo = snapshot(store, item.task, date);
  if (item.skipped) void store.removeCompletion(completionId(item.task.id, date));
  else void store.saveCompletion(completion(item.task, date, 'SKIPPED', item.subtasksDone));
  return undo;
}

/** Ticking the last subtask completes the task; unticking one reopens it. */
export function toggleSubtask(store: AppStore, item: TodayItem, subId: string, date: LocalDate): { undo: Undo; completedTask: boolean } {
  const undo = snapshot(store, item.task, date);
  const t = item.task;
  const set = new Set(item.subtasksDone);
  if (set.has(subId)) set.delete(subId);
  else set.add(subId);
  const all = t.subtasks.length > 0 && t.subtasks.every((s) => set.has(s.id));
  const ids = t.subtasks.filter((s) => set.has(s.id)).map((s) => s.id);

  if (t.repeat.type === 'NONE') {
    void store.saveTask({ ...t, subtasks: t.subtasks.map((s) => ({ ...s, done: set.has(s.id) })) });
    if (all) void store.saveCompletion(completion(t, date, 'DONE', ids));
    else if (item.done) void store.removeCompletion(completionId(t.id, date));
  } else if (all) {
    void store.saveCompletion(completion(t, date, 'DONE', ids));
  } else if (ids.length) {
    void store.saveCompletion(completion(t, date, 'PARTIAL', ids));
  } else {
    void store.removeCompletion(completionId(t.id, date));
  }
  return { undo, completedTask: all && !item.done };
}

/** Backfill from the calendar: toggles DONE on a past day. */
export function toggleDay(store: AppStore, task: Task, date: LocalDate) {
  const id = completionId(task.id, date);
  const c = store.getState().completions.find((x) => x.id === id);
  if (c?.status === 'DONE') void store.removeCompletion(id);
  else void store.saveCompletion(completion(task, date, 'DONE', task.subtasks.map((s) => s.id)));
}
