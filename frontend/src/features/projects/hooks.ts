import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../lib/api-client'
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
