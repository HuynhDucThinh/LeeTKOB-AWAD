import { API_ENDPOINTS } from '@/config/constants'
import type { ProblemDetail } from '@/types/problem'
import type { SubmissionDetail, SubmissionSummary } from '@/types/submission'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const submissionService = {
  async list(labId: number, studentId?: string): Promise<SubmissionSummary[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<SubmissionSummary[]>(API_ENDPOINTS.labs.submissions(labId))
    // return response.data

    await wait()
    return structuredClone(
      mockStore.submissions.filter(
        (item) => item.labId === labId && (!studentId || item.studentId === studentId),
      ),
    )
  },

  async submit(labId: number, studentId: string, sourceCode: string): Promise<SubmissionSummary> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<SubmissionSummary>(
    //   API_ENDPOINTS.labs.submissions(labId),
    //   { sourceCode },
    //   { headers: { 'Idempotency-Key': crypto.randomUUID() } },
    // )
    // return response.data

    await wait(300)
    const submission: SubmissionDetail = {
      id: Date.now(),
      labId,
      studentId,
      judgeStatus: 'queued',
      scoreTenths: 0,
      publishedScoreTenths: null,
      isCounted: true,
      passedTests: 0,
      totalTests: 5,
      executionTimeMs: null,
      memoryKb: null,
      compileError: null,
      submittedAt: new Date().toISOString(),
      judgedAt: null,
      traceId: `tr_${crypto.randomUUID().slice(0, 8)}`,
      sourceCode,
      strippedCode: sourceCode,
      publishedDeductions: null,
    }
    mockStore.submissions.push(submission)
    return structuredClone(submission)
  },

  async get(submissionId: number): Promise<SubmissionDetail> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<SubmissionDetail>(API_ENDPOINTS.submissions.detail(submissionId))
    // return response.data

    await wait()
    const submission = mockStore.submissions.find((item) => item.id === submissionId)
    if (!submission) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'Submission was not found.',
        instance: `/v1/submissions/${submissionId}`,
      }
      throw problem
    }
    return structuredClone(submission)
  },

  async mockAdvance(submissionId: number, status: 'running' | 'AC'): Promise<SubmissionSummary> {
    const item = mockStore.submissions.find((entry) => entry.id === submissionId)
    if (!item) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'Submission was not found.',
        instance: `/v1/submissions/${submissionId}`,
      }
      throw problem
    }
    item.judgeStatus = status
    if (status === 'AC') {
      item.scoreTenths = 100
      item.passedTests = 5
      item.executionTimeMs = 42
      item.memoryKb = 12400
      item.judgedAt = new Date().toISOString()
    }
    return structuredClone(item)
  },
}
