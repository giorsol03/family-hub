import { ActionIcon, Alert, Badge, Button, Group, Loader, Paper, Stack, Text, Title, Tooltip } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconPencil, IconUserCheck, IconUserMinus, IconUserPlus } from '@tabler/icons-react'
import { Link } from 'react-router'
import type { Member } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import { useCurrentMember } from '../auth/useCurrentMember'
import { useMembers, useSetMemberActive } from './useMembers'

export function MembersPage() {
  const { data: members, isPending, isError } = useMembers()
  const { data: me } = useCurrentMember()
  const setActive = useSetMemberActive()

  function toggleActive(member: Member) {
    setActive.mutate(
      { id: member.id, active: !member.active },
      {
        onSuccess: (m) =>
          notifications.show({ color: 'teal', message: `${m.name} ${m.active ? 're-activated' : 'deactivated'}` }),
        onError: (e) => notifications.show({ color: 'red', message: e.message }),
      },
    )
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Members</Title>
        <Button component={Link} to="/members/new" leftSection={<IconUserPlus size={18} />}>Add</Button>
      </Group>

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load members.</Alert>}

      {members?.map((member) => (
        <Paper key={member.id} withBorder p="sm" opacity={member.active ? 1 : 0.6}>
          <Group wrap="nowrap">
            <MemberAvatar member={member} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <Group gap={6}>
                <Text fw={600} truncate>{member.name}</Text>
                {me?.id === member.id && <Badge size="xs" variant="outline">You</Badge>}
                {!member.active && <Badge size="xs" color="gray">Inactive</Badge>}
              </Group>
              <Text size="sm" c="dimmed">@{member.username}</Text>
            </div>
            <Group gap={4} wrap="nowrap">
              <Tooltip label="Edit">
                <ActionIcon component={Link} to={`/members/${member.id}`} variant="subtle" color="gray" size="lg"
                            aria-label={`Edit ${member.name}`}>
                  <IconPencil size={18} />
                </ActionIcon>
              </Tooltip>
              {member.active ? (
                me?.id !== member.id && (
                  <Tooltip label="Deactivate">
                    <ActionIcon variant="subtle" color="red" size="lg" onClick={() => toggleActive(member)}
                                aria-label={`Deactivate ${member.name}`}>
                      <IconUserMinus size={18} />
                    </ActionIcon>
                  </Tooltip>
                )
              ) : (
                <Tooltip label="Re-activate">
                  <ActionIcon variant="subtle" color="teal" size="lg" onClick={() => toggleActive(member)}
                              aria-label={`Re-activate ${member.name}`}>
                    <IconUserCheck size={18} />
                  </ActionIcon>
                </Tooltip>
              )}
            </Group>
          </Group>
        </Paper>
      ))}
    </Stack>
  )
}
