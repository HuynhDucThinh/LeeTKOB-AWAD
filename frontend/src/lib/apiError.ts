import axios from 'axios'
import type { ProblemDetail } from '@/types/problem'

function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    typeof (value as Record<string, unknown>).status === 'number' &&
    'detail' in value &&
    typeof (value as Record<string, unknown>).detail === 'string'
  )
}

export function getProblemDetail(error: unknown): ProblemDetail {
  if (axios.isAxiosError<ProblemDetail>(error)) {
    if (!error.response || error.code === 'ERR_NETWORK') {
      return {
        type: 'https://leetkob.example/problems/internal',
        title: 'Network unavailable',
        status: 0,
        detail: 'Cannot connect to server. Please ensure the backend is running.',
      }
    }
    if (isProblemDetail(error.response.data)) {
      return error.response.data
    }
    return {
      type: 'https://leetkob.example/problems/internal',
      title: error.response.statusText || 'Request failed',
      status: error.response.status,
      detail: 'The request could not be completed.',
    }
  }

  if (isProblemDetail(error)) {
    return error
  }

  return {
    type: 'https://leetkob.example/problems/internal',
    title: 'Unexpected error',
    status: 500,
    detail: error instanceof Error ? error.message : 'The request could not be completed.',
  }
}

export const getProblemError = getProblemDetail

export function getApiErrorMessage(error: unknown): string {
  const problem = getProblemDetail(error)
  if (problem.errors && problem.errors.length > 0) {
    return problem.errors
      .map((item) => {
        const target = item.pointer ?? item.parameter
        return target ? `${target}: ${item.detail}` : item.detail
      })
      .join('; ')
  }
  return problem.detail
}
