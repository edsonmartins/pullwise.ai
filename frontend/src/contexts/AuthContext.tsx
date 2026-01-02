import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { User, Organization, authApi } from '@/lib/api'

interface AuthContextType {
  user: User | null
  organizations: Organization[]
  loading: boolean
  login: (token: string) => Promise<void>
  logout: () => void
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    checkAuth()
  }, [])

  const checkAuth = async () => {
    const token = localStorage.getItem('token')
    if (!token) {
      setLoading(false)
      return
    }

    try {
      const userData = await authApi.me()
      setUser(userData)
    } catch {
      localStorage.removeItem('token')
    } finally {
      setLoading(false)
    }
  }

  const login = async (token: string) => {
    const data = await authApi.login(token)
    setUser(data.user)
    setOrganizations(data.organizations)
  }

  const logout = () => {
    authApi.logout()
    setUser(null)
    setOrganizations([])
  }

  const refresh = async () => {
    await checkAuth()
  }

  return (
    <AuthContext.Provider value={{ user, organizations, loading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
