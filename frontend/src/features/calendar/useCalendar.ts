import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { calendarApi } from '../../api/calendar'
import type { EventInput, IsoDate } from '../../api/types'

const CALENDAR_KEY = ['calendar'] as const

export function useCalendarRange(from: IsoDate, to: IsoDate, memberId: number | null) {
  return useQuery({
    queryKey: [...CALENDAR_KEY, from, to, memberId],
    queryFn: () => calendarApi.range(from, to, memberId),
    placeholderData: (previous) => previous,
  })
}

export function useSaveEvent(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: EventInput) => (id === undefined ? calendarApi.create(input) : calendarApi.update(id, input)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CALENDAR_KEY }),
  })
}

export function useDeleteEvent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => calendarApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CALENDAR_KEY }),
  })
}
