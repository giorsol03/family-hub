import { Alert, Button, Group, Modal, NumberInput, Stack, TextInput } from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { IconTrash } from '@tabler/icons-react'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import type { Goal, IsoDate } from '../../api/types'
import { MemberSelect } from '../../components/MemberSelect'
import { useSaveGoal, useSetGoalActive } from './useGoals'

interface Props {
  opened: boolean
  onClose: () => void
  goal?: Goal
  /** Owner preselected for new goals (the person currently filtered, or me). */
  defaultOwnerId: number | null
  today: IsoDate
}

export function GoalFormModal({ opened, onClose, goal, defaultOwnerId, today }: Props) {
  return (
    <Modal opened={opened} onClose={onClose} title={goal ? 'Edit goal' : 'New goal'} centered>
      {opened && <GoalForm key={goal?.id ?? 'new'} goal={goal} defaultOwnerId={defaultOwnerId} today={today} onDone={onClose} />}
    </Modal>
  )
}

interface Values {
  name: string
  ownerId: number | null
  startDate: IsoDate | null
  targetDaysPerWeek: number | string
}

function GoalForm({ goal, defaultOwnerId, today, onDone }: { goal?: Goal; defaultOwnerId: number | null; today: IsoDate; onDone: () => void }) {
  const save = useSaveGoal(goal?.id)
  const setActive = useSetGoalActive()
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<Values>({
    mode: 'controlled',
    initialValues: {
      name: goal?.name ?? '',
      ownerId: goal?.owner.id ?? defaultOwnerId,
      startDate: goal?.startDate ?? today,
      targetDaysPerWeek: goal?.targetDaysPerWeek ?? '',
    },
    validate: {
      name: (v) => (!v.trim() ? 'Name your goal' : v.length > 100 ? 'Max 100 characters' : null),
      ownerId: (v) => (v == null ? 'Whose goal is it?' : null),
      targetDaysPerWeek: (v) => (v === '' || (Number(v) >= 1 && Number(v) <= 7) ? null : '1–7 days'),
    },
  })

  async function handleSubmit(values: Values) {
    setFormError(null)
    try {
      const saved = await save.mutateAsync({
        name: values.name.trim(),
        ownerId: values.ownerId!,
        startDate: values.startDate,
        targetDaysPerWeek: values.targetDaysPerWeek === '' ? null : Number(values.targetDaysPerWeek),
      })
      notifications.show({ color: 'teal', message: `${saved.name} saved` })
      onDone()
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) form.setErrors(e.fieldErrors)
      else setFormError(e instanceof Error ? e.message : 'Something went wrong')
    }
  }

  async function toggleActive() {
    if (!goal) return
    await setActive.mutateAsync({ id: goal.id, active: !goal.active })
    notifications.show({ color: 'teal', message: goal.active ? `${goal.name} removed (history kept)` : `${goal.name} restored` })
    onDone()
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Goal" placeholder="e.g. Yoga, Eat healthy, No caffeine" required data-autofocus
                   {...form.getInputProps('name')} />
        <MemberSelect label="Whose goal" required {...form.getInputProps('ownerId')} />
        <Group grow align="flex-start">
          <DatePickerInput label="Started" valueFormat="D MMM YYYY" firstDayOfWeek={1} maxDate={today}
                           {...form.getInputProps('startDate')} />
          <NumberInput label="Target days / week" placeholder="Optional" min={1} max={7} allowDecimal={false}
                       {...form.getInputProps('targetDaysPerWeek')} />
        </Group>
        <Group justify="space-between">
          <Group>
            <Button type="submit" loading={form.submitting}>Save</Button>
            <Button variant="default" onClick={onDone}>Cancel</Button>
          </Group>
          {goal && (
            <Button variant="subtle" color={goal.active ? 'red' : 'teal'} loading={setActive.isPending}
                    leftSection={goal.active ? <IconTrash size={16} /> : undefined} onClick={toggleActive}>
              {goal.active ? 'Remove' : 'Restore'}
            </Button>
          )}
        </Group>
      </Stack>
    </form>
  )
}
