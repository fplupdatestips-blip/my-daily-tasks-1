import type { Task } from '../core/model';
import { Icon } from './icons';
import { taskColors } from './palette';

/**
 * The coloured square for a task: the person's own picture when they
 * uploaded one, otherwise the chosen icon on the task colour.
 */
export function TaskIcon({ task, dark, size = 40, iconSize = 20, radius = '0.75rem', className = '' }: { task: Pick<Task, 'icon' | 'color' | 'image'>; dark: boolean; size?: number; iconSize?: number; radius?: string; className?: string }) {
  const { tint, fg } = taskColors(task.color, dark);
  return (
    <span className={`flex flex-none items-center justify-center overflow-hidden ${className}`} style={{ width: size, height: size, borderRadius: radius, background: tint, color: fg }}>
      {task.image ? <img src={task.image} alt="" className="size-full object-cover" draggable={false} /> : <Icon name={task.icon} size={iconSize} />}
    </span>
  );
}

/** Crops the picture to a square and shrinks it to 128px so it fits in the synced task. */
export async function imageFileToIcon(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 128;
  const ctx = canvas.getContext('2d')!;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, 128, 128);
  bitmap.close();
  const webp = canvas.toDataURL('image/webp', 0.85);
  return webp.startsWith('data:image/webp') ? webp : canvas.toDataURL('image/png');
}
