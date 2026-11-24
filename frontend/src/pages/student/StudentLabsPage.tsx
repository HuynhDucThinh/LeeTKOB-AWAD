import { useEffect, useMemo, useState } from 'react'
import { ArrowRight, CalendarDays, CheckCircle2, Clock3 } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { labService } from '@/services/labService'
import { submissionService } from '@/services/submissionService'
import { classService } from '@/services/classService'
import { useAuthStore } from '@/stores/authStore'
import type { LabSummary } from '@/types/lab'
import type { SubmissionSummary } from '@/types/submission'
import type { ClassRoom } from '@/types/admin'
import { formatScoreTenths, formatUtcDate } from '@/lib/utils'
import { StatusBadge } from '@/components/common/StatusBadge'
import { EmptyState } from '@/components/common/EmptyState'

export function StudentLabsPage() {
  const user = useAuthStore((state) => state.user)
  const [labs, setLabs] = useState<LabSummary[]>([])
  const [classes, setClasses] = useState<ClassRoom[]>([])
  const [latestByLab, setLatestByLab] = useState<Record<number, SubmissionSummary | undefined>>({})
  const [statusFilter, setStatusFilter] = useState<'all' | 'open' | 'published'>('all')
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedClassId = Number(searchParams.get('classId'))
  const selectedClassId = user?.classIds.includes(requestedClassId) ? requestedClassId : null
  useEffect(() => {
    if (!user) return
    Promise.all([labService.getLabs(user), classService.getMyClasses(user)]).then(async ([items, classItems]) => {
      setLabs(items)
      setClasses(classItems)
      const entries = await Promise.all(items.map(async (lab) => {
        const submissions = await submissionService.list(lab.id, user.id)
        return [lab.id, submissions.sort((a, b) => b.submittedAt.localeCompare(a.submittedAt))[0]] as const
      }))
      setLatestByLab(Object.fromEntries(entries))
    }).catch(() => setLabs([]))
  }, [user])
  const visibleLabs = useMemo(() => labs.filter((lab) =>
    (selectedClassId === null || lab.classId === selectedClassId) &&
    (statusFilter === 'all' || (statusFilter === 'published' ? lab.isPublished : !lab.isPublished))
  ), [labs, selectedClassId, statusFilter])
  const selectClass = (value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value === 'all') next.delete('classId')
    else next.set('classId', value)
    setSearchParams(next)
  }
  return (
    <div><div className="page-header"><div><p className="eyebrow">Student workspace</p><h1>Your programming labs</h1><p>Submit C++ solutions and follow your judge results in real time.</p></div></div>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <label className="flex items-center gap-2 rounded-xl border border-white bg-white/80 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wide text-slate-400 shadow-sm">Class
          <select value={selectedClassId ?? 'all'} onChange={(event) => selectClass(event.target.value)} className="bg-transparent py-1 text-xs font-semibold normal-case tracking-normal text-slate-700 outline-none">
            <option value="all">All Classes</option>
            {classes.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}
          </select>
        </label>
        <div className="flex items-center gap-1 rounded-xl bg-white/70 p-1 shadow-sm ring-1 ring-slate-200/70 sm:w-fit">{([['all', 'All labs'], ['open', 'Open'], ['published', 'Published']] as const).map(([value, label]) => <button key={value} onClick={() => setStatusFilter(value)} className={`rounded-lg px-3.5 py-1.5 text-[11px] font-semibold ${statusFilter === value ? 'bg-slate-950 text-lime-300 shadow-md' : 'text-slate-500'}`}>{label}</button>)}</div>
      </div>
      {visibleLabs.length === 0 ? <EmptyState title="No matching labs" detail="There are no labs for this class and status filter." /> : <div className="grid gap-4 xl:grid-cols-2">{visibleLabs.map((lab) => {
        const latest = latestByLab[lab.id]
        return <article key={lab.id} className="surface-card">
        <div className="flex items-start justify-between"><span className="rounded-lg bg-lime-50 px-2.5 py-1 text-[10px] font-semibold text-lime-700">{lab.className}</span>{lab.isPublished ? <span className="flex items-center gap-1 text-[11px] font-semibold text-emerald-600"><CheckCircle2 className="size-3" /> Marks published</span> : <span className="flex items-center gap-1 text-[11px] font-semibold text-amber-600"><Clock3 className="size-3" /> Pending publication</span>}</div>
        <h2 className="mt-5 text-xl font-semibold text-slate-950">{lab.title}</h2><p className="mt-2 flex items-center gap-2 text-xs text-slate-500"><CalendarDays className="size-3.5" /> Due {formatUtcDate(lab.deadline)}</p>
        <div className="my-4 rounded-xl bg-slate-50 p-4"><span className="text-[10px] uppercase tracking-wide text-slate-400">Latest submission</span><div className="mt-2 flex items-center justify-between gap-3">{latest ? <><div><StatusBadge value={latest.judgeStatus} /><span className="ml-2 text-xs text-slate-500">{latest.passedTests}/{latest.totalTests} tests</span></div><strong className="text-sm text-slate-900">{lab.isPublished && latest.publishedScoreTenths !== null ? formatScoreTenths(latest.publishedScoreTenths) : 'Pending publication'}</strong></> : <span className="text-xs text-slate-500">Not submitted</span>}</div></div>
        <button onClick={() => navigate(`/student/labs/${lab.id}`)} className="primary-button w-full justify-center">Open Lab <ArrowRight className="size-4" /></button>
      </article>})}</div>}
    </div>
  )
}
