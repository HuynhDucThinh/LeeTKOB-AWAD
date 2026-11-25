import { API_ENDPOINTS } from '@/config/constants'
import type { ReviewItem, ReviewQueuePageResponse, TaDecision } from '@/types/review'
import { apiClient } from './api'

export const reviewService = {
  async getQueue(labId: number): Promise<ReviewItem[]> {
    const response = await apiClient.get<ReviewQueuePageResponse>(
      API_ENDPOINTS.labs.reviewQueue(labId),
    )
    return response.data.items
  },

  async decide(
    labId: number,
    submissionId: number,
    constraintId: string,
    decision: TaDecision,
  ): Promise<ReviewItem> {
    const response = await apiClient.put<ReviewItem>(
      API_ENDPOINTS.labs.reviewDecision(labId, submissionId, constraintId),
      { decision },
    )
    return response.data
  },
}
