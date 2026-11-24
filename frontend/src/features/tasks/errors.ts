import { ApiError } from '../../lib/api-client'

/** User-facing message for a failed task mutation. */
export function describeTaskError(error: unknown): string {
  if (error instanceof ApiError && error.status === 409) {
    return 'This task was changed elsewhere. The list has been refreshed; review it and try again.'
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'This task no longer exists.'
  }
  return 'Something went wrong. Please try again.'
}
