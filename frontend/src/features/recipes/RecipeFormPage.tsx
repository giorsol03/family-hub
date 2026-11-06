import { Alert, Loader, Stack, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useNavigate, useParams } from 'react-router'
import { RecipeForm } from './RecipeForm'
import { useRecipe, useRecipeTags, useSaveRecipe } from './useRecipes'

export function RecipeFormPage() {
  const params = useParams()
  const id = params.id !== undefined ? Number(params.id) : undefined
  const navigate = useNavigate()
  const { data: recipe, isPending, isError } = useRecipe(id)
  const { data: tags = [] } = useRecipeTags()
  const save = useSaveRecipe(id)

  return (
    <Stack>
      <Title order={3}>{id === undefined ? 'New recipe' : 'Edit recipe'}</Title>
      {id !== undefined && isPending && <Loader />}
      {id !== undefined && isError && <Alert color="red">Recipe not found.</Alert>}
      {(id === undefined || recipe) && (
        <RecipeForm
          recipe={recipe}
          tagSuggestions={tags}
          onSubmit={async (input) => {
            const saved = await save.mutateAsync(input)
            notifications.show({ color: 'teal', message: `${saved.title} saved` })
            navigate(`/recipes/${saved.id}`, { replace: true })
          }}
          onCancel={() => navigate(id === undefined ? '/recipes' : `/recipes/${id}`)}
        />
      )}
    </Stack>
  )
}
