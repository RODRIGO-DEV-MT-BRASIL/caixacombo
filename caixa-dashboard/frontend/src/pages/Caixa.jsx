import { useState, useEffect } from 'react'
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

    if (socket) {
      socket.on('operacao_adicionada', (data) => {
        setOperacoes(prev => [...prev, data])
      })

      socket.on('operacoes_sync', (data) => {
        setOperacoes(data)
      })

      socket.on('venda_added', (data) => {
        setVendas(prev => [...prev, data])
      })

      socket.on('sale_update', (data) => {
        setVendas(prev => [...prev, data.sale])
      })

      socket.on('vendas_sync', (data) => {
        setVendas(data)
      })
    }

    return () => {
      socket.off('operacao_adicionada')
      socket.off('operacoes_sync')
      socket.off('venda_added')
      socket.off('sale_update')
      socket.off('vendas_sync')
    }
  }, [])

  // Escutar atualizações de operações via WebSocket
  useEffect(() => {
    if (!socket) return

    socket.on('operacao_adicionada', (operacao) => {
      console.log('Nova operação recebida:', operacao)
      setOperacoes(prev => [...prev, operacao])
    })

    socket.on('operacoes_sync', (data) => {
      if (data.operacoes) {
        console.log('Sincronização recebida:', data.operacoes)
        setOperacoes(data.operacoes)
      }
    })

    socket.on('venda_added', (venda) => {
      console.log('Nova venda recebida:', venda)
      setVendas(prev => [...prev, venda])
    })

    socket.on('sale_update', (data) => {
      console.log('Atualização de venda:', data)
      if (data.sale) {
        setVendas(prev => [...prev, data.sale])
      }
    })

    return () => {
      socket.off('operacao_adicionada')
      socket.off('operacoes_sync')
      socket.off('venda_added')
      socket.off('sale_update')
    }
  }, [socket])

  
  // Agrupar operações por dispositivo
  const operacoesPorDispositivo = operacoes.reduce((acc, operacao) => {
    const deviceId = operacao.deviceId || 'geral'
    if (!acc[deviceId]) {
      acc[deviceId] = {
        deviceId,
        operacoes: [],
        totalAbertura: 0,
        totalFechamento: 0,
        totalSuprimento: 0,
        totalSangria: 0,
        saldo: 0,
        ultimaAbertura: null,
        caixaAberto: false
      }
    }
    acc[deviceId].operacoes.push(operacao)
    
    if (operacao.tipo === 'abertura') {
      acc[deviceId].totalAbertura += (operacao.valor || 0)
      acc[deviceId].ultimaAbertura = operacao
      acc[deviceId].caixaAberto = true
    } else if (operacao.tipo === 'fechamento') {
      acc[deviceId].totalFechamento += (operacao.valor || 0)
      acc[deviceId].caixaAberto = false
    } else if (operacao.tipo === 'suprimento') {
      acc[deviceId].totalSuprimento += (operacao.valor || 0)
    } else if (operacao.tipo === 'sangria') {
      acc[deviceId].totalSangria += (operacao.valor || 0)
    }
    
    acc[deviceId].saldo = acc[deviceId].totalAbertura - acc[deviceId].totalFechamento + acc[deviceId].totalSuprimento - acc[deviceId].totalSangria
    return acc
  }, {})

  const dispositivos = Object.values(operacoesPorDispositivo)

  const totalAbertura = operacoes.filter(o => o.tipo === 'abertura').reduce((sum, o) => sum + (o.valor || 0), 0)
  const totalFechamento = operacoes.filter(o => o.tipo === 'fechamento').reduce((sum, o) => sum + (o.valor || 0), 0)
  const saldoGeral = totalAbertura - totalFechamento

  // Calcular totais de vendas por forma de pagamento
  const totalVendas = vendas.reduce((sum, v) => sum + (v.total || 0), 0)
  const totalDinheiro = vendas.filter(v => v.formaPagamento === 'DINHEIRO').reduce((sum, v) => sum + (v.total || 0), 0)
  const totalPix = vendas.filter(v => v.formaPagamento === 'PIX').reduce((sum, v) => sum + (v.total || 0), 0)
  const totalCredito = vendas.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((sum, v) => sum + (v.total || 0), 0)
  const totalDebito = vendas.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((sum, v) => sum + (v.total || 0), 0)
  const totalCartao = vendas.filter(v => v.formaPagamento === 'CARTAO').reduce((sum, v) => sum + (v.total || 0), 0)

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

  // Filtrar operações por aba selecionada (por dispositivo)
  const getOperacoesByTab = (dispositivo, tab) => {
    let filtered
    switch(tab) {
      case 0: filtered = dispositivo.operacoes.filter(o => o.tipo === 'abertura'); break
      case 1: filtered = dispositivo.operacoes.filter(o => o.tipo === 'fechamento'); break
      case 2: filtered = dispositivo.operacoes.filter(o => o.tipo === 'suprimento'); break
      case 3: filtered = dispositivo.operacoes.filter(o => o.tipo === 'sangria'); break
      case 4: filtered = vendas.filter(v => v.deviceId === dispositivo.deviceId && v.formaPagamento === 'DINHEIRO'); break
      case 5: filtered = vendas.filter(v => v.deviceId === dispositivo.deviceId && v.formaPagamento === 'PIX'); break
      case 6: filtered = vendas.filter(v => v.deviceId === dispositivo.deviceId && (v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO')); break
      case 7: filtered = vendas.filter(v => v.deviceId === dispositivo.deviceId && (v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO')); break
      default: filtered = dispositivo.operacoes
    }
    
    return filtered
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
    const dispositivosComCaixaAberto = dispositivos.filter(d => d.caixaAberto)
    
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
      })

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
            <LockOpen size={24} className="text-emerald-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {totalAbertura.toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Abertura</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center">
            <Lock size={24} className="text-red-400" />
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
            <p className="text-2xl font-bold text-white">R$ {operacoes.filter(o => o.tipo === 'suprimento').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</p>
            <p className="text-xs text-gray-400">Total Suprimento</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
            <Wallet size={24} className="text-amber-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">R$ {operacoes.filter(o => o.tipo === 'sangria').reduce((sum, o) => sum + (o.valor || 0), 0).toFixed(2)}</p>
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
            { id: 0, label: 'Aberturas', icon: LockOpen, color: 'emerald' },
            { id: 1, label: 'Fechamentos', icon: Lock, color: 'red' },
            { id: 2, label: 'Suprimentos', icon: PiggyBank, color: 'blue' },
            { id: 3, label: 'Sangrias', icon: Wallet, color: 'amber' },
            { id: 4, label: 'Dinheiro', icon: DollarSign, color: 'emerald' },
            { id: 5, label: 'PIX', icon: Smartphone, color: 'blue' },
            { id: 6, label: 'Crédito', icon: CreditCard, color: 'amber' },
            { id: 7, label: 'Débito', icon: CreditCard, color: 'cyan' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setSelectedTab(tab.id)}
              className={`flex-1 min-w-[100px] flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                selectedTab === tab.id
                  ? `bg-${tab.color}-500/20 text-${tab.color}-400 border border-${tab.color}-500/20`
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
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center">
                    <Monitor size={24} className="text-white" />
                  </div>
                  <div>
                    <p className="font-semibold text-white text-sm">{dispositivo.deviceId === 'geral' ? 'Geral' : dispositivo.deviceId}</p>
                    <p className="text-xs text-gray-500">{getOperacoesByTab(dispositivo, selectedTab).length} {selectedTab >= 4 ? 'vendas' : 'operações'}</p>
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
                          ) : dispositivo.caixaAberto ? (
                            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center">
                              <LockOpen size={16} className="text-emerald-400" />
                            </div>
                          ) : (
                            <div className="w-8 h-8 rounded-lg bg-red-500/20 flex items-center justify-center">
                              <Lock size={16} className="text-red-400" />
                            </div>
                          )}
                          <span className={`text-sm font-medium ${cardInfo.isVenda ? 'text-blue-400' : (dispositivo.caixaAberto ? 'text-emerald-400' : 'text-red-400')}`}>
                            {cardInfo.isVenda ? cardInfo.label : `Caixa ${dispositivo.caixaAberto ? 'Aberto' : 'Fechado'}`}
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
                    <div className="text-center py-4">
                      <p className="text-gray-400">Nenhuma operação encontrada</p>
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
                    <option key={d.deviceId} value={d.deviceId}>{d.deviceId}</option>
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
                        <span className="text-sm font-medium text-white">{d.deviceId}</span>
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
