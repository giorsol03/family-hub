import { ActionIcon, Alert, Badge, Button, Group, Loader, Menu, Paper, SimpleGrid, Stack, Text, Title, UnstyledButton } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconBook, IconChevronLeft, IconChevronRight, IconPlus, IconShoppingCartPlus, IconTrash } from '@tabler/icons-react'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { mealPlanApi } from '../../api/mealPlan'
import { MEAL_SLOTS } from '../../api/types'
import type { Ingredient, IsoDate, MealPlanEntry, MealSlot } from '../../api/types'
import { IngredientsModal } from './IngredientsModal'
import { RecipePickerModal } from './RecipePickerModal'
import { SLOT_LABELS, entriesFor, formatWeekRange, shiftWeek, weekDays } from './week'
import { useAddToGroceries, useMealWeek, usePlanRecipe, useRemoveEntry } from './useMealPlan'

interface IngredientsPrompt {
  entryId: number
  recipeTitle: string
  ingredients: Ingredient[]
}

export function MealPlanPage() {
  const [weekOf, setWeekOf] = useState<IsoDate | null>(null)
  const { data: week, isPending, isError } = useMealWeek(weekOf)
  const plan = usePlanRecipe()
  const remove = useRemoveEntry()
  const addToGroceries = useAddToGroceries()
  const navigate = useNavigate()
  const [picking, setPicking] = useState<{ date: IsoDate; slot: MealSlot } | null>(null)
  const [prompt, setPrompt] = useState<IngredientsPrompt | null>(null)

  const isCurrentWeek = week ? week.today >= week.weekStart && week.today <= week.weekEnd : true

  function pickRecipe(recipeId: number) {
    if (!picking) return
    plan.mutate({ ...picking, recipeId }, {
      onSuccess: ({ entry, ingredients }) => {
        setPicking(null)
        if (ingredients.length > 0) {
          setPrompt({ entryId: entry.id, recipeTitle: entry.recipe.title, ingredients })
        } else {
          notifications.show({ color: 'teal', message: `${entry.recipe.title} planned` })
        }
      },
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  function confirmAdd(indexes: number[]) {
    if (!prompt) return
    addToGroceries.mutate({ entryId: prompt.entryId, indexes }, {
      onSuccess: ({ added, merged }) => {
        setPrompt(null)
        const parts = [added > 0 && `${added} added`, merged > 0 && `${merged} merged with items already on the list`]
        notifications.show({ color: 'teal', title: 'Grocery list updated', message: parts.filter(Boolean).join(', ') })
      },
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  async function reopenPrompt(entry: MealPlanEntry) {
    try {
      const { ingredients } = await mealPlanApi.entry(entry.id)
      if (ingredients.length === 0) {
        notifications.show({ message: `${entry.recipe.title} has no ingredients listed` })
        return
      }
      setPrompt({ entryId: entry.id, recipeTitle: entry.recipe.title, ingredients })
    } catch (e) {
      notifications.show({ color: 'red', message: e instanceof Error ? e.message : 'Something went wrong' })
    }
  }

  function removeEntry(entry: MealPlanEntry) {
    remove.mutate(entry.id, {
      onSuccess: () => notifications.show({ color: 'teal', message: `${entry.recipe.title} removed from the plan` }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Meal plan</Title>
        <Button component={Link} to="/recipes" variant="default" leftSection={<IconBook size={16} />}>Recipes</Button>
      </Group>

      {week && (
        <Group justify="space-between" wrap="nowrap">
          <ActionIcon variant="default" size="lg" aria-label="Previous week"
                      onClick={() => setWeekOf(shiftWeek(week.weekStart, -1))}>
            <IconChevronLeft size={18} />
          </ActionIcon>
          <Stack gap={0} align="center">
            <Text fw={600}>{formatWeekRange(week.weekStart, week.weekEnd)}</Text>
            {!isCurrentWeek && (
              <Button variant="subtle" size="compact-xs" onClick={() => setWeekOf(null)}>Back to this week</Button>
            )}
          </Stack>
          <ActionIcon variant="default" size="lg" aria-label="Next week"
                      onClick={() => setWeekOf(shiftWeek(week.weekStart, 1))}>
            <IconChevronRight size={18} />
          </ActionIcon>
        </Group>
      )}

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load the meal plan.</Alert>}

      {week && (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4, xl: 7 }} spacing="sm">
          {weekDays(week.weekStart).map((date) => {
            const isToday = date === week.today
            return (
              <Paper key={date} withBorder p="sm"
                     style={isToday ? { borderColor: 'var(--mantine-primary-color-filled)', borderWidth: 2 } : undefined}>
                <Group justify="space-between" mb={6}>
                  <Text fw={700}>{dayjs(date).format('ddd D MMM')}</Text>
                  {isToday && <Badge size="xs">Today</Badge>}
                </Group>
                <Stack gap={8}>
                  {MEAL_SLOTS.map((slot) => (
                    <div key={slot}>
                      <Group justify="space-between" gap={4}>
                        <Text size="xs" c="dimmed" fw={600} tt="uppercase">{SLOT_LABELS[slot]}</Text>
                        <ActionIcon size="sm" variant="subtle" onClick={() => setPicking({ date, slot })}
                                    aria-label={`Add ${SLOT_LABELS[slot].toLowerCase()} on ${dayjs(date).format('ddd D MMM')}`}>
                          <IconPlus size={14} />
                        </ActionIcon>
                      </Group>
                      {entriesFor(week.entries, date, slot).map((entry) => (
                        <Menu key={entry.id} position="bottom-start" withinPortal>
                          <Menu.Target>
                            <UnstyledButton w="100%" aria-label={`${entry.recipe.title} options`}>
                              <Text size="sm" fw={500} truncate>{entry.recipe.title}</Text>
                            </UnstyledButton>
                          </Menu.Target>
                          <Menu.Dropdown>
                            <Menu.Item leftSection={<IconBook size={16} />}
                                       onClick={() => navigate(`/recipes/${entry.recipe.id}`)}>Open recipe</Menu.Item>
                            <Menu.Item leftSection={<IconShoppingCartPlus size={16} />}
                                       onClick={() => reopenPrompt(entry)}>Add ingredients to groceries</Menu.Item>
                            <Menu.Item leftSection={<IconTrash size={16} />} color="red"
                                       onClick={() => removeEntry(entry)}>Remove from plan</Menu.Item>
                          </Menu.Dropdown>
                        </Menu>
                      ))}
                    </div>
                  ))}
                </Stack>
              </Paper>
            )
          })}
        </SimpleGrid>
      )}

      <RecipePickerModal
        opened={picking !== null}
        title={picking ? `${SLOT_LABELS[picking.slot]} · ${dayjs(picking.date).format('ddd D MMM')}` : ''}
        busy={plan.isPending}
        onPick={pickRecipe}
        onClose={() => setPicking(null)}
      />

      {prompt && (
        <IngredientsModal
          key={prompt.entryId}
          opened
          recipeTitle={prompt.recipeTitle}
          ingredients={prompt.ingredients}
          adding={addToGroceries.isPending}
          onAdd={confirmAdd}
          onSkip={() => setPrompt(null)}
        />
      )}
    </Stack>
  )
}
