import { API_ENDPOINTS } from '@/config/constants'
import type { PublicationReceipt, PublishApiResponse } from '@/types/publish'
import { apiClient } from './api'

export const publishService = {
  async publish(labId: number): Promise<PublishApiResponse> {
    const response = await apiClient.post<PublicationReceipt>(API_ENDPOINTS.labs.publish(labId), {})
    return {
      receipt: response.data,
      replayed: response.headers['idempotent-replayed'] === 'true',
    }
  },
}
