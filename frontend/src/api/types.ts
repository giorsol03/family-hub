export interface Member {
  id: number
  name: string
  username: string
  color: string
  active: boolean
}

export interface MemberInput {
  name: string
  username: string
  color: string
  /** Required when creating; blank keeps the current password when updating. */
  password: string
}
