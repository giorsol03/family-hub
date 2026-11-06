import { ActionIcon, Alert, Button, Group, Loader, Paper, Stack, Switch, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconChevronLeft, IconChevronRight, IconPlus, IconTargetArrow } from '@tabler/icons-react'
import dayjs from 'dayjs'
import { useState } from 'react'
import type { Goal, GoalRow, IsoDate, YearMonth } from '../../api/types'
import { MemberSelect } from '../../components/MemberSelect'
import { useCurrentMember } from '../auth/useCurrentMember'
import { GoalFormModal } from './GoalFormModal'
import { GoalGrid } from './GoalGrid'
import { formatMonthLabel, shiftMonth } from './gridUtils'
import { useGoalList, useGoalMonth, useSetCheck, useSetGoalActive } from './useGoals'

type Editor = { open: false } | { open: true; goal?: Goal }

export function GoalsPage() {
  const [month, setMonth] = useState<YearMonth | null>(null) // null = this month (server decides)
  const [ownerId, setOwnerId] = useState<number | null>(null)
  const [showRemoved, setShowRemoved] = useState(false)
  const [editor, setEditor] = useState<Editor>({ open: false })
  const { data, isPending, isError } = useGoalMonth(month, ownerId)
  const { data: allGoals = [] } = useGoalList(true)
  const { data: me } = useCurrentMember()
  const setCheck = useSetCheck()
  const setActive = useSetGoalActive()
  const [browserToday] = useState(() => dayjs().format('YYYY-MM-DD')) // fallback until the server answers

  const shownMonth = data?.month ?? month
  const isThisMonth = data ? data.today.startsWith(data.month) : true
  const removed = allGoals.filter((g) => !g.active && (ownerId == null || g.owner.id === ownerId))

  function toggle(row: GoalRow, date: IsoDate, done: boolean) {
    setCheck.mutate({ goalId: row.goal.id, date, done }, {
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  function restore(goal: Goal) {
    setActive.mutate({ id: goal.id, active: true }, {
      onSuccess: () => notifications.show({ color: 'teal', message: `${goal.name} restored` }),
    })
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Daily goals</Title>
        <Button leftSection={<IconPlus size={18} />} onClick={() => setEditor({ open: true })}>Add goal</Button>
      </Group>

      <MemberSelect aria-label="Filter by person" emptyLabel="Everyone" value={ownerId} onChange={setOwnerId} />

      {shownMonth && (
        <Group justify="space-between" wrap="nowrap">
          <ActionIcon variant="default" size="lg" aria-label="Previous month"
                      onClick={() => setMonth(shiftMonth(shownMonth, -1))}>
            <IconChevronLeft size={18} />
          </ActionIcon>
          <Stack gap={0} align="center">
            <Text fw={600}>{formatMonthLabel(shownMonth)}</Text>
            {!isThisMonth && <Button variant="subtle" size="compact-xs" onClick={() => setMonth(null)}>This month</Button>}
          </Stack>
          <ActionIcon variant="default" size="lg" aria-label="Next month"
                      onClick={() => setMonth(shiftMonth(shownMonth, 1))}>
            <IconChevronRight size={18} />
          </ActionIcon>
        </Group>
      )}

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load goals.</Alert>}

      {data && data.rows.length === 0 && (
        <Paper withBorder p="xl">
          <Stack align="center" gap={4}>
            <IconTargetArrow size={40} stroke={1.4} color="var(--mantine-color-dimmed)" />
            <Text fw={500}>No goals {ownerId != null ? 'for this person ' : ''}yet</Text>
            <Text size="sm" c="dimmed">Add one — Yoga, Gym, Eat healthy, No caffeine…</Text>
          </Stack>
        </Paper>
      )}

      {data && data.rows.length > 0 && (
        <Paper withBorder p="xs">
          <GoalGrid month={data.month} today={data.today} rows={data.rows} groupByPerson={ownerId == null}
                    onToggle={toggle} onEditGoal={(goal) => setEditor({ open: true, goal })} />
        </Paper>
      )}

      {removed.length > 0 && (
        <Switch label={`Show removed goals (${removed.length})`} checked={showRemoved}
                onChange={(e) => setShowRemoved(e.currentTarget.checked)} />
      )}
      {showRemoved && removed.map((goal) => (
        <Group key={goal.id} justify="space-between">
          <Text size="sm" c="dimmed">{goal.name} · {goal.owner.name}</Text>
          <Button size="compact-xs" variant="light" onClick={() => restore(goal)}>Restore</Button>
        </Group>
      ))}

      <GoalFormModal
        opened={editor.open}
        goal={editor.open ? editor.goal : undefined}
        defaultOwnerId={ownerId ?? me?.id ?? null}
        today={data?.today ?? browserToday}
        onClose={() => setEditor({ open: false })}
      />
    </Stack>
  )
}
