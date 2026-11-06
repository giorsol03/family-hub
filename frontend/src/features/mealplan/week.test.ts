import { describe, expect, it } from 'vitest'
import type { MealPlanEntry } from '../../api/types'
import { entriesFor, formatWeekRange, shiftWeek, weekDays } from './week'

describe('week helpers', () => {
  it('lists the seven days of a week', () => {
    expect(weekDays('2026-11-09')).toEqual([
      '2026-11-09', '2026-11-10', '2026-11-11', '2026-11-12', '2026-11-13', '2026-11-14', '2026-11-15',
    ])
  })

  it('formats week ranges compactly', () => {
    expect(formatWeekRange('2026-11-09', '2026-11-15')).toBe('9 – 15 Nov 2026')
    expect(formatWeekRange('2026-11-30', '2026-12-06')).toBe('30 Nov – 6 Dec 2026')
    expect(formatWeekRange('2025-12-29', '2026-01-04')).toBe('29 Dec 2025 – 4 Jan 2026')
  })

  it('moves by whole weeks', () => {
    expect(shiftWeek('2026-11-09', 1)).toBe('2026-11-16')
    expect(shiftWeek('2026-11-09', -1)).toBe('2026-11-02')
  })

  it('finds the entries for a day and slot', () => {
    const entry = (id: number, date: string, slot: MealPlanEntry['slot']): MealPlanEntry => ({
      id, date, slot, recipe: { id: 1, title: 'X', imageUrl: null, prepMinutes: null }, plannedBy: null,
    })
    const entries = [entry(1, '2026-11-09', 'DINNER'), entry(2, '2026-11-09', 'LUNCH'), entry(3, '2026-11-10', 'DINNER')]
    expect(entriesFor(entries, '2026-11-09', 'DINNER').map((e) => e.id)).toEqual([1])
  })
})
