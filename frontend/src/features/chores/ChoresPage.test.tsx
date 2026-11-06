import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { choresApi } from '../../api/chores'
import { membersApi } from '../../api/members'
import type { Chore, ChoreList, ChoreView } from '../../api/types'
import { ADMIN, renderWithProviders } from '../../test/render'
import { ChoresPage } from './ChoresPage'

vi.mock('../../api/chores', () => ({
  choresApi: { list: vi.fn(), complete: vi.fn(), reopen: vi.fn(), skip: vi.fn(), templates: vi.fn() },
}))
vi.mock('../../api/members', () => ({ membersApi: { list: vi.fn() } }))

const api = vi.mocked(choresApi)
const TODAY = '2026-11-11' // a Wednesday

function chore(id: number, overrides: Partial<Chore>): Chore {
  return {
    id, title: `Chore ${id}`, description: null, scheduledDate: TODAY, assignee: null, status: 'TODO',
    completedAt: null, completedBy: null, templateId: null, recurrence: 'NONE', estimatedMinutes: null, ...overrides,
  }
}

function lists(byView: Partial<Record<ChoreView, Chore[]>>) {
  api.list.mockImplementation(async (view: ChoreView): Promise<ChoreList> => ({
    view,
    today: TODAY,
    from: view === 'WEEK' ? '2026-11-09' : view === 'TODAY' ? TODAY : null,
    to: view === 'WEEK' ? '2026-11-15' : TODAY,
    chores: byView[view] ?? [],
  }))
}

describe('ChoresPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(membersApi.list).mockResolvedValue([ADMIN])
    api.templates.mockResolvedValue([])
  })

  it("shows today's chores with the overdue count", async () => {
    lists({
      TODAY: [chore(1, { title: 'Wash the dishes', recurrence: 'DAILY', estimatedMinutes: 15, assignee: ADMIN })],
      OVERDUE: [chore(2, { title: 'Mopping', scheduledDate: '2026-11-08' })],
    })

    renderWithProviders(<ChoresPage />, { route: '/chores' })

    expect(await screen.findByText('Wash the dishes')).toBeInTheDocument()
    expect(screen.getByText('15 min')).toBeInTheDocument()
    expect(await screen.findByText('1')).toBeInTheDocument() // overdue badge
  })

  it('completing a recurring chore tells you when it comes back', async () => {
    const dishes = chore(1, { title: 'Wash the dishes', recurrence: 'DAILY' })
    lists({ TODAY: [dishes] })
    api.complete.mockResolvedValue({
      chore: { ...dishes, status: 'DONE' },
      next: chore(9, { title: 'Wash the dishes', scheduledDate: '2026-11-12' }),
    })

    renderWithProviders(<ChoresPage />, { route: '/chores' })
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Done: Wash the dishes' }))

    expect(api.complete).toHaveBeenCalledWith(1)
    expect(await screen.findByText(/Next: tomorrow/)).toBeInTheDocument()
  })

  it('ticking a done chore reopens it', async () => {
    lists({ TODAY: [chore(1, { title: 'Laundry', status: 'DONE' })] })
    api.reopen.mockResolvedValue(chore(1, { title: 'Laundry' }))

    renderWithProviders(<ChoresPage />, { route: '/chores' })
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Reopen Laundry' }))

    expect(api.reopen).toHaveBeenCalledWith(1)
  })

  it('week view lists every day Monday to Sunday', async () => {
    lists({ WEEK: [chore(1, { title: 'Vacuuming', scheduledDate: '2026-11-13' })] })

    renderWithProviders(<ChoresPage />, { route: '/chores' })
    await userEvent.click(await screen.findByText('This week'))

    expect(await screen.findByText('Vacuuming')).toBeInTheDocument()
    expect(screen.getByText('Mon 9 Nov')).toBeInTheDocument()
    expect(screen.getAllByText('Today')).toHaveLength(2) // tab label + Wednesday's heading
    expect(screen.getByText('Sun 15 Nov')).toBeInTheDocument()
    expect(api.list).toHaveBeenCalledWith('WEEK', null)
  })
})
