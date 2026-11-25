import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../lib/api-client'
import { tasksKey } from '../tasks/hooks'
import type { Project } from './types'

export const projectsKey = (userId: string) => ['projects', userId] as const

export function useProjects(userId: string) {
  return useQuery({
    queryKey: projectsKey(userId),
    queryFn: () => apiFetch<Project[]>('/api/v1/projects'),
  })
}

export function useCreateProject(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) =>
      apiFetch<Project>('/api/v1/projects', { method: 'POST', body: JSON.stringify({ name }) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectsKey(userId) }),
  })
}

export function useRenameProject(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) =>
      apiFetch<Project>(`/api/v1/projects/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ name }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectsKey(userId) }),
  })
}

/** Deletes a project; the backend refuses (409) while it still has tasks. */
export function useDeleteProject(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => apiFetch<void>(`/api/v1/projects/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey(userId) })
      void queryClient.invalidateQueries({ queryKey: tasksKey(userId) })
    },
  })
}
