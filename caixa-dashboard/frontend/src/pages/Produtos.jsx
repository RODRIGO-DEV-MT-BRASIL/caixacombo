import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToastContext } from '../contexts/ToastContext'
import { apiUrl } from '../utils/api'
import ProdutoModal from '../components/ProdutoModal'
import { Package, Plus, Pencil, Trash2, Search, Image as ImageIcon, ShoppingCart } from 'lucide-react'

export default function Produtos() {
  const { token } = useAuth()
  const { socket, vendas } = useSocket()
  const toast = useToastContext()
  const [produtos, setProdutos] = useState([])
  const [categorias, setCategorias] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingProduto, setEditingProduto] = useState(null)
  const [vendasLocais, setVendasLocais] = useState([])

  const fetchProdutos = async () => {
    try {
      const res = await fetch(apiUrl('/api/produtos'), {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      console.log('Produtos recebidos:', data)
      setProdutos(data)
    } catch (err) {
      console.error('Erro ao buscar produtos:', err)
    } finally {
      setLoading(false)
    }
  }

  const fetchCategorias = async () => {
    try {
      const res = await fetch(apiUrl('/api/categorias'), {
        headers: { Authorization: `Bearer ${token}` }
      })
      const data = await res.json()
      console.log('Categorias recebidas:', data)
      setCategorias(data)
    } catch (err) {
      console.error('Erro ao buscar categorias:', err)
    }
  }

  const fetchVendas = async () => {
    try {
      const res = await fetch(apiUrl('/api/vendas'), {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setVendasLocais(data)
      }
    } catch (err) {
      console.error('Erro ao buscar vendas:', err)
    }
  }

  useEffect(() => {
    fetchProdutos()
    fetchCategorias()
    fetchVendas()
  }, [])

  useEffect(() => {
    if (!socket) return
    const handlers = {
      produto_added: (p) => setProdutos(prev => [...prev, p]),
      produto_updated: (p) => setProdutos(prev => prev.map(x => x.id === p.id ? p : x)),
      produto_deleted: ({ id }) => setProdutos(prev => prev.filter(x => x.id !== id)),
      produtos_synced: (list) => setProdutos(list),
      venda_added: () => fetchVendas(),
    }
    Object.entries(handlers).forEach(([event, handler]) => socket.on(event, handler))
    return () => Object.entries(handlers).forEach(([event, handler]) => socket.off(event, handler))
  }, [socket])

  // Calcular quantidade vendida por produto
  const getQuantidadeVendida = (produtoId) => {
    const todasVendas = [...vendasLocais, ...(vendas || [])]
    return todasVendas.reduce((total, venda) => {
      if (venda.itens && Array.isArray(venda.itens)) {
        const itensProduto = venda.itens.filter(item => item.produtoId === produtoId)
        return total + itensProduto.reduce((sum, item) => sum + (item.quantidade || 0), 0)
      }
      return total
    }, 0)
  }

  // Buscar nome da categoria pelo ID
  const getCategoriaNome = (categoriaId) => {
    if (!categoriaId) return '-'
    const categoria = categorias.find(c => c.id == categoriaId)
    return categoria ? categoria.nome : '-'
  }

  const handleSave = async (produtoData) => {
    try {
      if (editingProduto) {
        await fetch(apiUrl(`/api/produtos/${editingProduto.id}`), {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify(produtoData)
        })
        toast.success('Produto atualizado com sucesso!')
      } else {
        await fetch(apiUrl('/api/produtos'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify(produtoData)
        })
        toast.success('Produto criado com sucesso!')
      }
      fetchProdutos()
    } catch (error) {
      toast.error('Erro ao salvar produto')
      throw error
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Excluir este produto?')) return
    try {
      await fetch(apiUrl(`/api/produtos/${id}`), {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      toast.success('Produto excluído com sucesso!')
      fetchProdutos()
    } catch (error) {
      toast.error('Erro ao excluir produto')
    }
  }

  const openNew = () => {
    setEditingProduto(null)
    setShowModal(true)
  }

  const openEdit = (produto) => {
    setEditingProduto(produto)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditingProduto(null)
  }

  const filtered = produtos.filter(p =>
    (p.nome || '').toLowerCase().includes(search.toLowerCase()) ||
    getCategoriaNome(p.categoriaId).toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
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
        <button onClick={openNew} className="btn-primary flex items-center gap-2 text-sm">
          <Plus size={16} /> Novo Produto
        </button>
      </div>

      {loading ? (
        <div className="glass p-12 text-center">
          <div className="animate-spin mx-auto mb-3 w-8 h-8 border-2 border-blue-400 border-t-transparent rounded-full" />
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
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Estoque (un)</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Vendido</th>
                  <th className="text-right px-5 py-3 text-gray-400 font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(p => {
                  const qtdVendida = getQuantidadeVendida(p.id)
                  return (
                    <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          {p.imagem ? (
                            <img src={apiUrl(p.imagem)} alt={p.nome} className="w-10 h-10 rounded-lg object-cover bg-white/5" />
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
                      <td className="px-5 py-3 text-gray-400">{getCategoriaNome(p.categoriaId)}</td>
                      <td className="px-5 py-3 text-emerald-400 font-mono text-right">
                        R$ {(p.preco || 0).toFixed(2)}
                      </td>
                      <td className="px-5 py-3 text-right">
                        <span className={`px-2 py-0.5 rounded-lg text-xs font-medium ${
                          p.estoque > 10 ? 'bg-emerald-500/10 text-emerald-400' :
                          p.estoque > 0 ? 'bg-amber-500/10 text-amber-400' :
                          'bg-red-500/10 text-red-400'
                        }`}>
                          {p.estoque || 0} <span className="text-gray-500">{p.unidade || 'un'}</span>
                        </span>
                      </td>
                      <td className="px-5 py-3 text-right">
                        {qtdVendida > 0 && (
                          <span className="flex items-center justify-end gap-1 text-blue-400">
                            <ShoppingCart size={12} />
                            <span className="text-xs font-medium">{qtdVendida}</span>
                          </span>
                        )}
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
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <ProdutoModal
        isOpen={showModal}
        onClose={closeModal}
        onSave={handleSave}
        produto={editingProduto}
        categorias={categorias}
        token={token}
      />
    </div>
  )
}
