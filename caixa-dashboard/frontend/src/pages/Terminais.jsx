import { useState, useEffect } from 'react'
import { Monitor, Wifi, WifiOff, Lock, Unlock, Smartphone, Search } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'

export default function Terminais() {
  const { user } = useAuth()
  const { devices } = useSocket()
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedEmpresa, setSelectedEmpresa] = useState(null)

  const isAdmin = user?.role === 'admin'
  const userEmpresaId = user?.role === 'empresa' ? user.empresaId : null

  // Filtrar dispositivos
  const filteredDevices = devices.filter(device => {
    const matchesSearch = device.deviceName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         device.deviceId?.toLowerCase().includes(searchTerm.toLowerCase())
    
    // Se for empresa, mostrar apenas dispositivos da empresa
    if (userEmpresaId) {
      return matchesSearch && device.empresaId === userEmpresaId
    }
    
    // Se for admin e selecionou uma empresa, mostrar dispositivos da empresa
    if (isAdmin && selectedEmpresa) {
      return matchesSearch && device.empresaId === selectedEmpresa
    }
    
    // Se for admin e não selecionou empresa, mostrar todos
    return matchesSearch
  })

  // Obter lista única de empresas dos dispositivos
  const empresasDisponiveis = [...new Set(devices.map(d => d.empresaId).filter(Boolean))]

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-white mb-2">Terminais</h1>
        <p className="text-gray-400">
          {isAdmin ? 'Gerencie os terminais conectados' : 'Visualize seus terminais conectados'}
        </p>
      </div>

      {/* Filtros */}
      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Buscar terminal..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-gray-800/50 border border-gray-700 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-blue-500"
          />
        </div>
        
        {isAdmin && empresasDisponiveis.length > 0 && (
          <select
            value={selectedEmpresa || ''}
            onChange={(e) => setSelectedEmpresa(e.target.value || null)}
            className="px-4 py-2.5 bg-gray-800/50 border border-gray-700 rounded-xl text-white focus:outline-none focus:border-blue-500"
          >
            <option value="">Todas as empresas</option>
            {empresasDisponiveis.map(empresaId => (
              <option key={empresaId} value={empresaId}>Empresa {empresaId}</option>
            ))}
          </select>
        )}
      </div>

      {/* Lista de terminais */}
      <div className="grid gap-4">
        {filteredDevices.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <Monitor size={48} className="mx-auto mb-4 opacity-50" />
            <p>Nenhum terminal encontrado</p>
          </div>
        ) : (
          filteredDevices.map(device => (
            <div
              key={device.deviceId}
              className="bg-gray-800/50 border border-gray-700 rounded-xl p-4 hover:border-gray-600 transition-colors"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className={`p-3 rounded-xl ${
                    device.online ? 'bg-emerald-500/20 text-emerald-400' : 'bg-gray-700 text-gray-400'
                  }`}>
                    {device.online ? <Wifi size={24} /> : <WifiOff size={24} />}
                  </div>
                  
                  <div>
                    <h3 className="font-semibold text-white">{device.deviceName}</h3>
                    <p className="text-sm text-gray-400">{device.deviceId}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <Smartphone size={14} className="text-gray-500" />
                      <span className="text-xs text-gray-500">{device.deviceType}</span>
                      {device.empresaId && (
                        <>
                          <span className="text-gray-600">•</span>
                          <span className="text-xs text-blue-400">Empresa {device.empresaId}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className={`px-3 py-1.5 rounded-full text-xs font-medium ${
                    device.status === 'online' ? 'bg-emerald-500/20 text-emerald-400' :
                    device.status === 'locked' ? 'bg-red-500/20 text-red-400' :
                    device.status === 'in_use' ? 'bg-blue-500/20 text-blue-400' :
                    'bg-gray-700 text-gray-400'
                  }`}>
                    {device.status === 'online' ? 'Online' :
                     device.status === 'locked' ? 'Bloqueado' :
                     device.status === 'in_use' ? 'Em uso' :
                     device.status}
                  </div>
                </div>
              </div>

              {/* Informações adicionais */}
              {device.usageTimeLimit && (
                <div className="mt-3 pt-3 border-t border-gray-700">
                  <p className="text-xs text-gray-400">
                    Tempo de uso: {device.usageTimeLimit} minutos
                  </p>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Legenda */}
      <div className="mt-6 p-4 bg-gray-800/30 rounded-xl border border-gray-700">
        <h4 className="text-sm font-medium text-white mb-2">Legenda de status</h4>
        <div className="flex flex-wrap gap-4 text-xs text-gray-400">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-400"></div>
            <span>Online</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-blue-400"></div>
            <span>Em uso</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-red-400"></div>
            <span>Bloqueado</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-gray-400"></div>
            <span>Offline</span>
          </div>
        </div>
      </div>
    </div>
  )
}
