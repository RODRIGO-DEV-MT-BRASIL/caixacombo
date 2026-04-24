import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { Package, Plus, Pencil, Trash2, Search, X, Loader2, Image as ImageIcon, Barcode, ChevronDown } from 'lucide-react'

export default function Produtos() {
  const { token } = useAuth()
  const { socket } = useSocket()
  const [produtos, setProdutos] = useState([])
  const [categorias, setCategorias] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ nome: '', descricao: '', imagem: '', precoVenda: '', categoria: '', estoque: '', codigoBarras: '' })

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

  const fetchCategorias = async () => {
    try {
      const res = await fetch('/api/categorias', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      setCategorias(data)
    } catch (err) {
      console.error(err)
    }
  }

  useEffect(() => { fetchProdutos(); fetchCategorias() }, [])

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
      descricao: form.descricao,
      imagem: form.imagem,
      precoVenda: parseFloat(form.precoVenda),
      categoria: form.categoria,
      estoque: parseInt(form.estoque) || 0,
      codigoBarras: form.codigoBarras || undefined,
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
    setForm({ nome: '', descricao: '', imagem: '', precoVenda: '', categoria: '', estoque: '', codigoBarras: '' })
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
      descricao: produto.descricao || '',
      imagem: produto.imagem || '',
      precoVenda: produto.precoVenda?.toString() || '',
      categoria: produto.categoria || '',
      estoque: produto.estoque?.toString() || '',
      codigoBarras: produto.codigoBarras || '',
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
        <button onClick={() => { setEditing(null); setForm({ nome: '', descricao: '', imagem: '', precoVenda: '', categoria: '', estoque: '', codigoBarras: '' }); setShowModal(true) }} className="btn-primary flex items-center gap-2 text-sm">
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
                  <th className="text-left px-5 py-3 text-gray-400 font-medium">Cód. Barras</th>
                  <th className="text-left px-5 py-3 text-gray-400 font-medium">Categoria</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Preço</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Estoque</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(p => (
                  <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        {p.imagem ? (
                          <img src={p.imagem} alt={p.nome} className="w-10 h-10 rounded-lg object-cover bg-white/5" />
                        ) : (
                          <div className="w-10 h-10 rounded-lg bg-white/5 flex items-center justify-center">
                            <ImageIcon size={16} className="text-gray-600" />
                          </div>
                        )}
                        <div>
                          <p className="text-white font-medium">{p.nome}</p>
                          {p.descricao && <p className="text-xs text-gray-500 truncate max-w-[200px]">{p.descricao}</p>}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-gray-400 font-mono text-xs">
                      {p.codigoBarras || '-'}
                    </td>
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
          <div className="glass p-6 w-full max-w-lg glow-blue max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-white">{editing ? 'Editar Produto' : 'Novo Produto'}</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-white"><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Nome</label>
                <input type="text" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} className="input-field" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Descrição</label>
                <textarea value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} className="input-field resize-none" rows="2" placeholder="Descrição do produto..." />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">URL da Imagem</label>
                <div className="relative">
                  <ImageIcon size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
                  <input type="url" value={form.imagem} onChange={e => setForm({ ...form, imagem: e.target.value })} className="input-field pl-9" placeholder="https://..." />
                </div>
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
                <div className="relative">
                  <select value={form.categoria} onChange={e => setForm({ ...form, categoria: e.target.value })} className="input-field appearance-none cursor-pointer">
                    <option value="">Selecione uma categoria</option>
                    {categorias.map(cat => (
                      <option key={cat.id} value={cat.nome}>{cat.nome}</option>
                    ))}
                  </select>
                  <ChevronDown size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">Código de Barras</label>
                <div className="relative">
                  <Barcode size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
                  <input type="text" value={form.codigoBarras} onChange={e => setForm({ ...form, codigoBarras: e.target.value })} className="input-field pl-9" placeholder="Deixe vazio para gerar automaticamente" />
                </div>
                <p className="text-xs text-gray-500 mt-1">Se vazio, será gerado automaticamente (EAN-13)</p>
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
