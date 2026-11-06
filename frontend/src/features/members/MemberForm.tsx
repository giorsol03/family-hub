import { Alert, Button, ColorInput, Group, PasswordInput, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import type { Member, MemberInput } from '../../api/types'
import { MEMBER_SWATCHES } from '../../theme'

interface Props {
  /** Existing member when editing; undefined when creating. */
  member?: Member
  onSubmit: (values: MemberInput) => Promise<unknown>
  onCancel: () => void
}

const USERNAME_PATTERN = /^[A-Za-z0-9._-]{3,50}$/
const COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/

/** Same rules as the server (MemberRequest); server field errors are shown on the inputs too. */
export function MemberForm({ member, onSubmit, onCancel }: Props) {
  const isNew = member === undefined
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<MemberInput>({
    mode: 'uncontrolled',
    initialValues: {
      name: member?.name ?? '',
      username: member?.username ?? '',
      color: member?.color ?? MEMBER_SWATCHES[0],
      password: '',
    },
    validate: {
      name: (v) => (!v.trim() ? 'Name is required' : v.length > 100 ? 'Max 100 characters' : null),
      username: (v) => (USERNAME_PATTERN.test(v.trim()) ? null : '3–50 characters: letters, digits, dot, dash or underscore'),
      color: (v) => (COLOR_PATTERN.test(v) ? null : 'Pick a colour'),
      password: (v) => {
        if (!v) return isNew ? 'Password is required' : null
        return v.length < 8 ? 'At least 8 characters' : null
      },
    },
  })

  async function handleSubmit(values: MemberInput) {
    setFormError(null)
    try {
      await onSubmit({ ...values, name: values.name.trim(), username: values.username.trim() })
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) {
        form.setErrors(e.fieldErrors)
      } else {
        setFormError(e instanceof Error ? e.message : 'Something went wrong')
      }
    }
  }

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack maw={480}>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <TextInput label="Name" required key={form.key('name')} {...form.getInputProps('name')} />
        <TextInput
          label="Username"
          required
          autoCapitalize="none"
          autoComplete="off"
          key={form.key('username')}
          {...form.getInputProps('username')}
        />
        <ColorInput
          label="Colour"
          format="hex"
          swatches={MEMBER_SWATCHES}
          swatchesPerRow={10}
          withEyeDropper={false}
          key={form.key('color')}
          {...form.getInputProps('color')}
        />
        <PasswordInput
          label="Password"
          required={isNew}
          autoComplete="new-password"
          description={isNew ? 'At least 8 characters' : 'Leave blank to keep the current password'}
          key={form.key('password')}
          {...form.getInputProps('password')}
        />
        <Group>
          <Button type="submit" loading={form.submitting}>Save</Button>
          <Button variant="default" onClick={onCancel}>Cancel</Button>
        </Group>
      </Stack>
    </form>
  )
}
