import axios, { AxiosError } from 'axios'
import { AUTH_API_BASE_URL } from '@/config/constants'
import type { AuthUser, LoginResponse, UserRole } from '@/types/auth'
import type { ProblemDetail } from '@/types/problem'

interface BackendUserResponse {
  id: string
  email: string
  fullName: string
  role: UserRole
  classIds?: number[]
  avatarUrl?: string | null
  createdAt: string
}

interface BackendAuthResponse {
  accessToken: string
  user: BackendUserResponse
}

interface BackendApiResponse<T> {
  success: boolean
  message: string
  data: T | null
}

const authClient = axios.create({
  baseURL: AUTH_API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

function mapUser(raw: BackendUserResponse): AuthUser {
  return {
    id: raw.id,
    username: raw.email,
    displayName: raw.fullName || raw.email,
    role: raw.role,
    classIds: raw.classIds ?? [],
    createdAt: raw.createdAt,
  }
}

function toProblemDetail(error: unknown, instance: string): ProblemDetail {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<BackendApiResponse<unknown> & Partial<ProblemDetail>>
    const status = axiosError.response?.status ?? 503
    const body = axiosError.response?.data
    return {
      type: body?.type ?? 'https://leetkob.example/problems/authentication-required',
      title: body?.title ?? (status === 401 ? 'Authentication required' : 'Authentication failed'),
      status,
      detail: body?.detail ?? body?.message ?? axiosError.message ?? 'Unable to reach authentication server.',
      instance,
    }
  }
  return {
    type: 'https://leetkob.example/problems/internal-error',
    title: 'Authentication failed',
    status: 500,
    detail: error instanceof Error ? error.message : 'Unexpected authentication error.',
    instance,
  }
}

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    try {
      const response = await authClient.post<BackendApiResponse<BackendAuthResponse>>('/login', {
        email: email.trim(),
        password,
      })
      const payload = response.data.data
      if (!payload) {
        throw new Error(response.data.message || 'Empty login response')
      }
      return {
        accessToken: payload.accessToken,
        user: mapUser(payload.user),
      }
    } catch (error) {
      throw toProblemDetail(error, '/api/auth/login')
    }
  },

  async refreshToken(): Promise<LoginResponse | null> {
    try {
      const response = await authClient.post<BackendApiResponse<BackendAuthResponse>>('/refresh-token', {})
      const payload = response.data.data
      if (!payload) {
        return null
      }
      return {
        accessToken: payload.accessToken,
        user: mapUser(payload.user),
      }
    } catch {
      return null
    }
  },

  async logout(): Promise<void> {
    try {
      await authClient.post('/logout', {})
    } catch {
      // Clear client session even if server cookie was already expired
    }
  },
}
