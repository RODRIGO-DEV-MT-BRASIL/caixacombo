import { createContext, useContext, useState, useEffect, useRef } from 'react'
import { io } from 'socket.io-client'
import { useAuth } from './AuthContext'

const SocketContext = createContext(null)

export function SocketProvider({ children }) {
  const { token } = useAuth()
  const [devices, setDevices] = useState([])
  const [connected, setConnected] = useState(false)
  const [vendas, setVendas] = useState([])
  const [timeUpdates, setTimeUpdates] = useState({})
  const socketRef = useRef(null)

  useEffect(() => {
    if (!token) return

    const apiUrl = import.meta.env.VITE_API_URL || window.location.origin
    const socket = io(apiUrl, {
      auth: { token },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 10000
    })
    socketRef.current = socket

    socket.on('connect', () => {
      setConnected(true)
      socket.emit('dashboard_connect', { token })
      
      // Atualização periódica leve a cada 60s (evita re-renderizações excessivas)
      const syncInterval = setInterval(() => {
        if (socket.connected) {
          socket.emit('dashboard_connect', { token })
        }
      }, 60000)
      
      // Salvar referência para limpar quando desconectar
      socket.syncInterval = syncInterval
    })

    socket.on('disconnect', () => {
      setConnected(false)
      // Limpar intervalo de sincronização
      if (socket.syncInterval) {
        clearInterval(socket.syncInterval)
      }
    })

    socket.on('devices_list', (list) => {
      setDevices(prev => {
        // Manter ordem estável: dispositivos existentes mantêm posição, novos vão ao final
        const existingIds = new Set(prev.map(d => d.deviceId))
        const updated = prev.map(d => {
          const updatedDevice = list.find(nd => nd.deviceId === d.deviceId)
          return updatedDevice ? { ...d, ...updatedDevice } : d
        })
        // Adicionar novos dispositivos que não existiam antes
        const newDevices = list.filter(d => !existingIds.has(d.deviceId))
        return [...updated, ...newDevices]
      })
    })
    
    socket.on('device_connected', (device) => {
      setDevices(prev => {
        const existingIndex = prev.findIndex(d => d.deviceId === device.deviceId)
        if (existingIndex >= 0) {
          // Atualizar in-place mantendo posição
          const updated = [...prev]
          updated[existingIndex] = { ...prev[existingIndex], ...device, online: true }
          return updated
        }
        // Novo dispositivo vai ao final
        return [...prev, { ...device, online: true }]
      })
    })

    socket.on('device_disconnected', ({ deviceId, status }) => {
      setDevices(prev => prev.map(d => 
        d.deviceId === deviceId ? { ...d, online: false, status: status || 'offline' } : d
      ))
    })

    socket.on('device_status_update', ({ deviceId, status, lockReason, lockedAt, lockPassword, usageTimeLimit, usageStartTime }) => {
      setDevices(prev => {
        const updatedDevices = prev.map(d => 
          d.deviceId === deviceId 
            ? { 
                ...d, 
                status, 
                lockReason: status === 'locked' ? lockReason : undefined,
                lockedAt: status === 'locked' ? lockedAt : undefined,
                lockPassword: lockPassword || d.lockPassword,
                usageTimeLimit, 
                usageStartTime, 
                online: status !== 'offline' 
              } 
            : d
        )
        
        // Detectar mudanças de status e mostrar notificações
        const device = updatedDevices.find(d => d.deviceId === deviceId)
        const oldDevice = prev.find(d => d.deviceId === deviceId)
        
        if (oldDevice && device) {
          // Detectar desbloqueio
          if (oldDevice.status === 'locked' && device.status === 'online') {
            console.log(`🔓 Dispositivo ${device.deviceName || deviceId} desbloqueado!`)
            
            // Emitir evento customizado para notificação
            const event = new CustomEvent('device_unlocked', {
              detail: {
                deviceId,
                deviceName: device.deviceName || deviceId,
                timestamp: new Date()
              }
            })
            window.dispatchEvent(event)
          }
          
          // Detectar bloqueio
          if (oldDevice.status !== 'locked' && device.status === 'locked') {
            console.log(`🔒 Dispositivo ${device.deviceName || deviceId} bloqueado!`)
            
            // Emitir evento customizado para notificação
            const event = new CustomEvent('device_locked', {
              detail: {
                deviceId,
                deviceName: device.deviceName || deviceId,
                reason: device.lockReason || 'Bloqueado pelo administrador',
                timestamp: new Date()
              }
            })
            window.dispatchEvent(event)
          }
        }
        
        return updatedDevices
      })
    })

    socket.on('time_update', ({ deviceId, elapsed, remaining, total }) => {
      setTimeUpdates(prev => ({ ...prev, [deviceId]: { elapsed, remaining, total } }))
    })

    socket.on('venda_added', (venda) => {
      setVendas(prev => [venda, ...prev].slice(0, 200)) // Limitar a 200 vendas em memória
    })

    socket.on('vendas_history', (history) => {
      setVendas(history.slice(0, 200))
    })

    socket.on('device_password_updated', ({ deviceId, lockPassword }) => {
      setDevices(prev => prev.map(d =>
        d.deviceId === deviceId ? { ...d, lockPassword } : d
      ))
    })

    socket.on('control_result', ({ deviceId, action, success, error }) => {
      console.log(`🎮 [CONTROL_RESULT] ${deviceId} - ${action} - sucesso=${success} ${error ? `- erro: ${error}` : ''}`)
      
      // Emitir evento customizado para notificação no Dashboard
      const event = new CustomEvent('control_result', {
        detail: {
          deviceId,
          action,
          success,
          error,
          timestamp: new Date()
        }
      })
      window.dispatchEvent(event)
    })

    return () => {
      // Limpar intervalo de sincronização
      if (socket.syncInterval) {
        clearInterval(socket.syncInterval)
      }
      socket.disconnect()
      socketRef.current = null
    }
  }, [token])

  const lockDevice = (deviceId, reason) => {
    // Atualizar otimisticamente no frontend (feedback imediato)
    setDevices(prev => prev.map(d => 
      d.deviceId === deviceId ? { ...d, status: 'locked', online: true, lockReason: reason, lockedAt: new Date() } : d
    ))
    socketRef.current?.emit('lock_device', { deviceId, reason })
  }

  const unlockDevice = (deviceId) => {
    // Atualizar otimisticamente no frontend (feedback imediato)
    setDevices(prev => prev.map(d => 
      d.deviceId === deviceId ? { ...d, status: 'online', online: true, lockReason: null, lockedAt: null } : d
    ))
    socketRef.current?.emit('unlock_device', { deviceId })
  }

  const forceUnlockDevice = (deviceId) => {
    socketRef.current?.emit('force_unlock', { deviceId })
  }

  const setUsageTime = (deviceId, minutes) => {
    socketRef.current?.emit('set_usage_time', { deviceId, minutes })
  }

  const commandDevice = (deviceId, command, params) => {
    socketRef.current?.emit('command_device', { deviceId, command, params })
  }

  const controlApp = (deviceId, action) => {
    socketRef.current?.emit('control_app', { deviceId, action })
  }

  return (
    <SocketContext.Provider value={{
      devices, setDevices, connected, vendas, setVendas, timeUpdates,
      lockDevice, unlockDevice, forceUnlockDevice, setUsageTime, commandDevice, controlApp,
      socket: socketRef.current
    }}>
      {children}
    </SocketContext.Provider>
  )
}

export const useSocket = () => useContext(SocketContext)
