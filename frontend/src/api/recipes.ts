import { api, request } from './client'
import type { Recipe, RecipeInput, RecipeSummary } from './types'

export const recipesApi = {
  list: (q?: string, tag?: string | null) => {
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (tag) params.set('tag', tag)
    const query = params.toString()
    return api.get<RecipeSummary[]>(`/api/recipes${query ? `?${query}` : ''}`)
  },
  tags: () => api.get<string[]>('/api/recipes/tags'),
  get: (id: number) => api.get<Recipe>(`/api/recipes/${id}`),
  create: (input: RecipeInput) => api.post<Recipe>('/api/recipes', input),
  update: (id: number, input: RecipeInput) => api.put<Recipe>(`/api/recipes/${id}`, input),
  remove: (id: number) => request<void>('DELETE', `/api/recipes/${id}`),
}
