import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { groceriesApi } from '../../api/groceries'
import type { GroceryItem } from '../../api/types'
import { renderWithProviders } from '../../test/render'
import { GroceryListPage } from './GroceryListPage'

vi.mock('../../api/groceries', () => ({
  groceriesApi: {
    items: vi.fn(),
    defaults: vi.fn(),
    setBought: vi.fn(),
    addFromDefault: vi.fn(),
    clearBought: vi.fn(),
  },
}))

const api = vi.mocked(groceriesApi)

function item(id: number, overrides: Partial<GroceryItem>): GroceryItem {
  return {
    id, name: `Item ${id}`, category: 'OTHER', quantity: 1, unit: 'PCS', merchant: null, notes: null,
    bought: false, boughtAt: null, source: 'MANUAL', addedBy: null, createdAt: '2026-11-06T10:00:00Z', ...overrides,
  }
}

describe('GroceryListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.defaults.mockResolvedValue([
      { id: 1, name: 'Milk', category: 'DAIRY', defaultQuantity: 1, unit: 'L', preferredMerchant: null, active: true },
    ])
  })

  it('groups by shop and category and lists bought items separately', async () => {
    api.items.mockResolvedValue([
      item(1, { name: 'Tomatoes', category: 'FRUIT_VEG', merchant: 'LIDL', quantity: 1.5, unit: 'KG' }),
      item(2, { name: 'Toilet paper', category: 'HOUSEHOLD', merchant: null, unit: 'PACK' }),
      item(3, { name: 'Bread', category: 'BAKERY', bought: true, boughtAt: '2026-11-06T11:00:00Z' }),
    ])

    renderWithProviders(<GroceryListPage />, { route: '/groceries' })

    expect(await screen.findByText('Tomatoes')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Lidl' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Any shop' })).toBeInTheDocument()
    expect(screen.getByText('Fruit & Veg')).toBeInTheDocument()
    expect(screen.getByText('1.5 kg')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Bought (1)' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Untick Bread' })).toBeChecked()
  })

  it('ticks an item immediately (optimistic) and calls the API', async () => {
    api.items.mockResolvedValue([item(1, { name: 'Milk', category: 'DAIRY' })])
    api.setBought.mockReturnValue(new Promise(() => {})) // never resolves: UI must not wait for it

    renderWithProviders(<GroceryListPage />, { route: '/groceries' })
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Tick Milk' }))

    expect(api.setBought).toHaveBeenCalledWith(1, true)
    expect(await screen.findByRole('heading', { name: 'Bought (1)' })).toBeInTheDocument()
  })

  it('quick-adds a favourite in one tap', async () => {
    api.items.mockResolvedValue([])
    api.addFromDefault.mockResolvedValue({ item: item(9, { name: 'Milk' }), merged: false })

    renderWithProviders(<GroceryListPage />, { route: '/groceries' })
    expect(await screen.findByText('Nothing to buy')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Quick add Milk' }))

    expect(api.addFromDefault).toHaveBeenCalledWith(1)
  })

  it('asks before clearing bought items', async () => {
    api.items.mockResolvedValue([item(3, { name: 'Bread', bought: true, boughtAt: '2026-11-06T11:00:00Z' })])
    api.clearBought.mockResolvedValue({ removed: 1 })

    renderWithProviders(<GroceryListPage />, { route: '/groceries' })
    await userEvent.click(await screen.findByRole('button', { name: 'Clear bought' }))
    const dialog = await screen.findByRole('dialog')
    expect(api.clearBought).not.toHaveBeenCalled()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Clear' }))

    expect(api.clearBought).toHaveBeenCalled()
  })
})
