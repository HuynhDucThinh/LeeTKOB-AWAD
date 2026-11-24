import { create } from 'zustand'

type Theme = 'light' | 'dark'

interface ThemeState {
  theme: Theme
  dark: boolean
  toggleTheme: () => void
  toggle: () => void
  setTheme: (theme: Theme) => void
}

const STORAGE_KEY = 'leetkob_theme'

function getInitialTheme(): Theme {
  if (typeof window === 'undefined') return 'light'
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored === 'light' || stored === 'dark') return stored
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyThemeClass(theme: Theme): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  root.classList.toggle('dark', theme === 'dark')
  root.classList.toggle('dark-mode', theme === 'dark')
}

export const useThemeStore = create<ThemeState>((set) => {
  const initialTheme = getInitialTheme()
  applyThemeClass(initialTheme)

  const setNextTheme = (nextTheme: Theme) => {
    window.localStorage.setItem(STORAGE_KEY, nextTheme)
    applyThemeClass(nextTheme)
    set({ theme: nextTheme, dark: nextTheme === 'dark' })
  }

  return {
    theme: initialTheme,
    dark: initialTheme === 'dark',
    toggleTheme: () => {
      set((state) => {
        const nextTheme: Theme = state.theme === 'light' ? 'dark' : 'light'
        window.localStorage.setItem(STORAGE_KEY, nextTheme)
        applyThemeClass(nextTheme)
        return { theme: nextTheme, dark: nextTheme === 'dark' }
      })
    },
    toggle: () => {
      set((state) => {
        const nextTheme: Theme = state.theme === 'light' ? 'dark' : 'light'
        window.localStorage.setItem(STORAGE_KEY, nextTheme)
        applyThemeClass(nextTheme)
        return { theme: nextTheme, dark: nextTheme === 'dark' }
      })
    },
    setTheme: setNextTheme,
  }
})
