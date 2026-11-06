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

// ---- chores ----

export const RECURRENCES = ['NONE', 'DAILY', 'WEEKLY', 'EVERY_N_DAYS', 'MONTHLY'] as const
export type Recurrence = (typeof RECURRENCES)[number]
export type ChoreStatus = 'TODO' | 'DONE' | 'SKIPPED'
export type ChoreView = 'TODAY' | 'WEEK' | 'OVERDUE'

/** ISO date, e.g. "2026-11-09" */
export type IsoDate = string

export interface Chore {
  id: number
  title: string
  description: string | null
  scheduledDate: IsoDate
  assignee: MemberSummary | null
  status: ChoreStatus
  completedAt: string | null
  completedBy: MemberSummary | null
  templateId: number | null
  recurrence: Recurrence
  estimatedMinutes: number | null
}

export interface ChoreList {
  view: ChoreView
  today: IsoDate
  from: IsoDate | null
  to: IsoDate
  chores: Chore[]
}

export interface ChoreInput {
  title: string
  description: string | null
  scheduledDate: IsoDate
  assigneeId: number | null
}

export interface FinishResult {
  chore: Chore
  /** auto-created next occurrence for recurring chores */
  next: Chore | null
}

export interface ChoreTemplate {
  id: number
  name: string
  description: string | null
  defaultAssignee: MemberSummary | null
  estimatedMinutes: number | null
  recurrence: Recurrence
  recurrenceInterval: number | null
  active: boolean
}

export interface ChoreTemplateInput {
  name: string
  description: string | null
  defaultAssigneeId: number | null
  estimatedMinutes: number | null
  recurrence: Recurrence
  recurrenceInterval: number | null
}

export interface MemberInput {
  name: string
  username: string
  color: string
  /** Required when creating; blank keeps the current password when updating. */
  password: string
}
