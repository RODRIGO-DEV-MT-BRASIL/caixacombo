import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../components/Toast'
import { Building2, Plus, Edit2, Trash2, Check, X, Key, Shield, Users, Phone, Mail, MapPin, Search, ArrowLeft, Palette, Globe, Eye, Layout, Save, Loader2, Upload, ChevronRight, UserCog, BadgeCheck, Settings, Printer } from 'lucide-react'

const defaultFormData = {
  nome: '', tipoPessoa: 'juridica', cnpj: '', email: '', telefone: '', login: '', senha: '',
  permissoes: { dashboard: false, produtos: false, categorias: false, vendas: false, caixa: false, auditoria: false },
  primaryColor: '#3b82f6', secondaryColor: '#06b6d4', accentColor: '#10b981', logoUrl: '',
  paginasPermitidas: ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa'],
  slug: ''
}

const STEPS = [
  { id: 'info', label: 'Informações', icon: Building2 },
  { id: 'access', label: 'Acesso', icon: Key },
  { id: 'branding', label: 'Identidade', icon: Palette },
  { id: 'pages', label: 'Páginas', icon: Layout },
]

export default function Empresas() {
  const { token, user } = useAuth()
  const { success } = useToast()
  const [activeTab, setActiveTab] = useState('empresas')
  const [empresas, setEmpresas] = useState([])
  const [clientes, setClientes] = useState([])
  const [view, setView] = useState('list') // 'list' | 'form'
  const [editando, setEditando] = useState(null)
  const [currentStep, setCurrentStep] = useState(0)
  const [saving, setSaving] = useState(false)
  const [clienteSearch, setClienteSearch] = useState('')
  const [funcionarios, setFuncionarios] = useState([])
  const [funcForm, setFuncForm] = useState({ nome: '', codigo: '', cargo: 'caixa', permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }, empresaId: '' })
  const [editingFunc, setEditingFunc] = useState(null)
  const [formData, setFormData] = useState({ ...defaultFormData })
  const [clienteForm, setClienteForm] = useState({
    nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '', empresaId: ''
  })

  const fetchEmpresas = async () => {
    try {
      const res = await fetch('/api/empresas', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setEmpresas(data)
      }
    } catch (err) {
      console.error('Erro ao buscar empresas:', err)
    }
  }

  const fetchClientes = async () => {
    try {
      const res = await fetch('/api/clientes', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setClientes(data)
      }
    } catch (err) {
      console.error('Erro ao buscar clientes:', err)
    }
  }

  const fetchFuncionarios = async () => {
    try {
      const res = await fetch('/api/funcionarios', { headers: { Authorization: `Bearer ${token}` } })
      if (res.ok) setFuncionarios(await res.json())
    } catch (err) { console.error('Erro ao buscar funcionários:', err) }
  }

  useEffect(() => {
    fetchEmpresas()
    fetchClientes()
    fetchFuncionarios()
  }, [token])

  useEffect(() => {
    if (user?.role === 'empresa') {
      setView('list')
      setActiveTab('clientes')
      setEditando(null)
      setCurrentStep(0)
    }
  }, [user?.role])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const url = editando ? `/api/empresas/${editando.id}` : '/api/empresas'
      const method = editando ? 'PUT' : 'POST'
      
      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      })
      
      if (res.ok) {
        success(editando ? 'Empresa atualizada com sucesso' : 'Empresa cadastrada com sucesso', 3000)
        setView('list')
        setEditando(null)
        setFormData({ ...defaultFormData })
        setCurrentStep(0)
        fetchEmpresas()
      } else {
        const data = await res.json()
        success(data.error || 'Erro ao salvar empresa', 5000)
      }
    } catch (err) {
      console.error('Erro ao salvar empresa:', err)
      success('Erro ao salvar empresa', 5000)
    }
    setSaving(false)
  }

  const handleDelete = async (id) => {
    if (!confirm('Tem certeza que deseja excluir esta empresa?')) return
    
    try {
      const res = await fetch(`/api/empresas/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      
      if (res.ok) {
        success('Empresa excluída com sucesso', 3000)
        fetchEmpresas()
      } else {
        const data = await res.json()
        success(data.error || 'Erro ao excluir empresa', 5000)
      }
    } catch (err) {
      console.error('Erro ao excluir empresa:', err)
      success('Erro ao excluir empresa', 5000)
    }
  }

  const handleEdit = (empresa) => {
    setEditando(empresa)
    setFormData({
      nome: empresa.nome,
      tipoPessoa: empresa.tipoPessoa || 'juridica',
      slug: empresa.slug || '',
      cnpj: empresa.cnpj,
      email: empresa.email,
      telefone: empresa.telefone,
      login: empresa.login,
      senha: '',
      permissoes: empresa.permissoes || {},
      primaryColor: empresa.primaryColor || '#3b82f6',
      secondaryColor: empresa.secondaryColor || '#06b6d4',
      accentColor: empresa.accentColor || '#10b981',
      logoUrl: empresa.logoUrl || '',
      paginasPermitidas: empresa.paginasPermitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa']
    })
    setCurrentStep(0)
    setView('form')
  }

  const togglePermissao = (permissao) => {
    setFormData({
      ...formData,
      permissoes: {
        ...formData.permissoes,
        [permissao]: !formData.permissoes[permissao]
      }
    })
  }

  const togglePagina = (pagina) => {
    const current = formData.paginasPermitidas || []
    const updated = current.includes(pagina) ? current.filter(p => p !== pagina) : [...current, pagina]
    setFormData({ ...formData, paginasPermitidas: updated })
  }

  // ==================== CLIENTES CRUD ====================
  const handleClienteSubmit = async (e) => {
    e.preventDefault()
    try {
      const targetEmpresaId = user?.role === 'empresa' ? user.empresaId : clienteForm.empresaId
      if (!targetEmpresaId && user?.role === 'admin') return success('Selecione a empresa', 3000)
      
      const url = editando ? `/api/clientes/${editando.id}` : '/api/clientes'
      const method = editando ? 'PUT' : 'POST'
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ ...clienteForm, empresaId: targetEmpresaId })
      })
      if (res.ok) {
        success(editando ? 'Cliente atualizado com sucesso' : 'Cliente cadastrado com sucesso', 3000)
        setEditando(null)
        setClienteForm({ nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '', empresaId: '' })
        fetchClientes()
      } else {
        const data = await res.json()
        success(data.error || 'Erro ao salvar cliente', 5000)
      }
    } catch (err) {
      console.error('Erro ao salvar cliente:', err)
      success('Erro ao salvar cliente', 5000)
    }
  }

  const handleClienteDelete = async (id) => {
    if (!confirm('Tem certeza que deseja excluir este cliente?')) return
    try {
      const res = await fetch(`/api/clientes/${id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
      if (res.ok) {
        success('Cliente excluído com sucesso', 3000)
        fetchClientes()
      }
    } catch (err) {
      console.error('Erro ao excluir cliente:', err)
    }
  }

  const handleClienteEdit = (cliente) => {
    setEditando(cliente)
    setClienteForm({
      nome: cliente.nome,
      cpfCnpj: cliente.cpfCnpj || '',
      telefone: cliente.telefone || '',
      email: cliente.email || '',
      endereco: cliente.endereco || '',
      cidade: cliente.cidade || '',
      cep: cliente.cep || '',
      observacao: cliente.observacao || '',
      empresaId: cliente.empresaId || ''
    })
  }

  const handleClienteToggleAtivo = async (cliente) => {
    try {
      const res = await fetch(`/api/clientes/${cliente.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ ...cliente, ativo: !cliente.ativo })
      })
      if (res.ok) fetchClientes()
    } catch (err) {
      console.error('Erro ao alterar status:', err)
    }
  }

  const filteredClientes = clientes.filter(c => {
    const s = clienteSearch.toLowerCase()
    return c.nome.toLowerCase().includes(s) || (c.cpfCnpj || '').toLowerCase().includes(s) || (c.telefone || '').toLowerCase().includes(s)
  })

  const canGoNext = () => {
    const stepId = STEPS[currentStep]?.id
    if (stepId === 'info') return !!formData.nome
    if (stepId === 'access') return !!formData.login && (editando ? true : !!formData.senha)
    if (stepId === 'branding') return true
    if (stepId === 'pages') return (formData.paginasPermitidas || []).length > 0
    return true
  }

  const goNext = () => {
    if (!canGoNext()) return
    setCurrentStep(s => Math.min(s + 1, STEPS.length - 1))
  }

  const goBack = () => setCurrentStep(s => Math.max(s - 1, 0))

  const renderEmpresaStep = () => {
    const stepId = STEPS[currentStep]?.id
    if (stepId === 'info') {
      return (
        <div className="space-y-6">
          <div>
            <h2 className="text-xl font-semibold text-white">Informações</h2>
            <p className="text-sm text-gray-500">Dados básicos da empresa</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="md:col-span-2">
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Nome *</label>
              <input type="text" value={formData.nome} onChange={e => setFormData({ ...formData, nome: e.target.value })} className="input-field text-base" required />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Tipo de Pessoa</label>
              <select value={formData.tipoPessoa} onChange={e => setFormData({ ...formData, tipoPessoa: e.target.value })} className="input-field bg-gray-900 text-white">
                <option value="juridica" className="text-gray-900">Pessoa Jurídica</option>
                <option value="fisica" className="text-gray-900">Pessoa Física</option>
              </select>
            </div>
            {editando && (
              <div className="md:col-span-2">
                <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">URL da Empresa</label>
                <div className="flex items-center gap-2">
                  <div className="flex-1 px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-sm text-blue-400 font-mono truncate">
                    {window.location.origin}/{formData.slug}
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      const url = `${window.location.origin}/${formData.slug}`;
                      navigator.clipboard.writeText(url);
                      success('URL copiada!', 2000);
                    }}
                    className="px-3 py-2 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium transition-all"
                  >
                    Copiar Link
                  </button>
                </div>
                <p className="text-[10px] text-gray-500 mt-1">Slug: {formData.slug}</p>
              </div>
            )}
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">{formData.tipoPessoa === 'fisica' ? 'CPF' : 'CNPJ'}</label>
              <input type="text" value={formData.cnpj} onChange={e => setFormData({ ...formData, cnpj: e.target.value })} className="input-field" />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Telefone</label>
              <input type="text" value={formData.telefone} onChange={e => setFormData({ ...formData, telefone: e.target.value })} className="input-field" />
            </div>
            <div className="md:col-span-2">
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Email</label>
              <input type="email" value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })} className="input-field" />
            </div>
          </div>
        </div>
      )
    }

    if (stepId === 'access') {
      return (
        <div className="space-y-6">
          <div>
            <h2 className="text-xl font-semibold text-white">Acesso</h2>
            <p className="text-sm text-gray-500">Login, senha e permissões</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Login *</label>
              <input type="text" value={formData.login} onChange={e => setFormData({ ...formData, login: e.target.value })} className="input-field" required />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Senha {editando ? '(vazio = manter)' : '*'}</label>
              <input type="password" value={formData.senha} onChange={e => setFormData({ ...formData, senha: e.target.value })} className="input-field" required={!editando} />
            </div>
          </div>
          <div className="border-t border-white/5 pt-5">
            <p className="text-[11px] font-semibold text-gray-400 mb-3 uppercase tracking-wider">Permissões</p>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {[
                { key: 'dashboard', label: 'Dashboard' },
                { key: 'produtos', label: 'Produtos' },
                { key: 'categorias', label: 'Categorias' },
                { key: 'vendas', label: 'Vendas' },
                { key: 'caixa', label: 'Caixa' },
                { key: 'auditoria', label: 'Auditoria' }
              ].map(perm => (
                <button
                  key={perm.key}
                  type="button"
                  onClick={() => togglePermissao(perm.key)}
                  className={`px-4 py-3 rounded-xl border text-left transition-all ${formData.permissoes[perm.key] ? 'border-blue-500/20 bg-blue-500/10' : 'border-white/5 bg-white/[0.02] hover:bg-white/5'}`}
                >
                  <div className="flex items-center justify-between">
                    <span className={`text-sm font-medium ${formData.permissoes[perm.key] ? 'text-blue-400' : 'text-gray-400'}`}>{perm.label}</span>
                    {formData.permissoes[perm.key] ? <Check size={16} className="text-blue-400" /> : <X size={16} className="text-gray-600" />}
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )
    }

    if (stepId === 'branding') {
      return (
        <div className="space-y-6">
          <div>
            <h2 className="text-xl font-semibold text-white">Identidade</h2>
            <p className="text-sm text-gray-500">Whitelabel (cores e logo)</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Cor Primária</label>
              <div className="flex items-center gap-3">
                <input type="color" value={formData.primaryColor} onChange={e => setFormData({ ...formData, primaryColor: e.target.value })} className="w-12 h-12 rounded-xl cursor-pointer border-2 border-white/10 bg-transparent" />
                <input type="text" value={formData.primaryColor} onChange={e => setFormData({ ...formData, primaryColor: e.target.value })} className="input-field flex-1 font-mono" />
              </div>
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Cor Secundária</label>
              <div className="flex items-center gap-3">
                <input type="color" value={formData.secondaryColor} onChange={e => setFormData({ ...formData, secondaryColor: e.target.value })} className="w-12 h-12 rounded-xl cursor-pointer border-2 border-white/10 bg-transparent" />
                <input type="text" value={formData.secondaryColor} onChange={e => setFormData({ ...formData, secondaryColor: e.target.value })} className="input-field flex-1 font-mono" />
              </div>
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Destaque</label>
              <div className="flex items-center gap-3">
                <input type="color" value={formData.accentColor} onChange={e => setFormData({ ...formData, accentColor: e.target.value })} className="w-12 h-12 rounded-xl cursor-pointer border-2 border-white/10 bg-transparent" />
                <input type="text" value={formData.accentColor} onChange={e => setFormData({ ...formData, accentColor: e.target.value })} className="input-field flex-1 font-mono" />
              </div>
            </div>
          </div>
          <div>
            <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase tracking-wider">Logo da Empresa</label>
            <div className="flex flex-col sm:flex-row gap-4 items-start">
              <label className={`group relative flex flex-col items-center justify-center w-32 h-32 rounded-2xl border-2 border-dashed cursor-pointer transition-all ${formData.logoUrl ? 'border-blue-500/30 bg-blue-500/5' : 'border-white/10 bg-white/[0.02] hover:border-blue-500/30 hover:bg-blue-500/5'}`}>
                {formData.logoUrl ? (
                  <>
                    <img src={formData.logoUrl} alt="Logo" className="w-24 h-24 rounded-xl object-contain" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex' }} />
                    <div className="hidden w-24 h-24 items-center justify-center text-gray-500"><Building2 size={32} /></div>
                    <div className="absolute inset-0 bg-black/50 rounded-2xl flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                      <Upload size={20} className="text-white" />
                    </div>
                  </>
                ) : (
                  <>
                    <Upload size={24} className="text-gray-500 mb-2" />
                    <span className="text-[10px] text-gray-500 text-center px-2">Clique ou arraste</span>
                  </>
                )}
                <input type="file" accept="image/*" className="hidden" onChange={async (e) => {
                  const file = e.target.files?.[0]
                  if (!file) return
                  const reader = new FileReader()
                  reader.onload = async (ev) => {
                    const base64 = ev.target.result
                    try {
                      const res = await fetch('/api/upload-base64', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
                        body: JSON.stringify({ base64 })
                      })
                      if (res.ok) {
                        const data = await res.json()
                        setFormData({ ...formData, logoUrl: data.url })
                      }
                    } catch (err) { console.error('Erro upload logo:', err) }
                  }
                  reader.readAsDataURL(file)
                }} />
              </label>
              <div className="flex-1 space-y-3">
                <div>
                  <label className="block text-[10px] text-gray-600 mb-1">Ou cole uma URL:</label>
                  <input type="text" value={formData.logoUrl?.startsWith('/uploads/') ? '' : formData.logoUrl} onChange={e => setFormData({ ...formData, logoUrl: e.target.value })} className="input-field text-xs" placeholder="https://..." />
                </div>
                {formData.logoUrl && (
                  <button type="button" onClick={() => setFormData({ ...formData, logoUrl: '' })} className="text-[10px] text-red-400 hover:text-red-300 flex items-center gap-1">
                    <X size={10} /> Remover logo
                  </button>
                )}
              </div>
            </div>
          </div>
          <div className="rounded-2xl border border-white/5 p-4 bg-white/[0.02]">
            <p className="text-[11px] font-semibold text-gray-400 mb-3 uppercase tracking-wider">Preview</p>
            <div className="flex items-center gap-3">
              {formData.logoUrl ? (
                <img src={formData.logoUrl} alt="" className="w-10 h-10 rounded-lg object-contain" onError={(e) => { e.target.style.display = 'none' }} />
              ) : (
                <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: formData.primaryColor + '33' }}>
                  <Building2 size={18} style={{ color: formData.primaryColor }} />
                </div>
              )}
              <div>
                <p className="text-sm font-semibold text-white">{formData.nome || 'Nome da Empresa'}</p>
                <div className="flex gap-2 mt-1">
                  <span className="w-5 h-5 rounded" style={{ backgroundColor: formData.primaryColor }} />
                  <span className="w-5 h-5 rounded" style={{ backgroundColor: formData.secondaryColor }} />
                  <span className="w-5 h-5 rounded" style={{ backgroundColor: formData.accentColor }} />
                </div>
              </div>
            </div>
          </div>
        </div>
      )
    }

    // pages
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-xl font-semibold text-white">Páginas</h2>
          <p className="text-sm text-gray-500">Controle do que a empresa pode acessar</p>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { id: 'dashboard', label: 'Dashboard' },
            { id: 'terminais', label: 'Terminais' },
            { id: 'categorias', label: 'Categorias' },
            { id: 'produtos', label: 'Produtos' },
            { id: 'vendas', label: 'Vendas' },
            { id: 'caixa', label: 'Caixa' },
            { id: 'fechamento', label: 'Fechamento' },
            { id: 'empresas', label: 'Funcionarios/Clientes' },
            { id: 'config', label: 'Config' },
            { id: 'impressao', label: 'Impressão' }
          ].map(pg => {
            const active = (formData.paginasPermitidas || []).includes(pg.id)
            return (
              <button
                key={pg.id}
                type="button"
                onClick={() => togglePagina(pg.id)}
                className={`px-4 py-3 rounded-xl border text-left transition-all ${active ? 'border-blue-500/20 bg-blue-500/10' : 'border-white/5 bg-white/[0.02] hover:bg-white/5'}`}
              >
                <div className="flex items-center justify-between">
                  <span className={`text-sm font-medium ${active ? 'text-blue-400' : 'text-gray-400'}`}>{pg.label}</span>
                  {active ? <Check size={16} className="text-blue-400" /> : <X size={16} className="text-gray-600" />}
                </div>
              </button>
            )
          })}
        </div>
      </div>
    )
  }

  return (
    view === 'form' ? (
      <div className="min-h-[calc(100vh-140px)] -m-6">
        <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr]">
          <aside className="bg-gray-950/70 border-b lg:border-b-0 lg:border-r border-white/5">
            <div className="p-6 border-b border-white/5 flex items-center justify-between">
              <button
                onClick={() => { setView('list'); setEditando(null); setCurrentStep(0) }}
                className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors"
              >
                <ArrowLeft size={16} /> Voltar
              </button>
              <span className="text-[11px] text-gray-600 uppercase tracking-widest">Cadastro</span>
            </div>
            <div className="p-4 space-y-2">
              {STEPS.map((step, i) => (
                <button
                  key={step.id}
                  type="button"
                  onClick={() => setCurrentStep(i)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-2xl border transition-all ${i === currentStep ? 'border-blue-500/20 bg-blue-500/10' : 'border-white/5 bg-white/[0.02] hover:bg-white/5'}`}
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${i < currentStep ? 'bg-emerald-500/15' : i === currentStep ? 'bg-blue-500/15' : 'bg-white/5'}`}>
                    {i < currentStep ? <Check size={16} className="text-emerald-400" /> : <step.icon size={16} className={i === currentStep ? 'text-blue-400' : 'text-gray-500'} />}
                  </div>
                  <div className="text-left">
                    <p className={`text-sm font-medium ${i === currentStep ? 'text-blue-300' : 'text-gray-300'}`}>{step.label}</p>
                    <p className="text-[10px] text-gray-600">Etapa {i + 1} de {STEPS.length}</p>
                  </div>
                  {i === currentStep && <ChevronRight size={16} className="ml-auto text-blue-400" />}
                </button>
              ))}
            </div>
          </aside>

          <main className="p-6 lg:p-10">
            <div className="max-w-4xl">
              <div className="flex items-start justify-between mb-6">
                <div>
                  <h1 className="text-2xl font-bold text-white">{editando ? 'Editar Empresa' : 'Nova Empresa'}</h1>
                  <p className="text-sm text-gray-500">Configure acesso e whitelabel em poucos passos</p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => { setView('list'); setEditando(null); setCurrentStep(0) }}
                    className="btn-ghost"
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    onClick={() => (STEPS[currentStep]?.id === 'pages' ? null : goNext())}
                    className="btn-primary"
                    disabled={!canGoNext() || STEPS[currentStep]?.id === 'pages'}
                  >
                    Próximo
                  </button>
                </div>
              </div>

              <form onSubmit={handleSubmit} className="glass p-6 lg:p-8 border border-white/5 rounded-3xl">
                {renderEmpresaStep()}

                <div className="flex flex-col sm:flex-row gap-3 pt-8 mt-8 border-t border-white/5">
                  <button type="button" onClick={goBack} className="btn-ghost" disabled={currentStep === 0}>Voltar</button>
                  <div className="flex-1" />
                  {STEPS[currentStep]?.id !== 'pages' ? (
                    <button type="button" onClick={goNext} className="btn-primary" disabled={!canGoNext()}>Continuar</button>
                  ) : (
                    <button type="submit" className="btn-primary" disabled={!canGoNext() || saving}>
                      {saving ? (
                        <span className="flex items-center gap-2"><Loader2 size={16} className="animate-spin" /> Salvando...</span>
                      ) : (
                        <span className="flex items-center gap-2"><Save size={16} /> {editando ? 'Atualizar' : 'Cadastrar'}</span>
                      )}
                    </button>
                  )}
                </div>
              </form>
            </div>
          </main>
        </div>
      </div>
    ) : (
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
              <Building2 size={24} className="text-blue-400" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Cadastro White-label</h3>
              <p className="text-xs text-gray-400">Gerencie empresas white-label</p>
            </div>
          </div>
          {user?.role !== 'empresa' && (
            <button
              onClick={() => {
                setEditando(null); setFormData({ ...defaultFormData }); setCurrentStep(0); setView('form')
              }}
              className="btn-primary flex items-center gap-2"
            >
              <Plus size={16} />
              Nova Empresa
            </button>
          )}
        </div>

        {/* Conteúdo da aba Empresas */}
        {empresas.length === 0 ? (
          <div className="glass p-12 text-center">
            <Building2 size={48} className="mx-auto text-gray-600 mb-3" />
            <p className="text-gray-400">Nenhuma empresa cadastrada</p>
            <p className="text-gray-600 text-sm mt-1">Cadastre empresas para liberar acesso ao sistema</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {empresas.map(empresa => (
              <div key={empresa.id} className={`glass p-5 border transition-all ${empresa.ativo !== false ? 'border-white/5 hover:border-blue-500/20' : 'border-red-500/10 opacity-60'}`}>
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    {empresa.logoUrl ? (
                      <img src={empresa.logoUrl} alt="" className="w-10 h-10 rounded-lg object-contain" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex' }} />
                    ) : null}
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${empresa.logoUrl ? 'hidden' : 'bg-blue-500/20'}`}>
                      <Building2 size={20} className="text-blue-400" />
                    </div>
                    <div>
                      <p className="font-semibold text-white">{empresa.nome}</p>
                      <div className="flex items-center gap-2">
                        <p className="text-xs text-gray-400">{empresa.login}</p>
                        <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${empresa.ativo !== false ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                          {empresa.ativo !== false ? 'Ativo' : 'Inativo'}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button onClick={() => handleEdit(empresa)} className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-blue-400 transition-colors" title="Editar">
                      <Edit2 size={14} />
                    </button>
                    <button onClick={() => handleDelete(empresa.id)} className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-red-400 transition-colors" title="Excluir">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
                {/* Branding Preview */}
                {(empresa.primaryColor || empresa.logoUrl) && (
                  <div className="flex items-center gap-2 mb-3 p-2 rounded-lg bg-black/20">
                    <div className="flex gap-1">
                      <div className="w-5 h-5 rounded" style={{ backgroundColor: empresa.primaryColor || '#3b82f6' }} />
                      <div className="w-5 h-5 rounded" style={{ backgroundColor: empresa.secondaryColor || '#06b6d4' }} />
                      <div className="w-5 h-5 rounded" style={{ backgroundColor: empresa.accentColor || '#10b981' }} />
                    </div>
                    <span className="text-[10px] text-gray-500">Whitelabel</span>
                  </div>
                )}
                {/* URL da Empresa */}
                {empresa.slug && (
                  <div className="flex items-center gap-2 mb-3">
                    <div className="flex-1 px-2 py-1.5 bg-white/5 rounded-lg text-xs text-blue-400 font-mono truncate">
                      {window.location.origin}/{empresa.slug}
                    </div>
                    <button
                      onClick={() => {
                        navigator.clipboard.writeText(`${window.location.origin}/${empresa.slug}`);
                        success('URL copiada!', 2000);
                      }}
                      className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-blue-400 transition-colors"
                      title="Copiar link"
                    >
                      <Globe size={14} />
                    </button>
                  </div>
                )}
                {empresa.cnpj && <div className="text-xs text-gray-400 mb-2"><span className="font-medium">CNPJ:</span> {empresa.cnpj}</div>}
                {empresa.email && <div className="text-xs text-gray-400 mb-2"><span className="font-medium">Email:</span> {empresa.email}</div>}
                {empresa.telefone && <div className="text-xs text-gray-400 mb-3"><span className="font-medium">Telefone:</span> {empresa.telefone}</div>}
                <div className="border-t border-white/5 pt-3">
                  <p className="text-xs text-gray-400 mb-2 flex items-center gap-1"><Shield size={12} /> Páginas:</p>
                  <div className="flex flex-wrap gap-1.5">
                    {(empresa.paginasPermitidas || []).map(pg => (
                      <span key={pg} className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-[10px] rounded capitalize">{pg}</span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    )
  )
}
