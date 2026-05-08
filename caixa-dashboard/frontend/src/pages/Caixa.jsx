import { useState, useEffect, useMemo } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { apiUrl } from '../utils/api'
import { DollarSign, Search, Loader2, TrendingUp, Monitor, ChevronDown, ChevronUp, Plus, ArrowUpCircle, ArrowDownCircle, Lock, LockOpen, Wallet, CreditCard, Smartphone, PiggyBank } from 'lucide-react'

export default function Caixa() {
  const { token } = useAuth()
  const { socket } = useSocket()
  const [operacoes, setOperacoes] = useState([])
  const [vendas, setVendas] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [expandedDevice, setExpandedDevice] = useState(null)
  const [expandedVenda, setExpandedVenda] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [showFechamentoModal, setShowFechamentoModal] = useState(false)
  const [dispositivosConectados, setDispositivosConectados] = useState([])
  const [form, setForm] = useState({ tipo: 'abertura', valor: '', deviceId: '' })
  const [selectedTab, setSelectedTab] = useState(0)
  const [showHistorico, setShowHistorico] = useState(false)
  const [showFaturamento, setShowFaturamento] = useState(false)
  const [sessoes, setSessoes] = useState([])
  const [faturamento, setFaturamento] = useState([])
  const [periodoFaturamento, setPeriodoFaturamento] = useState('diario')

  useEffect(() => {
    fetch(apiUrl('/api/operacoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setOperacoes(data); setLoading(false) })
      .catch(() => setLoading(false))

    fetch(apiUrl('/api/vendas'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setVendas(data))
      .catch(() => setVendas([]))

    fetch(apiUrl('/api/dispositivos'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setDispositivosConectados(data))
      .catch(() => setDispositivosConectados([]))

    fetch(apiUrl('/api/caixa-sessoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setSessoes(data))
      .catch(() => setSessoes([]))
  }, [])

  useEffect(() => {
    if (!showFaturamento) return
    fetch(apiUrl(`/api/faturamento?periodo=${periodoFaturamento}`), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setFaturamento(data))
      .catch(() => setFaturamento([]))
  }, [showFaturamento, periodoFaturamento, token])

  // Escutar atualizações via WebSocket (único effect consolidado)
  useEffect(() => {
    if (!socket) return

    const handleOperacaoAdicionada = (operacao) => {
      setOperacoes(prev => [...prev, operacao])
    }

    const handleOperacoesSync = (data) => {
      if (data.operacoes) {
        setOperacoes(data.operacoes)
      } else if (Array.isArray(data)) {
        setOperacoes(data)
      }
    }

    const handleVendaAdded = (venda) => {
      setVendas(prev => [...prev, venda])
    }

    const handleVendasSync = (data) => {
      if (Array.isArray(data)) {
        setVendas(data)
      }
    }

    socket.on('operacao_adicionada', handleOperacaoAdicionada)
    socket.on('operacoes_sync', handleOperacoesSync)
    socket.on('venda_added', handleVendaAdded)
    socket.on('vendas_sync', handleVendasSync)

    return () => {
      socket.off('operacao_adicionada', handleOperacaoAdicionada)
      socket.off('operacoes_sync', handleOperacoesSync)
      socket.off('venda_added', handleVendaAdded)
      socket.off('vendas_sync', handleVendasSync)
    }
  }, [socket])

  
  // ==================== LÓGICA DE CAIXA POR TERMINAL ====================
  // Cada terminal tem sua própria sessão de caixa (abertura → fechamento).
  // O dashboard mostra o consolidado (soma de todos os terminais).
  // Dispositivos de teste são filtrados. Só dispositivos online aparecem.

  // DeviceIds de teste para ignorar
  const TEST_DEVICE_IDS = ['test-check', 'test-local', 'test-render', 'deploy-check']

  // Agrupar operações e vendas por dispositivo, com sessão atual
  const { dispositivos, caixaAtual } = useMemo(() => {
    const ensureDevice = (acc, deviceId) => {
      if (!acc[deviceId]) {
        acc[deviceId] = {
          deviceId,
          operacoes: [],
          deviceName: dispositivosConectados.find(d => d.deviceId === deviceId)?.deviceName || null
        }
      }
    }

    // 1. Agrupar operações por deviceId (ignorar testes)
    const porDispositivo = operacoes.reduce((acc, operacao) => {
      const deviceId = operacao.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return acc
      ensureDevice(acc, deviceId)
      acc[deviceId].operacoes.push(operacao)
      return acc
    }, {})

    // 2. Incluir dispositivos que têm vendas mas não têm operações (ignorar testes)
    vendas.forEach(v => {
      const deviceId = v.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return
      ensureDevice(porDispositivo, deviceId)
    })

    // 3. Incluir dispositivos conectados que estão online (ignorar testes e offline)
    dispositivosConectados.forEach(d => {
      if (d.deviceId && !TEST_DEVICE_IDS.includes(d.deviceId) && d.status !== 'offline') {
        ensureDevice(porDispositivo, d.deviceId)
      }
    })

    // 4. Determinar sessão de cada dispositivo
    // Aberto: desde a última abertura até agora
    // Fechado: da última abertura até o fechamento correspondente
    const sessoes = {}
    Object.entries(porDispositivo).forEach(([deviceId, dev]) => {
      const opsSorted = [...dev.operacoes].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))
      const aberturas = opsSorted.filter(o => o.tipo === 'abertura')
      const ultimaAbertura = aberturas[aberturas.length - 1] || null

      if (!ultimaAbertura) {
        sessoes[deviceId] = { aberto: false, aberturaEm: null, aberturaTimestamp: null, fechamentoTimestamp: null, operador: null }
        return
      }

      // Verificar se há fechamento APÓS a última abertura
      const fechamentosApos = opsSorted.filter(o => o.tipo === 'fechamento' && o.timestamp > ultimaAbertura.timestamp)
      const aberto = fechamentosApos.length === 0
      const ultimoFechamento = fechamentosApos.length > 0 ? fechamentosApos[fechamentosApos.length - 1] : null

      sessoes[deviceId] = {
        aberto,
        aberturaEm: ultimaAbertura.dataHora,
        aberturaTimestamp: ultimaAbertura.timestamp,
        fechamentoTimestamp: ultimoFechamento ? ultimoFechamento.timestamp : null,
        fechamentoEm: ultimoFechamento ? ultimoFechamento.dataHora : null,
        operador: ultimaAbertura.nomeOperador
      }
    })

    return { dispositivos: Object.values(porDispositivo), caixaAtual: sessoes }
  }, [operacoes, vendas, dispositivosConectados])

  // Totais consolidados (soma de todos os terminais com sessão — aberta ou fechada)
  const { totalAbertura, totalFechamento, saldoGeral, totalSuprimento, totalSangria, totalVendas, totalDinheiro, totalPix, totalCredito, totalDebito } = useMemo(() => {
    let ab = 0, ft = 0, sup = 0, san = 0, tv = 0, din = 0, pix = 0, cred = 0, deb = 0
    
    dispositivos.forEach(d => {
      const sessao = caixaAtual[d.deviceId]
      if (!sessao || !sessao.aberturaTimestamp) return
      
      const ts = sessao.aberturaTimestamp
      const tf = sessao.aberto ? Date.now() : (sessao.fechamentoTimestamp || Date.now())
      const opsSessao = d.operacoes.filter(o => o.timestamp >= ts && o.timestamp <= tf)
      
      ab += opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
      sup += opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
      san += opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)
      ft += opsSessao.filter(o => o.tipo === 'fechamento').reduce((s, o) => s + (o.valor || 0), 0)
      
      const vendasSessao = vendas.filter(v => {
        const vTime = new Date(v.createdAt || v.dataHora).getTime()
        return vTime >= ts && vTime <= tf && (v.deviceId === d.deviceId || (d.deviceId === 'geral' && !v.deviceId))
      })
      tv += vendasSessao.reduce((s, v) => s + (v.total || 0), 0)
      din += vendasSessao.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0)
      pix += vendasSessao.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0)
      cred += vendasSessao.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0)
      deb += vendasSessao.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
    })
    
    return { totalAbertura: ab, totalFechamento: ft, saldoGeral: ab + sup - san - ft, totalSuprimento: sup, totalSangria: san, totalVendas: tv, totalDinheiro: din, totalPix: pix, totalCredito: cred, totalDebito: deb }
  }, [dispositivos, vendas, caixaAtual])

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

  // Vendas da sessão de um dispositivo (aberta ou fechada)
  const getVendasSessao = (deviceId) => {
    const sessao = caixaAtual[deviceId]
    if (!sessao || !sessao.aberturaTimestamp) return []
    const ts = sessao.aberturaTimestamp
    const tf = sessao.aberto ? Date.now() : (sessao.fechamentoTimestamp || Date.now())
    return vendas.filter(v => {
      const vTime = new Date(v.createdAt || v.dataHora).getTime()
      return vTime >= ts && vTime <= tf && (v.deviceId === deviceId || (deviceId === 'geral' && !v.deviceId))
    })
  }

  // Operações da sessão de um dispositivo (aberta ou fechada)
  const getOpsSessao = (deviceId) => {
    const sessao = caixaAtual[deviceId]
    if (!sessao || !sessao.aberturaTimestamp) return []
    const ts = sessao.aberturaTimestamp
    const tf = sessao.aberto ? Date.now() : (sessao.fechamentoTimestamp || Date.now())
    const dev = dispositivos.find(d => d.deviceId === deviceId)
    if (!dev) return []
    return dev.operacoes.filter(o => o.timestamp >= ts && o.timestamp <= tf)
  }

  // Filtrar operações/vendas por aba selecionada (por dispositivo, sessão atual)
  const getOperacoesByTab = (dispositivo, tab) => {
    const deviceId = dispositivo.deviceId
    const opsSessao = getOpsSessao(deviceId)
    const vendasSessao = getVendasSessao(deviceId)
    
    switch(tab) {
      case 0: return opsSessao.filter(o => o.tipo === 'abertura')
      case 1: return opsSessao.filter(o => o.tipo === 'fechamento')
      case 2: return opsSessao.filter(o => o.tipo === 'suprimento')
      case 3: return opsSessao.filter(o => o.tipo === 'sangria')
      case 4: return vendasSessao.filter(v => v.formaPagamento === 'DINHEIRO')
      case 5: return vendasSessao.filter(v => v.formaPagamento === 'PIX')
      case 6: return vendasSessao.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO')
      case 7: return vendasSessao.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO')
      default: return opsSessao
    }
  }

  // Obter informações do card baseadas na aba selecionada
  const getCardInfo = (dispositivo, tab) => {
    const tabItems = getOperacoesByTab(dispositivo, tab)
    const total = tabItems.reduce((sum, item) => sum + (item.valor || item.total || 0), 0)
    const lastItem = tabItems.length > 0 ? tabItems[tabItems.length - 1] : null
    
    const tabLabels = {
      0: { label: 'Total Abertura', tipo: 'abertura' },
      1: { label: 'Total Fechamento', tipo: 'fechamento' },
      2: { label: 'Total Suprimento', tipo: 'suprimento' },
      3: { label: 'Total Sangria', tipo: 'sangria' },
      4: { label: 'Total Dinheiro', tipo: 'DINHEIRO' },
      5: { label: 'Total PIX', tipo: 'PIX' },
      6: { label: 'Total Crédito', tipo: 'CREDITO' },
      7: { label: 'Total Débito', tipo: 'DEBITO' }
    }
    
    return {
      total,
      label: tabLabels[tab]?.label || 'Total',
      lastItem,
      isVenda: tab >= 4
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const body = {
      tipo: form.tipo,
      valor: parseFloat(form.valor),
      deviceId: form.deviceId || null,
      observacao: form.observacao || ''
    }

    await fetch(apiUrl('/api/operacoes'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body)
    })
    setShowModal(false)
    setForm({ tipo: 'abertura', valor: '', deviceId: '' })
    fetch(apiUrl('/api/operacoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setOperacoes(data))
  }

  const handleFechamentoGeral = async () => {
    // Verificar se há caixas abertos antes de permitir fechamento geral
    const dispositivosComCaixaAberto = dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto)
    
    if (dispositivosComCaixaAberto.length > 0) {
      alert(`⚠️ ATENÇÃO: Existem ${dispositivosComCaixaAberto.length} caixas abertos:\n\n${dispositivosComCaixaAberto.map(d => `- ${d.deviceId === 'geral' ? 'Geral' : d.deviceId}`).join('\n')}\n\nFaça o fechamento individual dos caixas antes do fechamento geral.`)
      return
    }
    
    const totalVendas = vendas.reduce((sum, v) => sum + (v.total || 0), 0)
    const totalAbertura = operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0)
    const totalSuprimento = operacoes.filter(o => o.tipo === 'suprimento').reduce((sum, o) => sum + (o.valor || 0), 0)
    const totalSangria = operacoes.filter(o => o.tipo === 'sangria').reduce((sum, o) => sum + (o.valor || 0), 0)
    const totalFechamento = operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0)
    
    const saldoFinal = totalAbertura + totalSuprimento - totalSangria - totalFechamento
    
    console.log('Cálculo Fechamento Geral:', {
      totalAbertura,
      totalSuprimento,
      totalSangria,
      totalFechamento,
      saldoFinal,
      totalVendas
    })
    
    // 1. Criar operação de fechamento geral
    const body = {
      tipo: 'fechamento',
      valor: saldoFinal,
      deviceId: null,
      observacao: `Fechamento geral - Vendas: R$ ${totalVendas.toFixed(2)}`
    }

    const response = await fetch(apiUrl('/api/operacoes'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body)
    })

    if (response.ok) {
      // 2. Gerar PDF do fechamento
      await gerarPDFFechamento({
        totalAbertura,
        totalSuprimento,
        totalSangria,
        totalFechamento: saldoFinal,
        totalVendas,
        dataHora: new Date().toLocaleString('pt-BR'),
        operacoes: operacoes.filter(o => o.tipo !== 'fechamento'),
        vendas: vendas
      })

      // 3. Enviar comando de fechamento para dispositivos Android
      if (socket) {
        socket.emit('fechamento_geral', {
          valor: saldoFinal,
          totalVendas,
          dataHora: new Date().toISOString(),
          observacao: `Fechamento geral - Vendas: R$ ${totalVendas.toFixed(2)}`
        })
      }
    }

    setShowFechamentoModal(false)
    fetch(apiUrl('/api/operacoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setOperacoes(data))
  }

  // Função para gerar PDF do fechamento
  const gerarPDFFechamento = async (dados) => {
    try {
      const response = await fetch(apiUrl('/api/fechamento-pdf', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json', 
          Authorization: `Bearer ${token}` 
        },
        body: JSON.stringify(dados)
      }))

      if (response.ok) {
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `fechamento-geral-${new Date().toISOString().split('T')[0]}.pdf`
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
      }
    } catch (error) {
      console.error('Erro ao gerar PDF:', error)
    }
  }

  return (
    <div className="space-y-6">
      {/* Stats Cards - Operações de Caixa */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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
            <PiggyBank size={24} className="text-blue-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalSuprimento.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Suprimento</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
            <Wallet size={24} className="text-amber-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalSangria.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Sangria</p>
          </div>
        </div>
      </div>

      {/* Stats Cards - Vendas por Forma de Pagamento */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
            <DollarSign size={24} className="text-emerald-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalVendas.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Vendas</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
            <DollarSign size={24} className="text-emerald-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalDinheiro.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Dinheiro</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
            <Smartphone size={24} className="text-blue-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalPix.toFixed(2)}</p>
            <p className="text-xs text-gray-400">PIX</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
            <CreditCard size={24} className="text-amber-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalCredito.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Crédito</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-cyan-500/20 flex items-center justify-center">
            <CreditCard size={24} className="text-cyan-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalDebito.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Débito</p>
          </div>
        </div>
      </div>

      {/* Abas de Operações e Vendas */}
      <div className="glass p-1 rounded-xl">
        <div className="flex flex-wrap gap-1">
          {[
            { id: 0, label: 'Aberturas', icon: ArrowUpCircle, activeClass: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/20' },
            { id: 1, label: 'Fechamentos', icon: ArrowDownCircle, activeClass: 'bg-red-500/20 text-red-400 border border-red-500/20' },
            { id: 2, label: 'Suprimentos', icon: PiggyBank, activeClass: 'bg-blue-500/20 text-blue-400 border border-blue-500/20' },
            { id: 3, label: 'Sangrias', icon: Wallet, activeClass: 'bg-amber-500/20 text-amber-400 border border-amber-500/20' },
            { id: 4, label: 'Dinheiro', icon: DollarSign, activeClass: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/20' },
            { id: 5, label: 'PIX', icon: Smartphone, activeClass: 'bg-blue-500/20 text-blue-400 border border-blue-500/20' },
            { id: 6, label: 'Crédito', icon: CreditCard, activeClass: 'bg-amber-500/20 text-amber-400 border border-amber-500/20' },
            { id: 7, label: 'Débito', icon: CreditCard, activeClass: 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/20' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setSelectedTab(tab.id)}
              className={`flex-1 min-w-[100px] flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                selectedTab === tab.id
                  ? tab.activeClass
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
              }`}
            >
              <tab.icon size={16} />
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div className="relative w-full sm:max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="input-field pl-9 py-2.5 text-sm" placeholder="Buscar dispositivo..." />
        </div>
        <div className="flex gap-2">
          <button onClick={() => { setShowFaturamento(!showFaturamento); setShowHistorico(false) }} className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${showFaturamento ? 'bg-blue-600 text-white' : 'bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/20'}`}>
            <TrendingUp size={16} /> Faturamento
          </button>
          <button onClick={() => { setShowHistorico(!showHistorico); setShowFaturamento(false) }} className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${showHistorico ? 'bg-amber-600 text-white' : 'bg-amber-600/20 hover:bg-amber-600/30 text-amber-400 border border-amber-500/20'}`}>
            <Wallet size={16} /> Histórico
          </button>
          <button onClick={() => setShowFechamentoModal(true)} className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors">
            <Lock size={16} /> Fechamento Geral
          </button>
          <button onClick={() => setShowModal(true)} className="btn-primary flex items-center gap-2 text-sm">
            <Plus size={16} /> Nova Operação
          </button>
        </div>
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
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${caixaAtual[dispositivo.deviceId]?.aberto ? 'bg-gradient-to-br from-emerald-600 to-emerald-400' : 'bg-gradient-to-br from-gray-600 to-gray-500'}`}>
                    <Monitor size={24} className="text-white" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-white text-sm">{dispositivo.deviceId === 'geral' ? 'Geral' : dispositivosConectados.find(d => d.deviceId === dispositivo.deviceId)?.deviceName || dispositivo.deviceName || dispositivo.deviceId}</p>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${caixaAtual[dispositivo.deviceId]?.aberto ? 'bg-emerald-500/20 text-emerald-400' : 'bg-gray-500/20 text-gray-400'}`}>
                        {caixaAtual[dispositivo.deviceId]?.aberto ? 'Aberto' : 'Fechado'}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500">
                      {getOperacoesByTab(dispositivo, selectedTab).length} {selectedTab >= 4 ? 'vendas' : 'operações'}
                      {caixaAtual[dispositivo.deviceId]?.aberturaEm && ` • Desde ${new Date(caixaAtual[dispositivo.deviceId].aberturaEm).toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'})}`}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-right">
                    <p className={`text-lg font-bold ${getCardInfo(dispositivo, selectedTab).total >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                      R$ {getCardInfo(dispositivo, selectedTab).total.toFixed(2)}
                    </p>
                    <p className="text-xs text-gray-500">{getCardInfo(dispositivo, selectedTab).label}</p>
                  </div>
                  {expandedDevice === dispositivo.deviceId ? <ChevronUp size={20} className="text-gray-400" /> : <ChevronDown size={20} className="text-gray-400" />}
                </div>
              </div>

              {/* Status do Caixa / Informações da Aba */}
              <div className="p-4 border-t border-white/5">
                {(() => {
                  const cardInfo = getCardInfo(dispositivo, selectedTab)
                  
                  return (
                    <>
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-2">
                          {cardInfo.isVenda ? (
                            <div className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center">
                              <Smartphone size={16} className="text-blue-400" />
                            </div>
                          ) : caixaAtual[dispositivo.deviceId]?.aberto ? (
                            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center">
                              <LockOpen size={16} className="text-emerald-400" />
                            </div>
                          ) : (
                            <div className="w-8 h-8 rounded-lg bg-red-500/20 flex items-center justify-center">
                              <Lock size={16} className="text-red-400" />
                            </div>
                          )}
                          <span className={`text-sm font-medium ${cardInfo.isVenda ? 'text-blue-400' : (caixaAtual[dispositivo.deviceId]?.aberto ? 'text-emerald-400' : 'text-red-400')}`}>
                            {cardInfo.isVenda ? cardInfo.label : `Caixa ${caixaAtual[dispositivo.deviceId]?.aberto ? 'Aberto' : 'Fechado'}`}
                          </span>
                        </div>
                        <div className="text-right">
                          <p className={`text-lg font-bold ${cardInfo.total >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                            R$ {cardInfo.total.toFixed(2)}
                          </p>
                          <p className="text-xs text-gray-500">{cardInfo.label}</p>
                        </div>
                      </div>
                      
                      {/* Última Operação/Venda */}
                      {cardInfo.lastItem && (
                        <div className="glass p-3 mb-3">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="text-xs text-gray-400">
                                {cardInfo.isVenda ? 'Última Venda' : 'Última Operação'}
                              </p>
                              <p className="text-sm font-medium text-white">
                                {new Date(cardInfo.lastItem.dataHora || cardInfo.lastItem.createdAt).toLocaleString('pt-BR')}
                              </p>
                              {cardInfo.lastItem.nomeOperador && (
                                <p className="text-xs text-gray-500">
                                  Operador: {cardInfo.lastItem.nomeOperador}
                                </p>
                              )}
                            </div>
                            <div className="text-right">
                              <p className="text-sm font-bold text-emerald-400">
                                R$ {(cardInfo.lastItem.valor || cardInfo.lastItem.total || 0).toFixed(2)}
                              </p>
                            </div>
                          </div>
                        </div>
                      )}
                    </>
                  )
                })()}
              </div>

              {/* Lista de operações do dispositivo (filtradas por aba) */}
              {expandedDevice === dispositivo.deviceId && (
                <div className="border-t border-white/5 p-4 space-y-2">
                  {getOperacoesByTab(dispositivo, selectedTab).length === 0 ? (
                    <div className="text-center py-6">
                      <div className="w-10 h-10 rounded-full bg-gray-500/10 flex items-center justify-center mx-auto mb-2">
                        {selectedTab >= 4 ? <DollarSign size={18} className="text-gray-500" /> : <Wallet size={18} className="text-gray-500" />}
                      </div>
                      <p className="text-gray-400 text-sm">
                        {selectedTab >= 4 
                          ? `Nenhuma venda via ${['','Dinheiro','PIX','Crédito','Débito'][selectedTab-3]} nesta sessão`
                          : `Nenhuma operação de ${['abertura','fechamento','suprimento','sangria'][selectedTab]} nesta sessão`
                        }
                      </p>
                      {!caixaAtual[dispositivo.deviceId]?.aberturaTimestamp && (
                        <p className="text-gray-600 text-xs mt-1">Abra o caixa para registrar operações</p>
                      )}
                    </div>
                  ) : (
                    getOperacoesByTab(dispositivo, selectedTab).map(item => {
                      const isVenda = item.formaPagamento !== undefined
                      
                      return (
                        <div key={item.id} className="glass">
                          <div 
                            className="p-3 flex items-center gap-3 cursor-pointer hover:bg-white/5"
                            onClick={() => isVenda && setExpandedVenda(expandedVenda === item.id ? null : item.id)}
                          >
                            <div className={`w-8 h-8 rounded-lg ${
                              isVenda 
                                ? (item.formaPagamento === 'DINHEIRO' ? 'bg-emerald-500/10 text-emerald-400' :
                                   item.formaPagamento === 'PIX' ? 'bg-blue-500/10 text-blue-400' :
                                   item.formaPagamento === 'CREDITO' || item.formaPagamento === 'CARTAO_CREDITO' ? 'bg-amber-500/10 text-amber-400' :
                                   item.formaPagamento === 'DEBITO' || item.formaPagamento === 'CARTAO_DEBITO' ? 'bg-cyan-500/10 text-cyan-400' :
                                   'bg-gray-500/10 text-gray-400')
                                : (tipoColors[item.tipo] || 'bg-gray-500/10 text-gray-400')
                            } flex items-center justify-center shrink-0`}>
                              {isVenda ? <DollarSign size={16} /> :
                               item.tipo === 'abertura' ? <ArrowUpCircle size={16} /> : 
                               item.tipo === 'fechamento' ? <ArrowDownCircle size={16} /> :
                               item.tipo === 'suprimento' ? <PiggyBank size={16} /> :
                               <Wallet size={16} />}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 mb-1">
                                <p className="text-sm font-medium text-white">
                                  {isVenda ? `Venda #${item.id?.toString().slice(-6)}` : item.tipo}
                                </p>
                                {isVenda && item.formaPagamento && (
                                  <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${
                                    item.formaPagamento === 'DINHEIRO' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                                    item.formaPagamento === 'PIX' ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' :
                                    item.formaPagamento === 'CREDITO' || item.formaPagamento === 'CARTAO_CREDITO' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                                    item.formaPagamento === 'DEBITO' || item.formaPagamento === 'CARTAO_DEBITO' ? 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20' :
                                    'bg-gray-500/10 text-gray-400 border-gray-500/20'
                                  }`}>
                                    {item.formaPagamento === 'CARTAO_CREDITO' ? 'Crédito' : item.formaPagamento === 'CARTAO_DEBITO' ? 'Débito' : item.formaPagamento}
                                  </span>
                                )}
                                {!isVenda && (
                                  <span className={`px-2 py-0.5 rounded-lg text-xs font-medium border ${tipoColors[item.tipo] || 'bg-gray-500/10 text-gray-400 border-gray-500/20'}`}>
                                    {item.tipo}
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-gray-500">
                                {new Date(item.dataHora || item.createdAt).toLocaleString('pt-BR')}
                                {item.nomeOperador && ` • ${item.nomeOperador}`}
                                {item.observacao && ` • ${item.observacao}`}
                              </p>
                            </div>
                            <p className={`text-sm font-bold shrink-0 ${isVenda ? 'text-emerald-400' : (item.tipo === 'abertura' || item.tipo === 'suprimento' ? 'text-emerald-400' : 'text-red-400')}`}>
                              {isVenda ? '' : (item.tipo === 'abertura' || item.tipo === 'suprimento' ? '+' : '-')}R$ {(item.valor || item.total || 0).toFixed(2)}
                            </p>
                            {isVenda && (
                              expandedVenda === item.id ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />
                            )}
                          </div>
                          
                          {/* Detalhes dos produtos da venda */}
                          {isVenda && expandedVenda === item.id && item.itens && item.itens.length > 0 && (
                            <div className="border-t border-white/5 p-3 space-y-2 bg-black/20">
                              <p className="text-xs font-medium text-gray-400 mb-2">Produtos:</p>
                              {item.itens.map((produto, idx) => (
                                <div key={idx} className="flex items-center justify-between text-sm">
                                  <div className="flex-1">
                                    <p className="text-white">{produto.produtoNome}</p>
                                    <p className="text-xs text-gray-500">{produto.quantidade}x R$ {produto.precoUnitario?.toFixed(2)}</p>
                                  </div>
                                  <p className="text-emerald-400 font-medium">R$ {produto.total?.toFixed(2)}</p>
                                </div>
                              ))}
                              <div className="border-t border-white/10 pt-2 mt-2 flex justify-between">
                                <p className="text-sm font-medium text-white">Total</p>
                                <p className="text-sm font-bold text-emerald-400">R$ {(item.total || 0).toFixed(2)}</p>
                              </div>
                            </div>
                          )}
                        </div>
                      )
                    })
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Histórico de Caixas Fechados */}
      {showHistorico && (
        <div className="glass p-4 rounded-xl">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Wallet size={20} className="text-amber-400" /> Histórico de Caixas
          </h3>
          {sessoes.length === 0 ? (
            <p className="text-gray-500 text-center py-8">Nenhum caixa fechado registrado</p>
          ) : (
            <div className="space-y-3">
              {[...sessoes].reverse().map(s => (
                <div key={s.id} className="glass p-4">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <p className="text-sm font-medium text-white">
                        {s.deviceId === 'geral' ? 'Geral' : dispositivosConectados.find(d => d.deviceId === s.deviceId)?.deviceName || s.deviceId}
                      </p>
                      <p className="text-xs text-gray-500">
                        {new Date(s.aberturaEm).toLocaleString('pt-BR')} → {new Date(s.fechamentoEm).toLocaleString('pt-BR')}
                      </p>
                      <p className="text-xs text-gray-500">
                        Operadores: {s.operadorAbertura} / {s.operadorFechamento}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold text-emerald-400">R$ {s.totalVendas.toFixed(2)}</p>
                      <p className="text-xs text-gray-500">{s.qtdVendas} vendas</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-4 gap-2 text-xs mt-2 pt-2 border-t border-white/5">
                    <div><span className="text-gray-500">Abertura</span><br/><span className="text-emerald-400">R$ {s.totalAbertura.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">Suprimento</span><br/><span className="text-blue-400">R$ {s.totalSuprimento.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">Sangria</span><br/><span className="text-amber-400">R$ {s.totalSangria.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">Fechamento</span><br/><span className="text-red-400">R$ {s.totalFechamento.toFixed(2)}</span></div>
                  </div>
                  <div className="grid grid-cols-4 gap-2 text-xs mt-2">
                    <div><span className="text-gray-500">Dinheiro</span><br/><span className="text-emerald-400">R$ {s.vendasDinheiro.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">PIX</span><br/><span className="text-blue-400">R$ {s.vendasPix.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">Crédito</span><br/><span className="text-amber-400">R$ {s.vendasCredito.toFixed(2)}</span></div>
                    <div><span className="text-gray-500">Débito</span><br/><span className="text-cyan-400">R$ {s.vendasDebito.toFixed(2)}</span></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Faturamento */}
      {showFaturamento && (
        <div className="glass p-4 rounded-xl">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <TrendingUp size={20} className="text-blue-400" /> Faturamento
            </h3>
            <div className="flex gap-1">
              {['diario', 'semanal', 'mensal', 'anual'].map(p => (
                <button key={p} onClick={() => setPeriodoFaturamento(p)}
                  className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${periodoFaturamento === p ? 'bg-blue-500/20 text-blue-400 border border-blue-500/20' : 'text-gray-400 hover:text-white'}`}>
                  {p === 'diario' ? 'Diário' : p === 'semanal' ? 'Semanal' : p === 'mensal' ? 'Mensal' : 'Anual'}
                </button>
              ))}
            </div>
          </div>
          {faturamento.length === 0 ? (
            <p className="text-gray-500 text-center py-8">Nenhum dado de faturamento</p>
          ) : (
            <div className="space-y-2">
              {/* Bar chart simples */}
              <div className="space-y-1.5">
                {faturamento.filter(f => f.totalVendas > 0).slice(0, 15).map((f, i) => {
                  const maxVal = Math.max(...faturamento.map(x => x.totalVendas), 1)
                  const pct = (f.totalVendas / maxVal) * 100
                  return (
                    <div key={i} className="flex items-center gap-3">
                      <span className="text-xs text-gray-400 w-16 text-right shrink-0">{f.label}</span>
                      <div className="flex-1 h-6 bg-gray-800 rounded overflow-hidden">
                        <div className="h-full bg-gradient-to-r from-blue-600 to-cyan-500 rounded flex items-center px-2 transition-all" style={{ width: `${Math.max(pct, 2)}%` }}>
                          <span className="text-xs font-medium text-white whitespace-nowrap">R$ {f.totalVendas.toFixed(2)}</span>
                        </div>
                      </div>
                      <span className="text-xs text-gray-500 w-10 shrink-0">{f.qtdVendas}v</span>
                    </div>
                  )
                })}
              </div>
              {/* Totais do período */}
              <div className="glass p-3 mt-4">
                <p className="text-xs text-gray-400 mb-2">Resumo do período</p>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex justify-between"><span className="text-gray-500">Total Geral:</span><span className="text-emerald-400 font-bold">R$ {faturamento.reduce((s, f) => s + f.totalVendas, 0).toFixed(2)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Total Vendas:</span><span className="text-white">{faturamento.reduce((s, f) => s + f.qtdVendas, 0)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Dinheiro:</span><span className="text-emerald-400">R$ {faturamento.reduce((s, f) => s + f.dinheiro, 0).toFixed(2)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">PIX:</span><span className="text-blue-400">R$ {faturamento.reduce((s, f) => s + f.pix, 0).toFixed(2)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Crédito:</span><span className="text-amber-400">R$ {faturamento.reduce((s, f) => s + f.credito, 0).toFixed(2)}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Débito:</span><span className="text-cyan-400">R$ {faturamento.reduce((s, f) => s + f.debito, 0).toFixed(2)}</span></div>
                </div>
              </div>
            </div>
          )}
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
                <select value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })} className="bg-gray-800 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent">
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
                <label className="block text-sm font-medium text-gray-300 mb-1">Dispositivo</label>
                <select value={form.deviceId} onChange={e => setForm({ ...form, deviceId: e.target.value })} className="bg-gray-800 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                  <option value="">Geral (todos os dispositivos)</option>
                  {dispositivosConectados.map(d => (
                    <option key={d.deviceId} value={d.deviceId}>{d.deviceName || d.deviceId}</option>
                  ))}
                </select>
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

      {/* Modal Fechamento Geral */}
      {showFechamentoModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setShowFechamentoModal(false)}>
          <div className="glass p-6 w-full max-w-lg glow-red max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-white">Fechamento Geral do Caixa</h3>
              <button onClick={() => setShowFechamentoModal(false)} className="text-gray-400 hover:text-white"><Plus size={20} className="rotate-45" /></button>
            </div>
            <div className="space-y-4">
              {/* Resumo de Operações de Caixa */}
              <div className="glass p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-300 mb-2">Operações de Caixa</h4>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Abertura</span>
                  <span className="text-emerald-400 font-medium">R$ {operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Suprimento</span>
                  <span className="text-emerald-400 font-medium">R$ {operacoes.filter(o => o.tipo === 'suprimento').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Sangria</span>
                  <span className="text-red-400 font-medium">R$ {operacoes.filter(o => o.tipo === 'sangria').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Fechamento Anterior</span>
                  <span className="text-red-400 font-medium">R$ {operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</span>
                </div>
              </div>

              {/* Resumo de Vendas por Forma de Pagamento */}
              <div className="glass p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-300 mb-2">Vendas por Forma de Pagamento</h4>
                <div className="flex justify-between">
                  <span className="text-gray-400">Dinheiro</span>
                  <span className="text-emerald-400 font-medium">R$ {totalDinheiro.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">PIX</span>
                  <span className="text-blue-400 font-medium">R$ {totalPix.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Crédito</span>
                  <span className="text-amber-400 font-medium">R$ {totalCredito.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Débito</span>
                  <span className="text-cyan-400 font-medium">R$ {totalDebito.toFixed(2)}</span>
                </div>
                <div className="border-t border-white/10 pt-3 flex justify-between">
                  <span className="text-white font-medium">Total Vendas</span>
                  <span className="text-emerald-400 font-bold">R$ {totalVendas.toFixed(2)}</span>
                </div>
              </div>

              {/* Detalhamento por Terminal */}
              <div className="glass p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-300 mb-2">Detalhamento por Terminal</h4>
                {dispositivos.map(d => {
                  const vendasDispositivo = vendas.filter(v => v.deviceId === d.deviceId)
                  const totalVendasDispositivo = vendasDispositivo.reduce((sum, v) => sum + (v.total || 0), 0)
                  const dinheiroDispositivo = vendasDispositivo.filter(v => v.formaPagamento === 'DINHEIRO').reduce((sum, v) => sum + (v.total || 0), 0)
                  const pixDispositivo = vendasDispositivo.filter(v => v.formaPagamento === 'PIX').reduce((sum, v) => sum + (v.total || 0), 0)
                  const creditoDispositivo = vendasDispositivo.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((sum, v) => sum + (v.total || 0), 0)
                  const debitoDispositivo = vendasDispositivo.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((sum, v) => sum + (v.total || 0), 0)
                  
                  return (
                    <div key={d.deviceId} className="border border-white/10 rounded-lg p-3 space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-sm font-medium text-white">{dispositivosConectados.find(dc => dc.deviceId === d.deviceId)?.deviceName || d.deviceId}</span>
                        <span className="text-emerald-400 font-bold">R$ {totalVendasDispositivo.toFixed(2)}</span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        <div className="flex justify-between">
                          <span className="text-gray-500">Dinheiro:</span>
                          <span className="text-emerald-400">R$ {dinheiroDispositivo.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-500">PIX:</span>
                          <span className="text-blue-400">R$ {pixDispositivo.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-500">Crédito:</span>
                          <span className="text-amber-400">R$ {creditoDispositivo.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-500">Débito:</span>
                          <span className="text-cyan-400">R$ {debitoDispositivo.toFixed(2)}</span>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>

              {/* Resumo Final */}
              <div className="glass p-4 space-y-3 border-2 border-red-500/30">
                <h4 className="text-sm font-semibold text-red-400 mb-2">Resumo Final</h4>
                <div className="flex justify-between">
                  <span className="text-gray-400">Entradas (Abertura + Suprimento)</span>
                  <span className="text-emerald-400 font-medium">R$ {(operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0) + operacoes.filter(o => o.tipo === 'suprimento').reduce((sum, o) => sum + (o.valor || 0), 0)).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Saídas (Sangria + Fechamento)</span>
                  <span className="text-red-400 font-medium">R$ {(operacoes.filter(o => o.tipo === 'sangria').reduce((sum, o) => sum + (o.valor || 0), 0) + operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0)).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Total Vendas</span>
                  <span className="text-blue-400 font-medium">R$ {totalVendas.toFixed(2)}</span>
                </div>
                <div className="border-t border-white/10 pt-3 flex justify-between">
                  <span className="text-white font-bold">Saldo Final do Caixa</span>
                  <span className="text-red-400 font-bold text-lg">R$ {(operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0) + operacoes.filter(o => o.tipo === 'suprimento').reduce((sum, o) => sum + (o.valor || 0), 0) - operacoes.filter(o => o.tipo === 'sangria').reduce((sum, o) => sum + (o.valor || 0), 0) - operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0)).toFixed(2)}</span>
                </div>
              </div>

              <p className="text-xs text-gray-500 text-center">Este fechamento será registrado como operação geral com o saldo calculado acima.</p>
              <div className="flex gap-3 pt-2">
                <button onClick={() => setShowFechamentoModal(false)} className="btn-ghost flex-1">Cancelar</button>
                <button onClick={handleFechamentoGeral} className="btn-primary flex-1">Confirmar Fechamento</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
