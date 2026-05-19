import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, RotateCw, Image, Building2, List, Eye, ChevronDown } from 'lucide-react'

const PAPER_FORMATS = {
  '80mm': { label: '80mm (Padrão)', width: 200 },
  '58mm': { label: '58mm (Pequeno)', width: 145 },
  '76mm': { label: '76mm', width: 190 },
  '57mm': { label: '57mm', width: 142 },
}

const defaultTemplate = {
  cabecalho: {
    nomeEmpresa: true,
    cnpj: true,
    endereco: true,
    telefone: true,
    email: true,
    cidade: true,
  },
  logo: {
    enabled: false,
    width: 120,
    height: 60,
    spacingTop: 10,
    spacingBottom: 10,
  },
  itens: {
    nome: true,
    quantidade: true,
    valorUnitario: true,
    valorTotal: true,
    separador: true,
  },
  adicionais: {
    subtotal: true,
    desconto: true,
    total: true,
    formaPagamento: true,
    valorRecebido: true,
    troco: true,
    numeroVenda: true,
    dataHora: true,
  },
  rodape: {
    linha1: 'Agradecemos sua vinda',
    linha2: 'Volte sempre',
    linha3: '',
    linha4: '',
  },
  estilo: {
    alinhamento: 'centro',
    tamanhoFonte: 'medio',
    espacoEntreLinhas: 8,
  }
}

const fieldLabels = {
  nomeEmpresa: 'Nome da Empresa',
  cnpj: 'CNPJ',
  endereco: 'Endereço',
  telefone: 'Telefone',
  email: 'E-mail',
  cidade: 'Cidade/UF',
}

const itemLabels = {
  nome: 'Nome do Produto',
  quantidade: 'Quantidade',
  valorUnitario: 'Valor Unitário',
  valorTotal: 'Subtotal do Item',
  separador: 'Linha Separadora',
}

const adicionalLabels = {
  subtotal: 'Subtotal',
  desconto: 'Desconto',
  total: 'TOTAL',
  formaPagamento: 'Forma de Pagamento',
  valorRecebido: 'Valor Recebido',
  troco: 'Troco',
  numeroVenda: 'Número da Venda',
  dataHora: 'Data/Hora',
}

function PreviewComprovante({ template, paperFormat = '80mm' }) {
  const exampleVenda = {
    id: 123,
    data: '19/05/2026 15:45',
    itens: [
      { nome: 'Coca-Cola 350ml', qtd: 2, valorUnit: 3.00, total: 6.00 },
      { nome: 'Sanduíche Natural', qtd: 1, valorUnit: 8.00, total: 8.00 },
    ],
    subtotal: 14.00,
    desconto: 0.00,
    total: 14.00,
    formaPagamento: 'DINHEIRO',
    valorRecebido: 20.00,
    troco: 6.00,
  }

  const fontSizeMap = { pequeno: 9, medio: 11, grande: 13 }
  const fs = fontSizeMap[template.estilo?.tamanhoFonte] || 11
  const lineHeight = template.estilo?.espacoEntreLinhas || 6
  const paperWidth = PAPER_FORMATS[paperFormat]?.width || 200

  const alignStyle = {
    esquerda: 'flex-start text-left',
    direita: 'flex-end text-right',
    centro: 'items-center text-center'
  }[template.estilo?.alinhamento] || 'items-center text-center'

  return (
    <div 
      className="bg-white text-black mx-auto rounded-lg font-mono overflow-hidden shadow-2xl" 
      style={{ 
        width: paperWidth, 
        fontSize: fs, 
        lineHeight: lineHeight,
        padding: '12px 8px'
      }}
    >
      {template.logo?.enabled && (
        <div className="flex justify-center mb-2" style={{ marginBottom: template.logo.spacingTop }}>
          <div className="bg-gray-200 flex items-center justify-center rounded" style={{ width: template.logo.width, height: template.logo.height }}>
            <span className="text-gray-500" style={{ fontSize: fs - 2 }}>[LOGO]</span>
          </div>
        </div>
      )}
      
      <div className={`flex flex-col ${alignStyle}`}>
        {template.cabecalho?.nomeEmpresa && <span className="font-bold">Rodrigo Dev MT</span>}
        {template.cabecalho?.cnpj && <span>CNPJ 12.345.678/0001-90</span>}
        {template.cabecalho?.endereco && <span>Rua Exemplo, 123 - Centro</span>}
        {template.cabecalho?.telefone && <span>(45) 99999-9999</span>}
      </div>
      
      <div className={`flex flex-col ${alignStyle} mt-2 border-t border-dashed border-gray-400 pt-2`}>
        COMPROVANTE DE VENDA
      </div>
      
      <div className={`flex flex-col ${alignStyle}`}>
        {template.adicionais?.numeroVenda && <span>Nr: {exampleVenda.id.toString().padStart(6, '0')}</span>}
        {template.adicionais?.dataHora && <span>DATA: {exampleVenda.data}</span>}
      </div>
      
      <div className={`flex flex-col ${alignStyle} mt-2 border-t border-dashed border-gray-400 pt-2`}>
        <span className="font-bold">ITENS:</span>
        {exampleVenda.itens.map((item, i) => (
          <div key={i} className="mt-1">
            {template.itens?.nome && <span>{item.nome}</span>}
            {template.itens?.separador && <span>----------------------------</span>}
            {(template.itens?.quantidade || template.itens?.valorUnitario || template.itens?.valorTotal) && (
              <span>
                {template.itens?.quantidade && `QTD: ${item.qtd}`}
                {template.itens?.valorUnitario && ` x R$ ${item.valorUnit.toFixed(2)}`}
                {template.itens?.valorTotal && ` = R$ ${item.total.toFixed(2)}`}
              </span>
            )}
          </div>
        ))}
      </div>

      <div className={`flex flex-col ${alignStyle} mt-2 pt-2 border-t border-dashed border-gray-400`}>
        {template.adicionais?.subtotal && (
          <div className="flex justify-between w-full"><span>SUBTOTAL:</span><span>R$ {exampleVenda.subtotal.toFixed(2)}</span></div>
        )}
        {template.adicionais?.desconto && (
          <div className="flex justify-between w-full"><span>DESCONTO:</span><span>R$ {exampleVenda.desconto.toFixed(2)}</span></div>
        )}
        <div className="flex justify-between w-full font-bold border-t border-gray-600 pt-1 mt-1">
          <span>TOTAL:</span><span>R$ {exampleVenda.total.toFixed(2)}</span>
        </div>
      </div>

      <div className={`flex flex-col ${alignStyle} mt-2 pt-2 border-t border-dashed border-gray-400`}>
        {template.adicionais?.formaPagamento && <span>FORMA DE PAGAMENTO: {exampleVenda.formaPagamento}</span>}
        {template.adicionais?.valorRecebido && <span>VALOR RECEBIDO: R$ {exampleVenda.valorRecebido.toFixed(2)}</span>}
        {template.adicionais?.troco && <span>TROCO: R$ {exampleVenda.troco.toFixed(2)}</span>}
      </div>

      <div className={`flex flex-col ${alignStyle} mt-4 pt-2 border-t border-dashed border-gray-400`}>
        {template.rodape?.linha1 && <span>{template.rodape.linha1}</span>}
        {template.rodape?.linha2 && <span>{template.rodape.linha2}</span>}
        {template.rodape?.linha3 && <span>{template.rodape.linha3}</span>}
        {template.rodape?.linha4 && <span>{template.rodape.linha4}</span>}
      </div>
    </div>
  )
}

export default function ConfiguracoesImpressao() {
  const { token, user } = useAuth()
  const [template, setTemplate] = useState(defaultTemplate)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('cabecalho')
  const [showPreview, setShowPreview] = useState(true)
  const [paperFormat, setPaperFormat] = useState('80mm')
  const [showFormatDropdown, setShowFormatDropdown] = useState(false)

  const isAdmin = user?.role === 'admin'
  const isEmpresa = user?.role === 'empresa'

  useEffect(() => {
    fetch(apiUrl('/api/impressao/template'), {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(r => r.ok ? r.json() : null)
      .then(data => {
        if (data) setTemplate({ ...defaultTemplate, ...data })
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [token])

  const handleSave = async () => {
    setSaving(true)
    try {
      const res = await fetch(apiUrl('/api/impressao/template'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ ...template, paperFormat })
      })
      if (res.ok) alert('✅ Template salvo com sucesso')
      else alert('❌ Erro ao salvar')
    } catch { alert('❌ Erro ao salvar') }
    setSaving(false)
  }

  const updateField = (section, field, value) => {
    setTemplate(prev => ({
      ...prev,
      [section]: { ...prev[section], [field]: value }
    }))
  }

  const toggleField = (section, field) => {
    setTemplate(prev => ({
      ...prev,
      [section]: { ...prev[section], [field]: !prev[section]?.[field] }
    }))
  }

  const handleLogoUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      setTemplate(prev => ({
        ...prev,
        logo: { ...prev.logo, base64: ev.target.result }
      }))
    }
    reader.readAsDataURL(file)
  }

  if (loading) {
    return (
      <div className="glass p-12 text-center">
        <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
        <p className="text-gray-400">Carregando template...</p>
      </div>
    )
  }

  const tabs = [
    { id: 'cabecalho', label: 'Cabeçalho', icon: Building2 },
    { id: 'logo', label: 'Logo', icon: Image },
    { id: 'itens', label: 'Itens', icon: List },
    { id: 'adicional', label: 'Adicionais', icon: List },
    { id: 'rodape', label: 'Rodapé', icon: List },
    { id: 'estilo', label: 'Estilo', icon: List },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Printer size={24} className="text-blue-400" />
          {isAdmin ? 'Template de Impressão' : isEmpresa ? 'Meu Template' : 'Configuração de Impressão'}
        </h2>
        <div className="flex gap-2">
          <button onClick={() => setShowPreview(!showPreview)} className="btn-ghost flex items-center gap-2 text-sm">
            <Eye size={16} /> {showPreview ? 'Ocultar' : 'Mostrar'} Preview
          </button>
          <button onClick={handleSave} disabled={saving} className="btn-primary flex items-center gap-2 text-sm">
            {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Salvar
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Editor */}
        <div className="xl:col-span-2 space-y-4">
          {/* Tabs */}
          <div className="flex gap-2 overflow-x-auto pb-2">
            {tabs.map(t => (
              <button key={t.id} onClick={() => setActiveTab(t.id)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all whitespace-nowrap ${activeTab === t.id ? 'bg-blue-600 text-white' : 'glass text-gray-400 hover:text-white'}`}>
                <t.icon size={14} className="inline mr-1" /> {t.label}
              </button>
            ))}
          </div>

          <div className="glass p-4">
            {/* Cabeçalho */}
            {activeTab === 'cabecalho' && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-gray-300">Campos do Cabeçalho</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {Object.entries(fieldLabels).map(([key, label]) => (
                    <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                      <input type="checkbox" checked={template.cabecalho?.[key] ?? true}
                        onChange={() => toggleField('cabecalho', key)}
                        className="rounded" />
                      <span className="text-sm text-gray-300">{label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Logo */}
            {activeTab === 'logo' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-300">Configurações do Logo</h3>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={template.logo?.enabled ?? false}
                      onChange={() => updateField('logo', 'enabled', !template.logo?.enabled)}
                      className="rounded" />
                    <span className="text-sm text-gray-300">Ativar Logo</span>
                  </label>
                </div>
                
                {template.logo?.enabled && (
                  <>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Largura (px)</label>
                        <input type="number" value={template.logo?.width || 120}
                          onChange={e => updateField('logo', 'width', parseInt(e.target.value) || 120)}
                          className="input-field text-sm" />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Altura (px)</label>
                        <input type="number" value={template.logo?.height || 60}
                          onChange={e => updateField('logo', 'height', parseInt(e.target.value) || 60)}
                          className="input-field text-sm" />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Espaço Acima (px)</label>
                        <input type="number" value={template.logo?.spacingTop || 10}
                          onChange={e => updateField('logo', 'spacingTop', parseInt(e.target.value) || 10)}
                          className="input-field text-sm" />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Espaço Abaixo (px)</label>
                        <input type="number" value={template.logo?.spacingBottom || 10}
                          onChange={e => updateField('logo', 'spacingBottom', parseInt(e.target.value) || 10)}
                          className="input-field text-sm" />
                      </div>
                    </div>
                    
                    {template.logo?.base64 ? (
                      <div className="border border-gray-600 rounded-lg p-4 text-center">
                        <img src={template.logo.base64} alt="Logo" className="max-h-20 mx-auto mb-2" />
                        <button onClick={() => updateField('logo', 'base64', null)} className="text-red-400 text-xs hover:underline">Remover</button>
                      </div>
                    ) : (
                      <div>
                        <label className="btn-ghost cursor-pointer text-sm">
                          <Image size={14} className="inline mr-1" /> Upload Logo
                          <input type="file" accept="image/*" onChange={handleLogoUpload} className="hidden" />
                        </label>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}

            {/* Itens */}
            {activeTab === 'itens' && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-gray-300">Campos dos Itens</h3>
                <div className="grid grid-cols-2 gap-3">
                  {Object.entries(itemLabels).map(([key, label]) => (
                    <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                      <input type="checkbox" checked={template.itens?.[key] ?? true}
                        onChange={() => toggleField('itens', key)}
                        className="rounded" />
                      <span className="text-sm text-gray-300">{label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Adicional */}
            {activeTab === 'adicional' && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-gray-300">Campos Adicionais</h3>
                <div className="grid grid-cols-2 gap-3">
                  {Object.entries(adicionalLabels).map(([key, label]) => (
                    <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                      <input type="checkbox" checked={template.adicionais?.[key] ?? true}
                        onChange={() => toggleField('adicionais', key)}
                        className="rounded" />
                      <span className="text-sm text-gray-300">{label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Rodapé */}
            {activeTab === 'rodape' && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-gray-300">Mensagens do Rodapé</h3>
                {[1, 2, 3, 4].map(n => (
                  <div key={n}>
                    <label className="block text-xs text-gray-400 mb-1">Linha {n}</label>
                    <input type="text" value={template.rodape?.[`linha${n}`] || ''}
                      onChange={e => updateField('rodape', `linha${n}`, e.target.value)}
                      className="input-field text-sm" placeholder={`Mensagem ${n} (opcional)`} />
                  </div>
                ))}
              </div>
            )}

            {/* Estilo */}
            {activeTab === 'estilo' && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-gray-300">Estilo do Comprovante</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Alinhamento</label>
                    <select value={template.estilo?.alinhamento || 'centro'}
                      onChange={e => updateField('estilo', 'alinhamento', e.target.value)}
                      className="input-field text-sm">
                      <option value="centro">Centro</option>
                      <option value="esquerda">Esquerda</option>
                      <option value="direita">Direita</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Tamanho da Fonte</label>
                    <select value={template.estilo?.tamanhoFonte || 'medio'}
                      onChange={e => updateField('estilo', 'tamanhoFonte', e.target.value)}
                      className="input-field text-sm">
                      <option value="pequeno">Pequeno</option>
                      <option value="medio">Médio</option>
                      <option value="grande">Grande</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Espaço entre Linhas</label>
                    <input type="number" value={template.estilo?.espacoEntreLinhas || 8}
                      onChange={e => updateField('estilo', 'espacoEntreLinhas', parseInt(e.target.value) || 8)}
                      className="input-field text-sm" />
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Preview */}
        {showPreview && (
          <div className="xl:col-span-1">
            <div className="sticky top-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
                  <Eye size={14} /> Preview do Comprovante
                </h3>
                
                {/* Dropdown Formato do Papel */}
                <div className="relative">
                  <button 
                    onClick={() => setShowFormatDropdown(!showFormatDropdown)}
                    className="flex items-center gap-2 px-3 py-1.5 bg-gray-800 hover:bg-gray-700 border border-gray-600 rounded-lg text-sm text-white transition-all"
                  >
                    <Printer size={14} className="text-blue-400" />
                    {PAPER_FORMATS[paperFormat]?.label || '80mm (Padrão)'}
                    <ChevronDown size={14} className={`transition-transform ${showFormatDropdown ? 'rotate-180' : ''}`} />
                  </button>
                  
                  {showFormatDropdown && (
                    <div className="absolute right-0 top-full mt-2 w-48 bg-gray-900 border border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden">
                      {Object.entries(PAPER_FORMATS).map(([key, { label }]) => (
                        <button
                          key={key}
                          onClick={() => { setPaperFormat(key); setShowFormatDropdown(false) }}
                          className={`w-full px-4 py-2.5 text-left text-sm transition-all flex items-center justify-between ${
                            paperFormat === key 
                              ? 'bg-blue-600/20 text-blue-400' 
                              : 'text-gray-300 hover:bg-gray-800'
                          }`}
                        >
                          <span>{label}</span>
                          {paperFormat === key && <span className="text-blue-400">✓</span>}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
              
              {/* Preview Container */}
              <div className="bg-gray-900 p-6 rounded-xl border border-gray-700 flex justify-center overflow-x-auto">
                <PreviewComprovante template={template} paperFormat={paperFormat} />
              </div>
              
              {/* Paper format info */}
              <div className="mt-3 text-center">
                <span className="text-xs text-gray-500">
                  Largura real: {PAPER_FORMATS[paperFormat]?.width || 200}px • {paperFormat}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}