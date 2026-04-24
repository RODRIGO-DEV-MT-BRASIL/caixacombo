import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { Package, Plus, Pencil, Trash2, Search, X, Loader2 } from 'lucide-react'

export default function Produtos() {
  const { token } = useAuth()
  const { socket } = useSocket()
  const [produtos, setProdutos] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ nome: '', precoVenda: '', categoria: '', estoque: '' })

  const fetchProdutos = async () => {
    try {
      const res = await fetch('/api/produtos', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      setProdutos(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProdutos() }, [])

  useEffect(() => {
    if (!socket) return
    const handlers = {
      produto_added: (p) => setProdutos(prev => [...prev, p]),
      produto_updated: (p) => setProdutos(prev => prev.map(x => x.id === p.id ? p : x)),
      produto_deleted: ({ id }) => setProdutos(prev => prev.filter(x => x.id !== id)),
      produtos_synced: (list) => setProdutos(list),
    }
    Object.entries(handlers).forEach(([event, handler]) => socket.on(event, handler))
    return () => Object.entries(handlers).forEach(([event, handler]) => socket.off(event, handler))
  }, [socket])

  const handleSubmit = async (e) => {
    e.preventDefault()
    const body = {
      nome: form.nome,
      precoVenda: parseFloat(form.precoVenda),
      categoria: form.categoria,
      estoque: parseInt(form.estoque) || 0,
    }

    if (editing) {
      await fetch(`/api/produtos/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    } else {
      await fetch('/api/produtos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body)
      })
    }
    setShowModal(false)
    setEditing(null)
    setForm({ nome: '', precoVenda: '', categoria: '', estoque: '' })
    fetchProdutos()
  }

  const handleDelete = async (id) => {
    if (!confirm('Excluir este produto?')) return
    await fetch(`/api/produtos/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` }
    })
    fetchProdutos()
  }

  const openEdit = (produto) => {
    setEditing(produto)
    setForm({
      nome: produto.nome || '',
      precoVenda: produto.precoVenda?.toString() || '',
      categoria: produto.categoria || '',
      estoque: produto.estoque?.toString() || '',
    })
    setShowModal(true)
  }

  const filtered = produtos.filter(p =>
    (p.nome || '').toLowerCase().includes(search.toLowerCase()) ||
    (p.categoria || '').toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="relative flex-1 w-full sm:max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="input-field pl-9 py-2.5 text-sm"
            placeholder="Buscar produto..."
          />
        </div>
        <button onClick={() => { setEditing(null); setForm({ nome: '', precoVenda: '', categoria: '', estoque: '' }); setShowModal(true) }} className="btn-primary flex items-center gap-2 text-sm">
          <Plus size={16} /> Novo Produto
        </button>
      </div>

      {/* Table */}
      {loading ? (
        <div className="glass p-12 text-center">
          <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
          <p className="text-gray-400">Carregando produtos...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass p-12 text-center">
          <Package size={48} className="mx-auto text-gray-600 mb-3" />
          <p className="text-gray-400">Nenhum produto encontrado</p>
        </div>
      ) : (
        <div className="glass overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/5">
                  <th className="text-left px-5 py-3 text-gray-400 font-medium">Produto</th>
                  <th className="text-left px-5 py-3 text-gray-400 font-medium">Categoria</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Preço</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Estoque</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(p => (
                  <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="px-5 py-3 text-white font-medium">{p.nome}</td>
                    <td className="px-5 py-3 text-gray-400">{p.categoria || '-'}</td>
                    <td className="px-5 py-3 text-emerald-400 font-mono text-right">
                      R$ {(p.precoVenda || 0).toFixed(2)}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <span className={`px-2 py-0.5 rounded-lg text-xs font-medium ${
                        p.estoque > 10 ? 'bg-emerald-500/10 text-emerald-400' :
                        p.estoque > 0 ? 'bg-amber-500/10 text-amber-400' :
                        'bg-red-500/10 text-red-400'
                      }`}>
                        {p.estoque || 0}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openEdit(p)} className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-blue-400 transition-all">
                          <Pencil size={14} />
                        </button>
                        <button onClick={() => handleDelete(p.id)} className="p-1.5 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-400 transition-all">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6" onClick={() => setShowModal(false)}>
          <div className="glass p-6 w-full max-w-md glow-blue" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-white">{editing ? 'Editar Produto' : 'Novo Produto'}</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-white"><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Nome</label>
                <input type="text" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} className="input-field" required />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Preço (R$)</label>
                  <input type="number" step="0.01" value={form.precoVenda} onChange={e => setForm({ ...form, precoVenda: e.target.value })} className="input-field" required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1">Estoque</label>
                  <input type="number" value={form.estoque} onChange={e => setForm({ ...form, estoque: e.target.value })} className="input-field" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Categoria</label>
                <input type="text" value={form.categoria} onChange={e => setForm({ ...form, categoria: e.target.value })} className="input-field" />
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
