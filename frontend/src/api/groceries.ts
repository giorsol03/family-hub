import { api, request } from './client'
import type { AddItemResult, GroceryDefault, GroceryDefaultInput, GroceryItem, GroceryItemInput } from './types'

export const groceriesApi = {
  items: () => api.get<GroceryItem[]>('/api/groceries/items'),
  add: (input: GroceryItemInput) => api.post<AddItemResult>('/api/groceries/items', input),
  addFromDefault: (defaultId: number) => api.post<AddItemResult>(`/api/groceries/items/from-default/${defaultId}`),
  update: (id: number, input: GroceryItemInput) => api.put<GroceryItem>(`/api/groceries/items/${id}`, input),
  setBought: (id: number, bought: boolean) => api.put<GroceryItem>(`/api/groceries/items/${id}/bought`, { bought }),
  remove: (id: number) => request<void>('DELETE', `/api/groceries/items/${id}`),
  clearBought: () => api.post<{ removed: number }>('/api/groceries/items/clear-bought'),

  defaults: (includeInactive = false) =>
    api.get<GroceryDefault[]>(`/api/groceries/defaults?includeInactive=${includeInactive}`),
  createDefault: (input: GroceryDefaultInput) => api.post<GroceryDefault>('/api/groceries/defaults', input),
  updateDefault: (id: number, input: GroceryDefaultInput) =>
    api.put<GroceryDefault>(`/api/groceries/defaults/${id}`, input),
  setDefaultActive: (id: number, active: boolean) =>
    api.post<GroceryDefault>(`/api/groceries/defaults/${id}/${active ? 'activate' : 'deactivate'}`),
}
