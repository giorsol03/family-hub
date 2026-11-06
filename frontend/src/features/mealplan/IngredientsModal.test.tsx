import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Ingredient } from '../../api/types'
import { renderWithProviders } from '../../test/render'
import { IngredientsModal } from './IngredientsModal'

const INGREDIENTS: Ingredient[] = [
  { name: 'Flour', quantity: 500, unit: 'G', category: 'PANTRY' },
  { name: 'Milk', quantity: 0.5, unit: 'L', category: null },
  { name: 'Eggs', quantity: 2, unit: 'PCS', category: 'DAIRY' },
]

function renderModal(onAdd = vi.fn(), onSkip = vi.fn()) {
  renderWithProviders(
    <IngredientsModal opened recipeTitle="Pancakes" ingredients={INGREDIENTS} onAdd={onAdd} onSkip={onSkip} />,
  )
  return { onAdd, onSkip }
}

describe('IngredientsModal', () => {
  it('asks the question with every ingredient ticked by default', () => {
    renderModal()

    expect(screen.getByText(/Should we add the ingredients for/)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Flour' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Milk' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Eggs' })).toBeChecked()
    expect(screen.getByText('500 g')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add selected (3)' })).toBeEnabled()
  })

  it('adds only the selected ingredients', async () => {
    const { onAdd } = renderModal()

    await userEvent.click(screen.getByRole('checkbox', { name: 'Milk' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add selected (2)' }))

    expect(onAdd).toHaveBeenCalledWith([0, 2])
  })

  it('select none disables Add; select all re-ticks everything', async () => {
    const { onAdd } = renderModal()

    await userEvent.click(screen.getByRole('button', { name: 'Select none' }))
    expect(screen.getByRole('button', { name: 'Add selected (0)' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Select all' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add selected (3)' }))
    expect(onAdd).toHaveBeenCalledWith([0, 1, 2])
  })

  it('Skip closes without adding', async () => {
    const { onAdd, onSkip } = renderModal()

    await userEvent.click(screen.getByRole('button', { name: 'Skip' }))

    expect(onSkip).toHaveBeenCalled()
    expect(onAdd).not.toHaveBeenCalled()
  })
})
