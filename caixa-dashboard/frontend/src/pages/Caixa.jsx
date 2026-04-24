import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { DollarSign, Search, Loader2, TrendingUp, Monitor, ChevronDown, ChevronUp, Plus, ArrowUpCircle, ArrowDownCircle } from 'lucide-react'

export default function Caixa() {
  const { token } = useAuth()
  const [operacoes, setOperacoes] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [expandedDevice, setExpandedDevice] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({ tipo: 'abertura', valor: '', deviceId: '' })

  useEffect(() => {
    fetch('/api/operacoes', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setOperacoes(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  // Agrupar operações por dispositivo
  const operacoesPorDispositivo = operacoes.reduce((acc, operacao) => {
    const deviceId = operacao.deviceId || 'geral'
    if (!acc[deviceId]) {
      acc[deviceId] = {
        deviceId,
        operacoes: [],
        totalAbertura: 0,
        totalFechamento: 0,
        saldo: 0
      }
    }
    acc[deviceId].operacoes.push(operacao)
    if (operacao.tipo === 'abertura') {
      acc[deviceId].totalAbertura += (operacao.valor || 0)
    } else if (operacao.tipo === 'fechamento') {
      acc[deviceId].totalFechamento += (operacao.valor || 0)
    }
    acc[deviceId].saldo = acc[deviceId].totalAbertura - acc[deviceId].totalFechamento
    return acc
  }, {})

  const dispositivos = Object.values(operacoesPorDispositivo)

  const totalAbertura = operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0)
  const totalFechamento = operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0)
  const saldoGeral = totalAbertura - totalFechamento

  const tipoColors = {
    abertura: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    fechamento: 'bg-red-500/10 text-red-400 border-red-500/20',
    suprimento: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    sangria: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  }

  const filteredDispositivos = dispositivos.filter(d => {
    const s = search.toLowerCase()
    return d.deviceId.toLowerCase().includes(s)
  })

  const handleSubmit = async (e) => {
    e.preventDefault()
    const body = {
      tipo: form.tipo,
      valor: parseFloat(form.valor),
      deviceId: form.deviceId || null,
      observacao: form.observacao || ''
    }

    await fetch('/api/operacoes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body)
    })
    setShowModal(false)
    setForm({ tipo: 'abertura', valor: '', deviceId: '' })
    fetch('/api/operacoes', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setOperacoes(data))
  }

  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
            <ArrowUpCircle size={24} className="text-emerald-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalAbertura.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Abertura</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center">
            <ArrowDownCircle size={24} className="text-red-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalFechamento.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Fechamento</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
            <DollarSign size={24} className="text-blue-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {saldoGeral.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Saldo Geral</p>
          </div>
        </div>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div className="relative w-full sm:max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="input-field pl-9 py-2.5 text-sm" placeholder="Buscar dispositivo..." />
        </div>
        <button onClick={() => setShowModal(true)} className="btn-primary flex items-center gap-2 text-sm">
          <Plus size={16} /> Nova Operação
        </button>
      </div>

      {/* Dispositivos */}
      {loading ? (
        <div className="glass p-12 text-center">
          <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
          <p className="text-gray-400">Carregando operações...</p>
        </div>
      ) : filteredDispositivos.length === 0 ? (
        <div className="glass p-12 text-center">
          <DollarSign size={48} className="mx-auto text-gray-600 mb-3" />
          <p className="text-gray-400">Nenhuma operação registrada</p>
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
                    <p className="font-semibold text-white">{dispositivo.deviceId === 'geral' ? 'Geral' : dispositivo.deviceId.substring(0, 16)}...</p>
                    <p className="text-xs text-gray-500">{dispositivo.operacoes.length} operações</p>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-right">
                    <p className={`text-lg font-bold ${dispositivo.saldo >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                      R$ {dispositivo.saldo.toFixed(2)}
                    </p>
                    <p className="text-xs text-gray-500">Saldo</p>
                  </div>
                  {expandedDevice === dispositivo.deviceId ? <ChevronUp size={20} className="text-gray-400" /> : <ChevronDown size={20} className="text-gray-400" />}
                </div>
              </div>

              {/* Lista de operações do dispositivo */}
              {expandedDevice === dispositivo.deviceId && (
                <div className="border-t border-white/5 p-4 space-y-2">
                  {dispositivo.operacoes.map(operacao => (
                    <div key={operacao.id} className="glass p-3 flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-lg ${tipoColors[operacao.tipo] || 'bg-gray-500/10 text-gray-400'} flex items-center justify-center shrink-0`}>
                        {operacao.tipo === 'abertura' ? <ArrowUpCircle size={16} /> : 
                         operacao.tipo === 'fechamento' ? <ArrowDownCircle size={16} /> :
                         <DollarSign size={16} />}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <p className="text-sm font-medium text-white capitalize">{operacao.tipo}</p>
                          <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${tipoColors[operacao.tipo] || 'bg-gray-500/10 text-gray-400 border-gray-500/20'}`}>
                            {operacao.tipo}
                          </span>
                        </div>
                        <p className="text-xs text-gray-500">
                          {new Date(operacao.createdAt).toLocaleString('pt-BR')}
                          {operacao.observacao && ` • ${operacao.observacao}`}
                        </p>
                      </div>
                      <p className={`text-sm font-bold shrink-0 ${operacao.tipo === 'abertura' || operacao.tipo === 'suprimento' ? 'text-emerald-400' : 'text-red-400'}`}>
                        {operacao.tipo === 'abertura' || operacao.tipo === 'suprimento' ? '+' : '-'}R$ {(operacao.valor || 0).toFixed(2)}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Modal Nova Operação */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setShowModal(false)}>
          <div className="glass p-6 w-full max-w-md glow-blue max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-white">Nova Operação de Caixa</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-white"><Plus size={20} className="rotate-45" /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Tipo</label>
                <select value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })} className="input-field">
                  <option value="abertura">Abertura</option>
                  <option value="fechamento">Fechamento</option>
                  <option value="suprimento">Suprimento</option>
                  <option value="sangria">Sangria</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Valor (R$)</label>
                <input type="number" step="0.01" value={form.valor} onChange={e => setForm({ ...form, valor: e.target.value })} className="input-field" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">ID do Dispositivo (opcional)</label>
                <input type="text" value={form.deviceId} onChange={e => setForm({ ...form, deviceId: e.target.value })} className="input-field" placeholder="Deixe vazio para geral" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Observação (opcional)</label>
                <input type="text" value={form.observacao} onChange={e => setForm({ ...form, observacao: e.target.value })} className="input-field" />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowModal(false)} className="btn-ghost flex-1">Cancelar</button>
                <button type="submit" className="btn-primary flex-1">Criar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
