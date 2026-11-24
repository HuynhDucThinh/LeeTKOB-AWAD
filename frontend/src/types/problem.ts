export interface FieldErrorItem {
  pointer?: string
  parameter?: string
  detail: string
}

export type ProblemTypeSlug =
  | 'invalid-json'
  | 'validation'
  | 'authentication-required'
  | 'forbidden'
  | 'not-found'
  | 'method-not-allowed'
  | 'deadline-not-reached'
  | 'judging-pending'
  | 'reviews-pending'
  | 'payload-too-large'
  | 'unsupported-media-type'
  | 'publication-unavailable'
  | 'internal'
  | 'appeal-before-publish'
  | 'appeal-already-exists'
  | 'deadline-passed'

export interface ProblemDetail {
  type: `https://leetkob.example/problems/${ProblemTypeSlug}` | string
  title: string
  status: number
  detail: string
  instance?: string
  errors?: FieldErrorItem[]
}
