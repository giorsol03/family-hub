import { Badge, Card, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'
import { NAV_ITEMS } from '../../components/navItems'
import { useCurrentMember } from '../auth/useCurrentMember'

export function HomePage() {
  const { data: me } = useCurrentMember()
  const sections = NAV_ITEMS.filter((item) => item.key !== 'home')

  return (
    <Stack>
      <Title order={3}>Hi {me?.name} 👋</Title>
      <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }}>
        {sections.map((item) => {
          const body = (
            <Stack align="center" gap={6} py="xs">
              <item.icon size={36} stroke={1.5} color="var(--mantine-primary-color-filled)" />
              <Text fw={600}>{item.label}</Text>
              {!item.enabled && <Badge size="xs" variant="light" color="gray">Soon</Badge>}
            </Stack>
          )
          return item.enabled ? (
            <Card key={item.key} withBorder component={Link} to={item.path}>
              {body}
            </Card>
          ) : (
            <Card key={item.key} withBorder opacity={0.5} aria-disabled="true">
              {body}
            </Card>
          )
        })}
      </SimpleGrid>
    </Stack>
  )
}
