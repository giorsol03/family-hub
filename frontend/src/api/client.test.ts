import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api } from './client'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function clearCookies() {
  document.cookie.split('; ').filter(Boolean).forEach((c) => {
    document.cookie = `${c.split('=')[0]}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
  })
}

describe('api client', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    clearCookies()
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => vi.unstubAllGlobals())

  it('GET sends no CSRF header and parses JSON', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [{ id: 1 }]))

    const result = await api.get<{ id: number }[]>('/api/members')

    expect(result).toEqual([{ id: 1 }])
    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers['X-XSRF-TOKEN']).toBeUndefined()
    expect(init.credentials).toBe('same-origin')
  })

  it('POST echoes the XSRF-TOKEN cookie in the X-XSRF-TOKEN header', async () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/'
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 7 }))

    await api.post('/api/members', { name: 'Maria' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/members')
    expect(init.headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ name: 'Maria' }))
  })

  it('fetches a CSRF token first when the cookie is missing', async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === '/api/auth/csrf') {
        document.cookie = 'XSRF-TOKEN=fresh; path=/'
        return jsonResponse(200, { token: 'x' })
      }
      return new Response(null, { status: 204 })
    })

    await api.post('/api/auth/logout')

    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual(['/api/auth/csrf', '/api/auth/logout'])
    expect(fetchMock.mock.calls[1][1].headers['X-XSRF-TOKEN']).toBe('fresh')
  })

  it('turns a ProblemDetail into an ApiError with field errors', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    fetchMock.mockResolvedValue(jsonResponse(409, {
      status: 409,
      detail: 'Username is already taken',
      fieldErrors: { username: 'Username is already taken' },
    }))

    const error = (await api.post('/api/members', {}).catch((e: unknown) => e)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.message).toBe('Username is already taken')
    expect(error.fieldErrors).toEqual({ username: 'Username is already taken' })
  })

  it('returns undefined for 204 No Content', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(api.post('/api/auth/logout')).resolves.toBeUndefined()
  })
})
