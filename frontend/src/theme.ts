import { createTheme } from '@mantine/core'

// Keep it simple: Mantine defaults, one accent colour, rounded corners.
export const theme = createTheme({
  primaryColor: 'teal',
  defaultRadius: 'md',
})

/** Palette offered when picking a member colour. */
export const MEMBER_SWATCHES = [
  '#3b82f6', '#0ca678', '#f59f00', '#e8590c', '#e03131',
  '#d6336c', '#ae3ec9', '#7048e8', '#1098ad', '#495057',
]
