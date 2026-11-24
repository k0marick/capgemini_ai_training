import { useEffect, useState } from 'react'
import { hasActiveFilters, type TaskFilters } from '../filters'
import {
  DUE_FILTERS,
  DUE_LABELS,
  PRIORITY_LABELS,
  STATUS_LABELS,
  TASK_PRIORITIES,
  TASK_STATUSES,
  type DueFilter,
  type TaskPriority,
  type TaskStatus,
} from '../types'

const SEARCH_DEBOUNCE_MS = 300

type Props = {
  filters: TaskFilters
  onChange: (next: Partial<TaskFilters>) => void
  onNewTask?: () => void
}

const fieldClass =
  'rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm focus-visible:outline-2 focus-visible:outline-indigo-600'

export default function TaskToolbar({ filters, onChange, onNewTask }: Props) {
  // Local draft keeps typing responsive; the URL (and query) updates after the debounce.
  const [draft, setDraft] = useState(filters.q)
  const [syncedQ, setSyncedQ] = useState(filters.q)

  // Adopt external changes to the query (e.g. reset) during render, not in an effect.
  if (filters.q !== syncedQ) {
    setSyncedQ(filters.q)
    setDraft(filters.q)
  }

  useEffect(() => {
    if (draft === filters.q) return
    const timer = setTimeout(() => onChange({ q: draft }), SEARCH_DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [draft, filters.q, onChange])

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex min-w-0 flex-1 flex-col text-xs font-medium text-slate-600">
          Search
          <input
            type="search"
            value={draft}
            maxLength={100}
            placeholder="Title or description"
            onChange={(event) => setDraft(event.target.value)}
            className={`${fieldClass} min-w-0`}
          />
        </label>
        {onNewTask && (
          <button
            type="button"
            onClick={onNewTask}
            className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-indigo-600"
          >
            New task
          </button>
        )}
      </div>
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex flex-col text-xs font-medium text-slate-600">
          Status
          <select
            value={filters.status}
            onChange={(event) => onChange({ status: event.target.value as TaskStatus | '' })}
            className={fieldClass}
          >
            <option value="">Any</option>
            {TASK_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col text-xs font-medium text-slate-600">
          Priority
          <select
            value={filters.priority}
            onChange={(event) => onChange({ priority: event.target.value as TaskPriority | '' })}
            className={fieldClass}
          >
            <option value="">Any</option>
            {TASK_PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>
                {PRIORITY_LABELS[priority]}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col text-xs font-medium text-slate-600">
          Due
          <select
            value={filters.due}
            onChange={(event) => onChange({ due: event.target.value as DueFilter })}
            className={fieldClass}
          >
            {DUE_FILTERS.map((due) => (
              <option key={due} value={due}>
                {DUE_LABELS[due]}
              </option>
            ))}
          </select>
        </label>
        {hasActiveFilters(filters) && (
          <button
            type="button"
            onClick={() => {
              setDraft('')
              onChange({ q: '', status: '', priority: '', due: 'ALL' })
            }}
            className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600"
          >
            Reset
          </button>
        )}
      </div>
    </div>
  )
}
