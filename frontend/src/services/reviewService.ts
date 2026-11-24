import { API_ENDPOINTS } from '@/config/constants'
import type { ProblemDetail } from '@/types/problem'
import type { ReviewItem, ReviewQueuePageResponse, TaDecision } from '@/types/review'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const reviewService = {
  async getQueue(labId: number): Promise<ReviewItem[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<ReviewQueuePageResponse>(API_ENDPOINTS.labs.reviewQueue(labId))
    // return response.data.items
    void (null as ReviewQueuePageResponse | null)

    await wait()
    return structuredClone(mockStore.reviewItems.filter((item) => item.labId === labId))
  },

  async decide(
    labId: number,
    submissionId: number,
    constraintId: string,
    decision: TaDecision,
  ): Promise<ReviewItem> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.put<ReviewItem>(
    //   API_ENDPOINTS.labs.reviewDecision(labId, submissionId, constraintId),
    //   { decision },
    // )
    // return response.data

    await wait(120)
    const item = mockStore.reviewItems.find(
      (entry) =>
        entry.labId === labId &&
        entry.submissionId === submissionId &&
        entry.constraintId === constraintId,
    )
    if (!item) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'Review item was not found.',
        instance: `/v1/labs/${labId}/review-queue/${submissionId}/${constraintId}`,
      }
      throw problem
    }
    item.decision = decision
    item.decidedBy = 'ta-own'
    item.decidedAt = new Date().toISOString()
    const lab = mockStore.labs.find((entry) => entry.id === labId)
    if (lab) {
      lab.unreviewedCount = mockStore.reviewItems.filter(
        (entry) => entry.labId === labId && entry.decision === null,
      ).length
    }
    return structuredClone(item)
  },
}
