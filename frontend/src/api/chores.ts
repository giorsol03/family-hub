import { api, request } from './client'
import type {
  Chore, ChoreInput, ChoreList, ChoreTemplate, ChoreTemplateInput, ChoreView, FinishResult, IsoDate,
} from './types'

export const choresApi = {
  list: (view: ChoreView, assigneeId: number | null) =>
    api.get<ChoreList>(`/api/chores?view=${view}${assigneeId != null ? `&assigneeId=${assigneeId}` : ''}`),
  create: (input: ChoreInput) => api.post<Chore>('/api/chores', input),
  createFromTemplate: (templateId: number, scheduledDate?: IsoDate) =>
    api.post<Chore>(`/api/chores/from-template/${templateId}`, { scheduledDate: scheduledDate ?? null }),
  update: (id: number, input: ChoreInput) => api.put<Chore>(`/api/chores/${id}`, input),
  remove: (id: number) => request<void>('DELETE', `/api/chores/${id}`),
  complete: (id: number) => api.post<FinishResult>(`/api/chores/${id}/complete`),
  skip: (id: number) => api.post<FinishResult>(`/api/chores/${id}/skip`),
  reopen: (id: number) => api.post<Chore>(`/api/chores/${id}/reopen`),

  templates: (includeInactive = false) =>
    api.get<ChoreTemplate[]>(`/api/chores/templates?includeInactive=${includeInactive}`),
  createTemplate: (input: ChoreTemplateInput) => api.post<ChoreTemplate>('/api/chores/templates', input),
  updateTemplate: (id: number, input: ChoreTemplateInput) =>
    api.put<ChoreTemplate>(`/api/chores/templates/${id}`, input),
  setTemplateActive: (id: number, active: boolean) =>
    api.post<ChoreTemplate>(`/api/chores/templates/${id}/${active ? 'activate' : 'deactivate'}`),
}
