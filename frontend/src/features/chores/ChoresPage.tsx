import { ActionIcon, Alert, Badge, Button, Group, Loader, Modal, Paper, SegmentedControl, Stack, Text, Title, Tooltip } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconChecklist, IconPlus, IconTemplate } from '@tabler/icons-react'
import dayjs from 'dayjs'
import { Fragment, useState } from 'react'
import { Link } from 'react-router'
import type { Chore, ChoreView } from '../../api/types'
import { MemberSelect } from '../../components/MemberSelect'
import { AddChoreModal } from './AddChoreModal'
import { ChoreForm } from './ChoreForm'
import { ChoreRow } from './ChoreRow'
import { formatDay, groupByDay, sortChores } from './dates'
import { useChoreAction, useChoreList, useDeleteChore, useSaveChore } from './useChores'

export function ChoresPage() {
  const [view, setView] = useState<ChoreView>('TODAY')
  const [assigneeId, setAssigneeId] = useState<number | null>(null)
  const { data, isPending, isError } = useChoreList(view, assigneeId)
  const { data: overdue } = useChoreList('OVERDUE', assigneeId)
  const action = useChoreAction()
  const remove = useDeleteChore()
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState<Chore | null>(null)
  const save = useSaveChore(editing?.id)
  const isPhone = useMediaQuery('(max-width: 48em)')

  // The server's "today" (household timezone) wins; the browser's date is only a fallback while loading.
  const [browserToday] = useState(() => dayjs().format('YYYY-MM-DD'))
  const today = data?.today ?? overdue?.today ?? browserToday
  const overdueCount = overdue?.chores.length ?? 0

  function toggle(chore: Chore) {
    const act = chore.status === 'TODO' ? 'complete' : 'reopen'
    action.mutate({ id: chore.id, action: act }, {
      onSuccess: (result) => {
        if (act === 'complete') {
          notifications.show({
            color: 'teal',
            message: result.next
              ? `${chore.title} done ✓ Next: ${formatDay(result.next.scheduledDate, today).toLowerCase()}`
              : `${chore.title} done ✓`,
          })
        }
      },
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  function skip(chore: Chore) {
    action.mutate({ id: chore.id, action: 'skip' }, {
      onSuccess: (result) => notifications.show({
        message: result.next
          ? `${chore.title} skipped. Next: ${formatDay(result.next.scheduledDate, today).toLowerCase()}`
          : `${chore.title} skipped`,
      }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  function del(chore: Chore) {
    remove.mutate(chore.id, {
      onSuccess: () => notifications.show({ color: 'teal', message: `${chore.title} deleted` }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  const rowProps = { today, onToggle: toggle, onSkip: skip, onEdit: setEditing, onDelete: del }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Chores</Title>
        <Group gap="xs">
          <Tooltip label="Templates">
            <ActionIcon component={Link} to="/chores/templates" variant="default" size="lg" aria-label="Templates">
              <IconTemplate size={18} />
            </ActionIcon>
          </Tooltip>
          <Button leftSection={<IconPlus size={18} />} onClick={() => setAdding(true)}>Add</Button>
        </Group>
      </Group>

      <SegmentedControl
        fullWidth
        value={view}
        onChange={(v) => setView(v as ChoreView)}
        data={[
          { value: 'TODAY', label: 'Today' },
          { value: 'WEEK', label: 'This week' },
          {
            value: 'OVERDUE',
            label: (
              <Group gap={6} justify="center" wrap="nowrap">
                Overdue
                {overdueCount > 0 && <Badge size="xs" color="red" circle>{overdueCount}</Badge>}
              </Group>
            ),
          },
        ]}
      />
      <MemberSelect aria-label="Filter by person" emptyLabel="Everyone" value={assigneeId} onChange={setAssigneeId} />

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load chores.</Alert>}

      {data && data.chores.length === 0 && (
        <Paper withBorder p="xl">
          <Stack align="center" gap={4}>
            <IconChecklist size={40} stroke={1.4} color="var(--mantine-color-dimmed)" />
            <Text fw={500}>{view === 'OVERDUE' ? 'Nothing overdue 🎉' : 'No chores'}</Text>
            {view !== 'OVERDUE' && <Text size="sm" c="dimmed">Tap “Add” to plan one.</Text>}
          </Stack>
        </Paper>
      )}

      {data && data.chores.length > 0 && view !== 'WEEK' && (
        <Paper withBorder>
          {sortChores(data.chores).map((chore, i) => (
            <Fragment key={chore.id}>
              {i > 0 && <div style={{ borderTop: '1px solid var(--mantine-color-default-border)' }} />}
              <ChoreRow chore={chore} showDate={view === 'OVERDUE'} {...rowProps} />
            </Fragment>
          ))}
        </Paper>
      )}

      {data && data.chores.length > 0 && view === 'WEEK' && data.from && (
        groupByDay(data.chores, data.from, data.to).map((day) => (
          <Stack key={day.date} gap={4}>
            <Text size="sm" fw={700} c={day.date === today ? undefined : 'dimmed'}>
              {formatDay(day.date, today)}
            </Text>
            {day.chores.length === 0
              ? <Text size="sm" c="dimmed" pl="sm">—</Text>
              : (
                <Paper withBorder>
                  {day.chores.map((chore, i) => (
                    <Fragment key={chore.id}>
                      {i > 0 && <div style={{ borderTop: '1px solid var(--mantine-color-default-border)' }} />}
                      <ChoreRow chore={chore} {...rowProps} />
                    </Fragment>
                  ))}
                </Paper>
              )}
          </Stack>
        ))
      )}

      <AddChoreModal opened={adding} onClose={() => setAdding(false)} today={today} />

      <Modal opened={editing !== null} onClose={() => setEditing(null)} title="Edit chore" fullScreen={isPhone}>
        {editing && (
          <ChoreForm
            key={editing.id}
            chore={editing}
            defaultDate={today}
            onSubmit={async (input) => {
              await save.mutateAsync(input)
              notifications.show({ color: 'teal', message: `${input.title} saved` })
              setEditing(null)
            }}
            onCancel={() => setEditing(null)}
          />
        )}
      </Modal>
    </Stack>
  )
}
