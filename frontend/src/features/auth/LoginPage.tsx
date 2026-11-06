import { Alert, Button, Center, Checkbox, Group, Paper, PasswordInput, Stack, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconHomeHeart } from '@tabler/icons-react'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { getMe, login } from '../../api/auth'
import { ME_QUERY_KEY, useCurrentMember } from './useCurrentMember'

export function LoginPage() {
  const { data: me } = useCurrentMember()
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: { username: '', password: '', rememberMe: true },
    validate: {
      username: (v) => (v.trim() ? null : 'Enter your username'),
      password: (v) => (v ? null : 'Enter your password'),
    },
  })

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  if (me) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(values: typeof form.values) {
    setError(null)
    setSubmitting(true)
    try {
      await login(values.username.trim(), values.password, values.rememberMe)
      await queryClient.fetchQuery({ queryKey: ME_QUERY_KEY, queryFn: getMe })
      navigate(from, { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Center mih="100dvh" p="md" bg="var(--mantine-color-body)">
      <Paper withBorder shadow="sm" p="xl" w="100%" maw={380}>
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <Stack>
            <Group justify="center" gap={8}>
              <IconHomeHeart size={28} color="var(--mantine-primary-color-filled)" />
              <Title order={2}>Family Hub</Title>
            </Group>

            {error && <Alert color="red" variant="light">{error}</Alert>}

            <TextInput
              label="Username"
              autoComplete="username"
              autoCapitalize="none"
              autoFocus
              key={form.key('username')}
              {...form.getInputProps('username')}
            />
            <PasswordInput
              label="Password"
              autoComplete="current-password"
              key={form.key('password')}
              {...form.getInputProps('password')}
            />
            <Checkbox
              label="Keep me logged in"
              key={form.key('rememberMe')}
              {...form.getInputProps('rememberMe', { type: 'checkbox' })}
            />
            <Button type="submit" fullWidth loading={submitting}>Log in</Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  )
}
