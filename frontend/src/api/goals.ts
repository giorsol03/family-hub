import { api } from './client'
import type { Goal, GoalInput, GoalMonth, GoalRow, IsoDate, YearMonth } from './types'

export const goalsApi = {
  month: (month: YearMonth | null, ownerId: number | null) => {
    const params = new URLSearchParams()
    if (month) params.set('month', month)
    if (ownerId != null) params.set('ownerId', String(ownerId))
    const query = params.toString()
    return api.get<GoalMonth>(`/api/goals/month${query ? `?${query}` : ''}`)
  },
  setCheck: (goalId: number, date: IsoDate, done: boolean) =>
    api.put<GoalRow>(`/api/goals/${goalId}/checks/${date}`, { done }),
  list: (includeInactive = false) => api.get<Goal[]>(`/api/goals?includeInactive=${includeInactive}`),
  create: (input: GoalInput) => api.post<Goal>('/api/goals', input),
  update: (id: number, input: GoalInput) => api.put<Goal>(`/api/goals/${id}`, input),
  setActive: (id: number, active: boolean) =>
    api.post<Goal>(`/api/goals/${id}/${active ? 'activate' : 'deactivate'}`),
}
