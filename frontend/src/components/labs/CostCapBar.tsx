import { formatUsd } from '@/lib/utils'

export function CostCapBar({ spent, cap }: { spent: number; cap: number }) {
  const percentage = cap <= 0 ? 100 : Math.min(100, Math.round((spent / cap) * 100))
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-xs"><span className="text-slate-500">Budget usage</span><span className="font-semibold text-slate-800">{formatUsd(spent)} / {formatUsd(cap)}</span></div>
      <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${cap <= 0 || spent >= cap ? 'bg-amber-500' : 'bg-gradient-to-r from-lime-500 to-emerald-400'}`} style={{ width: `${percentage}%` }} /></div>
    </div>
  )
}
