import {
  IconBook,
  IconCalendar,
  IconChecklist,
  IconHome,
  IconShoppingCart,
  IconTargetArrow,
  IconToolsKitchen2,
  IconUsers,
  type Icon,
} from '@tabler/icons-react'

export interface NavItem {
  key: string
  label: string
  path: string
  icon: Icon
  /** false = feature not built yet: shown greyed out as "Soon". Flip when its milestone lands. */
  enabled: boolean
  /** Shown in the phone bottom tab bar (max 6). Others live in the header menu on phones. */
  bottomBar: boolean
}

export const NAV_ITEMS: NavItem[] = [
  { key: 'home', label: 'Home', path: '/', icon: IconHome, enabled: true, bottomBar: true },
  { key: 'chores', label: 'Chores', path: '/chores', icon: IconChecklist, enabled: false, bottomBar: true },
  { key: 'groceries', label: 'Groceries', path: '/groceries', icon: IconShoppingCart, enabled: true, bottomBar: true },
  { key: 'meals', label: 'Meals', path: '/meal-plan', icon: IconToolsKitchen2, enabled: false, bottomBar: true },
  { key: 'recipes', label: 'Recipes', path: '/recipes', icon: IconBook, enabled: false, bottomBar: false },
  { key: 'calendar', label: 'Calendar', path: '/calendar', icon: IconCalendar, enabled: false, bottomBar: true },
  { key: 'goals', label: 'Goals', path: '/goals', icon: IconTargetArrow, enabled: false, bottomBar: true },
  { key: 'members', label: 'Members', path: '/members', icon: IconUsers, enabled: true, bottomBar: false },
]

export function isActive(item: NavItem, pathname: string): boolean {
  return item.path === '/' ? pathname === '/' : pathname === item.path || pathname.startsWith(`${item.path}/`)
}
