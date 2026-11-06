import { Alert, Center, Loader } from '@mantine/core'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentMember } from './useCurrentMember'

/** Renders child routes only when logged in; otherwise redirects to /login and remembers where to return. */
export function RequireAuth() {
  const { data: me, isPending, isError } = useCurrentMember()
  const location = useLocation()

  if (isPending) {
    return (
      <Center mih="100dvh">
        <Loader />
      </Center>
    )
  }
  if (isError) {
    return (
      <Center mih="100dvh" p="md">
        <Alert color="red" title="Can't reach the server">Please try again in a moment.</Alert>
      </Center>
    )
  }
  if (!me) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
