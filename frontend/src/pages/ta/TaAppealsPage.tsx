import { useEffect, useState } from 'react'
import { Check, X } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { appealService } from '@/services/appealService'
import type { AppealItem } from '@/types/appeal'
import { StatusBadge } from '@/components/common/StatusBadge'
import { formatDeduction, formatUtcDate } from '@/lib/utils'

export function TaAppealsPage() {
  const { labId } = useParams()
  const [appeals, setAppeals] = useState<AppealItem[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [reason, setReason] = useState('')
  const [validation, setValidation] = useState('')
  useEffect(() => { appealService.list(Number(labId)).then((items) => { setAppeals(items); setSelectedId(items[0]?.id ?? null) }).catch(() => setAppeals([])) }, [labId])
  const selected = appeals.find((item) => item.id === selectedId)
  const decide = async (status: 'accepted' | 'rejected') => {
    if (!selected) return
    if (!reason.trim()) {
      setValidation('Enter a decision reason before accepting or rejecting this appeal.')
      return
    }
    setValidation('')
    const updated = await appealService.decide(selected.id, status, reason)
    setAppeals((items) => items.map((item) => item.id === updated.id ? updated : item))
    setReason('')
  }
  return (
    <div><div className="page-header"><div><p className="eyebrow">Post-publication workflow</p><h1>Appeals</h1><p>Review student concerns and leave a complete decision audit.</p></div></div>
      <div className="grid gap-5 lg:grid-cols-[340px_1fr]">
        <div className="surface-card p-3">{appeals.map((item) => <button key={item.id} onClick={() => { setSelectedId(item.id); setValidation(''); setReason('') }} className={`mb-2 w-full rounded-2xl p-4 text-left ${selectedId === item.id ? 'bg-lime-50 ring-1 ring-lime-200' : 'bg-slate-50'}`}><div className="flex justify-between"><span className="font-mono text-xs">Appeal #{item.id}</span><StatusBadge value={item.status} /></div><p className="mt-2 text-xs font-medium">{item.studentId} · {item.constraintId}</p><p className="mt-1 line-clamp-2 text-[11px] text-slate-400">{item.reason}</p></button>)}</div>
        {selected && <div className="surface-card"><div className="flex items-start justify-between"><div><p className="eyebrow">Submission #{selected.submissionId}</p><h2 className="mt-2 text-lg font-semibold">{selected.constraintDescription}</h2></div><strong className="text-rose-600">{formatDeduction(selected.deductionTenths)}</strong></div><div className="my-5 rounded-2xl bg-slate-50 p-4"><p className="text-[10px] uppercase tracking-wide text-slate-400">Student reason</p><p className="mt-2 text-sm leading-6 text-slate-700">{selected.reason}</p><p className="mt-3 text-[10px] text-slate-400">{formatUtcDate(selected.createdAt)}</p></div>{selected.status === 'pending' ? <><label className="field-label">Decision reason<textarea value={reason} onChange={(event) => { setReason(event.target.value); setValidation('') }} className={`input mt-2 min-h-28 ${validation ? 'border-rose-400' : ''}`} placeholder="Explain why this appeal is accepted or rejected..." /></label>{validation && <p className="mt-2 text-xs font-medium text-rose-600">{validation}</p>}<div className="mt-4 flex gap-2"><button onClick={() => void decide('accepted')} className="primary-button bg-emerald-600"><Check className="size-4" /> Accept Appeal</button><button onClick={() => void decide('rejected')} className="danger-button"><X className="size-4" /> Reject Appeal</button></div></> : <div className="rounded-2xl border border-lime-100 bg-lime-50 p-4"><StatusBadge value={selected.status} /><p className="mt-3 text-sm">{selected.decisionReason}</p><p className="mt-2 text-[10px] text-slate-400">Decided by {selected.decidedBy} · {selected.decidedAt && formatUtcDate(selected.decidedAt)}</p></div>}</div>}
      </div>
    </div>
  )
}
