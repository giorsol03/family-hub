import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { goalsApi } from '../../api/goals'
import type { GoalInput, GoalMonth, IsoDate, YearMonth } from '../../api/types'
import { toggleDay } from './gridUtils'

const GOALS_KEY = ['goals'] as const

export function useGoalMonth(month: YearMonth | null, ownerId: number | null) {
  return useQuery({
    queryKey: [...GOALS_KEY, 'month', month, ownerId],
    queryFn: () => goalsApi.month(month, ownerId),
    placeholderData: (previous) => previous,
  })
}

export function useGoalList(includeInactive: boolean) {
  return useQuery({
    queryKey: [...GOALS_KEY, 'list', includeInactive],
    queryFn: () => goalsApi.list(includeInactive),
  })
}

/**
 * Tick / untick a day. The cell flips immediately in every cached month view; when the server answers,
 * the row (with fresh streak and %) replaces the optimistic one.
 */
export function useSetCheck() {
  const queryClient = useQueryClient()
  const monthKey = [...GOALS_KEY, 'month']
  return useMutation({
    mutationFn: ({ goalId, date, done }: { goalId: number; date: IsoDate; done: boolean }) =>
      goalsApi.setCheck(goalId, date, done),
    onMutate: async ({ goalId, date, done }) => {
      await queryClient.cancelQueries({ queryKey: monthKey })
      const previous = queryClient.getQueriesData<GoalMonth>({ queryKey: monthKey })
      queryClient.setQueriesData<GoalMonth>({ queryKey: monthKey }, (m) =>
        m && { ...m, rows: m.rows.map((r) => (r.goal.id === goalId ? toggleDay(r, date, done) : r)) })
      return { previous }
    },
    onSuccess: (updated, { date }) => {
      const month = date.slice(0, 7)
      // The returned row belongs to the ticked day's month…
      queryClient.setQueriesData<GoalMonth>({ queryKey: monthKey }, (m) =>
        m && m.month === month
          ? { ...m, rows: m.rows.map((r) => (r.goal.id === updated.goal.id ? updated : r)) }
          : m)
      // …other cached months may show a different streak now (e.g. ticking 31 Oct affects November).
      queryClient.invalidateQueries({
        queryKey: monthKey,
        predicate: (q) => (q.state.data as GoalMonth | undefined)?.month !== month,
      })
    },
    onError: (_error, _vars, context) => {
      context?.previous.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },
  })
}

export function useSaveGoal(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: GoalInput) => (id === undefined ? goalsApi.create(input) : goalsApi.update(id, input)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: GOALS_KEY }),
  })
}

export function useSetGoalActive() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => goalsApi.setActive(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: GOALS_KEY }),
  })
}
