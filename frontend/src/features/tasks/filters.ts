import {
  DUE_FILTERS,
  TASK_PRIORITIES,
  TASK_STATUSES,
  type DueFilter,
  type TaskPriority,
  type TaskStatus,
} from './types'

export type TaskFilters = {
  q: string
  status: TaskStatus | ''
  priority: TaskPriority | ''
  due: DueFilter
}

export const DEFAULT_FILTERS: TaskFilters = { q: '', status: '', priority: '', due: 'ALL' }
export const PAGE_SIZE = 20
const MAX_QUERY_LENGTH = 100

function isOneOf<T extends string>(values: readonly T[], value: string | null): value is T {
  return value !== null && (values as readonly string[]).includes(value)
}

/** Reads filters from URL search params; unknown values fall back to defaults. */
export function parseFilters(params: URLSearchParams): TaskFilters {
  const status = params.get('status')
  const priority = params.get('priority')
  const due = params.get('due')
  return {
    q: (params.get('q') ?? '').slice(0, MAX_QUERY_LENGTH),
    status: isOneOf(TASK_STATUSES, status) ? status : '',
    priority: isOneOf(TASK_PRIORITIES, priority) ? priority : '',
    due: isOneOf(DUE_FILTERS, due) ? due : 'ALL',
  }
}

/** Writes only non-default values so shareable URLs stay short. */
export function toSearchParams(filters: TaskFilters): URLSearchParams {
  const params = new URLSearchParams()
  if (filters.q !== '') params.set('q', filters.q)
  if (filters.status !== '') params.set('status', filters.status)
  if (filters.priority !== '') params.set('priority', filters.priority)
  if (filters.due !== 'ALL') params.set('due', filters.due)
  return params
}

export function hasActiveFilters(filters: TaskFilters): boolean {
  return (
    filters.q !== '' || filters.status !== '' || filters.priority !== '' || filters.due !== 'ALL'
  )
}

/** Builds the GET /api/v1/tasks query string. */
export function buildTaskQuery(
  filters: TaskFilters,
  projectId: string | undefined,
  page: number,
): URLSearchParams {
  const params = toSearchParams(filters)
  if (projectId) params.set('projectId', projectId)
  params.set('page', String(page))
  params.set('size', String(PAGE_SIZE))
  params.set('sort', 'dueDate,asc')
  return params
}
