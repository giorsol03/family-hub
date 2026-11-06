import { Avatar, type AvatarProps } from '@mantine/core'
import type { Member } from '../api/types'

interface Props extends Omit<AvatarProps, 'color'> {
  member: Pick<Member, 'name' | 'color'>
}

/** Coloured circle with the member's initial. */
export function MemberAvatar({ member, ...props }: Props) {
  return (
    <Avatar
      radius="xl"
      variant="filled"
      color={member.color}
      title={member.name}
      {...props}
    >
      {member.name.trim().charAt(0).toUpperCase()}
    </Avatar>
  )
}
