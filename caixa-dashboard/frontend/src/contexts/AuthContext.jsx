import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // SECURITY: Usar sessionStorage em vez de localStorage para proteger contra XSS
  // sessionStorage é limpo quando a aba é fechada
  const [token, setToken] = useState(() => {
    const saved = sessionStorage.getItem('token');
    return saved;
  });
  const [loading, setLoading] = useState(true)

  // Aplicar branding da empresa (cores, logo, nome)
  const applyBranding = (branding) => {
    if (!branding) return
    const root = document.documentElement
    if (branding.primaryColor) root.style.setProperty('--color-primary', branding.primaryColor)
    if (branding.secondaryColor) root.style.setProperty('--color-secondary', branding.secondaryColor)
    if (branding.accentColor) root.style.setProperty('--color-accent', branding.accentColor)
  }

  const clearBranding = () => {
    const root = document.documentElement
    root.style.removeProperty('--color-primary')
    root.style.removeProperty('--color-secondary')
    root.style.removeProperty('--color-accent')
  }

  useEffect(() => {
    if (token) {
      fetch(`${import.meta.env.VITE_API_URL || ''}/api/auth/verify`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(data => {
          setUser(data.user)
          if (data.user?.branding) applyBranding(data.user.branding)
          setLoading(false)
        })
        .catch(() => {
          sessionStorage.removeItem('token')
          setToken(null)
          setUser(null)
          clearBranding()
          setLoading(false)
        })
    } else {
      setLoading(false)
    }
  }, [token])

  const login = async (username, password, email, pin) => {
    const body = email && pin ? { email, pin } : { username, password }
    const res = await fetch(`${import.meta.env.VITE_API_URL || ''}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.error || 'Erro ao fazer login')
    // SECURITY: Usar sessionStorage em vez de localStorage
    sessionStorage.setItem('token', data.token)
    setToken(data.token)
    setUser(data.user)
    if (data.user?.branding) applyBranding(data.user.branding)
    return data
  }

  const logout = () => {
    // SECURITY: Usar sessionStorage em vez de localStorage
    sessionStorage.removeItem('token')
    setToken(null)
    setUser(null)
    clearBranding()
  }

  const hasPermission = (permission) => {
    if (user?.role === 'admin') return true
    if (user?.role === 'empresa') {
      return user?.permissoes?.[permission] === true
    }
    return false
  }

  const hasPageAccess = (pageId) => {
    if (user?.role === 'admin') return true
    if (user?.role === 'empresa') {
      const allowed = user?.paginasPermitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa']
      return allowed.includes(pageId)
    }
    return false
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading, hasPermission, hasPageAccess }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
