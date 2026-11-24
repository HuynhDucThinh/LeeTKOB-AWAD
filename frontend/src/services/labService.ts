import { API_ENDPOINTS } from '@/config/constants'
import type { AuthUser } from '@/types/auth'
import type { CreateLabPayload, LabDetail, LabSummary } from '@/types/lab'
import { apiClient } from './api'

export const labService = {
  async getLabs(_user?: AuthUser): Promise<LabSummary[]> {
    const response = await apiClient.get<LabSummary[]>(API_ENDPOINTS.labs.list)
    return response.data
  },

  async getLab(labId: number, _user?: AuthUser): Promise<LabDetail> {
    const response = await apiClient.get<LabDetail>(API_ENDPOINTS.labs.detail(labId))
    return response.data
  },

  async save(payload: CreateLabPayload, labId?: number): Promise<LabDetail> {
    const response = labId
      ? await apiClient.put<LabDetail>(API_ENDPOINTS.labs.detail(labId), payload)
      : await apiClient.post<LabDetail>(API_ENDPOINTS.labs.list, payload)
    return response.data
  },
}
