import { API_ENDPOINTS } from '@/config/constants'
import type { ClassRoom, CreateUserPayload } from '@/types/admin'
import type { AuthUser, UserRole } from '@/types/auth'
import { apiClient } from './api'

export const adminService = {
  async getClasses(): Promise<ClassRoom[]> {
    const response = await apiClient.get<ClassRoom[]>(API_ENDPOINTS.admin.classes)
    return response.data
  },

  async getUsers(role?: UserRole): Promise<AuthUser[]> {
    const response = await apiClient.get<AuthUser[]>(API_ENDPOINTS.admin.users, {
      params: role ? { role } : undefined,
    })
    return response.data
  },

  async createClass(name: string): Promise<ClassRoom> {
    const response = await apiClient.post<ClassRoom>(API_ENDPOINTS.admin.classes, { name })
    return response.data
  },

  async updateMembers(classId: number, taIds: string[], studentIds: string[]): Promise<ClassRoom> {
    const response = await apiClient.put<ClassRoom>(API_ENDPOINTS.admin.classMembers(classId), {
      taIds,
      studentIds,
    })
    return response.data
  },

  async createUser(payload: CreateUserPayload): Promise<AuthUser> {
    const response = await apiClient.post<AuthUser>(API_ENDPOINTS.admin.users, payload)
    return response.data
  },
}
