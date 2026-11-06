import dayjs from 'dayjs'
import type { GoalRow, IsoDate, MemberSummary, YearMonth } from '../../api/types'

/** All dates of a month: ["2026-11-01", …, "2026-11-30"]. */
export function monthDates(month: YearMonth): IsoDate[] {
  const first = dayjs(`${month}-01`)
  return Array.from({ length: first.daysInMonth() }, (_, i) => first.add(i, 'day').format('YYYY-MM-DD'))
}

export type CellState = 'done' | 'open' | 'future' | 'beforeStart'

/** What a grid cell shows and whether it can be clicked (only 'done' and 'open' can). */
export function cellState(row: GoalRow, date: IsoDate, today: IsoDate): CellState {
  if (date > today) return 'future'
  if (date < row.goal.startDate) return 'beforeStart'
  return row.doneDates.includes(date) ? 'done' : 'open'
}

export interface OwnerGroup {
  owner: MemberSummary
  rows: GoalRow[]
}

/** "Everyone" view: rows grouped by owner, keeping the server's order (owner name, then goal name). */
export function groupByOwner(rows: GoalRow[]): OwnerGroup[] {
  const groups: OwnerGroup[] = []
  for (const row of rows) {
    const last = groups.at(-1)
    if (last && last.owner.id === row.goal.owner.id) last.rows.push(row)
    else groups.push({ owner: row.goal.owner, rows: [row] })
  }
  return groups
}

export function shiftMonth(month: YearMonth, amount: number): YearMonth {
  return dayjs(`${month}-01`).add(amount, 'month').format('YYYY-MM')
}

export function formatMonthLabel(month: YearMonth): string {
  return dayjs(`${month}-01`).format('MMMM YYYY')
}

/** Optimistically flips one day in a row (stats are refreshed from the server response afterwards). */
export function toggleDay(row: GoalRow, date: IsoDate, done: boolean): GoalRow {
  const doneDates = done
    ? [...new Set([...row.doneDates, date])].sort()
    : row.doneDates.filter((d) => d !== date)
  return { ...row, doneDates }
}
