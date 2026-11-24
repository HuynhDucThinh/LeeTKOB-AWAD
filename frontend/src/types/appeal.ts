export type AppealStatus = 'pending' | 'accepted' | 'rejected'

export interface AppealItem {
  id: number
  labId: number
  submissionId: number
  studentId: string
  constraintId: string
  constraintDescription: string
  deductionTenths: number
  reason: string
  status: AppealStatus
  decisionReason: string | null
  decidedBy: string | null
  decidedAt: string | null
  createdAt: string
}

export interface CreateAppealPayload {
  constraintId: string
  reason: string
}

export interface DecideAppealPayload {
  status: Exclude<AppealStatus, 'pending'>
  decisionReason: string
}
