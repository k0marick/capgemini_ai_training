import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { projectsKey } from '../projects/hooks'
import { buildTaskQuery, type TaskFilters } from './filters'
import { changeTaskStatus, createTask, deleteTask, listTasks, updateTask } from './api'
import type { Page, Task, TaskRequest, TaskStatus } from './types'

/** All task caches of one user live under this prefix, so a logout or user switch never leaks data. */
export const tasksKey = (userId: string) => ['tasks', userId] as const

export function useTasks(
  userId: string,
  filters: TaskFilters,
  projectId: string | undefined,
  page: number,
) {
  const query = buildTaskQuery(filters, projectId, page)
  return useQuery({
    queryKey: [...tasksKey(userId), query.toString()],
    queryFn: () => listTasks(query),
    placeholderData: keepPreviousData,
  })
}

export function useCreateTask(userId: string, projectId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: TaskRequest) => createTask(projectId, body),
    onSettled: () => queryClient.invalidateQueries({ queryKey: tasksKey(userId) }),
  })
}

export function useUpdateTask(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ task, body }: { task: Task; body: TaskRequest }) =>
      updateTask(task.id, body, task.version),
    onSettled: () => queryClient.invalidateQueries({ queryKey: tasksKey(userId) }),
  })
}

/** Deletes a task; also refreshes project task counts. */
export function useDeleteTask(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (task: Task) => deleteTask(task.id),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: tasksKey(userId) })
      void queryClient.invalidateQueries({ queryKey: projectsKey(userId) })
    },
  })
}

/** Optimistic status change. Rolls back on failure; a 409 triggers a refetch so the list shows the server state. */
export function useChangeTaskStatus(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ task, status }: { task: Task; status: TaskStatus }) =>
      changeTaskStatus(task.id, status, task.version),
    onMutate: async ({ task, status }) => {
      await queryClient.cancelQueries({ queryKey: tasksKey(userId) })
      const snapshot = queryClient.getQueriesData<Page<Task>>({ queryKey: tasksKey(userId) })
      queryClient.setQueriesData<Page<Task>>(
        { queryKey: tasksKey(userId) },
        (page) =>
          page && {
            ...page,
            content: page.content.map((item) => (item.id === task.id ? { ...item, status } : item)),
          },
      )
      return { snapshot }
    },
    onError: (_error, _variables, context) => {
      context?.snapshot.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: tasksKey(userId) }),
  })
}
