import { loginUrl } from '../features/auth/auth'

const buttonClass =
  'flex w-full items-center justify-center rounded-lg border border-slate-300 bg-white px-4 py-2.5 ' +
  'text-sm font-medium text-slate-800 shadow-sm transition hover:bg-slate-50 ' +
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600'

export default function LoginPage() {
  const params = new URLSearchParams(window.location.search)
  const failed = params.get('error') === 'oauth_failed'

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-sm space-y-6 rounded-2xl bg-white p-8 shadow-md">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">TaskFlow</h1>
          <p className="text-sm text-slate-500">Sign in to manage your tasks</p>
        </div>
        {failed && (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            Sign-in failed. Please try again.
          </p>
        )}
        <div className="space-y-3">
          <a className={buttonClass} href={loginUrl('google')}>
            Continue with Google
          </a>
          <a className={buttonClass} href={loginUrl('github')}>
            Continue with GitHub
          </a>
        </div>
      </div>
    </main>
  )
}
