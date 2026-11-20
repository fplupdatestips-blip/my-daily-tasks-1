import { longLabel, monthLong, weekdayLong, weekdayShort, type LocalDate } from './dates';
import type { Repeat } from './model';

const ORD = ['', '1st', '2nd', '3rd', '4th'];

function joinAnd(xs: string[]): string {
  if (xs.length <= 1) return xs.join('');
  return `${xs.slice(0, -1).join(', ')} and ${xs[xs.length - 1]}`;
}

/** Plain-English summary shown while editing and on the task card. */
export function describeRepeat(r: Repeat, short = false): string {
  switch (r.type) {
    case 'NONE':
      return short ? 'One-off' : r.dueDate ? `Once, on ${longLabel(r.dueDate)}` : 'Once';
    case 'DAILY':
      return (r.interval ?? 1) <= 1 ? 'Every day' : `Every ${r.interval} days`;
    case 'WEEKLY': {
      const d = [...(r.daysOfWeek ?? [])].sort();
      if (d.join() === '1,2,3,4,5') return 'Weekdays';
      if (d.join() === '6,7') return 'Weekends';
      if (d.length === 7) return 'Every day';
      if (d.length === 1) return short ? `${weekdayLong(d[0])}s` : `Every ${weekdayLong(d[0])}`;
      return `Every ${joinAnd(d.map(weekdayShort))}`;
    }
    case 'MONTHLY':
      if (r.ordinal && r.weekday) {
        const o = r.ordinal === -1 ? 'last' : ORD[r.ordinal];
        return `Monthly, ${o} ${weekdayLong(r.weekday)}`;
      }
      return `Monthly on the ${ordinalNum(r.dayOfMonth ?? 1)}`;
    case 'YEARLY':
      return `Every year on ${r.dayOfMonth ?? 1} ${monthLong(r.month ?? 1)}`;
    case 'QUOTA':
      return `${r.count ?? 1} ${(r.count ?? 1) === 1 ? 'time' : 'times'} a ${r.period === 'MONTH' ? 'month' : 'week'}`;
    case 'AFTER_DONE':
      return `${r.interval ?? 1} ${(r.interval ?? 1) === 1 ? 'day' : 'days'} after last done`;
  }
}

export function ordinalNum(n: number): string {
  const s = n % 100 >= 11 && n % 100 <= 13 ? 'th' : ['th', 'st', 'nd', 'rd'][n % 10] ?? 'th';
  return `${n}${s}`;
}

export function formatTime(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number);
  const ap = h >= 12 ? 'PM' : 'AM';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${ap}`;
}

export function lateLabel(movedFrom: LocalDate | undefined, overdueDays: number, label: (d: LocalDate) => string): string | null {
  if (movedFrom) return overdueDays >= 3 ? `${overdueDays} days late` : `Moved from ${label(movedFrom)}`;
  if (overdueDays > 0) return `${overdueDays} ${overdueDays === 1 ? 'day' : 'days'} overdue`;
  return null;
}
