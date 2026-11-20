import { DEFAULT_SETTINGS, type Completion, type Settings, type Task } from '../core/model';

export type SyncState = 'synced' | 'syncing' | 'offline' | 'local';

export interface UserInfo {
  uid: string;
  name: string;
  email: string;
  photoURL?: string | null;
}

export interface AppState {
  ready: boolean;
  user: UserInfo | null;
  tasks: Task[];
  completions: Completion[];
  settings: Settings;
  sync: SyncState;
  error?: string;
}

/** Storage backend: Firestore (synced) or the browser (local test mode). */
export interface Backend {
  readonly mode: 'firebase' | 'local';
  start(onState: (patch: Partial<AppState>) => void): void;
  signIn(): Promise<void>;
  signOut(): Promise<void>;
  saveTask(task: Task): Promise<void>;
  /** Soft delete, so an offline device learns about it. */
  deleteTask(id: string): Promise<void>;
  saveCompletion(c: Completion): Promise<void>;
  removeCompletion(id: string): Promise<void>;
  saveSettings(s: Settings): Promise<void>;
}

export class AppStore {
  private state: AppState = { ready: false, user: null, tasks: [], completions: [], settings: DEFAULT_SETTINGS, sync: 'synced' };
  private listeners = new Set<() => void>();
  readonly backend: Backend;

  constructor(backend: Backend) {
    this.backend = backend;
    backend.start((patch) => {
      this.state = { ...this.state, ...patch };
      this.listeners.forEach((l) => l());
    });
  }

  getState = () => this.state;

  subscribe = (l: () => void) => {
    this.listeners.add(l);
    return () => this.listeners.delete(l);
  };

  /** Optimistic local update so the UI never waits on the network. */
  private patchLocal(patch: Partial<AppState>) {
    this.state = { ...this.state, ...patch };
    this.listeners.forEach((l) => l());
  }

  async saveTask(task: Task) {
    const others = this.state.tasks.filter((t) => t.id !== task.id);
    this.patchLocal({ tasks: [...others, task] });
    await this.backend.saveTask(task);
  }

  async deleteTask(id: string) {
    this.patchLocal({ tasks: this.state.tasks.filter((t) => t.id !== id) });
    await this.backend.deleteTask(id);
  }

  async saveCompletion(c: Completion) {
    const others = this.state.completions.filter((x) => x.id !== c.id);
    this.patchLocal({ completions: [...others, c] });
    await this.backend.saveCompletion(c);
  }

  async removeCompletion(id: string) {
    this.patchLocal({ completions: this.state.completions.filter((x) => x.id !== id) });
    await this.backend.removeCompletion(id);
  }

  async saveSettings(s: Settings) {
    this.patchLocal({ settings: s });
    await this.backend.saveSettings(s);
  }
}

export function deviceId(): string {
  try {
    let id = localStorage.getItem('dt.deviceId');
    if (!id) {
      id = `web-${Math.random().toString(36).slice(2, 10)}`;
      localStorage.setItem('dt.deviceId', id);
    }
    return id;
  } catch {
    return 'web';
  }
}
