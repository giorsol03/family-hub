import dayjs from 'dayjs'
import type { Chore, IsoDate, Recurrence } from '../../api/types'

/** "Today", "Tomorrow", "Yesterday", else "Mon 9 Nov" (year added when it isn't this year). */
export function formatDay(date: IsoDate, today: IsoDate): string {
  const d = dayjs(date)
  const diff = d.diff(dayjs(today), 'day')
  if (diff === 0) return 'Today'
  if (diff === 1) return 'Tomorrow'
  if (diff === -1) return 'Yesterday'
  return d.format(d.year() === dayjs(today).year() ? 'ddd D MMM' : 'ddd D MMM YYYY')
}

export function describeRecurrence(recurrence: Recurrence, interval: number | null): string {
  switch (recurrence) {
    case 'NONE': return 'One-off'
    case 'DAILY': return 'Every day'
    case 'WEEKLY': return 'Every week'
    case 'MONTHLY': return 'Every month'
    case 'EVERY_N_DAYS': return interval === 1 ? 'Every day' : `Every ${interval ?? '?'} days`
  }
}

export const RECURRENCE_OPTIONS: { value: Recurrence; label: string }[] = [
  { value: 'NONE', label: 'Does not repeat' },
  { value: 'DAILY', label: 'Every day' },
  { value: 'WEEKLY', label: 'Every week' },
  { value: 'EVERY_N_DAYS', label: 'Every N days' },
  { value: 'MONTHLY', label: 'Every month' },
]

export interface DayGroup {
  date: IsoDate
  chores: Chore[]
}

/** One group per day from {@code from} to {@code to} inclusive (empty days kept, so the week reads Mon–Sun). */
export function groupByDay(chores: Chore[], from: IsoDate, to: IsoDate): DayGroup[] {
  const days: DayGroup[] = []
  for (let d = dayjs(from); !d.isAfter(dayjs(to), 'day'); d = d.add(1, 'day')) {
    const date = d.format('YYYY-MM-DD')
    days.push({ date, chores: sortChores(chores.filter((c) => c.scheduledDate === date)) })
  }
  return days
}

/** To-do first, then done/skipped; alphabetical within each. */
export function sortChores(chores: Chore[]): Chore[] {
  return [...chores].sort((a, b) =>
    Number(a.status !== 'TODO') - Number(b.status !== 'TODO') || a.title.localeCompare(b.title))
}
