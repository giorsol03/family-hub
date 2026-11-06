import { AppShell, Badge, Group, Menu, NavLink, Text, UnstyledButton } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'
import { IconHomeHeart, IconLogout } from '@tabler/icons-react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { logout } from '../api/auth'
import { ME_QUERY_KEY, useCurrentMember } from '../features/auth/useCurrentMember'
import { MemberAvatar } from './MemberAvatar'
import classes from './AppLayout.module.css'
import { NAV_ITEMS, isActive } from './navItems'

/**
 * Phones: header + bottom tab bar (items not in the bar are in the avatar menu).
 * Tablets/desktop (≥ sm): header + left sidebar with every section.
 */
export function AppLayout() {
  const isPhone = useMediaQuery('(max-width: 48em)', false, { getInitialValueInEffect: false })
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: me } = useCurrentMember()

  async function handleLogout() {
    await logout()
    queryClient.clear()
    queryClient.setQueryData(ME_QUERY_KEY, null)
    navigate('/login', { replace: true })
  }

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 220, breakpoint: 'sm', collapsed: { mobile: true } }}
      footer={{ height: 64, collapsed: !isPhone }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <UnstyledButton component={Link} to="/">
            <Group gap={6}>
              <IconHomeHeart size={24} color="var(--mantine-primary-color-filled)" />
              <Text fw={700}>Family Hub</Text>
            </Group>
          </UnstyledButton>

          {me && (
            <Menu position="bottom-end" width={200} shadow="md">
              <Menu.Target>
                <UnstyledButton aria-label="Account menu">
                  <Group gap={8}>
                    <Text size="sm" visibleFrom="xs">{me.name}</Text>
                    <MemberAvatar member={me} size="md" />
                  </Group>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Label hiddenFrom="sm">More</Menu.Label>
                {NAV_ITEMS.filter((item) => !item.bottomBar).map((item) => (
                  <Menu.Item
                    key={item.key}
                    hiddenFrom="sm"
                    leftSection={<item.icon size={16} />}
                    disabled={!item.enabled}
                    rightSection={item.enabled ? undefined : <Badge size="xs" variant="light">Soon</Badge>}
                    onClick={() => navigate(item.path)}
                  >
                    {item.label}
                  </Menu.Item>
                ))}
                <Menu.Divider hiddenFrom="sm" />
                <Menu.Item leftSection={<IconLogout size={16} />} onClick={handleLogout}>
                  Log out
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          )}
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs" aria-label="Sections">
        {NAV_ITEMS.map((item) =>
          item.enabled ? (
            <NavLink
              key={item.key}
              component={Link}
              to={item.path}
              label={item.label}
              leftSection={<item.icon size={20} stroke={1.6} />}
              active={isActive(item, pathname)}
            />
          ) : (
            <NavLink
              key={item.key}
              component="div"
              label={item.label}
              leftSection={<item.icon size={20} stroke={1.6} />}
              disabled
              aria-disabled="true"
              rightSection={<Badge size="xs" variant="light" color="gray">Soon</Badge>}
            />
          ),
        )}
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>

      <AppShell.Footer className={classes.tabBar} aria-label="Tabs">
        {NAV_ITEMS.filter((item) => item.bottomBar).map((item) =>
          item.enabled ? (
            <Link
              key={item.key}
              to={item.path}
              className={classes.tab}
              data-active={isActive(item, pathname) || undefined}
              aria-current={isActive(item, pathname) ? 'page' : undefined}
            >
              <item.icon size={22} stroke={1.6} />
              <span>{item.label}</span>
            </Link>
          ) : (
            <span key={item.key} className={classes.tab} data-disabled title="Coming soon" aria-disabled="true">
              <item.icon size={22} stroke={1.6} />
              <span>{item.label}</span>
            </span>
          ),
        )}
      </AppShell.Footer>
    </AppShell>
  )
}
