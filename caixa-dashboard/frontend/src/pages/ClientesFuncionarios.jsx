import React, { useState, useEffect } from 'react'
import { Plus, Search, Edit, Trash2, UserCog, Users, Building2, X, Loader2 } from 'lucide-react'

export default function ClientesFuncionarios() {
  const [activeTab, setActiveTab] = useState('clientes')
  const [modalTab, setModalTab] = useState('dados')
  const [clientes, setClientes] = useState([])
  const [funcionarios, setFuncionarios] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)
  const [formData, setFormData] = useState({
    nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '',
    codigo: '', senha: '', cargo: 'caixa', permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
  })

  const fetchClientes = async () => {
    try {
      const t = sessionStorage.getItem('token')
      const res = await fetch('/api/clientes', {
        headers: { Authorization: `Bearer ${t}` }
      })
      if (res.ok) {
        const data = await res.json()
        const list = Array.isArray(data) ? data : data.data || []
        setClientes(list)
      }
    } catch (error) {
      console.error('Erro ao buscar clientes:', error)
    }
  }

  const fetchFuncionarios = async () => {
    try {
      const t = sessionStorage.getItem('token')
      const res = await fetch('/api/funcionarios', {
        headers: { Authorization: `Bearer ${t}` }
      })
      if (res.ok) {
        const data = await res.json()
        if (Array.isArray(data)) setFuncionarios(data)
      }
    } catch (error) {
      console.error('Erro ao buscar funcionários:', error)
    }
  }

  useEffect(() => {
    fetchClientes()
    fetchFuncionarios()
  }, [])

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    setSaveError(null)

    const currentToken = sessionStorage.getItem('token')
    if (!currentToken) {
      setSaveError('Sessão expirada. Faça login novamente.')
      setSaving(false)
      return
    }

    try {
      const url = activeTab === 'clientes' ? '/api/clientes' : '/api/funcionarios'
      const method = editingItem ? 'PUT' : 'POST'
      const endpoint = editingItem ? `${url}/${editingItem.id}` : url

      let body = formData
      if (activeTab === 'funcionarios') {
        body = {
          nome: formData.nome,
          cpfCnpj: formData.cpfCnpj,
          telefone: formData.telefone,
          email: formData.email,
          codigo: formData.codigo,
          senha: formData.senha,
          cargo: formData.cargo,
          permissoes: formData.permissoes,
          ativo: editingItem ? editingItem.ativo : true
        }
      }

      const res = await fetch(endpoint, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${currentToken}`
        },
        body: JSON.stringify(body)
      })

      if (res.ok) {
        setShowModal(false)
        setEditingItem(null)
        setSaveError(null)
        setFormData({
          nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '',
          codigo: '', senha: '', cargo: 'caixa', permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
        })
        if (activeTab === 'clientes') fetchClientes()
        else fetchFuncionarios()
      } else {
        const errData = await res.json().catch(() => ({}))
        setSaveError(errData?.error || `Erro ao salvar (${res.status})`)
      }
    } catch (error) {
      setSaveError('Erro de conexão. Verifique sua internet.')
      console.error('Erro ao salvar:', error)
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (item) => {
    setEditingItem(item)
    setModalTab('dados')
    if (activeTab === 'funcionarios') {
      setFormData({
        nome: item.nome,
        cpfCnpj: item.cpfCnpj || '',
        telefone: item.telefone || '',
        email: item.email || '',
        endereco: '',
        cidade: '',
        cep: '',
        observacao: '',
        codigo: item.codigo || '',
        senha: '',
        cargo: item.cargo || 'caixa',
        permissoes: item.permissoes || { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
      })
    } else {
      setFormData({
        nome: item.nome,
        cpfCnpj: item.cpfCnpj || '',
        telefone: item.telefone || '',
        email: item.email || '',
        endereco: item.endereco || '',
        cidade: item.cidade || '',
        cep: item.cep || '',
        observacao: item.observacao || '',
        codigo: '',
        cargo: 'caixa',
        permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
      })
    }
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    // Open custom confirmation modal instead of browser confirm
    const item = (activeTab === 'clientes' ? clientes : funcionarios).find(i => i.id === id)
    setConfirmDelete({ id, nome: item?.nome || 'Item' })
  }

  const confirmDeleteExecute = async (id) => {
    try {
      const t = sessionStorage.getItem('token')
      const url = activeTab === 'clientes' ? '/api/clientes' : '/api/funcionarios'
      const res = await fetch(`${url}/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${t}` }
      })
      if (res.ok) {
        if (activeTab === 'clientes') fetchClientes()
        else fetchFuncionarios()
      }
    } catch (error) {
      console.error('Erro ao excluir:', error)
    } finally {
      setConfirmDelete(null)
    }
  }

  const filteredItems = activeTab === 'clientes' 
    ? clientes.filter(c => c.nome.toLowerCase().includes(searchTerm.toLowerCase()))
    : funcionarios.filter(f => f.nome.toLowerCase().includes(searchTerm.toLowerCase()))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Funcionários/Clientes</h1>
          <p className="text-gray-400">Gerencie seus funcionários e clientes</p>
        </div>
        <button
          onClick={() => {
            setEditingItem(null)
            setModalTab('dados')
            setFormData({
              nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '',
              codigo: '', cargo: 'caixa', permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
            })
            setShowModal(true)
          }}
          className="btn-primary flex items-center gap-2"
        >
          <Plus size={16} />
          {activeTab === 'clientes' ? 'Novo Cliente' : 'Novo Funcionário'}
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setActiveTab('clientes')}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${activeTab === 'clientes' ? 'bg-emerald-600 text-white' : 'bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/20'}`}
        >
          <Users size={16} /> Clientes
        </button>
        <button
          onClick={() => setActiveTab('funcionarios')}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 text-sm transition-colors ${activeTab === 'funcionarios' ? 'bg-amber-600 text-white' : 'bg-amber-600/20 hover:bg-amber-600/30 text-amber-400 border border-amber-500/20'}`}
        >
          <UserCog size={16} /> Funcionários
        </button>
      </div>

      {/* Search */}
      <div className="relative">
        <Search size={20} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          type="text"
          placeholder={`Buscar ${activeTab === 'clientes' ? 'clientes' : 'funcionários'}...`}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="input-field pl-10 py-2.5 text-sm"
        />
      </div>

      {/* List */}
      <div className="glass rounded-xl overflow-hidden">
        <table className="w-full">
          <thead className="bg-white/5">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">Nome</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">CPF/CNPJ</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">Telefone</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">Email</th>
              <th className="px-4 py-3 text-right text-sm font-medium text-gray-400">Ações</th>
            </tr>
          </thead>
          <tbody>
            {filteredItems.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-12 text-center text-gray-500">
                  {activeTab === 'clientes' ? 'Nenhum cliente cadastrado' : 'Nenhum funcionário cadastrado'}
                </td>
              </tr>
            ) : (
              filteredItems.map((item) => (
                <tr key={item.id} className="border-t border-white/5 hover:bg-white/5">
                  <td className="px-4 py-3 text-white">{item.nome}</td>
                  <td className="px-4 py-3 text-gray-400">{item.cpfCnpj || '-'}</td>
                  <td className="px-4 py-3 text-gray-400">{item.telefone || '-'}</td>
                  <td className="px-4 py-3 text-gray-400">{item.email || '-'}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleEdit(item)}
                      className="text-blue-400 hover:text-blue-300 mr-2"
                    >
                      <Edit size={16} />
                    </button>
                    <button
                      onClick={() => handleDelete(item.id)}
                      className="text-red-400 hover:text-red-300"
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-2">
          <div className="bg-gray-900 border border-gray-700 rounded-xl w-full max-w-lg flex flex-col max-h-[95vh]">
            <div className="flex items-center justify-between px-3 py-2.5 border-b border-gray-700 shrink-0">
              <div className="flex items-center gap-2">
                <UserCog className="w-5 h-5 text-blue-400" />
                <h2 className="text-lg font-semibold text-white">
                  {editingItem ? 'Editar' : 'Novo'} {activeTab === 'clientes' ? 'Cliente' : 'Funcionário'}
                </h2>
              </div>
              <button onClick={() => { setShowModal(false); setEditingItem(null); setModalTab('dados') }} className="text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSave} className="flex flex-col min-h-0">
              {activeTab === 'funcionarios' && (
                <div className="flex gap-2 px-3 py-2 border-b border-gray-700 shrink-0">
                  <button
                    type="button"
                    onClick={() => setModalTab('dados')}
                    className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${modalTab === 'dados' ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
                  >
                    Dados
                  </button>
                  <button
                    type="button"
                    onClick={() => setModalTab('permissoes')}
                    className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${modalTab === 'permissoes' ? 'bg-blue-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'}`}
                  >
                    Permissões
                  </button>
                </div>
              )}

              <div className="overflow-y-auto px-3 py-2 space-y-1.5">
                {modalTab === 'dados' ? (
                  <>
                    <div>
                      <label className="block text-xs font-medium text-gray-300 mb-0.5">Nome *</label>
                      <input
                        type="text"
                        value={formData.nome}
                        onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                        className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                        required
                      />
                    </div>
                    
                    {activeTab === 'funcionarios' ? (
                      <>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Código *</label>
                            <input
                              type="text"
                              value={formData.codigo}
                              onChange={(e) => setFormData({ ...formData, codigo: e.target.value })}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Senha *</label>
                            <input
                              type="password"
                              value={formData.senha}
                              onChange={(e) => setFormData({ ...formData, senha: e.target.value })}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                              placeholder="4-6 dígitos"
                              maxLength={6}
                              required={!editingItem}
                            />
                          </div>
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-300 mb-0.5">Cargo</label>
                          <select
                            value={formData.cargo}
                            onChange={(e) => setFormData({ ...formData, cargo: e.target.value })}
                            className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                          >
                            <option value="caixa">Caixa</option>
                            <option value="gerente">Gerente</option>
                            <option value="supervisor">Supervisor</option>
                            <option value="admin">Admin</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-300 mb-0.5">CPF/CNPJ</label>
                          <input
                            type="text"
                            value={formData.cpfCnpj}
                            onChange={(e) => {
                              let value = e.target.value.replace(/\D/g, '')
                              if (value.length > 14) value = value.slice(0, 14)
                              if (value.length > 11) {
                                if (value.length > 12) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5, 8)}/${value.slice(8, 12)}-${value.slice(12)}`
                                else if (value.length > 8) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5, 8)}/${value.slice(8)}`
                                else if (value.length > 5) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5)}`
                                else if (value.length > 2) value = `${value.slice(0, 2)}.${value.slice(2)}`
                              } else if (value.length > 0) {
                                if (value.length > 9) value = `${value.slice(0, 3)}.${value.slice(3, 6)}.${value.slice(6, 9)}-${value.slice(9)}`
                                else if (value.length > 6) value = `${value.slice(0, 3)}.${value.slice(3, 6)}.${value.slice(6)}`
                                else if (value.length > 3) value = `${value.slice(0, 3)}.${value.slice(3)}`
                              }
                              setFormData({ ...formData, cpfCnpj: value })
                            }}
                            className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            placeholder="000.000.000-00 ou 00.000.000/0000-00"
                          />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Telefone</label>
                            <input
                              type="text"
                              value={formData.telefone}
                              onChange={(e) => {
                                let value = e.target.value.replace(/\D/g, '')
                                if (value.length > 11) value = value.slice(0, 11)
                                if (value.length > 0) {
                                  if (value.length <= 2) value = `(${value}`
                                  else if (value.length <= 7) value = `(${value.slice(0, 2)}) ${value.slice(2)}`
                                  else value = `(${value.slice(0, 2)}) ${value.slice(2, 7)}-${value.slice(7)}`
                                }
                                setFormData({ ...formData, telefone: value })
                              }}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                              placeholder="(11) 99999-9999"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Email</label>
                            <input
                              type="email"
                              value={formData.email}
                              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            />
                          </div>
                        </div>
                      </>
                    ) : (
                      <>
                        <div>
                          <label className="block text-xs font-medium text-gray-300 mb-0.5">CPF/CNPJ</label>
                          <input
                            type="text"
                            value={formData.cpfCnpj}
                            onChange={(e) => {
                              let value = e.target.value.replace(/\D/g, '')
                              if (value.length > 14) value = value.slice(0, 14)
                              if (value.length > 11) {
                                if (value.length > 12) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5, 8)}/${value.slice(8, 12)}-${value.slice(12)}`
                                else if (value.length > 8) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5, 8)}/${value.slice(8)}`
                                else if (value.length > 5) value = `${value.slice(0, 2)}.${value.slice(2, 5)}.${value.slice(5)}`
                                else if (value.length > 2) value = `${value.slice(0, 2)}.${value.slice(2)}`
                              } else if (value.length > 0) {
                                if (value.length > 9) value = `${value.slice(0, 3)}.${value.slice(3, 6)}.${value.slice(6, 9)}-${value.slice(9)}`
                                else if (value.length > 6) value = `${value.slice(0, 3)}.${value.slice(3, 6)}.${value.slice(6)}`
                                else if (value.length > 3) value = `${value.slice(0, 3)}.${value.slice(3)}`
                              }
                              setFormData({ ...formData, cpfCnpj: value })
                            }}
                            className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            placeholder="000.000.000-00 ou 00.000.000/0000-00"
                          />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Telefone</label>
                            <input
                              type="text"
                              value={formData.telefone}
                              onChange={(e) => {
                                let value = e.target.value.replace(/\D/g, '')
                                if (value.length > 11) value = value.slice(0, 11)
                                if (value.length > 0) {
                                  if (value.length <= 2) value = `(${value}`
                                  else if (value.length <= 7) value = `(${value.slice(0, 2)}) ${value.slice(2)}`
                                  else value = `(${value.slice(0, 2)}) ${value.slice(2, 7)}-${value.slice(7)}`
                                }
                                setFormData({ ...formData, telefone: value })
                              }}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                              placeholder="(11) 99999-9999"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Email</label>
                            <input
                              type="email"
                              value={formData.email}
                              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            />
                          </div>
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-300 mb-0.5">Endereço</label>
                          <input
                            type="text"
                            value={formData.endereco}
                            onChange={(e) => setFormData({ ...formData, endereco: e.target.value })}
                            className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                          />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">Cidade</label>
                            <input
                              type="text"
                              value={formData.cidade}
                              onChange={(e) => setFormData({ ...formData, cidade: e.target.value })}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-300 mb-0.5">CEP</label>
                            <input
                              type="text"
                              value={formData.cep}
                              onChange={(e) => {
                                let value = e.target.value.replace(/\D/g, '')
                                if (value.length > 8) value = value.slice(0, 8)
                                if (value.length > 5) value = `${value.slice(0, 5)}-${value.slice(5)}`
                                setFormData({ ...formData, cep: value })
                              }}
                              className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                              placeholder="00000-000"
                            />
                          </div>
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-300 mb-0.5">Observação</label>
                          <textarea
                            value={formData.observacao}
                            onChange={(e) => setFormData({ ...formData, observacao: e.target.value })}
                            className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500 resize-none"
                            rows={2}
                          />
                        </div>
                      </>
                    )}
                  </>
                ) : (
                  <div className="space-y-0.5">
                    {[
                      { key: 'vendas', label: 'Vendas' },
                      { key: 'caixa', label: 'Caixa' },
                      { key: 'produtos', label: 'Produtos' },
                      { key: 'categorias', label: 'Categorias' },
                      { key: 'relatorios', label: 'Relatórios' },
                      { key: 'desconto', label: 'Desconto' },
                      { key: 'cancelar_venda', label: 'Cancelar Venda' },
                      { key: 'operacoes_caixa', label: 'Operações de Caixa' },
                    ].map(({ key, label }) => (
                      <div key={key} className="flex items-center justify-between p-2 bg-gray-800/50 rounded-lg">
                        <label className="text-sm text-gray-300">{label}</label>
                        <input
                          type="checkbox"
                          checked={formData.permissoes[key]}
                          onChange={(e) => setFormData({ ...formData, permissoes: { ...formData.permissoes, [key]: e.target.checked } })}
                          className="w-4 h-4 rounded"
                        />
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {saveError && (
                <div className="px-3 py-2 bg-red-900/30 border-t border-red-800/50">
                  <p className="text-sm text-red-400">{saveError}</p>
                </div>
              )}
              <div className="flex gap-2 px-3 py-2 border-t border-gray-700 shrink-0">
                <button
                  type="button"
                  onClick={() => {
                    setShowModal(false)
                    setEditingItem(null)
                    setModalTab('dados')
                    setFormData({
                      nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '',
                      codigo: '', cargo: 'caixa', permissoes: { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true }
                    })
                  }}
                  className="flex-1 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white rounded-lg text-sm transition-colors flex items-center justify-center gap-2"
                >
                  {saving && <Loader2 size={14} className="animate-spin" />}
                  {saving ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* Delete confirmation modal */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-2">
          <div className="bg-gray-900 border border-gray-700 rounded-xl w-full max-w-sm">
            <div className="px-4 py-3 border-b border-gray-700">
              <h3 className="text-lg font-semibold text-white">Confirmar exclusão</h3>
            </div>
            <div className="px-4 py-4">
              <p className="text-sm text-gray-300">
                Tem certeza que deseja excluir <strong className="text-white">{confirmDelete.nome}</strong>?
              </p>
            </div>
            <div className="flex gap-2 px-4 py-3 border-t border-gray-700 justify-end">
              <button
                onClick={() => setConfirmDelete(null)}
                className="px-4 py-1.5 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={() => confirmDeleteExecute(confirmDelete.id)}
                className="px-4 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm transition-colors"
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
