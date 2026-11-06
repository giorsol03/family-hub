export interface Member {
  id: number
  name: string
  username: string
  color: string
  active: boolean
}

/** Compact member reference embedded in other responses. */
export interface MemberSummary {
  id: number
  name: string
  color: string
}

// ---- groceries ----

export const CATEGORIES = ['FRUIT_VEG', 'DAIRY', 'MEAT', 'BAKERY', 'PANTRY', 'HOUSEHOLD', 'OTHER'] as const
export type GroceryCategory = (typeof CATEGORIES)[number]

export const MERCHANTS = ['LIDL', 'ALPHAMEGA', 'SKLAVENITIS', 'METRO', 'OTHER'] as const
export type Merchant = (typeof MERCHANTS)[number]

export const UNITS = ['PCS', 'PACK', 'G', 'KG', 'ML', 'L'] as const
export type QuantityUnit = (typeof UNITS)[number]

export type GrocerySource = 'MANUAL' | 'DEFAULT' | 'RECIPE'

export interface GroceryItem {
  id: number
  name: string
  category: GroceryCategory
  quantity: number
  unit: QuantityUnit
  merchant: Merchant | null
  notes: string | null
  bought: boolean
  boughtAt: string | null
  source: GrocerySource
  addedBy: MemberSummary | null
  createdAt: string
}

export interface GroceryItemInput {
  name: string
  category: GroceryCategory
  quantity: number
  unit: QuantityUnit
  merchant: Merchant | null
  notes: string | null
}

export interface AddItemResult {
  item: GroceryItem
  /** true when an existing line's quantity was increased instead of adding a new line */
  merged: boolean
}

export interface GroceryDefault {
  id: number
  name: string
  category: GroceryCategory
  defaultQuantity: number
  unit: QuantityUnit
  preferredMerchant: Merchant | null
  active: boolean
}

export interface GroceryDefaultInput {
  name: string
  category: GroceryCategory
  defaultQuantity: number
  unit: QuantityUnit
  preferredMerchant: Merchant | null
}

export interface MemberInput {
  name: string
  username: string
  color: string
  /** Required when creating; blank keeps the current password when updating. */
  password: string
}
