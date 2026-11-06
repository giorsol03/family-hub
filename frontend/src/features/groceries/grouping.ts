import { CATEGORIES, MERCHANTS } from '../../api/types'
import type { GroceryCategory, GroceryItem, Merchant } from '../../api/types'

export interface CategoryGroup {
  category: GroceryCategory
  items: GroceryItem[]
}

export interface MerchantGroup {
  /** null = "Any shop" */
  merchant: Merchant | null
  count: number
  categories: CategoryGroup[]
}

const byName = (a: GroceryItem, b: GroceryItem) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })

/**
 * Shopping-list layout: group by merchant (in the fixed shop order, "Any shop" last),
 * then by category (in the fixed category order), items alphabetically. Empty groups are omitted.
 */
export function groupByMerchantAndCategory(items: GroceryItem[]): MerchantGroup[] {
  const merchantOrder: (Merchant | null)[] = [...MERCHANTS, null]
  return merchantOrder
    .map((merchant) => {
      const forMerchant = items.filter((item) => item.merchant === merchant)
      const categories = CATEGORIES
        .map((category) => ({ category, items: forMerchant.filter((i) => i.category === category).sort(byName) }))
        .filter((group) => group.items.length > 0)
      return { merchant, count: forMerchant.length, categories }
    })
    .filter((group) => group.count > 0)
}

/** Splits the list into what still needs buying and what's already ticked (most recently ticked first). */
export function splitBought(items: GroceryItem[]): { toBuy: GroceryItem[]; bought: GroceryItem[] } {
  return {
    toBuy: items.filter((i) => !i.bought),
    bought: items
      .filter((i) => i.bought)
      .sort((a, b) => (b.boughtAt ?? '').localeCompare(a.boughtAt ?? '') || byName(a, b)),
  }
}
