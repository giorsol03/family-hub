import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { membersApi } from '../../api/members'
import type { MemberInput } from '../../api/types'
import { ME_QUERY_KEY } from '../auth/useCurrentMember'

const MEMBERS_KEY = ['members'] as const

export function useMembers() {
  return useQuery({ queryKey: MEMBERS_KEY, queryFn: membersApi.list })
}

export function useMember(id: number | undefined) {
  return useQuery({
    queryKey: [...MEMBERS_KEY, id],
    queryFn: () => membersApi.get(id!),
    enabled: id !== undefined,
  })
}

/** Create when id is undefined, otherwise update. Refreshes the list and "me" (you may have edited yourself). */
export function useSaveMember(id: number | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: MemberInput) => (id === undefined ? membersApi.create(input) : membersApi.update(id, input)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: MEMBERS_KEY })
      queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY })
    },
  })
}

export function useSetMemberActive() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      active ? membersApi.activate(id) : membersApi.deactivate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBERS_KEY }),
  })
}
