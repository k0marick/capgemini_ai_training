import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { NotificationContext, type NotificationState, type Toast } from '../context'
import { newNotifications } from '../diff'
import { NotificationSocket } from '../socket'
import { socketUrl } from '../protocol'
import {
  NOTIFICATION_TYPES,
  type ConnectionStatus,
  type NotificationItem,
  type NotificationType,
} from '../types'

const MAX_TOASTS = 3

/**
 * Owns the single notification socket for the signed-in user. Unmounting (logout) closes it.
 * The socket is recreated per user, so no state carries over between accounts.
 */
export default function NotificationProvider({
  userId,
  children,
}: {
  userId: string
  children: ReactNode
}) {
  const [status, setStatus] = useState<ConnectionStatus>('connecting')
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [toasts, setToasts] = useState<Toast[]>([])
  const [enabledTypes, setEnabledTypesState] = useState<NotificationType[]>([...NOTIFICATION_TYPES])
  const socketRef = useRef<NotificationSocket | null>(null)
  const nextToastId = useRef(0)

  useEffect(() => {
    let previous: NotificationItem[] | null = null
    const socket = new NotificationSocket({
      url: socketUrl(window.location),
      onStatus: setStatus,
      onMessage: (message) => {
        if (message.type === 'NOTIFICATION_BATCH') {
          const incoming = message.payload.notifications
          const fresh = newNotifications(previous, incoming)
          previous = incoming
          setNotifications(incoming)
          if (fresh.length > 0) {
            const added = fresh.map((item) => ({ id: ++nextToastId.current, item }))
            setToasts((current) => [...current, ...added].slice(-MAX_TOASTS))
          }
        } else if (message.type === 'SUBSCRIPTION_UPDATED') {
          setEnabledTypesState(message.payload.enabledTypes)
        }
      },
    })
    socketRef.current = socket
    socket.start()
    return () => {
      socket.stop()
      socketRef.current = null
    }
  }, [userId])

  const dismissToast = useCallback(
    (id: number) => setToasts((current) => current.filter((toast) => toast.id !== id)),
    [],
  )

  const setEnabledTypes = useCallback((types: NotificationType[]) => {
    setEnabledTypesState(types)
    socketRef.current?.setPreferences(types)
  }, [])

  const value = useMemo<NotificationState>(
    () => ({ status, notifications, toasts, dismissToast, enabledTypes, setEnabledTypes }),
    [status, notifications, toasts, dismissToast, enabledTypes, setEnabledTypes],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
