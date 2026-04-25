import { useState, useEffect } from 'react'
import { X, Package, ImageIcon, Loader2 } from 'lucide-react'
import { apiUrl } from '../utils/api'

export default function ProdutoModal({ isOpen, onClose, onSave, produto, categorias, token }) {
  const [form, setForm] = useState({
    nome: '',
    descricao: '',
    preco: '',
    estoque: '',
    unidade: 'un',
    categoriaId: '',
    codigoBarras: ''
  })
  const [imagemFile, setImagemFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (isOpen) {
      if (produto) {
        setForm({
          nome: produto.nome || '',
          descricao: produto.descricao || '',
          preco: produto.preco ? produto.preco.toString().replace('.', ',') : '',
          estoque: produto.estoque || '',
          unidade: produto.unidade || 'un',
          categoriaId: produto.categoriaId || '',
          codigoBarras: produto.codigoBarras || ''
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
          codigoBarras: ''
        })
        setPreviewUrl('')
      }
      setImagemFile(null)
    }
  }, [isOpen, produto])

  const handleImagemChange = (e) => {
    const file = e.target.files[0]
    if (file) {
      setImagemFile(file)
      setPreviewUrl(URL.createObjectURL(file))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      let imagemUrl = produto?.imagem || ''

      if (imagemFile) {
        const formData = new FormData()
        formData.append('imagem', imagemFile)

        const res = await fetch(apiUrl('/api/upload'), {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
          body: formData
        })

        if (res.ok) {
          const data = await res.json()
          imagemUrl = data.url
        }
      }

      const produtoData = {
        nome: form.nome,
        descricao: form.descricao,
        preco: parseFloat(form.preco.replace(/[^0-9,]/g, '').replace(',', '.')) || 0,
        estoque: parseInt(form.estoque) || 0,
        unidade: form.unidade || 'un',
        categoriaId: form.categoriaId,
        codigoBarras: form.codigoBarras,
        imagem: imagemUrl
      }

      console.log('Dados do produto a enviar:', produtoData)
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
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-gray-900 border border-gray-700 rounded-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b border-gray-700">
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

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Nome</label>
            <input
              type="text"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Descrição</label>
            <textarea
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500 resize-none"
              rows="2"
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Preço (R$)</label>
              <input
                type="text"
                value={form.preco}
                onChange={(e) => {
                  let value = e.target.value.replace(/[^0-9,]/g, '')
                  if (value.includes(',')) {
                    const parts = value.split(',')
                    if (parts[1] && parts[1].length > 2) {
                      value = parts[0] + ',' + parts[1].slice(0, 2)
                    }
                  }
                  setForm({ ...form, preco: value })
                }}
                placeholder="0,00"
                className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Estoque</label>
              <input
                type="number"
                value={form.estoque}
                onChange={(e) => setForm({ ...form, estoque: e.target.value })}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Unidade</label>
              <select
                value={form.unidade}
                onChange={(e) => setForm({ ...form, unidade: e.target.value })}
                className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
              >
                <option value="un">un (Unidade)</option>
                <option value="cx">cx (Caixa)</option>
                <option value="kg">kg (Quilograma)</option>
                <option value="lt">lt (Litro)</option>
                <option value="ml">ml (Mililitro)</option>
                <option value="gr">gr (Grama)</option>
                <option value="mt">mt (Metro)</option>
                <option value="cm">cm (Centímetro)</option>
                <option value="pc">pc (Peça)</option>
                <option value="dz">dz (Dúzia)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Categoria</label>
            <select
              value={form.categoriaId}
              onChange={(e) => setForm({ ...form, categoriaId: e.target.value })}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
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
            <label className="block text-sm font-medium text-gray-300 mb-1">Código de Barras</label>
            <input
              type="text"
              value={form.codigoBarras}
              onChange={(e) => setForm({ ...form, codigoBarras: e.target.value })}
              className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
              placeholder="Deixe vazio para gerar automático"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Imagem</label>
            <div className="space-y-2">
              {previewUrl && (
                <div className="relative w-full h-32 bg-gray-800 rounded-lg overflow-hidden border border-gray-600">
                  <img
                    src={previewUrl}
                    alt="Preview"
                    className="w-full h-full object-contain"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      setPreviewUrl('')
                      setImagemFile(null)
                    }}
                    className="absolute top-2 right-2 w-6 h-6 bg-red-500 hover:bg-red-600 rounded-full flex items-center justify-center text-white"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              )}
              <div className="flex items-center gap-2">
                <label className="flex-1 flex items-center gap-2 px-4 py-2 bg-gray-800 border border-gray-600 border-dashed rounded-lg cursor-pointer hover:bg-gray-700 transition-colors">
                  <ImageIcon className="w-4 h-4 text-gray-400" />
                  <span className="text-sm text-gray-400">
                    {imagemFile ? imagemFile.name : 'Selecionar imagem'}
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImagemChange}
                    className="hidden"
                    disabled={loading}
                  />
                </label>
              </div>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition-colors disabled:opacity-50"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
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
