import { Button, Group, Loader, Modal, ScrollArea, Stack, Text, TextInput, UnstyledButton } from '@mantine/core'
import { useDebouncedValue, useMediaQuery } from '@mantine/hooks'
import { IconClock, IconSearch } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { formatMinutes } from '../recipes/steps'
import { useRecipes } from '../recipes/useRecipes'

interface Props {
  opened: boolean
  title: string
  busy?: boolean
  onPick: (recipeId: number) => void
  onClose: () => void
}

/** Choose a recipe for a meal slot. */
export function RecipePickerModal({ opened, title, busy, onPick, onClose }: Props) {
  const isPhone = useMediaQuery('(max-width: 48em)')
  const [search, setSearch] = useState('')
  const [debounced] = useDebouncedValue(search.trim(), 250)
  const { data: recipes, isPending } = useRecipes(debounced, null)

  return (
    <Modal opened={opened} onClose={onClose} title={title} fullScreen={isPhone}>
      <Stack>
        <TextInput placeholder="Search recipes" leftSection={<IconSearch size={16} />} value={search}
                   onChange={(e) => setSearch(e.currentTarget.value)} aria-label="Search recipes" data-autofocus />
        {(isPending || busy) && <Loader size="sm" />}
        {recipes && recipes.length === 0 && (
          <Stack align="center" gap="xs" py="md">
            <Text c="dimmed" size="sm">{debounced ? 'No recipes match.' : 'No recipes yet.'}</Text>
            <Button component={Link} to="/recipes/new" variant="light" size="xs" onClick={onClose}>Write a recipe</Button>
          </Stack>
        )}
        <ScrollArea.Autosize mah={isPhone ? undefined : 400}>
          <Stack gap={4}>
            {recipes?.map((r) => (
              <UnstyledButton key={r.id} onClick={() => onPick(r.id)} disabled={busy} p="xs"
                              style={{ borderRadius: 'var(--mantine-radius-md)', border: '1px solid var(--mantine-color-default-border)' }}>
                <Group justify="space-between" wrap="nowrap">
                  <Text fw={500} truncate>{r.title}</Text>
                  {r.prepMinutes != null && (
                    <Group gap={4} wrap="nowrap"><IconClock size={14} /><Text size="xs">{formatMinutes(r.prepMinutes)}</Text></Group>
                  )}
                </Group>
              </UnstyledButton>
            ))}
          </Stack>
        </ScrollArea.Autosize>
      </Stack>
    </Modal>
  )
}
