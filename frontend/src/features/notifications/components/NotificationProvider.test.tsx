import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { NotificationItem } from '../types'
import ConnectionIndicator from './ConnectionIndicator'
import NotificationBell from './NotificationBell'
import NotificationProvider from './NotificationProvider'
import ToastRegion from './ToastRegion'

class FakeSocket {
  static instances: FakeSocket[] = []
  readyState = 0
  sent: string[] = []
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null

  constructor() {
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

  receive(message: unknown) {
    this.onmessage?.({ data: JSON.stringify(message) } as MessageEvent)
  }
}

const item = (
  taskId: string,
  title: string,
  type: NotificationItem['type'] = 'OVERDUE',
): NotificationItem => ({
  type,
  taskId,
  projectId: 'p1',
  title,
  dueDate: '2026-11-23',
  priority: 'HIGH',
})

const batch = (...notifications: NotificationItem[]) => ({
  type: 'NOTIFICATION_BATCH',
  payload: { notifications },
})

function renderDashboardSlice() {
  return render(
    <MemoryRouter>
      <NotificationProvider userId="user-1">
        <ConnectionIndicator />
        <NotificationBell />
        <ToastRegion />
      </NotificationProvider>
    </MemoryRouter>,
  )
}

const socket = () => FakeSocket.instances[FakeSocket.instances.length - 1]

/** Toast elements live inside the polite live region. */
const toastTexts = () =>
  Array.from(document.querySelectorAll('[aria-live="polite"] p')).map((node) => node.textContent)

describe('NotificationProvider', () => {
  beforeEach(() => {
    FakeSocket.instances = []
    vi.stubGlobal('WebSocket', FakeSocket)
  })

  it('shows the connection status and the count of items needing attention', () => {
    renderDashboardSlice()
    expect(screen.getByTestId('connection-status')).toHaveTextContent('Connecting')

    act(() => socket().open())
    expect(screen.getByTestId('connection-status')).toHaveTextContent('Live')

    act(() => socket().receive(batch(item('t1', 'Pay rent'), item('t2', 'Docs', 'DUE_SOON'))))
    expect(
      screen.getByRole('button', { name: 'Notifications, 2 need attention' }),
    ).toBeInTheDocument()
  })

  it('toasts only items that are new since the previous batch, and not the first snapshot', () => {
    renderDashboardSlice()
    act(() => socket().open())

    act(() => socket().receive(batch(item('t1', 'Pay rent'))))
    expect(toastTexts()).toEqual([])

    act(() => socket().receive(batch(item('t1', 'Pay rent'), item('t2', 'Send invoice'))))
    expect(toastTexts()).toEqual(['Overdue: Send invoice'])

    act(() => socket().receive(batch(item('t1', 'Pay rent'), item('t2', 'Send invoice'))))
    expect(toastTexts()).toEqual(['Overdue: Send invoice'])
  })

  it('panel lists the snapshot and toggling a type sends SUBSCRIBE', async () => {
    renderDashboardSlice()
    act(() => socket().open())
    act(() => socket().receive(batch(item('t1', 'Pay rent'), item('t2', 'Docs', 'DUE_SOON'))))

    await userEvent.click(screen.getByRole('button', { name: /Notifications/ }))
    expect(screen.getByRole('link', { name: 'Pay rent' })).toHaveAttribute('href', '/projects/p1')
    expect(screen.getByRole('link', { name: 'Docs' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('checkbox', { name: 'Overdue' }))
    expect(socket().sent.at(-1)).toBe(
      '{"type":"SUBSCRIBE","payload":{"enabledTypes":["DUE_SOON"]}}',
    )
  })
})
