import { api, request } from './client'
import type { CalendarItem, CalendarRange, EventInput, IsoDate } from './types'

export const calendarApi = {
  range: (from: IsoDate, to: IsoDate, memberId: number | null) =>
    api.get<CalendarRange>(`/api/calendar?from=${from}&to=${to}${memberId != null ? `&memberId=${memberId}` : ''}`),
  create: (input: EventInput) => api.post<CalendarItem>('/api/calendar/events', input),
  update: (id: number, input: EventInput) => api.put<CalendarItem>(`/api/calendar/events/${id}`, input),
  remove: (id: number) => request<void>('DELETE', `/api/calendar/events/${id}`),
}
