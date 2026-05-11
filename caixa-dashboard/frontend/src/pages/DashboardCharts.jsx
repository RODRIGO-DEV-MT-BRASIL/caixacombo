import { useEffect, useState, useMemo } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { apiUrl } from '../utils/api'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, AreaChart, Area, LineChart, Line
} from 'recharts'
import { TrendingUp, DollarSign, CreditCard, Smartphone, Banknote, ShoppingCart, BarChart3 } from 'lucide-react'

const COLORS = {
  primary: '#3b82f6',
  secondary: '#06b6d4',
  accent: '#10b981',
  danger: '#ef4444',
  warning: '#f59e0b',
  purple: '#8b5cf6',
  pink: '#ec4899'
}

const CHART_COLORS = [
  COLORS.primary, COLORS.secondary, COLORS.accent, COLORS.warning,
  COLORS.purple, COLORS.pink, COLORS.danger
]

export default function DashboardCharts() {
  const { user, token } = useAuth()
  const { vendas, devices } = useSocket()
  const [vendasData, setVendasData] = useState(vendas || [])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Fetch vendas se não estiverem no socket ainda
    fetch(apiUrl('/api/vendas'), { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.json())
      .then(data => { setVendasData(data || []); setLoading(false) })
      .catch(() => setLoading(false))
  }, [token])

  // Dados de hoje
  const hoje = new Date().toDateString()
  const vendasHoje = vendasData.filter(v => new Date(v.createdAt || v.dataHora).toDateString() === hoje)

  // Total vendas hoje
  const totalVendasHoje = vendasHoje.reduce((s, v) => s + (v.total || 0), 0)

  // Vendas por forma de pagamento (hoje)
  const porPagamento = useMemo(() => {
    const map = { DINHEIRO: 0, PIX: 0, CREDITO: 0, DEBITO: 0, CARTAO_CREDITO: 0, CARTAO_DEBITO: 0 }
    vendasHoje.forEach(v => { map[v.formaPagamento] = (map[v.formaPagamento] || 0) + (v.total || 0) })
    return [
      { name: 'Dinheiro', value: map.DINHEIRO || 0, icon: Banknote, color: '#10b981' },
      { name: 'PIX', value: map.PIX || 0, icon: Smartphone, color: '#06b6d4' },
      { name: 'Crédito', value: (map.CREDITO || map.CARTAO_CREDITO || 0), icon: CreditCard, color: '#f59e0b' },
      { name: 'Débito', value: (map.DEBITO || map.CARTAO_DEBITO || 0), icon: CreditCard, color: '#3b82f6' }
    ].filter(d => d.value > 0)
  }, [vendasHoje])

  // Vendas por terminal (hoje)
  const porTerminal = useMemo(() => {
    const map = {}
    vendasHoje.forEach(v => {
      const key = v.deviceName || v.deviceId || 'N/A'
      map[key] = (map[key] || 0) + (v.total || 0)
    })
    return Object.entries(map).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
  }, [vendasHoje])

  // Vendas últimos 7 dias
  const vendasSemana = useMemo(() => {
    const dias = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      dias.push({ data: d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }), total: 0, count: 0 })
    }
    vendasData.forEach(v => {
      const vd = new Date(v.createdAt || v.dataHora)
      const str = vd.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
      const dia = dias.find(d => d.data === str)
      if (dia) { dia.total += (v.total || 0); dia.count += 1 }
    })
    return dias
  }, [vendasData])

  // Status dos terminais
  const onlineCount = devices.filter(d => d.online || d.status === 'online' || d.status === 'in_use').length
  const lockedCount = devices.filter(d => d.status === 'locked').length
  const inUseCount = devices.filter(d => d.status === 'in_use').length
  const offlineCount = devices.filter(d => !d.online && d.status !== 'online' && d.status !== 'in_use').length

  const terminalStatus = [
    { name: 'Online', value: onlineCount, color: COLORS.accent },
    { name: 'Bloqueado', value: lockedCount, color: COLORS.danger },
    { name: 'Em Uso', value: inUseCount, color: COLORS.warning },
    { name: 'Offline', value: offlineCount, color: '#6b7280' }
  ].filter(d => d.value > 0)

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-gray-900/95 border border-white/10 rounded-lg p-3 shadow-xl">
          <p className="text-gray-400 text-xs mb-1">{label}</p>
          {payload.map((p, i) => (
            <p key={i} className="text-white text-sm font-semibold">
              {p.dataKey === 'value' ? `R$ ${p.value?.toFixed(2)}` : `${p.value} vendas`}
            </p>
          ))}
        </div>
      )
    }
    return null
  }

  if (loading) {
    return (
      <div className="glass p-12 text-center">
        <div className="animate-spin w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full mx-auto mb-3" />
        <p className="text-gray-400">Carregando dados...</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Gráficos principais */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Vendas por período (Area) */}
        <div className="glass p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <TrendingUp size={18} className="text-blue-400" />
              <h3 className="font-semibold text-white">Vendas Últimos 7 Dias</h3>
            </div>
            <span className="text-xs text-gray-500">Em tempo real</span>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={vendasSemana}>
                <defs>
                  <linearGradient id="colorVendas" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={COLORS.primary} stopOpacity={0.3}/>
                    <stop offset="95%" stopColor={COLORS.primary} stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="data" stroke="#6b7280" fontSize={11} />
                <YAxis stroke="#6b7280" fontSize={11} tickFormatter={v => `R$${v}`} />
                <Tooltip content={<CustomTooltip />} />
                <Area type="monotone" dataKey="total" stroke={COLORS.primary} strokeWidth={2} fillOpacity={1} fill="url(#colorVendas)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Vendas por forma de pagamento (Pie) */}
        <div className="glass p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <CreditCard size={18} className="text-emerald-400" />
              <h3 className="font-semibold text-white">Pagamentos Hoje</h3>
            </div>
            <span className="text-2xl font-bold text-white">R$ {totalVendasHoje.toFixed(2)}</span>
          </div>
          <div className="h-56">
            {porPagamento.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={porPagamento}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    labelLine={false}
                  >
                    {porPagamento.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} stroke="transparent" />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">
                Sem vendas hoje
              </div>
            )}
          </div>
          <div className="grid grid-cols-2 gap-2 mt-2">
            {porPagamento.map(p => (
              <div key={p.name} className="flex items-center gap-2 text-xs">
                <div className="w-3 h-3 rounded" style={{ backgroundColor: p.color }} />
                <span className="text-gray-400">{p.name}:</span>
                <span className="text-white font-medium">R$ {p.value.toFixed(2)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Segunda linha de gráficos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Vendas por terminal (Bar) */}
        <div className="glass p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <BarChart3 size={18} className="text-purple-400" />
              <h3 className="font-semibold text-white">Vendas por Terminal</h3>
            </div>
            <span className="text-xs text-gray-500">Hoje</span>
          </div>
          <div className="h-56">
            {porTerminal.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={porTerminal} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                  <XAxis type="number" stroke="#6b7280" fontSize={11} tickFormatter={v => `R$${v}`} />
                  <YAxis type="category" dataKey="name" stroke="#6b7280" fontSize={11} width={100} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                    {porTerminal.map((_, i) => (
                      <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">
                Sem vendas hoje
              </div>
            )}
          </div>
        </div>

        {/* Status dos terminais (Pie) */}
        <div className="glass p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <ShoppingCart size={18} className="text-amber-400" />
              <h3 className="font-semibold text-white">Status Terminais</h3>
            </div>
            <span className="text-xs text-gray-500">{devices.length} total</span>
          </div>
          <div className="h-56">
            {terminalStatus.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={terminalStatus}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={4}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}
                  >
                    {terminalStatus.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} stroke="transparent" />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-gray-500">
                Sem terminais conectados
              </div>
            )}
          </div>
          <div className="flex flex-wrap gap-3 mt-2">
            {terminalStatus.map(s => (
              <div key={s.name} className="flex items-center gap-2 text-xs">
                <div className="w-3 h-3 rounded" style={{ backgroundColor: s.color }} />
                <span className="text-gray-400">{s.name}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
