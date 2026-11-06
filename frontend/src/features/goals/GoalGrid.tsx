import { Group, ScrollArea, Text, Tooltip, UnstyledButton } from '@mantine/core'
import { IconFlame } from '@tabler/icons-react'
import dayjs from 'dayjs'
import { Fragment, useEffect, useRef } from 'react'
import type { Goal, GoalRow, IsoDate, YearMonth } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import classes from './GoalGrid.module.css'
import { cellState, groupByOwner, monthDates } from './gridUtils'

interface Props {
  month: YearMonth
  today: IsoDate
  rows: GoalRow[]
  /** "Everyone" view: show a header row per person. */
  groupByPerson: boolean
  onToggle: (row: GoalRow, date: IsoDate, done: boolean) => void
  onEditGoal: (goal: Goal) => void
}

const CELL = 30 // px, keep in sync with GoalGrid.module.css

/** Paper-style habit tracker: one row per goal, one column per day. */
export function GoalGrid({ month, today, rows, groupByPerson, onToggle, onEditGoal }: Props) {
  const dates = monthDates(month)
  const viewport = useRef<HTMLDivElement>(null)

  // On phones the grid is wider than the screen: start scrolled so today is in view.
  useEffect(() => {
    const index = monthDates(month).indexOf(today)
    if (viewport.current && index > 0) viewport.current.scrollLeft = Math.max(0, (index - 4) * CELL)
  }, [month, today])

  const renderRow = (row: GoalRow) => (
    <tr key={row.goal.id}>
      <th scope="row" className={classes.goalCell}>
        <UnstyledButton onClick={() => onEditGoal(row.goal)} aria-label={`Edit ${row.goal.name}`}>
          <Text size="sm" fw={600} truncate maw={150} c={row.goal.active ? undefined : 'dimmed'}>{row.goal.name}</Text>
        </UnstyledButton>
        <Group gap={8} wrap="nowrap">
          <Tooltip label="Current streak">
            <Group gap={2} wrap="nowrap" c={row.stats.streak > 0 ? 'orange' : 'dimmed'} aria-label={`Streak ${row.stats.streak}`}>
              <IconFlame size={13} />
              <Text size="xs" fw={600}>{row.stats.streak}</Text>
            </Group>
          </Tooltip>
          {row.stats.percentThisMonth != null && (
            <Tooltip label={`${row.stats.doneThisMonth} of ${row.stats.eligibleDays} days this month`}>
              <Text size="xs" c="dimmed">{row.stats.percentThisMonth}%</Text>
            </Tooltip>
          )}
          {row.goal.targetDaysPerWeek != null && (
            <Tooltip label="This week / weekly target">
              <Text size="xs" c={row.stats.doneThisWeek >= row.goal.targetDaysPerWeek ? 'teal' : 'dimmed'}>
                {row.stats.doneThisWeek}/{row.goal.targetDaysPerWeek} wk
              </Text>
            </Tooltip>
          )}
        </Group>
      </th>
      {dates.map((date) => {
        const state = cellState(row, date, today)
        const clickable = state === 'done' || state === 'open'
        return (
          <td key={date} className={classes.cellTd} data-today={date === today || undefined}>
            <UnstyledButton
              className={classes.cell}
              data-state={state}
              disabled={!clickable}
              style={state === 'done' ? { background: row.goal.owner.color, borderColor: row.goal.owner.color } : undefined}
              onClick={() => onToggle(row, date, state !== 'done')}
              aria-label={`${row.goal.name}, ${dayjs(date).format('D MMMM')}`}
              aria-pressed={state === 'done'}
            />
          </td>
        )
      })}
    </tr>
  )

  return (
    <ScrollArea viewportRef={viewport} type="auto" offsetScrollbars>
      <table className={classes.table}>
        <thead>
          <tr>
            <th className={classes.goalCell} />
            {dates.map((date) => (
              <th key={date} className={classes.dayHead} data-today={date === today || undefined} scope="col">
                <Text size="xs" c="dimmed" lh={1}>{dayjs(date).format('dd').charAt(0)}</Text>
                <Text size="xs" fw={date === today ? 700 : 500} lh={1.4}>{dayjs(date).date()}</Text>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {groupByPerson
            ? groupByOwner(rows).map((group) => (
              <Fragment key={group.owner.id}>
                <tr>
                  <th colSpan={1} className={`${classes.goalCell} ${classes.ownerHead}`} scope="rowgroup">
                    <Group gap={6} wrap="nowrap">
                      <MemberAvatar member={group.owner} size={20} />
                      <Text size="sm" fw={700}>{group.owner.name}</Text>
                    </Group>
                  </th>
                  <td colSpan={dates.length} className={classes.ownerHead} />
                </tr>
                {group.rows.map(renderRow)}
              </Fragment>
            ))
            : rows.map(renderRow)}
        </tbody>
      </table>
    </ScrollArea>
  )
}
