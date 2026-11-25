export const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const
export type TaskStatus = (typeof TASK_STATUSES)[number]

export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const
export type TaskPriority = (typeof TASK_PRIORITIES)[number]

export const DUE_FILTERS = ['ALL', 'OVERDUE', 'TODAY', 'NEXT_7_DAYS'] as const
export type DueFilter = (typeof DUE_FILTERS)[number]

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
}

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
}

export const DUE_LABELS: Record<DueFilter, string> = {
  ALL: 'Any date',
  OVERDUE: 'Overdue',
  TODAY: 'Due today',
  NEXT_7_DAYS: 'Next 7 days',
}

export type Task = {
  id: string
  projectId: string
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export type TaskRequest = {
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
}

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
