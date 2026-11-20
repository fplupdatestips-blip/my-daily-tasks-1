import { initializeApp, type FirebaseOptions } from 'firebase/app';
import { GoogleAuthProvider, getAuth, onAuthStateChanged, signInWithPopup, signOut, type Auth } from 'firebase/auth';
import {
  collection,
  deleteDoc,
  doc,
  initializeFirestore,
  onSnapshot,
  persistentLocalCache,
  persistentMultipleTabManager,
  query,
  serverTimestamp,
  setDoc,
  where,
  type Firestore,
  type Unsubscribe,
} from 'firebase/firestore';
import { addDays, logicalToday } from '../core/dates';
import { DEFAULT_SETTINGS, type Completion, type Settings, type Task } from '../core/model';
import type { AppState, Backend, SyncState } from './store';

// Data lives at users/{uid}/tasks, users/{uid}/completions and
// users/{uid}/settings/main. The Android app uses the same paths.

/** How far back completions are loaded (enough for a year of history). */
const HISTORY_DAYS = 400;

export function firebaseConfigFromEnv(): FirebaseOptions | null {
  const env = import.meta.env;
  if (!env.VITE_FIREBASE_API_KEY || !env.VITE_FIREBASE_PROJECT_ID) return null;
  return {
    apiKey: env.VITE_FIREBASE_API_KEY,
    authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
    projectId: env.VITE_FIREBASE_PROJECT_ID,
    storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
    appId: env.VITE_FIREBASE_APP_ID,
  };
}

export class FirebaseBackend implements Backend {
  readonly mode = 'firebase' as const;
  private auth: Auth;
  private db: Firestore;
  private uid: string | null = null;
  private unsubs: Unsubscribe[] = [];
  private pending = { tasks: false, completions: false };
  private fromCache = false;
  private emit: (p: Partial<AppState>) => void = () => {};

  constructor(config: FirebaseOptions) {
    const app = initializeApp(config);
    this.auth = getAuth(app);
    this.db = initializeFirestore(app, {
      localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }),
      ignoreUndefinedProperties: true,
    });
  }

  start(onState: (patch: Partial<AppState>) => void) {
    this.emit = onState;
    window.addEventListener('online', () => this.pushSync());
    window.addEventListener('offline', () => this.pushSync());
    onAuthStateChanged(this.auth, (user) => {
      this.unsubs.forEach((u) => u());
      this.unsubs = [];
      this.uid = user?.uid ?? null;
      if (!user) {
        this.emit({ ready: true, user: null, tasks: [], completions: [] });
        return;
      }
      this.emit({
        ready: true,
        user: { uid: user.uid, name: user.displayName ?? 'You', email: user.email ?? '', photoURL: user.photoURL },
      });
      this.listen(user.uid);
    });
  }

  private pushSync() {
    let sync: SyncState = 'synced';
    if (!navigator.onLine) sync = 'offline';
    else if (this.pending.tasks || this.pending.completions || this.fromCache) sync = 'syncing';
    this.emit({ sync });
  }

  private listen(uid: string) {
    const base = `users/${uid}`;
    this.unsubs.push(
      onSnapshot(collection(this.db, `${base}/tasks`), { includeMetadataChanges: true }, (snap) => {
        this.pending.tasks = snap.metadata.hasPendingWrites;
        this.fromCache = snap.metadata.fromCache;
        const tasks = snap.docs.map((d) => ({ ...(d.data() as Task), id: d.id })).filter((t) => !t.deleted);
        this.emit({ tasks });
        this.pushSync();
      }, (e) => this.emit({ error: e.message })),
    );
    const since = addDays(logicalToday(), -HISTORY_DAYS);
    this.unsubs.push(
      onSnapshot(query(collection(this.db, `${base}/completions`), where('date', '>=', since)), { includeMetadataChanges: true }, (snap) => {
        this.pending.completions = snap.metadata.hasPendingWrites;
        const completions = snap.docs.map((d) => ({ ...(d.data() as Completion), id: d.id }));
        this.emit({ completions });
        this.pushSync();
      }, (e) => this.emit({ error: e.message })),
    );
    this.unsubs.push(
      onSnapshot(doc(this.db, `${base}/settings/main`), (snap) => {
        this.emit({ settings: { ...DEFAULT_SETTINGS, ...(snap.data() as Partial<Settings> | undefined) } });
      }),
    );
  }

  private path(...p: string[]) {
    if (!this.uid) throw new Error('Not signed in');
    return ['users', this.uid, ...p].join('/');
  }

  async signIn() {
    await signInWithPopup(this.auth, new GoogleAuthProvider());
  }

  async signOut() {
    await signOut(this.auth);
  }

  // Writes are not awaited for the server: with offline persistence the
  // promise only resolves once the server confirms, so we fire and forget
  // and let the snapshot listeners show pending state.
  async saveTask(task: Task) {
    void setDoc(doc(this.db, this.path('tasks', task.id)), { ...task, updatedAt: serverTimestamp(), deleted: false });
  }

  async deleteTask(id: string) {
    void setDoc(doc(this.db, this.path('tasks', id)), { deleted: true, updatedAt: serverTimestamp() }, { merge: true });
  }

  async saveCompletion(c: Completion) {
    void setDoc(doc(this.db, this.path('completions', c.id)), { ...c, updatedAt: serverTimestamp() });
  }

  async removeCompletion(id: string) {
    void deleteDoc(doc(this.db, this.path('completions', id)));
  }

  async saveSettings(s: Settings) {
    void setDoc(doc(this.db, this.path('settings', 'main')), { ...s, updatedAt: serverTimestamp() }, { merge: true });
  }
}
