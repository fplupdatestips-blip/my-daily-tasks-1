import type { Task } from '../core/model';

// Starter templates for the empty state and the editor. Same list on Android.
export const TEMPLATES: Partial<Task>[] = [
  { name: 'Drink water', description: '8 glasses through the day', icon: 'water', color: 'blue', timeOfDay: 'ANYTIME', repeat: { type: 'DAILY', interval: 1 } },
  { name: 'Take medicine', description: 'With breakfast', icon: 'pill', color: 'rose', timeOfDay: 'MORNING', repeat: { type: 'DAILY', interval: 1 } },
  { name: 'Go for a walk', description: '20 minutes outside', icon: 'walk', color: 'green', timeOfDay: 'AFTERNOON', repeat: { type: 'DAILY', interval: 1 } },
  { name: 'Read 10 pages', description: '', icon: 'book', color: 'purple', timeOfDay: 'EVENING', repeat: { type: 'DAILY', interval: 1 } },
  { name: 'Water plants', description: '', icon: 'leaf', color: 'green', timeOfDay: 'EVENING', repeat: { type: 'DAILY', interval: 3 } },
  { name: 'Exercise', description: '', icon: 'dumbbell', color: 'amber', timeOfDay: 'MORNING', repeat: { type: 'QUOTA', count: 3, period: 'WEEK' } },
];
