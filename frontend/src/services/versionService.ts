import { API_ENDPOINTS } from '@/config/constants'
import type { VersionInfo } from '@/types/version'
import { apiClient } from './api'

export const versionService = {
  async get(): Promise<VersionInfo> {
    const response = await apiClient.get<VersionInfo>(API_ENDPOINTS.version)
    return response.data
  },
}
