import { describe, expect, it } from 'vitest'
import type { GoalRow } from '../../api/types'
import { cellState, formatMonthLabel, groupByOwner, monthDates, shiftMonth, toggleDay } from './gridUtils'

const MARIA = { id: 2, name: 'Maria', color: '#e03131' }
const NIKOS = { id: 3, name: 'Nikos', color: '#1098ad' }

function row(id: number, owner = MARIA, overrides: Partial<GoalRow> = {}): GoalRow {
  return {
    goal: { id, name: `Goal ${id}`, owner, active: true, startDate: '2026-11-03', targetDaysPerWeek: null },
    doneDates: ['2026-11-05'],
    stats: { streak: 0, doneThisMonth: 1, eligibleDays: 9, percentThisMonth: 11, doneThisWeek: 0 },
    ...overrides,
  }
}

describe('monthDates', () => {
  it('lists every day of the month', () => {
    expect(monthDates('2026-11')).toHaveLength(30)
    expect(monthDates('2026-11')[0]).toBe('2026-11-01')
    expect(monthDates('2028-02')).toHaveLength(29)
  })
})

describe('cellState', () => {
  const today = '2026-11-11'
  it('marks done, open, future and before-start days', () => {
    expect(cellState(row(1), '2026-11-05', today)).toBe('done')
    expect(cellState(row(1), '2026-11-06', today)).toBe('open')
    expect(cellState(row(1), '2026-11-11', today)).toBe('open')
    expect(cellState(row(1), '2026-11-12', today)).toBe('future')
    expect(cellState(row(1), '2026-11-02', today)).toBe('beforeStart')
  })
})

describe('groupByOwner', () => {
  it('groups consecutive rows by owner', () => {
    const groups = groupByOwner([row(1, MARIA), row(2, MARIA), row(3, NIKOS)])
    expect(groups.map((g) => [g.owner.name, g.rows.length])).toEqual([['Maria', 2], ['Nikos', 1]])
  })
})

describe('month navigation', () => {
  it('shifts across years and formats labels', () => {
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(formatMonthLabel('2026-11')).toBe('November 2026')
  })
})

describe('toggleDay', () => {
  it('adds and removes a day without duplicates', () => {
    expect(toggleDay(row(1), '2026-11-04', true).doneDates).toEqual(['2026-11-04', '2026-11-05'])
    expect(toggleDay(row(1), '2026-11-05', true).doneDates).toEqual(['2026-11-05'])
    expect(toggleDay(row(1), '2026-11-05', false).doneDates).toEqual([])
  })
})
