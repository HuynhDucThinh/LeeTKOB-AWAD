import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types/auth'

const homeByRole: Record<UserRole, string> = {
  admin: '/admin/classes',
  ta: '/ta/labs',
  student: '/student/labs',
}

export function PublicRoute() {
  const { user, isAuthenticated, isInitializing } = useAuthStore()

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center text-slate-600">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 animate-spin rounded-full border-2 border-slate-200 border-t-lime-500" />
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Checking session...
          </span>
        </div>
      </div>
    )
  }

  if (isAuthenticated && user) {
    return <Navigate to={homeByRole[user.role]} replace />
  }

  return <Outlet />
}
