import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, RotateCw, Image, Building2, List, Eye } from 'lucide-react'

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

function PreviewComprovante({ template }) {
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

  const fontSize = { pequeno: 10, medio: 12, grande: 14 }
  const fs = fontSize[template.estilo?.tamanhoFonte] || 12
  const lineHeight = template.estilo?.espacoEntreLinhas || 8

  return (
    <div className="bg-white text-black p-4 rounded-lg font-mono" style={{ fontSize: fs, lineHeight: lineHeight }}>
      {template.logo?.enabled && (
        <div className="text-center mb-2" style={{ marginBottom: template.logo.spacingTop }}>
          <div className="bg-gray-200 h-12 mx-auto flex items-center justify-center rounded">
            <span className="text-gray-500 text-xs">[LOGO]</span>
          </div>
        </div>
      )}
      
      {template.cabecalho?.nomeEmpresa && <div className="text-center font-bold">Rodrigo Dev MT</div>}
      {template.cabecalho?.cnpj && <div className="text-center">CNPJ 12.345.678/0001-90</div>}
      {template.cabecalho?.endereco && <div className="text-center">Rua Exemplo, 123 - Centro</div>}
      {template.cabecalho?.telefone && <div className="text-center">(45) 99999-9999</div>}
      
      <div className="text-center mt-2 border-t border-dashed border-gray-400 pt-2">
        COMPROVANTE DE VENDA
      </div>
      
      {template.adicionais?.numeroVenda && <div>Nr: {exampleVenda.id.toString().padStart(6, '0')}</div>}
      {template.adicionais?.dataHora && <div>DATA: {exampleVenda.data}</div>}
      
      <div className="mt-2 border-t border-dashed border-gray-400 pt-2">
        <div className="font-bold">ITENS:</div>
        {exampleVenda.itens.map((item, i) => (
          <div key={i} className="mt-1">
            {template.itens?.nome && <div>{item.nome}</div>}
            {template.itens?.separador && <div>----------------------------</div>}
            {(template.itens?.quantidade || template.itens?.valorUnitario || template.itens?.valorTotal) && (
              <div>
                {template.itens?.quantidade && `QTD: ${item.qtd}`}
                {template.itens?.valorUnitario && ` x R$ ${item.valorUnit.toFixed(2)}`}
                {template.itens?.valorTotal && ` = R$ ${item.total.toFixed(2)}`}
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="mt-2 pt-2 border-t border-dashed border-gray-400">
        {template.adicionais?.subtotal && <div className="flex justify-between"><span>SUBTOTAL:</span><span>R$ {exampleVenda.subtotal.toFixed(2)}</span></div>}
        {template.adicionais?.desconto && <div className="flex justify-between"><span>DESCONTO:</span><span>R$ {exampleVenda.desconto.toFixed(2)}</span></div>}
        <div className="flex justify-between font-bold border-t border-gray-600 pt-1 mt-1">TOTAL: R$ {exampleVenda.total.toFixed(2)}</div>
      </div>

      <div className="mt-2 pt-2 border-t border-dashed border-gray-400">
        {template.adicionais?.formaPagamento && <div>FORMA DE PAGAMENTO: {exampleVenda.formaPagamento}</div>}
        {template.adicionais?.valorRecebido && <div>VALOR RECEBIDO: R$ {exampleVenda.valorRecebido.toFixed(2)}</div>}
        {template.adicionais?.troco && <div>TROCO: R$ {exampleVenda.troco.toFixed(2)}</div>}
      </div>

      <div className="mt-4 pt-2 border-t border-dashed border-gray-400 text-center">
        {template.rodape?.linha1 && <div>{template.rodape.linha1}</div>}
        {template.rodape?.linha2 && <div>{template.rodape.linha2}</div>}
        {template.rodape?.linha3 && <div>{template.rodape.linha3}</div>}
        {template.rodape?.linha4 && <div>{template.rodape.linha4}</div>}
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
        body: JSON.stringify(template)
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
              <h3 className="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
                <Eye size={14} /> Preview do Comprovante
              </h3>
              <PreviewComprovante template={template} />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}