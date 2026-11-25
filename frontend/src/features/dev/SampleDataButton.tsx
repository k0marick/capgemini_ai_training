import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../lib/api-client'

/** Dev-only: seeds sample lists and tasks via POST /api/v1/dev/sample-data. Renders nothing in production builds. */
export default function SampleDataButton() {
  const queryClient = useQueryClient()
  const seed = useMutation({
    mutationFn: () => apiFetch('/api/v1/dev/sample-data', { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries(),
  })

  if (!import.meta.env.DEV) return null

  return (
    <button
      type="button"
      onClick={() => seed.mutate()}
      disabled={seed.isPending}
      className="mt-2 w-full rounded-md border border-dashed border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-indigo-600 disabled:opacity-50"
    >
      {seed.isPending ? 'Loading sample data…' : 'Load sample data (dev)'}
    </button>
  )
}
