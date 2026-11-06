import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { recipesApi } from '../../api/recipes'
import type { RecipeInput } from '../../api/types'

const RECIPES_KEY = ['recipes'] as const

export function useRecipes(q: string, tag: string | null) {
  return useQuery({
    queryKey: [...RECIPES_KEY, 'list', { q, tag }],
    queryFn: () => recipesApi.list(q, tag),
    placeholderData: (previous) => previous, // keep showing results while typing
  })
}

export function useRecipeTags() {
  return useQuery({ queryKey: [...RECIPES_KEY, 'tags'], queryFn: () => recipesApi.tags() })
}

export function useRecipe(id: number | undefined) {
  return useQuery({
    queryKey: [...RECIPES_KEY, 'detail', id],
    queryFn: () => recipesApi.get(id!),
    enabled: id !== undefined,
  })
}

export function useSaveRecipe(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: RecipeInput) => (id === undefined ? recipesApi.create(input) : recipesApi.update(id, input)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RECIPES_KEY }),
  })
}

export function useDeleteRecipe() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => recipesApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RECIPES_KEY }),
  })
}
