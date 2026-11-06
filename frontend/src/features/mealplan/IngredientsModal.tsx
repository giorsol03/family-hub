import { Button, Checkbox, Group, Modal, Stack, Text } from '@mantine/core'
import { useState } from 'react'
import type { Ingredient } from '../../api/types'
import { formatQuantity } from '../groceries/labels'

interface Props {
  opened: boolean
  recipeTitle: string
  ingredients: Ingredient[]
  adding?: boolean
  onAdd: (indexes: number[]) => void
  onSkip: () => void
}

/**
 * "Should we add these ingredients to the grocery list?" — every ingredient ticked by default,
 * with Select all / none, and Add selected / Skip.
 */
export function IngredientsModal({ opened, recipeTitle, ingredients, adding, onAdd, onSkip }: Props) {
  const [selected, setSelected] = useState<Set<number>>(() => new Set(ingredients.map((_, i) => i)))
  const allSelected = selected.size === ingredients.length

  function toggle(index: number) {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(index)) next.delete(index)
      else next.add(index)
      return next
    })
  }

  return (
    <Modal opened={opened} onClose={onSkip} title="Add to the grocery list?" centered>
      <Stack>
        <Text size="sm">
          Should we add the ingredients for <b>{recipeTitle}</b> to the grocery list?
        </Text>
        <Group justify="space-between">
          <Text size="xs" c="dimmed">{selected.size} of {ingredients.length} selected</Text>
          <Group gap={4}>
            <Button size="compact-xs" variant="subtle" disabled={allSelected}
                    onClick={() => setSelected(new Set(ingredients.map((_, i) => i)))}>Select all</Button>
            <Button size="compact-xs" variant="subtle" disabled={selected.size === 0}
                    onClick={() => setSelected(new Set())}>Select none</Button>
          </Group>
        </Group>
        <Stack gap="xs">
          {ingredients.map((ing, i) => (
            <Checkbox
              key={i}
              checked={selected.has(i)}
              onChange={() => toggle(i)}
              label={<><b>{formatQuantity(ing.quantity, ing.unit)}</b> {ing.name}</>}
              aria-label={ing.name}
            />
          ))}
        </Stack>
        <Group justify="flex-end">
          <Button variant="default" onClick={onSkip}>Skip</Button>
          <Button disabled={selected.size === 0} loading={adding}
                  onClick={() => onAdd([...selected].sort((a, b) => a - b))}>
            Add selected ({selected.size})
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
