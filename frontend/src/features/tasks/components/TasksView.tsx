import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  hasActiveFilters,
  PAGE_SIZE,
  parseFilters,
  toSearchParams,
  type TaskFilters,
} from '../filters'
import { useTasks } from '../hooks'
import type { Task } from '../types'
import TaskEmptyState from './TaskEmptyState'
import TaskFormDialog from './TaskFormDialog'
import TaskListItem from './TaskListItem'
import TaskToolbar from './TaskToolbar'

type Props = {
  userId: string
  projectId?: string
  title: string
}

type Dialog = { mode: 'create' } | { mode: 'edit'; task: Task }

export default function TasksView({ userId, projectId, title }: Props) {
  const [params, setParams] = useSearchParams()
  const filters = parseFilters(params)
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState<Dialog | null>(null)

  const updateFilters = useCallback(
    (next: Partial<TaskFilters>) => {
      setPage(0)
      setParams(toSearchParams({ ...parseFilters(params), ...next }), { replace: true })
    },
    [params, setParams],
  )

  const tasks = useTasks(userId, filters, projectId, page)
  const data = tasks.data
  const filtered = hasActiveFilters(filters)

  return (
    <section aria-labelledby="tasks-heading" className="flex min-w-0 flex-1 flex-col gap-4">
      <h2 id="tasks-heading" className="text-xl font-semibold tracking-tight text-slate-900">
        {title}
      </h2>
      <TaskToolbar
        filters={filters}
        onChange={updateFilters}
        onNewTask={projectId ? () => setDialog({ mode: 'create' }) : undefined}
      />

      {tasks.isError && (
        <p role="alert" className="text-sm text-red-700">
          Tasks could not be loaded. Please refresh the page.
        </p>
      )}

      {!data && tasks.isPending && (
        <ul aria-busy="true" aria-label="Loading tasks" className="flex flex-col gap-2">
          {[0, 1, 2, 3].map((row) => (
            <li key={row} className="h-14 animate-pulse rounded-lg bg-slate-200" />
          ))}
        </ul>
      )}

      {data &&
        data.content.length === 0 &&
        (filtered ? (
          <TaskEmptyState
            variant="no-results"
            onReset={() => updateFilters({ q: '', status: '', priority: '', due: 'ALL' })}
          />
        ) : (
          <TaskEmptyState
            variant="no-tasks"
            onCreate={projectId ? () => setDialog({ mode: 'create' }) : undefined}
          />
        ))}

      {data && data.content.length > 0 && (
        <>
          <ul className="flex flex-col gap-2" aria-label="Tasks">
            {data.content.map((task) => (
              <TaskListItem
                key={task.id}
                task={task}
                userId={userId}
                onEdit={(item) => setDialog({ mode: 'edit', task: item })}
              />
            ))}
          </ul>
          {data.totalPages > 1 && (
            <nav
              aria-label="Pagination"
              className="flex items-center justify-between text-sm text-slate-600"
            >
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
                className="rounded-md px-3 py-1.5 font-medium hover:bg-slate-200 disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-indigo-600"
              >
                Previous
              </button>
              <span>
                Page {data.page + 1} of {data.totalPages} ({data.totalElements} tasks, {PAGE_SIZE}{' '}
                per page)
              </span>
              <button
                type="button"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((current) => current + 1)}
                className="rounded-md px-3 py-1.5 font-medium hover:bg-slate-200 disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-indigo-600"
              >
                Next
              </button>
            </nav>
          )}
        </>
      )}

      {dialog && projectId && (
        <TaskFormDialog
          key={dialog.mode === 'edit' ? dialog.task.id : 'create'}
          userId={userId}
          projectId={projectId}
          task={dialog.mode === 'edit' ? dialog.task : undefined}
          onClose={() => setDialog(null)}
        />
      )}
    </section>
  )
}
