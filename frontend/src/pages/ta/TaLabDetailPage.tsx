import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, CircleDollarSign, Clock3, FilePenLine, MessageSquareText, Send, Users } from 'lucide-react'
import { labService } from '@/services/labService'
import { submissionService } from '@/services/submissionService'
import { useAuthStore } from '@/stores/authStore'
import type { LabDetail } from '@/types/lab'
import type { SubmissionSummary } from '@/types/submission'
import { StatusBadge } from '@/components/common/StatusBadge'
import { CostCapBar } from '@/components/labs/CostCapBar'
import { formatScoreTenths, formatUsd, formatUtcDate } from '@/lib/utils'

export function TaLabDetailPage() {
  const { labId } = useParams()
  const user = useAuthStore((state) => state.user)
  const [lab, setLab] = useState<LabDetail | null>(null)
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([])
  const navigate = useNavigate()
  useEffect(() => { const id = Number(labId); if (user && id) { labService.getLab(id, user).then(setLab).catch(() => navigate('/not-found')); submissionService.list(id).then((items) => setSubmissions(items.filter((item) => item.isCounted))).catch(() => setSubmissions([])) } }, [labId, user, navigate])
  if (!lab) return <div className="h-80 animate-pulse rounded-3xl bg-white/70" />
  return (
    <div>
      <div className="page-header"><div><p className="eyebrow">{lab.className}</p><h1>{lab.title}</h1><p>Deadline · {formatUtcDate(lab.deadline)}</p></div><div className="flex flex-wrap gap-2"><button onClick={() => navigate(`/ta/labs/${lab.id}/edit`)} className="secondary-button"><FilePenLine className="size-4" /> {lab.isPublished ? 'View Configuration' : 'Edit'}</button><button onClick={() => navigate(`/ta/labs/${lab.id}/appeals`)} className="secondary-button"><MessageSquareText className="size-4" /> Appeals</button><button onClick={() => navigate(`/ta/labs/${lab.id}/review-queue`)} className="primary-button"><Send className="size-4" /> Review Queue</button></div></div>
      <div className="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <Kpi icon={<Users />} value={String(lab.submissionCount)} label="Counted submissions" note={`${lab.judgingPendingCount} still judging`} />
        <Kpi icon={<Clock3 />} value={`${lab.unreviewedCount}`} label="Pending TA reviews" note="Human gate status" />
        <div className="surface-card"><CircleDollarSign className="size-5 text-lime-600" /><p className="mt-5 text-2xl font-semibold">{formatUsd(lab.costSpentUsd)}</p><p className="mb-4 text-xs text-slate-400">LLM cost usage</p><CostCapBar spent={lab.costSpentUsd} cap={lab.costCapUsd} /></div>
        <Kpi icon={<CheckCircle2 />} value={lab.isPublished ? 'Published' : 'Unpublished'} label="Publication status" note={lab.publishedAt ? formatUtcDate(lab.publishedAt) : 'Awaiting completed review'} />
      </div>
      <div className="surface-card overflow-hidden p-0">
        <div className="border-b border-slate-100 p-5"><h2 className="font-semibold text-slate-950">Counted submissions</h2><p className="mt-1 text-xs text-slate-400">Accepted sandbox results eligible for constraint review.</p></div>
        <div className="overflow-x-auto"><table><thead><tr><th>Submission</th><th>Student</th><th>Judge</th><th>Raw score</th><th>Tests</th><th>Submitted</th></tr></thead><tbody>{submissions.map((item) => <tr key={item.id}><td className="font-mono text-xs">#{item.id}</td><td>{item.studentName ?? item.studentId}</td><td><StatusBadge value={item.judgeStatus} /></td><td>{formatScoreTenths(item.scoreTenths)}</td><td>{item.passedTests}/{item.totalTests}</td><td>{formatUtcDate(item.submittedAt)}</td></tr>)}</tbody></table></div>
      </div>
    </div>
  )
}

function Kpi({ icon, value, label, note }: { icon: React.ReactElement; value: string; label: string; note: string }) {
  return <div className="surface-card"><span className="grid size-9 place-items-center rounded-xl bg-lime-50 text-lime-600 [&_svg]:size-4">{icon}</span><p className="mt-5 text-2xl font-semibold text-slate-950">{value}</p><p className="mt-1 text-xs font-semibold text-slate-600">{label}</p><p className="mt-1 text-[11px] text-slate-400">{note}</p></div>
}
