import { Button, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <Stack align="center" mt="xl">
      <Title order={3}>Page not found</Title>
      <Text c="dimmed">That page doesn&apos;t exist (yet).</Text>
      <Button component={Link} to="/" variant="light">Go home</Button>
    </Stack>
  )
}
