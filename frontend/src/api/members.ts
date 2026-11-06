import { api } from './client'
import type { Member, MemberInput } from './types'

export const membersApi = {
  list: () => api.get<Member[]>('/api/members'),
  get: (id: number) => api.get<Member>(`/api/members/${id}`),
  create: (input: MemberInput) => api.post<Member>('/api/members', input),
  update: (id: number, input: MemberInput) => api.put<Member>(`/api/members/${id}`, input),
  deactivate: (id: number) => api.post<Member>(`/api/members/${id}/deactivate`),
  activate: (id: number) => api.post<Member>(`/api/members/${id}/activate`),
}
