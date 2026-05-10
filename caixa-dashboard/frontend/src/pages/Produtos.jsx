import { useState, useEffect, useRef } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useSocket } from '../contexts/SocketContext'
import { useToastContext } from '../contexts/ToastContext'
import { apiUrl } from '../utils/api'
import ProdutoModal from '../components/ProdutoModal'
import { Package, Plus, Pencil, Trash2, Search, Image as ImageIcon, ShoppingCart, Download, Upload, FileText, Loader2, RefreshCw } from 'lucide-react'
import Papa from 'papaparse'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'

export default function Produtos() {
  const { token } = useAuth()
  const { socket, vendas } = useSocket()
  const toast = useToastContext()
  const [produtos, setProdutos] = useState([])
  const [categorias, setCategorias] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [categoriaFiltro, setCategoriaFiltro] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editingProduto, setEditingProduto] = useState(null)
  const [vendasLocais, setVendasLocais] = useState([])
  const [importing, setImporting] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const fileInputRef = useRef(null)

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
        const res = await fetch(apiUrl(`/api/produtos/${editingProduto.id}`), {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify(produtoData)
        })
        if (!res.ok) {
          const err = await res.json().catch(() => ({}))
          throw new Error(err.error || 'Erro ao atualizar produto')
        }
        toast.success('Produto atualizado com sucesso!')
      } else {
        const res = await fetch(apiUrl('/api/produtos'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify(produtoData)
        })
        if (!res.ok) {
          const err = await res.json().catch(() => ({}))
          throw new Error(err.error || 'Erro ao criar produto')
        }
        toast.success('Produto criado com sucesso!')
      }
      fetchProdutos()
    } catch (error) {
      toast.error(error.message || 'Erro ao salvar produto')
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

  const filtered = produtos.filter(p => {
    const matchSearch = (p.nome || '').toLowerCase().includes(search.toLowerCase()) ||
      getCategoriaNome(p.categoriaId).toLowerCase().includes(search.toLowerCase())
    const matchCategoria = !categoriaFiltro || p.categoriaId == categoriaFiltro
    return matchSearch && matchCategoria
  })

  // ===== DOWNLOAD CSV =====
  const handleDownloadCSV = () => {
    const csvData = filtered.map(p => ({
      Nome: p.nome || '',
      Descricao: p.descricao || '',
      CodigoBarras: p.codigoBarras || '',
      Categoria: getCategoriaNome(p.categoriaId),
      Preco: p.preco || 0,
      Estoque: p.estoque || 0,
      Unidade: p.unidade || 'un',
      Vendidos: getQuantidadeVendida(p.id)
    }))
    const csv = Papa.unparse(csvData)
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `produtos_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    toast.success('CSV exportado com sucesso!')
  }

  // ===== UPLOAD CSV =====
  const handleUploadCSV = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setImporting(true)
    try {
      Papa.parse(file, {
        header: true,
        skipEmptyLines: true,
        complete: async (results) => {
          const rows = results.data
          let created = 0, updated = 0, errors = 0
          for (const row of rows) {
            try {
              const produtoData = {
                nome: row.Nome || row.nome || '',
                descricao: row.Descricao || row.descricao || '',
                codigoBarras: row.CodigoBarras || row.codigoBarras || row.codigo_barras || undefined,
                preco: parseFloat(row.Preco || row.preco || '0'),
                estoque: parseInt(row.Estoque || row.estoque || '0'),
                unidade: row.Unidade || row.unidade || 'un',
                categoriaId: null,
                imagem: null
              }
              // Buscar categoria pelo nome
              const catNome = row.Categoria || row.categoria || ''
              if (catNome) {
                const cat = categorias.find(c => c.nome.toLowerCase() === catNome.toLowerCase())
                if (cat) produtoData.categoriaId = cat.id
              }
              // Se tem ID, atualizar; senão criar
              const existing = produtos.find(p => p.nome.toLowerCase() === produtoData.nome.toLowerCase())
              if (existing) {
                await fetch(apiUrl(`/api/produtos/${existing.id}`), {
                  method: 'PUT',
                  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
                  body: JSON.stringify(produtoData)
                })
                updated++
              } else {
                await fetch(apiUrl('/api/produtos'), {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
                  body: JSON.stringify(produtoData)
                })
                created++
              }
            } catch { errors++ }
          }
          fetchProdutos()
          toast.success(`Importação concluída: ${created} criados, ${updated} atualizados${errors ? `, ${errors} erros` : ''}`)
          setImporting(false)
        }
      })
    } catch (err) {
      toast.error('Erro ao importar CSV')
      setImporting(false)
    }
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  // ===== GERAR PDF =====
  const handleGeneratePDF = () => {
    const doc = new jsPDF('landscape')
    const title = 'Relatório de Produtos'
    const date = new Date().toLocaleDateString('pt-BR')

    doc.setFontSize(18)
    doc.setTextColor(40, 40, 40)
    doc.text(title, 14, 22)
    doc.setFontSize(10)
    doc.setTextColor(100, 100, 100)
    doc.text(`Gerado em: ${date} | Total: ${filtered.length} produtos`, 14, 30)

    const categoriaNome = categoriaFiltro ? getCategoriaNome(categoriaFiltro) : 'Todas'
    doc.text(`Categoria: ${categoriaNome}`, 14, 36)

    autoTable(doc, {
      startY: 42,
      head: [['Produto', 'Cód. Barras', 'Categoria', 'Preço', 'Estoque', 'Vendidos']],
      body: filtered.map(p => [
        p.nome || '-',
        p.codigoBarras || '-',
        getCategoriaNome(p.categoriaId),
        `R$ ${(p.preco || 0).toFixed(2)}`,
        `${p.estoque || 0} ${p.unidade || 'un'}`,
        getQuantidadeVendida(p.id).toString()
      ]),
      styles: { fontSize: 8, cellPadding: 3 },
      headStyles: { fillColor: [59, 130, 246], textColor: 255, fontStyle: 'bold' },
      alternateRowStyles: { fillColor: [245, 247, 250] },
      theme: 'grid'
    })

    doc.save(`produtos_${new Date().toISOString().slice(0, 10)}.pdf`)
    toast.success('PDF gerado com sucesso!')
  }

  // ===== SINCRONIZAR TERMINAIS =====
  const handleSyncTerminais = async () => {
    setSyncing(true)
    try {
      const res = await fetch(apiUrl('/api/force-sync'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ type: 'all' })
      })
      const data = await res.json()
      if (data.success) {
        toast.success(`Sincronizado com ${data.devices} terminal(is): ${data.synced.join(', ')}`)
      } else {
        toast.error('Erro ao sincronizar terminais')
      }
    } catch {
      toast.error('Erro ao conectar com o servidor')
    } finally {
      setSyncing(false)
    }
  }

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
        <div className="flex items-center gap-2 flex-wrap">
          <button onClick={openNew} className="btn-primary flex items-center gap-2 text-sm">
            <Plus size={16} /> Novo Produto
          </button>
          <button onClick={handleDownloadCSV} className="flex items-center gap-2 px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm transition-colors">
            <Download size={16} /> Exportar CSV
          </button>
          <input ref={fileInputRef} type="file" accept=".csv" onChange={handleUploadCSV} className="hidden" />
          <button onClick={() => fileInputRef.current?.click()} disabled={importing} className="flex items-center gap-2 px-3 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-sm transition-colors disabled:opacity-50">
            {importing ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />} Importar CSV
          </button>
          <button onClick={handleGeneratePDF} className="flex items-center gap-2 px-3 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm transition-colors">
            <FileText size={16} /> Gerar PDF
          </button>
          <button onClick={handleSyncTerminais} disabled={syncing} className="flex items-center gap-2 px-3 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-sm transition-colors disabled:opacity-50">
            {syncing ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />} Sincronizar Terminais
          </button>
        </div>
      </div>

      {/* Filtro por categorias */}
      {categorias.length > 0 && (
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => setCategoriaFiltro(null)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
              !categoriaFiltro
                ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                : 'bg-white/5 text-gray-400 border border-white/10 hover:bg-white/10'
            }`}
          >
            Todos
          </button>
          {categorias.map(c => (
            <button
              key={c.id}
              onClick={() => setCategoriaFiltro(categoriaFiltro == c.id ? null : c.id)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                categoriaFiltro == c.id
                  ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                  : 'bg-white/5 text-gray-400 border border-white/10 hover:bg-white/10'
              }`}
            >
              {c.nome}
            </button>
          ))}
        </div>
      )}

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
