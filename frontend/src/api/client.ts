/**
 * Small fetch wrapper for the Spring Boot API.
 * - Same-origin session cookie auth (Vite proxies /api in dev; Spring serves the SPA in prod).
 * - CSRF: Spring sets an XSRF-TOKEN cookie; we echo it in the X-XSRF-TOKEN header on writes.
 * - Errors: Spring returns RFC 7807 ProblemDetail, optionally with a fieldErrors map.
 */

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

export type FieldErrors = Record<string, string>

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldErrors

  constructor(status: number, message: string, fieldErrors: FieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function readCookie(name: string): string | undefined {
  const entry = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`))
  return entry ? decodeURIComponent(entry.substring(name.length + 1)) : undefined
}

/** Returns the CSRF token, asking the server to issue one if the cookie is missing (first visit, after login/logout). */
async function csrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE)
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    token = readCookie(CSRF_COOKIE)
  }
  return token ?? ''
}

async function toApiError(res: Response): Promise<ApiError> {
  let message = res.statusText || `Request failed (${res.status})`
  let fieldErrors: FieldErrors = {}
  try {
    const body = await res.json()
    message = body.detail ?? body.title ?? body.message ?? message
    fieldErrors = body.fieldErrors ?? {}
  } catch {
    // not JSON – keep the status text
  }
  return new ApiError(res.status, message, fieldErrors)
}

type Body = { json: unknown } | { form: Record<string, string> }

export async function request<T>(method: string, url: string, body?: Body): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  let payload: BodyInit | undefined

  if (body && 'json' in body) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body.json)
  } else if (body && 'form' in body) {
    payload = new URLSearchParams(body.form)
  }
  if (method !== 'GET') {
    headers[CSRF_HEADER] = await csrfToken()
  }

  const res = await fetch(url, { method, headers, body: payload, credentials: 'same-origin' })
  if (!res.ok) {
    throw await toApiError(res)
  }
  if (res.status === 204 || res.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  return (await res.json()) as T
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, json?: unknown) => request<T>('POST', url, json === undefined ? undefined : { json }),
  put: <T>(url: string, json: unknown) => request<T>('PUT', url, { json }),
  postForm: <T>(url: string, form: Record<string, string>) => request<T>('POST', url, { form }),
}

/**
 * Server field paths use brackets for list items ("ingredients[0].name");
 * @mantine/form uses dots ("ingredients.0.name").
 */
export function toFormErrors(fieldErrors: FieldErrors): FieldErrors {
  return Object.fromEntries(
    Object.entries(fieldErrors).map(([path, message]) => [path.replace(/\[(\d+)\]/g, '.$1'), message]),
  )
}

export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}
