import { useEffect, type ReactNode } from 'react'
import { useNotifications } from '../context'
import { NOTIFICATION_LABELS } from '../types'

const TOAST_MS = 6_000

/** Polite live region for new notifications. Only items new since the previous batch produce toasts. */
export default function ToastRegion() {
  const { toasts, dismissToast } = useNotifications()
  return (
    <div
      aria-live="polite"
      className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2"
    >
      {toasts.map((toast) => (
        <ToastItem key={toast.id} id={toast.id} onDismiss={dismissToast}>
          <span className="font-medium">{NOTIFICATION_LABELS[toast.item.type]}:</span>{' '}
          {toast.item.title}
        </ToastItem>
      ))}
    </div>
  )
}

function ToastItem({
  id,
  children,
  onDismiss,
}: {
  id: number
  children: ReactNode
  onDismiss: (id: number) => void
}) {
  useEffect(() => {
    const timer = setTimeout(() => onDismiss(id), TOAST_MS)
    return () => clearTimeout(timer)
  }, [id, onDismiss])
  return (
    <div className="pointer-events-auto flex items-start justify-between gap-3 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 shadow-lg">
      <p className="min-w-0 break-words">{children}</p>
      <button
        type="button"
        onClick={() => onDismiss(id)}
        aria-label="Dismiss notification"
        className="shrink-0 rounded px-1 text-slate-500 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600"
      >
        ×
      </button>
    </div>
  )
}
