import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { recipesApi } from '../../api/recipes'
import { renderWithProviders } from '../../test/render'
import { RecipesPage } from './RecipesPage'

vi.mock('../../api/recipes', () => ({ recipesApi: { list: vi.fn(), tags: vi.fn() } }))
const api = vi.mocked(recipesApi)

describe('RecipesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.tags.mockResolvedValue(['breakfast', 'dinner'])
    api.list.mockResolvedValue([{
      id: 1, title: 'Pancakes', description: 'Fluffy', servings: 4, prepMinutes: 75, imageUrl: null,
      tags: ['breakfast'], ingredientCount: 3,
    }])
  })

  it('shows recipe cards linking to the detail page', async () => {
    renderWithProviders(<RecipesPage />, { route: '/recipes' })

    expect(await screen.findByText('Pancakes')).toBeInTheDocument()
    expect(screen.getByText('1 h 15 min')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Pancakes/ })).toHaveAttribute('href', '/recipes/1')
  })

  it('searches (debounced) and filters by tag', async () => {
    renderWithProviders(<RecipesPage />, { route: '/recipes' })
    await screen.findByText('Pancakes')

    await userEvent.type(screen.getByLabelText('Search recipes'), 'pan')
    await waitFor(() => expect(api.list).toHaveBeenLastCalledWith('pan', null))

    await userEvent.click(screen.getByText('dinner'))
    await waitFor(() => expect(api.list).toHaveBeenLastCalledWith('pan', 'dinner'))
  })
})
