import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { 
  History, Filter, Clock, Monitor, 
  Lock, Unlock, Wifi, WifiOff, Activity, AlertCircle,
  ChevronDown, RefreshCw
} from 'lucide-react'

const tipoIcons = {
  conexao: Wifi,
  desconexao: WifiOff,
  bloqueio: Lock,
  desbloqueio: Unlock,
  mudanca_status: Activity
}

const tipoColors = {
  conexao: 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20',
  desconexao: 'text-red-400 bg-red-400/10 border-red-400/20',
  bloqueio: 'text-red-400 bg-red-400/10 border-red-400/20',
  desbloqueio: 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20',
  mudanca_status: 'text-amber-400 bg-amber-400/10 border-amber-400/20'
}

export default function Auditoria() {
  const { token } = useAuth()
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filters, setFilters] = useState({
    tipo: '',
    deviceId: '',
    limit: 50
  })
  const [showFilters, setShowFilters] = useState(false)

  const fetchLogs = async () => {
    try {
      setLoading(true)
      setError(null)
      const params = new URLSearchParams(filters)
      const res = await fetch(`/api/auditoria?${params}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      
      if (!res.ok) {
        throw new Error(`Erro ${res.status}: ${res.statusText}`)
      }
      
      const data = await res.json()
      setLogs(data)
    } catch (err) {
      console.error('Erro ao buscar auditoria:', err)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (token) {
      fetchLogs()
    }
  }, [token, filters])

  const formatTimestamp = (timestamp) => {
    try {
      return new Date(timestamp).toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    } catch (e) {
      return 'Data inválida'
    }
  }

  const getDeviceOptions = () => {
    const devices = [...new Set(logs.map(log => log.deviceId))]
    return devices.map(id => ({
      value: id,
      label: `${logs.find(l => l.deviceId === id)?.deviceName || id} (${id})`
    }))
  }

  if (error) {
    return (
      <div className="glass p-8 text-center">
        <AlertCircle size={48} className="mx-auto text-red-400 mb-3" />
        <p className="text-red-400 mb-2">Erro ao carregar auditoria</p>
        <p className="text-gray-400 text-sm mb-4">{error}</p>
        <button
          onClick={fetchLogs}
          className="px-4 py-2 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-sm font-medium transition-all"
        >
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-white flex items-center gap-2">
          <History size={20} className="text-blue-400" />
          Auditoria de Dispositivos
        </h3>
        <button
          onClick={fetchLogs}
          disabled={loading}
          className="px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1 disabled:opacity-50"
        >
          <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
          Atualizar
        </button>
      </div>

      {/* Filtros */}
      <div className="glass p-4">
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors mb-3"
        >
          <Filter size={14} />
          Filtros
          <ChevronDown size={14} className={`transition-transform ${showFilters ? 'rotate-180' : ''}`} />
        </button>

        {showFilters && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label className="text-xs text-gray-500 block mb-1">Tipo de Evento</label>
              <select
                value={filters.tipo}
                onChange={(e) => setFilters({ ...filters, tipo: e.target.value })}
                className="w-full px-3 py-1.5 bg-gray-800 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500/50"
              >
                <option value="" style={{ backgroundColor: '#1f2937' }}>Todos</option>
                <option value="conexao" style={{ backgroundColor: '#1f2937' }}>Conexão</option>
                <option value="desconexao" style={{ backgroundColor: '#1f2937' }}>Desconexão</option>
                <option value="bloqueio" style={{ backgroundColor: '#1f2937' }}>Bloqueio</option>
                <option value="desbloqueio" style={{ backgroundColor: '#1f2937' }}>Desbloqueio</option>
                <option value="mudanca_status" style={{ backgroundColor: '#1f2937' }}>Mudança de Status</option>
              </select>
            </div>

            <div>
              <label className="text-xs text-gray-500 block mb-1">Dispositivo</label>
              <select
                value={filters.deviceId}
                onChange={(e) => setFilters({ ...filters, deviceId: e.target.value })}
                className="w-full px-3 py-1.5 bg-gray-800 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500/50"
              >
                <option value="" style={{ backgroundColor: '#1f2937' }}>Todos</option>
                {getDeviceOptions().map(device => (
                  <option key={device.value} value={device.value} style={{ backgroundColor: '#1f2937' }}>
                    {device.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-xs text-gray-500 block mb-1">Limite</label>
              <select
                value={filters.limit}
                onChange={(e) => setFilters({ ...filters, limit: parseInt(e.target.value) })}
                className="w-full px-3 py-1.5 bg-gray-800 border border-white/10 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500/50"
              >
                <option value="25" style={{ backgroundColor: '#1f2937' }}>25 últimos</option>
                <option value="50" style={{ backgroundColor: '#1f2937' }}>50 últimos</option>
                <option value="100" style={{ backgroundColor: '#1f2937' }}>100 últimos</option>
                <option value="200" style={{ backgroundColor: '#1f2937' }}>200 últimos</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Logs */}
      <div className="space-y-2">
        {loading ? (
          <div className="glass p-8 text-center">
            <RefreshCw size={24} className="mx-auto text-gray-600 mb-2 animate-spin" />
            <p className="text-gray-400">Carregando logs...</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="glass p-8 text-center">
            <History size={48} className="mx-auto text-gray-600 mb-3" />
            <p className="text-gray-400">Nenhum registro encontrado</p>
            <p className="text-gray-600 text-sm mt-1">Os eventos dos dispositivos aparecerão aqui</p>
          </div>
        ) : (
          logs.map((log) => {
            const Icon = tipoIcons[log.tipo] || AlertCircle
            const colorClass = tipoColors[log.tipo] || 'text-gray-400 bg-gray-400/10 border-gray-400/20'
            
            return (
              <div key={log.id} className="glass p-4 border border-white/5 hover:border-white/10 transition-all">
                <div className="flex items-start gap-3">
                  <div className={`w-10 h-10 rounded-lg ${colorClass} flex items-center justify-center flex-shrink-0`}>
                    <Icon size={18} />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div>
                        <p className="text-sm font-medium text-white capitalize">{log.tipo}</p>
                        <p className="text-xs text-gray-400 mt-1">{log.detalhes}</p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="text-xs text-gray-500">{formatTimestamp(log.timestamp)}</p>
                        <p className="text-xs text-gray-600 mt-1">por {log.usuario || 'Sistema'}</p>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-4 text-xs text-gray-500">
                      <div className="flex items-center gap-1">
                        <Monitor size={12} />
                        <span>{log.deviceName || 'Dispositivo'}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <span className="font-mono">{log.deviceId}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
