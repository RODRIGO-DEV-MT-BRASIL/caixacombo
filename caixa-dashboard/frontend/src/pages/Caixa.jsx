import { useState, useEffect, useMemo } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { apiUrl } from '../utils/api'
import { useToast } from '../components/Toast'
import { DollarSign, Search, Loader2, TrendingUp, Monitor, ChevronDown, ChevronUp, Plus, ArrowUpCircle, ArrowDownCircle, Lock, LockOpen, Wallet, CreditCard, Smartphone, PiggyBank, Printer, CheckCircle2, XCircle, AlertTriangle } from 'lucide-react'

export default function Caixa({ onNavigateToFechamento }) {
  const { user, token } = useAuth()
  const { socket } = useSocket()
  const [operacoes, setOperacoes] = useState([])
  const [vendas, setVendas] = useState([])
  const [empresas, setEmpresas] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filterEmpresa, setFilterEmpresa] = useState('')
  const [filterTerminal, setFilterTerminal] = useState('')
  const [expandedDevice, setExpandedDevice] = useState(null)
  const [expandedVenda, setExpandedVenda] = useState(null)
  const [showModal, setShowModal] = useState(false)

  const [dispositivosConectados, setDispositivosConectados] = useState([])
  const [form, setForm] = useState({ tipo: 'abertura', valor: '', deviceId: '' })
  const [selectedTab, setSelectedTab] = useState(0)
  const [showHistorico, setShowHistorico] = useState(false)
  const [showFaturamento, setShowFaturamento] = useState(false)
  const [sessoes, setSessoes] = useState([])
  const [faturamento, setFaturamento] = useState([])
  const [periodoFaturamento, setPeriodoFaturamento] = useState('diario')
  const [showFecharCaixaModal, setShowFecharCaixaModal] = useState(false)
  const [fechandoCaixa, setFechandoCaixa] = useState(false)
  const toast = useToast()

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

    fetch(apiUrl('/api/caixa-sessoes'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => setSessoes(Array.isArray(data) ? data : data.data || []))
      .catch(() => setSessoes([]))

    if (user?.role === 'admin' || user?.role === 'empresa') {
      fetch(apiUrl('/api/empresas'), { headers: { Authorization: `Bearer ${token}` } })
        .then(res => res.json())
        .then(data => setEmpresas(Array.isArray(data) ? data : []))
        .catch(() => {})
    }
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
    const porDispositivo = operacoesFiltradas.reduce((acc, operacao) => {
      const deviceId = operacao.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return acc
      ensureDevice(acc, deviceId)
      acc[deviceId].operacoes.push(operacao)
      return acc
    }, {})

    // 2. Incluir dispositivos que têm vendas mas não têm operações (ignorar testes)
    vendasFiltradas.forEach(v => {
      const deviceId = v.deviceId || 'geral'
      if (TEST_DEVICE_IDS.includes(deviceId)) return
      ensureDevice(porDispositivo, deviceId)
    })

    // 3. Incluir dispositivos conectados online APENAS se tiver caixa aberto
    // (evita mostrar dispositivos zerados sem sessão ativa)
    dispositivosConectados.forEach(d => {
      if (d.deviceId && !TEST_DEVICE_IDS.includes(d.deviceId) && d.status !== 'offline') {
        // Só incluir se já tem operações ou vendas (não criar card vazio)
        if (porDispositivo[d.deviceId]) {
          // já existe, ok
        }
        // Não criar card para dispositivo sem dados
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
  // Usa Set de IDs para evitar duplicação entre sessão 'geral' e sessões específicas
  const { totalAbertura, totalFechamento, saldoGeral, totalSuprimento, totalSangria, totalVendas, totalDinheiro, totalPix, totalCredito, totalDebito } = useMemo(() => {
    let ab = 0, ft = 0, sup = 0, san = 0
    const vendasContadas = new Set()
    let tv = 0, din = 0, pix = 0, cred = 0, deb = 0
    
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
        const vDev = v.deviceId || 'geral'
        const matchDevice = d.deviceId === 'geral' || vDev === d.deviceId
        return vTime >= ts && vTime <= tf && matchDevice
      })
      // Contar cada venda apenas uma vez (evitar duplicação entre 'geral' e específicos)
      vendasSessao.forEach(v => {
        const vid = v.id || v.numero
        if (vendasContadas.has(vid)) return
        vendasContadas.add(vid)
        tv += v.total || 0
        if (v.formaPagamento === 'DINHEIRO') din += v.total || 0
        else if (v.formaPagamento === 'PIX') pix += v.total || 0
        else if (v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO') cred += v.total || 0
        else if (v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO') deb += v.total || 0
      })
    })
    
    return { totalAbertura: ab, totalFechamento: ft, saldoGeral: ab + sup - san - ft, totalSuprimento: sup, totalSangria: san, totalVendas: tv, totalDinheiro: din, totalPix: pix, totalCredito: cred, totalDebito: deb }
  }, [dispositivos, vendas, caixaAtual])

  const tipoColors = {
    abertura: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    fechamento: 'bg-red-500/10 text-red-400 border-red-500/20',
    suprimento: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    sangria: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  }

  // Vendas da sessão de um dispositivo (aberta ou fechada)
  // Deve ser declarado ANTES de filteredDispositivos para evitar TDZ (Temporal Dead Zone)
  const getVendasSessao = (deviceId) => {
    const sessao = caixaAtual[deviceId]
    if (!sessao || !sessao.aberturaTimestamp) return []
    const ts = sessao.aberturaTimestamp
    const tf = sessao.aberto ? Date.now() : (sessao.fechamentoTimestamp || Date.now())
    return vendas.filter(v => {
      const vTime = new Date(v.createdAt || v.dataHora).getTime()
      const vDev = v.deviceId || 'geral'
      // Sessão 'geral' inclui todas as vendas (abertura sem dispositivo específico)
      // Sessão específica inclui apenas vendas do próprio dispositivo
      const matchDevice = deviceId === 'geral' || vDev === deviceId
      return vTime >= ts && vTime <= tf && matchDevice
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

  const filteredDispositivos = dispositivos.filter(d => {
    const s = search.toLowerCase()
    const matchSearch = d.deviceId.toLowerCase().includes(s)
    // Só mostrar dispositivos com caixa ABERTO
    const sessao = caixaAtual[d.deviceId]
    return matchSearch && sessao?.aberto === true
  })

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

  // Fechar caixas abertos por terminal + imprimir
  const handleFecharCaixa = async () => {
    const dispositivosAbertos = dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto)

    if (dispositivosAbertos.length === 0) {
      toast.warning('Nenhum caixa aberto para fechar')
      setShowFecharCaixaModal(false)
      return
    }

    setFechandoCaixa(true)
    let fechados = 0
    let erros = 0

    for (const dispositivo of dispositivosAbertos) {
      const sessao = caixaAtual[dispositivo.deviceId]
      const opsSessao = getOpsSessao(dispositivo.deviceId)
      const vendasSessao = getVendasSessao(dispositivo.deviceId)

      const totalAberturaSessao = opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
      const totalSuprimentoSessao = opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
      const totalSangriaSessao = opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)
      const totalVendasSessao = vendasSessao.reduce((s, v) => s + (v.total || 0), 0)

      const saldo = totalAberturaSessao + totalSuprimentoSessao - totalSangriaSessao + totalVendasSessao

      try {
        const res = await fetch(apiUrl('/api/operacoes'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({
            tipo: 'fechamento',
            valor: saldo,
            deviceId: dispositivo.deviceId,
            observacao: `Fechamento automático - Vendas: R$ ${totalVendasSessao.toFixed(2)}`
          })
        })
        if (res.ok) fechados++
        else erros++
      } catch {
        erros++
      }
    }

    // Recarregar dados
    const [opsRes, vendasRes] = await Promise.all([
      fetch(apiUrl('/api/operacoes'), { headers: { Authorization: `Bearer ${token}` } }),
      fetch(apiUrl('/api/vendas'), { headers: { Authorization: `Bearer ${token}` } })
    ])
    const opsData = await opsRes.json()
    const vendasData = await vendasRes.json()
    const novasOperacoes = Array.isArray(opsData) ? opsData : opsData.operacoes || []
    const novasVendas = Array.isArray(vendasData) ? vendasData : vendasData.data || []
    setOperacoes(novasOperacoes)
    setVendas(novasVendas)

    // Gerar PDF consolidado
    if (fechados > 0) {
      const totalAbertura = novasOperacoes.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
      const totalSuprimento = novasOperacoes.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
      const totalSangria = novasOperacoes.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)
      const totalVendas = novasVendas.reduce((s, v) => s + (v.total || 0), 0)
      const saldo = totalAbertura + totalSuprimento - totalSangria

      await gerarPDFFechamento({
        totalAbertura,
        totalSuprimento,
        totalSangria,
        totalFechamento: saldo,
        totalVendas,
        dataHora: new Date().toLocaleString('pt-BR'),
        operacoes: novasOperacoes.filter(o => o.tipo !== 'fechamento'),
        vendas: novasVendas
      })

      // Notificar dispositivos
      if (socket) {
        socket.emit('fechamento_geral', {
          valor: saldo,
          totalVendas,
          dataHora: new Date().toISOString()
        })
      }
    }

    setFechandoCaixa(false)
    setShowFecharCaixaModal(false)

    if (erros === 0) {
      toast.success(`${fechados} caixa(s) fechado(s) com sucesso! PDF gerado.`)
    } else {
      toast.warning(`${fechados} fechado(s), ${erros} erro(s)`)
    }
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
    <div className="space-y-5">
      {/* Summary Bar */}
      <div className="bg-gradient-to-r from-gray-900 to-gray-800 border border-white/5 rounded-2xl px-6 py-4 flex items-center justify-between flex-wrap gap-4 shadow-lg shadow-black/20">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center">
              <LockOpen size={18} className="text-emerald-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-500 uppercase tracking-wider">Caixas Abertos</p>
              <p className="text-lg font-bold text-white">{dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto).length}</p>
            </div>
          </div>
          <div className="w-px h-10 bg-white/5" />
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/20 flex items-center justify-center">
              <DollarSign size={18} className="text-blue-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-500 uppercase tracking-wider">Vendas Hoje</p>
              <p className="text-lg font-bold text-white whitespace-nowrap">R$ {totalVendas.toFixed(2)}</p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span className="px-3 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs">{dispositivos.length} dispositivo(s)</span>
        </div>
      </div>

      {/* Stats Cards Consolidados */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-4 gap-2.5">
        {[
          { label: 'Abertura', value: totalAbertura, icon: ArrowUpCircle, color: 'emerald' },
          { label: 'Fechamento', value: totalFechamento, icon: ArrowDownCircle, color: 'red' },
          { label: 'Suprimento', value: totalSuprimento, icon: PiggyBank, color: 'blue' },
          { label: 'Sangria', value: totalSangria, icon: Wallet, color: 'amber' },
          { label: 'Dinheiro', value: totalDinheiro, icon: DollarSign, color: 'emerald' },
          { label: 'PIX', value: totalPix, icon: Smartphone, color: 'blue' },
          { label: 'Crédito', value: totalCredito, icon: CreditCard, color: 'amber' },
          { label: 'Débito', value: totalDebito, icon: CreditCard, color: 'cyan' },
        ].map((stat, i) => {
          const c = stat.color
          const colors = {
            emerald: 'from-emerald-600/20 to-emerald-400/5 border-emerald-500/15 text-emerald-400',
            red: 'from-red-600/20 to-red-400/5 border-red-500/15 text-red-400',
            blue: 'from-blue-600/20 to-blue-400/5 border-blue-500/15 text-blue-400',
            amber: 'from-amber-600/20 to-amber-400/5 border-amber-500/15 text-amber-400',
            cyan: 'from-cyan-600/20 to-cyan-400/5 border-cyan-500/15 text-cyan-400',
          }
          return (
            <div key={i} className={`bg-gradient-to-br ${colors[c]} border rounded-xl p-3 transition-all hover:scale-[1.02]`}>
              <div className="flex items-center gap-2.5">
                <div className={`w-9 h-9 rounded-lg bg-white/10 flex items-center justify-center shrink-0`}>
                  <stat.icon size={16} className="text-current" />
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-bold text-white whitespace-nowrap">R$ {stat.value.toFixed(2)}</p>
                  <p className="text-[9px] text-white/50 uppercase tracking-wider">{stat.label}</p>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Abas de Operações e Vendas */}
      <div className="bg-gray-900/80 border border-white/5 rounded-2xl p-1.5 shadow-lg shadow-black/10 overflow-x-auto">
        <div className="flex gap-1 min-w-max">
          {[
            { id: 0, label: 'Aberturas', icon: ArrowUpCircle, color: 'emerald' },
            { id: 1, label: 'Fechamentos', icon: ArrowDownCircle, color: 'red' },
            { id: 2, label: 'Suprimentos', icon: PiggyBank, color: 'blue' },
            { id: 3, label: 'Sangrias', icon: Wallet, color: 'amber' },
            { id: 4, label: 'Dinheiro', icon: DollarSign, color: 'emerald' },
            { id: 5, label: 'PIX', icon: Smartphone, color: 'blue' },
            { id: 6, label: 'Crédito', icon: CreditCard, color: 'amber' },
            { id: 7, label: 'Débito', icon: CreditCard, color: 'cyan' }
          ].map(tab => {
            const activeColors = {
              emerald: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30 shadow-sm shadow-emerald-500/10',
              red: 'bg-red-500/20 text-red-300 border-red-500/30 shadow-sm shadow-red-500/10',
              blue: 'bg-blue-500/20 text-blue-300 border-blue-500/30 shadow-sm shadow-blue-500/10',
              amber: 'bg-amber-500/20 text-amber-300 border-amber-500/30 shadow-sm shadow-amber-500/10',
              cyan: 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30 shadow-sm shadow-cyan-500/10',
            }
            return (
              <button
                key={tab.id}
                onClick={() => setSelectedTab(tab.id)}
                className={`flex-1 min-w-[90px] flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                  selectedTab === tab.id
                    ? activeColors[tab.color] + ' border'
                    : 'text-gray-500 hover:text-gray-300 hover:bg-white/[0.03]'
                }`}
              >
                <tab.icon size={15} className={selectedTab === tab.id ? '' : 'text-gray-600'} />
                {tab.label}
              </button>
            )
          })}
        </div>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-3 flex-wrap">
          {user?.role === 'admin' && (
            <>
              <select
                value={filterEmpresa}
                onChange={(e) => setFilterEmpresa(e.target.value)}
                className="px-3 py-2.5 bg-gray-800/80 border border-white/5 rounded-xl text-sm text-white/80 focus:outline-none focus:border-blue-500/50 transition-colors"
              >
                <option value="">Todas as Empresas</option>
                {empresas.map(emp => (
                  <option key={emp.id} value={emp.id}>{emp.nome}</option>
                ))}
              </select>
              <select
                value={filterTerminal}
                onChange={(e) => setFilterTerminal(e.target.value)}
                className="px-3 py-2.5 bg-gray-800/80 border border-white/5 rounded-xl text-sm text-white/80 focus:outline-none focus:border-blue-500/50 transition-colors"
              >
                <option value="">Todos os Terminais</option>
                {dispositivosConectados.map(d => (
                  <option key={d.deviceId} value={d.deviceId}>{d.deviceName || d.deviceId}</option>
                ))}
              </select>
            </>
          )}
          <div className="relative">
            <Search size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
            <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="pl-9 pr-4 py-2.5 bg-gray-800/80 border border-white/5 rounded-xl text-sm text-white/80 placeholder-gray-500 focus:outline-none focus:border-blue-500/50 transition-colors w-52" placeholder="Buscar dispositivo..." />
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={() => { setShowFaturamento(!showFaturamento); setShowHistorico(false) }} className={`px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm font-medium transition-all ${showFaturamento ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20' : 'bg-white/5 hover:bg-white/10 text-gray-400 border border-white/5'}`}>
            <TrendingUp size={15} /> Faturamento
          </button>
          <button onClick={() => { setShowHistorico(!showHistorico); setShowFaturamento(false) }} className={`px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm font-medium transition-all ${showHistorico ? 'bg-amber-600 text-white shadow-lg shadow-amber-600/20' : 'bg-white/5 hover:bg-white/10 text-gray-400 border border-white/5'}`}>
            <Wallet size={15} /> Histórico
          </button>
          <button
            onClick={() => setShowFecharCaixaModal(true)}
            disabled={fechandoCaixa}
            className="relative px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm font-medium transition-all bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-red-600/20"
          >
            {fechandoCaixa ? (
              <Loader2 size={15} className="animate-spin" />
            ) : (
              <Printer size={15} />
            )}
            {fechandoCaixa ? 'Fechando...' : 'Fechar Caixa'}
            {!fechandoCaixa && dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto).length > 0 && (
              <span className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-amber-400 text-[10px] font-bold text-black flex items-center justify-center shadow-lg">
                {dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto).length}
              </span>
            )}
          </button>
          <button onClick={() => setShowModal(true)} className="px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm font-medium transition-all bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white shadow-lg shadow-blue-600/20">
            <Plus size={15} /> Nova Operação
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
        <div className="space-y-3">
          {filteredDispositivos.map(dispositivo => {
            const deviceName = dispositivo.deviceId === 'geral' ? 'Geral' : dispositivosConectados.find(d => d.deviceId === dispositivo.deviceId)?.deviceName || dispositivo.deviceName || dispositivo.deviceId
            const isExpanded = expandedDevice === dispositivo.deviceId
            const cardInfo = getCardInfo(dispositivo, selectedTab)
            const sessao = caixaAtual[dispositivo.deviceId]
            return (
            <div key={dispositivo.deviceId} className={`bg-gradient-to-br from-gray-900 to-gray-800/80 border border-white/5 rounded-2xl overflow-hidden transition-all duration-200 ${isExpanded ? 'shadow-xl shadow-black/30' : 'shadow-md shadow-black/10 hover:shadow-lg hover:shadow-black/20 hover:border-white/10'}`}>
              {/* Header do dispositivo */}
              <div 
                className="p-4 flex items-center justify-between cursor-pointer"
                onClick={() => setExpandedDevice(isExpanded ? null : dispositivo.deviceId)}
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl flex items-center justify-center bg-gradient-to-br from-emerald-500 to-emerald-600 shadow-lg shadow-emerald-500/20">
                    <Monitor size={22} className="text-white" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2.5">
                      <p className="font-semibold text-white text-sm truncate">{deviceName}</p>
                      <span className="px-2.5 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-wider bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 shrink-0">Aberto</span>
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5">
                      <span className="text-gray-400">{getOperacoesByTab(dispositivo, selectedTab).length}</span> {selectedTab >= 4 ? 'vendas' : 'operações'}
                      {sessao?.aberturaEm && (
                        <span> • <span className="text-gray-500">aberto</span> {new Date(sessao.aberturaEm).toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'})}</span>
                      )}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4 shrink-0">
                  <div className="text-right whitespace-nowrap">
                    <p className={`text-lg font-bold ${cardInfo.total >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                      R$ {cardInfo.total.toFixed(2)}
                    </p>
                    <p className="text-[10px] text-gray-500 uppercase tracking-wider">{cardInfo.label}</p>
                  </div>
                  <div className={`w-7 h-7 rounded-lg flex items-center justify-center transition-colors ${isExpanded ? 'bg-white/10' : 'bg-white/5'}`}>
                    {isExpanded ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />}
                  </div>
                </div>
              </div>

              {/* Status do Caixa / Informações da Aba */}
              <div className="px-4 pb-4">
                {(() => {
                  const isVenda = cardInfo.isVenda
                  const opsSessao = getOpsSessao(dispositivo.deviceId)
                  const vendasSessao = getVendasSessao(dispositivo.deviceId)
                  const totalVendasSessao = vendasSessao.reduce((s, v) => s + (v.total || 0), 0)
                  const totalAberturaSessao = opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0)
                  const totalSuprimentoSessao = opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0)
                  const totalSangriaSessao = opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0)

                  const diasSemana = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado']
                  const formatarData = (dt) => {
                    if (!dt) return null
                    const d = new Date(dt)
                    return { diaSemana: diasSemana[d.getDay()], data: d.toLocaleDateString('pt-BR'), hora: d.toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit', second: '2-digit'}) }
                  }
                  const aberturaInfo = sessao?.aberturaEm ? formatarData(sessao.aberturaEm) : null

                  return (
                    <div className="bg-black/20 rounded-xl p-3 space-y-3">
                      {/* Info da sessão - Abertura */}
                      {aberturaInfo && (
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-lg bg-emerald-500/15 flex items-center justify-center">
                              <LockOpen size={15} className="text-emerald-400" />
                            </div>
                            <div>
                              <p className="text-[10px] text-gray-500 uppercase tracking-wider">Abertura</p>
                              <p className="text-sm font-medium text-white">{aberturaInfo.diaSemana}, {aberturaInfo.data} às {aberturaInfo.hora}</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-bold text-emerald-400">R$ {totalAberturaSessao.toFixed(2)}</p>
                            {sessao?.operador && <p className="text-[10px] text-gray-500">{sessao.operador}</p>}
                          </div>
                        </div>
                      )}

                      {/* Resumo da sessão */}
                      {sessao?.aberturaTimestamp && !isVenda && (
                        <div className="grid grid-cols-3 gap-2">
                          {[
                            { label: 'Vendas', value: totalVendasSessao, color: 'text-blue-400', bg: 'bg-blue-500/10' },
                            { label: 'Suprimento', value: totalSuprimentoSessao, color: 'text-blue-400', bg: 'bg-blue-500/10' },
                            { label: 'Sangria', value: totalSangriaSessao, color: 'text-amber-400', bg: 'bg-amber-500/10' },
                          ].map((item, i) => (
                            <div key={i} className={`${item.bg} rounded-lg p-2.5 text-center`}>
                              <p className="text-[10px] text-gray-500 uppercase tracking-wider">{item.label}</p>
                              <p className={`text-sm font-bold ${item.color}`}>R$ {item.value.toFixed(2)}</p>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Última Operação/Venda da aba */}
                      {cardInfo.lastItem && (
                        <div className="flex items-center justify-between pt-2 border-t border-white/5">
                          <div>
                            <p className="text-[10px] text-gray-500 uppercase tracking-wider">{isVenda ? 'Última Venda' : 'Última Operação'}</p>
                            <p className="text-xs text-gray-300 mt-0.5">{new Date(cardInfo.lastItem.dataHora || cardInfo.lastItem.createdAt).toLocaleString('pt-BR')}</p>
                            {cardInfo.lastItem.nomeOperador && <p className="text-[10px] text-gray-500">por {cardInfo.lastItem.nomeOperador}</p>}
                          </div>
                          <p className="text-sm font-bold text-emerald-400">R$ {(cardInfo.lastItem.valor || cardInfo.lastItem.total || 0).toFixed(2)}</p>
                        </div>
                      )}
                    </div>
                  )
                })()}
              </div>

              {/* Lista de operações do dispositivo (filtradas por aba) */}
              {isExpanded && (
                <div className="border-t border-white/5 p-4 space-y-2 bg-black/20">
                  {getOperacoesByTab(dispositivo, selectedTab).length === 0 ? (
                    <div className="text-center py-8">
                      <div className="w-12 h-12 rounded-xl bg-gray-500/5 flex items-center justify-center mx-auto mb-3">
                        {selectedTab >= 4 ? <DollarSign size={20} className="text-gray-600" /> : <Wallet size={20} className="text-gray-600" />}
                      </div>
                      <p className="text-gray-500 text-sm">
                        {selectedTab >= 4 
                          ? `Nenhuma venda via ${['','Dinheiro','PIX','Crédito','Débito'][selectedTab-3]} nesta sessão`
                          : `Nenhuma operação de ${['abertura','fechamento','suprimento','sangria'][selectedTab]} nesta sessão`
                        }
                      </p>
                      {!sessao?.aberturaTimestamp && (
                        <p className="text-gray-600 text-xs mt-1">Abra o caixa para registrar operações</p>
                      )}
                    </div>
                  ) : (
                    <div className="space-y-1.5">
                    {getOperacoesByTab(dispositivo, selectedTab).map((item, idx) => {
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
                            <p className={`text-sm font-bold shrink-0 whitespace-nowrap ${isVenda ? 'text-emerald-400' : (item.tipo === 'abertura' || item.tipo === 'suprimento' ? 'text-emerald-400' : 'text-red-400')}`}>
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
                  }
                </div>
              )}
            </div>
          )}
        </div>
      )})}
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

      {/* Modal Fechar Caixa */}
      {showFecharCaixaModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => !fechandoCaixa && setShowFecharCaixaModal(false)}>
          <div className="glass p-6 w-full max-w-md glow-red max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            {fechandoCaixa ? (
              <div className="text-center py-8">
                <Loader2 size={40} className="animate-spin mx-auto text-red-400 mb-4" />
                <p className="text-white font-medium">Fechando caixas...</p>
                <p className="text-gray-500 text-sm mt-1">Processando fechamento dos terminais</p>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between mb-5">
                  <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                    <Printer size={20} className="text-red-400" /> Fechar Caixa
                  </h3>
                  <button onClick={() => setShowFecharCaixaModal(false)} className="text-gray-400 hover:text-white">
                    <Plus size={20} className="rotate-45" />
                  </button>
                </div>
                <p className="text-gray-400 text-sm mb-4">
                  Serão fechados os caixas dos terminais abaixo. Um PDF consolidado será gerado automaticamente.
                </p>
                <div className="space-y-2 mb-6 max-h-60 overflow-y-auto">
                  {dispositivos.filter(d => caixaAtual[d.deviceId]?.aberto).map(d => {
                    const vendasSessao = getVendasSessao(d.deviceId)
                    const totalVendas = vendasSessao.reduce((s, v) => s + (v.total || 0), 0)
                    return (
                      <div key={d.deviceId} className="flex items-center justify-between p-3 bg-black/20 rounded-lg border border-white/5">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center">
                            <Monitor size={16} className="text-emerald-400" />
                          </div>
                          <div>
                            <p className="text-sm font-medium text-white">{d.deviceId === 'geral' ? 'Geral' : dispositivosConectados.find(dc => dc.deviceId === d.deviceId)?.deviceName || d.deviceId}</p>
                            <p className="text-xs text-gray-500">{vendasSessao.length} venda(s)</p>
                          </div>
                        </div>
                        <p className="text-sm font-bold text-emerald-400">R$ {totalVendas.toFixed(2)}</p>
                      </div>
                    )
                  })}
                </div>
                {dispositivos.filter(d => !caixaAtual[d.deviceId]?.aberto && caixaAtual[d.deviceId]?.aberturaTimestamp).length > 0 && (
                  <p className="text-xs text-gray-500 mb-4 flex items-center gap-1">
                    <CheckCircle2 size={12} className="text-gray-500" />
                    {dispositivos.filter(d => !caixaAtual[d.deviceId]?.aberto && caixaAtual[d.deviceId]?.aberturaTimestamp).length} caixa(s) já fechado(s) — serão ignorados
                  </p>
                )}
                <div className="flex gap-3 pt-2">
                  <button onClick={() => setShowFecharCaixaModal(false)} className="btn-ghost flex-1">Cancelar</button>
                  <button onClick={handleFecharCaixa} className="flex-1 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white px-4 py-2.5 rounded-lg flex items-center justify-center gap-2 text-sm font-medium transition-all shadow-lg shadow-red-600/20">
                    <Printer size={16} />
                    Fechar & Imprimir
                  </button>
                </div>
              </>
            )}
          </div>
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

    </div>
  )
}
