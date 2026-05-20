import { useState, useEffect, useRef } from 'react'
import { X, Package, ImageIcon, Loader2, Upload } from 'lucide-react'
import { useToast } from '../contexts/ToastContext'

export default function ProdutoModal({ isOpen, onClose, onSave, produto, categorias, empresas, user, token }) {
  const toast = useToast()
  const [form, setForm] = useState({
    nome: '',
    descricao: '',
    preco: '',
    estoque: '',
    unidade: 'un',
    categoriaId: '',
    codigoBarras: '',
    empresaId: ''
  })
  const [imagemFile, setImagemFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => {
    if (isOpen) {
      if (produto) {
        setForm({
          nome: produto.nome || '',
          descricao: produto.descricao || '',
          preco: produto.preco ? produto.preco.toFixed(2).replace('.', ',') : '',
          estoque: produto.estoque || '',
          unidade: produto.unidade || 'un',
          categoriaId: produto.categoriaId || '',
          codigoBarras: produto.codigoBarras || '',
          empresaId: produto.empresaId || ''
        })
        setPreviewUrl(produto.imagem || '')
      } else {
        setForm({
          nome: '',
          descricao: '',
          preco: '',
          estoque: '',
          unidade: 'un',
          categoriaId: '',
          codigoBarras: '',
          empresaId: ''
        })
        setPreviewUrl('')
      }
      setImagemFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }, [isOpen, produto])

  const handleImagemChange = (e) => {
    const file = e.target.files[0]
    if (file) {
      setImagemFile(file)
      setPreviewUrl(URL.createObjectURL(file))
    }
  }

  const fileToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => resolve(reader.result)
      reader.onerror = error => reject(error)
    })
  }

  const formatPreco = (value) => {
    // Permite apenas números e uma vírgula
    let v = value.replace(/[^0-9,]/g, '')
    // Remove vírgulas extras
    const parts = v.split(',')
    if (parts.length > 2) v = parts[0] + ',' + parts.slice(1).join('')
    // Limita decimais a 2 casas
    if (parts.length === 2 && parts[1].length > 2) {
      v = parts[0] + ',' + parts[1].slice(0, 2)
    }
    return v
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      let imagemData = produto?.imagem || ''

      if (imagemFile) {
        imagemData = await fileToBase64(imagemFile)
      }

      const produtoData = {
        nome: form.nome,
        descricao: form.descricao,
        preco: parseFloat(form.preco.replace(',', '.')) || 0,
        estoque: parseInt(form.estoque) || 0,
        unidade: form.unidade,
        categoriaId: form.categoriaId || null,
        codigoBarras: form.codigoBarras || undefined,
        imagem: imagemData || null
      }

      if (user?.role === 'admin') {
        if (!form.empresaId) {
          setLoading(false)
          toast.warning('Selecione uma empresa para cadastrar o produto')
          return
        }
        const empresaSelecionada = empresas?.find(e => e.id === form.empresaId)
        if (!empresaSelecionada) {
          setLoading(false)
          toast.error('Empresa selecionada inválida')
          return
        }
        produtoData.empresaId = form.empresaId
        console.log(`📦 [PRODUTO] Admin criando produto para empresa: ${empresaSelecionada.nome} (ID: ${form.empresaId})`)
      } else {
        produtoData.empresaId = user?.empresaId
        console.log(`📦 [PRODUTO] Empresa criando produto - empresaId: ${user?.empresaId}`)
      }

      await onSave(produtoData)
      onClose()
    } catch (error) {
      console.error('Erro ao salvar:', error)
    } finally {
      setLoading(false)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-2">
      <div className="bg-gray-900 border border-gray-700 rounded-xl w-full max-w-lg flex flex-col max-h-[95vh]">
        <div className="flex items-center justify-between px-3 py-2.5 border-b border-gray-700 shrink-0">
          <div className="flex items-center gap-2">
            <Package className="w-5 h-5 text-blue-400" />
            <h2 className="text-lg font-semibold text-white">
              {produto ? 'Editar Produto' : 'Novo Produto'}
            </h2>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col min-h-0">
          <div className="overflow-y-auto px-3 py-2 space-y-1.5">
            {user?.role === 'admin' && (
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Empresa</label>
                <select
                  value={form.empresaId}
                  onChange={(e) => setForm({ ...form, empresaId: e.target.value })}
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                >
                  <option value="">Selecione uma empresa</option>
                  {empresas?.map(emp => (
                    <option key={emp.id} value={emp.id}>{emp.nome}</option>
                  ))}
                </select>
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-gray-300 mb-0.5">Nome</label>
              <input
                type="text"
                value={form.nome}
                onChange={(e) => setForm({ ...form, nome: e.target.value })}
                className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-300 mb-0.5">Descrição</label>
              <textarea
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500 resize-none"
                rows="1"
              />
            </div>

            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Preço (R$)</label>
                <input
                  type="text"
                  inputMode="decimal"
                  value={form.preco}
                  onChange={(e) => setForm({ ...form, preco: formatPreco(e.target.value) })}
                  placeholder="0,00"
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Estoque</label>
                <input
                  type="number"
                  inputMode="numeric"
                  value={form.estoque}
                  onChange={(e) => setForm({ ...form, estoque: e.target.value })}
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Unidade</label>
                <select
                  value={form.unidade}
                  onChange={(e) => setForm({ ...form, unidade: e.target.value })}
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                >
                  <option value="un">un</option>
                  <option value="cx">cx</option>
                  <option value="kg">kg</option>
                  <option value="lt">lt</option>
                  <option value="ml">ml</option>
                  <option value="gr">gr</option>
                  <option value="mt">mt</option>
                  <option value="cm">cm</option>
                  <option value="pc">pc</option>
                  <option value="dz">dz</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Categoria</label>
                <select
                  value={form.categoriaId}
                  onChange={(e) => setForm({ ...form, categoriaId: e.target.value })}
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                >
                  <option value="">Selecione...</option>
                  {categorias.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.nome}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-300 mb-0.5">Código de Barras</label>
                <input
                  type="text"
                  value={form.codigoBarras}
                  onChange={(e) => setForm({ ...form, codigoBarras: e.target.value })}
                  className="w-full px-2.5 py-1.5 bg-gray-800 border border-gray-600 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500"
                  placeholder="Automático"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-300 mb-0.5">Imagem do Produto</label>
              <div className="flex items-center gap-2">
                {previewUrl && (
                  <div className="relative w-12 h-12 shrink-0 bg-gray-800 rounded-lg overflow-hidden border border-gray-600">
                    <img src={previewUrl} alt="Preview" className="w-full h-full object-contain" />
                    <button
                      type="button"
                      onClick={() => { setPreviewUrl(''); setImagemFile(null); if (fileInputRef.current) fileInputRef.current.value = '' }}
                      className="absolute top-0.5 right-0.5 w-4 h-4 bg-red-500 hover:bg-red-600 rounded-full flex items-center justify-center text-white"
                    >
                      <X className="w-2 h-2" />
                    </button>
                  </div>
                )}
                <input ref={fileInputRef} type="file" accept="image/*" onChange={handleImagemChange} className="hidden" />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={loading}
                  className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 bg-gray-800 border border-gray-600 border-dashed rounded-lg hover:bg-gray-700 transition-colors text-gray-400 text-xs disabled:opacity-50"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>{imagemFile ? imagemFile.name : 'Selecionar imagem'}</span>
                </button>
              </div>
            </div>
          </div>

          <div className="flex gap-2 px-3 py-2 border-t border-gray-700 shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-sm transition-colors disabled:opacity-50"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-1.5">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  Salvando...
                </span>
              ) : (
                produto ? 'Salvar' : 'Criar'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
