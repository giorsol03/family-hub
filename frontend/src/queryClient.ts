import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { isUnauthorized } from './api/client'
import { ME_QUERY_KEY } from './features/auth/useCurrentMember'

/**
 * If any request comes back 401 (session expired), forget the current member;
 * RequireAuth then sends the user to the login page.
 */
export function createQueryClient(): QueryClient {
  const client: QueryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: (count, error) => !isUnauthorized(error) && count < 2, refetchOnWindowFocus: false },
    },
    queryCache: new QueryCache({ onError: (error) => handleError(error) }),
    mutationCache: new MutationCache({ onError: (error) => handleError(error) }),
  })

  function handleError(error: unknown) {
    if (isUnauthorized(error)) {
      client.setQueryData(ME_QUERY_KEY, null)
    }
  }

  return client
}
