import { Alert, Loader, Stack, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useNavigate, useParams } from 'react-router'
import type { MemberInput } from '../../api/types'
import { MemberForm } from './MemberForm'
import { useMember, useSaveMember } from './useMembers'

export function MemberFormPage() {
  const params = useParams()
  const id = params.id !== undefined ? Number(params.id) : undefined
  const navigate = useNavigate()
  const { data: member, isPending, isError } = useMember(id)
  const save = useSaveMember(id)

  async function handleSubmit(values: MemberInput) {
    const saved = await save.mutateAsync(values)
    notifications.show({ color: 'teal', message: `${saved.name} ${id === undefined ? 'added' : 'updated'}` })
    navigate('/members')
  }

  return (
    <Stack>
      <Title order={3}>{id === undefined ? 'Add member' : 'Edit member'}</Title>
      {id !== undefined && isPending && <Loader />}
      {id !== undefined && isError && <Alert color="red">Member not found.</Alert>}
      {(id === undefined || member) && (
        <MemberForm member={member} onSubmit={handleSubmit} onCancel={() => navigate('/members')} />
      )}
    </Stack>
  )
}
