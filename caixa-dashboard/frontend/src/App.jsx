import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import { ToastProvider, useToast } from './contexts/ToastContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Toast from './components/Toast'
import { Loader2 } from 'lucide-react'

function ProtectedRoute({ children }) {
  const { token, loading } = useAuth()
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <Loader2 size={40} className="animate-spin text-blue-400" />
      </div>
    )
  }
  return token ? children : <Navigate to="/login" replace />
}

function AppWithToast() {
  const { toasts, removeToast } = useToast()

  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/*" element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        } />
      </Routes>
      
      {/* Renderizar toasts */}
      {toasts.map(toast => (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          duration={toast.duration}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </>
  )
}

export default function App() {
  return (
    <ToastProvider>
      <AppWithToast />
    </ToastProvider>
  )
}
