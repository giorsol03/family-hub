import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { calendarApi } from '../../api/calendar'
import { membersApi } from '../../api/members'
import type { CalendarItem, CalendarRange } from '../../api/types'
import { ADMIN, renderWithProviders } from '../../test/render'
import { CalendarPage } from './CalendarPage'

vi.mock('../../api/calendar', () => ({ calendarApi: { range: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() } }))
vi.mock('../../api/members', () => ({ membersApi: { list: vi.fn() } }))

const api = vi.mocked(calendarApi)

const NIGHT_OUT: CalendarItem = {
  kind: 'EVENT', id: 1, title: 'Cinema', type: 'NIGHT_OUT', allDay: false,
  start: '2026-11-14T20:00:00', end: '2026-11-14T22:30:00', location: 'Mall of Cyprus', description: null,
  attendees: [ADMIN], createdBy: ADMIN, slot: null, recipeId: null,
}
const DINNER: CalendarItem = {
  kind: 'MEAL_PLAN', id: 7, title: 'Dinner: Moussaka', type: 'MEAL', allDay: true,
  start: '2026-11-14T00:00:00', end: '2026-11-14T00:00:00', location: null, description: null,
  attendees: [], createdBy: null, slot: 'DINNER', recipeId: 3,
}

describe('CalendarPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true, toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-11T10:00:00'))
    vi.mocked(membersApi.list).mockResolvedValue([ADMIN])
    api.range.mockImplementation(async (from, to): Promise<CalendarRange> => ({
      from, to, today: '2026-11-11', items: [DINNER, NIGHT_OUT],
    }))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('loads the whole month grid and shows items on their day', async () => {
    renderWithProviders(<CalendarPage />, { route: '/calendar' })

    expect(await screen.findByText('November 2026')).toBeInTheDocument()
    await waitFor(() => expect(api.range).toHaveBeenCalledWith('2026-10-26', '2026-12-06', null))
    expect(await screen.findByRole('button', { name: 'Saturday 14 November, 2 items' })).toBeInTheDocument()
  })

  it('clicking a busy day shows its agenda; clicking an empty day opens the new-event form', async () => {
    renderWithProviders(<CalendarPage />, { route: '/calendar' })

    await userEvent.click(await screen.findByRole('button', { name: 'Saturday 14 November, 2 items' }))
    expect(screen.getByRole('button', { name: 'Open Cinema' })).toHaveTextContent('20:00 – 22:30')
    expect(screen.getByRole('button', { name: 'Open Dinner: Moussaka' })).toHaveTextContent('Meal plan')

    await userEvent.click(screen.getByRole('button', { name: 'Tuesday 17 November' }))
    expect(await screen.findByRole('dialog', { name: 'New event' })).toBeInTheDocument()
  })

  it('creates an event with local date-times', async () => {
    api.create.mockResolvedValue({ ...NIGHT_OUT, id: 2, title: 'Dentist' })
    renderWithProviders(<CalendarPage />, { route: '/calendar' })

    await userEvent.click(await screen.findByRole('button', { name: 'Tuesday 17 November' }))
    const dialog = await screen.findByRole('dialog', { name: 'New event' })
    await userEvent.type(within(dialog).getByLabelText(/^Title/), 'Dentist')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(api.create).toHaveBeenCalledWith({
      title: 'Dentist', type: 'EVENT', allDay: false,
      start: '2026-11-17T09:00:00', end: '2026-11-17T10:00:00',
      location: null, description: null, attendeeIds: [],
    }))
  })

  it('week view lists each day', async () => {
    renderWithProviders(<CalendarPage />, { route: '/calendar' })
    await userEvent.click(await screen.findByText('Week'))

    await waitFor(() => expect(api.range).toHaveBeenLastCalledWith('2026-11-09', '2026-11-15', null))
    expect(await screen.findByText('9 – 15 Nov 2026')).toBeInTheDocument()
    expect(screen.getByText('Sat 14 Nov')).toBeInTheDocument()
  })
})
