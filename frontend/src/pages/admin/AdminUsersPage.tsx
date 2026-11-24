import { useEffect, useMemo, useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { adminService } from '@/services/adminService'
import type { AuthUser, UserRole } from '@/types/auth'
import type { ProblemDetail } from '@/types/problem'
import { StatusBadge } from '@/components/common/StatusBadge'
import { Modal } from '@/components/common/Modal'
import { ProblemBanner } from '@/components/common/ProblemBanner'
import { getProblemError } from '@/lib/apiError'
import { formatUtcDate } from '@/lib/utils'

const emptyForm = { username: '', displayName: '', role: 'student' as UserRole, password: '' }

export function AdminUsersPage() {
  const [users, setUsers] = useState<AuthUser[]>([])
  const [role, setRole] = useState<UserRole | 'all'>('all')
  const [query, setQuery] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  useEffect(() => { adminService.getUsers().then(setUsers).catch(() => setUsers([])) }, [])
  const filtered = useMemo(() => users.filter((user) => (role === 'all' || user.role === role) && `${user.id} ${user.username} ${user.displayName}`.toLowerCase().includes(query.toLowerCase())), [users, role, query])
  const createUser = async () => {
    setProblem(null)
    try {
      const created = await adminService.createUser(form)
      setUsers((items) => [...items, created])
      setForm(emptyForm)
      setCreateOpen(false)
    } catch (error) {
      setProblem(getProblemError(error))
    }
  }
  return (
    <div>
      <div className="page-header"><div><p className="eyebrow">Administration</p><h1>User Directory</h1><p>Manage roles, demo accounts and class access.</p></div><button onClick={() => setCreateOpen(true)} className="primary-button"><Plus className="size-4" /> Create User</button></div>
      <div className="surface-card mb-4 flex flex-col gap-3 md:flex-row">
        <div className="relative flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><input className="input pl-10" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name, username or ID..." /></div>
        <div className="flex rounded-xl bg-slate-100 p-1">{(['all', 'admin', 'ta', 'student'] as const).map((item) => <button key={item} onClick={() => setRole(item)} className={`rounded-lg px-3 py-2 text-xs font-semibold capitalize ${role === item ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500'}`}>{item}</button>)}</div>
      </div>
      <div className="table-shell">
        <table><thead><tr><th>User</th><th>Role</th><th>Assigned classes</th><th>Created</th></tr></thead><tbody>{filtered.map((user) => <tr key={user.id}><td><strong className="block text-slate-900">{user.displayName}</strong><span className="font-mono text-[11px] text-slate-400">{user.username} · {user.id}</span></td><td><StatusBadge value={user.role} /></td><td>{user.classIds.length ? user.classIds.map((id) => `Class ${id}`).join(', ') : '—'}</td><td className="text-slate-500">{formatUtcDate(user.createdAt)}</td></tr>)}</tbody></table>
      </div>
      {createOpen && <Modal title="Create user" onClose={() => { setCreateOpen(false); setProblem(null) }}>
        {problem && <div className="mb-4"><ProblemBanner problem={problem} /></div>}
        <div className="space-y-3">
          <label className="field-label">Display name<input className="input mt-1.5" value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>
          <label className="field-label">Username<input className="input mt-1.5" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></label>
          <label className="field-label">Role<select className="input mt-1.5" value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as UserRole })}><option value="student">Student</option><option value="ta">Teaching assistant</option><option value="admin">Admin</option></select></label>
          <label className="field-label">Temporary password<input type="password" className="input mt-1.5" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
        </div>
        <button onClick={() => void createUser()} disabled={!form.username || !form.displayName || !form.password} className="primary-button mt-5 w-full justify-center">Create User</button>
      </Modal>}
    </div>
  )
}
