import { API_ENDPOINTS } from '@/config/constants'
import type { ClassRoom, CreateUserPayload } from '@/types/admin'
import type { AuthUser, UserRole } from '@/types/auth'
import type { ProblemDetail } from '@/types/problem'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

void apiClient
void API_ENDPOINTS

export const adminService = {
  async getClasses(): Promise<ClassRoom[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<ClassRoom[]>(API_ENDPOINTS.admin.classes)
    // return response.data

    await wait()
    return structuredClone(mockStore.classes)
  },

  async getUsers(role?: UserRole): Promise<AuthUser[]> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.get<AuthUser[]>(API_ENDPOINTS.admin.users, { params: { role } })
    // return response.data

    await wait()
    return structuredClone(role ? mockStore.users.filter((user) => user.role === role) : mockStore.users)
  },

  async createClass(name: string): Promise<ClassRoom> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<ClassRoom>(API_ENDPOINTS.admin.classes, { name })
    // return response.data

    await wait()
    const classroom: ClassRoom = {
      id: Math.max(...mockStore.classes.map((item) => item.id), 0) + 1,
      name,
      taIds: [],
      studentIds: [],
      labCount: 0,
    }
    mockStore.classes.push(classroom)
    return structuredClone(classroom)
  },

  async updateMembers(classId: number, taIds: string[], studentIds: string[]): Promise<ClassRoom> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.put<ClassRoom>(API_ENDPOINTS.admin.classMembers(classId), { taIds, studentIds })
    // return response.data

    await wait()
    const classroom = mockStore.classes.find((item) => item.id === classId)
    if (!classroom) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/not-found',
        title: 'Not found',
        status: 404,
        detail: 'The requested class was not found.',
        instance: `/v1/admin/classes/${classId}/members`,
      }
      throw problem
    }
    classroom.taIds = taIds
    classroom.studentIds = studentIds
    for (const user of mockStore.users) {
      const shouldBelong = taIds.includes(user.id) || studentIds.includes(user.id)
      user.classIds = shouldBelong
        ? Array.from(new Set([...user.classIds, classId]))
        : user.classIds.filter((id) => id !== classId)
    }
    return structuredClone(classroom)
  },

  async createUser(payload: CreateUserPayload): Promise<AuthUser> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<AuthUser>(API_ENDPOINTS.admin.users, payload)
    // return response.data

    await wait()
    if (mockStore.users.some((item) => item.username === payload.username)) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/validation',
        title: 'Username unavailable',
        status: 409,
        detail: 'This username is already in use.',
        instance: '/v1/admin/users',
      }
      throw problem
    }
    const user: AuthUser = {
      id: `${payload.role}-${Date.now().toString().slice(-5)}`,
      username: payload.username,
      displayName: payload.displayName,
      role: payload.role,
      classIds: [],
      createdAt: new Date().toISOString(),
    }
    mockStore.users.push(user)
    return structuredClone(user)
  },
}
