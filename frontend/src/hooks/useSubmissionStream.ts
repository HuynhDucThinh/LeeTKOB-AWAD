import { useEffect, useState } from 'react'
import { submissionEventsUrl } from '@/config/constants'
import type { JudgeStatusSseEvent, SubmissionSummary } from '@/types/submission'

interface UseSubmissionStreamOptions {
  submissionId: number | null
  enabled: boolean
  onUpdate: (updated: SubmissionSummary | JudgeStatusSseEvent) => void
}

export function useSubmissionStream({
  submissionId,
  enabled,
  onUpdate,
}: UseSubmissionStreamOptions) {
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!submissionId || !enabled) {
      setConnected(false)
      return
    }

    const source = new EventSource(submissionEventsUrl(submissionId), { withCredentials: true })
    source.onopen = () => setConnected(true)
    source.onmessage = (event) => {
      const payload = JSON.parse(event.data) as JudgeStatusSseEvent
      onUpdate(payload)
      if (payload.judgeStatus !== 'queued' && payload.judgeStatus !== 'running') {
        source.close()
        setConnected(false)
      }
    }
    source.onerror = () => {
      setConnected(false)
    }
    return () => {
      source.close()
      setConnected(false)
    }
  }, [submissionId, enabled, onUpdate])

  return { connected }
}
