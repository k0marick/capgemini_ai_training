type Props =
  { variant: 'no-tasks'; onCreate?: () => void } | { variant: 'no-results'; onReset: () => void }

export default function TaskEmptyState(props: Props) {
  if (props.variant === 'no-results') {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 p-8 text-center">
        <p className="font-medium text-slate-900">No tasks match your filters</p>
        <button
          type="button"
          onClick={props.onReset}
          className="mt-3 rounded-md px-3 py-1.5 text-sm font-medium text-indigo-700 hover:bg-indigo-50 focus-visible:outline-2 focus-visible:outline-indigo-600"
        >
          Reset filters
        </button>
      </div>
    )
  }
  return (
    <div className="rounded-lg border border-dashed border-slate-300 p-8 text-center">
      <p className="font-medium text-slate-900">No tasks yet</p>
      {props.onCreate ? (
        <button
          type="button"
          onClick={props.onCreate}
          className="mt-3 rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-indigo-600"
        >
          Add the first task
        </button>
      ) : (
        <p className="mt-1 text-sm text-slate-600">Open a project in the sidebar to add a task.</p>
      )}
    </div>
  )
}
