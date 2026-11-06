import { describe, expect, it } from 'vitest'
import type { CalendarItem } from '../../api/types'
import { formatItemTime, itemsOnDay, monthGrid, startOfWeek, weekOf } from './calendarUtils'

function item(overrides: Partial<CalendarItem>): CalendarItem {
  return {
    kind: 'EVENT', id: 1, title: 'X', type: 'EVENT', allDay: false,
    start: '2026-11-10T19:30:00', end: '2026-11-10T22:00:00', location: null, description: null,
    attendees: [], createdBy: null, slot: null, recipeId: null, ...overrides,
  }
}

describe('monthGrid', () => {
  it('covers the month in whole Monday–Sunday weeks', () => {
    const weeks = monthGrid('2026-11-20') // Nov 2026 starts on a Sunday, ends on a Monday
    expect(weeks[0][0]).toBe('2026-10-26')
    expect(weeks[0][6]).toBe('2026-11-01')
    expect(weeks.at(-1)![0]).toBe('2026-11-30')
    expect(weeks.at(-1)![6]).toBe('2026-12-06')
    expect(weeks).toHaveLength(6)
  })

  it('handles a month starting on Monday', () => {
    const weeks = monthGrid('2027-02-10') // Feb 2027: Mon 1st … Sun 28th
    expect(weeks).toHaveLength(4)
    expect(weeks[0][0]).toBe('2027-02-01')
  })
})

describe('week helpers', () => {
  it('finds the Monday of a week, including from a Sunday', () => {
    expect(startOfWeek('2026-11-11')).toBe('2026-11-09')
    expect(startOfWeek('2026-11-15')).toBe('2026-11-09')
    expect(weekOf('2026-11-15')).toHaveLength(7)
  })
})

describe('itemsOnDay', () => {
  it('shows multi-day items on every day they cover', () => {
    const trip = item({ allDay: true, start: '2026-11-10T00:00:00', end: '2026-11-12T00:00:00' })
    expect(itemsOnDay([trip], '2026-11-09')).toEqual([])
    expect(itemsOnDay([trip], '2026-11-10')).toEqual([trip])
    expect(itemsOnDay([trip], '2026-11-12')).toEqual([trip])
    expect(itemsOnDay([trip], '2026-11-13')).toEqual([])
  })
})

describe('formatItemTime', () => {
  it('formats all-day, same-day and overnight items', () => {
    expect(formatItemTime(item({ allDay: true }))).toBe('All day')
    expect(formatItemTime(item({}))).toBe('19:30 – 22:00')
    expect(formatItemTime(item({ start: '2026-11-14T22:00:00', end: '2026-11-15T02:00:00' }))).toBe('Sat 22:00 – Sun 02:00')
  })
})
