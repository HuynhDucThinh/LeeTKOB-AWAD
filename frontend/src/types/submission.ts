import type { EvidenceLine } from './review'

export type JudgeStatus = 'queued' | 'running' | 'AC' | 'WA' | 'TLE' | 'MLE' | 'RE' | 'CE'

export interface SubmissionSummary {
  id: number
  labId: number
  studentId: string
  studentName?: string
  judgeStatus: JudgeStatus
  scoreTenths: number
  publishedScoreTenths: number | null
  isCounted: boolean
  passedTests: number
  totalTests: number
  executionTimeMs: number | null
  memoryKb: number | null
  compileError: string | null
  submittedAt: string
  judgedAt: string | null
  traceId: string
}

export interface SubmissionDetail extends SubmissionSummary {
  sourceCode: string
  strippedCode: string | null
  publishedDeductions: Array<{
    constraintId: string
    description: string
    deductionTenths: number
    evidence: EvidenceLine[]
  }> | null
}

export interface SubmitCodePayload {
  sourceCode: string
}

export interface JudgeStatusSseEvent {
  submissionId: number
  labId: number
  judgeStatus: JudgeStatus
  passedTests: number
  totalTests: number
  executionTimeMs: number | null
  memoryKb: number | null
  compileError: string | null
  scoreTenths: number
  judgedAt: string | null
}
