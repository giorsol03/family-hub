import { ActionIcon, Alert, Anchor, Badge, Button, Group, Loader, Modal, Paper, Stack, Switch, Text, Title, Tooltip } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconArrowLeft, IconEye, IconEyeOff, IconPencil, IconPlus } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { GroceryDefault } from '../../api/types'
import { GroceryForm, type GroceryFormResult } from './GroceryForm'
import { CATEGORY_LABELS, formatQuantity, merchantLabel } from './labels'
import { useGroceryDefaults, useSaveDefault, useSetDefaultActive } from './useGroceries'

type Editor = { open: false } | { open: true; item?: GroceryDefault }

/** "Favourites" = grocery defaults: reusable items that can be added to the list in one tap. */
export function GroceryDefaultsPage() {
  const [showInactive, setShowInactive] = useState(false)
  const { data: defaults, isPending, isError } = useGroceryDefaults(showInactive)
  const setActive = useSetDefaultActive()
  const [editor, setEditor] = useState<Editor>({ open: false })
  const isPhone = useMediaQuery('(max-width: 48em)')
  const editing = editor.open ? editor.item : undefined
  const save = useSaveDefault(editing?.id)

  async function handleSubmit(values: GroceryFormResult) {
    const saved = await save.mutateAsync({
      name: values.name,
      category: values.category,
      defaultQuantity: values.quantity,
      unit: values.unit,
      preferredMerchant: values.merchant,
    })
    notifications.show({ color: 'teal', message: `${saved.name} ${editing ? 'updated' : 'added to favourites'}` })
    setEditor({ open: false })
  }

  function toggleActive(d: GroceryDefault) {
    setActive.mutate({ id: d.id, active: !d.active }, {
      onSuccess: (r) => notifications.show({ color: 'teal', message: `${r.name} ${r.active ? 'shown again' : 'hidden'}` }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  return (
    <Stack>
      <Anchor component={Link} to="/groceries" size="sm">
        <Group gap={4}><IconArrowLeft size={14} />Groceries</Group>
      </Anchor>
      <Group justify="space-between">
        <Title order={3}>Favourites</Title>
        <Button leftSection={<IconPlus size={18} />} onClick={() => setEditor({ open: true })}>Add</Button>
      </Group>
      <Text size="sm" c="dimmed">Items you buy often. Tap one on the grocery list to add it in one go.</Text>
      <Switch label="Show hidden favourites" checked={showInactive}
              onChange={(e) => setShowInactive(e.currentTarget.checked)} />

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load favourites.</Alert>}

      {defaults?.map((d) => (
        <Paper key={d.id} withBorder p="sm" opacity={d.active ? 1 : 0.6}>
          <Group wrap="nowrap">
            <div style={{ flex: 1, minWidth: 0 }}>
              <Group gap={6}>
                <Text fw={600} truncate>{d.name}</Text>
                {!d.active && <Badge size="xs" color="gray">Hidden</Badge>}
              </Group>
              <Text size="sm" c="dimmed">
                {formatQuantity(d.defaultQuantity, d.unit)} · {CATEGORY_LABELS[d.category]} · {merchantLabel(d.preferredMerchant)}
              </Text>
            </div>
            <Group gap={4} wrap="nowrap">
              <Tooltip label="Edit">
                <ActionIcon variant="subtle" color="gray" size="lg" onClick={() => setEditor({ open: true, item: d })}
                            aria-label={`Edit ${d.name}`}>
                  <IconPencil size={18} />
                </ActionIcon>
              </Tooltip>
              <Tooltip label={d.active ? 'Hide' : 'Show again'}>
                <ActionIcon variant="subtle" color={d.active ? 'red' : 'teal'} size="lg" onClick={() => toggleActive(d)}
                            aria-label={`${d.active ? 'Hide' : 'Show'} ${d.name}`}>
                  {d.active ? <IconEyeOff size={18} /> : <IconEye size={18} />}
                </ActionIcon>
              </Tooltip>
            </Group>
          </Group>
        </Paper>
      ))}

      <Modal opened={editor.open} onClose={() => setEditor({ open: false })}
             title={editing ? 'Edit favourite' : 'Add favourite'} fullScreen={isPhone}>
        <GroceryForm
          key={editing?.id ?? 'new'}
          withNotes={false}
          initial={editing && {
            name: editing.name,
            category: editing.category,
            quantity: editing.defaultQuantity,
            unit: editing.unit,
            merchant: editing.preferredMerchant,
          }}
          onSubmit={handleSubmit}
          onCancel={() => setEditor({ open: false })}
        />
      </Modal>
    </Stack>
  )
}
