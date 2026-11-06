import { Button, Modal } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconTrash } from '@tabler/icons-react'
import type { GroceryDefault, GroceryItem } from '../../api/types'
import { GroceryForm, type GroceryFormResult } from './GroceryForm'
import { formatQuantity } from './labels'
import { useAddItem, useDeleteItem, useUpdateItem } from './useGroceries'

interface Props {
  opened: boolean
  onClose: () => void
  /** Item being edited; undefined = adding a new one. */
  item?: GroceryItem
  suggestions: GroceryDefault[]
}

export function GroceryItemModal({ opened, onClose, item, suggestions }: Props) {
  const isPhone = useMediaQuery('(max-width: 48em)')
  const add = useAddItem()
  const update = useUpdateItem()
  const remove = useDeleteItem()

  async function handleSubmit(values: GroceryFormResult) {
    if (item) {
      await update.mutateAsync({ id: item.id, input: values })
      notifications.show({ color: 'teal', message: `${values.name} updated` })
    } else {
      const result = await add.mutateAsync(values)
      notifications.show({
        color: 'teal',
        message: result.merged
          ? `${result.item.name}: now ${formatQuantity(result.item.quantity, result.item.unit)} (merged)`
          : `Added ${result.item.name}`,
      })
    }
    onClose()
  }

  async function handleDelete() {
    if (!item) return
    await remove.mutateAsync(item.id)
    notifications.show({ color: 'teal', message: `Removed ${item.name}` })
    onClose()
  }

  return (
    <Modal opened={opened} onClose={onClose} title={item ? 'Edit item' : 'Add item'} fullScreen={isPhone}>
      <GroceryForm
        key={item?.id ?? 'new'}
        initial={item && {
          name: item.name,
          category: item.category,
          quantity: item.quantity,
          unit: item.unit,
          merchant: item.merchant,
          notes: item.notes ?? '',
        }}
        suggestions={item ? [] : suggestions}
        submitLabel={item ? 'Save' : 'Add'}
        onSubmit={handleSubmit}
        onCancel={onClose}
        extraActions={item && (
          <Button variant="subtle" color="red" leftSection={<IconTrash size={16} />}
                  loading={remove.isPending} onClick={handleDelete}>
            Delete
          </Button>
        )}
      />
    </Modal>
  )
}
