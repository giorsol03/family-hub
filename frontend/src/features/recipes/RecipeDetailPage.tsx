import { Alert, Anchor, Badge, Button, Group, List, Loader, Modal, Paper, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconArrowLeft, IconClock, IconPencil, IconTrash, IconUsers } from '@tabler/icons-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { formatQuantity } from '../groceries/labels'
import { RecipeImage } from './RecipeImage'
import { formatMinutes, splitSteps } from './steps'
import { useDeleteRecipe, useRecipe } from './useRecipes'

export function RecipeDetailPage() {
  const id = Number(useParams().id)
  const { data: recipe, isPending, isError } = useRecipe(id)
  const remove = useDeleteRecipe()
  const navigate = useNavigate()
  const [confirmDelete, setConfirmDelete] = useState(false)

  async function handleDelete() {
    try {
      await remove.mutateAsync(id)
      notifications.show({ color: 'teal', message: `${recipe?.title} deleted` })
      navigate('/recipes', { replace: true })
    } catch (e) {
      // e.g. 409: the recipe is still on the meal plan — the message says when
      setConfirmDelete(false)
      notifications.show({ color: 'red', message: e instanceof Error ? e.message : 'Could not delete', autoClose: 8000 })
    }
  }

  const back = (
    <Anchor component={Link} to="/recipes" size="sm">
      <Group gap={4}><IconArrowLeft size={14} />Recipes</Group>
    </Anchor>
  )

  if (isPending) return <Stack>{back}<Loader /></Stack>
  if (isError || !recipe) return <Stack>{back}<Alert color="red">Recipe not found.</Alert></Stack>

  const steps = splitSteps(recipe.instructions)

  return (
    <Stack maw={760}>
      {back}
      {recipe.imageUrl && (
        <Paper radius="md" style={{ overflow: 'hidden' }}>
          <RecipeImage src={recipe.imageUrl} alt={recipe.title} height={220} />
        </Paper>
      )}
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <Title order={2}>{recipe.title}</Title>
        <Group gap="xs" wrap="nowrap">
          <Button component={Link} to={`/recipes/${id}/edit`} variant="default" size="xs"
                  leftSection={<IconPencil size={14} />}>Edit</Button>
          <Button variant="subtle" color="red" size="xs" onClick={() => setConfirmDelete(true)}
                  aria-label="Delete recipe"><IconTrash size={16} /></Button>
        </Group>
      </Group>
      <Group gap="md">
        {recipe.prepMinutes != null && (
          <Group gap={4}><IconClock size={16} /><Text size="sm">{formatMinutes(recipe.prepMinutes)}</Text></Group>
        )}
        {recipe.servings != null && (
          <Group gap={4}><IconUsers size={16} /><Text size="sm">Serves {recipe.servings}</Text></Group>
        )}
        {recipe.tags.map((t) => <Badge key={t} variant="light">{t}</Badge>)}
      </Group>
      {recipe.description && <Text>{recipe.description}</Text>}

      <Title order={4} mt="sm">Ingredients</Title>
      {recipe.ingredients.length === 0
        ? <Text c="dimmed" size="sm">No ingredients listed.</Text>
        : (
          <List spacing={4}>
            {recipe.ingredients.map((ing, i) => (
              <List.Item key={i}>
                <Text span fw={600}>{formatQuantity(ing.quantity, ing.unit)}</Text> {ing.name}
              </List.Item>
            ))}
          </List>
        )}

      <Title order={4} mt="sm">Method</Title>
      {steps.length === 0
        ? <Text c="dimmed" size="sm">No instructions yet.</Text>
        : (
          <List type="ordered" spacing="sm">
            {steps.map((step, i) => <List.Item key={i}>{step}</List.Item>)}
          </List>
        )}

      <Modal opened={confirmDelete} onClose={() => setConfirmDelete(false)} title="Delete recipe?" centered size="sm">
        <Text size="sm" mb="md">“{recipe.title}” will be removed permanently.</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => setConfirmDelete(false)}>Cancel</Button>
          <Button color="red" loading={remove.isPending} onClick={handleDelete}>Delete</Button>
        </Group>
      </Modal>
    </Stack>
  )
}
