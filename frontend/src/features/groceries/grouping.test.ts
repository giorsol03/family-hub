import { describe, expect, it } from 'vitest'
import type { GroceryItem } from '../../api/types'
import { groupByMerchantAndCategory, splitBought } from './grouping'
import { formatQuantity } from './labels'

let nextId = 1
function item(overrides: Partial<GroceryItem>): GroceryItem {
  return {
    id: nextId++,
    name: 'Item',
    category: 'OTHER',
    quantity: 1,
    unit: 'PCS',
    merchant: null,
    notes: null,
    bought: false,
    boughtAt: null,
    source: 'MANUAL',
    addedBy: null,
    createdAt: '2026-11-06T10:00:00Z',
    ...overrides,
  }
}

describe('groupByMerchantAndCategory', () => {
  it('orders shops, then categories, then names, with "Any shop" last', () => {
    const groups = groupByMerchantAndCategory([
      item({ name: 'Toilet paper', category: 'HOUSEHOLD', merchant: null }),
      item({ name: 'tomatoes', category: 'FRUIT_VEG', merchant: 'LIDL' }),
      item({ name: 'Milk', category: 'DAIRY', merchant: 'LIDL' }),
      item({ name: 'Cucumbers', category: 'FRUIT_VEG', merchant: 'LIDL' }),
      item({ name: 'Halloumi', category: 'DAIRY', merchant: 'ALPHAMEGA' }),
    ])

    expect(groups.map((g) => g.merchant)).toEqual(['LIDL', 'ALPHAMEGA', null])
    expect(groups[0].count).toBe(3)
    expect(groups[0].categories.map((c) => c.category)).toEqual(['FRUIT_VEG', 'DAIRY'])
    expect(groups[0].categories[0].items.map((i) => i.name)).toEqual(['Cucumbers', 'tomatoes'])
  })

  it('returns nothing for an empty list', () => {
    expect(groupByMerchantAndCategory([])).toEqual([])
  })
})

describe('splitBought', () => {
  it('separates ticked items, most recently bought first', () => {
    const { toBuy, bought } = splitBought([
      item({ name: 'A' }),
      item({ name: 'B', bought: true, boughtAt: '2026-11-06T09:00:00Z' }),
      item({ name: 'C', bought: true, boughtAt: '2026-11-06T11:00:00Z' }),
    ])

    expect(toBuy.map((i) => i.name)).toEqual(['A'])
    expect(bought.map((i) => i.name)).toEqual(['C', 'B'])
  })
})

describe('formatQuantity', () => {
  it('formats amounts with short unit labels', () => {
    expect(formatQuantity(1.5, 'KG')).toBe('1.5 kg')
    expect(formatQuantity(500, 'G')).toBe('500 g')
    expect(formatQuantity(4, 'PCS')).toBe('4 pcs')
    expect(formatQuantity(1, 'PACK')).toBe('1 pack')
    expect(formatQuantity(2, 'PACK')).toBe('2 packs')
    expect(formatQuantity(1.25, 'L')).toBe('1.25 l')
  })
})
