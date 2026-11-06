import { CATEGORIES, MERCHANTS, UNITS } from '../../api/types'
import type { GroceryCategory, Merchant, QuantityUnit } from '../../api/types'

export const CATEGORY_LABELS: Record<GroceryCategory, string> = {
  FRUIT_VEG: 'Fruit & Veg',
  DAIRY: 'Dairy',
  MEAT: 'Meat',
  BAKERY: 'Bakery',
  PANTRY: 'Pantry',
  HOUSEHOLD: 'Household',
  OTHER: 'Other',
}

export const MERCHANT_LABELS: Record<Merchant, string> = {
  LIDL: 'Lidl',
  ALPHAMEGA: 'Alphamega',
  SKLAVENITIS: 'Sklavenitis',
  METRO: 'Metro',
  OTHER: 'Other shop',
}

export const ANY_SHOP_LABEL = 'Any shop'

export const UNIT_LABELS: Record<QuantityUnit, string> = {
  PCS: 'pcs',
  PACK: 'pack',
  G: 'g',
  KG: 'kg',
  ML: 'ml',
  L: 'l',
}

export const categoryOptions = CATEGORIES.map((value) => ({ value, label: CATEGORY_LABELS[value] }))
export const merchantOptions = MERCHANTS.map((value) => ({ value, label: MERCHANT_LABELS[value] }))
export const unitOptions = UNITS.map((value) => ({ value, label: UNIT_LABELS[value] }))

export function merchantLabel(merchant: Merchant | null): string {
  return merchant ? MERCHANT_LABELS[merchant] : ANY_SHOP_LABEL
}

const numberFormat = new Intl.NumberFormat('en', { maximumFractionDigits: 3 })

/** "1.5 kg", "4 pcs", "1 pack", "2 packs" */
export function formatQuantity(quantity: number, unit: QuantityUnit): string {
  const label = unit === 'PACK' && quantity !== 1 ? 'packs' : UNIT_LABELS[unit]
  return `${numberFormat.format(quantity)} ${label}`
}
