import { API_ENDPOINTS } from '@/config/constants'
import type { AppealItem, AppealStatus } from '@/types/appeal'
import type { ProblemDetail } from '@/types/problem'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const appealService = {
  async list(labId: number, studentId?: string): Promise<AppealItem[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<AppealItem[]>(API_ENDPOINTS.labs.appeals(labId))
    // return response.data

    await wait()
    return structuredClone(
      mockStore.appeals.filter(
        (item) => item.labId === labId && (!studentId || item.studentId === studentId),
      ),
    )
  },

  async decide(
    appealId: number,
    status: Exclude<AppealStatus, 'pending'>,
    decisionReason: string,
  ): Promise<AppealItem> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.put<AppealItem>(
    //   API_ENDPOINTS.appeals.decision(appealId),
    //   { status, decisionReason },
    // )
    // return response.data

    await wait()
    const appeal = mockStore.appeals.find((item) => item.id === appealId)
    if (!appeal) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'Appeal was not found.',
        instance: `/v1/appeals/${appealId}/decision`,
      }
      throw problem
    }
    appeal.status = status
    appeal.decisionReason = decisionReason
    appeal.decidedBy = 'ta-own'
    appeal.decidedAt = new Date().toISOString()
    if (status === 'accepted') {
      const submission = mockStore.submissions.find((item) => item.id === appeal.submissionId)
      if (submission?.publishedDeductions) {
        submission.publishedDeductions = submission.publishedDeductions.filter(
          (item) => item.constraintId !== appeal.constraintId,
        )
        submission.publishedScoreTenths = Math.min(
          100,
          (submission.publishedScoreTenths ?? submission.scoreTenths) + appeal.deductionTenths,
        )
      }
    }
    return structuredClone(appeal)
  },

  async create(
    submissionId: number,
    studentId: string,
    constraintId: string,
    reason: string,
  ): Promise<AppealItem> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<AppealItem>(
    //   API_ENDPOINTS.submissions.appeals(submissionId),
    //   { constraintId, reason },
    // )
    // return response.data

    await wait()
    const submission = mockStore.submissions.find(
      (item) => item.id === submissionId && item.studentId === studentId,
    )
    const deduction = submission?.publishedDeductions?.find(
      (item) => item.constraintId === constraintId,
    )
    if (!submission || submission.publishedScoreTenths === null || !deduction) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/appeal-before-publish',
        title: 'Appeal unavailable',
        status: 409,
        detail: 'Appeals are available only for published deductions.',
        instance: `/v1/submissions/${submissionId}/appeals`,
      }
      throw problem
    }
    const existing = mockStore.appeals.find(
      (item) => item.submissionId === submissionId && item.constraintId === constraintId,
    )
    if (existing) return structuredClone(existing)
    const appeal: AppealItem = {
      id: Math.max(...mockStore.appeals.map((item) => item.id), 100) + 1,
      labId: submission.labId,
      submissionId,
      studentId,
      constraintId,
      constraintDescription: deduction.description,
      deductionTenths: deduction.deductionTenths,
      reason,
      status: 'pending',
      decisionReason: null,
      decidedBy: null,
      decidedAt: null,
      createdAt: new Date().toISOString(),
    }
    mockStore.appeals.push(appeal)
    return structuredClone(appeal)
  },
}
