import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { DEFAULT_FILTERS } from '../filters'
import TaskEmptyState from './TaskEmptyState'
import TaskFormDialog from './TaskFormDialog'
import TaskToolbar from './TaskToolbar'

function withQueryClient(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>
}

describe('TaskEmptyState', () => {
  it('offers to add the first task when the project is empty', async () => {
    const onCreate = vi.fn()
    render(<TaskEmptyState variant="no-tasks" onCreate={onCreate} />)
    expect(screen.getByText('No tasks yet')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Add the first task' }))
    expect(onCreate).toHaveBeenCalledOnce()
  })

  it('explains how to open a project when there is no create action', () => {
    render(<TaskEmptyState variant="no-tasks" />)
    expect(screen.getByText(/Open a project in the sidebar/)).toBeInTheDocument()
  })

  it('offers a reset when filters match nothing', async () => {
    const onReset = vi.fn()
    render(<TaskEmptyState variant="no-results" onReset={onReset} />)
    expect(screen.getByText('No tasks match your filters')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Reset filters' }))
    expect(onReset).toHaveBeenCalledOnce()
  })
})

describe('TaskFormDialog validation', () => {
  it('shows field errors and does not call the API when the title is blank', async () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)

    render(
      withQueryClient(<TaskFormDialog userId="u-1" projectId="p-1" onClose={() => undefined} />),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('closes on Escape', async () => {
    const onClose = vi.fn()
    render(withQueryClient(<TaskFormDialog userId="u-1" projectId="p-1" onClose={onClose} />))
    await userEvent.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalled()
  })
})

describe('TaskToolbar', () => {
  it('debounces search input before reporting the change', async () => {
    const onChange = vi.fn()
    render(<TaskToolbar filters={DEFAULT_FILTERS} onChange={onChange} />)

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search' }), 'milk')
    expect(onChange).not.toHaveBeenCalled()

    await waitFor(() => expect(onChange).toHaveBeenCalledWith({ q: 'milk' }), { timeout: 1000 })
    expect(onChange).toHaveBeenCalledTimes(1)
  })

  it('resets every filter from the reset button', async () => {
    const onChange = vi.fn()
    render(
      <TaskToolbar
        filters={{ ...DEFAULT_FILTERS, status: 'DONE', due: 'OVERDUE' }}
        onChange={onChange}
      />,
    )
    await act(async () => {
      await userEvent.click(screen.getByRole('button', { name: 'Reset' }))
    })
    expect(onChange).toHaveBeenCalledWith({ q: '', status: '', priority: '', due: 'ALL' })
  })
})
