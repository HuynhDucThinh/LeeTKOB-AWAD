import { API_ENDPOINTS } from '@/config/constants'
import type { ClassRoom } from '@/types/admin'
import type { AuthUser } from '@/types/auth'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const classService = {
  async getMyClasses(user: AuthUser): Promise<ClassRoom[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<ClassRoom[]>(API_ENDPOINTS.classes)
    // return response.data

    await wait()
    return structuredClone(
      mockStore.classes.filter((classroom) => user.classIds.includes(classroom.id)),
    )
  },
}
