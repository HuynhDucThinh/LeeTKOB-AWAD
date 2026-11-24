import { useState } from 'react'
import { ArrowRight, BookOpenCheck, Eye, EyeOff, LoaderCircle, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/authStore'
import { getProblemError } from '@/lib/apiError'
import { ProblemBanner } from '@/components/common/ProblemBanner'
import type { ProblemDetail } from '@/types/problem'
import type { UserRole } from '@/types/auth'

const homeByRole: Record<UserRole, string> = { admin: '/admin/classes', ta: '/ta/labs', student: '/student/labs' }
const accounts = [
  { id: 'admin-1', label: 'Admin' },
  { id: 'ta-own', label: 'TA Class 10' },
  { id: 'ta-foreign', label: 'TA Class 20' },
  { id: 'student-1', label: 'Student 1' },
  { id: 'student-2', label: 'Student 2' },
]

export function AuthPage() {
  const [username, setUsername] = useState('ta1')
  const [password, setPassword] = useState('demo')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState<ProblemDetail | null>(null)
  const setSession = useAuthStore((state) => state.setSession)
  const navigate = useNavigate()

  const login = async (identity = username) => {
    setLoading(true)
    setProblem(null)
    try {
      const response = await authService.login(identity, password || 'demo')
      setSession(response.user, response.accessToken)
      navigate(homeByRole[response.user.role])
    } catch (error) {
      setProblem(getProblemError(error))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden bg-[radial-gradient(circle_at_18%_15%,#26351c_0%,#12160f_34%,#080a08_100%)] p-4">
      <div className="pointer-events-none absolute left-[8%] top-[12%] size-72 rounded-full bg-lime-400/10 blur-[100px]" />
      <div className="pointer-events-none absolute bottom-[5%] right-[8%] size-80 rounded-full bg-emerald-500/10 blur-[120px]" />
      <section className="relative w-full max-w-md rounded-[26px] border border-white/80 bg-white/95 p-7 text-slate-800 shadow-2xl shadow-black/35 backdrop-blur-xl sm:p-9">
        <div className="flex items-center gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-lime-400 text-slate-950 shadow-lg shadow-lime-400/20"><BookOpenCheck className="size-5" /></span><div><strong className="block text-lg text-slate-950">LeeTKOB</strong><span className="text-[9px] font-bold uppercase tracking-[.18em] text-lime-700">Human-first grading</span></div></div>
        <div className="mt-8">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-950">Sign in to your workspace</h1>
          <p className="mt-1.5 text-xs text-slate-500">C++ constraint review & grading pipeline</p>
        </div>
        {problem && <div className="mt-4"><ProblemBanner problem={problem} /></div>}
        <form className="mt-6 space-y-3.5" onSubmit={(event) => { event.preventDefault(); void login() }}>
          <label className="block text-xs font-semibold text-slate-700">Username<input value={username} onChange={(event) => setUsername(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-lime-400 focus:ring-4 focus:ring-lime-100" placeholder="Enter your username" /></label>
          <label className="block text-xs font-semibold text-slate-700">Password<div className="relative mt-1.5"><input type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => setPassword(event.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 pr-11 text-sm text-slate-900 outline-none transition focus:border-lime-400 focus:ring-4 focus:ring-lime-100" /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-lime-700">{showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</button></div></label>
          <button disabled={loading} className="primary-button w-full justify-center">{loading ? <><LoaderCircle className="size-4 animate-spin" /> Signing in...</> : <>Sign In <ArrowRight className="size-4" /></>}</button>
        </form>
        <div className="mt-5 border-t border-slate-100 pt-4">
          <span className="text-[9px] font-bold uppercase tracking-wider text-slate-400">Demo accounts</span>
          <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1.5">{accounts.map((account) => <button key={account.id} onClick={() => void login(account.id)} className="text-[11px] font-semibold text-slate-500 underline decoration-slate-300 underline-offset-4 hover:text-lime-700">{account.label}</button>)}</div>
        </div>
        <p className="mt-5 flex items-center gap-1.5 text-[10px] text-slate-400"><ShieldCheck className="size-3" /> Demo data stays in this session</p>
      </section>
    </div>
  )
}
