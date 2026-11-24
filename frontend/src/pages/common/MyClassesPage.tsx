import { useEffect, useMemo, useState } from 'react'
import { ArrowRight, BookOpen, CheckCircle2, Layers3, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { classService } from '@/services/classService'
import { labService } from '@/services/labService'
import { useAuthStore } from '@/stores/authStore'
import type { ClassRoom } from '@/types/admin'
import type { LabSummary } from '@/types/lab'
import { LoadingState } from '@/components/common/LoadingState'
import { EmptyState } from '@/components/common/EmptyState'

export function MyClassesPage() {
  const user = useAuthStore((state) => state.user)
  const [classes, setClasses] = useState<ClassRoom[] | null>(null)
  const [labs, setLabs] = useState<LabSummary[]>([])
  const navigate = useNavigate()

  useEffect(() => {
    if (!user) return
    Promise.all([classService.getMyClasses(user), labService.getLabs(user)])
      .then(([classItems, labItems]) => {
        setClasses(classItems)
        setLabs(labItems)
      })
      .catch(() => setClasses([]))
  }, [user])

  const labsByClass = useMemo(() => new Map((classes ?? []).map((classroom) => [
    classroom.id,
    labs.filter((lab) => lab.classId === classroom.id),
  ])), [classes, labs])

  if (!user) return null
  const isTa = user.role === 'ta'

  return (
    <div>
      <div className="page-header"><div><p className="eyebrow">{isTa ? 'Teaching workspace' : 'Student workspace'}</p><h1>My Classes</h1><p>Select a class to view only its labs and activity.</p></div></div>
      {!classes ? <LoadingState /> : classes.length === 0 ? <EmptyState title="No classes assigned" detail="Your account has not been assigned to a class yet." /> : (
        <div className="grid gap-4 xl:grid-cols-2">
          {classes.map((classroom) => {
            const classLabs = labsByClass.get(classroom.id) ?? []
            const openCount = classLabs.filter((lab) => !lab.isPublished).length
            const secondaryCount = isTa
              ? classLabs.reduce((sum, lab) => sum + lab.unreviewedCount, 0)
              : classLabs.filter((lab) => lab.isPublished).length
            return <article key={classroom.id} className="surface-card">
              <div className="flex items-start justify-between gap-4">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-lime-100 text-lime-700"><Layers3 className="size-5" /></span>
                <span className="rounded-full bg-slate-100 px-3 py-1 font-mono text-[10px] text-slate-500">CLASS {classroom.id}</span>
              </div>
              <h2 className="mt-4 text-lg font-semibold text-slate-950">{classroom.name}</h2>
              <div className="mt-4 grid grid-cols-3 gap-2">
                <ClassMetric icon={<BookOpen />} value={classLabs.length} label="Total labs" />
                <ClassMetric icon={<CheckCircle2 />} value={openCount} label="Open labs" />
                <ClassMetric icon={<ShieldCheck />} value={secondaryCount} label={isTa ? 'Pending reviews' : 'Published marks'} />
              </div>
              <button onClick={() => navigate(`/${user.role}/labs?classId=${classroom.id}`)} className="primary-button mt-4 w-full justify-center">View Labs <ArrowRight className="size-4" /></button>
            </article>
          })}
        </div>
      )}
    </div>
  )
}

function ClassMetric({ icon, value, label }: { icon: React.ReactElement; value: number; label: string }) {
  return <div className="rounded-xl bg-slate-50 p-3"><span className="text-lime-600 [&_svg]:size-3.5">{icon}</span><strong className="mt-2 block text-lg text-slate-900">{value}</strong><span className="text-[9px] uppercase tracking-wide text-slate-400">{label}</span></div>
}
