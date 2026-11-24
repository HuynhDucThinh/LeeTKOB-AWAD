import { useEffect, useState } from 'react'
import { Plus, School, Users, UserRoundCog, FlaskConical } from 'lucide-react'
import { adminService } from '@/services/adminService'
import type { ClassRoom } from '@/types/admin'
import type { AuthUser } from '@/types/auth'
import { LoadingState } from '@/components/common/LoadingState'
import { EmptyState } from '@/components/common/EmptyState'
import { Modal } from '@/components/common/Modal'

export function AdminClassesPage() {
  const [classes, setClasses] = useState<ClassRoom[] | null>(null)
  const [users, setUsers] = useState<AuthUser[]>([])
  const [createOpen, setCreateOpen] = useState(false)
  const [className, setClassName] = useState('')
  const [managing, setManaging] = useState<ClassRoom | null>(null)
  const [taIds, setTaIds] = useState<string[]>([])
  const [studentIds, setStudentIds] = useState<string[]>([])

  const load = () => {
    adminService.getClasses().then(setClasses).catch(() => setClasses([]))
    adminService.getUsers().then(setUsers).catch(() => setUsers([]))
  }
  useEffect(load, [])

  const createClass = async () => {
    if (!className.trim()) return
    const created = await adminService.createClass(className.trim())
    setClasses((items) => [...(items ?? []), created])
    setClassName('')
    setCreateOpen(false)
  }
  const openMembers = (item: ClassRoom) => {
    setManaging(item)
    setTaIds(item.taIds)
    setStudentIds(item.studentIds)
  }
  const saveMembers = async () => {
    if (!managing) return
    const updated = await adminService.updateMembers(managing.id, taIds, studentIds)
    setClasses((items) => (items ?? []).map((item) => item.id === updated.id ? updated : item))
    setManaging(null)
  }

  return (
    <div>
      <div className="page-header"><div><p className="eyebrow">Administration</p><h1>Classes & Assignments</h1><p>Organize classes and manage workspace access.</p></div><button onClick={() => setCreateOpen(true)} className="primary-button"><Plus className="size-4" /> Create Class</button></div>
      {!classes ? <LoadingState /> : classes.length === 0 ? <EmptyState title="No classes created yet" detail="Create your first class to assign teaching assistants and students." action={<button onClick={() => setCreateOpen(true)} className="primary-button">Create Class</button>} /> : (
        <div className="grid gap-4 xl:grid-cols-2">
          {classes.map((item) => <article key={item.id} className="surface-card group">
            <div className="flex items-start justify-between"><span className="grid size-10 place-items-center rounded-xl bg-lime-100 text-lime-700"><School className="size-4" /></span><span className="rounded-full bg-slate-100 px-3 py-1 font-mono text-[10px] text-slate-500">CLASS {item.id}</span></div>
            <h2 className="mt-4 text-lg font-semibold text-slate-950">{item.name}</h2>
            <div className="mt-4 grid grid-cols-3 gap-2">
              <Metric icon={<UserRoundCog />} value={item.taIds.length} label="TAs" />
              <Metric icon={<Users />} value={item.studentIds.length} label="Students" />
              <Metric icon={<FlaskConical />} value={item.labCount} label="Labs" />
            </div>
            <button onClick={() => openMembers(item)} className="secondary-button mt-4 w-full justify-center">Manage Members</button>
          </article>)}
        </div>
      )}
      {createOpen && <Modal title="Create class" onClose={() => setCreateOpen(false)}>
        <label className="field-label">Class name<input autoFocus className="input mt-2" value={className} onChange={(event) => setClassName(event.target.value)} placeholder="CS101 - Class 30 (CQ2023/5)" /></label>
        <button onClick={() => void createClass()} disabled={!className.trim()} className="primary-button mt-5 w-full justify-center">Create Class</button>
      </Modal>}
      {managing && <Modal title={`Members · Class ${managing.id}`} onClose={() => setManaging(null)}>
        <MemberGroup title="Teaching assistants" users={users.filter((user) => user.role === 'ta')} selected={taIds} onChange={setTaIds} />
        <div className="my-5 h-px bg-slate-100" />
        <MemberGroup title="Students" users={users.filter((user) => user.role === 'student')} selected={studentIds} onChange={setStudentIds} />
        <button onClick={() => void saveMembers()} className="primary-button mt-5 w-full justify-center">Save Assignments</button>
      </Modal>}
    </div>
  )
}

function MemberGroup({ title, users, selected, onChange }: { title: string; users: AuthUser[]; selected: string[]; onChange: (ids: string[]) => void }) {
  return <div><p className="text-xs font-semibold text-slate-800">{title}</p><div className="mt-2 space-y-2">{users.map((user) => <label key={user.id} className="flex cursor-pointer items-center gap-3 rounded-xl bg-slate-50 p-3 text-xs"><input type="checkbox" checked={selected.includes(user.id)} onChange={(event) => onChange(event.target.checked ? [...selected, user.id] : selected.filter((id) => id !== user.id))} className="accent-lime-500" /><span className="flex-1"><strong className="block">{user.displayName}</strong><span className="font-mono text-[10px] text-slate-400">{user.id}</span></span></label>)}</div></div>
}

function Metric({ icon, value, label }: { icon: React.ReactElement; value: number; label: string }) {
  return <div className="rounded-xl bg-slate-50 p-3"><span className="text-lime-600 [&_svg]:size-3.5">{icon}</span><strong className="mt-1.5 block text-base text-slate-900">{value}</strong><span className="text-[9px] uppercase tracking-wide text-slate-400">{label}</span></div>
}
