import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToast } from '../components/Toast'
import { 
  LayoutDashboard, Package, Tags, ShoppingCart, Wifi, WifiOff,
  LogOut, Menu, X, Monitor, Lock, Unlock, Clock, Play, 
  ChevronRight, Activity, TrendingUp, AlertTriangle, CheckCircle2,
  Search, RefreshCw, Eye, EyeOff, RefreshCw as RotateCw, Cpu, DollarSign,
  Play as PlayIcon, X as CloseIcon, Power, History, Building2
} from 'lucide-react'
import Produtos from './Produtos'
import Categorias from './Categorias'
import Vendas from './Vendas'
import Caixa from './Caixa'
import Auditoria from './Auditoria'
import Empresas from './Empresas'
import Terminais from './Terminais'

const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, permission: 'dashboard' },
  { id: 'terminais', label: 'Terminais', icon: Monitor, permission: 'dashboard' },
  { id: 'empresas', label: 'Empresas', icon: Building2, permission: 'empresas' },
  { id: 'categorias', label: 'Categorias', icon: Tags, permission: 'categorias' },
  { id: 'produtos', label: 'Produtos', icon: Package, permission: 'produtos' },
  { id: 'vendas', label: 'Vendas', icon: ShoppingCart, permission: 'vendas' },
  { id: 'caixa', label: 'Caixa', icon: DollarSign, permission: 'caixa' },
  { id: 'auditoria', label: 'Auditoria', icon: History, permission: 'auditoria' },
]

export default function Dashboard() {
  const { user, logout, token, hasPermission } = useAuth()
  const { devices, connected, lockDevice, unlockDevice, forceUnlockDevice, setUsageTime, controlApp, timeUpdates, socket } = useSocket()
  const { success } = useToast()
  const [page, setPage] = useState('dashboard')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [usageModal, setUsageModal] = useState(null)
  const [usageMinutes, setUsageMinutes] = useState('')
  const [lockModal, setLockModal] = useState(null)
  const [lockReason, setLockReason] = useState('')
  const [showPassword, setShowPassword] = useState({})
  const [changePasswordModal, setChangePasswordModal] = useState(null)

  // Listener para eventos de desbloqueio e bloqueio de terminal
  useEffect(() => {
    const handleDeviceUnlocked = (event) => {
      const { deviceId, deviceName } = event.detail
      console.log(`🔓 Terminal desbloqueado: ${deviceName}`)

      // Mostrar notificação toast
      success(
        `🔓 Terminal desbloqueado: ${deviceName}`,
        5000
      )
    }

    const handleDeviceLocked = (event) => {
      const { deviceId, deviceName, reason } = event.detail
      console.log(`🔒 Terminal bloqueado: ${deviceName} - ${reason}`)

      // Mostrar notificação toast
      success(
        `🔒 Terminal bloqueado: ${deviceName} - ${reason}`,
        5000
      )
    }

    const handleControlResult = (event) => {
      const { deviceId, action, success: isSuccess, error } = event.detail
      const device = devices.find(d => d.deviceId === deviceId)
      const deviceName = device?.deviceName || deviceId

      const actionNames = {
        'open_app': 'Abrir app',
        'close_app': 'Fechar app',
        'restart': 'Reiniciar dispositivo',
        'shutdown': 'Desligar dispositivo'
      }

      if (isSuccess) {
        success(`✅ ${actionNames[action] || action} executado com sucesso em ${deviceName}`, 3000)
      } else {
        success(`❌ Erro ao ${actionNames[action] || action} em ${deviceName}: ${error}`, 5000)
      }
    }

    window.addEventListener('device_unlocked', handleDeviceUnlocked)
    window.addEventListener('device_locked', handleDeviceLocked)
    window.addEventListener('control_result', handleControlResult)

    return () => {
      window.removeEventListener('device_unlocked', handleDeviceUnlocked)
      window.removeEventListener('device_locked', handleDeviceLocked)
      window.removeEventListener('control_result', handleControlResult)
    }
  }, [success, devices])

  const onlineDevices = devices.filter(d => d.online || d.status === 'online' || d.status === 'in_use')
  const lockedDevices = devices.filter(d => d.status === 'locked')
  const inUseDevices = devices.filter(d => d.status === 'in_use')

  // Filtrar itens do menu baseado em permissões
  const filteredNavItems = navItems.filter(item => {
    if (item.adminOnly) return user?.role === 'admin'
    if (item.permission) return hasPermission(item.permission)
    return true
  })

  // Função para formatar tempo (MM:SS)
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  // Verificar se tempo expirou para mostrar botão "Forçar Bloqueio"
  const isTimeExpired = (deviceId) => {
    const timeUpdate = timeUpdates[deviceId]
    return timeUpdate && timeUpdate.remaining <= 0
  }

  const statusConfig = {
    online: { color: 'text-emerald-400', bg: 'bg-emerald-400/10', border: 'border-emerald-400/20', icon: CheckCircle2, label: 'Online' },
    locked: { color: 'text-red-400', bg: 'bg-red-400/10', border: 'border-red-400/20', icon: Lock, label: 'Bloqueado' },
    offline: { color: 'text-gray-500', bg: 'bg-gray-500/10', border: 'border-gray-500/20', icon: WifiOff, label: 'Offline' },
    in_use: { color: 'text-amber-400', bg: 'bg-amber-400/10', border: 'border-amber-400/20', icon: Clock, label: 'Em Uso' },
  }

  const getDeviceStatus = (device) => {
    if (device.status === 'locked') return 'locked'
    if (device.status === 'in_use') return 'in_use'
    if (device.online === false && device.status !== 'online') return 'offline'
    return 'online'
  }

  const getConnectedTime = (connectedAt) => {
    if (!connectedAt) return '-'
    const now = new Date()
    const connected = new Date(connectedAt)
    const diff = now - connected
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    
    if (days > 0) return `${days}d ${hours % 24}h`
    if (hours > 0) return `${hours}h ${minutes % 60}m`
    return `${minutes}m`
  }

  const handleChangePassword = async (deviceId) => {
    try {
      const res = await fetch(`/api/dispositivos/${deviceId}/password`, {
        method: 'PUT',
        headers: { 
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}` 
        }
      })
      const data = await res.json()
      if (res.ok) {
        // Mostrar notificação de sucesso
        success(`🔑 Nova senha gerada: ${data.lockPassword}`, 5000)
        setChangePasswordModal(null)
      } else {
        console.error('Erro ao gerar nova senha:', data.error)
      }
    } catch (err) {
      console.error('Erro ao gerar nova senha:', err)
    }
  }

  const handleControlDevice = useCallback(async (deviceId, action) => {
    console.log(`🎮 [DEBUG] handleControlDevice chamado: deviceId=${deviceId}, action=${action}`)
    try {
      const res = await fetch(`/api/dispositivos/${deviceId}/control`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ action })
      })
      const data = await res.json()
      console.log(`🎮 [DEBUG] Resposta do servidor:`, data)
      if (!res.ok) {
        console.error('Erro ao controlar dispositivo:', data.error)
        // Mostrar erro ao usuário
        if (data.error) {
          success(`❌ Erro: ${data.error}`, 5000)
        }
      } else {
        // Mostrar confirmação
        const actionNames = {
          'open_app': 'Abrindo app',
          'close_app': 'Fechando app',
          'restart': 'Reiniciando dispositivo',
          'shutdown': 'Desligando dispositivo'
        }
        success(`✅ ${actionNames[action] || 'Comando enviado'}`, 3000)
      }
    } catch (err) {
      console.error(err)
      success('❌ Erro ao enviar comando', 3000)
    }
  }, [token, success])

  const handleForceSync = () => {
    console.log('🔄 [DEBUG] Botão sincronizar clicado')
    console.log('🔄 [DEBUG] Socket conectado:', connected)
    console.log('🔄 [DEBUG] Socket object:', socket ? 'disponível' : 'nulo')
    
    if (socket) {
      console.log('🔄 [DEBUG] Socket ID:', socket.id)
      console.log('🔄 [DEBUG] Socket connected:', socket.connected)
    }
    
    if (socket && connected) {
      console.log('🔄 [DEBUG] Enviando dashboard_connect...')
      socket.emit('dashboard_connect', { token })
      success('🔄 Sincronização forçada - atualizando dispositivos...', 3000)
    } else {
      console.error('❌ [DEBUG] Socket não disponível ou desconectado')
      success('❌ WebSocket desconectado - tentando reconectar...', 3000)
      // Forçar reconexão se necessário
      if (socket) {
        socket.connect()
      }
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 flex">
      {/* Sidebar */}
      <aside className={`fixed lg:static inset-y-0 left-0 z-40 w-64 glass border-r border-white/5 transform transition-transform duration-300 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="p-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
              <span className="text-sm font-black text-white">CC</span>
            </div>
            <div>
              <h1 className="font-bold text-white text-lg leading-tight">CaixaCombo</h1>
              <p className="text-xs text-gray-500">Dashboard v1.1</p>
            </div>
            <button onClick={() => setSidebarOpen(false)} className="lg:hidden ml-auto text-gray-400 hover:text-white">
              <X size={20} />
            </button>
          </div>

          {/* Nav */}
          <nav className="flex-1 px-3 space-y-1">
            {filteredNavItems.map(item => (
              <button
                key={item.id}
                onClick={() => { setPage(item.id); setSidebarOpen(false) }}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                  page === item.id 
                    ? 'bg-blue-600/20 text-blue-400 border border-blue-500/20' 
                    : 'text-gray-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <item.icon size={18} />
                {item.label}
              </button>
            ))}
          </nav>

          {/* Connection Status */}
          <div className="px-3 mb-3">
            <div className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-medium ${
              connected ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
            }`}>
              {connected ? <Wifi size={14} /> : <WifiOff size={14} />}
              {connected ? 'WebSocket Conectado' : 'WebSocket Desconectado'}
            </div>
          </div>

          {/* User */}
          <div className="p-3 border-t border-white/5">
            <div className="flex items-center gap-3 px-3 py-2">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-xs font-bold text-white">
                {user?.username?.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{user?.username}</p>
                <p className="text-xs text-gray-500 capitalize">{user?.role}</p>
              </div>
              <button onClick={logout} className="text-gray-500 hover:text-red-400 transition-colors" title="Sair">
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </div>
      </aside>

      {/* Overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/50 z-30 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Main Content */}
      <main className="flex-1 min-h-screen overflow-auto">
        {/* Top bar */}
        <header className="sticky top-0 z-20 glass border-b border-white/5 px-6 py-4 flex items-center gap-4">
          <button onClick={() => setSidebarOpen(true)} className="lg:hidden text-gray-400 hover:text-white">
            <Menu size={22} />
          </button>
          <h2 className="text-lg font-semibold text-white capitalize">{navItems.find(i => i.id === page)?.label}</h2>
          <div className="ml-auto flex items-center gap-3">
            <button
              onClick={handleForceSync}
              className="px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
              title="Forçar sincronização dos dispositivos"
            >
              <RefreshCw size={14} />
              Sincronizar
            </button>
            <span className="text-xs text-gray-500">{new Date().toLocaleDateString('pt-BR')}</span>
          </div>
        </header>

        <div className="p-6">
          {page === 'dashboard' && (
            <div className="space-y-6">
              {/* Stats */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="stat-card glow-blue">
                  <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                    <Monitor size={24} className="text-blue-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{devices.length}</p>
                    <p className="text-xs text-gray-400">Total Dispositivos</p>
                  </div>
                </div>
                <div className="stat-card glow-green">
                  <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
                    <Wifi size={24} className="text-emerald-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{onlineDevices.length}</p>
                    <p className="text-xs text-gray-400">Online</p>
                  </div>
                </div>
                <div className="stat-card glow-red">
                  <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center">
                    <Lock size={24} className="text-red-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{lockedDevices.length}</p>
                    <p className="text-xs text-gray-400">Bloqueados</p>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
                    <Clock size={24} className="text-amber-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{inUseDevices.length}</p>
                    <p className="text-xs text-gray-400">Em Uso</p>
                  </div>
                </div>
              </div>

              {/* Devices Grid */}
              <div>
                <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                  <Activity size={20} className="text-blue-400" />
                  Dispositivos
                </h3>
                {devices.length === 0 ? (
                  <div className="glass p-12 text-center">
                    <Monitor size={48} className="mx-auto text-gray-600 mb-3" />
                    <p className="text-gray-400">Nenhum dispositivo conectado</p>
                    <p className="text-gray-600 text-sm mt-1">Dispositivos Android aparecerão aqui ao conectar</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                    {devices.map(device => {
                      const status = getDeviceStatus(device)
                      const cfg = statusConfig[status] || statusConfig.offline
                      const StatusIcon = cfg.icon
                      return (
                        <div key={device.deviceId} className={`glass glass-hover p-5 border ${cfg.border}`}>
                          <div className="flex items-start justify-between mb-4">
                            <div className="flex items-center gap-3">
                              <div className={`w-12 h-12 rounded-xl ${cfg.bg} flex items-center justify-center`}>
                                <Monitor size={24} className={cfg.color} />
                              </div>
                              <div>
                                <p className="font-semibold text-white text-base">{device.deviceName || device.deviceId}</p>
                                <p className="text-xs text-gray-400 font-mono">{device.deviceId}</p>
                              </div>
                            </div>
                            <div className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg ${cfg.bg} text-xs font-medium ${cfg.color}`}>
                              <StatusIcon size={14} />
                              {cfg.label}
                            </div>
                          </div>

                          {/* Dados completos do dispositivo */}
                          <div className="grid grid-cols-1 gap-3 mb-4 text-xs">
                            <div className="flex items-center gap-2 text-gray-400">
                              <Cpu size={14} />
                              <span className="font-mono">S/N: {device.serialNumber || device.deviceId}</span>
                            </div>
                            <div className="flex items-center gap-2 text-gray-400">
                              <Clock size={14} />
                              <span>Conectado: {getConnectedTime(device.connectedAt)}</span>
                            </div>
                            {device.connectedAt && (
                              <div className="flex items-center gap-2 text-gray-400">
                                <Activity size={14} />
                                <span>Desde: {new Date(device.connectedAt).toLocaleString('pt-BR')}</span>
                              </div>
                            )}
                          </div>

                          {/* Senha de bloqueio */}
                          {device.lockPassword && (
                            <div className="mb-4 p-3 rounded-lg bg-blue-600/10 border border-blue-500/20">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                  <Lock size={14} className="text-blue-400" />
                                  <span className="text-xs text-gray-400">Senha de Bloqueio:</span>
                                </div>
                                <div className="flex items-center gap-2">
                                  <span className="text-lg font-mono font-bold text-blue-300 bg-black/30 px-2 py-1 rounded border border-blue-500/30">
                                    {showPassword[device.deviceId] ? device.lockPassword : '••••••'}
                                  </span>
                                  <button
                                    onClick={() => setShowPassword({ ...showPassword, [device.deviceId]: !showPassword[device.deviceId] })}
                                    className="text-gray-500 hover:text-white transition-colors p-1 rounded hover:bg-white/5"
                                    title={showPassword[device.deviceId] ? "Ocultar senha" : "Mostrar senha"}
                                  >
                                    {showPassword[device.deviceId] ? <EyeOff size={14} /> : <Eye size={14} />}
                                  </button>
                                  <button
                                    onClick={() => setChangePasswordModal(device.deviceId)}
                                    className="text-gray-500 hover:text-blue-400 transition-colors p-1 rounded hover:bg-white/5"
                                    title="Mudar senha"
                                  >
                                    <RotateCw size={14} />
                                  </button>
                                </div>
                              </div>
                            </div>
                          )}

                          {device.lockReason && device.status === 'locked' && (
                            <div className="flex items-center gap-1.5 text-xs text-red-400 mb-3">
                              <AlertTriangle size={12} />
                              {device.lockReason}
                            </div>
                          )}

                          {device.usageTimeLimit && (
                            <div className="flex items-center gap-1.5 text-xs text-amber-400 mb-3">
                              <Clock size={12} />
                              {timeUpdates[device.deviceId] ? (
                                <span className={`font-mono ${timeUpdates[device.deviceId].remaining <= 60 ? 'text-red-400 animate-pulse' : ''}`}>
                                  {formatTime(timeUpdates[device.deviceId].remaining)}
                                </span>
                              ) : (
                                <span>{device.usageTimeLimit} min</span>
                              )}
                            </div>
                          )}

                          <div className="flex gap-2 mt-3">
                            {status === 'online' && (
                              <>
                                <button
                                  onClick={() => setLockModal(device.deviceId)}
                                  className="btn-danger text-xs py-1.5 px-3 flex items-center gap-1"
                                >
                                  <Lock size={12} /> Bloquear
                                </button>
                                <button
                                  onClick={() => setUsageModal(device.deviceId)}
                                  className="px-3 py-1.5 bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/20 text-amber-400 rounded-xl text-xs font-medium transition-all flex items-center gap-1"
                                >
                                  <Clock size={12} /> Tempo
                                </button>
                              </>
                            )}
                            {status === 'locked' && (
                              <>
                                <button
                                  onClick={() => unlockDevice(device.deviceId)}
                                  className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs font-medium transition-all flex items-center gap-1"
                                >
                                  <Unlock size={12} /> Desbloquear
                                </button>
                                <button
                                  onClick={() => forceUnlockDevice(device.deviceId)}
                                  className="px-3 py-1.5 bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/20 text-amber-400 rounded-xl text-xs font-medium transition-all flex items-center gap-1"
                                  title="Forçar desbloqueio (se o terminal não funcionar)"
                                >
                                  <AlertTriangle size={12} /> Forçar
                                </button>
                              </>
                            )}
                            {status === 'in_use' && (
                              <button
                                onClick={() => lockDevice(device.deviceId, 'Uso interrompido pelo admin')}
                                className="btn-danger text-xs py-1.5 px-3 flex items-center gap-1"
                              >
                                <Lock size={12} /> Bloquear
                              </button>
                            )}
                            {/* Botões de controle do app */}
                            <div className="flex gap-2">
                              <button
                                onClick={() => handleControlDevice(device.deviceId, 'open_app')}
                                className="px-2 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
                                title="Abrir App"
                              >
                                <PlayIcon size={12} />
                              </button>
                              <button
                                onClick={() => handleControlDevice(device.deviceId, 'close_app')}
                                className="px-2 py-1.5 bg-purple-600/20 hover:bg-purple-600/30 border border-purple-500/20 text-purple-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
                                title="Fechar App"
                              >
                                <CloseIcon size={12} />
                              </button>
                              <button
                                onClick={() => handleControlDevice(device.deviceId, 'restart')}
                                className="px-2 py-1.5 bg-orange-600/20 hover:bg-orange-600/30 border border-orange-500/20 text-orange-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
                                title="Reiniciar"
                              >
                                <RotateCw size={12} />
                              </button>
                              <button
                                onClick={() => handleControlDevice(device.deviceId, 'shutdown')}
                                className="px-2 py-1.5 bg-red-600/20 hover:bg-red-600/30 border border-red-500/20 text-red-400 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
                                title="Desligar"
                              >
                                <Power size={12} />
                              </button>
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>
          )}

          {page === 'terminais' && <Terminais />}
          {page === 'empresas' && <Empresas />}
          {page === 'produtos' && <Produtos />}
          {page === 'categorias' && <Categorias />}
          {page === 'vendas' && <Vendas />}
          {page === 'caixa' && <Caixa />}
          {page === 'auditoria' && <Auditoria />}
        </div>
      </main>

      {/* Lock Modal */}
      {lockModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setLockModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-red" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <Lock size={20} className="text-red-400" />
              Bloquear Dispositivo
            </h3>
            <input
              type="text"
              value={lockReason}
              onChange={e => setLockReason(e.target.value)}
              className="input-field mb-4"
              placeholder="Motivo do bloqueio (opcional)"
            />
            <div className="flex gap-3">
              <button onClick={() => setLockModal(null)} className="btn-ghost flex-1">Cancelar</button>
              <button
                onClick={() => { lockDevice(lockModal, lockReason || 'Bloqueado pelo administrador'); setLockModal(null); setLockReason('') }}
                className="btn-danger flex-1"
              >
                Bloquear
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Usage Time Modal */}
      {usageModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setUsageModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <Clock size={20} className="text-amber-400" />
              Definir Tempo de Uso
            </h3>
            <input
              type="number"
              value={usageMinutes}
              onChange={e => setUsageMinutes(e.target.value)}
              className="input-field mb-4"
              placeholder="Minutos (ex: 30)"
              min="1"
            />
            <div className="flex gap-2 mb-4">
              {[15, 30, 60, 120].map(m => (
                <button
                  key={m}
                  onClick={() => setUsageMinutes(String(m))}
                  className="flex-1 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-xs text-gray-300 transition-all"
                >
                  {m}min
                </button>
              ))}
            </div>
            <div className="flex gap-3">
              <button onClick={() => setUsageModal(null)} className="btn-ghost flex-1">Cancelar</button>
              <button
                onClick={() => { if (usageMinutes > 0) { setUsageTime(usageModal, parseInt(usageMinutes)); setUsageModal(null); setUsageMinutes('') } }}
                className="btn-primary flex-1"
              >
                Definir
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Change Password Modal */}
      {changePasswordModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setChangePasswordModal(null)}>
          <div className="glass p-6 w-full max-w-sm glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <RotateCw size={20} className="text-blue-400" />
              Mudar Senha de Bloqueio
            </h3>
            <p className="text-sm text-gray-400 mb-4">
              Uma nova senha de 6 dígitos será gerada para o dispositivo.
            </p>
            <div className="flex gap-3">
              <button onClick={() => setChangePasswordModal(null)} className="btn-ghost flex-1">Cancelar</button>
              <button
                onClick={() => handleChangePassword(changePasswordModal)}
                className="btn-primary flex-1"
              >
                Gerar Nova Senha
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
