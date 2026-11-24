import { useEffect, useRef, useState } from 'react'
import { Check, CircleDot, FileCode2, LockKeyhole, MessageSquareText, Radio, Send, ShieldCheck, Upload } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { labService } from '@/services/labService'
import { submissionService } from '@/services/submissionService'
import { appealService } from '@/services/appealService'
import { useAuthStore } from '@/stores/authStore'
import type { LabDetail } from '@/types/lab'
import type { SubmissionDetail, SubmissionSummary } from '@/types/submission'
import type { AppealItem } from '@/types/appeal'
import { StatusBadge } from '@/components/common/StatusBadge'
import { Modal } from '@/components/common/Modal'
import { formatScoreTenths, formatUtcDate } from '@/lib/utils'

const starterCode = `#include <iostream>
#include <vector>
using namespace std;

void mergeSort(vector<int>& a, int left, int right) {
  // Write your solution here
}

int main() {
  return 0;
}`

export function StudentLabDetailPage() {
  const { labId } = useParams()
  const user = useAuthStore((state) => state.user)
  const [lab, setLab] = useState<LabDetail | null>(null)
  const [code, setCode] = useState(starterCode)
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([])
  const [submissionDetail, setSubmissionDetail] = useState<SubmissionDetail | null>(null)
  const [appeals, setAppeals] = useState<AppealItem[]>([])
  const [appealTarget, setAppealTarget] = useState<NonNullable<SubmissionDetail['publishedDeductions']>[number] | null>(null)
  const [appealReason, setAppealReason] = useState('')
  const [live, setLive] = useState<SubmissionSummary | null>(null)
  const [uploadError, setUploadError] = useState('')
  const timers = useRef<number[]>([])
  const navigate = useNavigate()
  useEffect(() => {
    const id = Number(labId)
    if (user && id) {
      labService.getLab(id, user).then(setLab).catch(() => navigate('/not-found'))
      submissionService.list(id, user.id).then((items) => {
        const sorted = items.sort((a, b) => b.submittedAt.localeCompare(a.submittedAt))
        setSubmissions(sorted)
        const latest = sorted[0]
        if (latest) submissionService.get(latest.id).then(setSubmissionDetail).catch(() => setSubmissionDetail(null))
      }).catch(() => setSubmissions([]))
      appealService.list(id, user.id).then(setAppeals).catch(() => setAppeals([]))
    }
    return () => timers.current.forEach(window.clearTimeout)
  }, [labId, user, navigate])

  const submit = async () => {
    if (!user || !lab) return
    const queued = await submissionService.submit(lab.id, user.id, code)
    setLive(queued)
    setSubmissions((items) => [queued, ...items])
    submissionService.get(queued.id).then(setSubmissionDetail).catch(() => undefined)
    timers.current.push(window.setTimeout(() => { submissionService.mockAdvance(queued.id, 'running').then(setLive).catch(() => undefined) }, 1000))
    timers.current.push(window.setTimeout(() => { submissionService.mockAdvance(queued.id, 'AC').then((result) => { setLive(result); setSubmissions((items) => items.map((item) => item.id === result.id ? result : item)) }).catch(() => undefined) }, 2500))
  }

  const createAppeal = async () => {
    if (!user || !submissionDetail || !appealTarget || !appealReason.trim()) return
    const created = await appealService.create(submissionDetail.id, user.id, appealTarget.constraintId, appealReason.trim())
    setAppeals((items) => items.some((item) => item.id === created.id) ? items : [created, ...items])
    setAppealTarget(null)
    setAppealReason('')
  }

  if (!lab) return <div className="h-96 animate-pulse rounded-3xl bg-white/70" />
  return (
    <div>
      <div className="page-header"><div><button onClick={() => navigate('/student/labs')} className="eyebrow">← Back to labs</button><h1>{lab.title}</h1><p>{lab.className} · Due {formatUtcDate(lab.deadline)}</p></div><StatusBadge value={lab.isPublished ? 'published' : 'running'} label={lab.isPublished ? 'Marks published' : 'Open for submissions'} /></div>
      <div className="grid items-stretch gap-4 xl:grid-cols-[.9fr_1.1fr]">
        <div className="grid h-full gap-4 xl:grid-rows-[1fr_auto]">
          <section className="surface-card h-full"><p className="eyebrow">Problem statement</p><h2 className="mt-3 text-lg font-semibold text-slate-950">{lab.title.replace(/^Lab \d+:\s*/, '')}</h2><p className="mt-3 whitespace-pre-line text-sm leading-6 text-slate-600">{lab.statementMd}</p><div className="mt-5 rounded-xl bg-slate-950 p-4 font-mono text-xs text-slate-200"><span className="text-slate-500">Sample input</span><pre className="mt-2 whitespace-pre-wrap">{lab.tests[0]?.input}</pre><span className="mt-4 block text-slate-500">Expected output</span><pre className="mt-2 whitespace-pre-wrap">{lab.tests[0]?.expectedOutput}</pre></div></section>
          <section className="surface-card"><p className="eyebrow">Lab constraints</p><div className="mt-4 space-y-2">{lab.constraints.map((constraint) => <div key={constraint.constraintId} className="flex gap-3 rounded-xl bg-slate-50 p-3"><span className="grid size-8 shrink-0 place-items-center rounded-xl bg-lime-100 font-mono text-xs font-semibold text-lime-700">{constraint.constraintId}</span><div><p className="text-sm font-medium text-slate-800">{constraint.description}</p><p className="mt-1 text-[11px] text-slate-400">{constraint.type} · deduction set by TA: {(constraint.deductionTenths / 10).toFixed(1)} pts</p></div></div>)}</div></section>
        </div>
        <section className="surface-card flex h-full flex-col overflow-hidden p-0">
            <div className="flex items-center justify-between border-b border-slate-100 p-4"><div><p className="eyebrow">C++ submission</p><h2 className="mt-1 font-semibold text-slate-950">Solution editor</h2></div><label className="secondary-button cursor-pointer"><Upload className="size-4" /> Upload .cpp<input type="file" accept=".cpp" className="hidden" onChange={(event) => {
              const file = event.target.files?.[0]
              setUploadError('')
              if (!file) return
              if (!file.name.toLowerCase().endsWith('.cpp')) {
                setUploadError('Only .cpp files are accepted.')
                return
              }
              if (file.size > 512 * 1024) {
                setUploadError('File must be 512 KB or smaller.')
                return
              }
              file.text().then(setCode).catch(() => setUploadError('Could not read this file.'))
            }} /></label></div>
            {uploadError && <div className="border-b border-rose-200 bg-rose-50 px-4 py-2 text-xs text-rose-700">{uploadError}</div>}
            <div className="grid min-h-[420px] flex-1 grid-cols-[44px_minmax(0,1fr)] overflow-hidden bg-[#0e120c] font-mono text-xs leading-6 text-slate-200"><div className="select-none overflow-hidden border-r border-slate-700 py-4 text-right text-slate-600">{code.split('\n').map((_, index) => <div key={index} className="pr-3">{index + 1}</div>)}</div><textarea value={code} onChange={(event) => setCode(event.target.value)} spellCheck={false} className="h-full min-w-0 resize-none overflow-auto whitespace-pre bg-transparent p-4 outline-none" /></div>
            <div className="flex items-center justify-between p-4"><span className="flex items-center gap-2 text-[11px] text-slate-400"><FileCode2 className="size-3" /> GNU C++20 · max 512 KB</span><button onClick={() => void submit()} disabled={!code.trim()} className="primary-button"><Send className="size-4" /> Submit Solution</button></div>
        </section>
        <div className="space-y-4">
          <div className="flex gap-3 rounded-2xl border border-lime-200 bg-lime-50 p-4 text-xs leading-5 text-lime-800"><ShieldCheck className="mt-0.5 size-4 shrink-0" /><p><strong>Privacy notice.</strong> Accepted submissions are reviewed for constraint compliance before final marks are published. Your name and student ID are stripped before model review.</p></div>
          {lab.isPublished && submissionDetail && submissionDetail.publishedScoreTenths !== null ? <section className="surface-card border-emerald-200"><p className="eyebrow text-emerald-600">Official mark</p><div className="mt-3 flex items-end justify-between"><p className="text-3xl font-semibold text-emerald-600">{formatScoreTenths(submissionDetail.publishedScoreTenths)}</p><span className="text-xs text-slate-400">Raw: {formatScoreTenths(submissionDetail.scoreTenths)}</span></div>
            {submissionDetail.publishedDeductions && submissionDetail.publishedDeductions.length > 0 ? <div className="mt-4 space-y-2">{submissionDetail.publishedDeductions.map((deduction) => {
              const existingAppeal = appeals.find((item) => item.constraintId === deduction.constraintId && item.submissionId === submissionDetail.id)
              return <div key={deduction.constraintId} className="rounded-xl bg-slate-50 p-3"><div className="flex items-start justify-between gap-3"><div><strong className="text-xs text-slate-800">{deduction.constraintId} · {deduction.description}</strong><p className="mt-1 text-[10px] text-rose-500">-{(deduction.deductionTenths / 10).toFixed(1)} pts</p></div>{existingAppeal ? <StatusBadge value={existingAppeal.status} label={`Appeal ${existingAppeal.status}`} /> : <button onClick={() => setAppealTarget(deduction)} className="secondary-button shrink-0"><MessageSquareText className="size-3.5" /> Appeal</button>}</div></div>
            })}</div> : <p className="mt-3 text-xs text-emerald-600">No constraint deductions were applied.</p>}
          </section> : <section className="surface-card flex gap-3"><LockKeyhole className="size-5 text-slate-400" /><div><strong className="text-sm">Marks pending publication</strong><p className="mt-1 text-xs text-slate-400">Constraint verdicts and deductions stay hidden until the TA completes Human Gate.</p></div></section>}
          {lab.isPublished && <section className="surface-card"><p className="eyebrow">My appeals</p>{appeals.length ? <div className="mt-3 space-y-2">{appeals.map((appeal) => <div key={appeal.id} className="rounded-xl bg-slate-50 p-3"><div className="flex items-center justify-between"><strong className="text-xs">{appeal.constraintId} · Appeal #{appeal.id}</strong><StatusBadge value={appeal.status} /></div><p className="mt-2 text-xs text-slate-500">{appeal.reason}</p>{appeal.decisionReason && <p className="mt-2 border-t border-slate-200 pt-2 text-xs text-slate-700"><strong>TA response:</strong> {appeal.decisionReason}</p>}</div>)}</div> : <p className="mt-3 text-xs text-slate-400">No appeals submitted for this lab.</p>}</section>}
        </div>
        <div className="space-y-4">
          {live && <section className="surface-card">
            <div className="flex items-center justify-between"><div><p className="eyebrow">Live judge</p><h2 className="mt-1 font-semibold">Submission #{live.id}</h2></div><span className="flex items-center gap-1.5 text-[10px] font-semibold text-emerald-600"><Radio className="size-3 animate-pulse" /> Live connected</span></div>
            <div className="mt-6 grid grid-cols-3 gap-2">{['queued', 'running', 'AC'].map((status, index) => { const current = ['queued', 'running', 'AC'].indexOf(live.judgeStatus); return <div key={status} className={`rounded-2xl p-3 text-center ${index <= current ? 'bg-lime-50 text-lime-700' : 'bg-slate-50 text-slate-300'}`}>{index < current ? <Check className="mx-auto size-4" /> : <CircleDot className="mx-auto size-4" />}<span className="mt-2 block text-[10px] font-semibold uppercase">{status === 'AC' ? 'Final result' : status}</span></div> })}</div>
            {live.judgeStatus === 'AC' && <div className="mt-4 rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-800"><strong>Accepted · 5/5 tests</strong><p className="mt-1 text-xs">Execution 42 ms · Memory 12.1 MB</p></div>}
          </section>}
          <section className="surface-card"><p className="eyebrow">Submission history</p><div className="mt-4 space-y-2">{submissions.map((item) => <div key={item.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3"><div><span className="font-mono text-xs">#{item.id}</span><p className="mt-1 text-[10px] text-slate-400">{formatUtcDate(item.submittedAt)}</p></div><StatusBadge value={item.judgeStatus} /></div>)}</div></section>
        </div>
      </div>
      {appealTarget && <Modal title={`Appeal ${appealTarget.constraintId} deduction`} onClose={() => setAppealTarget(null)}>
        <div className="rounded-xl bg-slate-50 p-3 text-xs text-slate-600">{appealTarget.description}<strong className="mt-1 block text-rose-600">-{(appealTarget.deductionTenths / 10).toFixed(1)} pts</strong></div>
        <label className="field-label mt-4">Reason<textarea autoFocus className="input mt-2 min-h-28" value={appealReason} onChange={(event) => setAppealReason(event.target.value)} placeholder="Explain why this deduction should be reconsidered..." /></label>
        <button onClick={() => void createAppeal()} disabled={!appealReason.trim()} className="primary-button mt-4 w-full justify-center"><Send className="size-4" /> Submit Appeal</button>
      </Modal>}
    </div>
  )
}
