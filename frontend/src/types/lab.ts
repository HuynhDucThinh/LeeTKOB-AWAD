import type { PublicationReceipt } from './publish'

export type ConstraintType = 'hard' | 'semantic'

export interface LabTestCase {
  id: number
  name: string
  input: string
  expectedOutput: string
  timeLimitMs: number
  memoryLimitMb: number
  isSample: boolean
}

export interface LabConstraint {
  constraintId: string
  type: ConstraintType
  description: string
  deductionTenths: number
}

export interface LabSummary {
  id: number
  classId: number
  className: string
  title: string
  deadline: string
  costCapUsd: number
  costSpentUsd: number
  isPublished: boolean
  publishedAt: string | null
  submissionCount: number
  judgingPendingCount: number
  unreviewedCount: number
}

export interface LabDetail extends LabSummary {
  statementMd: string
  tests: LabTestCase[]
  constraints: LabConstraint[]
  publicationReceipt: PublicationReceipt | null
}

export interface CreateLabPayload {
  classId: number
  title: string
  deadline: string
  costCapUsd: number
  statementMd: string
  tests: Omit<LabTestCase, 'id'>[]
  constraints: LabConstraint[]
}
