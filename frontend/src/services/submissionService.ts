import { API_ENDPOINTS } from '@/config/constants'
import type { SubmissionDetail, SubmissionSummary } from '@/types/submission'
import { apiClient } from './api'

export const submissionService = {
  async list(labId: number, _studentId?: string): Promise<SubmissionSummary[]> {
    const response = await apiClient.get<SubmissionSummary[]>(
      API_ENDPOINTS.labs.submissions(labId),
    )
    return response.data
  },

  async submit(labId: number, _studentId: string, sourceCode: string): Promise<SubmissionSummary> {
    const response = await apiClient.post<SubmissionSummary>(
      API_ENDPOINTS.labs.submissions(labId),
      { sourceCode },
      { headers: { 'Idempotency-Key': crypto.randomUUID() } },
    )
    return response.data
  },

  async get(submissionId: number): Promise<SubmissionDetail> {
    const response = await apiClient.get<SubmissionDetail>(
      API_ENDPOINTS.submissions.detail(submissionId),
    )
    return response.data
  },
}
