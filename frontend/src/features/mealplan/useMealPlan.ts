import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mealPlanApi } from '../../api/mealPlan'
import type { IsoDate, MealSlot } from '../../api/types'

const MEAL_PLAN_KEY = ['mealPlan'] as const

export function useMealWeek(weekOf: IsoDate | null) {
  return useQuery({
    queryKey: [...MEAL_PLAN_KEY, weekOf],
    queryFn: () => mealPlanApi.week(weekOf),
    placeholderData: (previous) => previous, // keep the old week visible while the next one loads
  })
}

export function usePlanRecipe() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ date, slot, recipeId }: { date: IsoDate; slot: MealSlot; recipeId: number }) =>
      mealPlanApi.plan(date, slot, recipeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEAL_PLAN_KEY }),
  })
}

export function useRemoveEntry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => mealPlanApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEAL_PLAN_KEY }),
  })
}

/** Adds ingredients and refreshes the grocery list cache. */
export function useAddToGroceries() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ entryId, indexes }: { entryId: number; indexes: number[] }) =>
      mealPlanApi.addToGroceries(entryId, indexes),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['groceries'] }),
  })
}
