import { API_ENDPOINTS } from '@/config/constants'
import type { AppealItem, AppealStatus } from '@/types/appeal'
import { apiClient } from './api'

export const appealService = {
  async list(labId: number, _studentId?: string): Promise<AppealItem[]> {
    const response = await apiClient.get<AppealItem[]>(API_ENDPOINTS.labs.appeals(labId))
    return response.data
  },

  async decide(
    appealId: number,
    status: Exclude<AppealStatus, 'pending'>,
    decisionReason: string,
  ): Promise<AppealItem> {
    const response = await apiClient.put<AppealItem>(API_ENDPOINTS.appeals.decision(appealId), {
      status,
      decisionReason,
    })
    return response.data
  },

  async create(
    submissionId: number,
    _studentId: string,
    constraintId: string,
    reason: string,
  ): Promise<AppealItem> {
    const response = await apiClient.post<AppealItem>(
      API_ENDPOINTS.submissions.appeals(submissionId),
      { constraintId, reason },
    )
    return response.data
  },
}
