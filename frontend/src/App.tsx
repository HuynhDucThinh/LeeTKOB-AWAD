import { useEffect } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { AppRoutes } from '@/routes/AppRoutes'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/authStore'

export function App() {
  const setAuth = useAuthStore((state) => state.setAuth)
  const clearAuth = useAuthStore((state) => state.clearAuth)
  const setInitializing = useAuthStore((state) => state.setInitializing)

  useEffect(() => {
    let active = true

    const hydrateSession = async () => {
      try {
        const session = await authService.refreshToken()
        if (!active) return
        if (session) {
          setAuth(session.user, session.accessToken)
        } else {
          clearAuth()
        }
      } catch {
        if (active) clearAuth()
      } finally {
        if (active) setInitializing(false)
      }
    }

    void hydrateSession()
    return () => {
      active = false
    }
  }, [setAuth, clearAuth, setInitializing])

  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

export default App
