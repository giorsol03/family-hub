import dayjs from 'dayjs'
import type { IsoDate, MealPlanEntry, MealSlot } from '../../api/types'

export const SLOT_LABELS: Record<MealSlot, string> = {
  BREAKFAST: 'Breakfast',
  LUNCH: 'Lunch',
  DINNER: 'Dinner',
}

/** The 7 ISO dates of the week starting on {@code weekStart}. */
export function weekDays(weekStart: IsoDate): IsoDate[] {
  return Array.from({ length: 7 }, (_, i) => dayjs(weekStart).add(i, 'day').format('YYYY-MM-DD'))
}

/** "9 – 15 Nov 2026", "29 Dec 2025 – 4 Jan 2026" */
export function formatWeekRange(start: IsoDate, end: IsoDate): string {
  const s = dayjs(start)
  const e = dayjs(end)
  if (s.year() !== e.year()) return `${s.format('D MMM YYYY')} – ${e.format('D MMM YYYY')}`
  if (s.month() !== e.month()) return `${s.format('D MMM')} – ${e.format('D MMM YYYY')}`
  return `${s.format('D')} – ${e.format('D MMM YYYY')}`
}

export function shiftWeek(weekStart: IsoDate, weeks: number): IsoDate {
  return dayjs(weekStart).add(weeks * 7, 'day').format('YYYY-MM-DD')
}

export function entriesFor(entries: MealPlanEntry[], date: IsoDate, slot: MealSlot): MealPlanEntry[] {
  return entries.filter((e) => e.date === date && e.slot === slot)
}
