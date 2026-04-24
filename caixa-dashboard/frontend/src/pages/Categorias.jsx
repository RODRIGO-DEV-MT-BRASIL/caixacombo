import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { Tags, Plus, Pencil, Trash2, Search, X, Loader2 } from 'lucide-react'

export default function Categorias() {
  const { token } = useAuth()
  const { socket } = useSocket()
  const [categorias, setCategorias] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ nome: '', descricao: '' })

  const fetchCategorias = async () => {
    try {
      const res = await fetch('/api/categorias', {
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

  useEffect(() => { fetchCategorias() }, [])

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

    if (editing) {
      await fetch(`/api/categorias/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    } else {
      await fetch('/api/categorias', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    }
    setShowModal(false)
    setEditing(null)
    setForm({ nome: '', descricao: '' })
    fetchCategorias()
  }

  const handleDelete = async (id) => {
    if (!confirm('Excluir esta categoria?')) return
    await fetch(`/api/categorias/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchCategorias()
  }

  const openEdit = (cat) => {
    setEditing(cat)
    setForm({ nome: cat.nome || '', descricao: cat.descricao || '' })
    setShowModal(true)
  }

  const filtered = categorias.filter(c =>
    (c.nome || '').toLowerCase().includes(search.toLowerCase())
  )

  const colors = ['from-blue-500 to-cyan-400', 'from-purple-500 to-pink-400', 'from-emerald-500 to-teal-400', 'from-amber-500 to-orange-400', 'from-red-500 to-rose-400']

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="relative flex-1 w-full sm:max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input type="text" value={search} onChange={e => setSearch(e.target.value)} className="input-field pl-9 py-2.5 text-sm" placeholder="Buscar categoria..." />
        </div>
        <button onClick={() => { setEditing(null); setForm({ nome: '', descricao: '' }); setShowModal(true) }} className="btn-primary flex items-center gap-2 text-sm">
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
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
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
    </div>
  )
}
