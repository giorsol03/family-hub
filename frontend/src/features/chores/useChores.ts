import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { choresApi } from '../../api/chores'
import type { ChoreInput, ChoreList, ChoreStatus, ChoreTemplateInput, ChoreView, IsoDate } from '../../api/types'

const CHORES_KEY = ['chores', 'list'] as const
const TEMPLATES_KEY = ['chores', 'templates'] as const

export function useChoreList(view: ChoreView, assigneeId: number | null) {
  return useQuery({
    queryKey: [...CHORES_KEY, view, assigneeId],
    queryFn: () => choresApi.list(view, assigneeId),
  })
}

export function useChoreTemplates(includeInactive = false) {
  return useQuery({
    queryKey: [...TEMPLATES_KEY, { includeInactive }],
    queryFn: () => choresApi.templates(includeInactive),
  })
}

function useInvalidateChores() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: CHORES_KEY })
}

export function useSaveChore(id: number | undefined) {
  const invalidate = useInvalidateChores()
  return useMutation({
    mutationFn: (input: ChoreInput) => (id === undefined ? choresApi.create(input) : choresApi.update(id, input)),
    onSuccess: invalidate,
  })
}

export function useCreateFromTemplate() {
  const invalidate = useInvalidateChores()
  return useMutation({
    mutationFn: ({ templateId, date }: { templateId: number; date?: IsoDate }) =>
      choresApi.createFromTemplate(templateId, date),
    onSuccess: invalidate,
  })
}

export function useDeleteChore() {
  const invalidate = useInvalidateChores()
  return useMutation({ mutationFn: (id: number) => choresApi.remove(id), onSuccess: invalidate })
}

type Action = 'complete' | 'skip' | 'reopen'
const STATUS_AFTER: Record<Action, ChoreStatus> = { complete: 'DONE', skip: 'SKIPPED', reopen: 'TODO' }

/**
 * Complete / skip / reopen. Optimistic: the chore's status changes in every cached view at once,
 * then all views are refetched (a recurring chore may have created its next occurrence).
 */
export function useChoreAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, action }: { id: number; action: Action }) => {
      if (action === 'reopen') {
        return { chore: await choresApi.reopen(id), next: null }
      }
      return action === 'complete' ? choresApi.complete(id) : choresApi.skip(id)
    },
    onMutate: async ({ id, action }) => {
      await queryClient.cancelQueries({ queryKey: CHORES_KEY })
      const previous = queryClient.getQueriesData<ChoreList>({ queryKey: CHORES_KEY })
      queryClient.setQueriesData<ChoreList>({ queryKey: CHORES_KEY }, (list) =>
        list && { ...list, chores: list.chores.map((c) => (c.id === id ? { ...c, status: STATUS_AFTER[action] } : c)) },
      )
      return { previous }
    },
    onError: (_error, _vars, context) => {
      context?.previous.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: CHORES_KEY }),
  })
}

export function useSaveTemplate(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: ChoreTemplateInput) =>
      id === undefined ? choresApi.createTemplate(input) : choresApi.updateTemplate(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY }),
  })
}

export function useSetTemplateActive() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => choresApi.setTemplateActive(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY }),
  })
}
