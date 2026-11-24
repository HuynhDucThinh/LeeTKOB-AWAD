import { useCallback, useEffect, useState } from 'react'
import { getProblemDetail } from '@/lib/apiError'
import { reviewService } from '@/services/reviewService'
import type { ProblemDetail } from '@/types/problem'
import type { ReviewItem, TaDecision } from '@/types/review'

export function useReviewQueue(labId: number) {
  const [items, setItems] = useState<ReviewItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ProblemDetail | null>(null)

  const reload = useCallback(async () => {
    if (!Number.isFinite(labId) || labId <= 0) return
    setLoading(true)
    setError(null)
    try {
      const queue = await reviewService.getQueue(labId)
      setItems(queue)
    } catch (err: unknown) {
      setError(getProblemDetail(err))
    } finally {
      setLoading(false)
    }
  }, [labId])

  const submitDecision = useCallback(
    async (submissionId: number, constraintId: string, decision: TaDecision) => {
      const updated = await reviewService.decide(labId, submissionId, constraintId, decision)
      setItems((prev) =>
        prev.map((item) =>
          item.submissionId === submissionId && item.constraintId === constraintId ? updated : item,
        ),
      )
      return updated
    },
    [labId],
  )

  useEffect(() => {
    void reload()
  }, [reload])

  return { items, loading, error, reload, submitDecision }
}
