import type { ConnectionStatus } from '../types'
import { useNotifications } from '../context'

const LABELS: Record<ConnectionStatus, { text: string; dot: string }> = {
  connecting: { text: 'Connecting', dot: 'bg-slate-400' },
  open: { text: 'Live', dot: 'bg-emerald-500' },
  reconnecting: { text: 'Reconnecting', dot: 'bg-amber-500' },
  closed: { text: 'Offline', dot: 'bg-slate-400' },
}

export default function ConnectionIndicator() {
  const { status } = useNotifications()
  const label = LABELS[status]
  return (
    <span
      className="hidden items-center gap-1.5 text-xs text-slate-600 sm:inline-flex"
      title="Notification connection"
    >
      <span aria-hidden="true" className={`h-2 w-2 rounded-full ${label.dot}`} />
      <span data-testid="connection-status">{label.text}</span>
    </span>
  )
}
