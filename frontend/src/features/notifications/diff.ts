import type { NotificationItem } from './types'

/**
 * Items in `next` that were not in `previous`, matched by (type, taskId).
 * `previous` is null before the first batch of a session: that batch is a snapshot, not news.
 */
export function newNotifications(
  previous: NotificationItem[] | null,
  next: NotificationItem[],
): NotificationItem[] {
  if (previous === null) return []
  const seen = new Set(previous.map((item) => `${item.type}:${item.taskId}`))
  return next.filter((item) => !seen.has(`${item.type}:${item.taskId}`))
}
