import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { apiUrl } from '../utils/api'
import { ShoppingCart, Search, Loader2, TrendingUp, DollarSign, Calendar, Monitor, ChevronDown, ChevronUp, Printer, XCircle, Ban } from 'lucide-react'
import SenhaConfirmModal from '../components/SenhaConfirmModal'

export default function Vendas() {
  const { token } = useAuth()
  const { vendas, setVendas, devices } = useSocket()
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [expandedDevice, setExpandedDevice] = useState(null)
  const [expandedVenda, setExpandedVenda] = useState(null)
  const [senhaModal, setSenhaModal] = useState(null) // { action: 'reimprimir'|'cancelar', vendaId, venda }
  const [cancelarMotivo, setCancelarMotivo] = useState('')

  useEffect(() => {
    fetch(apiUrl('/api/vendas'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setVendas(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  // Agrupar vendas por dispositivo
  const vendasPorDispositivo = vendas.reduce((acc, venda) => {
    const deviceId = venda.deviceId || 'sem_dispositivo'
    // Resolver nome do dispositivo a partir da lista de conectados
    const connectedDevice = devices.find(d => d.deviceId === deviceId)
    const deviceName = venda.deviceName || connectedDevice?.deviceName || null
    if (!acc[deviceId]) {
      acc[deviceId] = {
        deviceId,
        deviceName,
        vendas: [],
        total: 0,
        count: 0
      }
    }
    // Atualizar nome se encontrado
    if (deviceName && !acc[deviceId].deviceName) {
      acc[deviceId].deviceName = deviceName
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

  const handleReimprimir = async (vendaId) => {
    try {
      const res = await fetch(apiUrl(`/api/vendas/${vendaId}/reimprimir`), {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      if (data.success) alert('✅ Comando de reimpressão enviado ao terminal')
      else alert('❌ Erro: ' + data.error)
    } catch { alert('❌ Erro ao enviar reimpressão') }
  }

  const handleCancelar = async (vendaId) => {
    try {
      const res = await fetch(apiUrl(`/api/vendas/${vendaId}/cancelar`), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ motivo: cancelarMotivo })
      })
      const data = await res.json()
      if (data.success) {
        alert('✅ Cancelamento enviado ao terminal Stone')
        setVendas(prev => prev.map(v => v.id == vendaId ? { ...v, cancelada: true, canceladaEm: new Date().toISOString(), motivoCancelamento: cancelarMotivo } : v))
        setCancelarMotivo('')
      } else alert('❌ Erro: ' + data.error)
    } catch { alert('❌ Erro ao cancelar venda') }
  }

  // Verificar se venda pode ser cancelada via Stone deeplink
  const formasStone = ['CARTAO_CREDITO', 'CARTAO_DEBITO', 'CREDITO', 'DEBITO', 'PIX']
  const canCancelStone = (v) => v.stoneAtk || v.atk || !formasStone.includes(v.formaPagamento)
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
                    <p className="font-semibold text-white">{dispositivo.deviceName || `Terminal ${dispositivo.deviceId.substring(0, 8)}`}</p>
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
                    <div key={venda.id}>
                      <div 
                        className="glass p-3 flex items-center gap-3 cursor-pointer hover:bg-white/5 transition-colors"
                        onClick={() => setExpandedVenda(expandedVenda === venda.id ? null : venda.id)}
                      >
                        <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center shrink-0">
                          <ShoppingCart size={16} className="text-emerald-400" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <p className={`text-sm font-medium ${venda.cancelada ? 'text-red-400 line-through' : 'text-white'}`}>Venda #{venda.id?.toString().slice(-6)}</p>
                            {venda.cancelada && (
                              <span className="px-2 py-0.5 rounded-lg text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">CANCELADA</span>
                            )}
                            {venda.formaPagamento && !venda.cancelada && (
                              <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${paymentColors[venda.formaPagamento] || 'bg-gray-500/10 text-gray-400 border-gray-500/20'}`}>
                                {venda.formaPagamento}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-gray-500">
                            <Calendar size={10} className="inline mr-1" />
                            {venda.createdAt ? new Date(venda.createdAt).toLocaleString('pt-BR') : venda.dataHora ? new Date(venda.dataHora).toLocaleString('pt-BR') : 'Data indisponível'}
                          </p>
                        </div>
                        <p className="text-sm font-bold text-emerald-400 shrink-0">
                          R$ {(venda.total || 0).toFixed(2)}
                        </p>
                        {expandedVenda === venda.id ? <ChevronUp size={14} className="text-gray-400" /> : <ChevronDown size={14} className="text-gray-400" />}
                      </div>
                      {/* Itens da venda */}
                      {expandedVenda === venda.id && venda.itens && (
                        <div className="mt-2 ml-11 space-y-1">
                          <div className="grid grid-cols-[1fr_auto_auto] gap-x-4 text-xs text-gray-500 mb-1 px-2">
                            <span>Produto</span>
                            <span className="text-center">Qtd</span>
                            <span className="text-right">Subtotal</span>
                          </div>
                          {venda.itens.map((item, idx) => (
                            <div key={idx} className="grid grid-cols-[1fr_auto_auto] gap-x-4 text-xs px-2 py-1 rounded hover:bg-white/5">
                              <span className="text-gray-300 truncate">{item.produtoNome || `Produto #${item.produtoId}`}</span>
                              <span className="text-gray-400">{item.quantidade}{item.unidade || 'un'}</span>
                              <span className="text-gray-300 text-right">R$ {(item.total || item.precoUnitario * item.quantidade).toFixed(2)}</span>
                            </div>
                          ))}
                          {venda.desconto > 0 && (
                            <div className="grid grid-cols-[1fr_auto_auto] gap-x-4 text-xs px-2 py-1 text-red-400">
                              <span>Desconto</span>
                              <span></span>
                              <span className="text-right">- R$ {venda.desconto.toFixed(2)}</span>
                            </div>
                          )}
                          <div className="grid grid-cols-[1fr_auto_auto] gap-x-4 text-xs px-2 py-1 border-t border-white/10 mt-1 pt-1">
                            <span className="text-white font-medium">Total</span>
                            <span></span>
                            <span className="text-emerald-400 font-bold text-right">R$ {(venda.total || 0).toFixed(2)}</span>
                          </div>
                          {/* Ações da venda */}
                          {!venda.cancelada && (
                            <div className="flex gap-2 mt-2 pt-2 border-t border-white/10">
                              <button
                                onClick={(e) => { e.stopPropagation(); setSenhaModal({ action: 'reimprimir', vendaId: venda.id, venda }) }}
                                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20 transition-colors"
                              >
                                <Printer size={12} /> Reimprimir
                              </button>
                              {canCancelStone(venda) ? (
                                <button
                                  onClick={(e) => { e.stopPropagation(); setSenhaModal({ action: 'cancelar', vendaId: venda.id, venda }) }}
                                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20 transition-colors"
                                >
                                  <XCircle size={12} /> Cancelar
                                </button>
                              ) : (
                                <span className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-gray-500/10 text-gray-500 border border-gray-500/20 cursor-not-allowed" title="Venda sem ATK - cancelamento indisponível via Stone">
                                  <XCircle size={12} /> Sem ATK
                                </span>
                              )}
                            </div>
                          )}
                          {venda.cancelada && (
                            <div className="mt-2 pt-2 border-t border-red-500/20">
                              <div className="flex items-center gap-2 text-xs text-red-400">
                                <Ban size={12} />
                                <span className="font-medium">Venda Cancelada</span>
                              </div>
                              {venda.motivoCancelamento && <p className="text-xs text-gray-500 mt-1">Motivo: {venda.motivoCancelamento}</p>}
                              {venda.canceladaPor && <p className="text-xs text-gray-500">Por: {venda.canceladaPor} em {new Date(venda.canceladaEm).toLocaleString('pt-BR')}</p>}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
      {/* Modal de confirmação de senha para ações sensíveis */}
      <SenhaConfirmModal
        open={!!senhaModal}
        onClose={() => { setSenhaModal(null); setCancelarMotivo('') }}
        onConfirm={() => {
          if (senhaModal?.action === 'reimprimir') {
            handleReimprimir(senhaModal.vendaId)
          } else if (senhaModal?.action === 'cancelar') {
            handleCancelar(senhaModal.vendaId)
          }
          setSenhaModal(null)
        }}
        title={senhaModal?.action === 'reimprimir' ? 'Reimprimir Venda' : 'Cancelar Venda'}
        description={senhaModal?.action === 'reimprimir'
          ? `Confirme para reimprimir a venda #${senhaModal?.vendaId?.toString().slice(-6)}`
          : `Confirme para cancelar a venda #${senhaModal?.vendaId?.toString().slice(-6)}`
        }
      />
    </div>
  )
}
