import { Alert, Button, Group, Stack, TextInput, Textarea } from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import type { Chore, ChoreInput, IsoDate } from '../../api/types'
import { MemberSelect } from '../../components/MemberSelect'

interface Props {
  chore?: Chore
  defaultDate: IsoDate
  onSubmit: (input: ChoreInput) => Promise<unknown>
  onCancel: () => void
}

interface Values {
  title: string
  description: string
  scheduledDate: IsoDate | null
  assigneeId: number | null
}

/** A one-off chore (or editing any chore). */
export function ChoreForm({ chore, defaultDate, onSubmit, onCancel }: Props) {
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<Values>({
    mode: 'uncontrolled',
    initialValues: {
      title: chore?.title ?? '',
      description: chore?.description ?? '',
      scheduledDate: chore?.scheduledDate ?? defaultDate,
      assigneeId: chore?.assignee?.id ?? null,
    },
    validate: {
      title: (v) => (!v.trim() ? 'Title is required' : v.length > 100 ? 'Max 100 characters' : null),
      scheduledDate: (v) => (v ? null : 'Pick a day'),
      description: (v) => (v.length > 1000 ? 'Max 1000 characters' : null),
    },
  })

  async function handleSubmit(values: Values) {
    setFormError(null)
    try {
      await onSubmit({
        title: values.title.trim(),
        description: values.description.trim() || null,
        scheduledDate: values.scheduledDate!,
        assigneeId: values.assigneeId,
      })
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) form.setErrors(e.fieldErrors)
      else setFormError(e instanceof Error ? e.message : 'Something went wrong')
    }
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Chore" required data-autofocus key={form.key('title')} {...form.getInputProps('title')} />
        <DatePickerInput label="Day" required valueFormat="ddd D MMM YYYY" firstDayOfWeek={1}
                         key={form.key('scheduledDate')} {...form.getInputProps('scheduledDate')} />
        <MemberSelect label="Who" emptyLabel="Anyone" key={form.key('assigneeId')}
                      {...form.getInputProps('assigneeId')} error={form.errors.assigneeId} />
        <Textarea label="Notes" autosize minRows={1} maxRows={4} key={form.key('description')}
                  {...form.getInputProps('description')} />
        <Group>
          <Button type="submit" loading={form.submitting}>{chore ? 'Save' : 'Add'}</Button>
          <Button variant="default" onClick={onCancel}>Cancel</Button>
        </Group>
      </Stack>
    </form>
  )
}
