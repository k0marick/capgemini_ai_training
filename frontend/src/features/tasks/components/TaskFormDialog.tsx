import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { taskFormSchema, toTaskRequest, type TaskFormValues } from '../schemas'
import { useCreateTask, useUpdateTask } from '../hooks'
import { describeTaskError } from '../errors'
import { PRIORITY_LABELS, STATUS_LABELS, TASK_PRIORITIES, TASK_STATUSES, type Task } from '../types'

type Props = {
  userId: string
  projectId: string
  /** Present when editing; absent when creating. */
  task?: Task
  onClose: () => void
}

const fieldClass =
  'rounded-md border border-slate-300 bg-white px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-indigo-600'

/** Mounted fresh for each open, so defaults never need resetting. */
export default function TaskFormDialog({ userId, projectId, task, onClose }: Props) {
  const [serverError, setServerError] = useState<string | null>(null)
  const createTask = useCreateTask(userId, projectId)
  const updateTask = useUpdateTask(userId)
  const isEdit = task !== undefined

  const form = useForm<TaskFormValues>({
    resolver: zodResolver(taskFormSchema),
    defaultValues: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      status: task?.status ?? 'TODO',
      priority: task?.priority ?? 'MEDIUM',
      dueDate: task?.dueDate ?? '',
    },
  })

  const onSubmit = form.handleSubmit(async (values) => {
    setServerError(null)
    const body = toTaskRequest(values)
    try {
      if (task) {
        await updateTask.mutateAsync({ task, body })
      } else {
        await createTask.mutateAsync(body)
      }
      onClose()
    } catch (error) {
      setServerError(describeTaskError(error))
    }
  })

  const titleId = 'task-form-title'
  const errors = form.formState.errors

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4"
      onKeyDown={(event) => {
        if (event.key === 'Escape') onClose()
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl"
      >
        <h2 id={titleId} className="text-lg font-semibold text-slate-900">
          {isEdit ? 'Edit task' : 'New task'}
        </h2>
        <form noValidate onSubmit={onSubmit} className="mt-4 flex flex-col gap-4">
          <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
            Title
            <input
              autoFocus
              className={fieldClass}
              aria-invalid={errors.title ? true : undefined}
              {...form.register('title')}
            />
            {errors.title && (
              <span className="text-sm font-normal text-red-700">{errors.title.message}</span>
            )}
          </label>
          <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
            Description
            <textarea
              rows={3}
              className={fieldClass}
              aria-invalid={errors.description ? true : undefined}
              {...form.register('description')}
            />
            {errors.description && (
              <span className="text-sm font-normal text-red-700">{errors.description.message}</span>
            )}
          </label>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
              Status
              <select className={fieldClass} {...form.register('status')}>
                {TASK_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {STATUS_LABELS[status]}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
              Priority
              <select className={fieldClass} {...form.register('priority')}>
                {TASK_PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {PRIORITY_LABELS[priority]}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm font-medium text-slate-700">
              Due date
              <input
                type="date"
                className={fieldClass}
                aria-invalid={errors.dueDate ? true : undefined}
                {...form.register('dueDate')}
              />
              {errors.dueDate && (
                <span className="text-sm font-normal text-red-700">{errors.dueDate.message}</span>
              )}
            </label>
          </div>
          {serverError && (
            <p role="alert" className="text-sm text-red-700">
              {serverError}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-indigo-600"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={form.formState.isSubmitting}
              className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-indigo-600"
            >
              {isEdit ? 'Save changes' : 'Create task'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
