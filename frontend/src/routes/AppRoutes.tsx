import { Navigate, Route, Routes } from 'react-router-dom'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { AdminClassesPage } from '@/pages/admin/AdminClassesPage'
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage'
import { AuthPage } from '@/pages/auth/AuthPage'
import { MyClassesPage } from '@/pages/common/MyClassesPage'
import { NotFoundPage } from '@/pages/not-found/NotFoundPage'
import { StudentLabDetailPage } from '@/pages/student/StudentLabDetailPage'
import { StudentLabsPage } from '@/pages/student/StudentLabsPage'
import { TaAppealsPage } from '@/pages/ta/TaAppealsPage'
import { TaLabDetailPage } from '@/pages/ta/TaLabDetailPage'
import { TaLabFormPage } from '@/pages/ta/TaLabFormPage'
import { TaLabsPage } from '@/pages/ta/TaLabsPage'
import { TaReviewQueuePage } from '@/pages/ta/TaReviewQueuePage'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/auth" element={<AuthPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route element={<ProtectedRoute allowedRoles={['admin']} />}>
            <Route path="/admin/classes" element={<AdminClassesPage />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['ta']} />}>
            <Route path="/ta/classes" element={<MyClassesPage />} />
            <Route path="/ta/labs" element={<TaLabsPage />} />
            <Route path="/ta/labs/new" element={<TaLabFormPage />} />
            <Route path="/ta/labs/:labId/edit" element={<TaLabFormPage />} />
            <Route path="/ta/labs/:labId" element={<TaLabDetailPage />} />
            <Route path="/ta/labs/:labId/review-queue" element={<TaReviewQueuePage />} />
            <Route path="/ta/labs/:labId/appeals" element={<TaAppealsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['student']} />}>
            <Route path="/student/classes" element={<MyClassesPage />} />
            <Route path="/student/labs" element={<StudentLabsPage />} />
            <Route path="/student/labs/:labId" element={<StudentLabDetailPage />} />
          </Route>

          <Route path="/not-found" element={<NotFoundPage />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/auth" replace />} />
      <Route path="*" element={<Navigate to="/not-found" replace />} />
    </Routes>
  )
}
