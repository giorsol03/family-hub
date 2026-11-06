import { Avatar, Badge, Button, Group, Paper, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconMapPin, IconPlus } from '@tabler/icons-react'
import type { CalendarItem, IsoDate } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import { formatDay } from '../chores/dates'
import { formatItemTime, itemColor } from './calendarUtils'

interface Props {
  date: IsoDate
  today: IsoDate
  items: CalendarItem[]
  onAdd: (date: IsoDate) => void
  onOpen: (item: CalendarItem) => void
  compact?: boolean
}

/** The list of things on one day, with an "Add event" button. */
export function DayAgenda({ date, today, items, onAdd, onOpen, compact }: Props) {
  return (
    <Stack gap={4}>
      <Group justify="space-between">
        <Text fw={700} size={compact ? 'sm' : 'md'} c={date === today ? undefined : compact ? 'dimmed' : undefined}>
          {formatDay(date, today)}
        </Text>
        <Button size="compact-xs" variant="subtle" leftSection={<IconPlus size={12} />} onClick={() => onAdd(date)}
                aria-label={`Add event on ${formatDay(date, today)}`}>
          Add
        </Button>
      </Group>
      {items.length === 0 && <Text size="sm" c="dimmed" pl="sm">{compact ? '—' : 'Nothing planned.'}</Text>}
      {items.map((item) => (
        <Paper key={`${item.kind}-${item.id}`} withBorder p={0}
               style={{ borderLeft: `4px solid var(--mantine-color-${itemColor(item)}-filled)` }}>
          <UnstyledButton w="100%" p="xs" onClick={() => onOpen(item)} aria-label={`Open ${item.title}`}>
            <Group justify="space-between" wrap="nowrap" align="flex-start">
              <div style={{ minWidth: 0 }}>
                <Text fw={500} truncate>{item.title}</Text>
                <Group gap={8}>
                  <Text size="xs" c="dimmed">{formatItemTime(item)}</Text>
                  {item.location && (
                    <Group gap={2} wrap="nowrap"><IconMapPin size={12} color="var(--mantine-color-dimmed)" />
                      <Text size="xs" c="dimmed" truncate>{item.location}</Text></Group>
                  )}
                  {item.kind === 'MEAL_PLAN' && <Badge size="xs" variant="light">Meal plan</Badge>}
                </Group>
              </div>
              {item.attendees.length > 0 && (
                <Avatar.Group spacing="xs">
                  {item.attendees.map((a) => <MemberAvatar key={a.id} member={a} size={24} />)}
                </Avatar.Group>
              )}
            </Group>
          </UnstyledButton>
        </Paper>
      ))}
    </Stack>
  )
}
