import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      fetch(`${import.meta.env.VITE_API_URL || ''}/api/auth/verify`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(data => {
          setUser(data.user)
          setLoading(false)
        })
        .catch(() => {
          localStorage.removeItem('token')
          setToken(null)
          setUser(null)
          setLoading(false)
        })
    } else {
      setLoading(false)
    }
  }, [token])

  const login = async (username, password) => {
    const res = await fetch(`${import.meta.env.VITE_API_URL || ''}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.error || 'Erro ao fazer login')
    localStorage.setItem('token', data.token)
    setToken(data.token)
    setUser(data.user)
    return data
  }

  const logout = () => {
    localStorage.removeItem('token')
    setToken(null)
    setUser(null)
  }

  const hasPermission = (permission) => {
    // Admin tem todas as permissões
    if (user?.role === 'admin') return true
    // Empresas verificam suas permissões específicas
    if (user?.role === 'empresa') {
      return user?.permissoes?.[permission] === true
    }
    // Usuários comuns não têm permissões específicas
    return false
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading, hasPermission }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
