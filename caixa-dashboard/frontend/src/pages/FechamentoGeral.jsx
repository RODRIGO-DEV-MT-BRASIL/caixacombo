import { useState, useEffect, useMemo } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToastContext } from '../contexts/ToastContext'
import { apiUrl } from '../utils/api'
import { 
  ArrowLeft, Lock, ArrowUpCircle, ArrowDownCircle, Wallet, CreditCard, 
  Smartphone, DollarSign, Monitor, TrendingUp, TrendingDown, AlertTriangle,
  CheckCircle2, Loader2, FileText, Printer, Shield, BarChart3, Receipt,
  PiggyBank, CircleDot, Activity
} from 'lucide-react'

export default function FechamentoGeral({ onBack }) {
  const { user, token } = useAuth()
  const { socket } = useSocket()
  const toast = useToastContext()
  const [operacoes, setOperacoes] = useState([])
  const [vendas, setVendas] = useState([])
  const [empresas, setEmpresas] = useState([])
  const [dispositivosConectados, setDispositivosConectados] = useState([])
  const [loading, setLoading] = useState(true)
  const [confirming, setConfirming] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [expandedTerminal, setExpandedTerminal] = useState(null)
  const [filterEmpresa, setFilterEmpresa] = useState('')
  const [filterTerminal, setFilterTerminal] = useState('')

  useEffect(() => {
    fetch(apiUrl('/api/operacoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setOperacoes(Array.isArray(data) ? data : data.operacoes || []); setLoading(false) })
      .catch(() => setLoading(false))

    fetch(apiUrl('/api/vendas'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setVendas(Array.isArray(data) ? data : data.data || []))
      .catch(() => setVendas([]))

    fetch(apiUrl('/api/dispositivos'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setDispositivosConectados(Array.isArray(data) ? data : []))
      .catch(() => setDispositivosConectados([]))

    if (user?.role === 'admin' || user?.role === 'empresa') {
      fetch(apiUrl('/api/empresas'), { headers: { Authorization: `Bearer ${token}` } })
        .then(res => res.json())
        .then(data => setEmpresas(Array.isArray(data) ? data : []))
        .catch(() => {})
    }
  }, [])

  useEffect(() => {
    if (!socket) return
    const handlers = {
      operacao_adicionada: (o) => setOperacoes(prev => [...prev, o]),
      operacoes_sync: (data) => setOperacoes(Array.isArray(data) ? data : data.operacoes || []),
      venda_added: (v) => setVendas(prev => [...prev, v]),
      vendas_sync: (data) => setVendas(Array.isArray(data) ? data : []),
    }
    Object.entries(handlers).forEach(([e, h]) => socket.on(e, h))
    return () => Object.entries(handlers).forEach(([e, h]) => socket.off(e, h))
  }, [socket])

  const TEST_DEVICE_IDS = ['test-check', 'test-local', 'test-render', 'deploy-check']

  // Aplicar filtros - empresa vê tudo, admin pode filtrar
  const operacoesFiltradas = operacoes.filter(op => {
    if (user?.role === 'admin' && filterEmpresa) {
      if (op.empresaId !== filterEmpresa) return false
    }
    if (filterTerminal && op.deviceId !== filterTerminal) return false
    return true
  })

  const vendasFiltradas = vendas.filter(v => {
    if (user?.role === 'admin' && filterEmpresa) {
      if (v.empresaId !== filterEmpresa) return false
    }
    if (filterTerminal && v.deviceId !== filterTerminal) return false
    return true
  })

  const dispositivos = useMemo(() => {
    const porDispositivo = operacoesFiltradas.reduce((acc, op) => {
      const deviceId = op.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return acc
      if (!acc[deviceId]) acc[deviceId] = { deviceId, operacoes: [], deviceName: dispositivosConectados.find(d => d.deviceId === deviceId)?.deviceName || null }
      acc[deviceId].operacoes.push(op)
      return acc
    }, {})
    vendasFiltradas.forEach(v => {
      const deviceId = v.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return
      if (!porDispositivo[deviceId]) porDispositivo[deviceId] = { deviceId, operacoes: [], deviceName: dispositivosConectados.find(d => d.deviceId === deviceId)?.deviceName || null }
    })
    return Object.values(porDispositivo)
  }, [operacoesFiltradas, vendasFiltradas, dispositivosConectados])

  const caixaAtual = useMemo(() => {
    const sessoes = {}
    dispositivos.forEach(d => {
      const opsSorted = [...d.operacoes].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))
      const aberturas = opsSorted.filter(o => o.tipo === 'abertura')
      const ultimaAbertura = aberturas[aberturas.length - 1] || null
      if (!ultimaAbertura) {
        sessoes[d.deviceId] = { aberto: false, aberturaTimestamp: null, fechamentoTimestamp: null, aberturaEm: null, fechamentoEm: null, operador: null }
        return
      }
      const fechamentosApos = opsSorted.filter(o => o.tipo === 'fechamento' && o.timestamp > ultimaAbertura.timestamp)
      const ultimoFechamento = fechamentosApos.length > 0 ? fechamentosApos[fechamentosApos.length - 1] : null
      sessoes[d.deviceId] = {
        aberto: fechamentosApos.length === 0,
        aberturaTimestamp: ultimaAbertura.timestamp,
        fechamentoTimestamp: ultimoFechamento ? ultimoFechamento.timestamp : null,
        aberturaEm: ultimaAbertura.dataHora,
        fechamentoEm: ultimoFechamento ? ultimoFechamento.dataHora : null,
        operador: ultimaAbertura.nomeOperador
      }
    })
    return sessoes
  }, [dispositivos])

  const caixasAbertos = dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto)

  const totais = useMemo(() => {
    // Calcular totais apenas da sessão ATUAL de cada terminal
    let ab = 0, sup = 0, san = 0, ft = 0, din = 0, pix = 0, cred = 0, deb = 0
    dispositivos.forEach(d => {
      const sessao = caixaAtual[d.deviceId]
      if (!sessao || sessao.aberturaTimestamp === null) return
      
      const ts = sessao.aberturaTimestamp
      const tf = sessao.aberto ? Date.now() : (sessao.fechamentoTimestamp || Date.now())
      
      // Operações da sessão atual
      const opsSessao = d.operacoes.filter(o => (o.timestamp || 0) >= ts && (o.timestamp || 0) <= tf)
      ab += opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
      sup += opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
      san += opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)
      ft += opsSessao.filter(o => o.tipo === 'fechamento').reduce((s, o) => s + (o.valor || 0), 0)
      
      // Vendas da sessão atual
      const vendasSessao = vendasFiltradas.filter(v => {
        if (!v.deviceId || TEST_DEVICE_IDS.includes(v.deviceId)) return false
        const vt = new Date(v.createdAt).getTime()
        return vt >= ts && vt <= tf
      })
      din += vendasSessao.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0)
      pix += vendasSessao.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0)
      cred += vendasSessao.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0)
      deb += vendasSessao.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
    })
    const tv = din + pix + cred + deb
    const saldo = ab + sup - san - ft
    return { abertura: ab, suprimento: sup, sangria: san, fechamento: ft, dinheiro: din, pix, credito: cred, debito: deb, vendas: tv, saldo }
  }, [operacoes, vendas, dispositivos, caixaAtual])

  const handleFechamentoGeral = async () => {
    setConfirming(true)
    try {
      if (caixasAbertos.length > 0) {
        toast.error(`Existem ${caixasAbertos.length} caixas abertos. Feche todos antes do fechamento geral.`)
        setConfirming(false)
        return
      }

      const body = {
        tipo: 'fechamento',
        valor: totais.saldo,
        deviceId: null,
        observacao: `Fechamento geral - Vendas: R$ ${totais.vendas.toFixed(2)}`
      }

      const response = await fetch(apiUrl('/api/operacoes'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })

      if (response.ok) {
        // Gerar PDF
        try {
          const pdfRes = await fetch(apiUrl('/api/fechamento-pdf'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
            body: JSON.stringify({
              totalAbertura: totais.abertura,
              totalSuprimento: totais.suprimento,
              totalSangria: totais.sangria,
              totalFechamento: totais.saldo,
              totalVendas: totais.vendas,
              dataHora: new Date().toLocaleString('pt-BR'),
              operacoes: operacoes.filter(o => o.tipo !== 'fechamento'),
              vendas: vendas
            })
          })
          if (pdfRes.ok) {
            const blob = await pdfRes.blob()
            const url = window.URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = `fechamento-geral-${new Date().toISOString().split('T')[0]}.pdf`
            a.click()
            window.URL.revokeObjectURL(url)
          }
        } catch (e) { console.error('Erro PDF:', e) }

        if (socket) {
          socket.emit('fechamento_geral', {
            valor: totais.saldo,
            totalVendas: totais.vendas,
            dataHora: new Date().toISOString(),
            observacao: `Fechamento geral - Vendas: R$ ${totais.vendas.toFixed(2)}`
          })
        }

        toast.success('Fechamento geral realizado com sucesso!')
        setShowConfirm(false)
      }
    } catch (err) {
      toast.error('Erro ao realizar fechamento')
    } finally {
      setConfirming(false)
    }
  }

  const maxPagamento = Math.max(totais.dinheiro, totais.pix, totais.credito, totais.debito, 1)

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <Loader2 size={40} className="animate-spin mx-auto text-blue-400 mb-4" />
          <p className="text-gray-400">Carregando dados para fechamento...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <button onClick={onBack} className="p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
            <ArrowLeft size={20} className="text-gray-400" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-600 to-red-400 flex items-center justify-center">
                <Lock size={20} className="text-white" />
              </div>
              Fechamento Geral
            </h1>
            <p className="text-sm text-gray-500 mt-1">{new Date().toLocaleDateString('pt-BR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
          </div>
        </div>
        <div className="flex items-center gap-3 flex-wrap">
          {user?.role === 'admin' && (
            <>
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
              <select
                value={filterTerminal}
                onChange={(e) => setFilterTerminal(e.target.value)}
                className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
              >
                <option value="">Todos os Terminais</option>
                {dispositivosConectados.map(d => (
                  <option key={d.deviceId} value={d.deviceId}>{d.deviceName || d.deviceId}</option>
                ))}
              </select>
            </>
          )}
          {caixasAbertos.length > 0 && (
            <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500/10 border border-amber-500/20">
              <AlertTriangle size={16} className="text-amber-400" />
              <span className="text-sm text-amber-400 font-medium">{caixasAbertos.length} caixa(s) aberto(s)</span>
            </div>
          )}
          <button
            onClick={() => setShowConfirm(true)}
            disabled={caixasAbertos.length > 0}
            className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-500 hover:to-red-400 text-white rounded-xl font-semibold text-sm transition-all disabled:opacity-40 disabled:cursor-not-allowed shadow-lg shadow-red-500/20"
          >
            <Shield size={18} />
            Confirmar Fechamento
          </button>
        </div>
      </div>

      {/* Alerta de caixas abertos */}
      {caixasAbertos.length > 0 && (
        <div className="glass p-4 border border-amber-500/30 rounded-xl">
          <div className="flex items-start gap-3">
            <AlertTriangle size={20} className="text-amber-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-amber-400">Caixas abertos detectados</p>
              <p className="text-xs text-gray-400 mt-1">Feche os caixas individuais antes de realizar o fechamento geral:</p>
              <div className="flex flex-wrap gap-2 mt-2">
                {caixasAbertos.map(d => (
                  <span key={d.deviceId} className="px-3 py-1 rounded-lg bg-amber-500/10 text-amber-400 text-xs font-medium border border-amber-500/20">
                    {d.deviceId === 'geral' ? 'Geral' : d.deviceName || d.deviceId}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Grid principal */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Coluna esquerda - Operações de Caixa */}
        <div className="space-y-4">
          <div className="glass p-5 rounded-xl">
            <h3 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
              <Activity size={16} className="text-blue-400" /> Operações de Caixa
            </h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 rounded-lg bg-emerald-500/5 border border-emerald-500/10">
                <div className="flex items-center gap-3">
                  <ArrowUpCircle size={18} className="text-emerald-400" />
                  <span className="text-sm text-gray-300">Abertura</span>
                </div>
                <span className="text-emerald-400 font-bold">R$ {totais.abertura.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-lg bg-blue-500/5 border border-blue-500/10">
                <div className="flex items-center gap-3">
                  <PiggyBank size={18} className="text-blue-400" />
                  <span className="text-sm text-gray-300">Suprimento</span>
                </div>
                <span className="text-blue-400 font-bold">R$ {totais.suprimento.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-lg bg-amber-500/5 border border-amber-500/10">
                <div className="flex items-center gap-3">
                  <Wallet size={18} className="text-amber-400" />
                  <span className="text-sm text-gray-300">Sangria</span>
                </div>
                <span className="text-amber-400 font-bold">R$ {totais.sangria.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-lg bg-red-500/5 border border-red-500/10">
                <div className="flex items-center gap-3">
                  <ArrowDownCircle size={18} className="text-red-400" />
                  <span className="text-sm text-gray-300">Fechamento Anterior</span>
                </div>
                <span className="text-red-400 font-bold">R$ {totais.fechamento.toFixed(2)}</span>
              </div>
            </div>
          </div>

          {/* Saldo Final */}
          <div className="glass p-5 rounded-xl border-2 border-red-500/30">
            <h3 className="text-sm font-semibold text-red-400 mb-4 flex items-center gap-2">
              <TrendingDown size={16} /> Saldo do Caixa
            </h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-400">Entradas</span>
                <span className="text-emerald-400 font-bold">R$ {(totais.abertura + totais.suprimento).toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-400">Saídas</span>
                <span className="text-red-400 font-bold">R$ {(totais.sangria + totais.fechamento).toFixed(2)}</span>
              </div>
              <div className="border-t border-white/10 pt-3">
                <div className="flex items-center justify-between">
                  <span className="text-white font-bold">Saldo Final</span>
                  <span className={`text-2xl font-black ${totais.saldo >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                    R$ {totais.saldo.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Coluna central - Vendas por Pagamento */}
        <div className="space-y-4">
          <div className="glass p-5 rounded-xl">
            <h3 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
              <BarChart3 size={16} className="text-emerald-400" /> Vendas por Pagamento
            </h3>
            <div className="space-y-4">
              {[
                { label: 'Dinheiro', valor: totais.dinheiro, color: 'emerald', icon: DollarSign },
                { label: 'PIX', valor: totais.pix, color: 'blue', icon: Smartphone },
                { label: 'Crédito', valor: totais.credito, color: 'amber', icon: CreditCard },
                { label: 'Débito', valor: totais.debito, color: 'cyan', icon: CreditCard },
              ].map(item => {
                const pct = maxPagamento > 0 ? (item.valor / maxPagamento) * 100 : 0
                const colorMap = {
                  emerald: { bar: 'from-emerald-600 to-emerald-400', text: 'text-emerald-400', bg: 'bg-emerald-500/10' },
                  blue: { bar: 'from-blue-600 to-blue-400', text: 'text-blue-400', bg: 'bg-blue-500/10' },
                  amber: { bar: 'from-amber-600 to-amber-400', text: 'text-amber-400', bg: 'bg-amber-500/10' },
                  cyan: { bar: 'from-cyan-600 to-cyan-400', text: 'text-cyan-400', bg: 'bg-cyan-500/10' },
                }
                const c = colorMap[item.color]
                return (
                  <div key={item.label}>
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <item.icon size={14} className={c.text} />
                        <span className="text-sm text-gray-300">{item.label}</span>
                      </div>
                      <span className={`text-sm font-bold ${c.text}`}>R$ {item.valor.toFixed(2)}</span>
                    </div>
                    <div className="h-3 bg-gray-800 rounded-full overflow-hidden">
                      <div
                        className={`h-full bg-gradient-to-r ${c.bar} rounded-full transition-all duration-700`}
                        style={{ width: `${Math.max(pct, 2)}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
            <div className="border-t border-white/10 mt-4 pt-4">
              <div className="flex items-center justify-between">
                <span className="text-white font-bold flex items-center gap-2">
                  <Receipt size={16} className="text-emerald-400" /> Total Vendas
                </span>
                <span className="text-xl font-black text-emerald-400">R$ {totais.vendas.toFixed(2)}</span>
              </div>
            </div>
          </div>

          {/* Donut visual simples */}
          <div className="glass p-5 rounded-xl">
            <h3 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
              <CircleDot size={16} className="text-purple-400" /> Distribuição
            </h3>
            {totais.vendas > 0 ? (
              <div className="space-y-2">
                {[
                  { label: 'Dinheiro', valor: totais.dinheiro, pct: ((totais.dinheiro / totais.vendas) * 100).toFixed(1), color: 'bg-emerald-500' },
                  { label: 'PIX', valor: totais.pix, pct: ((totais.pix / totais.vendas) * 100).toFixed(1), color: 'bg-blue-500' },
                  { label: 'Crédito', valor: totais.credito, pct: ((totais.credito / totais.vendas) * 100).toFixed(1), color: 'bg-amber-500' },
                  { label: 'Débito', valor: totais.debito, pct: ((totais.debito / totais.vendas) * 100).toFixed(1), color: 'bg-cyan-500' },
                ].map(item => (
                  <div key={item.label} className="flex items-center gap-3">
                    <div className={`w-3 h-3 rounded-full ${item.color}`} />
                    <span className="text-sm text-gray-300 flex-1">{item.label}</span>
                    <span className="text-xs text-gray-500">{item.pct}%</span>
                    <span className="text-sm font-medium text-white">R$ {item.valor.toFixed(2)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500 text-center py-4 text-sm">Nenhuma venda registrada</p>
            )}
          </div>
        </div>

        {/* Coluna direita - Terminais */}
        <div className="space-y-4">
          <div className="glass p-5 rounded-xl">
            <h3 className="text-sm font-semibold text-gray-300 mb-4 flex items-center gap-2">
              <Monitor size={16} className="text-cyan-400" /> Detalhamento por Terminal
            </h3>
            {dispositivos.length === 0 ? (
              <p className="text-gray-500 text-center py-8 text-sm">Nenhum terminal registrado</p>
            ) : (
              <div className="space-y-3">
                {dispositivos.map(d => {
                  const sessao = caixaAtual[d.deviceId]
                  const ts = sessao?.aberturaTimestamp
                  const tf = sessao?.aberto ? Date.now() : (sessao?.fechamentoTimestamp || Date.now())
                  const opsSessao = d.operacoes.filter(o => o.timestamp >= (ts || 0) && o.timestamp <= tf)
                  const vDev = vendas.filter(v => {
                    if (!ts) return (v.deviceId || 'geral') === d.deviceId
                    const vTime = new Date(v.createdAt || v.dataHora).getTime()
                    const vDev2 = v.deviceId || 'geral'
                    const matchDevice = d.deviceId === 'geral' || vDev2 === d.deviceId
                    return vTime >= ts && vTime <= tf && matchDevice
                  })
                  const totalVendasDev = vDev.reduce((s, v) => s + (v.total || 0), 0)
                  const dinDev = vDev.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0)
                  const pixDev = vDev.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0)
                  const credDev = vDev.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0)
                  const debDev = vDev.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
                  const aberturaDev = opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
                  const suprimentoDev = opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
                  const sangriaDev = opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)
                  const fechamentoDev = opsSessao.filter(o => o.tipo === 'fechamento').reduce((s, o) => s + (o.valor || 0), 0)
                  const saldoDev = aberturaDev + suprimentoDev - sangriaDev + totalVendasDev - fechamentoDev
                  const isAberto = sessao?.aberto
                  const name = d.deviceId === 'geral' ? 'Geral' : d.deviceName || d.deviceId
                  const isExpanded = expandedTerminal === d.deviceId

                  // Produtos vendidos neste terminal
                  const produtosVendidos = {}
                  vDev.forEach(v => {
                    if (v.itens && Array.isArray(v.itens)) {
                      v.itens.forEach(item => {
                        const key = item.produtoNome || item.nome || 'N/A'
                        if (!produtosVendidos[key]) produtosVendidos[key] = { qtd: 0, total: 0 }
                        produtosVendidos[key].qtd += item.quantidade || 0
                        produtosVendidos[key].total += item.total || (item.quantidade * item.precoUnitario) || 0
                      })
                    }
                  })
                  const topProdutos = Object.entries(produtosVendidos).sort((a, b) => b[1].total - a[1].total)

                  return (
                    <div key={d.deviceId} className={`rounded-xl bg-white/[0.02] border transition-colors cursor-pointer ${isExpanded ? 'border-blue-500/30 bg-blue-500/[0.03]' : 'border-white/5 hover:border-white/10'}`}>
                      <div className="p-4" onClick={() => setExpandedTerminal(isExpanded ? null : d.deviceId)}>
                        <div className="flex items-center justify-between mb-3">
                          <div className="flex items-center gap-2">
                            <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${isAberto ? 'bg-emerald-500/20' : 'bg-gray-500/20'}`}>
                              <Monitor size={14} className={isAberto ? 'text-emerald-400' : 'text-gray-400'} />
                            </div>
                            <span className="text-sm font-medium text-white">{name}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className={`px-2 py-0.5 rounded-lg text-[10px] font-bold ${isAberto ? 'bg-emerald-500/20 text-emerald-400' : 'bg-gray-500/20 text-gray-400'}`}>
                              {isAberto ? 'ABERTO' : 'FECHADO'}
                            </span>
                            <span className={`font-bold text-sm ${saldoDev >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>R$ {saldoDev.toFixed(2)}</span>
                            <svg className={`w-4 h-4 text-gray-400 transition-transform ${isExpanded ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-500">Vendas</span>
                            <span className="text-emerald-400">R$ {totalVendasDev.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-500">Abertura</span>
                            <span className="text-emerald-400">R$ {aberturaDev.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-500">Suprimento</span>
                            <span className="text-blue-400">R$ {suprimentoDev.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-500">Sangria</span>
                            <span className="text-amber-400">R$ {sangriaDev.toFixed(2)}</span>
                          </div>
                        </div>
                      </div>

                      {/* Detalhes expandidos */}
                      {isExpanded && (
                        <div className="border-t border-white/5 p-4 space-y-4">
                          {/* Pagamentos */}
                          <div>
                            <p className="text-xs font-semibold text-gray-400 mb-2">Formas de Pagamento</p>
                            <div className="grid grid-cols-2 gap-2">
                              <div className="flex justify-between text-xs p-2 rounded-lg bg-emerald-500/5">
                                <span className="text-gray-400">Dinheiro</span>
                                <span className="text-emerald-400 font-medium">R$ {dinDev.toFixed(2)}</span>
                              </div>
                              <div className="flex justify-between text-xs p-2 rounded-lg bg-blue-500/5">
                                <span className="text-gray-400">PIX</span>
                                <span className="text-blue-400 font-medium">R$ {pixDev.toFixed(2)}</span>
                              </div>
                              <div className="flex justify-between text-xs p-2 rounded-lg bg-amber-500/5">
                                <span className="text-gray-400">Crédito</span>
                                <span className="text-amber-400 font-medium">R$ {credDev.toFixed(2)}</span>
                              </div>
                              <div className="flex justify-between text-xs p-2 rounded-lg bg-cyan-500/5">
                                <span className="text-gray-400">Débito</span>
                                <span className="text-cyan-400 font-medium">R$ {debDev.toFixed(2)}</span>
                              </div>
                            </div>
                          </div>

                          {/* Operações */}
                          {opsSessao.length > 0 && (
                            <div>
                              <p className="text-xs font-semibold text-gray-400 mb-2">Operações ({opsSessao.length})</p>
                              <div className="space-y-1 max-h-32 overflow-y-auto">
                                {opsSessao.map(op => (
                                  <div key={op.id} className="flex items-center justify-between text-xs py-1.5 px-2 rounded-lg bg-white/[0.02]">
                                    <div className="flex items-center gap-2">
                                      <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${op.tipo === 'abertura' ? 'bg-emerald-500/10 text-emerald-400' : op.tipo === 'suprimento' ? 'bg-blue-500/10 text-blue-400' : op.tipo === 'sangria' ? 'bg-amber-500/10 text-amber-400' : 'bg-red-500/10 text-red-400'}`}>{op.tipo}</span>
                                      <span className="text-gray-400">{new Date(op.dataHora || op.timestamp).toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'})}</span>
                                      {op.nomeOperador && <span className="text-gray-600">• {op.nomeOperador}</span>}
                                    </div>
                                    <span className={op.tipo === 'abertura' || op.tipo === 'suprimento' ? 'text-emerald-400 font-medium' : 'text-red-400 font-medium'}>
                                      {op.tipo === 'abertura' || op.tipo === 'suprimento' ? '+' : '-'}R$ {(op.valor || 0).toFixed(2)}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Produtos vendidos */}
                          {topProdutos.length > 0 && (
                            <div>
                              <p className="text-xs font-semibold text-gray-400 mb-2">Produtos Vendidos ({topProdutos.length})</p>
                              <div className="space-y-1 max-h-40 overflow-y-auto">
                                {topProdutos.map(([nome, data]) => (
                                  <div key={nome} className="flex items-center justify-between text-xs py-1.5 px-2 rounded-lg bg-white/[0.02]">
                                    <div className="flex items-center gap-2 flex-1 min-w-0">
                                      <span className="text-white truncate">{nome}</span>
                                      <span className="text-gray-600 shrink-0">{data.qtd}x</span>
                                    </div>
                                    <span className="text-emerald-400 font-medium shrink-0 ml-2">R$ {data.total.toFixed(2)}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Vendas */}
                          {vDev.length > 0 && (
                            <div>
                              <p className="text-xs font-semibold text-gray-400 mb-2">Vendas ({vDev.length})</p>
                              <div className="space-y-1 max-h-40 overflow-y-auto">
                                {vDev.map(v => (
                                  <div key={v.id} className="flex items-center justify-between text-xs py-1.5 px-2 rounded-lg bg-white/[0.02]">
                                    <div className="flex items-center gap-2">
                                      <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${v.formaPagamento === 'DINHEIRO' ? 'bg-emerald-500/10 text-emerald-400' : v.formaPagamento === 'PIX' ? 'bg-blue-500/10 text-blue-400' : v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO' ? 'bg-amber-500/10 text-amber-400' : 'bg-cyan-500/10 text-cyan-400'}`}>{v.formaPagamento === 'CARTAO_CREDITO' ? 'Crédito' : v.formaPagamento === 'CARTAO_DEBITO' ? 'Débito' : v.formaPagamento}</span>
                                      <span className="text-gray-400">{new Date(v.createdAt || v.dataHora).toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'})}</span>
                                    </div>
                                    <span className="text-emerald-400 font-medium">R$ {(v.total || 0).toFixed(2)}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Resumo do caixa */}
                          <div className="p-3 rounded-lg bg-white/[0.03] border border-white/5">
                            <div className="flex justify-between text-xs mb-1">
                              <span className="text-gray-400">Abertura + Suprimento</span>
                              <span className="text-emerald-400">R$ {(aberturaDev + suprimentoDev).toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between text-xs mb-1">
                              <span className="text-gray-400">Sangria + Fechamento</span>
                              <span className="text-red-400">R$ {(sangriaDev + fechamentoDev).toFixed(2)}</span>
                            </div>
                            <div className="flex justify-between text-xs mb-1">
                              <span className="text-gray-400">Total Vendas</span>
                              <span className="text-blue-400">R$ {totalVendasDev.toFixed(2)}</span>
                            </div>
                            <div className="border-t border-white/10 pt-2 mt-2 flex justify-between">
                              <span className="text-white font-bold text-sm">Saldo</span>
                              <span className={`font-bold text-sm ${saldoDev >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>R$ {saldoDev.toFixed(2)}</span>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal de confirmação */}
      {showConfirm && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="glass p-6 w-full max-w-md border border-red-500/30 shadow-2xl shadow-red-500/10">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center">
                <Shield size={24} className="text-red-400" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Confirmar Fechamento</h3>
                <p className="text-sm text-gray-400">Esta ação não pode ser desfeita</p>
              </div>
            </div>
            <div className="glass p-4 rounded-lg mb-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Saldo do Caixa</span>
                <span className={`font-bold ${totais.saldo >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>R$ {totais.saldo.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Total Vendas</span>
                <span className="text-emerald-400 font-bold">R$ {totais.vendas.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Terminais</span>
                <span className="text-white font-medium">{dispositivos.length}</span>
              </div>
            </div>
            <p className="text-xs text-gray-500 mb-4">O PDF do fechamento será gerado automaticamente e os terminais serão notificados.</p>
            <div className="flex gap-3">
              <button onClick={() => setShowConfirm(false)} className="flex-1 px-4 py-2.5 bg-white/5 hover:bg-white/10 text-white rounded-xl text-sm font-medium transition-colors">
                Cancelar
              </button>
              <button onClick={handleFechamentoGeral} disabled={confirming} className="flex-1 px-4 py-2.5 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-500 hover:to-red-400 text-white rounded-xl text-sm font-semibold transition-all disabled:opacity-50 flex items-center justify-center gap-2">
                {confirming ? <><Loader2 size={16} className="animate-spin" /> Processando...</> : <><CheckCircle2 size={16} /> Confirmar</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
