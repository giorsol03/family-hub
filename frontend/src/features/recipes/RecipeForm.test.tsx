import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import type { Recipe } from '../../api/types'
import { renderWithProviders } from '../../test/render'
import { RecipeForm } from './RecipeForm'

const PANCAKES: Recipe = {
  id: 1, title: 'Pancakes', description: null, servings: 4, prepMinutes: 20, instructions: 'Mix\nFry',
  imageUrl: null, tags: ['breakfast'], createdBy: null, createdAt: '2026-11-06T10:00:00Z',
  ingredients: [
    { name: 'Flour', quantity: 250, unit: 'G', category: 'PANTRY' },
    { name: 'Milk', quantity: 500, unit: 'ML', category: 'DAIRY' },
  ],
}

describe('RecipeForm', () => {
  it('adds and removes ingredient rows and submits them in order', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(<RecipeForm recipe={PANCAKES} tagSuggestions={[]} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Add ingredient' }))
    await userEvent.type(screen.getByLabelText('Ingredient 3 name'), 'Eggs')
    await userEvent.click(screen.getByRole('button', { name: 'Remove ingredient 1' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    const input = onSubmit.mock.calls[0][0]
    expect(input.ingredients).toEqual([
      { name: 'Milk', quantity: 500, unit: 'ML', category: 'DAIRY' },
      { name: 'Eggs', quantity: 1, unit: 'PCS', category: null },
    ])
    expect(input.tags).toEqual(['breakfast'])
    expect(input.servings).toBe(4)
  })

  it('validates title and ingredient names before submitting', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(<RecipeForm tagSuggestions={[]} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(screen.getByText('Name?')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('shows server errors on nested ingredient fields', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError(400, 'Please fix', { 'ingredients[1].quantity': 'Too many digits' }),
    )
    renderWithProviders(<RecipeForm recipe={PANCAKES} tagSuggestions={[]} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Too many digits')).toBeInTheDocument()
  })
})
