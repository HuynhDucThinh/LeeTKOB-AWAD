import type { ConstraintType } from './lab'

export type CheckerVerdict = 'pass' | 'violation' | 'unsure'
export type TaDecision = 'pass' | 'violation'
export type VerdictSource = 'hard_rule' | 'llm' | 'guardrail'
export type GuardrailReason =
  | 'schema_invalid'
  | 'evidence_mismatch'
  | 'timeout'
  | 'cost_cap_exceeded'
  | 'model_off'
  | null

export interface EvidenceLine {
  line: number
  text: string
}

export interface ReviewItem {
  submissionId: number
  labId: number
  studentId: string
  constraintId: string
  constraintType: ConstraintType
  constraintDescription: string
  deductionTenths: number
  verdict: CheckerVerdict
  source: VerdictSource
  evidence: EvidenceLine[]
  guardrailReason: GuardrailReason
  sampledPass: boolean
  decision: TaDecision | null
  decidedBy: string | null
  decidedAt: string | null
  sourceCode: string
  strippedCode: string
}

export interface SubmitTaDecisionPayload {
  decision: TaDecision
}

export interface ReviewQueuePageResponse {
  items: ReviewItem[]
  total: number
  page: number
  size: number
}
