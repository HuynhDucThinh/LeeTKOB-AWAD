export interface ClassRoom {
  id: number
  name: string
  taIds: string[]
  studentIds: string[]
  labCount: number
}

export interface CreateUserPayload {
  username: string
  displayName: string
  role: 'admin' | 'ta' | 'student'
  password: string
}
