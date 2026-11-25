import type { TaskPriority } from '../tasks/types'

export const NOTIFICATION_TYPES = ['OVERDUE', 'DUE_SOON'] as const
export type NotificationType = (typeof NOTIFICATION_TYPES)[number]

export const NOTIFICATION_LABELS: Record<NotificationType, string> = {
  OVERDUE: 'Overdue',
  DUE_SOON: 'Due soon',
}

export type NotificationItem = {
  type: NotificationType
  taskId: string
  projectId: string
  title: string
  dueDate: string
  priority: TaskPriority
}

export type ConnectionStatus = 'connecting' | 'open' | 'reconnecting' | 'closed'

/** Server → client frames (architecture §10.2). */
export type ServerMessage =
  | { type: 'NOTIFICATION_BATCH'; payload: { notifications: NotificationItem[] } }
  | { type: 'SUBSCRIPTION_UPDATED'; payload: { enabledTypes: NotificationType[] } }
  | { type: 'ERROR'; payload: { code: string; message: string } }
