import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  BookOpen,
  ChevronRight,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Moon,
  School,
  Sun,
  UserRound,
  UsersRound,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { authService } from '@/services/authService'
import { versionService } from '@/services/versionService'
import { useAuthStore } from '@/stores/authStore'
import { useThemeStore } from '@/stores/themeStore'
import type { UserRole } from '@/types/auth'
import type { VersionInfo } from '@/types/version'

const navByRole = {
  admin: [
    { to: '/admin/classes', label: 'Classes', icon: School },
    { to: '/admin/users', label: 'Users', icon: UsersRound },
  ],
  ta: [
    { to: '/ta/classes', label: 'Classes', icon: School },
    { to: '/ta/labs', label: 'My Labs', icon: LayoutDashboard },
  ],
  student: [
    { to: '/student/classes', label: 'Classes', icon: School },
    { to: '/student/labs', label: 'My Labs', icon: BookOpen },
  ],
}

const roleHome: Record<UserRole, string> = {
  admin: '/admin/classes',
  ta: '/ta/labs',
  student: '/student/labs',
}

export function DashboardLayout() {
  const user = useAuthStore((state) => state.user)
  const clearAuth = useAuthStore((state) => state.clearAuth)
  const setAuth = useAuthStore((state) => state.setAuth)
  const { dark, toggleTheme } = useThemeStore()
  const [version, setVersion] = useState<VersionInfo | null>(null)
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    void versionService
      .get()
      .then(setVersion)
      .catch(() => undefined)
  }, [])

  if (!user) return null

  const breadcrumb = location.pathname
    .split('/')
    .filter(Boolean)
    .map((part) => part.replace(/-/g, ' '))

  const switchRole = (role: UserRole) => {
    const ids: Record<UserRole, string> = { admin: 'admin-1', ta: 'ta-own', student: 'student-1' }
    void authService
      .login(ids[role], 'demo')
      .then(({ user: nextUser, accessToken }) => {
        setAuth(nextUser, accessToken)
        navigate(roleHome[role])
      })
      .catch(() => undefined)
  }

  return (
    <div className={cn('min-h-screen text-slate-800', dark && 'dark')}>
      <aside className="app-sidebar fixed inset-y-3 left-3 z-30 hidden w-[212px] flex-col rounded-[24px] border border-white/80 bg-white/90 p-3.5 shadow-xl shadow-lime-950/5 backdrop-blur-xl lg:flex">
        <button
          onClick={() => navigate(roleHome[user.role])}
          className="flex items-center gap-3 px-2 py-2 text-left"
        >
          <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-lime-400 text-black shadow-lg shadow-lime-500/20">
            <GraduationCap className="size-4" />
          </span>
          <span className="min-w-0">
            <strong className="block whitespace-nowrap text-lg tracking-tight text-slate-950">
              LeeTKOB
            </strong>
            <small className="block whitespace-nowrap text-[8px] font-bold uppercase tracking-[0.12em] text-lime-600">
              {user.role} workspace
            </small>
          </span>
        </button>

        <nav className="mt-8 space-y-1.5">
          {navByRole[user.role].map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                  isActive
                    ? 'bg-slate-950 text-lime-300 shadow-lg shadow-slate-950/10'
                    : 'text-slate-500 hover:bg-lime-50 hover:text-slate-950',
                )
              }
            >
              <Icon className="size-4" /> {label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto rounded-2xl bg-slate-50 p-3">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-xl bg-lime-100 text-lime-700">
              <UserRound className="size-4" />
            </span>
            <span className="min-w-0 flex-1">
              <strong className="block truncate text-sm text-slate-900">{user.displayName}</strong>
              <small className="font-mono text-[10px] text-slate-400">{user.id}</small>
            </span>
            <button
              onClick={() => {
                void authService.logout().finally(() => {
                  clearAuth()
                  navigate('/auth')
                })
              }}
              className="text-slate-400 hover:text-rose-600"
              aria-label="Sign out"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </div>
      </aside>

      <div className="min-h-screen pb-20 lg:ml-[236px] lg:pb-0">
        <header className="app-header sticky top-3 z-20 mx-3 flex h-14 items-center justify-between rounded-2xl border border-white/80 bg-white/80 px-4 shadow-sm backdrop-blur-xl md:px-5">
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <span>Workspace</span>
            {breadcrumb.slice(1).map((part) => (
              <span key={part} className="flex items-center gap-1.5 capitalize">
                <ChevronRight className="size-3" />
                <span className="text-slate-700">{part}</span>
              </span>
            ))}
          </div>
          <div className="flex items-center gap-2">
            <div className="hidden rounded-full border border-white bg-white/80 p-1 shadow-sm sm:flex">
              {(['admin', 'ta', 'student'] as UserRole[]).map((role) => (
                <button
                  key={role}
                  onClick={() => switchRole(role)}
                  className={cn(
                    'rounded-full px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wide',
                    user.role === role ? 'bg-slate-950 text-lime-300' : 'text-slate-400',
                  )}
                >
                  {role}
                </button>
              ))}
            </div>
            <button onClick={toggleTheme} className="icon-button" aria-label="Toggle theme">
              {dark ? <Sun className="size-4" /> : <Moon className="size-4" />}
            </button>
          </div>
        </header>

        <main className="mx-auto max-w-[1360px] p-4 md:p-6">
          <Outlet />
        </main>

        <footer className="px-8 pb-7 text-right font-mono text-[10px] text-slate-400">
          Build: {version ? `${version.commit}${version.tag ? ` (${version.tag})` : ''}` : 'loading'}
        </footer>
      </div>

      <nav className="fixed inset-x-4 bottom-4 z-30 flex justify-around rounded-2xl border border-white/80 bg-white/90 p-2 shadow-xl backdrop-blur lg:hidden">
        {navByRole[user.role].map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center gap-1 rounded-xl px-5 py-2 text-[10px] font-medium',
                isActive ? 'bg-slate-950 text-lime-300' : 'text-slate-500',
              )
            }
          >
            <Icon className="size-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
