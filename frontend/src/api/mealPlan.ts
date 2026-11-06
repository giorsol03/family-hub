import { api, request } from './client'
import type { AddToGroceriesResult, IsoDate, MealPlanWeek, MealSlot, PlannedResult } from './types'

export const mealPlanApi = {
  week: (weekOf: IsoDate | null) => api.get<MealPlanWeek>(`/api/meal-plan${weekOf ? `?weekOf=${weekOf}` : ''}`),
  plan: (date: IsoDate, slot: MealSlot, recipeId: number) =>
    api.post<PlannedResult>('/api/meal-plan', { date, slot, recipeId }),
  entry: (id: number) => api.get<PlannedResult>(`/api/meal-plan/${id}`),
  remove: (id: number) => request<void>('DELETE', `/api/meal-plan/${id}`),
  addToGroceries: (id: number, ingredientIndexes: number[]) =>
    api.post<AddToGroceriesResult>(`/api/meal-plan/${id}/add-to-groceries`, { ingredientIndexes }),
}
