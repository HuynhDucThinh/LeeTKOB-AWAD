import { AlertCircle, RotateCcw } from 'lucide-react'
import type { ProblemDetail } from '@/types/problem'

export function ProblemBanner({ problem, onRetry }: { problem: ProblemDetail; onRetry?: () => void }) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-rose-800">
      <AlertCircle className="mt-0.5 size-4 shrink-0" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{problem.title}</p>
        <p className="mt-0.5 text-sm text-rose-700">{problem.detail}</p>
      </div>
      {onRetry && <button onClick={onRetry} className="inline-flex items-center gap-1 text-xs font-semibold"><RotateCcw className="size-3" /> Retry</button>}
    </div>
  )
}
