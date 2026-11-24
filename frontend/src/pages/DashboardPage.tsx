import { useState } from 'react'
import { useParams } from 'react-router'
import type { Me } from '../features/auth/auth'
import { useLogout } from '../features/auth/auth'
import ProjectSidebar from '../features/projects/components/ProjectSidebar'
import { useProjects } from '../features/projects/hooks'
import TasksView from '../features/tasks/components/TasksView'

export default function DashboardPage({ me }: { me: Me }) {
  const { projectId } = useParams()
  const logout = useLogout()
  const projects = useProjects(me.id)
  const [menuOpen, setMenuOpen] = useState(false)

  const title = projectId
    ? (projects.data?.find((project) => project.id === projectId)?.name ?? 'Project')
    : 'All tasks'

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3">
        <div className="flex items-center gap-3">
          <button
            type="button"
            aria-expanded={menuOpen}
            aria-controls="project-nav"
            onClick={() => setMenuOpen((open) => !open)}
            className="rounded-md px-2 py-1 text-sm font-medium text-slate-700 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600 md:hidden"
          >
            Projects
          </button>
          <h1 className="text-xl font-semibold tracking-tight text-slate-900">TaskFlow</h1>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden text-sm text-slate-600 sm:inline">{me.displayName}</span>
          <button
            type="button"
            onClick={() => logout.mutate()}
            disabled={logout.isPending}
            className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600 disabled:opacity-50"
          >
            Log out
          </button>
        </div>
      </header>

      <div className="mx-auto flex max-w-6xl gap-6 p-4 md:p-6">
        <aside
          id="project-nav"
          className={
            menuOpen
              ? 'fixed inset-y-0 left-0 z-40 w-72 overflow-y-auto bg-white p-4 shadow-xl md:static md:block md:w-56 md:overflow-visible md:bg-transparent md:p-0 md:shadow-none'
              : 'hidden md:block md:w-56'
          }
        >
          <ProjectSidebar userId={me.id} onNavigate={() => setMenuOpen(false)} />
        </aside>

        <main className="flex min-w-0 flex-1">
          <TasksView key={projectId ?? 'all'} userId={me.id} projectId={projectId} title={title} />
        </main>
      </div>
    </div>
  )
}
