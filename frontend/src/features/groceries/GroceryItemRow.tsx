import { Badge, Checkbox, Group, Text, UnstyledButton } from '@mantine/core'
import type { GroceryItem } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import { formatQuantity } from './labels'

interface Props {
  item: GroceryItem
  onToggle: (item: GroceryItem) => void
  onEdit: (item: GroceryItem) => void
}

/** Tick box on the left (big tap target); tapping the rest of the row opens the editor. */
export function GroceryItemRow({ item, onToggle, onEdit }: Props) {
  return (
    <Group wrap="nowrap" gap="sm" px="sm" py={10}>
      <Checkbox
        size="md"
        radius="xl"
        checked={item.bought}
        onChange={() => onToggle(item)}
        aria-label={`${item.bought ? 'Untick' : 'Tick'} ${item.name}`}
      />
      <UnstyledButton style={{ flex: 1, minWidth: 0 }} onClick={() => onEdit(item)} aria-label={`Edit ${item.name}`}>
        <Group wrap="nowrap" gap="xs" justify="space-between">
          <div style={{ minWidth: 0 }}>
            <Text fw={500} truncate td={item.bought ? 'line-through' : undefined} c={item.bought ? 'dimmed' : undefined}>
              {item.name}
            </Text>
            {item.notes && <Text size="xs" c="dimmed" truncate>{item.notes}</Text>}
          </div>
          <Group gap={6} wrap="nowrap">
            <Badge variant="light" color={item.bought ? 'gray' : undefined} style={{ textTransform: 'none' }}>
              {formatQuantity(item.quantity, item.unit)}
            </Badge>
            {item.addedBy && <MemberAvatar member={item.addedBy} size={22} />}
          </Group>
        </Group>
      </UnstyledButton>
    </Group>
  )
}
