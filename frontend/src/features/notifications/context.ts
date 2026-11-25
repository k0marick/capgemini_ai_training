import { createContext, useContext } from 'react'
import type { ConnectionStatus, NotificationItem, NotificationType } from './types'

export type Toast = { id: number; item: NotificationItem }

export type NotificationState = {
  status: ConnectionStatus
  /** Snapshot from the latest batch; replaced, never merged. */
  notifications: NotificationItem[]
  toasts: Toast[]
  dismissToast: (id: number) => void
  enabledTypes: NotificationType[]
  setEnabledTypes: (types: NotificationType[]) => void
}

export const NotificationContext = createContext<NotificationState | null>(null)

export function useNotifications(): NotificationState {
  const value = useContext(NotificationContext)
  if (!value) throw new Error('useNotifications must be used inside NotificationProvider')
  return value
}
