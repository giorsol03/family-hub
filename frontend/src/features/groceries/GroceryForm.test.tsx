import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import type { GroceryDefault } from '../../api/types'
import { renderWithProviders } from '../../test/render'
import { GroceryForm } from './GroceryForm'

const FLOUR: GroceryDefault = {
  id: 6, name: 'Flour', category: 'PANTRY', defaultQuantity: 1, unit: 'KG', preferredMerchant: 'LIDL', active: true,
}

describe('GroceryForm', () => {
  it('requires a name and a positive quantity', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(<GroceryForm initial={{ quantity: '' }} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Must be more than 0')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects fractional pieces', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(
      <GroceryForm initial={{ name: 'Bread', quantity: 1.5, unit: 'PCS' }} onSubmit={onSubmit} onCancel={() => {}} />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Use a whole number for pieces and packs')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('picking a suggestion fills in its details', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(<GroceryForm suggestions={[FLOUR]} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.type(screen.getByRole('combobox', { name: /^Item/ }), 'Flo')
    await userEvent.click(await screen.findByRole('option', { name: 'Flour' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      name: 'Flour', category: 'PANTRY', quantity: 1, unit: 'KG', merchant: 'LIDL', notes: null,
    }))
  })

  it('maps server field names for favourites onto the inputs', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError(400, 'Use a whole number', { defaultQuantity: 'Use a whole number for pieces and packs' }),
    )
    renderWithProviders(
      <GroceryForm withNotes={false} initial={{ name: 'Eggs', quantity: 2, unit: 'PACK' }}
                   onSubmit={onSubmit} onCancel={() => {}} />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Use a whole number for pieces and packs')).toBeInTheDocument()
    expect(screen.queryByLabelText('Notes')).not.toBeInTheDocument()
  })
})
