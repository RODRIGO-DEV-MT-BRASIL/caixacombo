import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, Image, Building2, List, Eye, ChevronDown, X } from 'lucide-react'

const PAPER_FORMATS = {
  '80mm': { label: '80mm Padrão', width: 200 },
  '58mm': { label: '58mm Pequeno', width: 145 },
  '76mm': { label: '76mm', width: 190 },
  '57mm': { label: '57mm', width: 142 },
}

const defaultTemplate = {
  cabecalho: { nomeEmpresa: true, cnpj: true, endereco: true, telefone: true, email: true, cidade: true },
  logo: { enabled: false, width: 120, height: 60, spacingTop: 10, spacingBottom: 10 },
  itens: { nome: true, quantidade: true, valorUnitario: true, valorTotal: true, separador: true },
  adicionais: { subtotal: true, desconto: true, total: true, formaPagamento: true, valorRecebido: true, troco: true, numeroVenda: true, dataHora: true },
  rodape: { linha1: 'Agradecemos sua vinda', linha2: 'Volte sempre', linha3: '', linha4: '' },
  estilo: { alinhamento: 'centro', tamanhoFonte: 'medio', espacoEntreLinhas: 8 }
}

const fieldLabels = { nomeEmpresa: 'Nome da Empresa', cnpj: 'CNPJ', endereco: 'Endereço', telefone: 'Telefone', email: 'E-mail', cidade: 'Cidade/UF' }
const itemLabels = { nome: 'Nome do Produto', quantidade: 'Quantidade', valorUnitario: 'Valor Unitário', valorTotal: 'Subtotal do Item', separador: 'Linha Separadora' }
const adicionalLabels = { subtotal: 'Subtotal', desconto: 'Desconto', total: 'TOTAL', formaPagamento: 'Forma de Pagamento', valorRecebido: 'Valor Recebido', troco: 'Troco', numeroVenda: 'Número da Venda', dataHora: 'Data/Hora' }

function PreviewComprovante({ template, paperFormat = '80mm' }) {
  const exampleVenda = { id: 123, data: '19/05/2026 15:45', itens: [{ nome: 'Coca-Cola 350ml', qtd: 2, valorUnit: 3.00, total: 6.00 }, { nome: 'Sanduíche Natural', qtd: 1, valorUnit: 8.00, total: 8.00 }], subtotal: 14.00, desconto: 0.00, total: 14.00, formaPagamento: 'DINHEIRO', valorRecebido: 20.00, troco: 6.00 }
  const fs = { pequeno: 9, medio: 11, grande: 13 }[template.estilo?.tamanhoFonte] || 11
  const paperWidth = PAPER_FORMATS[paperFormat]?.width || 200
  const alignClass = { esquerda: 'text-left', direita: 'text-right', centro: 'text-center' }[template.estilo?.alinhamento] || 'text-center'

  return (
    <div className="bg-white text-black rounded font-mono overflow-hidden" style={{ width: paperWidth, fontSize: fs, lineHeight: 1.4, padding: '10px 6px' }}>
      {template.logo?.enabled && (
        <div className="flex justify-center mb-1">
          <div className="bg-gray-200 flex items-center justify-center rounded text-gray-500" style={{ width: template.logo.width, height: template.logo.height, fontSize: fs - 2 }}>[LOGO]</div>
        </div>
      )}
      <div className={alignClass}>
        {template.cabecalho?.nomeEmpresa && <div className="font-bold">Rodrigo Dev MT</div>}
        {template.cabecalho?.cnpj && <div>CNPJ 12.345.678/0001-90</div>}
        {template.cabecalho?.endereco && <div>Rua Exemplo, 123 - Centro</div>}
        {template.cabecalho?.telefone && <div>(45) 99999-9999</div>}
      </div>
      <div className={`${alignClass} my-2 border-t border-b border-dashed border-gray-400 py-1`}>COMPROVANTE DE VENDA</div>
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
        {template.rodape?.linha1 && <div>{template.rodape.linha1}</div>}
        {template.rodape?.linha2 && <div>{template.rodape.linha2}</div>}
        {template.rodape?.linha3 && <div>{template.rodape.linha3}</div>}
        {template.rodape?.linha4 && <div>{template.rodape.linha4}</div>}
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

export default function ConfiguracoesImpressao() {
  const { token, user } = useAuth()
  const [template, setTemplate] = useState(defaultTemplate)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('cabecalho')
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

  const updateField = (section, field, value) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: value } }))
  const toggleField = (section, field) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: !prev[section]?.[field] } }))

  const handleLogoUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (ev) => setTemplate(prev => ({ ...prev, logo: { ...prev.logo, base64: ev.target.result } }))
    reader.readAsDataURL(file)
  }

  if (loading) return <div className="glass p-12 text-center"><Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" /><p className="text-gray-400">Carregando template...</p></div>

  const tabs = [
    { id: 'cabecalho', label: 'Cabeçalho', icon: Building2 },
    { id: 'logo', label: 'Logo', icon: Image },
    { id: 'itens', label: 'Itens', icon: List },
    { id: 'adicional', label: 'Adicionais', icon: List },
    { id: 'rodape', label: 'Rodapé', icon: List },
    { id: 'estilo', label: 'Estilo', icon: List },
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
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all whitespace-nowrap ${activeTab === t.id ? 'bg-blue-600 text-white' : 'glass text-gray-400 hover:text-white'}`}>
            <t.icon size={14} className="inline mr-1" /> {t.label}
          </button>
        ))}
      </div>

      {/* Editor */}
      <div className="glass p-4">
        {/* Cabeçalho */}
        {activeTab === 'cabecalho' && (
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-300">Campos do Cabeçalho</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {Object.entries(fieldLabels).map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.cabecalho?.[key] ?? true} onChange={() => toggleField('cabecalho', key)} className="rounded" />
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
                <input type="checkbox" checked={template.logo?.enabled ?? false} onChange={() => updateField('logo', 'enabled', !template.logo?.enabled)} className="rounded" />
                <span className="text-sm text-gray-300">Ativar Logo</span>
              </label>
            </div>
            {template.logo?.enabled && (
              <>
                <div className="grid grid-cols-2 gap-4">
                  <div><label className="block text-xs text-gray-400 mb-1">Largura (px)</label><input type="number" value={template.logo?.width || 120} onChange={e => updateField('logo', 'width', parseInt(e.target.value) || 120)} className="input-field text-sm" /></div>
                  <div><label className="block text-xs text-gray-400 mb-1">Altura (px)</label><input type="number" value={template.logo?.height || 60} onChange={e => updateField('logo', 'height', parseInt(e.target.value) || 60)} className="input-field text-sm" /></div>
                  <div><label className="block text-xs text-gray-400 mb-1">Espaço Acima (px)</label><input type="number" value={template.logo?.spacingTop || 10} onChange={e => updateField('logo', 'spacingTop', parseInt(e.target.value) || 10)} className="input-field text-sm" /></div>
                  <div><label className="block text-xs text-gray-400 mb-1">Espaço Abaixo (px)</label><input type="number" value={template.logo?.spacingBottom || 10} onChange={e => updateField('logo', 'spacingBottom', parseInt(e.target.value) || 10)} className="input-field text-sm" /></div>
                </div>
                {template.logo?.base64 ? (
                  <div className="border border-gray-600 rounded-lg p-4 text-center">
                    <img src={template.logo.base64} alt="Logo" className="max-h-20 mx-auto mb-2" />
                    <button onClick={() => updateField('logo', 'base64', null)} className="text-red-400 text-xs hover:underline">Remover</button>
                  </div>
                ) : (
                  <label className="btn-ghost cursor-pointer text-sm"><Image size={14} className="inline mr-1" /> Upload Logo<input type="file" accept="image/*" onChange={handleLogoUpload} className="hidden" /></label>
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
                  <input type="checkbox" checked={template.itens?.[key] ?? true} onChange={() => toggleField('itens', key)} className="rounded" />
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
                  <input type="checkbox" checked={template.adicionais?.[key] ?? true} onChange={() => toggleField('adicionais', key)} className="rounded" />
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
                <input type="text" value={template.rodape?.[`linha${n}`] || ''} onChange={e => updateField('rodape', `linha${n}`, e.target.value)} className="input-field text-sm" placeholder={`Mensagem ${n} (opcional)`} />
              </div>
            ))}
          </div>
        )}

        {/* Estilo */}
        {activeTab === 'estilo' && (
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-gray-300">Estilo do Comprovante</h3>
            <div className="flex flex-wrap gap-4">
              <div>
                <label className="block text-xs text-gray-400 mb-1">Alinhamento</label>
                <DarkSelect value={template.estilo?.alinhamento || 'centro'} onChange={v => updateField('estilo', 'alinhamento', v)} options={[{ value: 'centro', label: 'Centro' }, { value: 'esquerda', label: 'Esquerda' }, { value: 'direita', label: 'Direita' }]} className="w-36" />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">Tamanho da Fonte</label>
                <DarkSelect value={template.estilo?.tamanhoFonte || 'medio'} onChange={v => updateField('estilo', 'tamanhoFonte', v)} options={[{ value: 'pequeno', label: 'Pequeno' }, { value: 'medio', label: 'Médio' }, { value: 'grande', label: 'Grande' }]} className="w-36" />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">Espaço entre Linhas</label>
                <input type="number" value={template.estilo?.espacoEntreLinhas || 8} onChange={e => updateField('estilo', 'espacoEntreLinhas', parseInt(e.target.value) || 8)} className="input-field text-sm w-24" />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Preview - abaixo do editor */}
      {showPreview && (
        <div className="glass p-4">
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