import { ActionIcon, Badge, Checkbox, Group, Menu, Text, UnstyledButton } from '@mantine/core'
import { IconClock, IconDots, IconPencil, IconPlayerSkipForward, IconRepeat, IconTrash } from '@tabler/icons-react'
import type { Chore, IsoDate } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import { describeRecurrence, formatDay } from './dates'

interface Props {
  chore: Chore
  today: IsoDate
  /** Show the scheduled date (overdue view). */
  showDate?: boolean
  onToggle: (chore: Chore) => void
  onSkip: (chore: Chore) => void
  onEdit: (chore: Chore) => void
  onDelete: (chore: Chore) => void
}

export function ChoreRow({ chore, today, showDate, onToggle, onSkip, onEdit, onDelete }: Props) {
  const finished = chore.status !== 'TODO'
  const overdue = !finished && chore.scheduledDate < today

  return (
    <Group wrap="nowrap" gap="sm" px="sm" py={10}>
      <Checkbox
        size="md"
        radius="xl"
        checked={chore.status === 'DONE'}
        indeterminate={chore.status === 'SKIPPED'}
        onChange={() => onToggle(chore)}
        aria-label={`${finished ? 'Reopen' : 'Done:'} ${chore.title}`}
      />
      <UnstyledButton style={{ flex: 1, minWidth: 0 }} onClick={() => onEdit(chore)} aria-label={`Edit ${chore.title}`}>
        <Text fw={500} truncate td={finished ? 'line-through' : undefined} c={finished ? 'dimmed' : undefined}>
          {chore.title}
        </Text>
        <Group gap={8} wrap="nowrap">
          {showDate && (
            <Text size="xs" c={overdue ? 'red' : 'dimmed'} fw={overdue ? 600 : undefined}>
              {formatDay(chore.scheduledDate, today)}
            </Text>
          )}
          {chore.status === 'SKIPPED' && <Badge size="xs" color="gray" variant="light">Skipped</Badge>}
          {chore.recurrence !== 'NONE' && (
            <Group gap={2} wrap="nowrap" c="dimmed">
              <IconRepeat size={12} aria-label={describeRecurrence(chore.recurrence, null)} />
            </Group>
          )}
          {chore.estimatedMinutes != null && (
            <Group gap={2} wrap="nowrap">
              <IconClock size={12} color="var(--mantine-color-dimmed)" />
              <Text size="xs" c="dimmed">{chore.estimatedMinutes} min</Text>
            </Group>
          )}
          {chore.status === 'DONE' && chore.completedBy && (
            <Text size="xs" c="dimmed" truncate>by {chore.completedBy.name}</Text>
          )}
        </Group>
      </UnstyledButton>
      {chore.assignee
        ? <MemberAvatar member={chore.assignee} size={26} />
        : <Text size="xs" c="dimmed">Anyone</Text>}
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <ActionIcon variant="subtle" color="gray" aria-label={`More for ${chore.title}`}>
            <IconDots size={18} />
          </ActionIcon>
        </Menu.Target>
        <Menu.Dropdown>
          {!finished && (
            <Menu.Item leftSection={<IconPlayerSkipForward size={16} />} onClick={() => onSkip(chore)}>Skip</Menu.Item>
          )}
          <Menu.Item leftSection={<IconPencil size={16} />} onClick={() => onEdit(chore)}>Edit</Menu.Item>
          <Menu.Item leftSection={<IconTrash size={16} />} color="red" onClick={() => onDelete(chore)}>Delete</Menu.Item>
        </Menu.Dropdown>
      </Menu>
    </Group>
  )
}
