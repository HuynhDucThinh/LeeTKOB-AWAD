import { useCallback, useEffect, useState } from 'react'
import { getProblemDetail } from '@/lib/apiError'
import { labService } from '@/services/labService'
import { useAuthStore } from '@/stores/authStore'
import type { LabSummary } from '@/types/lab'
import type { ProblemDetail } from '@/types/problem'

export function useLabs() {
  const user = useAuthStore((state) => state.user)
  const [labs, setLabs] = useState<LabSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ProblemDetail | null>(null)

  const reload = useCallback(async () => {
    if (!user) return
    setLoading(true)
    setError(null)
    try {
      const data = await labService.getLabs(user)
      setLabs(data)
    } catch (err: unknown) {
      setError(getProblemDetail(err))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    void reload()
  }, [reload])

  return { labs, loading, error, reload }
}
