import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { goalsApi } from '../../api/goals'
import { membersApi } from '../../api/members'
import type { GoalMonth, GoalRow } from '../../api/types'
import { ADMIN, renderWithProviders } from '../../test/render'
import { GoalsPage } from './GoalsPage'

vi.mock('../../api/goals', () => ({ goalsApi: { month: vi.fn(), list: vi.fn(), setCheck: vi.fn(), setActive: vi.fn() } }))
vi.mock('../../api/members', () => ({ membersApi: { list: vi.fn() } }))

const api = vi.mocked(goalsApi)
const MARIA = { id: 2, name: 'Maria', color: '#e03131' }

function row(id: number, name: string, owner: { id: number; name: string; color: string }, doneDates: string[], target: number | null = null): GoalRow {
  return {
    goal: { id, name, owner, active: true, startDate: '2026-11-01', targetDaysPerWeek: target },
    doneDates,
    stats: { streak: 2, doneThisMonth: doneDates.length, eligibleDays: 11, percentThisMonth: 18, doneThisWeek: 2 },
  }
}

const MONTH: GoalMonth = {
  month: '2026-11',
  today: '2026-11-11',
  daysInMonth: 30,
  rows: [
    row(1, 'Gym', ADMIN, ['2026-11-10', '2026-11-11'], 3),
    row(2, 'Yoga', MARIA, ['2026-11-09']),
  ],
}

describe('GoalsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(membersApi.list).mockResolvedValue([ADMIN, { ...MARIA, username: 'maria', active: true }])
    api.month.mockResolvedValue(MONTH)
    api.list.mockResolvedValue([])
  })

  it('shows one row per goal grouped by person, with streak, % and weekly target', async () => {
    renderWithProviders(<GoalsPage />, { route: '/goals' })

    expect(await screen.findByText('November 2026')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Gym' })).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Maria')).toBeInTheDocument() // owner group header
    expect(screen.getAllByLabelText('Streak 2')).toHaveLength(2)
    expect(screen.getAllByText('18%')).toHaveLength(2)
    expect(screen.getByText('2/3 wk')).toBeInTheDocument()
  })

  it('cells show done, open and future days', async () => {
    renderWithProviders(<GoalsPage />, { route: '/goals' })

    expect(await screen.findByRole('button', { name: 'Gym, 11 November' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Gym, 9 November' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: 'Gym, 12 November' })).toBeDisabled()
  })

  it('ticking a day updates immediately and calls the API', async () => {
    api.setCheck.mockReturnValue(new Promise(() => {})) // never resolves: the UI must not wait
    renderWithProviders(<GoalsPage />, { route: '/goals' })

    const cell = await screen.findByRole('button', { name: 'Yoga, 10 November' })
    await userEvent.click(cell)

    expect(api.setCheck).toHaveBeenCalledWith(2, '2026-11-10', true)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Yoga, 10 November' })).toHaveAttribute('aria-pressed', 'true'))
  })

  it('unticking sends done=false', async () => {
    api.setCheck.mockResolvedValue(row(1, 'Gym', ADMIN, ['2026-11-10']))
    renderWithProviders(<GoalsPage />, { route: '/goals' })

    await userEvent.click(await screen.findByRole('button', { name: 'Gym, 11 November' }))

    expect(api.setCheck).toHaveBeenCalledWith(1, '2026-11-11', false)
  })

  it('filters by person and navigates months', async () => {
    renderWithProviders(<GoalsPage />, { route: '/goals' })
    await userEvent.click(await screen.findByRole('button', { name: 'Next month' }))

    await waitFor(() => expect(api.month).toHaveBeenLastCalledWith('2026-12', null))
  })
})
