import { API_ENDPOINTS } from '@/config/constants'
import type { LoginResponse } from '@/types/auth'
import type { ProblemDetail } from '@/types/problem'
import { apiClient } from './api'
import { mockStore, wait } from './mock/mockStore'

// Keep reference for quick switch to real API
void apiClient
void API_ENDPOINTS

export const authService = {
  async login(username: string, password: string): Promise<LoginResponse> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<LoginResponse>(API_ENDPOINTS.auth.login, { username, password })
    // return response.data

    await wait(300)
    const user = mockStore.users.find((item) => item.username === username || item.id === username)
    if (!user || !password) {
      const problem: ProblemDetail = {
        type: 'https://leetkob.example/problems/authentication-required',
        title: 'Authentication required',
        status: 401,
        detail: 'Invalid username or password.',
        instance: '/v1/auth/login',
      }
      throw problem
    }
    return { user: structuredClone(user), accessToken: `mock-token-${user.id}` }
  },

  async refreshToken(): Promise<LoginResponse | null> {
    // REAL API (uncomment when backend endpoint is ready):
    // const response = await apiClient.post<LoginResponse>(API_ENDPOINTS.auth.refresh, {})
    // return response.data

    await wait(80)
    return null
  },

  async logout(): Promise<void> {
    // REAL API (uncomment when backend endpoint is ready):
    // await apiClient.post(API_ENDPOINTS.auth.logout, {})

    await wait(80)
  },
}
