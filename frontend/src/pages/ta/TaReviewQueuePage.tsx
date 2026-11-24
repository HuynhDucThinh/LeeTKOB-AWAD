import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, ArrowRight, Check, CheckCircle2, ExternalLink, LockKeyhole, Send, ShieldAlert, Sparkles } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { reviewService } from '@/services/reviewService'
import { publishService } from '@/services/publishService'
import { labService } from '@/services/labService'
import { useAuthStore } from '@/stores/authStore'
import type { LabDetail } from '@/types/lab'
import type { ReviewItem, TaDecision } from '@/types/review'
import type { ProblemDetail } from '@/types/problem'
import type { PublicationReceipt } from '@/types/publish'
import { getProblemError } from '@/lib/apiError'
import { formatDeduction } from '@/lib/utils'
import { StatusBadge } from '@/components/common/StatusBadge'
import { CppCodeViewer } from '@/components/code/CppCodeViewer'
import { Modal } from '@/components/common/Modal'
import { ProblemBanner } from '@/components/common/ProblemBanner'
import { cn } from '@/lib/utils'

type Filter = 'all' | 'unreviewed' | 'violation' | 'unsure' | 'pass'

export function TaReviewQueuePage() {
  const { labId } = useParams()
  const id = Number(labId)
  const [items, setItems] = useState<ReviewItem[]>([])
  const [selectedKey, setSelectedKey] = useState('')
  const [filter, setFilter] = useState<Filter>('all')
  const [publishOpen, setPublishOpen] = useState(false)
  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const [receipt, setReceipt] = useState<PublicationReceipt | null>(null)
  const [lab, setLab] = useState<LabDetail | null>(null)
  const user = useAuthStore((state) => state.user)
  const navigate = useNavigate()

  const load = () => reviewService.getQueue(id).then((data) => { setItems(data); if (!selectedKey && data[0]) setSelectedKey(keyOf(data[0])) }).catch((error) => setProblem(getProblemError(error)))
  useEffect(() => {
    if (!id) return
    load()
    if (user) labService.getLab(id, user).then((detail) => {
      setLab(detail)
    }).catch(() => undefined)
  }, [id, user])
  const selected = items.find((item) => keyOf(item) === selectedKey) ?? items[0]
  const visible = useMemo(() => items.filter((item) => filter === 'all' || (filter === 'unreviewed' ? item.decision === null : item.verdict === filter)), [items, filter])
  const reviewed = items.filter((item) => item.decision !== null).length
  const violations = items.filter((item) => item.verdict === 'violation').length
  const unsure = items.filter((item) => item.verdict === 'unsure').length
  const pending = items.length - reviewed

  const decide = async (decision: TaDecision) => {
    if (!selected) return
    const updated = await reviewService.decide(id, selected.submissionId, selected.constraintId, decision)
    setItems((current) => current.map((item) => keyOf(item) === keyOf(updated) ? updated : item))
    const next = items.find((item) => item.decision === null && keyOf(item) !== keyOf(selected))
    if (next) setSelectedKey(keyOf(next))
  }

  const publish = async () => {
    setProblem(null)
    try {
      const result = await publishService.publish(id)
      setReceipt(result.receipt)
      setLab((current) => current ? { ...current, isPublished: true, publishedAt: result.receipt.publishedAt, publicationReceipt: result.receipt } : current)
      setPublishOpen(false)
    } catch (error) {
      setProblem(getProblemError(error))
      setPublishOpen(false)
    }
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 rounded-2xl border border-lime-300/20 bg-gradient-to-r from-[#10140e] to-[#252e1e] p-5 text-white shadow-xl shadow-slate-950/10 xl:flex-row xl:items-center">
        <div className="flex-1"><div className="mb-2 flex items-center gap-2"><button onClick={() => navigate(`/ta/labs/${id}`)} className="text-xs text-lime-300 hover:text-white">Lab 01 · Merge Sort</button>{lab?.isPublished && <StatusBadge value="published" label="Published" />}</div><h1 className="text-xl font-semibold">Review Queue & Publish Gate</h1><p className="mt-1 text-xs text-slate-400">Human verification is required before marks become visible.</p></div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="rounded-2xl bg-white/10 px-5 py-3 backdrop-blur"><span className="text-[10px] uppercase tracking-widest text-lime-200">Progress</span><strong className="ml-3 text-xl">{reviewed}/{items.length}</strong></div>
          <button onClick={() => setPublishOpen(true)} className={cn('inline-flex items-center gap-2 rounded-xl px-5 py-3 text-sm font-semibold shadow-lg', pending === 0 ? 'bg-lime-400 text-black' : 'bg-white/10 text-white ring-1 ring-white/20')}><LockKeyhole className="size-4" /> {lab?.isPublished ? 'Republish Marks' : 'Publish Marks'}</button>
        </div>
      </div>
      {problem && <div className="mb-5"><ProblemBanner problem={problem} /></div>}
      {receipt && <div className="mb-5 flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-800"><CheckCircle2 className="size-5" /><div><strong className="block">Marks published successfully</strong><span className="text-xs">Receipt for lab #{receipt.labId} · {receipt.submissionCount} submissions · by {receipt.publishedBy}</span></div></div>}
      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <MiniStat label="Violations" value={violations} tone="rose" />
        <MiniStat label="Unsure / guardrail" value={unsure} tone="amber" />
        <MiniStat label="Pass audit sample" value={items.filter((item) => item.sampledPass).length} tone="blue" />
      </div>
      <div className="grid gap-5 xl:grid-cols-[370px_1fr]">
        <aside className="surface-card h-fit p-0 xl:sticky xl:top-24">
          <div className="border-b border-slate-100 p-4"><div className="flex flex-wrap gap-1">{(['all', 'unreviewed', 'violation', 'unsure', 'pass'] as Filter[]).map((item) => <button key={item} onClick={() => setFilter(item)} className={cn('rounded-lg px-2.5 py-1.5 text-[10px] font-semibold capitalize', filter === item ? 'bg-lime-600 text-white' : 'bg-slate-50 text-slate-500')}>{item}</button>)}</div></div>
          <div className="max-h-[680px] space-y-2 overflow-auto p-3">{visible.map((item) => <button key={keyOf(item)} onClick={() => setSelectedKey(keyOf(item))} className={cn('w-full rounded-2xl border p-4 text-left transition', keyOf(selected) === keyOf(item) ? 'border-lime-300 bg-lime-50 shadow-sm' : 'border-transparent bg-slate-50 hover:border-slate-200')}>
            <div className="flex items-center justify-between"><span className="font-mono text-[11px] font-semibold text-slate-700">#{item.submissionId} · {item.constraintId}</span><StatusBadge value={item.verdict} /></div>
            <p className="mt-2 text-xs font-medium text-slate-700">{item.studentId}</p>
            <div className="mt-3 flex items-center justify-between text-[10px]"><span className="text-slate-400">{sourceLabel(item.source)}</span><span className={item.decision ? 'text-emerald-600' : 'text-amber-600'}>{item.decision ? `Marked ${item.decision}` : 'Pending decision'}</span></div>
          </button>)}</div>
        </aside>
        {selected && <section className="space-y-5">
          <div className="surface-card">
            <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between"><div><p className="eyebrow">Constraint {selected.constraintId} · {selected.constraintType}</p><h2 className="mt-2 max-w-2xl text-lg font-semibold text-slate-950">{selected.constraintDescription}</h2></div><div className="rounded-2xl bg-rose-50 px-4 py-3 text-right"><span className="block text-[10px] uppercase tracking-wide text-rose-500">TA-defined deduction</span><strong className="text-rose-700">{formatDeduction(selected.deductionTenths)}</strong></div></div>
            {selected.source === 'hard_rule' && <div className="mt-5 flex items-center gap-2 rounded-xl bg-lime-50 p-3 text-xs font-medium text-lime-700"><ShieldAlert className="size-4" /> Decided by Syntax-Aware Hard Rule · Model not called</div>}
            {selected.guardrailReason && <div className="mt-5 flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs font-medium text-amber-800"><AlertTriangle className="size-4" /> Guardrail returned unsure: {selected.guardrailReason.replace(/_/g, ' ')}</div>}
            {selected.sampledPass && <div className="mt-5 flex items-center gap-2 rounded-xl bg-emerald-50 p-3 text-xs font-medium text-emerald-800"><Sparkles className="size-4" /> Random 10% Pass Audit Sample · Server-selected</div>}
          </div>
          <div className="grid gap-5 lg:grid-cols-[230px_1fr]">
            <div className="surface-card h-fit"><p className="eyebrow">Evidence lines</p><div className="mt-4 space-y-2">{selected.evidence.length ? selected.evidence.map((evidence) => <a key={evidence.line} href={`#line-${evidence.line}`} className="block rounded-xl border border-slate-100 bg-slate-50 p-3 font-mono text-[10px] text-slate-600 hover:border-lime-200"><span className="mb-1 block text-lime-600">Line {evidence.line} <ExternalLink className="inline size-2.5" /></span>{evidence.text}</a>) : <p className="text-xs text-slate-400">No trustworthy evidence returned.</p>}</div></div>
            <CppCodeViewer sourceCode={selected.sourceCode} strippedCode={selected.strippedCode} evidence={selected.evidence} />
          </div>
          <div className="sticky bottom-4 flex flex-col gap-3 rounded-2xl border border-white bg-white/90 p-4 shadow-2xl shadow-lime-950/10 backdrop-blur md:flex-row md:items-center">
            <div className="flex-1"><strong className="block text-sm text-slate-900">TA Decision</strong><span className="text-[11px] text-slate-400">Your decision controls the final deduction.</span></div>
            <button onClick={() => void decide('pass')} className="secondary-button justify-center text-emerald-700"><Check className="size-4" /> Mark as Pass</button>
            <button onClick={() => void decide('violation')} className="danger-button justify-center"><ShieldAlert className="size-4" /> Confirm Violation · {formatDeduction(selected.deductionTenths)}</button>
          </div>
        </section>}
      </div>
      {publishOpen && <Modal title={lab?.isPublished ? 'Republish marks?' : 'Publish marks?'} onClose={() => setPublishOpen(false)}>
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"><strong className="block">{lab?.isPublished ? 'Student marks will be recalculated.' : 'This action is irreversible.'}</strong><span className="mt-1 block text-xs">{lab?.isPublished ? 'The latest TA decisions will replace currently published deductions.' : 'Published marks and deductions immediately become visible to students.'}</span></div>
        <div className="my-5 space-y-3 text-sm">{[['Deadline reached', true], ['Judging complete', true], ['All reviews completed', pending === 0]].map(([label, ready]) => <div key={String(label)} className="flex items-center justify-between"><span>{label}</span><StatusBadge value={ready ? 'pass' : 'pending'} label={ready ? 'Ready' : `${pending} pending`} /></div>)}</div>
        <button onClick={() => void publish()} className="primary-button w-full justify-center"><Send className="size-4" /> {lab?.isPublished ? 'Confirm Republish' : 'Confirm Publish'} <ArrowRight className="size-4" /></button>
      </Modal>}
    </div>
  )
}

const keyOf = (item: ReviewItem) => `${item.submissionId}-${item.constraintId}`
const sourceLabel = (source: ReviewItem['source']) => ({ hard_rule: 'Hard Rule', llm: 'LLM', guardrail: 'Guardrail' })[source]
function MiniStat({ label, value, tone }: { label: string; value: number; tone: 'rose' | 'amber' | 'blue' }) {
  const styles = { rose: 'bg-rose-50 text-rose-700', amber: 'bg-amber-50 text-amber-700', blue: 'bg-lime-50 text-lime-700' }
  return <div className="surface-card flex items-center justify-between py-4"><span className="text-xs font-medium text-slate-500">{label}</span><strong className={`rounded-xl px-3 py-1.5 ${styles[tone]}`}>{value}</strong></div>
}
