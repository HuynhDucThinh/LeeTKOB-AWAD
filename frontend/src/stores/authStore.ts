import { create } from 'zustand'
import type { AuthUser } from '@/types/auth'

interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  isAuthenticated: boolean
  isInitializing: boolean
  setAuth: (user: AuthUser, accessToken: string) => void
  setSession: (user: AuthUser, accessToken: string) => void
  setAccessToken: (accessToken: string) => void
  setUser: (user: AuthUser) => void
  clearAuth: () => void
  logout: () => void
  setInitializing: (isInitializing: boolean) => void
}

// Memory-only authentication store (never persists access tokens to localStorage/sessionStorage)
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  isInitializing: true,
  setAuth: (user, accessToken) =>
    set({
      user,
      accessToken,
      isAuthenticated: true,
      isInitializing: false,
    }),
  setSession: (user, accessToken) =>
    set({
      user,
      accessToken,
      isAuthenticated: true,
      isInitializing: false,
    }),
  setAccessToken: (accessToken) =>
    set({
      accessToken,
      isAuthenticated: true,
    }),
  setUser: (user) =>
    set({
      user,
      isAuthenticated: true,
    }),
  clearAuth: () =>
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isInitializing: false,
    }),
  logout: () =>
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isInitializing: false,
    }),
  setInitializing: (isInitializing) => set({ isInitializing }),
}))
