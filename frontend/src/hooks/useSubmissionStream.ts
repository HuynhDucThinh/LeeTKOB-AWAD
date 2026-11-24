import { useEffect, useState } from 'react'
import { submissionEventsUrl } from '@/config/constants'
import { submissionService } from '@/services/submissionService'
import type { JudgeStatusSseEvent, SubmissionSummary } from '@/types/submission'

void submissionEventsUrl

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

    // REAL SSE STREAM (uncomment when backend SSE endpoint is ready):
    // const source = new EventSource(submissionEventsUrl(submissionId), { withCredentials: true })
    // source.onopen = () => setConnected(true)
    // source.onmessage = (event) => {
    //   const payload = JSON.parse(event.data) as JudgeStatusSseEvent
    //   onUpdate(payload)
    //   if (payload.judgeStatus !== 'queued' && payload.judgeStatus !== 'running') {
    //     source.close()
    //     setConnected(false)
    //   }
    // }
    // source.onerror = () => {
    //   setConnected(false)
    // }
    // return () => {
    //   source.close()
    //   setConnected(false)
    // }

    setConnected(true)
    let cancelled = false
    const runningTimer = window.setTimeout(() => {
      void submissionService
        .mockAdvance(submissionId, 'running')
        .then((updated) => {
          if (!cancelled) onUpdate(updated)
        })
        .catch(() => undefined)
    }, 900)

    const doneTimer = window.setTimeout(() => {
      void submissionService
        .mockAdvance(submissionId, 'AC')
        .then((updated) => {
          if (!cancelled) {
            onUpdate(updated)
            setConnected(false)
          }
        })
        .catch(() => undefined)
    }, 2200)

    return () => {
      cancelled = true
      window.clearTimeout(runningTimer)
      window.clearTimeout(doneTimer)
      setConnected(false)
    }
  }, [submissionId, enabled, onUpdate])

  return { connected }
}
