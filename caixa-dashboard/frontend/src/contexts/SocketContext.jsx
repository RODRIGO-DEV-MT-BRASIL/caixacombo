import { createContext, useContext, useState, useEffect, useRef } from 'react'
import Pusher from 'pusher-js'
import { useAuth } from './AuthContext'

const SocketContext = createContext(null)

export function SocketProvider({ children }) {
  const { token } = useAuth()
  const [devices, setDevices] = useState([])
  const [connected, setConnected] = useState(false)
  const [vendas, setVendas] = useState([])
  const [timeUpdates, setTimeUpdates] = useState({})
  const pusherRef = useRef(null)
  const channelRef = useRef(null)

  useEffect(() => {
    if (!token) return

    // Inicializar Pusher
    const pusher = new Pusher(import.meta.env.VITE_PUSHER_APP_KEY || 'your-pusher-key', {
      cluster: import.meta.env.VITE_PUSHER_CLUSTER || 'us2',
      auth: {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    })
    pusherRef.current = pusher

    // Conectar ao canal do dashboard
    const channel = pusher.subscribe('private-dashboard')
    channelRef.current = channel

    channel.bind('pusher:subscription_succeeded', () => {
      setConnected(true)
      console.log('Conectado ao Pusher')
    })

    channel.bind('pusher:subscription_error', (err) => {
      console.error('Erro ao conectar ao Pusher:', err)
      setConnected(false)
    })

    channel.bind('devices_list', (list) => setDevices(list))

    channel.bind('device_connected', (device) => {
      setDevices(prev => {
        const filtered = prev.filter(d => d.deviceId !== device.deviceId)
        return [...filtered, { ...device, online: true }]
      })
    })

    channel.bind('device_disconnected', ({ deviceId, status }) => {
      setDevices(prev => prev.map(d => 
        d.deviceId === deviceId ? { ...d, online: false, status: status || 'offline' } : d
      ))
    })

    channel.bind('device_status_update', ({ deviceId, status, lockReason, lockedAt, lockPassword, usageTimeLimit, usageStartTime }) => {
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

    channel.bind('time_update', ({ deviceId, elapsed, remaining, total }) => {
      setTimeUpdates(prev => ({ ...prev, [deviceId]: { elapsed, remaining, total } }))
    })

    channel.bind('sale_update', ({ sale }) => {
      setVendas(prev => [sale, ...prev])
    })

    channel.bind('venda_added', (venda) => {
      setVendas(prev => [venda, ...prev])
    })

    channel.bind('device_password_updated', ({ deviceId, lockPassword }) => {
      setDevices(prev => prev.map(d =>
        d.deviceId === deviceId ? { ...d, lockPassword } : d
      ))
    })

    channel.bind('control_result', ({ deviceId, action, success, error }) => {
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
      if (channel) {
        channel.unbind_all()
        pusher.unsubscribe('private-dashboard')
      }
      if (pusher) {
        pusher.disconnect()
      }
      pusherRef.current = null
      channelRef.current = null
    }
  }, [token])

  const lockDevice = async (deviceId, reason) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/lock`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ reason })
    })
  }

  const unlockDevice = async (deviceId) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/unlock`, {
      method: 'POST',
      headers: { 
        Authorization: `Bearer ${token}`
      }
    })
  }

  const forceUnlockDevice = async (deviceId) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/force-unlock`, {
      method: 'POST',
      headers: { 
        Authorization: `Bearer ${token}`
      }
    })
  }

  const setUsageTime = async (deviceId, minutes) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/usage-time`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ minutes })
    })
  }

  const commandDevice = async (deviceId, command, params) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/command`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ command, params })
    })
  }

  const controlApp = async (deviceId, action) => {
    await fetch(`${import.meta.env.VITE_API_URL || ''}/api/devices/${deviceId}/control-app`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ action })
    })
  }

  return (
    <SocketContext.Provider value={{
      devices, connected, vendas, setVendas, timeUpdates,
      lockDevice, unlockDevice, forceUnlockDevice, setUsageTime, commandDevice, controlApp,
      pusher: pusherRef.current
    }}>
      {children}
    </SocketContext.Provider>
  )
}

export const useSocket = () => useContext(SocketContext)
