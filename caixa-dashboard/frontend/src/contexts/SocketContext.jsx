import { createContext, useContext, useState, useEffect, useRef } from 'react'
import { io } from 'socket.io-client'
import { useAuth } from './AuthContext'

const SocketContext = createContext(null)

export function SocketProvider({ children }) {
  const { token } = useAuth()
  const [devices, setDevices] = useState([])
  const [connected, setConnected] = useState(false)
  const [vendas, setVendas] = useState([])
  const socketRef = useRef(null)

  useEffect(() => {
    if (!token) return

    const socket = io('http://localhost:3001', {
      auth: { token },
      transports: ['websocket', 'polling']
    })
    socketRef.current = socket

    socket.on('connect', () => {
      setConnected(true)
      socket.emit('dashboard_connect', { token })
    })

    socket.on('disconnect', () => setConnected(false))

    socket.on('devices_list', (list) => setDevices(list))

    socket.on('device_connected', (device) => {
      setDevices(prev => {
        const filtered = prev.filter(d => d.deviceId !== device.deviceId)
        return [...filtered, { ...device, online: true }]
      })
    })

    socket.on('device_disconnected', ({ deviceId, status }) => {
      setDevices(prev => prev.map(d => 
        d.deviceId === deviceId ? { ...d, online: false, status: status || 'offline' } : d
      ))
    })

    socket.on('device_status_update', ({ deviceId, status, lockReason, lockedAt, lockPassword, usageTimeLimit, usageStartTime }) => {
      setDevices(prev => prev.map(d => 
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
      ))
    })

    socket.on('sale_update', ({ sale }) => {
      setVendas(prev => [sale, ...prev])
    })

    socket.on('venda_added', (venda) => {
      setVendas(prev => [venda, ...prev])
    })

    return () => {
      socket.disconnect()
      socketRef.current = null
    }
  }, [token])

  const lockDevice = (deviceId, reason) => {
    socketRef.current?.emit('lock_device', { deviceId, reason })
  }

  const unlockDevice = (deviceId) => {
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
      devices, connected, vendas, setVendas,
      lockDevice, unlockDevice, forceUnlockDevice, setUsageTime, commandDevice, controlApp,
      socket: socketRef.current
    }}>
      {children}
    </SocketContext.Provider>
  )
}

export const useSocket = () => useContext(SocketContext)
