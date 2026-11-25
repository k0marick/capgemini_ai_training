import { useState } from 'react'
import { Link } from 'react-router'
import { useNotifications } from '../context'
import { NOTIFICATION_LABELS, NOTIFICATION_TYPES, type NotificationType } from '../types'

export default function NotificationBell() {
  const { notifications, enabledTypes, setEnabledTypes } = useNotifications()
  const [open, setOpen] = useState(false)
  const count = notifications.length

  const toggleType = (type: NotificationType) => {
    const next = enabledTypes.includes(type)
      ? enabledTypes.filter((value) => value !== type)
      : NOTIFICATION_TYPES.filter((value) => value === type || enabledTypes.includes(value))
    setEnabledTypes(next)
  }

  return (
    <div className="relative">
      <button
        type="button"
        aria-expanded={open}
        aria-controls="notification-panel"
        aria-label={count === 0 ? 'Notifications' : `Notifications, ${count} need attention`}
        onClick={() => setOpen((value) => !value)}
        className="relative rounded-md px-2 py-1 text-sm font-medium text-slate-700 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600"
      >
        Alerts
        {count > 0 && (
          <span className="ml-1 rounded-full bg-indigo-600 px-1.5 text-xs font-semibold text-white">
            {count}
          </span>
        )}
      </button>

      {open && (
        <div
          id="notification-panel"
          className="absolute right-0 z-40 mt-2 w-80 max-w-[calc(100vw-2rem)] rounded-md border border-slate-200 bg-white p-3 shadow-xl"
        >
          <fieldset className="mb-3 flex flex-wrap gap-3 text-sm text-slate-700">
            <legend className="mb-1 text-xs font-medium text-slate-600">Show</legend>
            {NOTIFICATION_TYPES.map((type) => (
              <label key={type} className="flex items-center gap-1.5">
                <input
                  type="checkbox"
                  checked={enabledTypes.includes(type)}
                  onChange={() => toggleType(type)}
                  className="h-4 w-4 accent-indigo-600"
                />
                {NOTIFICATION_LABELS[type]}
              </label>
            ))}
          </fieldset>

          {count === 0 ? (
            <p className="text-sm text-slate-600">Nothing overdue or due in the next 3 days.</p>
          ) : (
            <ul className="flex max-h-80 flex-col gap-2 overflow-y-auto">
              {notifications.map((item) => (
                <li
                  key={`${item.type}:${item.taskId}`}
                  className="rounded-md border border-slate-200 px-3 py-2 text-sm"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span
                      className={
                        item.type === 'OVERDUE'
                          ? 'text-xs font-semibold text-red-600'
                          : 'text-xs font-semibold text-amber-700'
                      }
                    >
                      {NOTIFICATION_LABELS[item.type]}
                    </span>
                    <span className="text-xs text-slate-500">{item.dueDate}</span>
                  </div>
                  <Link
                    to={`/projects/${item.projectId}`}
                    className="mt-1 block break-words font-medium text-slate-900 hover:underline"
                  >
                    {item.title}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
