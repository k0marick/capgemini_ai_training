import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { useMe } from './features/auth/auth'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: false } },
})

function Home() {
  const me = useMe()
  if (me.isPending) {
    return (
      <main className="flex min-h-screen items-center justify-center" aria-busy="true">
        <div className="h-8 w-8 animate-pulse rounded-full bg-slate-300" />
      </main>
    )
  }
  if (!me.data) return <Navigate to="/login" replace />
  return <DashboardPage me={me.data} />
}

function Login() {
  const me = useMe()
  if (me.data) return <Navigate to="/" replace />
  return <LoginPage />
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/projects/:projectId" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
