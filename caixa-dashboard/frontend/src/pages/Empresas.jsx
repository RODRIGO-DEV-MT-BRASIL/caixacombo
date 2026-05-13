import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useToastContext } from '../contexts/ToastContext'
import { Building2, Plus, Edit2, Trash2, Check, X, Shield, Users, Phone, Mail, MapPin, Search } from 'lucide-react'
import { createClientForm, createCompanyForm } from '../constants/forms'

export default function Empresas() {
  const { token } = useAuth()
  const { success } = useToastContext()
  const [activeTab, setActiveTab] = useState('empresas')
  const [empresas, setEmpresas] = useState([])
  const [clientes, setClientes] = useState([])
  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState(null)
  const [clienteSearch, setClienteSearch] = useState('')
  /** @type {[import('../types/entities').EmpresaForm, Function]} */
  const [formData, setFormData] = useState(() => createCompanyForm())
  /** @type {[import('../types/entities').ClienteForm, Function]} */
  const [clienteForm, setClienteForm] = useState(() => createClientForm())

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

  useEffect(() => {
    fetchEmpresas()
    fetchClientes()
  }, [token])

  const handleSubmit = async (e) => {
    e.preventDefault()
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
        setModalOpen(false)
        setEditando(null)
        setFormData(createCompanyForm())
        fetchEmpresas()
      } else {
        const data = await res.json()
        success(data.error || 'Erro ao salvar empresa', 5000)
      }
    } catch (err) {
      console.error('Erro ao salvar empresa:', err)
      success('Erro ao salvar empresa', 5000)
    }
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
    setFormData(createCompanyForm({
      nome: empresa.nome,
      cnpj: empresa.cnpj || '',
      email: empresa.email || '',
      telefone: empresa.telefone || '',
      login: empresa.login,
      senha: '',
      permissoes: empresa.permissoes
    }))
    setModalOpen(true)
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

  // ==================== CLIENTES CRUD ====================
  const handleClienteSubmit = async (e) => {
    e.preventDefault()
    try {
      const url = editando ? `/api/clientes/${editando.id}` : '/api/clientes'
      const method = editando ? 'PUT' : 'POST'
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(clienteForm)
      })
      if (res.ok) {
        success(editando ? 'Cliente atualizado com sucesso' : 'Cliente cadastrado com sucesso', 3000)
        setModalOpen(false)
        setEditando(null)
        setClienteForm(createClientForm())
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
    setClienteForm(createClientForm({
      nome: cliente.nome,
      cpfCnpj: cliente.cpfCnpj || '',
      telefone: cliente.telefone || '',
      email: cliente.email || '',
      endereco: cliente.endereco || '',
      cidade: cliente.cidade || '',
      cep: cliente.cep || '',
      observacao: cliente.observacao || ''
    }))
    setModalOpen(true)
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

  return (
    <div className="space-y-6">
      {/* Header com abas */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
            <Building2 size={24} className="text-blue-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">Empresas & Clientes</h3>
            <p className="text-xs text-gray-400">Gerencie empresas, acessos e clientes</p>
          </div>
        </div>
        <button
          onClick={() => {
            if (activeTab === 'empresas') {
              setModalOpen(true); setEditando(null); setFormData(createCompanyForm())
            } else {
              setModalOpen(true); setEditando(null); setClienteForm(createClientForm())
            }
          }}
          className="btn-primary flex items-center gap-2"
        >
          <Plus size={16} />
          {activeTab === 'empresas' ? 'Nova Empresa' : 'Novo Cliente'}
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setActiveTab('empresas')}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${activeTab === 'empresas' ? 'bg-blue-600 text-white' : 'bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/20'}`}
        >
          <Building2 size={16} /> Empresas
        </button>
        <button
          onClick={() => setActiveTab('clientes')}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${activeTab === 'clientes' ? 'bg-emerald-600 text-white' : 'bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/20'}`}
        >
          <Users size={16} /> Clientes
        </button>
      </div>

      {/* Conteúdo da aba Empresas */}
      {activeTab === 'empresas' && (
        empresas.length === 0 ? (
          <div className="glass p-12 text-center">
            <Building2 size={48} className="mx-auto text-gray-600 mb-3" />
            <p className="text-gray-400">Nenhuma empresa cadastrada</p>
            <p className="text-gray-600 text-sm mt-1">Cadastre empresas para liberar acesso ao sistema</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {empresas.map(empresa => (
              <div key={empresa.id} className="glass p-5 border border-white/5 hover:border-blue-500/20 transition-all">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-blue-500/20 flex items-center justify-center">
                      <Building2 size={20} className="text-blue-400" />
                    </div>
                    <div>
                      <p className="font-semibold text-white">{empresa.nome}</p>
                      <p className="text-xs text-gray-400">{empresa.login}</p>
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
                {empresa.cnpj && <div className="text-xs text-gray-400 mb-2"><span className="font-medium">CNPJ:</span> {empresa.cnpj}</div>}
                {empresa.email && <div className="text-xs text-gray-400 mb-2"><span className="font-medium">Email:</span> {empresa.email}</div>}
                {empresa.telefone && <div className="text-xs text-gray-400 mb-3"><span className="font-medium">Telefone:</span> {empresa.telefone}</div>}
                <div className="border-t border-white/5 pt-3">
                  <p className="text-xs text-gray-400 mb-2 flex items-center gap-1"><Shield size={12} /> Permissões:</p>
                  <div className="flex flex-wrap gap-1.5">
                    {empresa.permissoes?.dashboard && <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs rounded">Dashboard</span>}
                    {empresa.permissoes?.produtos && <span className="px-2 py-0.5 bg-green-500/20 text-green-400 text-xs rounded">Produtos</span>}
                    {empresa.permissoes?.categorias && <span className="px-2 py-0.5 bg-purple-500/20 text-purple-400 text-xs rounded">Categorias</span>}
                    {empresa.permissoes?.vendas && <span className="px-2 py-0.5 bg-orange-500/20 text-orange-400 text-xs rounded">Vendas</span>}
                    {empresa.permissoes?.caixa && <span className="px-2 py-0.5 bg-cyan-500/20 text-cyan-400 text-xs rounded">Caixa</span>}
                    {empresa.permissoes?.auditoria && <span className="px-2 py-0.5 bg-pink-500/20 text-pink-400 text-xs rounded">Auditoria</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {/* Conteúdo da aba Clientes */}
      {activeTab === 'clientes' && (
        <>
          {/* Busca */}
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input
              type="text"
              placeholder="Buscar por nome, CPF/CNPJ ou telefone..."
              value={clienteSearch}
              onChange={e => setClienteSearch(e.target.value)}
              className="input-field pl-10"
            />
          </div>

          {filteredClientes.length === 0 ? (
            <div className="glass p-12 text-center">
              <Users size={48} className="mx-auto text-gray-600 mb-3" />
              <p className="text-gray-400">Nenhum cliente cadastrado</p>
              <p className="text-gray-600 text-sm mt-1">Cadastre clientes para sincronizar com os terminais</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredClientes.map(cliente => (
                <div key={cliente.id} className={`glass p-5 border transition-all ${cliente.ativo ? 'border-white/5 hover:border-emerald-500/20' : 'border-white/5 opacity-60'}`}>
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${cliente.ativo ? 'bg-emerald-500/20' : 'bg-gray-500/20'}`}>
                        <Users size={20} className={cliente.ativo ? 'text-emerald-400' : 'text-gray-400'} />
                      </div>
                      <div>
                        <p className="font-semibold text-white">{cliente.nome}</p>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${cliente.ativo ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                          {cliente.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <button onClick={() => handleClienteEdit(cliente)} className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-emerald-400 transition-colors" title="Editar">
                        <Edit2 size={14} />
                      </button>
                      <button onClick={() => handleClienteToggleAtivo(cliente)} className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-amber-400 transition-colors" title={cliente.ativo ? 'Desativar' : 'Ativar'}>
                        {cliente.ativo ? <X size={14} /> : <Check size={14} />}
                      </button>
                      <button onClick={() => handleClienteDelete(cliente.id)} className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-red-400 transition-colors" title="Excluir">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  {cliente.cpfCnpj && <div className="text-xs text-gray-400 mb-1"><span className="font-medium">CPF/CNPJ:</span> {cliente.cpfCnpj}</div>}
                  {cliente.telefone && <div className="text-xs text-gray-400 mb-1 flex items-center gap-1"><Phone size={10} /> {cliente.telefone}</div>}
                  {cliente.email && <div className="text-xs text-gray-400 mb-1 flex items-center gap-1"><Mail size={10} /> {cliente.email}</div>}
                  {(cliente.cidade || cliente.endereco) && <div className="text-xs text-gray-400 mb-1 flex items-center gap-1"><MapPin size={10} /> {[cliente.endereco, cliente.cidade].filter(Boolean).join(', ')}</div>}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Modal de Cadastro/Edição - Empresas */}
      {modalOpen && activeTab === 'empresas' && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setModalOpen(false)}>
          <div className="glass p-6 w-full max-w-2xl glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-2">
              <Building2 size={20} className="text-blue-400" />
              {editando ? 'Editar Empresa' : 'Nova Empresa'}
            </h3>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Nome da Empresa *</label>
                  <input type="text" value={formData.nome} onChange={e => setFormData({ ...formData, nome: e.target.value })} className="input-field" required />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">CNPJ</label>
                  <input type="text" value={formData.cnpj} onChange={e => setFormData({ ...formData, cnpj: e.target.value })} className="input-field" placeholder="00.000.000/0000-00" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Email</label>
                  <input type="email" value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })} className="input-field" placeholder="email@empresa.com" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Telefone</label>
                  <input type="text" value={formData.telefone} onChange={e => setFormData({ ...formData, telefone: e.target.value })} className="input-field" placeholder="(00) 00000-0000" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Login de Acesso *</label>
                  <input type="text" value={formData.login} onChange={e => setFormData({ ...formData, login: e.target.value })} className="input-field" required />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Senha de Acesso {editando ? '(deixe vazio para manter)' : '*'}</label>
                  <input type="password" value={formData.senha} onChange={e => setFormData({ ...formData, senha: e.target.value })} className="input-field" required={!editando} />
                </div>
              </div>

              <div className="border-t border-white/5 pt-4 mt-4">
                <p className="text-sm text-gray-400 mb-3 flex items-center gap-2"><Shield size={16} /> Permissões de Acesso:</p>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {[
                    { key: 'dashboard', label: 'Dashboard', color: 'blue' },
                    { key: 'produtos', label: 'Produtos', color: 'green' },
                    { key: 'categorias', label: 'Categorias', color: 'purple' },
                    { key: 'vendas', label: 'Vendas', color: 'orange' },
                    { key: 'caixa', label: 'Caixa', color: 'cyan' },
                    { key: 'auditoria', label: 'Auditoria', color: 'pink' }
                  ].map(perm => (
                    <button key={perm.key} type="button" onClick={() => togglePermissao(perm.key)} className={`p-3 rounded-lg border transition-all flex items-center gap-2 ${formData.permissoes[perm.key] ? `bg-${perm.color}-500/20 border-${perm.color}-500/30 text-${perm.color}-400` : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10'}`}>
                      {formData.permissoes[perm.key] ? <Check size={16} /> : <X size={16} />}
                      <span className="text-sm">{perm.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-ghost flex-1">Cancelar</button>
                <button type="submit" className="btn-primary flex-1">{editando ? 'Atualizar' : 'Cadastrar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de Cadastro/Edição - Clientes */}
      {modalOpen && activeTab === 'clientes' && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setModalOpen(false)}>
          <div className="glass p-6 w-full max-w-2xl glow-blue" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-2">
              <Users size={20} className="text-emerald-400" />
              {editando ? 'Editar Cliente' : 'Novo Cliente'}
            </h3>

            <form onSubmit={handleClienteSubmit} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Nome *</label>
                  <input type="text" value={clienteForm.nome} onChange={e => setClienteForm({ ...clienteForm, nome: e.target.value })} className="input-field" required />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">CPF/CNPJ</label>
                  <input type="text" value={clienteForm.cpfCnpj} onChange={e => setClienteForm({ ...clienteForm, cpfCnpj: e.target.value })} className="input-field" placeholder="000.000.000-00" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Telefone</label>
                  <input type="text" value={clienteForm.telefone} onChange={e => setClienteForm({ ...clienteForm, telefone: e.target.value })} className="input-field" placeholder="(00) 00000-0000" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Email</label>
                  <input type="email" value={clienteForm.email} onChange={e => setClienteForm({ ...clienteForm, email: e.target.value })} className="input-field" placeholder="email@exemplo.com" />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm text-gray-400 mb-1">Endereço</label>
                  <input type="text" value={clienteForm.endereco} onChange={e => setClienteForm({ ...clienteForm, endereco: e.target.value })} className="input-field" placeholder="Rua, número, complemento" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Cidade</label>
                  <input type="text" value={clienteForm.cidade} onChange={e => setClienteForm({ ...clienteForm, cidade: e.target.value })} className="input-field" />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">CEP</label>
                  <input type="text" value={clienteForm.cep} onChange={e => setClienteForm({ ...clienteForm, cep: e.target.value })} className="input-field" placeholder="00000-000" />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm text-gray-400 mb-1">Observação</label>
                  <textarea value={clienteForm.observacao} onChange={e => setClienteForm({ ...clienteForm, observacao: e.target.value })} className="input-field min-h-[80px]" placeholder="Observações sobre o cliente..." />
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-ghost flex-1">Cancelar</button>
                <button type="submit" className="btn-primary flex-1">{editando ? 'Atualizar' : 'Cadastrar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
