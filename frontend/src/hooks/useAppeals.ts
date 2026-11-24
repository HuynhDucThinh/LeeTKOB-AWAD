import { useCallback, useEffect, useState } from 'react'
import { getProblemDetail } from '@/lib/apiError'
import { appealService } from '@/services/appealService'
import type { AppealItem, AppealStatus } from '@/types/appeal'
import type { ProblemDetail } from '@/types/problem'

export function useAppeals(labId: number, studentId?: string) {
  const [appeals, setAppeals] = useState<AppealItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ProblemDetail | null>(null)

  const reload = useCallback(async () => {
    if (!Number.isFinite(labId) || labId <= 0) return
    setLoading(true)
    setError(null)
    try {
      const data = await appealService.list(labId, studentId)
      setAppeals(data)
    } catch (err: unknown) {
      setError(getProblemDetail(err))
    } finally {
      setLoading(false)
    }
  }, [labId, studentId])

  const decideAppeal = useCallback(
    async (appealId: number, status: Exclude<AppealStatus, 'pending'>, decisionReason: string) => {
      const updated = await appealService.decide(appealId, status, decisionReason)
      setAppeals((prev) => prev.map((item) => (item.id === appealId ? updated : item)))
      return updated
    },
    [],
  )

  useEffect(() => {
    void reload()
  }, [reload])

  return { appeals, loading, error, reload, decideAppeal }
}
