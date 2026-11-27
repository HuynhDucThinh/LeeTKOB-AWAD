import { useEffect, useState } from 'react'
import { Plus, Save, Trash2 } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { labService } from '@/services/labService'
import { classService } from '@/services/classService'
import { useAuthStore } from '@/stores/authStore'
import type { CreateLabPayload, LabConstraint, LabTestCase } from '@/types/lab'
import type { ClassRoom } from '@/types/admin'

const emptyTest = (): Omit<LabTestCase, 'id'> => ({
  name: 'Sample test',
  input: '',
  expectedOutput: '',
  timeLimitMs: 2000,
  memoryLimitMb: 256,
  isSample: true,
})

export function TaLabFormPage() {
  const { labId } = useParams()
  const user = useAuthStore((state) => state.user)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [classes, setClasses] = useState<ClassRoom[]>([])
  const [classId, setClassId] = useState(0)
  const [title, setTitle] = useState('')
  const [deadline, setDeadline] = useState('2026-12-20T22:00')
  const [costCapUsd, setCostCapUsd] = useState(1.5)
  const [statementMd, setStatementMd] = useState('# Problem\n\nDescribe the programming task here.')
  const [tests, setTests] = useState<Array<Omit<LabTestCase, 'id'>>>([emptyTest()])
  const [constraints, setConstraints] = useState<LabConstraint[]>([
    { constraintId: 'C1', type: 'hard', description: 'Do not use `std::sort`.', deductionTenths: 30 },
  ])
  const [saving, setSaving] = useState(false)
  const [published, setPublished] = useState(false)

  useEffect(() => {
    if (!user) return
    classService.getMyClasses(user).then((items) => {
      setClasses(items)
      if (!labId) {
        const requestedClassId = Number(searchParams.get('classId'))
        setClassId(user.classIds.includes(requestedClassId) ? requestedClassId : (user.classIds[0] ?? 0))
      }
    }).catch(() => setClasses([]))
  }, [labId, searchParams, user])

  useEffect(() => {
    if (!labId || !user) return
    labService.getLab(Number(labId), user).then((lab) => {
      setTitle(lab.title)
      setDeadline(lab.deadline.slice(0, 16))
      setCostCapUsd(lab.costCapUsd)
      setStatementMd(lab.statementMd)
      setTests(lab.tests.map(({ id: _id, ...test }) => test))
      setConstraints(lab.constraints)
      setPublished(lab.isPublished)
      setClassId(lab.classId)
    }).catch(() => navigate('/not-found'))
  }, [labId, user, navigate])

  const save = async () => {
    if (!title.trim() || tests.length === 0 || !user?.classIds.includes(classId)) return
    setSaving(true)
    const payload: CreateLabPayload = {
      classId,
      title: title.trim(),
      deadline: new Date(deadline).toISOString(),
      costCapUsd,
      statementMd,
      tests,
      constraints,
    }
    try {
      const saved = await labService.save(payload, labId ? Number(labId) : undefined)
      navigate(`/ta/labs/${saved.id}`)
    } finally {
      setSaving(false)
    }
  }

  const updateTest = (index: number, patch: Partial<Omit<LabTestCase, 'id'>>) =>
    setTests((items) => items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item))
  const updateConstraint = (index: number, patch: Partial<LabConstraint>) =>
    setConstraints((items) => items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item))

  return (
    <div className="mx-auto max-w-4xl">
      <div className="page-header"><div><p className="eyebrow">Lab configuration</p><h1>{labId ? 'Edit lab' : 'Create a new lab'}</h1><p>Define the problem, sandbox tests and review constraints.</p></div><button onClick={() => navigate(`/ta/labs${classId ? `?classId=${classId}` : ''}`)} className="secondary-button">Cancel</button></div>
      {published && <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800"><strong className="block">Published lab · read-only configuration</strong><span className="mt-1 block">Published labs cannot be edited. You can still inspect all tests and constraints here.</span></div>}
      <fieldset disabled={published} className="space-y-4 disabled:opacity-75">
        <Section number="01" title="Basic information" detail="Class ownership, deadline and model budget.">
          <div className="grid gap-3 md:grid-cols-2"><label className="field-label">Class<select className="input mt-1.5" value={classId} onChange={(event) => setClassId(Number(event.target.value))}>{classes.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}</select></label><label className="field-label">Lab title<input className="input mt-1.5" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Lab 03: Quick Sort" /></label><label className="field-label">Deadline<input className="input mt-1.5" type="datetime-local" value={deadline} onChange={(event) => setDeadline(event.target.value)} /></label><label className="field-label">LLM cost cap (USD)<input className="input mt-1.5" type="number" min="0" step="0.01" value={costCapUsd} onChange={(event) => setCostCapUsd(Number(event.target.value))} /></label></div>
        </Section>
        <Section number="02" title="Problem statement" detail="Markdown is supported in the student-facing statement."><textarea className="input min-h-36 font-mono text-xs" value={statementMd} onChange={(event) => setStatementMd(event.target.value)} /></Section>
        <Section number="03" title="Test cases" detail="At least one sandbox test is required.">
          <div className="space-y-3">{tests.map((test, index) => <div key={index} className="rounded-xl border border-slate-200 p-4">
            <div className="mb-3 flex gap-3"><input className="input py-2" value={test.name} onChange={(event) => updateTest(index, { name: event.target.value })} /><button onClick={() => setTests((items) => items.filter((_, itemIndex) => itemIndex !== index))} className="icon-button text-rose-500" aria-label="Remove test"><Trash2 className="size-4" /></button></div>
            <div className="grid gap-3 md:grid-cols-2"><textarea className="input h-24 font-mono text-xs" value={test.input} onChange={(event) => updateTest(index, { input: event.target.value })} placeholder="Input" /><textarea className="input h-24 font-mono text-xs" value={test.expectedOutput} onChange={(event) => updateTest(index, { expectedOutput: event.target.value })} placeholder="Expected output" /></div>
            <label className="mt-3 flex items-center gap-2 text-xs text-slate-500"><input type="checkbox" checked={test.isSample} onChange={(event) => updateTest(index, { isSample: event.target.checked })} className="accent-lime-500" /> Show as sample</label>
          </div>)}</div>
          <button onClick={() => setTests((items) => [...items, emptyTest()])} className="secondary-button mt-3"><Plus className="size-4" /> Add test case</button>
        </Section>
        <Section number="04" title="Constraints" detail="Models review semantic rules; they never choose deductions.">
          <div className="space-y-3">{constraints.map((constraint, index) => <div key={constraint.constraintId} className="rounded-xl border border-lime-200 bg-lime-50/50 p-4">
            <div className="grid gap-3 md:grid-cols-[120px_1fr_110px_40px]"><select className="input" value={constraint.type} onChange={(event) => updateConstraint(index, { type: event.target.value as LabConstraint['type'] })}><option value="hard">Hard rule</option><option value="semantic">Semantic</option></select><input className="input" value={constraint.description} onChange={(event) => updateConstraint(index, { description: event.target.value })} /><input className="input" type="number" min="0" max="10" step=".1" value={constraint.deductionTenths / 10} onChange={(event) => updateConstraint(index, { deductionTenths: Math.round(Number(event.target.value) * 10) })} /><button onClick={() => setConstraints((items) => items.filter((_, itemIndex) => itemIndex !== index).map((item, nextIndex) => ({ ...item, constraintId: `C${nextIndex + 1}` })))} className="icon-button text-rose-500" aria-label="Remove constraint"><Trash2 className="size-4" /></button></div>
            <p className="mt-2 text-[10px] text-lime-800">Hard rules begin with “Do not use” or “Do not call”. Deduction is set by the TA.</p>
          </div>)}</div>
          <button onClick={() => setConstraints((items) => [...items, { constraintId: `C${items.length + 1}`, type: 'semantic', description: '', deductionTenths: 30 }])} className="secondary-button mt-3"><Plus className="size-4" /> Add constraint</button>
        </Section>
      </fieldset>
      <div className="mt-4 flex justify-end">{published ? <button type="button" onClick={() => navigate(`/ta/labs/${labId}`)} className="secondary-button">Back to Overview</button> : <button type="button" onClick={() => void save()} disabled={saving || !title.trim() || tests.length === 0} className="primary-button"><Save className="size-4" /> {saving ? 'Saving...' : 'Save Lab'}</button>}</div>
    </div>
  )
}

function Section({ number, title, detail, children }: { number: string; title: string; detail: string; children: React.ReactNode }) {
  return <section className="surface-card"><div className="mb-5 flex items-start gap-4"><span className="font-mono text-xs font-semibold text-lime-600">{number}</span><div><h2 className="font-semibold text-slate-950">{title}</h2><p className="mt-1 text-xs text-slate-500">{detail}</p></div></div>{children}</section>
}
