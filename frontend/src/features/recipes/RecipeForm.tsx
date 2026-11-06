import {
  ActionIcon, Alert, Button, Group, NumberInput, Paper, Select, Stack, TagsInput, Text, TextInput, Textarea, Title,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { randomId } from '@mantine/hooks'
import { IconPlus, IconTrash } from '@tabler/icons-react'
import { useState } from 'react'
import { ApiError, toFormErrors } from '../../api/client'
import type { GroceryCategory, QuantityUnit, Recipe, RecipeInput } from '../../api/types'
import { categoryOptions, unitOptions } from '../groceries/labels'

interface IngredientRow {
  key: string
  name: string
  quantity: number | string
  unit: QuantityUnit
  category: GroceryCategory | null
}

interface Values {
  title: string
  description: string
  servings: number | string
  prepMinutes: number | string
  imageUrl: string
  tags: string[]
  ingredients: IngredientRow[]
  instructions: string
}

interface Props {
  recipe?: Recipe
  tagSuggestions: string[]
  onSubmit: (input: RecipeInput) => Promise<unknown>
  onCancel: () => void
}

const emptyIngredient = (): IngredientRow => ({ key: randomId(), name: '', quantity: 1, unit: 'PCS', category: null })
const toNumberOrNull = (v: number | string) => (v === '' ? null : Number(v))

export function RecipeForm({ recipe, tagSuggestions, onSubmit, onCancel }: Props) {
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<Values>({
    mode: 'controlled',
    initialValues: {
      title: recipe?.title ?? '',
      description: recipe?.description ?? '',
      servings: recipe?.servings ?? '',
      prepMinutes: recipe?.prepMinutes ?? '',
      imageUrl: recipe?.imageUrl ?? '',
      tags: recipe?.tags ?? [],
      ingredients: recipe?.ingredients.map((i) => ({ ...i, key: randomId() })) ?? [emptyIngredient()],
      instructions: recipe?.instructions ?? '',
    },
    validate: {
      title: (v) => (!v.trim() ? 'Title is required' : v.length > 150 ? 'Max 150 characters' : null),
      servings: (v) => (v === '' || (Number(v) >= 1 && Number(v) <= 100) ? null : '1–100'),
      prepMinutes: (v) => (v === '' || (Number(v) >= 0 && Number(v) <= 1440) ? null : '0–1440 minutes'),
      imageUrl: (v) => (!v.trim() || /^https?:\/\/\S+$/.test(v.trim()) ? null : 'Must start with http:// or https://'),
      ingredients: {
        name: (v) => (v.trim() ? null : 'Name?'),
        quantity: (v) => (v !== '' && Number(v) > 0 ? null : '> 0'),
      },
    },
  })

  async function handleSubmit(values: Values) {
    setFormError(null)
    try {
      await onSubmit({
        title: values.title.trim(),
        description: values.description.trim() || null,
        servings: toNumberOrNull(values.servings),
        prepMinutes: toNumberOrNull(values.prepMinutes),
        imageUrl: values.imageUrl.trim() || null,
        tags: values.tags,
        instructions: values.instructions.trim() || null,
        ingredients: values.ingredients.map(({ name, quantity, unit, category }) => ({
          name: name.trim(), quantity: Number(quantity), unit, category,
        })),
      })
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) form.setErrors(toFormErrors(e.fieldErrors))
      else setFormError(e instanceof Error ? e.message : 'Something went wrong')
    }
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack maw={760}>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Title" required {...form.getInputProps('title')} />
        <Textarea label="Short description" autosize minRows={1} maxRows={3} {...form.getInputProps('description')} />
        <Group grow align="flex-start">
          <NumberInput label="Servings" min={1} max={100} allowDecimal={false} {...form.getInputProps('servings')} />
          <NumberInput label="Prep time (min)" min={0} max={1440} allowDecimal={false}
                       {...form.getInputProps('prepMinutes')} />
        </Group>
        <TagsInput label="Tags" placeholder="e.g. dinner, vegetarian" data={tagSuggestions} clearable
                   {...form.getInputProps('tags')} />
        <TextInput label="Photo URL" placeholder="https://…" {...form.getInputProps('imageUrl')} />

        <Group justify="space-between" mt="sm">
          <Title order={4}>Ingredients</Title>
          <Text size="xs" c="dimmed">Category is used when adding to the grocery list</Text>
        </Group>
        {form.values.ingredients.map((ing, index) => (
          <Paper key={ing.key} withBorder p="xs">
            <Group wrap="nowrap" align="flex-start" gap="xs">
              <Stack gap={6} style={{ flex: 1 }}>
                <TextInput placeholder="Ingredient" aria-label={`Ingredient ${index + 1} name`}
                           {...form.getInputProps(`ingredients.${index}.name`)} />
                <Group grow gap={6} align="flex-start">
                  <NumberInput aria-label={`Ingredient ${index + 1} quantity`} min={0} decimalScale={3}
                               allowNegative={false} {...form.getInputProps(`ingredients.${index}.quantity`)} />
                  <Select aria-label={`Ingredient ${index + 1} unit`} data={unitOptions} allowDeselect={false}
                          {...form.getInputProps(`ingredients.${index}.unit`)} />
                  <Select aria-label={`Ingredient ${index + 1} category`} placeholder="Category" data={categoryOptions}
                          clearable {...form.getInputProps(`ingredients.${index}.category`)} />
                </Group>
              </Stack>
              <ActionIcon variant="subtle" color="red" mt={4} onClick={() => form.removeListItem('ingredients', index)}
                          aria-label={`Remove ingredient ${index + 1}`}>
                <IconTrash size={16} />
              </ActionIcon>
            </Group>
          </Paper>
        ))}
        <Button variant="light" leftSection={<IconPlus size={16} />} style={{ alignSelf: 'flex-start' }}
                onClick={() => form.insertListItem('ingredients', emptyIngredient())}>
          Add ingredient
        </Button>

        <Textarea label="Method" description="One step per line" autosize minRows={4} maxRows={16} mt="sm"
                  {...form.getInputProps('instructions')} />

        <Group>
          <Button type="submit" loading={form.submitting}>Save</Button>
          <Button variant="default" onClick={onCancel}>Cancel</Button>
        </Group>
      </Stack>
    </form>
  )
}
