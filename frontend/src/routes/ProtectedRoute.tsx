import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types/auth'

const homeByRole: Record<UserRole, string> = {
  admin: '/admin/classes',
  ta: '/ta/labs',
  student: '/student/labs',
}

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { user, isAuthenticated, isInitializing } = useAuthStore()
  const location = useLocation()

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center text-slate-600">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 animate-spin rounded-full border-2 border-slate-200 border-t-lime-500" />
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Loading session...
          </span>
        </div>
      </div>
    )
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/auth" state={{ from: location }} replace />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={homeByRole[user.role]} replace />
  }

  return <Outlet />
}
