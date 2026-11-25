import { apiFetch } from '../../lib/api-client'
import type { Page, Task, TaskRequest, TaskStatus } from './types'

export function listTasks(query: URLSearchParams): Promise<Page<Task>> {
  return apiFetch<Page<Task>>(`/api/v1/tasks?${query.toString()}`)
}

export function createTask(projectId: string, body: TaskRequest): Promise<Task> {
  return apiFetch<Task>(`/api/v1/projects/${projectId}/tasks`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function updateTask(id: string, body: TaskRequest, version: number): Promise<Task> {
  return apiFetch<Task>(`/api/v1/tasks/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ ...body, version }),
  })
}

export function changeTaskStatus(id: string, status: TaskStatus, version: number): Promise<Task> {
  return apiFetch<Task>(`/api/v1/tasks/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, version }),
  })
}

export function deleteTask(id: string): Promise<void> {
  return apiFetch<void>(`/api/v1/tasks/${id}`, { method: 'DELETE' })
}
