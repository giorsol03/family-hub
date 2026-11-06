import { Select, type SelectProps } from '@mantine/core'
import { useMembers } from '../features/members/useMembers'

interface Props extends Omit<SelectProps, 'data' | 'value' | 'defaultValue' | 'onChange'> {
  /** Controlled value (member id, or null = the empty option). */
  value?: number | null
  /** Uncontrolled initial value (used by @mantine/form's uncontrolled mode). */
  defaultValue?: number | null
  onChange?: (memberId: number | null) => void
  /** Label for the "nobody / everyone" choice; when set, null is a selectable option. */
  emptyLabel?: string
}

/** Pick an active household member (ids are converted to/from the strings Mantine's Select uses). */
export function MemberSelect({ value, defaultValue, onChange, emptyLabel, ...props }: Props) {
  const { data: members = [] } = useMembers()
  const current = value !== undefined ? value : defaultValue
  const toOption = (id: number | null | undefined) => (id == null ? (emptyLabel ? '' : null) : String(id))
  const data = [
    ...(emptyLabel ? [{ value: '', label: emptyLabel }] : []),
    ...members.filter((m) => m.active || m.id === current).map((m) => ({ value: String(m.id), label: m.name })),
  ]
  return (
    <Select
      data={data}
      {...(value !== undefined ? { value: toOption(value) } : { defaultValue: toOption(defaultValue) })}
      onChange={(v) => onChange?.(v ? Number(v) : null)}
      allowDeselect={false}
      {...props}
    />
  )
}
