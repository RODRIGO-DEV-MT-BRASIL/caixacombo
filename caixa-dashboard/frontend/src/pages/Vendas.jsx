import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { ShoppingCart, Search, Loader2, TrendingUp, DollarSign, Calendar, Monitor, ChevronDown, ChevronUp } from 'lucide-react'

export default function Vendas() {
  const { token } = useAuth()
  const { vendas, setVendas } = useSocket()
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [expandedDevice, setExpandedDevice] = useState(null)

  useEffect(() => {
    fetch('/api/vendas', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setVendas(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  // Agrupar vendas por dispositivo
  const vendasPorDispositivo = vendas.reduce((acc, venda) => {
    const deviceId = venda.deviceId || 'sem_dispositivo'
    if (!acc[deviceId]) {
      acc[deviceId] = {
        deviceId,
        vendas: [],
        total: 0,
        count: 0
      }
    }
    acc[deviceId].vendas.push(venda)
    acc[deviceId].total += (venda.total || 0)
    acc[deviceId].count += 1
    return acc
  }, {})

  const dispositivos = Object.values(vendasPorDispositivo)

  const totalVendas = vendas.reduce((sum, v) => sum + (v.total || 0), 0)
  const vendasHoje = vendas.filter(v => {
    const d = new Date(v.createdAt)
    const hoje = new Date()
    return d.toDateString() === hoje.toDateString()
  })
  const totalHoje = vendasHoje.reduce((sum, v) => sum + (v.total || 0), 0)

  const paymentColors = {
    DINHEIRO: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    PIX: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    CARTAO: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    CREDITO: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    DEBITO: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
  }

  const filteredDispositivos = dispositivos.filter(d => {
    const s = search.toLowerCase()
    return d.deviceId.toLowerCase().includes(s)
  })

  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
            <DollarSign size={24} className="text-emerald-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalVendas.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Geral</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
            <TrendingUp size={24} className="text-blue-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalHoje.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Vendas Hoje</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
            <Monitor size={24} className="text-amber-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">{dispositivos.length}</p>
            <p className="text-xs text-gray-400">Dispositivos</p>
          </div>
        </div>
      </div>

      {/* Search */}
      <div className="relative w-full sm:max-w-xs">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="input-field pl-9 py-2.5 text-sm" placeholder="Buscar dispositivo..." />
      </div>

      {/* Dispositivos */}
      {loading ? (
        <div className="glass p-12 text-center">
          <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
          <p className="text-gray-400">Carregando vendas...</p>
        </div>
      ) : filteredDispositivos.length === 0 ? (
        <div className="glass p-12 text-center">
          <ShoppingCart size={48} className="mx-auto text-gray-600 mb-3" />
          <p className="text-gray-400">Nenhuma venda registrada</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredDispositivos.map(dispositivo => (
            <div key={dispositivo.deviceId} className="glass glass-hover">
              {/* Header do dispositivo */}
              <div 
                className="p-4 flex items-center justify-between cursor-pointer"
                onClick={() => setExpandedDevice(expandedDevice === dispositivo.deviceId ? null : dispositivo.deviceId)}
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center">
                    <Monitor size={24} className="text-white" />
                  </div>
                  <div>
                    <p className="font-semibold text-white">{dispositivo.deviceId.substring(0, 16)}...</p>
                    <p className="text-xs text-gray-500">{dispositivo.count} vendas</p>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-right">
                    <p className="text-lg font-bold text-emerald-400">R$ {dispositivo.total.toFixed(2)}</p>
                    <p className="text-xs text-gray-500">Total</p>
                  </div>
                  {expandedDevice === dispositivo.deviceId ? <ChevronUp size={20} className="text-gray-400" /> : <ChevronDown size={20} className="text-gray-400" />}
                </div>
              </div>

              {/* Lista de vendas do dispositivo */}
              {expandedDevice === dispositivo.deviceId && (
                <div className="border-t border-white/5 p-4 space-y-2">
                  {dispositivo.vendas.map(venda => (
                    <div key={venda.id} className="glass p-3 flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center shrink-0">
                        <ShoppingCart size={16} className="text-emerald-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-medium text-white">Venda #{venda.id?.toString().slice(-6)}</p>
                          {venda.formaPagamento && (
                            <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${paymentColors[venda.formaPagamento] || 'bg-gray-500/10 text-gray-400 border-gray-500/20'}`}>
                              {venda.formaPagamento}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-gray-500">
                          <Calendar size={10} className="inline mr-1" />
                          {new Date(venda.createdAt).toLocaleString('pt-BR')}
                        </p>
                      </div>
                      <p className="text-sm font-bold text-emerald-400 shrink-0">
                        R$ {(venda.total || 0).toFixed(2)}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
