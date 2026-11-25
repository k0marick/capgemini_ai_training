import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../../lib/api-client'

export type Me = {
  id: string
  provider: 'GOOGLE' | 'GITHUB'
  email: string | null
  displayName: string
  avatarUrl: string | null
  timezone: string
}

export const meQueryKey = ['me'] as const

/** Resolves to the current user, or null when the session is missing or expired. */
export function useMe() {
  return useQuery<Me | null>({
    queryKey: meQueryKey,
    queryFn: async () => {
      try {
        return await apiFetch<Me>('/api/v1/me')
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) return null
        throw err
      }
    },
    retry: false,
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<void>('/api/v1/logout', { method: 'POST' }),
    onSettled: () => {
      // Drop all private cached data so the next user never sees it.
      // Mark the session as signed out (keeps the observer attached, so the app redirects to /login),
      // then drop all other cached data so the next user never sees it.
      queryClient.setQueryData(meQueryKey, null)
      queryClient.removeQueries({
        predicate: (query) => JSON.stringify(query.queryKey) !== JSON.stringify(meQueryKey),
      })
    },
  })
}

export function loginUrl(provider: 'google' | 'github'): string {
  return `/oauth2/authorization/${provider}`
}
