import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/constants'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/authStore'
import type { ProblemDetail } from '@/types/problem'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null = null): void {
  failedQueue.forEach((entry) => {
    if (error) {
      entry.reject(error)
    } else if (token) {
      entry.resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor: inject Bearer access token from memory-only authStore
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const accessToken = useAuthStore.getState().accessToken
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  (error: unknown) => Promise.reject(error),
)

// Response interceptor: handle 401 with silent token rotation & concurrent failedQueue
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ProblemDetail>) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined

    if (!error.response || !originalRequest) {
      return Promise.reject(error)
    }

    const requestUrl = originalRequest.url ?? ''
    const isAuthRoute =
      requestUrl.includes(API_ENDPOINTS.auth.login) ||
      requestUrl.includes(API_ENDPOINTS.auth.refresh) ||
      requestUrl.includes(API_ENDPOINTS.auth.logout)

    if (error.response.status === 401 && !originalRequest._retry && !isAuthRoute) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(apiClient(originalRequest))
            },
            reject: (err: unknown) => reject(err),
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshed = await authService.refreshToken()
        if (!refreshed) {
          throw new Error('Session refresh failed')
        }

        const { user, accessToken } = refreshed
        useAuthStore.getState().setAuth(user, accessToken)
        processQueue(null, accessToken)

        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return apiClient(originalRequest)
      } catch (refreshError: unknown) {
        processQueue(refreshError, null)
        useAuthStore.getState().clearAuth()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)
