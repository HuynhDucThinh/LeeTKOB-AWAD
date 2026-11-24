import { API_ENDPOINTS } from '@/config/constants'
import type { ProblemDetail } from '@/types/problem'
import type { PublicationReceipt, PublishApiResponse } from '@/types/publish'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const publishService = {
  async publish(labId: number): Promise<PublishApiResponse> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<PublicationReceipt>(API_ENDPOINTS.labs.publish(labId), {})
    // return {
    //   receipt: response.data,
    //   replayed: response.headers['idempotent-replayed'] === 'true',
    // }

    await wait(300)
    const lab = mockStore.labs.find((item) => item.id === labId)
    if (!lab) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'The requested resource was not found.',
        instance: `/v1/labs/${labId}/publish`,
      }
      throw problem
    }
    if (mockStore.reviewItems.some((item) => item.labId === labId && item.decision === null)) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/reviews-pending',
        title: 'Reviews pending',
        status: 409,
        detail: 'Required TA reviews are not complete.',
        instance: `/v1/labs/${labId}/publish`,
      }
      throw problem
    }

    const replayed = lab.publicationReceipt !== null
    for (const submission of mockStore.submissions.filter(
      (item) => item.labId === labId && item.isCounted,
    )) {
      const violations = mockStore.reviewItems.filter(
        (item) => item.submissionId === submission.id && item.decision === 'violation',
      )
      submission.publishedDeductions = violations.map((item) => ({
        constraintId: item.constraintId,
        description: item.constraintDescription,
        deductionTenths: item.deductionTenths,
        evidence: item.evidence,
      }))
      const totalDeduction = violations.reduce((sum, item) => sum + item.deductionTenths, 0)
      submission.publishedScoreTenths = Math.max(0, submission.scoreTenths - totalDeduction)
    }

    const receipt: PublicationReceipt = lab.publicationReceipt ?? {
      labId,
      status: 'published',
      publishedAt: new Date().toISOString(),
      publishedBy: 'ta-own',
      submissionCount: lab.submissionCount,
    }
    lab.isPublished = true
    lab.publishedAt = receipt.publishedAt
    lab.publicationReceipt = receipt
    lab.unreviewedCount = 0
    return { receipt: structuredClone(receipt), replayed }
  },
}
