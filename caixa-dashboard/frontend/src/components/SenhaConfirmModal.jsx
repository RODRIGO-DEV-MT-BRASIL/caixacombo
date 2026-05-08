import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Lock, AlertTriangle, Loader2 } from 'lucide-react'

export default function SenhaConfirmModal({ open, onClose, onConfirm, title, description }) {
  const { token } = useAuth()
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (!open) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const res = await fetch(apiUrl('/api/auth/verify-password'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ password })
      })

      const data = await res.json()
      if (!data.valid) {
        setError('Senha incorreta')
        setLoading(false)
        return
      }

      setPassword('')
      setLoading(false)
      onConfirm()
    } catch {
      setError('Erro ao verificar senha')
      setLoading(false)
    }
  }

  const handleClose = () => {
    setPassword('')
    setError('')
    onClose()
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={handleClose}>
      <div className="glass p-6 w-full max-w-sm glow-red" onClick={e => e.stopPropagation()}>
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center">
            <AlertTriangle size={20} className="text-red-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">{title || 'Ação Sensível'}</h3>
            <p className="text-xs text-gray-400">{description || 'Esta ação requer confirmação'}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Senha do administrador</label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
              <input
                type="password"
                value={password}
                onChange={e => { setPassword(e.target.value); setError('') }}
                className="input-field pl-9"
                placeholder="Digite sua senha"
                autoFocus
                required
              />
            </div>
            {error && <p className="text-red-400 text-xs mt-1">{error}</p>}
          </div>

          <div className="flex gap-3">
            <button type="button" onClick={handleClose} className="btn-ghost flex-1">Cancelar</button>
            <button type="submit" disabled={loading} className="bg-red-600 hover:bg-red-700 text-white flex-1 px-4 py-2 rounded-lg flex items-center justify-center gap-2 text-sm transition-colors disabled:opacity-50">
              {loading ? <Loader2 size={16} className="animate-spin" /> : <Lock size={16} />}
              Confirmar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
