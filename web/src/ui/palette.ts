// Task colors: [tint, foreground] for light and dark. Same keys on Android.
export const TASK_COLORS: Record<string, { name: string; light: [string, string]; dark: [string, string]; swatch: string }> = {
  teal: { name: 'Teal', light: ['#CCFBF1', '#0F766E'], dark: ['#134E4A', '#5EEAD4'], swatch: '#0F766E' },
  green: { name: 'Green', light: ['#DCFCE7', '#047857'], dark: ['#123224', '#6EE7B7'], swatch: '#047857' },
  blue: { name: 'Blue', light: ['#DBEAFE', '#1D4ED8'], dark: ['#1E2A44', '#93C5FD'], swatch: '#1D4ED8' },
  purple: { name: 'Purple', light: ['#EDE9FE', '#6D28D9'], dark: ['#2A2145', '#C4B5FD'], swatch: '#6D28D9' },
  amber: { name: 'Amber', light: ['#FEF3C7', '#B45309'], dark: ['#3A2A10', '#FCD34D'], swatch: '#B45309' },
  rose: { name: 'Rose', light: ['#FFE4E6', '#BE123C'], dark: ['#3F1620', '#FDA4AF'], swatch: '#BE123C' },
};

export function taskColors(key: string, dark: boolean): { tint: string; fg: string } {
  const c = TASK_COLORS[key] ?? TASK_COLORS.teal;
  const [tint, fg] = dark ? c.dark : c.light;
  return { tint, fg };
}
