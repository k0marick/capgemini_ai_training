import type { NotificationType, ServerMessage } from './types'

const SERVER_TYPES = new Set(['NOTIFICATION_BATCH', 'SUBSCRIPTION_UPDATED', 'ERROR'])

/** Returns null for anything that is not a known server frame, so a bad frame never breaks the UI. */
export function parseServerMessage(raw: string): ServerMessage | null {
  let value: unknown
  try {
    value = JSON.parse(raw)
  } catch {
    return null
  }
  if (typeof value !== 'object' || value === null) return null
  const type = (value as { type?: unknown }).type
  if (typeof type !== 'string' || !SERVER_TYPES.has(type)) return null
  return value as ServerMessage
}

export function subscribeFrame(enabledTypes: NotificationType[]): string {
  return JSON.stringify({ type: 'SUBSCRIBE', payload: { enabledTypes } })
}

/** Same-origin socket URL; the Vite dev server proxies /ws with upgrade enabled. */
export function socketUrl(location: Pick<Location, 'protocol' | 'host'>): string {
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
  return `${scheme}://${location.host}/ws/notifications`
}
