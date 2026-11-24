import { API_ENDPOINTS } from '@/config/constants'
import type { AuthUser } from '@/types/auth'
import type { CreateLabPayload, LabDetail, LabSummary } from '@/types/lab'
import type { ProblemDetail } from '@/types/problem'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const labService = {
  async getLabs(user: AuthUser): Promise<LabSummary[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<LabSummary[]>(API_ENDPOINTS.labs.list)
    // return response.data

    await wait()
    return structuredClone(
      mockStore.labs.filter((lab) => user.role === 'admin' || user.classIds.includes(lab.classId)),
    )
  },

  async getLab(labId: number, user: AuthUser): Promise<LabDetail> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<LabDetail>(API_ENDPOINTS.labs.detail(labId))
    // return response.data

    await wait()
    const lab = mockStore.labs.find((item) => item.id === labId)
    if (!lab || !user.classIds.includes(lab.classId)) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'The requested resource was not found.',
        instance: `/v1/labs/${labId}`,
      }
      throw problem
    }
    return structuredClone(lab)
  },

  async save(payload: CreateLabPayload, labId?: number): Promise<LabDetail> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = labId
    //   ? await apiClient.put<LabDetail>(API_ENDPOINTS.labs.detail(labId), payload)
    //   : await apiClient.post<LabDetail>(API_ENDPOINTS.labs.list, payload)
    // return response.data

    await wait()
    const classroom = mockStore.classes.find((item) => item.id === payload.classId)
    if (!classroom) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'Assigned class was not found.',
        instance: labId ? `/v1/labs/${labId}` : '/v1/labs',
      }
      throw problem
    }
    const existing = labId ? mockStore.labs.find((item) => item.id === labId) : undefined
    const detail: LabDetail = {
      id: existing?.id ?? Math.max(...mockStore.labs.map((item) => item.id), 0) + 1,
      ...payload,
      className: classroom.name,
      tests: payload.tests.map((test, index) => ({
        ...test,
        id: existing?.tests[index]?.id ?? Date.now() + index,
      })),
      costSpentUsd: existing?.costSpentUsd ?? 0,
      isPublished: existing?.isPublished ?? false,
      publishedAt: existing?.publishedAt ?? null,
      submissionCount: existing?.submissionCount ?? 0,
      judgingPendingCount: existing?.judgingPendingCount ?? 0,
      unreviewedCount: existing?.unreviewedCount ?? 0,
      publicationReceipt: existing?.publicationReceipt ?? null,
    }
    if (existing) {
      Object.assign(existing, detail)
    } else {
      mockStore.labs.push(detail)
      classroom.labCount += 1
    }
    return structuredClone(detail)
  },
}
