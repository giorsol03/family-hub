import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { groceriesApi } from '../../api/groceries'
import type { GroceryDefaultInput, GroceryItem, GroceryItemInput } from '../../api/types'

const ITEMS_KEY = ['groceries', 'items'] as const
const DEFAULTS_KEY = ['groceries', 'defaults'] as const

export function useGroceryItems() {
  return useQuery({ queryKey: ITEMS_KEY, queryFn: groceriesApi.items })
}

export function useGroceryDefaults(includeInactive = false) {
  return useQuery({
    queryKey: [...DEFAULTS_KEY, { includeInactive }],
    queryFn: () => groceriesApi.defaults(includeInactive),
  })
}

function useInvalidateItems() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ITEMS_KEY })
}

export function useAddItem() {
  const invalidate = useInvalidateItems()
  return useMutation({ mutationFn: (input: GroceryItemInput) => groceriesApi.add(input), onSuccess: invalidate })
}

export function useAddFromDefault() {
  const invalidate = useInvalidateItems()
  return useMutation({ mutationFn: (defaultId: number) => groceriesApi.addFromDefault(defaultId), onSuccess: invalidate })
}

export function useUpdateItem() {
  const invalidate = useInvalidateItems()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: GroceryItemInput }) => groceriesApi.update(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteItem() {
  const invalidate = useInvalidateItems()
  return useMutation({ mutationFn: (id: number) => groceriesApi.remove(id), onSuccess: invalidate })
}

export function useClearBought() {
  const invalidate = useInvalidateItems()
  return useMutation({ mutationFn: () => groceriesApi.clearBought(), onSuccess: invalidate })
}

/** Ticking is optimistic: the checkbox flips instantly and rolls back if the server says no. */
export function useSetBought() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, bought }: { id: number; bought: boolean }) => groceriesApi.setBought(id, bought),
    onMutate: async ({ id, bought }) => {
      await queryClient.cancelQueries({ queryKey: ITEMS_KEY })
      const previous = queryClient.getQueryData<GroceryItem[]>(ITEMS_KEY)
      queryClient.setQueryData<GroceryItem[]>(ITEMS_KEY, (items) =>
        items?.map((i) => (i.id === id ? { ...i, bought, boughtAt: bought ? new Date().toISOString() : null } : i)),
      )
      return { previous }
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) queryClient.setQueryData(ITEMS_KEY, context.previous)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ITEMS_KEY }),
  })
}

export function useSaveDefault(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: GroceryDefaultInput) =>
      id === undefined ? groceriesApi.createDefault(input) : groceriesApi.updateDefault(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: DEFAULTS_KEY }),
  })
}

export function useSetDefaultActive() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => groceriesApi.setDefaultActive(id, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: DEFAULTS_KEY }),
  })
}
