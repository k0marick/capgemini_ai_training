import type { Me } from '../features/auth/auth'
import { useLogout } from '../features/auth/auth'

export default function DashboardPage({ me }: { me: Me }) {
  const logout = useLogout()

  return (
    <main className="min-h-screen bg-slate-50 p-6">
      <header className="mx-auto flex max-w-5xl items-center justify-between">
        <h1 className="text-xl font-semibold tracking-tight text-slate-900">TaskFlow</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-600">{me.displayName}</span>
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
    </main>
  )
}
