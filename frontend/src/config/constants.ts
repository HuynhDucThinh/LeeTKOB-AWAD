export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/v1'
export const AUTH_API_BASE_URL = import.meta.env.VITE_AUTH_API_BASE_URL ?? '/api/auth'
export const USE_MOCK_API = import.meta.env.VITE_USE_MOCK !== 'false'

export const API_ENDPOINTS = {
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    refresh: '/auth/refresh-token',
  },
  version: '/version',
  classes: '/classes',
  admin: {
    classes: '/admin/classes',
    classMembers: (classId: number) => `/admin/classes/${classId}/members`,
    users: '/admin/users',
  },
  labs: {
    list: '/labs',
    detail: (labId: number) => `/labs/${labId}`,
    submissions: (labId: number) => `/labs/${labId}/submissions`,
    reviewQueue: (labId: number) => `/labs/${labId}/review-queue`,
    reviewDecision: (labId: number, submissionId: number, constraintId: string) =>
      `/labs/${labId}/review-queue/${submissionId}/${constraintId}`,
    publish: (labId: number) => `/labs/${labId}/publish`,
    appeals: (labId: number) => `/labs/${labId}/appeals`,
  },
  submissions: {
    detail: (submissionId: number) => `/submissions/${submissionId}`,
    events: (submissionId: number) => `${API_BASE_URL}/submissions/${submissionId}/events`,
    appeals: (submissionId: number) => `/submissions/${submissionId}/appeals`,
  },
  appeals: {
    decision: (appealId: number) => `/appeals/${appealId}/decision`,
  },
} as const

export const submissionEventsUrl = (submissionId: number): string =>
  API_ENDPOINTS.submissions.events(submissionId)
