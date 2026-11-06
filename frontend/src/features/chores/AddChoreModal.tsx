import { Button, Group, Modal, SimpleGrid, Stack, Tabs, Text } from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { useMediaQuery } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { IconRepeat } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router'
import type { IsoDate } from '../../api/types'
import { ChoreForm } from './ChoreForm'
import { formatDay } from './dates'
import { useChoreTemplates, useCreateFromTemplate, useSaveChore } from './useChores'

interface Props {
  opened: boolean
  onClose: () => void
  today: IsoDate
}

/** Add a chore: one tap on a template, or a custom one-off chore. */
export function AddChoreModal({ opened, onClose, today }: Props) {
  const isPhone = useMediaQuery('(max-width: 48em)')
  const [date, setDate] = useState<IsoDate | null>(today)
  const { data: templates = [] } = useChoreTemplates()
  const fromTemplate = useCreateFromTemplate()
  const create = useSaveChore(undefined)

  function addTemplate(templateId: number, name: string) {
    const day = date ?? today
    fromTemplate.mutate({ templateId, date: day }, {
      onSuccess: () => {
        notifications.show({ color: 'teal', message: `${name} added for ${formatDay(day, today).toLowerCase()}` })
        onClose()
      },
      onError: (e) => notifications.show({ color: 'red', message: e.message }),
    })
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Add chore" fullScreen={isPhone}>
      <Tabs defaultValue="templates">
        <Tabs.List grow mb="md">
          <Tabs.Tab value="templates">From template</Tabs.Tab>
          <Tabs.Tab value="custom">Custom</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="templates">
          <Stack>
            <DatePickerInput label="Day" value={date} onChange={setDate} valueFormat="ddd D MMM YYYY"
                             firstDayOfWeek={1} />
            <SimpleGrid cols={2} spacing="xs">
              {templates.map((t) => (
                <Button key={t.id} variant="light" justify="flex-start" loading={fromTemplate.isPending &&
                        fromTemplate.variables?.templateId === t.id}
                        leftSection={t.recurrence !== 'NONE' ? <IconRepeat size={14} /> : undefined}
                        onClick={() => addTemplate(t.id, t.name)}>
                  <Text size="sm" truncate>{t.name}</Text>
                </Button>
              ))}
            </SimpleGrid>
            <Group justify="flex-end">
              <Button component={Link} to="/chores/templates" variant="subtle" size="xs" onClick={onClose}>
                Manage templates
              </Button>
            </Group>
          </Stack>
        </Tabs.Panel>

        <Tabs.Panel value="custom">
          <ChoreForm
            defaultDate={today}
            onSubmit={async (input) => {
              const chore = await create.mutateAsync(input)
              notifications.show({ color: 'teal', message: `${chore.title} added` })
              onClose()
            }}
            onCancel={onClose}
          />
        </Tabs.Panel>
      </Tabs>
    </Modal>
  )
}
