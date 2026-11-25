import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { ApiError } from '../../../lib/api-client'
import { useDeleteProject, useRenameProject } from '../hooks'
import { projectFormSchema, type ProjectFormValues } from '../schemas'
import type { Project } from '../types'

type Props = {
  userId: string
  project: Project
}

const buttonClass =
  'rounded-md px-3 py-1.5 text-sm font-medium focus-visible:outline-2 focus-visible:outline-indigo-600 disabled:opacity-50'

function describeProjectError(error: unknown): string {
  if (error instanceof ApiError && error.status === 409) {
    return 'Delete all tasks in this project first.'
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'This project no longer exists.'
  }
  return 'Something went wrong. Please try again.'
}

/** Project title with project-level actions: rename inline, delete only when it has no tasks. */
export default function ProjectHeader({ userId, project }: Props) {
  const navigate = useNavigate()
  const [renaming, setRenaming] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const renameProject = useRenameProject(userId)
  const deleteProject = useDeleteProject(userId)
  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    defaultValues: { name: project.name },
  })
  const hasTasks = project.taskCount > 0

  const startRename = () => {
    form.reset({ name: project.name })
    setServerError(null)
    setRenaming(true)
  }

  const onRename = form.handleSubmit(async ({ name }) => {
    setServerError(null)
    try {
      await renameProject.mutateAsync({ id: project.id, name })
      setRenaming(false)
    } catch (error) {
      setServerError(describeProjectError(error))
    }
  })

  const onDelete = async () => {
    if (!window.confirm(`Delete project "${project.name}"?`)) return
    setServerError(null)
    try {
      await deleteProject.mutateAsync(project.id)
      void navigate('/', { replace: true })
    } catch (error) {
      setServerError(describeProjectError(error))
    }
  }

  if (renaming) {
    return (
      <form noValidate onSubmit={onRename} className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <label className="sr-only" htmlFor="project-rename">
            Project name
          </label>
          <input
            id="project-rename"
            autoFocus
            className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-1.5 text-xl font-semibold text-slate-900 focus-visible:outline-2 focus-visible:outline-indigo-600"
            aria-invalid={form.formState.errors.name ? true : undefined}
            {...form.register('name')}
          />
          <button
            type="submit"
            disabled={renameProject.isPending}
            className={`${buttonClass} bg-indigo-600 text-white hover:bg-indigo-700`}
          >
            Save
          </button>
          <button
            type="button"
            onClick={() => {
              setRenaming(false)
              setServerError(null)
            }}
            className={`${buttonClass} text-slate-700 hover:bg-slate-200`}
          >
            Cancel
          </button>
        </div>
        {form.formState.errors.name && (
          <p role="alert" className="text-sm text-red-700">
            {form.formState.errors.name.message}
          </p>
        )}
        {serverError && (
          <p role="alert" className="text-sm text-red-700">
            {serverError}
          </p>
        )}
      </form>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2
          id="tasks-heading"
          className="min-w-0 truncate text-xl font-semibold tracking-tight text-slate-900"
        >
          {project.name}
        </h2>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={startRename}
            className={`${buttonClass} text-slate-700 hover:bg-slate-200`}
          >
            Rename
          </button>
          <button
            type="button"
            onClick={() => void onDelete()}
            disabled={hasTasks || deleteProject.isPending}
            aria-describedby={hasTasks ? 'project-delete-hint' : undefined}
            className={`${buttonClass} text-red-700 hover:bg-red-50`}
          >
            Delete project
          </button>
        </div>
      </div>
      {hasTasks && (
        <p id="project-delete-hint" className="text-xs text-slate-500">
          Delete all {project.taskCount} task{project.taskCount === 1 ? '' : 's'} before deleting
          this project.
        </p>
      )}
      {serverError && (
        <p role="alert" className="text-sm text-red-700">
          {serverError}
        </p>
      )}
    </div>
  )
}
