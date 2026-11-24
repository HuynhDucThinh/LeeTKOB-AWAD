import { cn } from '@/lib/utils'

const toneMap: Record<string, string> = {
  AC: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  published: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  pass: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  accepted: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  violation: 'bg-rose-50 text-rose-700 ring-rose-200',
  rejected: 'bg-rose-50 text-rose-700 ring-rose-200',
  unsure: 'bg-amber-50 text-amber-700 ring-amber-200',
  pending: 'bg-amber-50 text-amber-700 ring-amber-200',
  running: 'bg-lime-50 text-lime-700 ring-lime-200',
  queued: 'bg-slate-100 text-slate-600 ring-slate-200',
}

export function StatusBadge({ value, label }: { value: string; label?: string }) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold ring-1 ring-inset', toneMap[value] ?? 'bg-lime-50 text-lime-700 ring-lime-200')}>
      {label ?? value}
    </span>
  )
}
