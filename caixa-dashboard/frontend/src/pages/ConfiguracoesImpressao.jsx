import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, Image, Building2, List, Eye, ChevronDown, Type, Ruler, Palette, AlignLeft, AlignCenter, AlignRight, Upload, Trash2, Edit3, Phone, Mail, MapPin, Hash, Building } from 'lucide-react'

const PAPER_FORMATS = {
  '80mm': { label: '80mm Padrão', width: 200 },
  '58mm': { label: '58mm Pequeno', width: 145 },
  '76mm': { label: '76mm', width: 190 },
  '57mm': { label: '57mm', width: 142 },
}

const defaultTemplate = {
  empresa: { nome: 'Rodrigo Dev MT', cnpj: '12.345.678/0001-90', ie: '', endereco: '', telefone: '', email: '', cidade: '' },
  logo: { enabled: false, width: 120, height: 60, spacingTop: 10, spacingBottom: 10, base64: null },
  cabecalho: { titulo: 'COMPROVANTE DE VENDA', subtitulo: '' },
  itens: { nome: true, quantidade: true, valorUnitario: true, valorTotal: true, separador: true },
  adicionais: { subtotal: true, desconto: true, total: true, formaPagamento: true, valorRecebido: true, troco: true, numeroVenda: true, dataHora: true },
  rodape: { linha1: 'Agradecemos sua vinda', linha2: 'Volte sempre', linha3: '', linha4: '' },
  estilo: { alinhamento: 'centro', tamanhoFonte: 'medio', espacoEntreLinhas: 8 },
  tamanhos: { titulo: 32, subtitulo: 24, corpo: 22, rodape: 20, empresa: 28 }
}

function PreviewComprovante({ template, paperFormat = '80mm' }) {
  const exampleVenda = { id: 123, data: '19/05/2026 15:45', itens: [{ nome: 'Coca-Cola 350ml', qtd: 2, valorUnit: 3.00, total: 6.00 }, { nome: 'Sanduíche Natural', qtd: 1, valorUnit: 8.00, total: 8.00 }], subtotal: 14.00, desconto: 0.00, total: 14.00, formaPagamento: 'DINHEIRO', valorRecebido: 20.00, troco: 6.00 }
  const fs = { pequeno: 9, medio: 11, grande: 13 }[template.estilo?.tamanhoFonte] || 11
  const tituloFs = (template.tamanhos?.titulo || 32) * 0.35
  const empresaFs = (template.tamanhos?.empresa || 28) * 0.4
  const paperWidth = PAPER_FORMATS[paperFormat]?.width || 200
  const alignClass = { esquerda: 'text-left', direita: 'text-right', centro: 'text-center' }[template.estilo?.alinhamento] || 'text-center'

  return (
    <div className="bg-white text-black rounded font-mono overflow-hidden" style={{ width: paperWidth, fontSize: fs, lineHeight: 1.4, padding: '10px 6px' }}>
      {template.logo?.enabled && template.logo?.base64 && (
        <div className="flex justify-center mb-1" style={{ marginBottom: template.logo.spacingTop || 10 }}>
          <img src={template.logo.base64} alt="Logo" style={{ width: template.logo.width, height: template.logo.height, objectFit: 'contain' }} />
        </div>
      )}
      {template.logo?.enabled && !template.logo?.base64 && (
        <div className="flex justify-center mb-1" style={{ marginBottom: template.logo.spacingTop || 10 }}>
          <div className="bg-gray-200 flex items-center justify-center rounded text-gray-500" style={{ width: template.logo.width, height: template.logo.height, fontSize: fs - 2 }}>[LOGO]</div>
        </div>
      )}
      <div className={alignClass}>
        <div className="font-bold" style={{ fontSize: empresaFs }}>{template.empresa?.nome || 'Empresa'}</div>
        {template.empresa?.cnpj && <div>CNPJ: {template.empresa.cnpj}</div>}
        {template.empresa?.ie && <div>IE: {template.empresa.ie}</div>}
        {template.empresa?.endereco && <div>{template.empresa.endereco}</div>}
        {template.empresa?.cidade && <div>{template.empresa.cidade}</div>}
        {template.empresa?.telefone && <div>Tel: {template.empresa.telefone}</div>}
        {template.empresa?.email && <div>{template.empresa.email}</div>}
      </div>
      <div className={`${alignClass} my-2 border-t border-b border-dashed border-gray-400 py-1`} style={{ fontSize: tituloFs }}>
        {template.cabecalho?.titulo || 'COMPROVANTE DE VENDA'}
      </div>
      {template.cabecalho?.subtitulo && <div className={`${alignClass} text-xs`}>{template.cabecalho.subtitulo}</div>}
      <div className={alignClass}>
        {template.adicionais?.numeroVenda && <div>Nr: {exampleVenda.id.toString().padStart(6, '0')}</div>}
        {template.adicionais?.dataHora && <div>DATA: {exampleVenda.data}</div>}
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        <div className="font-bold">ITENS:</div>
        {exampleVenda.itens.map((item, i) => (
          <div key={i} className="mt-1">
            {template.itens?.nome && <div>{item.nome}</div>}
            {template.itens?.separador && <div>----------------------------</div>}
            {(template.itens?.quantidade || template.itens?.valorUnitario || template.itens?.valorTotal) && (
              <div>{template.itens?.quantidade && `QTD: ${item.qtd}`}{template.itens?.valorUnitario && ` x R$ ${item.valorUnit.toFixed(2)}`}{template.itens?.valorTotal && ` = R$ ${item.total.toFixed(2)}`}</div>
            )}
          </div>
        ))}
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        {template.adicionais?.subtotal && <div className="flex justify-between"><span>SUBTOTAL:</span><span>R$ {exampleVenda.subtotal.toFixed(2)}</span></div>}
        {template.adicionais?.desconto && <div className="flex justify-between"><span>DESCONTO:</span><span>R$ {exampleVenda.desconto.toFixed(2)}</span></div>}
        <div className="flex justify-between font-bold border-t border-gray-600 mt-1 pt-1"><span>TOTAL:</span><span>R$ {exampleVenda.total.toFixed(2)}</span></div>
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        {template.adicionais?.formaPagamento && <div>FORMA: {exampleVenda.formaPagamento}</div>}
        {template.adicionais?.valorRecebido && <div>RECEBIDO: R$ {exampleVenda.valorRecebido.toFixed(2)}</div>}
        {template.adicionais?.troco && <div>TROCO: R$ {exampleVenda.troco.toFixed(2)}</div>}
      </div>
      <div className={`${alignClass} mt-3 pt-2 border-t border-dashed border-gray-400`}>
        {template.rodape?.linha1 && <div style={{ fontSize: (template.tamanhos?.rodape || 20) * 0.4 }}>{template.rodape.linha1}</div>}
        {template.rodape?.linha2 && <div style={{ fontSize: (template.tamanhos?.rodape || 20) * 0.4 }}>{template.rodape.linha2}</div>}
        {template.rodape?.linha3 && <div style={{ fontSize: (template.tamanhos?.rodape || 20) * 0.4 }}>{template.rodape.linha3}</div>}
        {template.rodape?.linha4 && <div style={{ fontSize: (template.tamanhos?.rodape || 20) * 0.4 }}>{template.rodape.linha4}</div>}
      </div>
    </div>
  )
}

function DarkSelect({ value, onChange, options, className = '' }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className={`flex items-center justify-between gap-2 px-3 py-2 bg-gray-900 hover:bg-gray-800 border border-gray-700 rounded-lg text-sm text-white transition-all ${className}`}>
        <span>{options.find(o => o.value === value)?.label || 'Selecione'}</span>
        <ChevronDown size={14} className={`transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && (
        <div className="absolute left-0 top-full mt-1 w-full min-w-[140px] bg-gray-900 border border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden">
          {options.map(opt => (
            <button key={opt.value} onClick={() => { onChange(opt.value); setOpen(false) }}
              className={`w-full px-3 py-2 text-left text-sm transition-all ${value === opt.value ? 'bg-blue-600/20 text-blue-400' : 'text-gray-300 hover:bg-gray-800'}`}>
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function RangeSlider({ value, onChange, min = 8, max = 60, step = 1, label }) {
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between">
        <span className="text-xs text-gray-400">{label}</span>
        <span className="text-xs text-blue-400 font-mono">{value}px</span>
      </div>
      <input type="range" min={min} max={max} step={step} value={value} onChange={e => onChange(parseInt(e.target.value))}
        className="w-full h-2 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-blue-500" />
    </div>
  )
}

function SectionCard({ title, icon: Icon, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen)
  return (
    <div className="glass rounded-xl overflow-hidden">
      <button onClick={() => setOpen(!open)} className="w-full flex items-center justify-between p-4 hover:bg-white/5 transition-colors">
        <div className="flex items-center gap-3">
          <Icon size={18} className="text-blue-400" />
          <span className="font-semibold text-white">{title}</span>
        </div>
        <ChevronDown size={18} className={`text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && <div className="p-4 pt-0 border-t border-white/5">{children}</div>}
    </div>
  )
}

export default function ConfiguracoesImpressao() {
  const { token, user } = useAuth()
  const [template, setTemplate] = useState(defaultTemplate)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('empresa')
  const [showPreview, setShowPreview] = useState(true)
  const [paperFormat, setPaperFormat] = useState('80mm')

  const isAdmin = user?.role === 'admin'
  const isEmpresa = user?.role === 'empresa'

  useEffect(() => {
    fetch(apiUrl('/api/impressao/template'), { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.ok ? r.json() : null)
      .then(data => { if (data) setTemplate({ ...defaultTemplate, ...data }); setLoading(false) })
      .catch(() => setLoading(false))
  }, [token])

  const handleSave = async () => {
    setSaving(true)
    try {
      const res = await fetch(apiUrl('/api/impressao/template'), {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ ...template, paperFormat })
      })
      alert(res.ok ? '✅ Template salvo com sucesso' : '❌ Erro ao salvar')
    } catch { alert('❌ Erro ao salvar') }
    setSaving(false)
  }

  const updateEmpresa = (field, value) => setTemplate(prev => ({ ...prev, empresa: { ...prev.empresa, [field]: value } }))
  const updateCabecalho = (field, value) => setTemplate(prev => ({ ...prev, cabecalho: { ...prev.cabecalho, [field]: value } }))
  const updateField = (section, field, value) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: value } }))
  const updateSize = (field, value) => setTemplate(prev => ({ ...prev, tamanhos: { ...prev.tamanhos, [field]: value } }))
  const toggleField = (section, field) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: !prev[section]?.[field] } }))

  const handleLogoUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => setTemplate(prev => ({ ...prev, logo: { ...prev.logo, base64: ev.target.result, enabled: true } }))
    reader.readAsDataURL(file)
  }

  if (loading) return <div className="glass p-12 text-center"><Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" /><p className="text-gray-400">Carregando template...</p></div>

  const tabs = [
    { id: 'empresa', label: 'Empresa', icon: Building },
    { id: 'logo', label: 'Logo', icon: Image },
    { id: 'cabecalho', label: 'Cabeçalho', icon: Edit3 },
    { id: 'itens', label: 'Itens', icon: List },
    { id: 'rodape', label: 'Rodapé', icon: List },
    { id: 'estilo', label: 'Estilo', icon: Palette },
    { id: 'tamanhos', label: 'Tamanhos', icon: Type },
  ]

  return (
    <div className="space-y-4">
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

      {/* Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        {tabs.map(t => (
          <button key={t.id} onClick={() => setActiveTab(t.id)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all whitespace-nowrap flex items-center gap-2 ${activeTab === t.id ? 'bg-blue-600 text-white' : 'glass text-gray-400 hover:text-white'}`}>
            <t.icon size={14} /> {t.label}
          </button>
        ))}
      </div>

      {/* Editor */}
      <div className="space-y-3">
        {/* Empresa */}
        {activeTab === 'empresa' && (
          <SectionCard title="Dados da Empresa" icon={Building}>
            <div className="space-y-4">
              <div>
                <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Building size={12} /> Nome da Empresa</label>
                <input type="text" value={template.empresa?.nome || ''} onChange={e => updateEmpresa('nome', e.target.value)} className="input-field text-sm" placeholder="Nome da empresa" />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Hash size={12} /> CNPJ</label>
                  <input type="text" value={template.empresa?.cnpj || ''} onChange={e => updateEmpresa('cnpj', e.target.value)} className="input-field text-sm" placeholder="00.000.000/0000-00" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Hash size={12} /> Inscrição Estadual (IE)</label>
                  <input type="text" value={template.empresa?.ie || ''} onChange={e => updateEmpresa('ie', e.target.value)} className="input-field text-sm" placeholder="000.000.000" />
                </div>
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><MapPin size={12} /> Endereço</label>
                <input type="text" value={template.empresa?.endereco || ''} onChange={e => updateEmpresa('endereco', e.target.value)} className="input-field text-sm" placeholder="Rua, número, bairro" />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><MapPin size={12} /> Cidade/UF</label>
                  <input type="text" value={template.empresa?.cidade || ''} onChange={e => updateEmpresa('cidade', e.target.value)} className="input-field text-sm" placeholder="Cidade - UF" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Phone size={12} /> Telefone</label>
                  <input type="text" value={template.empresa?.telefone || ''} onChange={e => updateEmpresa('telefone', e.target.value)} className="input-field text-sm" placeholder="(00) 00000-0000" />
                </div>
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Mail size={12} /> E-mail</label>
                <input type="text" value={template.empresa?.email || ''} onChange={e => updateEmpresa('email', e.target.value)} className="input-field text-sm" placeholder="contato@empresa.com" />
              </div>
            </div>
          </SectionCard>
        )}

        {/* Logo */}
        {activeTab === 'logo' && (
          <SectionCard title="Logo do Comprovante" icon={Image}>
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-gray-300">Exibir Logo</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" checked={template.logo?.enabled ?? false} onChange={() => updateField('logo', 'enabled', !template.logo?.enabled)} className="sr-only peer" />
                <div className="w-11 h-6 bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-500/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
            {template.logo?.enabled && (
              <div className="space-y-4">
                <RangeSlider label="Largura da Logo" value={template.logo?.width || 120} onChange={v => updateField('logo', 'width', v)} min={60} max={200} />
                <RangeSlider label="Altura da Logo" value={template.logo?.height || 60} onChange={v => updateField('logo', 'height', v)} min={30} max={120} />
                <RangeSlider label="Espaço Acima" value={template.logo?.spacingTop || 10} onChange={v => updateField('logo', 'spacingTop', v)} min={0} max={30} />
                <RangeSlider label="Espaço Abaixo" value={template.logo?.spacingBottom || 10} onChange={v => updateField('logo', 'spacingBottom', v)} min={0} max={30} />
                {template.logo?.base64 ? (
                  <div className="border border-gray-600 rounded-lg p-4 text-center">
                    <img src={template.logo.base64} alt="Logo" className="max-h-24 mx-auto mb-3 rounded" />
                    <div className="flex gap-2 justify-center">
                      <label className="px-3 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm text-white cursor-pointer transition-all flex items-center gap-2">
                        <Upload size={14} /> Trocar
                        <input type="file" accept="image/*" onChange={handleLogoUpload} className="hidden" />
                      </label>
                      <button onClick={() => updateField('logo', 'base64', null)} className="px-3 py-2 bg-red-600/20 hover:bg-red-600/30 border border-red-500/20 text-red-400 rounded-lg text-sm transition-all flex items-center gap-2">
                        <Trash2 size={14} /> Remover
                      </button>
                    </div>
                  </div>
                ) : (
                  <label className="flex items-center justify-center gap-2 px-4 py-6 bg-gray-800 hover:bg-gray-700 border-2 border-dashed border-gray-600 rounded-lg cursor-pointer text-gray-400 transition-all hover:text-white">
                    <Upload size={24} />
                    <div>
                      <div className="text-sm font-medium">Upload Logo</div>
                      <div className="text-xs text-gray-500">PNG, JPG ou GIF • Máx 2MB</div>
                    </div>
                    <input type="file" accept="image/*" onChange={handleLogoUpload} className="hidden" />
                  </label>
                )}
              </div>
            )}
          </SectionCard>
        )}

        {/* Cabeçalho */}
        {activeTab === 'cabecalho' && (
          <SectionCard title="Título e Subtítulo do Comprovante" icon={Edit3}>
            <div className="space-y-4">
              <div>
                <label className="block text-xs text-gray-400 mb-1">Título Principal</label>
                <input type="text" value={template.cabecalho?.titulo || ''} onChange={e => updateCabecalho('titulo', e.target.value)} className="input-field text-sm" placeholder="COMPROVANTE DE VENDA" />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">Subtítulo (opcional)</label>
                <input type="text" value={template.cabecalho?.subtitulo || ''} onChange={e => updateCabecalho('subtitulo', e.target.value)} className="input-field text-sm" placeholder="Ex: Via do Cliente" />
              </div>
            </div>
          </SectionCard>
        )}

        {/* Itens */}
        {activeTab === 'itens' && (
          <SectionCard title="Campos dos Itens" icon={List}>
            <div className="grid grid-cols-2 gap-2">
              {[['nome', 'Nome do Produto'], ['quantidade', 'Quantidade'], ['valorUnitario', 'Valor Unitário'], ['valorTotal', 'Subtotal do Item'], ['separador', 'Linha Separadora']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.itens?.[key] ?? true} onChange={() => toggleField('itens', key)} className="rounded" />
                  <span className="text-sm text-gray-300">{label}</span>
                </label>
              ))}
            </div>
            <div className="mt-4 pt-4 border-t border-white/5">
              <h4 className="text-sm font-medium text-gray-300 mb-3">Campos Adicionais</h4>
              <div className="grid grid-cols-2 gap-2">
                {[['subtotal', 'Subtotal'], ['desconto', 'Desconto'], ['total', 'TOTAL'], ['formaPagamento', 'Forma de Pagamento'], ['valorRecebido', 'Valor Recebido'], ['troco', 'Troco'], ['numeroVenda', 'Número da Venda'], ['dataHora', 'Data/Hora']].map(([key, label]) => (
                  <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                    <input type="checkbox" checked={template.adicionais?.[key] ?? true} onChange={() => toggleField('adicionais', key)} className="rounded" />
                    <span className="text-sm text-gray-300">{label}</span>
                  </label>
                ))}
              </div>
            </div>
          </SectionCard>
        )}

        {/* Rodapé */}
        {activeTab === 'rodape' && (
          <SectionCard title="Mensagens do Rodapé" icon={List}>
            <div className="space-y-3">
              {[1, 2, 3, 4].map(n => (
                <div key={n}>
                  <label className="block text-xs text-gray-400 mb-1">Linha {n}</label>
                  <input type="text" value={template.rodape?.[`linha${n}`] || ''} onChange={e => updateField('rodape', `linha${n}`, e.target.value)} className="input-field text-sm" placeholder={`Mensagem ${n}`} />
                </div>
              ))}
            </div>
          </SectionCard>
        )}

        {/* Estilo */}
        {activeTab === 'estilo' && (
          <SectionCard title="Estilo e Alinhamento" icon={Palette}>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs text-gray-400 mb-2">Alinhamento</label>
                <div className="flex gap-2">
                  {[{ value: 'esquerda', icon: AlignLeft }, { value: 'centro', icon: AlignCenter }, { value: 'direita', icon: AlignRight }].map(({ value, icon: Icon }) => (
                    <button key={value} onClick={() => updateField('estilo', 'alinhamento', value)}
                      className={`flex-1 p-3 rounded-lg border transition-all ${template.estilo?.alinhamento === value ? 'bg-blue-600/20 border-blue-500 text-blue-400' : 'border-gray-700 text-gray-400 hover:bg-white/5'}`}>
                      <Icon size={18} className="mx-auto" />
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-2">Tamanho da Fonte</label>
                <DarkSelect value={template.estilo?.tamanhoFonte || 'medio'} onChange={v => updateField('estilo', 'tamanhoFonte', v)} options={[{ value: 'pequeno', label: 'Pequeno' }, { value: 'medio', label: 'Médio' }, { value: 'grande', label: 'Grande' }]} className="w-full" />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-2">Espaço entre Linhas</label>
                <RangeSlider label="" value={template.estilo?.espacoEntreLinhas || 8} onChange={v => updateField('estilo', 'espacoEntreLinhas', v)} min={2} max={16} />
              </div>
            </div>
          </SectionCard>
        )}

        {/* Tamanhos */}
        {activeTab === 'tamanhos' && (
          <SectionCard title="Tamanhos dos Títulos" icon={Type}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <RangeSlider label="Nome da Empresa" value={template.tamanhos?.empresa || 28} onChange={v => updateSize('empresa', v)} min={16} max={48} />
              <RangeSlider label="Título Principal" value={template.tamanhos?.titulo || 32} onChange={v => updateSize('titulo', v)} min={20} max={48} />
              <RangeSlider label="Subtítulo" value={template.tamanhos?.subtitulo || 24} onChange={v => updateSize('subtitulo', v)} min={16} max={36} />
              <RangeSlider label="Corpo do Texto" value={template.tamanhos?.corpo || 22} onChange={v => updateSize('corpo', v)} min={14} max={32} />
              <RangeSlider label="Rodapé" value={template.tamanhos?.rodape || 20} onChange={v => updateSize('rodape', v)} min={12} max={28} />
            </div>
          </SectionCard>
        )}
      </div>

      {/* Preview */}
      {showPreview && (
        <div className="glass rounded-xl p-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
              <Eye size={14} /> Preview do Comprovante
            </h3>
            <div className="flex items-center gap-3">
              <span className="text-xs text-gray-500">Formato:</span>
              <DarkSelect value={paperFormat} onChange={setPaperFormat} options={Object.entries(PAPER_FORMATS).map(([k, v]) => ({ value: k, label: v.label }))} className="w-40" />
            </div>
          </div>
          <div className="bg-gray-900 p-6 rounded-lg border border-gray-700 flex justify-center overflow-x-auto">
            <PreviewComprovante template={template} paperFormat={paperFormat} />
          </div>
        </div>
      )}
    </div>
  )
}