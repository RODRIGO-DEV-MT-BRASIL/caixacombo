import React, { useState, useEffect } from 'react'
import { Plus, Search, Edit, Trash2, UserCog, Users, Building2 } from 'lucide-react'

export default function ClientesFuncionarios() {
  const [activeTab, setActiveTab] = useState('clientes')
  const [clientes, setClientes] = useState([])
  const [funcionarios, setFuncionarios] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [formData, setFormData] = useState({
    nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: ''
  })

  const token = localStorage.getItem('token')

  const fetchClientes = async () => {
    try {
      const res = await fetch('/api/clientes', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      setClientes(data.data || data)
    } catch (error) {
      console.error('Erro ao buscar clientes:', error)
    }
  }

  const fetchFuncionarios = async () => {
    try {
      const res = await fetch('/api/funcionarios', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      setFuncionarios(data)
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
    try {
      const url = activeTab === 'clientes' ? '/api/clientes' : '/api/funcionarios'
      const method = editingItem ? 'PUT' : 'POST'
      const endpoint = editingItem ? `${url}/${editingItem.id}` : url

      const res = await fetch(endpoint, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      })

      if (res.ok) {
        setShowModal(false)
        setEditingItem(null)
        setFormData({ nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '' })
        if (activeTab === 'clientes') fetchClientes()
        else fetchFuncionarios()
      }
    } catch (error) {
      console.error('Erro ao salvar:', error)
    }
  }

  const handleEdit = (item) => {
    setEditingItem(item)
    setFormData({
      nome: item.nome,
      cpfCnpj: item.cpfCnpj || '',
      telefone: item.telefone || '',
      email: item.email || '',
      endereco: item.endereco || '',
      cidade: item.cidade || '',
      cep: item.cep || '',
      observacao: item.observacao || ''
    })
    setShowModal(true)
  }

  const handleDelete = async (id) => {
    if (!confirm('Tem certeza que deseja excluir?')) return
    try {
      const url = activeTab === 'clientes' ? '/api/clientes' : '/api/funcionarios'
      const res = await fetch(`${url}/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        if (activeTab === 'clientes') fetchClientes()
        else fetchFuncionarios()
      }
    } catch (error) {
      console.error('Erro ao excluir:', error)
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
            setFormData({ nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '' })
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
          className="w-full pl-10 pr-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-blue-500"
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
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="glass rounded-xl p-6 w-full max-w-md mx-4">
            <h2 className="text-xl font-semibold text-white mb-4">
              {editingItem ? 'Editar' : activeTab === 'clientes' ? 'Novo Cliente' : 'Novo Funcionário'}
            </h2>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Nome *</label>
                <input
                  type="text"
                  value={formData.nome}
                  onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">CPF/CNPJ</label>
                <input
                  type="text"
                  value={formData.cpfCnpj}
                  onChange={(e) => setFormData({ ...formData, cpfCnpj: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Telefone</label>
                <input
                  type="text"
                  value={formData.telefone}
                  onChange={(e) => setFormData({ ...formData, telefone: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Endereço</label>
                <input
                  type="text"
                  value={formData.endereco}
                  onChange={(e) => setFormData({ ...formData, endereco: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Cidade</label>
                <input
                  type="text"
                  value={formData.cidade}
                  onChange={(e) => setFormData({ ...formData, cidade: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">CEP</label>
                <input
                  type="text"
                  value={formData.cep}
                  onChange={(e) => setFormData({ ...formData, cep: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Observação</label>
                <textarea
                  value={formData.observacao}
                  onChange={(e) => setFormData({ ...formData, observacao: e.target.value })}
                  className="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white focus:outline-none focus:border-blue-500"
                  rows={2}
                />
              </div>
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => {
                    setShowModal(false)
                    setEditingItem(null)
                    setFormData({ nome: '', cpfCnpj: '', telefone: '', email: '', endereco: '', cidade: '', cep: '', observacao: '' })
                  }}
                  className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
                >
                  Salvar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
