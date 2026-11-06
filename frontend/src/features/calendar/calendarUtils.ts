import dayjs from 'dayjs'
import type { CalendarItem, EventType, IsoDate } from '../../api/types'

const ISO = 'YYYY-MM-DD'

export const EVENT_TYPE_META: Record<EventType, { label: string; color: string }> = {
  EVENT: { label: 'Event', color: 'blue' },
  MEAL: { label: 'Meal', color: 'orange' },
  NIGHT_OUT: { label: 'Night out', color: 'grape' },
  OTHER: { label: 'Other', color: 'gray' },
}

/** Meal-plan entries get the app's accent colour so they read as "from the planner". */
export function itemColor(item: CalendarItem): string {
  return item.kind === 'MEAL_PLAN' ? 'teal' : EVENT_TYPE_META[item.type].color
}

export const WEEKDAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

/** Monday of the week containing {@code date}. */
export function startOfWeek(date: IsoDate): IsoDate {
  const d = dayjs(date)
  return d.subtract((d.day() + 6) % 7, 'day').format(ISO)
}

/** The month grid: whole Mon–Sun weeks covering the month of {@code anyDayInMonth}. */
export function monthGrid(anyDayInMonth: IsoDate): IsoDate[][] {
  const first = dayjs(anyDayInMonth).startOf('month')
  const last = first.endOf('month')
  const weeks: IsoDate[][] = []
  let day = dayjs(startOfWeek(first.format(ISO)))
  while (!day.isAfter(last, 'day')) {
    weeks.push(Array.from({ length: 7 }, (_, i) => day.add(i, 'day').format(ISO)))
    day = day.add(7, 'day')
  }
  return weeks
}

export function weekOf(anyDay: IsoDate): IsoDate[] {
  const monday = dayjs(startOfWeek(anyDay))
  return Array.from({ length: 7 }, (_, i) => monday.add(i, 'day').format(ISO))
}

/** Items happening on {@code date} (multi-day items appear on every day they cover). */
export function itemsOnDay(items: CalendarItem[], date: IsoDate): CalendarItem[] {
  return items.filter((item) => item.start.slice(0, 10) <= date && date <= item.end.slice(0, 10))
}

/** "All day", "19:30 – 22:00", or for multi-day timed events "Sat 19:30 – Sun 02:00". */
export function formatItemTime(item: CalendarItem): string {
  if (item.allDay) return 'All day'
  const start = dayjs(item.start)
  const end = dayjs(item.end)
  if (start.isSame(end, 'day')) return `${start.format('HH:mm')} – ${end.format('HH:mm')}`
  return `${start.format('ddd HH:mm')} – ${end.format('ddd HH:mm')}`
}

export function formatMonth(anyDay: IsoDate): string {
  return dayjs(anyDay).format('MMMM YYYY')
}

export function shift(anyDay: IsoDate, unit: 'month' | 'week', amount: number): IsoDate {
  return dayjs(anyDay).add(amount, unit).format(ISO)
}
