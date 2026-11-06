import { Alert, Badge, Button, Card, Chip, Group, Loader, ScrollArea, SimpleGrid, Stack, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconClock, IconPlus, IconSearch, IconUsers } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { RecipeImage } from './RecipeImage'
import { formatMinutes } from './steps'
import { useRecipeTags, useRecipes } from './useRecipes'

export function RecipesPage() {
  const [search, setSearch] = useState('')
  const [debounced] = useDebouncedValue(search.trim(), 250)
  const [tag, setTag] = useState<string | null>(null)
  const { data: recipes, isPending, isError } = useRecipes(debounced, tag)
  const { data: tags = [] } = useRecipeTags()

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Recipes</Title>
        <Button component={Link} to="/recipes/new" leftSection={<IconPlus size={18} />}>Add</Button>
      </Group>

      <TextInput
        placeholder="Search recipes or ingredients"
        leftSection={<IconSearch size={16} />}
        value={search}
        onChange={(e) => setSearch(e.currentTarget.value)}
        aria-label="Search recipes"
      />

      {tags.length > 0 && (
        <ScrollArea type="never">
          <Chip.Group multiple={false} value={tag ?? ''} onChange={(v) => setTag(v || null)}>
            <Group gap={6} wrap="nowrap">
              <Chip value="" size="sm">All</Chip>
              {tags.map((t) => <Chip key={t} value={t} size="sm">{t}</Chip>)}
            </Group>
          </Chip.Group>
        </ScrollArea>
      )}

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load recipes.</Alert>}

      {recipes && recipes.length === 0 && (
        <Text c="dimmed" ta="center" py="xl">
          {debounced || tag ? 'No recipes match.' : 'No recipes yet. Tap “Add” to write the first one.'}
        </Text>
      )}

      <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }}>
        {recipes?.map((r) => (
          <Card key={r.id} withBorder padding="sm" component={Link} to={`/recipes/${r.id}`}>
            <Card.Section>
              <RecipeImage src={r.imageUrl} alt={r.title} height={140} />
            </Card.Section>
            <Text fw={600} mt="sm" lineClamp={1}>{r.title}</Text>
            {r.description && <Text size="sm" c="dimmed" lineClamp={2}>{r.description}</Text>}
            <Group gap="sm" mt={6}>
              {r.prepMinutes != null && (
                <Group gap={4}><IconClock size={14} /><Text size="xs">{formatMinutes(r.prepMinutes)}</Text></Group>
              )}
              {r.servings != null && (
                <Group gap={4}><IconUsers size={14} /><Text size="xs">{r.servings}</Text></Group>
              )}
              {r.tags.map((t) => <Badge key={t} size="xs" variant="light">{t}</Badge>)}
            </Group>
          </Card>
        ))}
      </SimpleGrid>
    </Stack>
  )
}
