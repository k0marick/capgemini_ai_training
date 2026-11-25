import { useChangeTaskStatus, useDeleteTask } from '../hooks'
import { PRIORITY_LABELS, STATUS_LABELS, TASK_STATUSES, type Task, type TaskStatus } from '../types'
import { describeTaskError } from '../errors'

type Props = {
  task: Task
  userId: string
  onEdit: (task: Task) => void
}

export default function TaskListItem({ task, userId, onEdit }: Props) {
  const changeStatus = useChangeTaskStatus(userId)
  const deleteTask = useDeleteTask(userId)

  const onDelete = () => {
    if (window.confirm(`Delete task "${task.title}"? This cannot be undone.`)) {
      deleteTask.mutate(task)
    }
  }

  return (
    <li className="flex flex-wrap items-center gap-3 rounded-lg border border-slate-200 bg-white p-3">
      <select
        aria-label={`Status of ${task.title}`}
        value={task.status}
        onChange={(event) =>
          changeStatus.mutate({ task, status: event.target.value as TaskStatus })
        }
        className="rounded-md border border-slate-300 bg-white px-2 py-1 text-sm focus-visible:outline-2 focus-visible:outline-indigo-600"
      >
        {TASK_STATUSES.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>
      <button
        type="button"
        onClick={() => onEdit(task)}
        className="min-w-0 flex-1 rounded-md px-1 text-left focus-visible:outline-2 focus-visible:outline-indigo-600"
      >
        <span
          className={`block truncate font-medium ${task.status === 'DONE' ? 'text-slate-500 line-through' : 'text-slate-900'}`}
        >
          {task.title}
        </span>
        <span className="text-xs text-slate-500">
          {PRIORITY_LABELS[task.priority]} priority
          {task.dueDate ? ` · due ${task.dueDate}` : ''}
        </span>
      </button>
      <button
        type="button"
        aria-label={`Edit ${task.title}`}
        onClick={() => onEdit(task)}
        className="rounded-md px-2 py-1 text-sm font-medium text-slate-700 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600"
      >
        Edit
      </button>
      <button
        type="button"
        aria-label={`Delete ${task.title}`}
        onClick={onDelete}
        disabled={deleteTask.isPending}
        className="rounded-md px-2 py-1 text-sm font-medium text-red-700 hover:bg-red-50 focus-visible:outline-2 focus-visible:outline-indigo-600 disabled:opacity-50"
      >
        Delete
      </button>
      {changeStatus.isError && (
        <p role="alert" className="w-full text-sm text-red-700">
          {describeTaskError(changeStatus.error)}
        </p>
      )}
      {deleteTask.isError && (
        <p role="alert" className="w-full text-sm text-red-700">
          {describeTaskError(deleteTask.error)}
        </p>
      )}
    </li>
  )
}
