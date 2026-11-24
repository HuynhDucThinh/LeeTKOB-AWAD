import { Inbox } from 'lucide-react'

export function EmptyState({ title, detail, action }: { title: string; detail: string; action?: React.ReactNode }) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white/60 px-6 text-center">
      <div className="mb-4 grid size-11 place-items-center rounded-2xl bg-lime-50 text-lime-600"><Inbox className="size-5" /></div>
      <h3 className="font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">{detail}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
