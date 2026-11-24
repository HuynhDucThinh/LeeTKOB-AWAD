import { API_ENDPOINTS } from '@/config/constants'
import type { VersionInfo } from '@/types/version'
import { apiClient } from './api'

void apiClient
void API_ENDPOINTS

export const versionService = {
  async get(): Promise<VersionInfo> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<VersionInfo>(API_ENDPOINTS.version)
    // return response.data

    return {
      commit: 'a1b2c3d',
      tag: 'pa5',
      deployedAt: '2026-12-16T10:00:00.000Z',
    }
  },
}
