import {
  ActionIcon, Alert, Button, Divider, Group, Loader, Modal, Paper, ScrollArea, Stack, Text, Title, Tooltip,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconPlus, IconShoppingCartCheck, IconStar } from '@tabler/icons-react'
import { Fragment, useState } from 'react'
import { Link } from 'react-router'
import type { GroceryDefault, GroceryItem } from '../../api/types'
import { GroceryItemModal } from './GroceryItemModal'
import { GroceryItemRow } from './GroceryItemRow'
import { groupByMerchantAndCategory, splitBought } from './grouping'
import { CATEGORY_LABELS, formatQuantity, merchantLabel } from './labels'
import { useAddFromDefault, useClearBought, useGroceryDefaults, useGroceryItems, useSetBought } from './useGroceries'

type Editor = { open: false } | { open: true; item?: GroceryItem }

export function GroceryListPage() {
  const { data: items, isPending, isError } = useGroceryItems()
  const { data: defaults = [] } = useGroceryDefaults()
  const setBought = useSetBought()
  const addFromDefault = useAddFromDefault()
  const clearBought = useClearBought()
  const [editor, setEditor] = useState<Editor>({ open: false })
  const [confirmClear, setConfirmClear] = useState(false)

  const { toBuy, bought } = splitBought(items ?? [])
  const groups = groupByMerchantAndCategory(toBuy)

  function toggle(item: GroceryItem) {
    setBought.mutate(
      { id: item.id, bought: !item.bought },
      { onError: (e) => notifications.show({ color: 'red', message: e.message }) },
    )
  }

  function quickAdd(d: GroceryDefault) {
    addFromDefault.mutate(d.id, {
      onSuccess: (result) => notifications.show({
        color: 'teal',
        message: result.merged
          ? `${result.item.name}: now ${formatQuantity(result.item.quantity, result.item.unit)}`
          : `Added ${result.item.name}`,
      }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  async function handleClearBought() {
    const { removed } = await clearBought.mutateAsync()
    setConfirmClear(false)
    notifications.show({ color: 'teal', message: `Cleared ${removed} bought item${removed === 1 ? '' : 's'}` })
  }

  const edit = (item: GroceryItem) => setEditor({ open: true, item })

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Groceries</Title>
        <Group gap="xs">
          <Tooltip label="Favourites">
            <ActionIcon component={Link} to="/groceries/defaults" variant="default" size="lg" aria-label="Favourites">
              <IconStar size={18} />
            </ActionIcon>
          </Tooltip>
          <Button leftSection={<IconPlus size={18} />} onClick={() => setEditor({ open: true })}>Add</Button>
        </Group>
      </Group>

      {defaults.length > 0 && (
        <ScrollArea type="never" offsetScrollbars={false}>
          <Group gap={6} wrap="nowrap" pb={2}>
            {defaults.map((d) => (
              <Button key={d.id} size="compact-sm" radius="xl" variant="light" style={{ flexShrink: 0 }}
                      leftSection={<IconPlus size={12} />} onClick={() => quickAdd(d)}
                      aria-label={`Quick add ${d.name}`}>
                {d.name}
              </Button>
            ))}
          </Group>
        </ScrollArea>
      )}

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load the grocery list.</Alert>}

      {items && toBuy.length === 0 && (
        <Paper withBorder p="xl">
          <Stack align="center" gap={4}>
            <IconShoppingCartCheck size={40} stroke={1.4} color="var(--mantine-color-dimmed)" />
            <Text fw={500}>Nothing to buy</Text>
            <Text size="sm" c="dimmed">Tap a favourite above or “Add” to start the list.</Text>
          </Stack>
        </Paper>
      )}

      {groups.map((group) => (
        <Stack key={group.merchant ?? 'any'} gap={6}>
          <Group justify="space-between">
            <Title order={5}>{merchantLabel(group.merchant)}</Title>
            <Text size="sm" c="dimmed">{group.count}</Text>
          </Group>
          <Paper withBorder>
            {group.categories.map((cat, index) => (
              <Fragment key={cat.category}>
                {index > 0 && <Divider />}
                <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="sm" pt="xs">
                  {CATEGORY_LABELS[cat.category]}
                </Text>
                {cat.items.map((item) => (
                  <GroceryItemRow key={item.id} item={item} onToggle={toggle} onEdit={edit} />
                ))}
              </Fragment>
            ))}
          </Paper>
        </Stack>
      ))}

      {bought.length > 0 && (
        <Stack gap={6} mt="md">
          <Group justify="space-between">
            <Title order={5} c="dimmed">Bought ({bought.length})</Title>
            <Button size="xs" variant="light" color="red" onClick={() => setConfirmClear(true)}>Clear bought</Button>
          </Group>
          <Paper withBorder>
            {bought.map((item, index) => (
              <Fragment key={item.id}>
                {index > 0 && <Divider />}
                <GroceryItemRow item={item} onToggle={toggle} onEdit={edit} />
              </Fragment>
            ))}
          </Paper>
        </Stack>
      )}

      <GroceryItemModal
        opened={editor.open}
        item={editor.open ? editor.item : undefined}
        suggestions={defaults}
        onClose={() => setEditor({ open: false })}
      />

      <Modal opened={confirmClear} onClose={() => setConfirmClear(false)} title="Clear bought items?" centered size="sm">
        <Text size="sm" mb="md">This removes the {bought.length} ticked item{bought.length === 1 ? '' : 's'} from the list.</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => setConfirmClear(false)}>Cancel</Button>
          <Button color="red" loading={clearBought.isPending} onClick={handleClearBought}>Clear</Button>
        </Group>
      </Modal>
    </Stack>
  )
}
