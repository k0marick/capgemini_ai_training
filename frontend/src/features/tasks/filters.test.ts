import { describe, expect, it } from 'vitest'
import {
  buildTaskQuery,
  DEFAULT_FILTERS,
  hasActiveFilters,
  parseFilters,
  toSearchParams,
} from './filters'

describe('parseFilters', () => {
  it('returns defaults for an empty URL', () => {
    expect(parseFilters(new URLSearchParams())).toEqual(DEFAULT_FILTERS)
  })

  it('falls back to defaults for unknown enum values', () => {
    const parsed = parseFilters(new URLSearchParams('status=NOPE&priority=URGENT&due=LATER'))
    expect(parsed).toEqual(DEFAULT_FILTERS)
  })

  it('caps the search term at 100 characters', () => {
    const parsed = parseFilters(new URLSearchParams({ q: 'x'.repeat(150) }))
    expect(parsed.q).toHaveLength(100)
  })
})

describe('toSearchParams', () => {
  it('omits default values so URLs stay short', () => {
    expect(toSearchParams(DEFAULT_FILTERS).toString()).toBe('')
  })

  it('round-trips non-default filters', () => {
    const filters = {
      q: 'milk',
      status: 'DONE' as const,
      priority: 'HIGH' as const,
      due: 'OVERDUE' as const,
    }
    expect(parseFilters(toSearchParams(filters))).toEqual(filters)
  })
})

describe('hasActiveFilters', () => {
  it('is false for defaults and true when any filter is set', () => {
    expect(hasActiveFilters(DEFAULT_FILTERS)).toBe(false)
    expect(hasActiveFilters({ ...DEFAULT_FILTERS, due: 'TODAY' })).toBe(true)
  })
})

describe('buildTaskQuery', () => {
  it('adds project, paging and the default sort', () => {
    const query = buildTaskQuery(DEFAULT_FILTERS, 'p-1', 2)
    expect(query.get('projectId')).toBe('p-1')
    expect(query.get('page')).toBe('2')
    expect(query.get('size')).toBe('20')
    expect(query.get('sort')).toBe('dueDate,asc')
  })

  it('omits projectId for the all-tasks view', () => {
    expect(buildTaskQuery(DEFAULT_FILTERS, undefined, 0).has('projectId')).toBe(false)
  })
})
