import { zodResolver } from '@hookform/resolvers/zod'
import { NavLink } from 'react-router'
import { useForm } from 'react-hook-form'
import { useCreateProject, useProjects } from '../hooks'
import { projectFormSchema, type ProjectFormValues } from '../schemas'
import SampleDataButton from '../../dev/SampleDataButton'

type Props = {
  userId: string
  onNavigate: () => void
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `block truncate rounded-md px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-indigo-600 ${
    isActive ? 'bg-indigo-50 font-medium text-indigo-800' : 'text-slate-700 hover:bg-slate-100'
  }`

export default function ProjectSidebar({ userId, onNavigate }: Props) {
  const projects = useProjects(userId)
  const createProject = useCreateProject(userId)
  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    defaultValues: { name: '' },
  })

  const onSubmit = form.handleSubmit(async ({ name }) => {
    try {
      await createProject.mutateAsync(name)
      form.reset()
    } catch {
      // Shown through createProject.isError.
    }
  })

  return (
    <nav aria-label="Projects" className="flex flex-col gap-4">
      <ul className="flex flex-col gap-1">
        <li>
          <NavLink to="/" end className={linkClass} onClick={onNavigate}>
            All tasks
          </NavLink>
        </li>
      </ul>

      <div>
        <h2 className="px-3 text-xs font-semibold uppercase tracking-wide text-slate-500">
          Projects
        </h2>
        {projects.isPending && (
          <p className="px-3 py-2 text-sm text-slate-500">Loading projects…</p>
        )}
        {projects.isError && (
          <p role="alert" className="px-3 py-2 text-sm text-red-700">
            Projects could not be loaded.
          </p>
        )}
        {projects.data && (
          <ul className="mt-1 flex flex-col gap-1">
            {projects.data.map((project) => (
              <li key={project.id}>
                <NavLink to={`/projects/${project.id}`} className={linkClass} onClick={onNavigate}>
                  <span>{project.name}</span>
                  <span className="ml-2 text-xs text-slate-400">{project.taskCount}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        )}
      </div>

      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-2">
        <label className="flex flex-col gap-1 text-xs font-medium text-slate-600">
          New project
          <input
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm focus-visible:outline-2 focus-visible:outline-indigo-600"
            placeholder="Project name"
            aria-invalid={form.formState.errors.name ? true : undefined}
            {...form.register('name')}
          />
        </label>
        {form.formState.errors.name && (
          <p className="text-xs text-red-700">{form.formState.errors.name.message}</p>
        )}
        {createProject.isError && (
          <p className="text-xs text-red-700">Project could not be created.</p>
        )}
        <button
          type="submit"
          disabled={createProject.isPending}
          className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-indigo-600"
        >
          Add project
        </button>
      </form>
      <SampleDataButton />
    </nav>
  )
}
