import { useEffect, useMemo, useState } from 'react'
import { ArrowUpRight, CalendarDays, CheckCircle2, Clock3, Plus, ShieldCheck } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { labService } from '@/services/labService'
import { classService } from '@/services/classService'
import { useAuthStore } from '@/stores/authStore'
import type { LabSummary } from '@/types/lab'
import type { ClassRoom } from '@/types/admin'
import { formatUtcDate } from '@/lib/utils'
import { LoadingState } from '@/components/common/LoadingState'
import { CostCapBar } from '@/components/labs/CostCapBar'
import { EmptyState } from '@/components/common/EmptyState'

export function TaLabsPage() {
  const user = useAuthStore((state) => state.user)
  const [labs, setLabs] = useState<LabSummary[] | null>(null)
  const [classes, setClasses] = useState<ClassRoom[]>([])
  const [filter, setFilter] = useState<'all' | 'review' | 'published'>('all')
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedClassId = Number(searchParams.get('classId'))
  const selectedClassId = user?.classIds.includes(requestedClassId) ? requestedClassId : null
  useEffect(() => {
    if (!user) return
    Promise.all([labService.getLabs(user), classService.getMyClasses(user)])
      .then(([labItems, classItems]) => {
        setLabs(labItems)
        setClasses(classItems)
      })
      .catch(() => setLabs([]))
  }, [user])
  const scopedLabs = useMemo(() => (labs ?? []).filter((lab) => selectedClassId === null || lab.classId === selectedClassId), [labs, selectedClassId])
  const visible = useMemo(() => scopedLabs.filter((lab) => filter === 'all' || (filter === 'review' ? lab.unreviewedCount > 0 : lab.isPublished)), [scopedLabs, filter])
  const pending = scopedLabs.reduce((sum, lab) => sum + lab.unreviewedCount, 0)
  const selectClass = (value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value === 'all') next.delete('classId')
    else next.set('classId', value)
    setSearchParams(next)
  }
  return (
    <div>
      <div className="page-header"><div><p className="eyebrow">Teaching workspace</p><h1>My Labs</h1><p>Monitor submissions, review constraints and publish marks safely.</p></div><button onClick={() => navigate(`/ta/labs/new${selectedClassId === null ? '' : `?classId=${selectedClassId}`}`)} className="primary-button"><Plus className="size-4" /> Create Lab</button></div>
      <div className="mb-5 grid gap-3 md:grid-cols-3">
        <Summary icon={<ShieldCheck />} label="Pending reviews" value={String(pending)} note="Require a TA decision" accent />
        <Summary icon={<CheckCircle2 />} label="Published labs" value={String(scopedLabs.filter((lab) => lab.isPublished).length)} note="Marks visible to students" />
        <Summary icon={<Clock3 />} label="Active submissions" value={String(scopedLabs.reduce((sum, lab) => sum + lab.judgingPendingCount, 0))} note="Queued or running" />
      </div>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <label className="flex items-center gap-2 rounded-xl border border-white bg-white/80 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wide text-slate-400 shadow-sm">Class
          <select value={selectedClassId ?? 'all'} onChange={(event) => selectClass(event.target.value)} className="bg-transparent py-1 text-xs font-semibold normal-case tracking-normal text-slate-700 outline-none">
            <option value="all">All Classes</option>
            {classes.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}
          </select>
        </label>
        <div className="flex items-center gap-1 rounded-xl bg-white/70 p-1 shadow-sm ring-1 ring-slate-200/70 sm:w-fit">{([['all', 'All labs'], ['review', 'Needs review'], ['published', 'Published']] as const).map(([value, label]) => <button key={value} onClick={() => setFilter(value)} className={`rounded-lg px-3.5 py-1.5 text-[11px] font-semibold ${filter === value ? 'bg-slate-950 text-lime-300 shadow-md' : 'text-slate-500'}`}>{label}</button>)}</div>
      </div>
      {!labs ? <LoadingState /> : visible.length === 0 ? <EmptyState title="No matching labs" detail="This class and status filter has no labs yet." action={<button onClick={() => navigate(`/ta/labs/new${selectedClassId === null ? '' : `?classId=${selectedClassId}`}`)} className="primary-button">Create Lab</button>} /> : (
        <div className="grid gap-4 xl:grid-cols-2">
          {visible.map((lab) => <article key={lab.id} className="surface-card flex flex-col">
            <div className="flex items-start justify-between gap-4">
              <div><span className="mb-2 inline-flex rounded-lg bg-lime-50 px-2.5 py-1 text-[9px] font-semibold uppercase tracking-wide text-lime-700">{lab.className}</span><h2 className="text-lg font-semibold text-slate-950">{lab.title}</h2></div>
              <span className={`rounded-full px-3 py-1 text-[11px] font-semibold ${lab.isPublished ? 'bg-emerald-50 text-emerald-700' : 'bg-lime-50 text-lime-700'}`}>{lab.isPublished ? 'Published' : 'Open'}</span>
            </div>
            <div className="mt-4 flex items-center gap-2 text-xs text-slate-500"><CalendarDays className="size-3.5" /> Deadline · {formatUtcDate(lab.deadline)}</div>
            <div className="my-4 grid grid-cols-3 divide-x divide-slate-100 rounded-xl bg-slate-50 py-3 text-center">
              <div><strong className="block text-lg text-slate-900">{lab.submissionCount}</strong><span className="text-[10px] text-slate-400">Submissions</span></div>
              <div><strong className="block text-lg text-amber-600">{lab.unreviewedCount}</strong><span className="text-[10px] text-slate-400">Unreviewed</span></div>
              <div><strong className="block text-lg text-slate-900">{lab.judgingPendingCount}</strong><span className="text-[10px] text-slate-400">Judging</span></div>
            </div>
            <CostCapBar spent={lab.costSpentUsd} cap={lab.costCapUsd} />
            <div className="mt-4 flex gap-2">
              <button onClick={() => navigate(`/ta/labs/${lab.id}`)} className="secondary-button flex-1 justify-center">Overview</button>
              <button onClick={() => navigate(`/ta/labs/${lab.id}/review-queue`)} className="primary-button flex-1 justify-center">Review Queue {lab.unreviewedCount > 0 && <span className="rounded-full bg-white/20 px-1.5">{lab.unreviewedCount}</span>}<ArrowUpRight className="size-3.5" /></button>
            </div>
          </article>)}
        </div>
      )}
    </div>
  )
}

function Summary({ icon, label, value, note, accent = false }: { icon: React.ReactElement; label: string; value: string; note: string; accent?: boolean }) {
  return <div className={`rounded-2xl border p-4 shadow-sm ${accent ? 'border-slate-800 bg-[#121610] text-white' : 'border-white bg-white/80'}`}><div className="flex items-center justify-between"><span className={`grid size-8 place-items-center rounded-xl [&_svg]:size-4 ${accent ? 'bg-lime-400 text-black' : 'bg-lime-50 text-lime-700'}`}>{icon}</span><strong className={accent ? 'text-2xl text-lime-300' : 'text-2xl'}>{value}</strong></div><p className={`mt-3 text-xs font-semibold ${accent ? 'text-white' : 'text-slate-800'}`}>{label}</p><p className="mt-1 text-[10px] text-slate-400">{note}</p></div>
}
