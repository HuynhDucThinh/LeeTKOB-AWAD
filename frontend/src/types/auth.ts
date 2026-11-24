export type UserRole = 'admin' | 'ta' | 'student'

export interface AuthUser {
  id: string
  username: string
  displayName: string
  role: UserRole
  classIds: number[]
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  user: AuthUser
}
