import { MantineProvider } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import type { Member } from '../api/types'
import { ME_QUERY_KEY } from '../features/auth/useCurrentMember'
import { theme } from '../theme'

export const ADMIN: Member = { id: 1, name: 'Admin', username: 'admin', color: '#3b82f6', active: true }

interface Options {
  route?: string
  me?: Member | null
}

/** Renders with Mantine, a fresh QueryClient (optionally pre-seeded with the current member) and a router. */
export function renderWithProviders(ui: ReactElement, { route = '/', me = ADMIN }: Options = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  queryClient.setQueryData(ME_QUERY_KEY, me)
  return render(
    <MantineProvider theme={theme} env="test">
      <Notifications />
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </QueryClientProvider>
    </MantineProvider>,
  )
}
