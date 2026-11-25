import { describe, expect, it } from 'vitest'
import { taskFormSchema, toTaskRequest } from './schemas'

const valid = {
  title: '  Ship P4  ',
  description: '',
  status: 'TODO' as const,
  priority: 'HIGH' as const,
  dueDate: '',
}

describe('taskFormSchema', () => {
  it('accepts a minimal valid task and trims the title', () => {
    const result = taskFormSchema.safeParse(valid)
    expect(result.success).toBe(true)
  })

  it('rejects a blank title', () => {
    const result = taskFormSchema.safeParse({ ...valid, title: '   ' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.message).toBe('Title is required')
  })

  it('rejects a title longer than 255 characters', () => {
    const result = taskFormSchema.safeParse({ ...valid, title: 'x'.repeat(256) })
    expect(result.success).toBe(false)
  })

  it('rejects a due date that is not YYYY-MM-DD', () => {
    expect(taskFormSchema.safeParse({ ...valid, dueDate: '01/12/2026' }).success).toBe(false)
    expect(taskFormSchema.safeParse({ ...valid, dueDate: '2026-02-31x' }).success).toBe(false)
  })

  it('accepts an empty due date and a valid ISO date', () => {
    expect(taskFormSchema.safeParse({ ...valid, dueDate: '' }).success).toBe(true)
    expect(taskFormSchema.safeParse({ ...valid, dueDate: '2026-12-01' }).success).toBe(true)
  })
})

describe('toTaskRequest', () => {
  it('maps empty optional fields to null', () => {
    const parsed = taskFormSchema.parse(valid)
    expect(toTaskRequest(parsed)).toEqual({
      title: 'Ship P4',
      description: null,
      status: 'TODO',
      priority: 'HIGH',
      dueDate: null,
    })
  })
})
