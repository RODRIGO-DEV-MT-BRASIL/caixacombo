import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToast } from '../components/Toast'
import {
  LayoutDashboard, Package, Tags, ShoppingCart, Wifi, WifiOff,
  LogOut, Menu, X, Monitor, Lock, Unlock, Clock, Play,
  ChevronRight, Activity, TrendingUp, AlertTriangle, CheckCircle2,
  Search, RefreshCw, Eye, EyeOff, RefreshCw as RotateCw, Cpu, DollarSign,
  Settings,
  Play as PlayIcon, X as CloseIcon, Power, History, Building2, Users,
  BarChart3, PieChart, TrendingUp as Trending, DollarSign as Dollar
} from 'lucide-react'
import Produtos from './Produtos'
import Categorias from './Categorias'
import Vendas from './Vendas'
import Caixa from './Caixa'
import FechamentoGeral from './FechamentoGeral'
import Auditoria from './Auditoria'
import Empresas from './Empresas'
import ClientesFuncionarios from './ClientesFuncionarios'
import Terminais from './Terminais'
import Configuracoes from './Configuracoes'
import ConfiguracoesImpressao from './ConfiguracoesImpressao'
import DashboardCharts from './DashboardCharts'
import { Printer } from 'lucide-react'

const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, permission: 'dashboard' },
  { id: 'terminais', label: 'Terminais', icon: Monitor, permission: 'dashboard' },
  { id: 'empresas-admin', label: 'Cadastro White-label', icon: Building2, permission: 'empresas', adminOnly: true },
  { id: 'empresas', label: 'Funcionários/Clientes', icon: Users, empresaOnly: true },
  { id: 'categorias', label: 'Categorias', icon: Tags, permission: 'categorias' },
  { id: 'produtos', label: 'Produtos', icon: Package, permission: 'produtos' },
  { id: 'vendas', label: 'Vendas', icon: ShoppingCart, permission: 'vendas' },
  { id: 'caixa', label: 'Caixa', icon: DollarSign, permission: 'caixa' },
  { id: 'fechamento', label: 'Fechamento', icon: Lock, permission: 'caixa' },
  { id: 'auditoria', label: 'Auditoria', icon: History, permission: 'auditoria' },
  { id: 'config', label: 'Config', icon: Settings, permission: 'config' },
  { id: 'impressao', label: 'Impressão', icon: Printer, permission: 'config' },
]

export default function Dashboard() {
  const { user, logout, token, hasPermission, hasPageAccess } = useAuth()
  const { devices, connected, socket, vendas } = useSocket()
  const [page, setPage] = useState(() => sessionStorage.getItem('currentPage') || 'dashboard')
  const [stats, setStats] = useState({ vendasHoje: 0, qtdVendas: 0 })

  // Persistir página atual no sessionStorage para não voltar ao dashboard ao recarregar
  useEffect(() => {
    if (page) sessionStorage.setItem('currentPage', page)
  }, [page])

  // Aplicar cores do branding
  useEffect(() => {
    const root = document.documentElement
    const b = user?.branding || {}
    const primary = b.primaryColor || '#3b82f6'
    const secondary = b.secondaryColor || '#06b6d4'
    const accent = b.accentColor || '#10b981'
    root.style.setProperty('--color-primary', primary)
    root.style.setProperty('--color-secondary', secondary)
    root.style.setProperty('--color-accent', accent)
    root.style.setProperty('--color-primary-rgb', hexToRgb(primary))
  }, [user?.branding])

  const hexToRgb = (hex) => {
    const r = parseInt(hex.slice(1, 3), 16)
    const g = parseInt(hex.slice(3, 5), 16)
    const b = parseInt(hex.slice(5, 7), 16)
    return `${r}, ${g}, ${b}`
  }

  const primaryColor = user?.branding?.primaryColor || '#3b82f6'
  const primaryBg = `${primaryColor}20`
  const primaryBorder = `${primaryColor}33`

  // Calcular stats reais das vendas
  useEffect(() => {
    const hoje = new Date().toDateString()
    const vendasHoje = vendas.filter(v => {
      if (v.cancelada) return false
      const d = new Date(v.createdAt || v.dataHora)
      return d.toDateString() === hoje
    })
    const total = vendasHoje.reduce((s, v) => s + (v.total || 0), 0)
    setStats({ vendasHoje: total, qtdVendas: vendasHoje.length })
  }, [vendas])

  // Redirecionar se página atual não está permitida
  useEffect(() => {
    if (user && !hasPageAccess(page)) {
      const firstAllowed = navItems.find(item => hasPageAccess(item.id))
      if (firstAllowed) setPage(firstAllowed.id)
    }
  }, [user, page, hasPageAccess])

  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)


  const onlineDevices = devices.filter(d => d.online || d.status === 'online' || d.status === 'in_use')
  const inUseDevices = devices.filter(d => d.status === 'in_use')

  // Filtrar itens do menu baseado em permissões e páginas permitidas
  const filteredNavItems = navItems.filter(item => {
    if (!hasPageAccess(item.id)) return false
    if (item.adminOnly && user?.role !== 'admin') return false
    if (item.empresaOnly && user?.role !== 'empresa') return false
    if (item.permission && item.id !== 'terminais' && user?.role !== 'empresa') return hasPermission(item.permission)
    return true
  })

  const handleForceSync = async () => {
    if (socket && connected) {
      socket.emit('dashboard_connect', { token })
    } else {
      if (socket) socket.connect()
    }
  }

  return (
    <div className="h-screen bg-gray-950 flex overflow-hidden">
      {/* Sidebar */}
      <aside className={`fixed lg:static inset-y-0 left-0 z-40 glass border-r border-white/5 transform transition-all duration-300 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'} ${sidebarCollapsed ? 'w-20' : 'w-64'} h-full`}>
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className={`flex items-center gap-2 shrink-0 ${sidebarCollapsed ? 'p-2 justify-center' : 'p-3'}`}>
            <img src={user?.branding?.logoUrl || "/controle.png"} alt="Logo" className={`${sidebarCollapsed ? 'w-8 h-8' : 'w-10 h-10'} rounded-xl object-contain shrink-0`} onError={(e) => { e.target.src = "/controle.png" }} />
            {!sidebarCollapsed && (
              <div>
                <h1 className="font-bold text-white text-base leading-tight">{user?.branding?.companyName || 'CaixaCombo'}</h1>
                <p className="text-xs text-gray-500">{user?.role === 'empresa' ? user?.empresaNome || 'Empresa' : 'Dashboard v1.1'}</p>
              </div>
            )}
            <button onClick={() => setSidebarOpen(false)} className="lg:hidden ml-auto text-gray-400 hover:text-white">
              <X size={18} />
            </button>
          </div>

          {/* Nav */}
          <nav className="flex-1 min-h-0 overflow-y-auto px-2 py-2 space-y-0.5">
            {filteredNavItems.map(item => (
              <button
                key={item.id}
                onClick={() => { setPage(item.id); setSidebarOpen(false) }}
                className={`w-full flex items-center gap-3 ${sidebarCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-2'} rounded-lg text-sm font-medium transition-all duration-200 ${
                  page === item.id ? '' : 'text-gray-400 hover:text-white hover:bg-white/5'
                }`}
                style={page === item.id ? { backgroundColor: primaryBg, borderColor: primaryBorder, borderWidth: '1px', color: primaryColor } : {}}
                title={sidebarCollapsed ? item.label : ''}
              >
                <item.icon size={18} className="shrink-0" />
                {!sidebarCollapsed && item.label}
              </button>
            ))}
          </nav>

          {/* Connection Status */}
          <div className="px-2 py-1.5 shrink-0">
            <div className={`flex items-center gap-2 ${sidebarCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-1.5'} rounded-lg text-xs font-medium ${
              connected ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
            }`}>
              {connected ? <Wifi size={14} className="shrink-0" /> : <WifiOff size={14} className="shrink-0" />}
              {!sidebarCollapsed && (connected ? 'WebSocket Conectado' : 'WebSocket Desconectado')}
            </div>
          </div>

          {/* User */}
          <div className="px-2 py-1.5 border-t border-white/5 shrink-0">
            <div className={`flex items-center gap-3 ${sidebarCollapsed ? 'px-0 justify-center' : 'px-3 py-1.5'}`}>
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-xs font-bold text-white shrink-0">
                {user?.username?.charAt(0).toUpperCase()}
              </div>
              {!sidebarCollapsed && (
                <>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white truncate">{user?.username}</p>
                    <p className="text-xs text-gray-500 capitalize">{user?.role}</p>
                  </div>
                  <button onClick={logout} className="text-gray-500 hover:text-red-400 transition-colors" title="Sair">
                    <LogOut size={16} />
                  </button>
                </>
              )}
              {sidebarCollapsed && (
                <button onClick={logout} className="text-gray-500 hover:text-red-400 transition-colors" title="Sair">
                  <LogOut size={16} />
                </button>
              )}
            </div>
          </div>

          {/* Collapse Toggle */}
          <div className="hidden lg:block px-2 py-1.5 border-t border-white/5 shrink-0">
            <button
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="w-full flex items-center justify-center gap-2 px-3 py-1.5 rounded-lg text-xs text-gray-500 hover:text-white hover:bg-white/5 transition-all"
              title={sidebarCollapsed ? 'Expandir sidebar' : 'Recolher sidebar'}
            >
              <svg className={`w-4 h-4 transition-transform duration-300 ${sidebarCollapsed ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
              </svg>
              {!sidebarCollapsed && 'Recolher'}
            </button>
          </div>
        </div>
      </aside>

      {/* Overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/50 z-30 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Main Content */}
      <main className="flex-1 h-screen overflow-hidden flex flex-col">
        {/* Top bar */}
        <header className="shrink-0 z-20 glass border-b border-white/5 px-6 py-4 flex items-center gap-4">
          <button onClick={() => setSidebarOpen(true)} className="lg:hidden text-gray-400 hover:text-white">
            <Menu size={22} />
          </button>
          <h2 className="text-lg font-semibold text-white capitalize">{navItems.find(i => i.id === page)?.label}</h2>
          <div className="ml-auto flex items-center gap-3">
            <button
              onClick={handleForceSync}
              className="px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1"
              style={{ backgroundColor: `${primaryColor}20`, borderColor: `${primaryColor}33`, borderWidth: '1px', color: primaryColor }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = `${primaryColor}30`}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = `${primaryColor}20`}
              title="Forçar sincronização dos dispositivos"
            >
              <RefreshCw size={14} />
              Sincronizar
            </button>
            <span className="text-xs text-gray-500">{new Date().toLocaleString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit', day: '2-digit', month: '2-digit', year: '2-digit' })}</span>
          </div>
        </header>

        <div className="p-6 flex-1 min-h-0 overflow-auto max-w-7xl mx-auto w-full">
          {page === 'dashboard' && (
            <div className="space-y-6">
              {/* Stats */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="stat-card" style={{ boxShadow: `0 0 20px ${primaryColor}26, 0 0 60px ${primaryColor}0D` }}>
                  <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ backgroundColor: `${primaryColor}33` }}>
                    <Dollar size={24} style={{ color: primaryColor }} />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">R$ {stats.vendasHoje.toFixed(2).replace('.', ',')}</p>
                    <p className="text-xs text-gray-400">Vendas Hoje</p>
                  </div>
                </div>
                <div className="stat-card glow-green">
                  <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
                    <Trending size={24} className="text-emerald-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{stats.qtdVendas}</p>
                    <p className="text-xs text-gray-400">Vendas no Dia</p>
                  </div>
                </div>
                <div className="stat-card glow-red">
                  <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center">
                    <Monitor size={24} className="text-red-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{onlineDevices.length}</p>
                    <p className="text-xs text-gray-400">Terminais Online</p>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
                    <Clock size={24} className="text-amber-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-white">{inUseDevices.length}</p>
                    <p className="text-xs text-gray-400">Terminais em Uso</p>
                  </div>
                </div>
              </div>

              {/* Dashboard Charts */}
              <DashboardCharts />
            </div>
          )}

          {page === 'terminais' && <Terminais />}
          {page === 'empresas-admin' && <Empresas />}
          {page === 'empresas' && user?.role === 'empresa' && <ClientesFuncionarios />}
          {page === 'empresas' && user?.role === 'admin' && <Empresas />}
          {page === 'produtos' && <Produtos />}
          {page === 'categorias' && <Categorias />}
          {page === 'vendas' && <Vendas />}
          {page === 'caixa' && <Caixa onNavigateToFechamento={() => setPage('fechamento')} />}
          {page === 'fechamento' && <FechamentoGeral onBack={() => setPage('caixa')} />}
          {page === 'auditoria' && <Auditoria />}
          {page === 'config' && <Configuracoes />}
          {page === 'impressao' && <ConfiguracoesImpressao />}
        </div>
      </main>
    </div>
  )
}
