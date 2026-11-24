export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const UNSAFE_METHODS = new Set(['POST', 'PATCH', 'PUT', 'DELETE'])
let csrfToken: { headerName: string; token: string } | null = null

async function loadCsrfToken(): Promise<{ headerName: string; token: string }> {
  if (!csrfToken) {
    const res = await fetch('/api/v1/csrf', { credentials: 'same-origin' })
    csrfToken = (await res.json()) as { headerName: string; token: string }
  }
  return csrfToken
}

/** Fetch wrapper: same-origin cookies, CSRF header on unsafe methods, JSON errors. */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (UNSAFE_METHODS.has(method)) {
    const csrf = await loadCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const res = await fetch(path, { ...init, method, headers, credentials: 'same-origin' })
  if (!res.ok) {
    if (res.status === 403) csrfToken = null // token may be stale after login/logout
    throw new ApiError(res.status, res.statusText || 'Request failed')
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}
