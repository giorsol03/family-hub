import { ActionIcon, Alert, Button, Group, Loader, SegmentedControl, Stack, Text, Title } from '@mantine/core'
import { IconChevronLeft, IconChevronRight, IconPlus } from '@tabler/icons-react'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import type { CalendarItem, IsoDate } from '../../api/types'
import { MemberSelect } from '../../components/MemberSelect'
import { formatWeekRange } from '../mealplan/week'
import { DayAgenda } from './DayAgenda'
import { EventFormModal } from './EventFormModal'
import { MonthView } from './MonthView'
import { formatMonth, itemsOnDay, monthGrid, shift, weekOf } from './calendarUtils'
import { useCalendarRange } from './useCalendar'

type Mode = 'month' | 'week'
type Editor = { open: false } | { open: true; event?: CalendarItem; date: IsoDate }

export function CalendarPage() {
  const [mode, setMode] = useState<Mode>('month')
  // Browser date only positions the first view; the server's "today" is used for highlighting.
  const [anchor, setAnchor] = useState<IsoDate>(() => dayjs().format('YYYY-MM-DD'))
  const [selected, setSelected] = useState<IsoDate | null>(null)
  const [memberId, setMemberId] = useState<number | null>(null)
  const [editor, setEditor] = useState<Editor>({ open: false })
  const navigate = useNavigate()

  const days = mode === 'month' ? monthGrid(anchor).flat() : weekOf(anchor)
  const from = days[0]
  const to = days[days.length - 1]
  const { data, isPending, isError } = useCalendarRange(from, to, memberId)
  const items = data?.items ?? []
  const today = data?.today ?? anchor

  function openItem(item: CalendarItem) {
    if (item.kind === 'MEAL_PLAN') navigate('/meal-plan') // meals are edited in the planner
    else setEditor({ open: true, event: item, date: item.start.slice(0, 10) })
  }

  /** "Click a day to add an event": an empty day opens the form; a busy day shows its agenda first. */
  function onDayClick(date: IsoDate) {
    setSelected(date)
    if (itemsOnDay(items, date).length === 0) setEditor({ open: true, date })
  }

  const step = (n: number) => {
    setAnchor(shift(anchor, mode, n))
    setSelected(null)
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={3}>Calendar</Title>
        <Button leftSection={<IconPlus size={18} />} onClick={() => setEditor({ open: true, date: selected ?? today })}>
          Add
        </Button>
      </Group>

      <Group grow>
        <SegmentedControl value={mode} onChange={(v) => { setMode(v as Mode); setSelected(null) }}
                          data={[{ value: 'month', label: 'Month' }, { value: 'week', label: 'Week' }]} />
        <MemberSelect aria-label="Filter by person" emptyLabel="Everyone" value={memberId} onChange={setMemberId} />
      </Group>

      <Group justify="space-between" wrap="nowrap">
        <ActionIcon variant="default" size="lg" onClick={() => step(-1)} aria-label={`Previous ${mode}`}>
          <IconChevronLeft size={18} />
        </ActionIcon>
        <Stack gap={0} align="center">
          <Text fw={600}>{mode === 'month' ? formatMonth(anchor) : formatWeekRange(from, to)}</Text>
          {!days.includes(today) && (
            <Button variant="subtle" size="compact-xs" onClick={() => { setAnchor(today); setSelected(null) }}>
              Today
            </Button>
          )}
        </Stack>
        <ActionIcon variant="default" size="lg" onClick={() => step(1)} aria-label={`Next ${mode}`}>
          <IconChevronRight size={18} />
        </ActionIcon>
      </Group>

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load the calendar.</Alert>}

      {mode === 'month' && (
        <>
          <MonthView month={anchor} today={today} selected={selected} items={items} onDayClick={onDayClick} />
          {selected && (
            <DayAgenda date={selected} today={today} items={itemsOnDay(items, selected)}
                       onAdd={(date) => setEditor({ open: true, date })} onOpen={openItem} />
          )}
        </>
      )}

      {mode === 'week' && days.map((date) => (
        <DayAgenda key={date} compact date={date} today={today} items={itemsOnDay(items, date)}
                   onAdd={(d) => setEditor({ open: true, date: d })} onOpen={openItem} />
      ))}

      <EventFormModal
        opened={editor.open}
        event={editor.open ? editor.event : undefined}
        defaultDate={editor.open ? editor.date : today}
        onClose={() => setEditor({ open: false })}
      />
    </Stack>
  )
}
