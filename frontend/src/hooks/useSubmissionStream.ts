import { useEffect, useRef, useState } from 'react'
import { submissionEventsUrl } from '@/config/constants'
import { useAuthStore } from '@/stores/authStore'
import type { JudgeStatusSseEvent, SubmissionSummary } from '@/types/submission'

interface UseSubmissionStreamOptions {
  submissionId: number | null
  enabled: boolean
  onUpdate: (updated: SubmissionSummary | JudgeStatusSseEvent) => void
}

const isTerminalStatus = (status: string): boolean => status !== 'queued' && status !== 'running'

export function useSubmissionStream({
  submissionId,
  enabled,
  onUpdate,
}: UseSubmissionStreamOptions) {
  const [connected, setConnected] = useState(false)
  const onUpdateRef = useRef(onUpdate)
  onUpdateRef.current = onUpdate

  useEffect(() => {
    if (!submissionId || !enabled) {
      setConnected(false)
      return
    }

    const abortController = new AbortController()
    const accessToken = useAuthStore.getState().accessToken

    const connectStream = async () => {
      try {
        const headers: Record<string, string> = { Accept: 'text/event-stream' }
        if (accessToken) {
          headers.Authorization = `Bearer ${accessToken}`
        }

        const response = await fetch(submissionEventsUrl(submissionId), {
          method: 'GET',
          headers,
          credentials: 'include',
          signal: abortController.signal,
        })

        if (!response.ok || !response.body) {
          setConnected(false)
          return
        }

        setConnected(true)
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (!abortController.signal.aborted) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const events = buffer.split(/\r?\n\r?\n/)
          buffer = events.pop() ?? ''

          for (const rawEvent of events) {
            const dataLines = rawEvent
              .split(/\r?\n/)
              .filter((line) => line.startsWith('data:'))
              .map((line) => line.slice(5).trim())
            if (dataLines.length === 0) continue

            try {
              const payload = JSON.parse(dataLines.join('\n')) as JudgeStatusSseEvent
              onUpdateRef.current(payload)
              if (isTerminalStatus(payload.judgeStatus)) {
                abortController.abort()
                setConnected(false)
                return
              }
            } catch {
              // ignore non-JSON heartbeat frames
            }
          }
        }
      } catch {
        // stream aborted or closed
      } finally {
        setConnected(false)
      }
    }

    void connectStream()

    return () => {
      abortController.abort()
      setConnected(false)
    }
  }, [submissionId, enabled])

  return { connected }
}
