import { describe, expect, it } from 'vitest'
import type { Chore } from '../../api/types'
import { describeRecurrence, formatDay, groupByDay, sortChores } from './dates'

function chore(id: number, overrides: Partial<Chore>): Chore {
  return {
    id, title: `Chore ${id}`, description: null, scheduledDate: '2026-11-09', assignee: null, status: 'TODO',
    completedAt: null, completedBy: null, templateId: null, recurrence: 'NONE', estimatedMinutes: null, ...overrides,
  }
}

describe('formatDay', () => {
  const today = '2026-11-11'
  it('uses relative names around today', () => {
    expect(formatDay('2026-11-11', today)).toBe('Today')
    expect(formatDay('2026-11-12', today)).toBe('Tomorrow')
    expect(formatDay('2026-11-10', today)).toBe('Yesterday')
  })
  it('uses a short date otherwise, with the year only when different', () => {
    expect(formatDay('2026-11-09', today)).toBe('Mon 9 Nov')
    expect(formatDay('2025-12-31', today)).toBe('Wed 31 Dec 2025')
  })
})

describe('describeRecurrence', () => {
  it('describes each rule', () => {
    expect(describeRecurrence('NONE', null)).toBe('One-off')
    expect(describeRecurrence('WEEKLY', null)).toBe('Every week')
    expect(describeRecurrence('EVERY_N_DAYS', 3)).toBe('Every 3 days')
  })
})

describe('groupByDay', () => {
  it('returns every day of the range, with chores on their day', () => {
    const days = groupByDay([
      chore(1, { scheduledDate: '2026-11-09', title: 'B' }),
      chore(2, { scheduledDate: '2026-11-09', title: 'A' }),
      chore(3, { scheduledDate: '2026-11-15' }),
    ], '2026-11-09', '2026-11-15')

    expect(days).toHaveLength(7)
    expect(days[0].date).toBe('2026-11-09')
    expect(days[0].chores.map((c) => c.title)).toEqual(['A', 'B'])
    expect(days[3].chores).toEqual([])
    expect(days[6].chores.map((c) => c.id)).toEqual([3])
  })
})

describe('sortChores', () => {
  it('puts to-do before done and skipped', () => {
    const sorted = sortChores([
      chore(1, { title: 'A', status: 'DONE' }),
      chore(2, { title: 'Z', status: 'TODO' }),
      chore(3, { title: 'B', status: 'SKIPPED' }),
    ])
    expect(sorted.map((c) => c.id)).toEqual([2, 1, 3])
  })
})
