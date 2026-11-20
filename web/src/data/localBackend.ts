import { DEFAULT_SETTINGS, type Completion, type Settings, type Task } from '../core/model';
import type { AppState, Backend } from './store';

// Local test mode: used when no Firebase config is set. Data stays in this
// browser's localStorage only and is not synced anywhere.

const KEY = 'dt.local.v1';

interface Saved {
  tasks: Task[];
  completions: Completion[];
  settings: Settings;
  signedIn: boolean;
}

function load(): Saved {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return { settings: DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    /* fall through */
  }
  return { tasks: [], completions: [], settings: DEFAULT_SETTINGS, signedIn: false };
}

export class LocalBackend implements Backend {
  readonly mode = 'local' as const;
  private data = load();
  private emit: (p: Partial<AppState>) => void = () => {};

  start(onState: (patch: Partial<AppState>) => void) {
    this.emit = onState;
    this.push();
  }

  private persist() {
    try {
      localStorage.setItem(KEY, JSON.stringify(this.data));
    } catch {
      /* storage full or blocked: keep working in memory */
    }
  }

  private push() {
    this.emit({
      ready: true,
      user: this.data.signedIn ? { uid: 'local', name: 'Test mode', email: 'Saved in this browser only' } : null,
      tasks: this.data.tasks.filter((t) => !t.deleted),
      completions: this.data.completions,
      settings: this.data.settings,
      sync: 'local',
    });
  }

  async signIn() {
    this.data.signedIn = true;
    this.persist();
    this.push();
  }

  async signOut() {
    this.data.signedIn = false;
    this.persist();
    this.push();
  }

  async saveTask(task: Task) {
    this.data.tasks = [...this.data.tasks.filter((t) => t.id !== task.id), { ...task, updatedAt: Date.now() }];
    this.persist();
  }

  async deleteTask(id: string) {
    this.data.tasks = this.data.tasks.filter((t) => t.id !== id);
    this.data.completions = this.data.completions.filter((c) => c.taskId !== id);
    this.persist();
  }

  async saveCompletion(c: Completion) {
    this.data.completions = [...this.data.completions.filter((x) => x.id !== c.id), c];
    this.persist();
  }

  async removeCompletion(id: string) {
    this.data.completions = this.data.completions.filter((x) => x.id !== id);
    this.persist();
  }

  async saveSettings(s: Settings) {
    this.data.settings = s;
    this.persist();
  }
}
