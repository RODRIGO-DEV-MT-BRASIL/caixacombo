import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../components/Toast'
import { Building2, Plus, Edit2, Trash2, Check, X, Key, Shield } from 'lucide-react'

export default function Empresas() {
  const { token } = useAuth()
  const { success } = useToast()
  const [empresas, setEmpresas] = useState([])
  const [modalOpen, setModalOpen] = useState(false)
  const [editando, setEditando] = useState(null)
  const [formData, setFormData] = useState({
    nome: '',
    cnpj: '',
    email: '',
    telefone: '',
    login: '',
    senha: '',
    permissoes: {
      dashboard: false,
      produtos: false,
      categorias: false,
      vendas: false,
      caixa: false,
      auditoria: false
    }
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

  useEffect(() => {
    fetchEmpresas()
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
        setFormData({
          nome: '',
          cnpj: '',
          email: '',
          telefone: '',
          login: '',
          senha: '',
          permissoes: {
            dashboard: false,
            produtos: false,
            categorias: false,
            vendas: false,
            caixa: false,
            auditoria: false
          }
        })
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
    setFormData({
      nome: empresa.nome,
      cnpj: empresa.cnpj || '',
      email: empresa.email || '',
      telefone: empresa.telefone || '',
      login: empresa.login,
      senha: '',
      permissoes: empresa.permissoes || {
        dashboard: false,
        produtos: false,
        categorias: false,
        vendas: false,
        caixa: false,
        auditoria: false
      }
    })
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

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
            <Building2 size={24} className="text-blue-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">Empresas</h3>
            <p className="text-xs text-gray-400">Gerencie empresas e seus acessos</p>
          </div>
        </div>
        <button
          onClick={() => { setModalOpen(true); setEditando(null); setFormData({ nome: '', cnpj: '', email: '', telefone: '', login: '', senha: '', permissoes: { dashboard: false, produtos: false, categorias: false, vendas: false, caixa: false, auditoria: false } }) }}
          className="btn-primary flex items-center gap-2"
        >
          <Plus size={16} />
          Nova Empresa
        </button>
      </div>

      {empresas.length === 0 ? (
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
                  <button
                    onClick={() => handleEdit(empresa)}
                    className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-blue-400 transition-colors"
                    title="Editar"
                  >
                    <Edit2 size={14} />
                  </button>
                  <button
                    onClick={() => handleDelete(empresa.id)}
                    className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-red-400 transition-colors"
                    title="Excluir"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>

              {empresa.cnpj && (
                <div className="text-xs text-gray-400 mb-2">
                  <span className="font-medium">CNPJ:</span> {empresa.cnpj}
                </div>
              )}
              {empresa.email && (
                <div className="text-xs text-gray-400 mb-2">
                  <span className="font-medium">Email:</span> {empresa.email}
                </div>
              )}
              {empresa.telefone && (
                <div className="text-xs text-gray-400 mb-3">
                  <span className="font-medium">Telefone:</span> {empresa.telefone}
                </div>
              )}

              <div className="border-t border-white/5 pt-3">
                <p className="text-xs text-gray-400 mb-2 flex items-center gap-1">
                  <Shield size={12} />
                  Permissões:
                </p>
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
      )}

      {/* Modal de Cadastro/Edição */}
      {modalOpen && (
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
                  <input
                    type="text"
                    value={formData.nome}
                    onChange={e => setFormData({ ...formData, nome: e.target.value })}
                    className="input-field"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">CNPJ</label>
                  <input
                    type="text"
                    value={formData.cnpj}
                    onChange={e => setFormData({ ...formData, cnpj: e.target.value })}
                    className="input-field"
                    placeholder="00.000.000/0000-00"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Email</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                    className="input-field"
                    placeholder="email@empresa.com"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Telefone</label>
                  <input
                    type="text"
                    value={formData.telefone}
                    onChange={e => setFormData({ ...formData, telefone: e.target.value })}
                    className="input-field"
                    placeholder="(00) 00000-0000"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Login de Acesso *</label>
                  <input
                    type="text"
                    value={formData.login}
                    onChange={e => setFormData({ ...formData, login: e.target.value })}
                    className="input-field"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Senha de Acesso {editando ? '(deixe vazio para manter)' : '*'}</label>
                  <input
                    type="password"
                    value={formData.senha}
                    onChange={e => setFormData({ ...formData, senha: e.target.value })}
                    className="input-field"
                    required={!editando}
                  />
                </div>
              </div>

              <div className="border-t border-white/5 pt-4 mt-4">
                <p className="text-sm text-gray-400 mb-3 flex items-center gap-2">
                  <Shield size={16} />
                  Permissões de Acesso:
                </p>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {[
                    { key: 'dashboard', label: 'Dashboard', color: 'blue' },
                    { key: 'produtos', label: 'Produtos', color: 'green' },
                    { key: 'categorias', label: 'Categorias', color: 'purple' },
                    { key: 'vendas', label: 'Vendas', color: 'orange' },
                    { key: 'caixa', label: 'Caixa', color: 'cyan' },
                    { key: 'auditoria', label: 'Auditoria', color: 'pink' }
                  ].map(perm => (
                    <button
                      key={perm.key}
                      type="button"
                      onClick={() => togglePermissao(perm.key)}
                      className={`p-3 rounded-lg border transition-all flex items-center gap-2 ${
                        formData.permissoes[perm.key]
                          ? `bg-${perm.color}-500/20 border-${perm.color}-500/30 text-${perm.color}-400`
                          : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10'
                      }`}
                    >
                      {formData.permissoes[perm.key] ? <Check size={16} /> : <X size={16} />}
                      <span className="text-sm">{perm.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-ghost flex-1">Cancelar</button>
                <button type="submit" className="btn-primary flex-1">
                  {editando ? 'Atualizar' : 'Cadastrar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
