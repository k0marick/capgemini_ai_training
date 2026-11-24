import { parseServerMessage, subscribeFrame } from './protocol'
import type { ConnectionStatus, NotificationType, ServerMessage } from './types'

/** Close code sent when the HTTP session ends (logout or expiry). Terminal: no reconnect. */
export const CLOSE_SESSION_ENDED = 4401

/** WebSocket.OPEN; a constant so the class does not depend on the global WebSocket. */
const OPEN = 1
const BASE_DELAY_MS = 1_000
const MAX_DELAY_MS = 30_000

/**
 * Exponential backoff from 1 s up to 30 s, with jitter in [ceiling/2, ceiling].
 * `attempt` is 0 for the first retry.
 */
export function backoffDelay(attempt: number, random: () => number = Math.random): number {
  const ceiling = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * 2 ** attempt)
  return ceiling / 2 + random() * (ceiling / 2)
}

export type SocketOptions = {
  url: string
  onStatus: (status: ConnectionStatus) => void
  onMessage: (message: ServerMessage) => void
  /** Test seam; defaults to the browser WebSocket. */
  createSocket?: (url: string) => WebSocket
  random?: () => number
}

/**
 * One notification socket with reconnect. Owned by NotificationProvider.
 * Re-sends the last SUBSCRIBE preferences on every open; stops on stop() or close code 4401.
 */
export class NotificationSocket {
  private readonly options: SocketOptions
  private socket: WebSocket | null = null
  private attempt = 0
  private timer: ReturnType<typeof setTimeout> | undefined
  private preferences: NotificationType[] | null = null
  private stopped = true

  constructor(options: SocketOptions) {
    this.options = options
  }

  start(): void {
    this.stopped = false
    this.connect()
  }

  stop(): void {
    this.stopped = true
    clearTimeout(this.timer)
    const socket = this.socket
    this.socket = null
    socket?.close(1000, 'client-stop')
  }

  /** Stores the preferences and sends them now if the socket is open; otherwise on the next open. */
  setPreferences(enabledTypes: NotificationType[]): void {
    this.preferences = enabledTypes
    if (this.socket?.readyState === OPEN) {
      this.socket.send(subscribeFrame(enabledTypes))
    }
  }

  private connect(): void {
    // Retries report 'reconnecting' from onclose, before the delay starts.
    if (this.attempt === 0) this.options.onStatus('connecting')
    const socket = this.options.createSocket
      ? this.options.createSocket(this.options.url)
      : new WebSocket(this.options.url)
    this.socket = socket

    socket.onopen = () => {
      this.attempt = 0
      this.options.onStatus('open')
      if (this.preferences) socket.send(subscribeFrame(this.preferences))
    }
    socket.onmessage = (event: MessageEvent) => {
      const message = parseServerMessage(String(event.data))
      if (message) this.options.onMessage(message)
    }
    socket.onclose = (event: CloseEvent) => {
      if (socket !== this.socket) return
      this.socket = null
      if (this.stopped || event.code === CLOSE_SESSION_ENDED) {
        this.stopped = true
        this.options.onStatus('closed')
        return
      }
      const delay = backoffDelay(this.attempt, this.options.random)
      this.attempt += 1
      this.options.onStatus('reconnecting')
      this.timer = setTimeout(() => this.connect(), delay)
    }
  }
}
