import {
  Alert, Button, Group, Modal, MultiSelect, SegmentedControl, Stack, Switch, TextInput, Textarea,
} from '@mantine/core'
import { DatePickerInput, TimeInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconTrash } from '@tabler/icons-react'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { EVENT_TYPES } from '../../api/types'
import type { CalendarItem, EventType, IsoDate } from '../../api/types'
import { useMembers } from '../members/useMembers'
import { EVENT_TYPE_META } from './calendarUtils'
import { useDeleteEvent, useSaveEvent } from './useCalendar'

interface Props {
  opened: boolean
  onClose: () => void
  /** Event being edited; undefined = new event on {@code defaultDate}. */
  event?: CalendarItem
  defaultDate: IsoDate
}

interface Values {
  title: string
  type: EventType
  allDay: boolean
  startDate: IsoDate | null
  startTime: string
  endDate: IsoDate | null
  endTime: string
  location: string
  attendeeIds: string[]
  description: string
}

export function EventFormModal({ opened, onClose, event, defaultDate }: Props) {
  const isPhone = useMediaQuery('(max-width: 48em)')
  return (
    <Modal opened={opened} onClose={onClose} title={event ? 'Edit event' : 'New event'} fullScreen={isPhone}>
      {opened && <EventForm key={event?.id ?? defaultDate} event={event} defaultDate={defaultDate} onDone={onClose} />}
    </Modal>
  )
}

function EventForm({ event, defaultDate, onDone }: { event?: CalendarItem; defaultDate: IsoDate; onDone: () => void }) {
  const save = useSaveEvent(event?.id)
  const remove = useDeleteEvent()
  const { data: members = [] } = useMembers()
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<Values>({
    mode: 'controlled',
    initialValues: {
      title: event?.title ?? '',
      type: event?.type ?? 'EVENT',
      allDay: event?.allDay ?? false,
      startDate: event?.start.slice(0, 10) ?? defaultDate,
      startTime: event && !event.allDay ? event.start.slice(11, 16) : '09:00',
      endDate: event?.end.slice(0, 10) ?? defaultDate,
      endTime: event && !event.allDay ? event.end.slice(11, 16) : '10:00',
      location: event?.location ?? '',
      attendeeIds: event?.attendees.map((a) => String(a.id)) ?? [],
      description: event?.description ?? '',
    },
    validate: {
      title: (v) => (!v.trim() ? 'Title is required' : v.length > 150 ? 'Max 150 characters' : null),
      startDate: (v) => (v ? null : 'Pick a day'),
      endDate: (v, values) => {
        if (!v) return 'Pick a day'
        if (!values.startDate) return null
        const start = values.allDay ? values.startDate : `${values.startDate}T${values.startTime}`
        const end = values.allDay ? v : `${v}T${values.endTime}`
        return end < start ? 'Ends before it starts' : null
      },
      startTime: (v, values) => (values.allDay || /^\d{2}:\d{2}$/.test(v) ? null : 'Time?'),
      endTime: (v, values) => (values.allDay || /^\d{2}:\d{2}$/.test(v) ? null : 'Time?'),
    },
  })

  async function handleSubmit(values: Values) {
    setFormError(null)
    const time = (t: string) => (values.allDay ? '00:00:00' : `${t}:00`)
    try {
      const saved = await save.mutateAsync({
        title: values.title.trim(),
        type: values.type,
        allDay: values.allDay,
        start: `${values.startDate}T${time(values.startTime)}`,
        end: `${values.endDate}T${time(values.endTime)}`,
        location: values.location.trim() || null,
        description: values.description.trim() || null,
        attendeeIds: values.attendeeIds.map(Number),
      })
      notifications.show({ color: 'teal', message: `${saved.title} ${event ? 'updated' : 'added'}` })
      onDone()
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) {
        const { end, start, ...rest } = e.fieldErrors
        form.setErrors({ ...rest, ...(end && { endDate: end }), ...(start && { startDate: start }) })
      } else {
        setFormError(e instanceof Error ? e.message : 'Something went wrong')
      }
    }
  }

  async function handleDelete() {
    if (!event) return
    await remove.mutateAsync(event.id)
    notifications.show({ color: 'teal', message: `${event.title} deleted` })
    onDone()
  }

  // Keep the end date from falling before a newly picked start date.
  const startDateProps = form.getInputProps('startDate')
  function onStartDateChange(value: IsoDate | null) {
    startDateProps.onChange(value)
    if (value && form.values.endDate && form.values.endDate < value) form.setFieldValue('endDate', value)
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Title" required data-autofocus {...form.getInputProps('title')} />
        <SegmentedControl fullWidth size="xs" aria-label="Type" data={EVENT_TYPES.map((t) => ({ value: t, label: EVENT_TYPE_META[t].label }))}
                          {...form.getInputProps('type')} />
        <Switch label="All day" {...form.getInputProps('allDay', { type: 'checkbox' })} />
        <Group grow align="flex-start">
          <DatePickerInput label="Starts" valueFormat="ddd D MMM YYYY" firstDayOfWeek={1}
                           {...startDateProps} onChange={onStartDateChange} />
          {!form.values.allDay && <TimeInput label="Time" aria-label="Start time" {...form.getInputProps('startTime')} />}
        </Group>
        <Group grow align="flex-start">
          <DatePickerInput label="Ends" valueFormat="ddd D MMM YYYY" firstDayOfWeek={1} {...form.getInputProps('endDate')} />
          {!form.values.allDay && <TimeInput label="Time" aria-label="End time" {...form.getInputProps('endTime')} />}
        </Group>
        <TextInput label="Location" {...form.getInputProps('location')} />
        <MultiSelect label="Who's going" placeholder="Everyone (household)" clearable
                     data={members.filter((m) => m.active || form.values.attendeeIds.includes(String(m.id)))
                       .map((m) => ({ value: String(m.id), label: m.name }))}
                     {...form.getInputProps('attendeeIds')} />
        <Textarea label="Notes" autosize minRows={1} maxRows={4} {...form.getInputProps('description')} />
        <Group justify="space-between">
          <Group>
            <Button type="submit" loading={form.submitting}>Save</Button>
            <Button variant="default" onClick={onDone}>Cancel</Button>
          </Group>
          {event && (
            <Button variant="subtle" color="red" leftSection={<IconTrash size={16} />} loading={remove.isPending}
                    onClick={handleDelete}>Delete</Button>
          )}
        </Group>
      </Stack>
    </form>
  )
}
