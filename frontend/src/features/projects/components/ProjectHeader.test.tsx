import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { Project } from '../types'
import ProjectHeader from './ProjectHeader'

const project = (taskCount: number): Project => ({
  id: 'p-1',
  name: 'Home',
  taskCount,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
})

function renderHeader(taskCount: number) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProjectHeader userId="u-1" project={project(taskCount)} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProjectHeader', () => {
  it('disables project delete and explains why while tasks exist', () => {
    renderHeader(2)
    expect(screen.getByRole('button', { name: 'Delete project' })).toBeDisabled()
    expect(screen.getByText(/Delete all 2 tasks before deleting/)).toBeInTheDocument()
  })

  it('enables project delete when the project is empty', () => {
    renderHeader(0)
    expect(screen.getByRole('button', { name: 'Delete project' })).toBeEnabled()
    expect(screen.queryByText(/before deleting/)).not.toBeInTheDocument()
  })

  it('offers rename for any project', () => {
    renderHeader(3)
    expect(screen.getByRole('button', { name: 'Rename' })).toBeEnabled()
  })
})
