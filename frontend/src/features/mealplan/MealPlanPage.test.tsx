import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mealPlanApi } from '../../api/mealPlan'
import { recipesApi } from '../../api/recipes'
import type { MealPlanWeek } from '../../api/types'
import { renderWithProviders } from '../../test/render'
import { MealPlanPage } from './MealPlanPage'

vi.mock('../../api/mealPlan', () => ({
  mealPlanApi: { week: vi.fn(), plan: vi.fn(), addToGroceries: vi.fn(), remove: vi.fn(), entry: vi.fn() },
}))
vi.mock('../../api/recipes', () => ({ recipesApi: { list: vi.fn() } }))

const api = vi.mocked(mealPlanApi)

const WEEK: MealPlanWeek = {
  weekStart: '2026-11-09',
  weekEnd: '2026-11-15',
  today: '2026-11-11',
  entries: [{
    id: 5, date: '2026-11-09', slot: 'DINNER',
    recipe: { id: 2, title: 'Moussaka', imageUrl: null, prepMinutes: 90 }, plannedBy: null,
  }],
}

describe('MealPlanPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.week.mockResolvedValue(WEEK)
    vi.mocked(recipesApi.list).mockResolvedValue([{
      id: 1, title: 'Pancakes', description: null, servings: 4, prepMinutes: 20, imageUrl: null, tags: [], ingredientCount: 2,
    }])
  })

  it('shows Monday to Sunday with planned meals and today marked', async () => {
    renderWithProviders(<MealPlanPage />, { route: '/meal-plan' })

    expect(await screen.findByText('9 – 15 Nov 2026')).toBeInTheDocument()
    expect(screen.getByText('Mon 9 Nov')).toBeInTheDocument()
    expect(screen.getByText('Sun 15 Nov')).toBeInTheDocument()
    expect(screen.getByText('Moussaka')).toBeInTheDocument()
    expect(screen.getByText('Today')).toBeInTheDocument()
  })

  it('planning a recipe asks whether to add its ingredients, then adds the selected ones', async () => {
    api.plan.mockResolvedValue({
      entry: { id: 9, date: '2026-11-10', slot: 'BREAKFAST', recipe: { id: 1, title: 'Pancakes', imageUrl: null, prepMinutes: 20 }, plannedBy: null },
      ingredients: [
        { name: 'Flour', quantity: 250, unit: 'G', category: 'PANTRY' },
        { name: 'Milk', quantity: 0.5, unit: 'L', category: 'DAIRY' },
      ],
    })
    api.addToGroceries.mockResolvedValue({ added: 1, merged: 1, items: [] })

    renderWithProviders(<MealPlanPage />, { route: '/meal-plan' })
    await userEvent.click(await screen.findByRole('button', { name: 'Add breakfast on Tue 10 Nov' }))
    await userEvent.click(await screen.findByText('Pancakes'))

    expect(api.plan).toHaveBeenCalledWith('2026-11-10', 'BREAKFAST', 1)
    const dialog = await screen.findByRole('dialog', { name: 'Add to the grocery list?' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add selected (2)' }))

    expect(api.addToGroceries).toHaveBeenCalledWith(9, [0, 1])
    expect(await screen.findByText('1 added, 1 merged with items already on the list')).toBeInTheDocument()
  })

  it('navigates between weeks', async () => {
    renderWithProviders(<MealPlanPage />, { route: '/meal-plan' })
    await userEvent.click(await screen.findByRole('button', { name: 'Next week' }))

    expect(api.week).toHaveBeenLastCalledWith('2026-11-16')
  })
})
