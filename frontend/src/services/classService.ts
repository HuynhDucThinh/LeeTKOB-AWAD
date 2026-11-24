import { API_ENDPOINTS } from '@/config/constants'
import type { ClassRoom } from '@/types/admin'
import type { AuthUser } from '@/types/auth'
import { apiClient } from './api'

export const classService = {
  async getMyClasses(_user?: AuthUser): Promise<ClassRoom[]> {
    const response = await apiClient.get<ClassRoom[]>(API_ENDPOINTS.classes)
    return response.data
  },
}
