import { Box, Group, Text, UnstyledButton } from '@mantine/core'
import dayjs from 'dayjs'
import type { CalendarItem, IsoDate } from '../../api/types'
import classes from './MonthView.module.css'
import { WEEKDAY_LABELS, itemColor, itemsOnDay, monthGrid } from './calendarUtils'

interface Props {
  month: IsoDate
  today: IsoDate
  selected: IsoDate | null
  items: CalendarItem[]
  onDayClick: (date: IsoDate) => void
}

const MAX_LABELS = 3

/** Month grid. Phones show coloured dots per item; wider screens show short labels. */
export function MonthView({ month, today, selected, items, onDayClick }: Props) {
  const currentMonth = dayjs(month).month()
  return (
    <Box className={classes.grid} role="grid" aria-label={dayjs(month).format('MMMM YYYY')}>
      {WEEKDAY_LABELS.map((d) => <Text key={d} className={classes.weekday} size="xs" c="dimmed" fw={600}>{d}</Text>)}
      {monthGrid(month).flat().map((date) => {
        const dayItems = itemsOnDay(items, date)
        const outside = dayjs(date).month() !== currentMonth
        return (
          <UnstyledButton
            key={date}
            className={classes.day}
            data-outside={outside || undefined}
            data-today={date === today || undefined}
            data-selected={date === selected || undefined}
            onClick={() => onDayClick(date)}
            aria-label={`${dayjs(date).format('dddd D MMMM')}${dayItems.length ? `, ${dayItems.length} item${dayItems.length > 1 ? 's' : ''}` : ''}`}
          >
            <Text size="sm" fw={date === today ? 700 : 400} className={classes.number}>{dayjs(date).date()}</Text>
            <Group gap={3} hiddenFrom="sm" mt={2}>
              {dayItems.slice(0, 4).map((item) => (
                <span key={`${item.kind}-${item.id}`} className={classes.dot}
                      style={{ background: `var(--mantine-color-${itemColor(item)}-filled)` }} />
              ))}
            </Group>
            <Box visibleFrom="sm">
              {dayItems.slice(0, MAX_LABELS).map((item) => (
                <Text key={`${item.kind}-${item.id}`} size="xs" truncate className={classes.label}
                      style={{ background: `var(--mantine-color-${itemColor(item)}-light)`,
                               color: `var(--mantine-color-${itemColor(item)}-light-color)` }}>
                  {item.title}
                </Text>
              ))}
              {dayItems.length > MAX_LABELS && <Text size="xs" c="dimmed">+{dayItems.length - MAX_LABELS} more</Text>}
            </Box>
          </UnstyledButton>
        )
      })}
    </Box>
  )
}
