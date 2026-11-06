import { ApiError, api, isUnauthorized } from './client'
import type { Member } from './types'

/** The logged-in member, or null when not logged in. */
export async function getMe(): Promise<Member | null> {
  try {
    return await api.get<Member>('/api/auth/me')
  } catch (error) {
    if (isUnauthorized(error)) return null
    throw error
  }
}

export async function login(username: string, password: string, rememberMe: boolean): Promise<void> {
  try {
    await api.postForm<void>('/api/auth/login', {
      username,
      password,
      ...(rememberMe ? { 'remember-me': 'true' } : {}),
    })
  } catch (error) {
    if (isUnauthorized(error)) {
      throw new ApiError(401, 'Wrong username or password')
    }
    throw error
  }
}

export function logout(): Promise<void> {
  return api.post<void>('/api/auth/logout')
}
