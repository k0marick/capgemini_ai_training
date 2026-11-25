import { z } from 'zod'

// Same limits as the backend ProjectRequest.
export const projectFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Name is required')
    .max(150, 'Name must be 150 characters or fewer'),
})

export type ProjectFormValues = z.infer<typeof projectFormSchema>
