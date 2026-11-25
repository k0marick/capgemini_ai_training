import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { newNotifications } from './diff'
import { parseServerMessage } from './protocol'
import { backoffDelay, NotificationSocket } from './socket'
import type { NotificationItem } from './types'

/** Scripted stand-in for the browser WebSocket: the test decides when it opens, receives and closes. */
class FakeSocket {
  static instances: FakeSocket[] = []
  readyState = 0
  sent: string[] = []
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null

  readonly url: string

  constructor(url: string) {
    this.url = url
    FakeSocket.instances.push(this)
  }

  send(data: string) {
    this.sent.push(data)
  }

  close() {
    this.readyState = 3
  }

  open() {
    this.readyState = 1
    this.onopen?.(new Event('open'))
  }

  receive(data: string) {
    this.onmessage?.({ data } as MessageEvent)
  }

  end(code: number) {
    this.readyState = 3
    this.onclose?.({ code } as CloseEvent)
  }
}

function createTestSocket(statuses: string[]) {
  return new NotificationSocket({
    url: 'ws://test/ws/notifications',
    onStatus: (status) => statuses.push(status),
    onMessage: () => {},
    createSocket: (url) => new FakeSocket(url) as unknown as WebSocket,
    random: () => 0,
  })
}

const latest = () => FakeSocket.instances[FakeSocket.instances.length - 1]

describe('backoffDelay', () => {
  it('grows exponentially from 1 s with jitter in [half, full]', () => {
    expect(backoffDelay(0, () => 0)).toBe(500)
    expect(backoffDelay(0, () => 1)).toBe(1000)
    expect(backoffDelay(1, () => 1)).toBe(2000)
    expect(backoffDelay(2, () => 1)).toBe(4000)
  })

  it('caps at 30 s', () => {
    expect(backoffDelay(10, () => 1)).toBe(30_000)
    expect(backoffDelay(10, () => 0)).toBe(15_000)
  })
})

describe('NotificationSocket', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    FakeSocket.instances = []
    vi.stubGlobal('WebSocket', FakeSocket)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('re-sends the stored preferences on every open', () => {
    const statuses: string[] = []
    const socket = createTestSocket(statuses)
    socket.start()
    latest().open()
    expect(latest().sent).toEqual([])

    socket.setPreferences(['OVERDUE'])
    expect(latest().sent).toEqual(['{"type":"SUBSCRIBE","payload":{"enabledTypes":["OVERDUE"]}}'])

    latest().end(1006)
    vi.advanceTimersByTime(1000)
    latest().open()
    expect(latest().sent).toEqual(['{"type":"SUBSCRIBE","payload":{"enabledTypes":["OVERDUE"]}}'])
    expect(statuses).toEqual(['connecting', 'open', 'reconnecting', 'open'])
  })

  it('reconnects after an unexpected close, with growing delays', () => {
    const statuses: string[] = []
    const socket = createTestSocket(statuses)
    socket.start()
    latest().end(1006)
    expect(FakeSocket.instances).toHaveLength(1)
    vi.advanceTimersByTime(500)
    expect(FakeSocket.instances).toHaveLength(2)

    latest().end(1006)
    vi.advanceTimersByTime(999)
    expect(FakeSocket.instances).toHaveLength(2)
    vi.advanceTimersByTime(1)
    expect(FakeSocket.instances).toHaveLength(3)
    expect(statuses.at(-1)).toBe('reconnecting')
  })

  it('treats close code 4401 as terminal', () => {
    const statuses: string[] = []
    const socket = createTestSocket(statuses)
    socket.start()
    latest().open()
    latest().end(4401)

    vi.advanceTimersByTime(60_000)
    expect(FakeSocket.instances).toHaveLength(1)
    expect(statuses.at(-1)).toBe('closed')
  })

  it('stop() cancels a pending reconnect', () => {
    const statuses: string[] = []
    const socket = createTestSocket(statuses)
    socket.start()
    latest().end(1006)
    socket.stop()

    vi.advanceTimersByTime(60_000)
    expect(FakeSocket.instances).toHaveLength(1)
  })

  it('ignores frames that are not known server messages', () => {
    const received: string[] = []
    const socket = new NotificationSocket({
      url: 'ws://test',
      onStatus: () => {},
      onMessage: (message) => received.push(message.type),
      createSocket: (url) => new FakeSocket(url) as unknown as WebSocket,
    })
    socket.start()
    latest().receive('not json')
    latest().receive('{"type":"WHATEVER"}')
    latest().receive('{"type":"SUBSCRIPTION_UPDATED","payload":{"enabledTypes":[]}}')
    expect(received).toEqual(['SUBSCRIPTION_UPDATED'])
  })
})

describe('parseServerMessage and newNotifications', () => {
  const item = (taskId: string, title = 'Task'): NotificationItem => ({
    type: 'OVERDUE',
    taskId,
    projectId: 'p1',
    title,
    dueDate: '2026-11-23',
    priority: 'HIGH',
  })

  it('parses known frames and rejects everything else', () => {
    expect(parseServerMessage('{"type":"ERROR","payload":{"code":"X","message":"m"}}')?.type).toBe(
      'ERROR',
    )
    expect(parseServerMessage('[]')).toBeNull()
    expect(parseServerMessage('null')).toBeNull()
    expect(parseServerMessage('{oops')).toBeNull()
  })

  it('reports only items new since the previous batch, matched by type and task', () => {
    expect(newNotifications(null, [item('a')])).toEqual([])
    expect(newNotifications([item('a')], [item('a', 'Renamed'), item('b')])).toEqual([item('b')])
  })
})
