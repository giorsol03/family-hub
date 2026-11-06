import {
  ActionIcon, Alert, Anchor, Badge, Button, Group, Loader, Modal, NumberInput, Paper, Select, Stack, Switch, Text,
  TextInput, Textarea, Title, Tooltip,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconArrowLeft, IconCalendarPlus, IconEye, IconEyeOff, IconPencil, IconPlus } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../../api/client'
import type { ChoreTemplate, ChoreTemplateInput, Recurrence } from '../../api/types'
import { MemberAvatar } from '../../components/MemberAvatar'
import { MemberSelect } from '../../components/MemberSelect'
import { RECURRENCE_OPTIONS, describeRecurrence } from './dates'
import { useChoreTemplates, useCreateFromTemplate, useSaveTemplate, useSetTemplateActive } from './useChores'

type Editor = { open: false } | { open: true; template?: ChoreTemplate }

/** Chore templates ("defaults"): reusable chores, optionally repeating. */
export function ChoreTemplatesPage() {
  const [showInactive, setShowInactive] = useState(false)
  const { data: templates, isPending, isError } = useChoreTemplates(showInactive)
  const setActive = useSetTemplateActive()
  const addToday = useCreateFromTemplate()
  const [editor, setEditor] = useState<Editor>({ open: false })
  const isPhone = useMediaQuery('(max-width: 48em)')
  const editing = editor.open ? editor.template : undefined

  function toggleActive(t: ChoreTemplate) {
    setActive.mutate({ id: t.id, active: !t.active }, {
      onSuccess: (r) => notifications.show({ color: 'teal', message: `${r.name} ${r.active ? 'shown again' : 'hidden'}` }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  function addForToday(t: ChoreTemplate) {
    addToday.mutate({ templateId: t.id }, {
      onSuccess: () => notifications.show({ color: 'teal', message: `${t.name} added for today` }),
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  return (
    <Stack>
      <Anchor component={Link} to="/chores" size="sm">
        <Group gap={4}><IconArrowLeft size={14} />Chores</Group>
      </Anchor>
      <Group justify="space-between">
        <Title order={3}>Chore templates</Title>
        <Button leftSection={<IconPlus size={18} />} onClick={() => setEditor({ open: true })}>Add</Button>
      </Group>
      <Text size="sm" c="dimmed">
        Reusable chores. Repeating ones schedule the next time automatically when they&apos;re done or skipped.
      </Text>
      <Switch label="Show hidden templates" checked={showInactive}
              onChange={(e) => setShowInactive(e.currentTarget.checked)} />

      {isPending && <Loader />}
      {isError && <Alert color="red">Couldn&apos;t load templates.</Alert>}

      {templates?.map((t) => (
        <Paper key={t.id} withBorder p="sm" opacity={t.active ? 1 : 0.6}>
          <Group wrap="nowrap">
            <div style={{ flex: 1, minWidth: 0 }}>
              <Group gap={6}>
                <Text fw={600} truncate>{t.name}</Text>
                {!t.active && <Badge size="xs" color="gray">Hidden</Badge>}
              </Group>
              <Text size="sm" c="dimmed">
                {describeRecurrence(t.recurrence, t.recurrenceInterval)}
                {t.estimatedMinutes != null && ` · ${t.estimatedMinutes} min`}
              </Text>
            </div>
            {t.defaultAssignee && <MemberAvatar member={t.defaultAssignee} size={26} />}
            <Group gap={2} wrap="nowrap">
              {t.active && (
                <Tooltip label="Add for today">
                  <ActionIcon variant="subtle" size="lg" onClick={() => addForToday(t)} aria-label={`Add ${t.name} for today`}>
                    <IconCalendarPlus size={18} />
                  </ActionIcon>
                </Tooltip>
              )}
              <Tooltip label="Edit">
                <ActionIcon variant="subtle" color="gray" size="lg" onClick={() => setEditor({ open: true, template: t })}
                            aria-label={`Edit ${t.name}`}>
                  <IconPencil size={18} />
                </ActionIcon>
              </Tooltip>
              <Tooltip label={t.active ? 'Hide' : 'Show again'}>
                <ActionIcon variant="subtle" color={t.active ? 'red' : 'teal'} size="lg" onClick={() => toggleActive(t)}
                            aria-label={`${t.active ? 'Hide' : 'Show'} ${t.name}`}>
                  {t.active ? <IconEyeOff size={18} /> : <IconEye size={18} />}
                </ActionIcon>
              </Tooltip>
            </Group>
          </Group>
        </Paper>
      ))}

      <Modal opened={editor.open} onClose={() => setEditor({ open: false })}
             title={editing ? 'Edit template' : 'Add template'} fullScreen={isPhone}>
        <ChoreTemplateForm key={editing?.id ?? 'new'} template={editing} onDone={() => setEditor({ open: false })} />
      </Modal>
    </Stack>
  )
}

interface FormValues {
  name: string
  description: string
  defaultAssigneeId: number | null
  estimatedMinutes: number | string
  recurrence: Recurrence
  recurrenceInterval: number | string
}

function ChoreTemplateForm({ template, onDone }: { template?: ChoreTemplate; onDone: () => void }) {
  const save = useSaveTemplate(template?.id)
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<FormValues>({
    mode: 'controlled',
    initialValues: {
      name: template?.name ?? '',
      description: template?.description ?? '',
      defaultAssigneeId: template?.defaultAssignee?.id ?? null,
      estimatedMinutes: template?.estimatedMinutes ?? '',
      recurrence: template?.recurrence ?? 'WEEKLY',
      recurrenceInterval: template?.recurrenceInterval ?? 2,
    },
    validate: {
      name: (v) => (!v.trim() ? 'Name is required' : v.length > 100 ? 'Max 100 characters' : null),
      estimatedMinutes: (v) => (v === '' || (Number(v) >= 1 && Number(v) <= 600) ? null : '1–600 minutes'),
      recurrenceInterval: (v, values) =>
        values.recurrence !== 'EVERY_N_DAYS' || (Number(v) >= 1 && Number(v) <= 365) ? null : '1–365 days',
    },
  })

  async function handleSubmit(values: FormValues) {
    setFormError(null)
    const input: ChoreTemplateInput = {
      name: values.name.trim(),
      description: values.description.trim() || null,
      defaultAssigneeId: values.defaultAssigneeId,
      estimatedMinutes: values.estimatedMinutes === '' ? null : Number(values.estimatedMinutes),
      recurrence: values.recurrence,
      recurrenceInterval: values.recurrence === 'EVERY_N_DAYS' ? Number(values.recurrenceInterval) : null,
    }
    try {
      const saved = await save.mutateAsync(input)
      notifications.show({ color: 'teal', message: `${saved.name} saved` })
      onDone()
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) form.setErrors(e.fieldErrors)
      else setFormError(e instanceof Error ? e.message : 'Something went wrong')
    }
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Name" required data-autofocus {...form.getInputProps('name')} />
        <Group grow align="flex-start">
          <Select label="Repeats" data={RECURRENCE_OPTIONS} allowDeselect={false} {...form.getInputProps('recurrence')} />
          {form.values.recurrence === 'EVERY_N_DAYS' && (
            <NumberInput label="Every … days" min={1} max={365} allowDecimal={false}
                         {...form.getInputProps('recurrenceInterval')} />
          )}
        </Group>
        <Group grow align="flex-start">
          <MemberSelect label="Usually done by" emptyLabel="Anyone" {...form.getInputProps('defaultAssigneeId')} />
          <NumberInput label="Minutes" min={1} max={600} allowDecimal={false} placeholder="Optional"
                       {...form.getInputProps('estimatedMinutes')} />
        </Group>
        <Textarea label="Notes" autosize minRows={1} maxRows={4} {...form.getInputProps('description')} />
        <Group>
          <Button type="submit" loading={form.submitting}>Save</Button>
          <Button variant="default" onClick={onDone}>Cancel</Button>
        </Group>
      </Stack>
    </form>
  )
}
