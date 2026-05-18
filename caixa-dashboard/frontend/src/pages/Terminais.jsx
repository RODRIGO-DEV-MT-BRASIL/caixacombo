import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToast } from '../components/Toast'
import {
  Monitor, Wifi, WifiOff, Lock, Unlock, Clock, AlertTriangle, CheckCircle2,
  Eye, EyeOff, RefreshCw as RotateCw, Power, Play as PlayIcon, X as CloseIcon, Search, Shield, Check, Building2
} from 'lucide-react'

const statusConfig = {
  online: { color: 'text-emerald-400', bg: 'bg-emerald-400/10', border: 'border-emerald-400/20', icon: CheckCircle2, label: 'Disponível' },
  locked: { color: 'text-red-400', bg: 'bg-red-400/10', border: 'border-red-400/20', icon: Lock, label: 'Bloqueado' },
  locked_timer: { color: 'text-orange-400', bg: 'bg-orange-400/10', border: 'border-orange-400/20', icon: Clock, label: 'Bloq. por Tempo' },
  offline: { color: 'text-gray-500', bg: 'bg-gray-500/10', border: 'border-gray-500/20', icon: WifiOff, label: 'Offline' },
  in_use: { color: 'text-amber-400', bg: 'bg-amber-400/10', border: 'border-amber-400/20', icon: Clock, label: 'Em Uso' },
  pending: { color: 'text-yellow-400', bg: 'bg-yellow-400/10', border: 'border-yellow-400/20', icon: Shield, label: 'Pendente' },
}

function getDeviceStatus(device) {
  if (device.status === 'pending') return 'pending'
  if (device.status === 'locked') return device.lockReason === 'Tempo de uso expirado' ? 'locked_timer' : 'locked'
  if (device.status === 'in_use') return 'in_use'
  if (device.online === false && device.status !== 'online') return 'offline'
  return 'online'
}

function getConnectedTime(connectedAt) {
  if (!connectedAt) return '-'
  const diff = Date.now() - new Date(connectedAt).getTime()
  const m = Math.floor(diff / 60000), h = Math.floor(m / 60), d = Math.floor(h / 24)
  if (d > 0) return `${d}d ${h % 24}h`
  if (h > 0) return `${h}h ${m % 60}m`
  return `${m}m`
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60), s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export default function Terminais() {
  const { user, token } = useAuth()
  const { devices, connected, lockDevice, unlockDevice, setUsageTime, timeUpdates, socket } = useSocket()
  const { success } = useToast()
  const [usageModal, setUsageModal] = useState(null)
  const [usageMinutes, setUsageMinutes] = useState('')
  const [lockModal, setLockModal] = useState(null)
  const [lockReason, setLockReason] = useState('')
  const [showPassword, setShowPassword] = useState({})
  const [search, setSearch] = useState('')
  const [empresas, setEmpresas] = useState([])
  const [approveModal, setApproveModal] = useState(null) // deviceId
  const [selectedEmpresa, setSelectedEmpresa] = useState('')
  const [filterEmpresa, setFilterEmpresa] = useState('') // Filtro de empresa para admin

  // Buscar empresas para o modal de aprovação
  useEffect(() => {
    if (user?.role === 'admin' || user?.role === 'empresa') {
      fetch('/api/empresas', { headers: { Authorization: `Bearer ${token}` } })
        .then(r => r.json()).then(setEmpresas).catch(() => {})
    }
  }, [user, token])

  const handleApprove = async (deviceId, empresaId) => {
    try {
      const res = await fetch(`/api/dispositivos/${deviceId}/aprovar`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ empresaId })
      })
      const data = await res.json()
      if (res.ok) { success('Terminal aprovado com sucesso', 3000); setApproveModal(null); setSelectedEmpresa('') }
      else success(data.error || 'Erro ao aprovar', 5000)
    } catch { success('Erro ao aprovar terminal', 5000) }
  }

  const handleReject = async (deviceId) => {
    try {
      const res = await fetch(`/api/dispositivos/${deviceId}/rejeitar`, {
        method: 'PUT', headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) { success('Terminal rejeitado', 3000); setApproveModal(null) }
    } catch { success('Erro ao rejeitar', 5000) }
  }

  const empresaId = user?.role === 'empresa' ? user?.empresaId : null
  const filteredByEmpresa = filterEmpresa ? devices.filter(d => d.empresaId === filterEmpresa) : devices
  const filtered = empresaId ? filteredByEmpresa.filter(d => d.empresaId === empresaId) : filteredByEmpresa
  const pending = filtered.filter(d => d.status === 'pending')
  const online = filtered.filter(d => d.online || d.status === 'online' || d.status === 'in_use')

  const sl = search.toLowerCase()
  const searched = search ? online.filter(d =>
    (d.deviceName || '').toLowerCase().includes(sl) || (d.deviceId || '').toLowerCase().includes(sl)
  ) : online

  const handleControl = useCallback(async (deviceId, action) => {
    try {
      const res = await fetch(`/api/dispositivos/${deviceId}/control`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ action })
      })
      const data = await res.json()
      if (!res.ok && data.error) success(`Erro: ${data.error}`, 5000)
      else success('Comando enviado', 3000)
    } catch { success('Erro ao enviar comando', 3000) }
  }, [token, success])

  const handleSync = async () => {
    if (socket && connected) socket.emit('dashboard_connect', { token })
    else if (socket) socket.connect()
    try {
      await fetch('/api/force-sync', { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
      success('Sincronizado', 3000)
    } catch { success('Reconectando...', 3000) }
  }

  useEffect(() => {
    const h = (e) => success(`Terminal ${e.type === 'device_unlocked' ? 'desbloqueado' : 'bloqueado'}: ${e.detail.deviceName}`, 5000)
    window.addEventListener('device_unlocked', h)
    window.addEventListener('device_locked', h)
    return () => { window.removeEventListener('device_unlocked', h); window.removeEventListener('device_locked', h) }
  }, [success])

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center"><Monitor size={24} className="text-blue-400" /></div>
          <div>
            <h3 className="text-lg font-semibold text-white">Terminais</h3>
            <p className="text-xs text-gray-400">{online.length} conectados · {filtered.filter(d => d.status === 'locked').length} bloqueados · {pending.length} pendentes</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {user?.role !== 'funcionario' && (
            <select
              value={filterEmpresa}
              onChange={(e) => setFilterEmpresa(e.target.value)}
              className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
            >
              <option value="">Todas as Empresas</option>
              {empresas.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.nome}</option>
              ))}
            </select>
          )}
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input type="text" value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar terminal..."
              className="pl-9 pr-4 py-2 bg-white/5 border border-white/10 rounded-xl text-sm text-white placeholder-gray-500 focus:outline-none focus:border-blue-500/50 transition-all w-48" />
          </div>
          <button onClick={handleSync} className="px-3 py-2 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1">
            <RotateCw size={14} /> Sincronizar
          </button>
        </div>
      </div>

      {/* Terminais Pendentes - Aprovação */}
      {pending.length > 0 && user?.role === 'admin' && (
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <Shield size={18} className="text-yellow-400" />
            <h4 className="text-sm font-semibold text-yellow-400">Terminais Pendentes ({pending.length})</h4>
            <span className="text-[11px] text-gray-500">— Aguardando aprovação</span>
          </div>
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            {pending.map(device => (
              <div key={device.deviceId} className="glass border border-yellow-400/20 overflow-hidden">
                <div className="flex items-center justify-between p-4 border-b border-white/5">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-yellow-400/10 flex items-center justify-center">
                      <Monitor size={20} className="text-yellow-400" />
                    </div>
                    <div>
                      <p className="font-semibold text-white text-sm">{device.deviceName || device.deviceId}</p>
                      <div className="flex items-center gap-2 text-[11px] text-gray-500">
                        <span className="font-mono">{device.serialNumber || device.deviceId}</span>
                        <span>·</span>
                        <span>{getConnectedTime(device.connectedAt)}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-yellow-400/10 text-[11px] font-semibold text-yellow-400">
                    <Shield size={12} /> Pendente
                  </div>
                </div>
                <div className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <button onClick={() => setApproveModal(device.deviceId)} className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
                      <Check size={12} /> Aprovar
                    </button>
                    <button onClick={() => handleReject(device.deviceId)} className="px-3 py-1.5 bg-red-600/20 hover:bg-red-600/30 border border-red-500/20 text-red-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
                      <CloseIcon size={12} /> Rejeitar
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
        {searched.map(device => {
          const status = getDeviceStatus(device)
          const cfg = statusConfig[status] || statusConfig.offline
          const StatusIcon = cfg.icon
          const isLocked = status === 'locked' || status === 'locked_timer'
          const hasTimer = !!device.usageTimeLimit
          const tr = timeUpdates[device.deviceId]?.remaining
          const tt = device.usageTimeLimit ? device.usageTimeLimit * 60 : 0
          const tp = tr != null && tt > 0 ? Math.max(0, (tr / tt) * 100) : null
          return (
            <div key={device.deviceId} className={`glass border ${cfg.border} overflow-hidden`}>
              <div className="flex items-center justify-between p-4 border-b border-white/5">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-lg ${cfg.bg} flex items-center justify-center`}>
                    <Monitor size={20} className={cfg.color} />
                  </div>
                  <div>
                    <p className="font-semibold text-white text-sm">{device.deviceName || device.deviceId}</p>
                    <div className="flex items-center gap-2 text-[11px] text-gray-500">
                      <span className="font-mono">{device.serialNumber || device.deviceId}</span>
                      <span>·</span>
                      <span>{getConnectedTime(device.connectedAt)}</span>
                    </div>
                  </div>
                </div>
                <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md ${cfg.bg} text-[11px] font-semibold ${cfg.color}`}>
                  <StatusIcon size={12} /> {cfg.label}
                </div>
              </div>
              {hasTimer && !isLocked && (
                <div className="px-4 py-3 border-b border-white/5 bg-amber-500/5">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-medium text-amber-400 flex items-center gap-1.5"><Clock size={12} /> Tempo</span>
                    <span className={`text-sm font-mono font-bold ${tr && tr <= 60 ? 'text-red-400 animate-pulse' : 'text-amber-300'}`}>
                      {tr != null ? formatTime(tr) : `${device.usageTimeLimit}:00`}
                    </span>
                  </div>
                  {tp != null && (
                    <div className="w-full h-1.5 rounded-full bg-black/30 overflow-hidden">
                      <div className={`h-full rounded-full transition-all duration-1000 ${tr <= 60 ? 'bg-red-500' : tr <= 300 ? 'bg-amber-500' : 'bg-emerald-500'}`} style={{ width: `${tp}%` }} />
                    </div>
                  )}
                </div>
              )}
              {isLocked && device.lockReason && (
                <div className={`px-4 py-2.5 border-b border-white/5 flex items-center gap-2 text-xs ${status === 'locked_timer' ? 'bg-orange-500/5 text-orange-400' : 'bg-red-500/5 text-red-400'}`}>
                  {status === 'locked_timer' ? <Clock size={12} /> : <AlertTriangle size={12} />}
                  <span>{status === 'locked_timer' ? 'Tempo expirado' : device.lockReason}</span>
                </div>
              )}
              {device.lockPassword && (
                <div className="px-4 py-2.5 border-b border-white/5 flex items-center justify-between">
                  <span className="text-[11px] text-gray-500 flex items-center gap-1.5"><Lock size={11} /> Senha</span>
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-mono font-bold text-blue-300 bg-black/30 px-2 py-0.5 rounded border border-blue-500/20">
                      {showPassword[device.deviceId] ? device.lockPassword : '••••••'}
                    </span>
                    <button onClick={() => setShowPassword({ ...showPassword, [device.deviceId]: !showPassword[device.deviceId] })} className="text-gray-600 hover:text-white transition-colors p-0.5 rounded hover:bg-white/5">
                      {showPassword[device.deviceId] ? <EyeOff size={12} /> : <Eye size={12} />}
                    </button>
                  </div>
                </div>
              )}
              <div className="px-4 py-3">
                <div className="flex items-center gap-2 flex-wrap">
                  {isLocked ? (
                    <button onClick={() => unlockDevice(device.deviceId)} className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
                      <Unlock size={12} /> Desbloquear
                    </button>
                  ) : (
                    <button onClick={() => setLockModal(device.deviceId)} className="px-3 py-1.5 bg-red-600/20 hover:bg-red-600/30 border border-red-500/20 text-red-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5">
                      <Lock size={12} /> Bloquear
                    </button>
                  )}
                  {!isLocked && (
                    <button onClick={() => setUsageModal(device.deviceId)} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 ${hasTimer ? 'bg-amber-600/25 hover:bg-amber-600/35 border border-amber-500/25 text-amber-300' : 'bg-white/5 hover:bg-white/10 border border-white/10 text-gray-400 hover:text-white'}`}>
                      <Clock size={12} /> {hasTimer ? 'Alterar' : 'Timer'}
                    </button>
                  )}
                  <div className="w-px h-5 bg-white/10 mx-1" />
                  <button onClick={() => handleControl(device.deviceId, 'open_app')} className="p-1.5 bg-white/5 hover:bg-blue-600/20 border border-white/5 hover:border-blue-500/20 text-gray-500 hover:text-blue-400 rounded-md transition-all" title="Abrir"><PlayIcon size={12} /></button>
                  <button onClick={() => handleControl(device.deviceId, 'close_app')} className="p-1.5 bg-white/5 hover:bg-purple-600/20 border border-white/5 hover:border-purple-500/20 text-gray-500 hover:text-purple-400 rounded-md transition-all" title="Fechar"><CloseIcon size={12} /></button>
                  <button onClick={() => handleControl(device.deviceId, 'restart')} className="p-1.5 bg-white/5 hover:bg-orange-600/20 border border-white/5 hover:border-orange-500/20 text-gray-500 hover:text-orange-400 rounded-md transition-all" title="Reiniciar"><RotateCw size={12} /></button>
                  <button onClick={() => handleControl(device.deviceId, 'shutdown')} className="p-1.5 bg-white/5 hover:bg-red-600/20 border border-white/5 hover:border-red-500/20 text-gray-500 hover:text-red-400 rounded-md transition-all" title="Desligar"><Power size={12} /></button>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Modal Aprovar Terminal */}
      {approveModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setApproveModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><Building2 size={20} className="text-emerald-400" /> Aprovar Terminal</h3>
            <p className="text-sm text-gray-400 mb-4">Selecione a empresa para associar este terminal:</p>
            <select value={selectedEmpresa} onChange={e => setSelectedEmpresa(e.target.value)} className="input-field mb-4">
              <option value="">Selecione uma empresa...</option>
              {empresas.filter(e => e.ativo !== false).map(e => (
                <option key={e.id} value={e.id}>{e.nome} ({e.login})</option>
              ))}
            </select>
            <div className="flex gap-3">
              <button onClick={() => { setApproveModal(null); setSelectedEmpresa('') }} className="btn-ghost flex-1">Cancelar</button>
              <button onClick={() => { if (selectedEmpresa) handleApprove(approveModal, selectedEmpresa) }} disabled={!selectedEmpresa} className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed">Aprovar</button>
            </div>
          </div>
        </div>
      )}

      {lockModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setLockModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-red" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><Lock size={20} className="text-red-400" /> Bloquear</h3>
            <input type="text" value={lockReason} onChange={e => setLockReason(e.target.value)} className="input-field mb-4" placeholder="Motivo (opcional)" />
            <div className="flex gap-3">
              <button onClick={() => setLockModal(null)} className="btn-ghost flex-1">Cancelar</button>
              <button onClick={() => { lockDevice(lockModal, lockReason || 'Bloqueado pelo admin'); setLockModal(null); setLockReason('') }} className="btn-danger flex-1">Bloquear</button>
            </div>
          </div>
        </div>
      )}

      {usageModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setUsageModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><Clock size={20} className="text-amber-400" /> Tempo de Uso</h3>
            <input type="number" value={usageMinutes} onChange={e => setUsageMinutes(e.target.value)} className="input-field mb-4" placeholder="Minutos" min="1" />
            <div className="flex gap-2 mb-4">
              {[15, 30, 60, 120].map(m => (
                <button key={m} onClick={() => setUsageMinutes(String(m))} className="flex-1 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-xs text-gray-300 transition-all">{m}min</button>
              ))}
            </div>
            <div className="flex gap-3">
              <button onClick={() => setUsageModal(null)} className="btn-ghost flex-1">Cancelar</button>
              <button onClick={() => { if (usageMinutes > 0) { setUsageTime(usageModal, parseInt(usageMinutes)); setUsageModal(null); setUsageMinutes('') } }} className="btn-primary flex-1">Definir</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
