import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { apiUrl } from '../utils/api'
import { Tags, Plus, Pencil, Trash2, Search, X, Loader2 } from 'lucide-react'
import { useToast } from '../components/Toast'

export default function Categorias() {
  const { user, token } = useAuth()
  const { socket } = useSocket()
  const toast = useToast()
  const [categorias, setCategorias] = useState([])
  const [empresas, setEmpresas] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [form, setForm] = useState({ nome: '', descricao: '', empresaId: '' })
  const [filterEmpresa, setFilterEmpresa] = useState('')

  const fetchCategorias = async () => {
    try {
      const res = await fetch(apiUrl('/api/categorias'), {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      setCategorias(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const fetchEmpresas = async () => {
    if (user?.role === 'admin') {
      try {
        const res = await fetch(apiUrl('/api/empresas'), {
          headers: { Authorization: `Bearer ${token}` }
        })
        const data = await res.json()
        setEmpresas(data)
      } catch (err) {
        console.error(err)
      }
    }
  }

  useEffect(() => {
    fetchCategorias()
    fetchEmpresas()
  }, [])

  useEffect(() => {
    if (!socket) return
    const handlers = {
      categoria_added: (c) => setCategorias(prev => [...prev, c]),
      categoria_updated: (c) => setCategorias(prev => prev.map(x => x.id === c.id ? c : x)),
      categoria_deleted: ({ id }) => setCategorias(prev => prev.filter(x => x.id !== id)),
      categorias_synced: (list) => setCategorias(list),
    }
    Object.entries(handlers).forEach(([event, handler]) => socket.on(event, handler))
    return () => Object.entries(handlers).forEach(([event, handler]) => socket.off(event, handler))
  }, [socket])

  const handleSubmit = async (e) => {
    e.preventDefault()
    const body = { nome: form.nome, descricao: form.descricao }
    if (user?.role === 'admin') {
      if (!form.empresaId) {
        toast.warning('Selecione uma empresa para cadastrar a categoria')
        return
      }
      body.empresaId = form.empresaId
    }
    // Empresa usa automaticamente sua propria empresaId (enviado pelo backend via token)

    if (editing) {
      await fetch(apiUrl(`/api/categorias/${editing.id}`), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    } else {
      await fetch(apiUrl('/api/categorias'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    }
    setShowModal(false)
    setEditing(null)
    setForm({ nome: '', descricao: '', empresaId: '' })
    fetchCategorias()
  }

  const handleDelete = async (id) => {
    const categoria = categorias.find(c => c.id === id)
    setConfirmDelete(categoria)
  }

  const confirmDeleteExecute = async () => {
    if (!confirmDelete) return
    await fetch(apiUrl(`/api/categorias/${confirmDelete.id}`), {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` }
    })
    setConfirmDelete(null)
    fetchCategorias()
  }

  const openEdit = (cat) => {
    setEditing(cat)
    setForm({ nome: cat.nome || '', descricao: cat.descricao || '', empresaId: cat.empresaId || '' })
    setShowModal(true)
  }

  const filteredByEmpresa = filterEmpresa ? categorias.filter(c => c.empresaId === filterEmpresa) : categorias
  const filtered = filteredByEmpresa.filter(c =>
    (c.nome || '').toLowerCase().includes(search.toLowerCase())
  )

  const colors = ['from-blue-500 to-cyan-400', 'from-purple-500 to-pink-400', 'from-emerald-500 to-teal-400', 'from-amber-500 to-orange-400', 'from-red-500 to-rose-400']

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="relative flex-1 w-full sm:max-w-xs">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="input-field pl-9 py-2.5 text-sm" placeholder="Buscar categoria..." />
          </div>
          {user?.role === 'admin' && (
            <select
              value={filterEmpresa}
              onChange={(e) => setFilterEmpresa(e.target.value)}
              className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
            >
              <option value="">Todas as Empresas</option>
              {empresas.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.nome}</option>
              ))}
            </select>
          )}
        </div>
        <button onClick={() => { setEditing(null); setForm({ nome: '', descricao: '', empresaId: '' }); setShowModal(true) }} className="btn-primary flex items-center gap-2 text-sm">
          <Plus size={16} /> Nova Categoria
        </button>
      </div>

      {loading ? (
        <div className="glass p-12 text-center">
          <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
          <p className="text-gray-400">Carregando categorias...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass p-12 text-center">
          <Tags size={48} className="mx-auto text-gray-600 mb-3" />
          <p className="text-gray-400">Nenhuma categoria encontrada</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((cat, i) => (
            <div key={cat.id} className="glass glass-hover p-5 group">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${colors[i % colors.length]} flex items-center justify-center shadow-lg`}>
                    <Tags size={18} className="text-white" />
                  </div>
                  <div>
                    <p className="font-semibold text-white text-sm">{cat.nome}</p>
                    {cat.descricao && <p className="text-xs text-gray-500 mt-0.5">{cat.descricao}</p>}
                  </div>
                </div>
                <div className="flex gap-1">
                  <button onClick={() => openEdit(cat)} className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-blue-400 transition-all">
                    <Pencil size={14} />
                  </button>
                  <button onClick={() => handleDelete(cat.id)} className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-400 transition-all">
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setShowModal(false)}>
          <div className="glass p-6 w-full max-w-md glow-blue" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-white">{editing ? 'Editar Categoria' : 'Nova Categoria'}</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-white"><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              {user?.role === 'admin' && (
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Empresa</label>
                  <select
                    value={form.empresaId}
                    onChange={e => setForm({ ...form, empresaId: e.target.value })}
                    className="input-field"
                  >
                    <option value="">Selecione uma empresa</option>
                    {empresas.map(emp => (
                      <option key={emp.id} value={emp.id}>{emp.nome}</option>
                    ))}
                  </select>
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Nome</label>
                <input type="text" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} className="input-field" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Descrição</label>
                <input type="text" value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} className="input-field" />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowModal(false)} className="btn-ghost flex-1">Cancelar</button>
                <button type="submit" className="btn-primary flex-1">{editing ? 'Salvar' : 'Criar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-gray-900 border border-gray-700 rounded-xl w-full max-w-sm">
            <div className="p-6 border-b border-gray-700">
              <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                <Trash2 className="text-red-400" size={20} />
                Confirmar Exclusão
              </h3>
            </div>
            <div className="p-6">
              <p className="text-gray-300 mb-2">Deseja excluir esta categoria?</p>
              <p className="text-white font-medium">{confirmDelete.nome}</p>
            </div>
            <div className="p-4 border-t border-gray-700 flex gap-3 justify-end">
              <button onClick={() => setConfirmDelete(null)} className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors">
                Cancelar
              </button>
              <button onClick={confirmDeleteExecute} className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg transition-colors">
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
