import { z } from 'zod'
import { TASK_PRIORITIES, TASK_STATUSES, type TaskRequest } from './types'

// Same limits as the backend TaskCreateRequest / TaskUpdateRequest.
export const taskFormSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, 'Title is required')
    .max(255, 'Title must be 255 characters or fewer'),
  description: z.string().max(5000, 'Description must be 5000 characters or fewer'),
  status: z.enum(TASK_STATUSES),
  priority: z.enum(TASK_PRIORITIES),
  dueDate: z
    .string()
    .refine(
      (value) =>
        value === '' || (/^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value))),
      {
        message: 'Use a date in YYYY-MM-DD format',
      },
    ),
})

export type TaskFormValues = z.infer<typeof taskFormSchema>

export function toTaskRequest(values: TaskFormValues): TaskRequest {
  return {
    title: values.title.trim(),
    description: values.description.trim() === '' ? null : values.description.trim(),
    status: values.status,
    priority: values.priority,
    dueDate: values.dueDate === '' ? null : values.dueDate,
  }
}
