import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, Image, Building2, List, Eye, ChevronDown, Type, Ruler, Palette, AlignLeft, AlignCenter, AlignRight, Upload, Trash2, Edit3, Phone, Mail, MapPin, Hash, Building, DollarSign, Receipt, CreditCard, QrCode, LayoutGrid, ShoppingBag, Package, Search, CheckCircle } from 'lucide-react'
import { useToast } from '../components/Toast'

const PAPER_FORMATS = {
  '80mm': { label: '80mm Padrão', width: 200 },
  '58mm': { label: '58mm Pequeno', width: 145 },
  '76mm': { label: '76mm', width: 190 },
  '57mm': { label: '57mm', width: 142 },
}

const defaultTemplate = {
  empresa: { nome: 'Rodrigo Dev MT', cnpj: '12.345.678/0001-90', ie: '', inscricaoMunicipal: '', endereco: '', telefone: '', email: '', cidade: '', cep: '' },
  logo: { enabled: false, width: 120, height: 60, spacingTop: 10, spacingBottom: 10, base64: null },
  
  cabecalho: { titulo: 'COMPROVANTE DE VENDA', subtitulo: '' },
  
  modelos: {
    abertura: { ativo: true, titulo: 'ABERTURA DE CAIXA', campos: { operador: true, data: true, hora: true, valorInicial: true } },
    fechamento: { ativo: true, titulo: 'FECHAMENTO DE CAIXA', campos: { operador: true, data: true, hora: true, totalVendas: true, totalSangrias: true, saldo: true } },
    sangria: { ativo: true, titulo: 'SANGRIA', campos: { operador: true, data: true, hora: true, valor: true, motivo: true, saldo: true } },
    suprimento: { ativo: true, titulo: 'SUPRIMENTO', campos: { operador: true, data: true, hora: true, valor: true, motivo: true, saldo: true } },
    venda: { ativo: true, titulo: 'COMPROVANTE DE VENDA', campos: { itens: true, quantidade: true, valorUnitario: true, valorTotal: true } },
    nfce: { ativo: false, titulo: 'DADOS DA NFC-e', numeroNFCe: '', serie: '001', campos: { itens: true, un: true, quantidade: true, valorUnitario: true, valorTotal: true, cpf: true } }
  },
  
  itens: { nome: true, quantidade: true, valorUnitario: true, valorTotal: true, separador: true, un: true },
  
  adicionais: { subtotal: true, desconto: true, total: true, formaPagamento: true, valorRecebido: true, troco: true, numeroVenda: true, dataHora: true, hora: true },
  
  pagamento: { dinheiro: true, pix: true, credito: true, debito: true, troco: true, valorTotal: true, cpfCnpj: false },
  
  imposto: { ativo: false, Tributos: true, icms: true, pis: true, cofins: true, ibnpt: true, totalTributos: 12 },
  
  qrCode: { ativo: false, url: '', csc: '', cscId: '000001' },
  
  rodape: { linha1: 'Agradecemos sua vinda', linha2: 'Volte sempre', linha3: '', linha4: '' },
  
  estilo: { alinhamento: 'centro', tamanhoFonte: 'medio', espacoEntreLinhas: 8 },

  designApp: { tipo: 'mercado' },

  tamanhos: {
    empresa: 28, titulo: 32, subtitulo: 24, corpo: 22,
    numeroNFCe: 18, serie: 16, dataHora: 18,
    itensTitulo: 20, itensNome: 20, itensValores: 16,
    subtotal: 18, desconto: 18, total: 24,
    formaPagamento: 18, valorRecebido: 18, troco: 18,
    tributos: 14, qrCode: 12, rodape: 20
  }
}

function PreviewComprovante({ template, paperFormat = '80mm', tipo = 'venda' }) {
  const fs = { pequeno: 9, medio: 11, grande: 13 }[template.estilo?.tamanhoFonte] || 11
  const t = template.tamanhos || {}
  const paperWidth = PAPER_FORMATS[paperFormat]?.width || 200
  const alignClass = { esquerda: 'text-left', direita: 'text-right', centro: 'text-center' }[template.estilo?.alinhamento] || 'text-center'
  
  const modelos = template.modelos || {}
  const tituloModelo = modelos[tipo]?.titulo || modelos.venda?.titulo || 'COMPROVANTE'

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
      
      {/* Cabeçalho da Empresa */}
      <div className={alignClass}>
        <div className="font-bold" style={{ fontSize: (t.empresa || 28) * 0.4 }}>{template.empresa?.nome || 'Empresa'}</div>
        {template.empresa?.cnpj && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>CNPJ: {template.empresa.cnpj}</div>}
        {template.empresa?.ie && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>IE: {template.empresa.ie}</div>}
        {template.empresa?.inscricaoMunicipal && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>IM: {template.empresa.inscricaoMunicipal}</div>}
        {template.empresa?.endereco && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.endereco}</div>}
        {template.empresa?.cidade && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.cidade}{template.empresa?.cep ? ` - ${template.empresa.cep}` : ''}</div>}
        {template.empresa?.telefone && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>FONE: {template.empresa.telefone}</div>}
        {template.empresa?.email && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.email}</div>}
      </div>
      
      {/* Título do Modelo */}
      <div className={`${alignClass} my-2 border-t border-b border-dashed border-gray-400 py-1`} style={{ fontSize: (t.titulo || 32) * 0.4 }}>
        {tituloModelo}
      </div>
      
      {/* Conteúdo baseado no tipo */}
      {tipo === 'venda' && (
        <>
          <div className={alignClass}>
            {template.adicionais?.numeroVenda && <div style={{ fontSize: (t.numeroNFCe || 18) * 0.38 }}>Nr: 000123</div>}
            {template.adicionais?.dataHora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: 19/05/2026{template.adicionais?.hora ? ' 15:45:00' : ''}</div>}
          </div>
          <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
            <div className="font-bold" style={{ fontSize: (t.itensTitulo || 20) * 0.4 }}>ITENS:</div>
            {[{ nome: 'Coca-Cola 350ml', un: 'UN', qtd: 2, valorUnit: 3.00, total: 6.00 }, { nome: 'Sanduíche Natural', un: 'UN', qtd: 1, valorUnit: 8.00, total: 8.00 }].map((item, i) => (
              <div key={i} className="mt-1">
                {template.itens?.nome && <div style={{ fontSize: (t.itensNome || 20) * 0.38 }}>{item.nome}</div>}
                {template.itens?.separador && <div>----------------------------</div>}
                <div style={{ fontSize: (t.itensValores || 16) * 0.38 }}>
                  {template.itens?.un && <span>{item.un} </span>}
                  {template.itens?.quantidade && `${item.qtd}`}
                  {template.itens?.valorUnitario && ` x R$ ${item.valorUnit.toFixed(2)}`}
                  {template.itens?.valorTotal && ` R$ ${item.total.toFixed(2)}`}
                </div>
              </div>
            ))}
          </div>
          <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
            {template.adicionais?.subtotal && <div className="flex justify-between" style={{ fontSize: (t.subtotal || 18) * 0.38 }}><span>Subtotal:</span><span>R$ 14,00</span></div>}
            {template.adicionais?.desconto && <div className="flex justify-between" style={{ fontSize: (t.desconto || 18) * 0.38 }}><span>Desconto:</span><span>R$ 0,00</span></div>}
            <div className="flex justify-between font-bold border-t border-gray-600 mt-1 pt-1" style={{ fontSize: (t.total || 24) * 0.4 }}><span>TOTAL:</span><span>R$ 14,00</span></div>
          </div>
          <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
            {template.adicionais?.formaPagamento && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>FORMA: DINHEIRO</div>}
            {template.adicionais?.valorRecebido && <div style={{ fontSize: (t.valorRecebido || 18) * 0.38 }}>RECEBIDO: R$ 20,00</div>}
            {template.adicionais?.troco && <div style={{ fontSize: (t.troco || 18) * 0.38 }}>TROCO: R$ 6,00</div>}
          </div>
        </>
      )}
      
      {tipo === 'abertura' && (
        <div className={alignClass}>
          {modelos.abertura?.campos?.operador && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>Operador: Fulano</div>}
          {modelos.abertura?.campos?.data && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: 19/05/2026</div>}
          {modelos.abertura?.campos?.hora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>HORA: 08:00</div>}
          {modelos.abertura?.campos?.valorInicial && <div style={{ fontSize: (t.total || 24) * 0.4 }}>VALOR INICIAL: R$ 100,00</div>}
        </div>
      )}
      
      {tipo === 'fechamento' && (
        <div className={alignClass}>
          {modelos.fechamento?.campos?.operador && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>Operador: Fulano</div>}
          {modelos.fechamento?.campos?.data && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: 19/05/2026</div>}
          {modelos.fechamento?.campos?.totalVendas && <div style={{ fontSize: (t.total || 24) * 0.4 }}>TOTAL VENDAS: R$ 1.234,56</div>}
          {modelos.fechamento?.campos?.totalSangrias && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>TOTAL SANGRIA: R$ 50,00</div>}
          {modelos.fechamento?.campos?.saldo && <div style={{ fontSize: (t.total || 24) * 0.4 }}>SALDO CAIXA: R$ 1.284,56</div>}
        </div>
      )}
      
      {tipo === 'sangria' && (
        <div className={alignClass}>
          {modelos.sangria?.campos?.operador && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>Operador: Fulano</div>}
          {modelos.sangria?.campos?.data && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: 19/05/2026</div>}
          {modelos.sangria?.campos?.hora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>HORA: 14:30</div>}
          {modelos.sangria?.campos?.valor && <div style={{ fontSize: (t.total || 24) * 0.4 }}>VALOR RETIRADO: R$ 50,00</div>}
          {modelos.sangria?.campos?.motivo && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>MOTIVO: Pagamento fornecedor</div>}
          {modelos.sangria?.campos?.saldo && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>SALDO RESTANTE: R$ 1.234,56</div>}
        </div>
      )}
      
      {tipo === 'suprimento' && (
        <div className={alignClass}>
          {modelos.suprimento?.campos?.operador && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>Operador: Fulano</div>}
          {modelos.suprimento?.campos?.data && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: 19/05/2026</div>}
          {modelos.suprimento?.campos?.hora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>HORA: 10:00</div>}
          {modelos.suprimento?.campos?.valor && <div style={{ fontSize: (t.total || 24) * 0.4 }}>VALOR ADICIONADO: R$ 100,00</div>}
          {modelos.suprimento?.campos?.motivo && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>MOTIVO: Troco para início</div>}
          {modelos.suprimento?.campos?.saldo && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>SALDO ATUAL: R$ 200,00</div>}
        </div>
      )}

      {/* Impostos (se NFC-e ativo) */}
      {template.imposto?.ativo && template.imposto?.Tributos && (
        <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`} style={{ fontSize: (t.tributos || 14) * 0.38 }}>
          <div>Tributos Totais Incidentes (Lei 12.741/12)</div>
          {template.imposto?.ibnpt && <div>IBPT: R$ {(14 * template.imposto.totalTributos / 100).toFixed(2)}</div>}
        </div>
      )}

      {/* QR Code */}
      {template.qrCode?.ativo && (
        <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
          <div className="border border-gray-400 p-2 rounded flex items-center justify-center" style={{ fontSize: (t.qrCode || 12) * 0.38 }}>
            <div className="w-16 h-16 bg-gray-300 flex items-center justify-center">QR</div>
          </div>
          {template.qrCode?.csc && <div style={{ fontSize: (t.qrCode || 12) * 0.35 }}> CSC: {template.qrCode.csc}</div>}
        </div>
      )}

      {/* Rodapé */}
      <div className={`${alignClass} mt-3 pt-2 border-t border-dashed border-gray-400`}>
        {template.rodape?.linha1 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha1}</div>}
        {template.rodape?.linha2 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha2}</div>}
        {template.rodape?.linha3 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha3}</div>}
        {template.rodape?.linha4 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha4}</div>}
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
  const toast = useToast()
  const [template, setTemplate] = useState(defaultTemplate)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('empresa')
  const [showPreview, setShowPreview] = useState(true)
  const [paperFormat, setPaperFormat] = useState('80mm')
  const [previewTipo, setPreviewTipo] = useState('venda')

  const isAdmin = user?.role === 'admin'
  const isEmpresa = user?.role === 'empresa'

  useEffect(() => {
    fetch(apiUrl('/api/impressao/template'), { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.ok ? r.json() : null)
      .then(data => { if (data) setTemplate({ ...defaultTemplate, ...data }); setLoading(false) })
      .catch(() => setLoading(false))
  }, [token])

  const handleSave = async () => {
    if (!template?.empresa?.nome) {
      toast.error('Preencha o nome da empresa primeiro')
      return
    }
    setSaving(true)
    try {
      const url = apiUrl('/api/impressao/template')
      console.log('Salvando template para:', url, 'token:', token ? 'SIM' : 'NAO')
      const res = await fetch(url, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ ...template, paperFormat })
      })
      const data = await res.json()
      console.log('Resposta save:', res.status, JSON.stringify(data))
      if (res.ok) toast.success('Template salvo com sucesso')
      else toast.error(data.error || 'Erro ao salvar')
    } catch (e) { 
      console.error('Erro save:', e)
      toast.error('Erro ao salvar')
    }
    setSaving(false)
  }

  const updateEmpresa = (field, value) => setTemplate(prev => ({ ...prev, empresa: { ...prev.empresa, [field]: value } }))
  const updateModelo = (tipo, field, value) => setTemplate(prev => ({ ...prev, modelos: { ...prev.modelos, [tipo]: { ...prev.modelos?.[tipo], [field]: value } } }))
  const updateModeloCampo = (tipo, campo, ativo) => setTemplate(prev => ({ ...prev, modelos: { ...prev.modelos, [tipo]: { ...prev.modelos?.[tipo], campos: { ...prev.modelos?.[tipo]?.campos, [campo]: ativo } } } }))
  const updateField = (section, field, value) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: value } }))
  const updateSize = (field, value) => setTemplate(prev => ({ ...prev, tamanhos: { ...prev.tamanhos, [field]: value } }))
  const toggleField = (section, field) => setTemplate(prev => ({ ...prev, [section]: { ...prev[section], [field]: !prev[section]?.[field] } }))
  const toggleModelo = (tipo) => setTemplate(prev => ({ ...prev, modelos: { ...prev.modelos, [tipo]: { ...prev.modelos?.[tipo], ativo: !prev.modelos?.[tipo]?.ativo } } }))

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
    { id: 'modelos', label: 'Modelos', icon: Receipt },
    { id: 'logo', label: 'Logo', icon: Image },
    { id: 'itens', label: 'Itens', icon: List },
    { id: 'pagamento', label: 'Pagamento', icon: CreditCard },
    { id: 'nfce', label: 'NFC-e', icon: QrCode },
    { id: 'rodape', label: 'Rodapé', icon: List },
    { id: 'estilo', label: 'Estilo', icon: Palette },
    { id: 'tamanhos', label: 'Tamanhos', icon: Type },
    { id: 'design', label: 'Design App', icon: LayoutGrid },
  ]

  const modelosLista = [
    { key: 'venda', label: 'Venda', icon: ShoppingBag },
    { key: 'abertura', label: 'Abertura Caixa', icon: Receipt },
    { key: 'fechamento', label: 'Fechamento', icon: Receipt },
    { key: 'sangria', label: 'Sangria', icon: DollarSign },
    { key: 'suprimento', label: 'Suprimento', icon: DollarSign },
    { key: 'nfce', label: 'NFC-e', icon: QrCode },
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
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Hash size={12} /> Inscrição Municipal (IM)</label>
                  <input type="text" value={template.empresa?.inscricaoMunicipal || ''} onChange={e => updateEmpresa('inscricaoMunicipal', e.target.value)} className="input-field text-sm" placeholder="00000000" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><MapPin size={12} /> CEP</label>
                  <input type="text" value={template.empresa?.cep || ''} onChange={e => updateEmpresa('cep', e.target.value)} className="input-field text-sm" placeholder="00000-000" />
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

        {/* Modelos - Configuração de cada tipo de comprovante */}
        {activeTab === 'modelos' && (
          <SectionCard title="Configurar Modelos de Comprovante" icon={Receipt}>
            <div className="space-y-4">
              {modelosLista.map(({ key, label, icon: Icon }) => (
                <div key={key} className="p-4 bg-gray-800/50 rounded-xl">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <Icon size={16} className="text-blue-400" />
                      <span className="font-medium text-white">{label}</span>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" checked={template.modelos?.[key]?.ativo ?? true} onChange={() => toggleModelo(key)} className="sr-only peer" />
                      <div className="w-11 h-6 bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-500/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                    </label>
                  </div>
                  <div className="flex gap-2">
                    <input type="text" value={template.modelos?.[key]?.titulo || ''} onChange={e => updateModelo(key, 'titulo', e.target.value)} className="input-field text-sm flex-1" placeholder="Título do modelo" />
                  </div>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {['operador', 'data', 'hora', 'valor', 'motivo', 'saldo', 'totalVendas', 'totalSangrias', 'valorInicial'].map(campo => (
                      <label key={campo} className="flex items-center gap-1 px-2 py-1 rounded bg-gray-700/50 cursor-pointer">
                        <input type="checkbox" checked={template.modelos?.[key]?.campos?.[campo] ?? false} onChange={e => updateModeloCampo(key, campo, e.target.checked)} className="rounded" />
                        <span className="text-xs text-gray-300 capitalize">{campo.replace(/([A-Z])/g, ' $1').trim()}</span>
                      </label>
                    ))}
                  </div>
                </div>
              ))}
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

        {/* Itens (para vendas) */}
        {activeTab === 'itens' && (
          <SectionCard title="Campos dos Itens (Vendas/NFC-e)" icon={List}>
            <div className="grid grid-cols-2 gap-2">
              {[['nome', 'Nome do Produto'], ['un', 'Unidade (UN)'], ['quantidade', 'Quantidade'], ['valorUnitario', 'Valor Unitário'], ['valorTotal', 'Subtotal do Item'], ['separador', 'Linha Separadora']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.itens?.[key] ?? true} onChange={() => toggleField('itens', key)} className="rounded" />
                  <span className="text-sm text-gray-300">{label}</span>
                </label>
              ))}
            </div>
            <div className="mt-4 pt-4 border-t border-white/5">
              <h4 className="text-sm font-medium text-gray-300 mb-3">Campos Adicionais</h4>
              <div className="grid grid-cols-2 gap-2">
                {[['subtotal', 'Subtotal'], ['desconto', 'Desconto'], ['total', 'TOTAL'], ['numeroVenda', 'Número da Venda'], ['dataHora', 'Data/Hora'], ['hora', 'Hora']].map(([key, label]) => (
                  <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                    <input type="checkbox" checked={template.adicionais?.[key] ?? true} onChange={() => toggleField('adicionais', key)} className="rounded" />
                    <span className="text-sm text-gray-300">{label}</span>
                  </label>
                ))}
              </div>
            </div>
          </SectionCard>
        )}

        {/* Pagamento */}
        {activeTab === 'pagamento' && (
          <SectionCard title="Formas de Pagamento" icon={CreditCard}>
            <div className="grid grid-cols-2 gap-2">
              {[['dinheiro', 'Dinheiro'], ['pix', 'PIX'], ['credito', 'Crédito'], ['debito', 'Débito'], ['valorTotal', 'Valor Total'], ['troco', 'Troco'], ['cpfCnpj', 'CPF/CNPJ']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.pagamento?.[key] ?? true} onChange={() => toggleField('pagamento', key)} className="rounded" />
                  <span className="text-sm text-gray-300">{label}</span>
                </label>
              ))}
            </div>
          </SectionCard>
        )}

        {/* NFC-e */}
        {activeTab === 'nfce' && (
          <SectionCard title="NFC-e (Cupom Fiscal Eletrônico)" icon={QrCode}>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-300">Ativar NFC-e</span>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" checked={template.imposto?.ativo ?? false} onChange={() => updateField('imposto', 'ativo', !template.imposto?.ativo)} className="sr-only peer" />
                  <div className="w-11 h-6 bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-500/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </label>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1">CSC (Código de Segurança)</label>
                  <input type="text" value={template.qrCode?.csc || ''} onChange={e => updateField('qrCode', 'csc', e.target.value)} className="input-field text-sm" placeholder="Código de Segurança" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1">CSC ID</label>
                  <input type="text" value={template.qrCode?.cscId || '000001'} onChange={e => updateField('qrCode', 'cscId', e.target.value)} className="input-field text-sm" placeholder="000001" />
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-300">Exibir QR Code</span>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" checked={template.qrCode?.ativo ?? false} onChange={() => updateField('qrCode', 'ativo', !template.qrCode?.ativo)} className="sr-only peer" />
                  <div className="w-11 h-6 bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-500/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </label>
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">% Total Tributos (Lei 12.741/12)</label>
                <input type="number" value={template.imposto?.totalTributos || 12} onChange={e => updateField('imposto', 'totalTributos', parseFloat(e.target.value) || 0)} className="input-field text-sm" placeholder="12" />
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
          <SectionCard title="Tamanhos dos Textos" icon={Type}>
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <RangeSlider label="Nome da Empresa" value={template.tamanhos?.empresa || 28} onChange={v => updateSize('empresa', v)} min={16} max={48} />
                <RangeSlider label="Título" value={template.tamanhos?.titulo || 32} onChange={v => updateSize('titulo', v)} min={20} max={48} />
                <RangeSlider label="Corpo (dados)" value={template.tamanhos?.corpo || 22} onChange={v => updateSize('corpo', v)} min={14} max={32} />
                <RangeSlider label="Data/Hora" value={template.tamanhos?.dataHora || 18} onChange={v => updateSize('dataHora', v)} min={12} max={28} />
              </div>
              <div className="border-t border-white/10 pt-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <RangeSlider label="Itens - Nome" value={template.tamanhos?.itensNome || 20} onChange={v => updateSize('itensNome', v)} min={14} max={32} />
                  <RangeSlider label="Itens - Valores" value={template.tamanhos?.itensValores || 16} onChange={v => updateSize('itensValores', v)} min={12} max={28} />
                  <RangeSlider label="Subtotal/Desconto" value={template.tamanhos?.subtotal || 18} onChange={v => updateSize('subtotal', v)} min={12} max={28} />
                  <RangeSlider label="TOTAL (destaque)" value={template.tamanhos?.total || 24} onChange={v => updateSize('total', v)} min={16} max={40} />
                  <RangeSlider label="Forma Pagamento" value={template.tamanhos?.formaPagamento || 18} onChange={v => updateSize('formaPagamento', v)} min={12} max={28} />
                  <RangeSlider label="Troco" value={template.tamanhos?.troco || 18} onChange={v => updateSize('troco', v)} min={12} max={28} />
                </div>
              </div>
              <div className="border-t border-white/10 pt-4">
                <RangeSlider label="Rodapé" value={template.tamanhos?.rodape || 20} onChange={v => updateSize('rodape', v)} min={12} max={28} />
              </div>
            </div>
          </SectionCard>
        )}

        {/* Design App - Tela de Vendas */}
        {activeTab === 'design' && (
          <SectionCard title="Design da Tela de Vendas" icon={LayoutGrid}>
            <div className="space-y-4">
              <p className="text-xs text-gray-400">Escolha o layout da tela de vendas do app Android para esta empresa.</p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {[
                  { key: 'mercado', label: 'Mercado', desc: 'Grid de produtos grande + carrinho na lateral. Ideal para bares, restaurantes, fast-food.', cor: '#3b82f6' },
                  { key: 'master', label: 'Master', desc: 'Lista compacta vertical + teclado numérico. Ideal para minimercados, padarias, lojas.', cor: '#f59e0b' },
                  { key: 'premium', label: 'Premium', desc: 'Cards com imagem do produto + carrinho na parte inferior. Ideal para butiques, eletrônicos.', cor: '#8b5cf6' },
                ].map(design => (
                  <div key={design.key} onClick={() => updateField('designApp', 'tipo', design.key)}
                    className={`cursor-pointer rounded-xl border-2 transition-all p-4 ${template.designApp?.tipo === design.key ? 'border-blue-500 bg-blue-500/10' : 'border-gray-700 hover:border-gray-500'}`}>
                    <div className="flex items-center gap-2 mb-3">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ backgroundColor: design.cor }}>
                        {design.key === 'mercado' && <LayoutGrid size={16} className="text-white" />}
                        {design.key === 'master' && <List size={16} className="text-white" />}
                        {design.key === 'premium' && <ShoppingBag size={16} className="text-white" />}
                      </div>
                      <span className="font-bold text-white">{design.label}</span>
                      {template.designApp?.tipo === design.key && (
                        <CheckCircle size={16} className="text-blue-400 ml-auto" />
                      )}
                    </div>
                    <p className="text-xs text-gray-400 mb-4">{design.desc}</p>
                    {/* Mini preview do design */}
                    <div className="bg-gray-900 rounded-lg p-2 h-40 overflow-hidden relative">
                      {design.key === 'mercado' && (
                        <div className="flex h-full gap-1">
                          <div className="w-2/5 flex flex-col gap-1">
                            <div className="bg-purple-600/40 rounded h-6" />
                            <div className="grid grid-cols-2 gap-1 flex-1">
                              {[1,2,3,4].map(i => <div key={i} className="bg-gray-700 rounded h-6" />)}
                            </div>
                          </div>
                          <div className="w-3/5 flex flex-col gap-1">
                            <div className="flex gap-1 mb-1">
                              {[1,2,3].map(i => <div key={i} className="bg-gray-600 rounded h-4 flex-1" />)}
                            </div>
                            <div className="flex-1 space-y-1">
                              {[1,2,3,4].map(i => (
                                <div key={i} className="flex gap-1 items-center">
                                  <div className="bg-gray-500 rounded h-3 flex-1" />
                                  <div className="bg-gray-600 rounded h-3 w-6" />
                                  <div className="bg-gray-600 rounded h-3 w-6" />
                                </div>
                              ))}
                            </div>
                            <div className="bg-blue-600/40 rounded h-6 mt-auto" />
                          </div>
                        </div>
                      )}
                      {design.key === 'master' && (
                        <div className="flex flex-col h-full gap-1">
                          <div className="flex gap-1">
                            <div className="bg-gray-700 rounded h-5 flex-1" />
                            <div className="bg-gray-700 rounded h-5 w-12" />
                          </div>
                          <div className="flex gap-1 flex-1">
                            <div className="flex-1 space-y-1">
                              {[1,2,3,4].map(i => (
                                <div key={i} className="flex items-center gap-1">
                                  <div className="bg-gray-600 rounded h-4 flex-1" />
                                  <div className="bg-gray-500 rounded h-4 w-8" />
                                  <div className="bg-gray-500 rounded h-4 w-5" />
                                </div>
                              ))}
                            </div>
                            <div className="grid grid-cols-3 gap-1 w-16">
                              {[1,2,3,4,5,6,7,8,9,0].map(i => (
                                <div key={i} className="bg-gray-700 rounded h-5 flex items-center justify-center text-xs text-gray-300">{i}</div>
                              ))}
                            </div>
                          </div>
                        </div>
                      )}
                      {design.key === 'premium' && (
                        <div className="flex flex-col h-full gap-1">
                          <div className="flex gap-1">
                            <div className="bg-gray-700 rounded h-5 flex-1" />
                            <div className="bg-gray-600 rounded h-5 w-16" />
                          </div>
                          <div className="grid grid-cols-3 gap-1 flex-1">
                            {[1,2,3,4,5,6].map(i => (
                              <div key={i} className="bg-gray-700 rounded flex flex-col items-center justify-center p-1">
                                <div className="bg-gray-600 rounded w-8 h-6 mb-1" />
                                <div className="bg-gray-500 rounded h-2 w-full" />
                                <div className="bg-gray-600 rounded h-2 w-6 mt-0.5" />
                              </div>
                            ))}
                          </div>
                          <div className="bg-blue-600/40 rounded h-5" />
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-4 p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg">
                <p className="text-xs text-amber-400">
                  <strong>Nota:</strong> O design selecionado será enviado para o terminal Android da empresa. Cada empresa pode ter um design diferente.
                </p>
              </div>
            </div>
          </SectionCard>
        )}
      </div>

      {/* Preview */}
      {showPreview && (
        <div className="glass rounded-xl p-4">
          <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
            <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
              <Eye size={14} /> Preview do Comprovante
            </h3>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs text-gray-500">Modelo:</span>
              <div className="flex gap-1">
                {modelosLista.map(({ key, label }) => (
                  <button key={key} onClick={() => setPreviewTipo(key)}
                    className={`px-2 py-1 rounded text-xs transition-all ${previewTipo === key ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:bg-gray-600'}`}>
                    {label}
                  </button>
                ))}
              </div>
              <span className="text-xs text-gray-500 ml-2">Formato:</span>
              <DarkSelect value={paperFormat} onChange={setPaperFormat} options={Object.entries(PAPER_FORMATS).map(([k, v]) => ({ value: k, label: v.label }))} className="w-36" />
            </div>
          </div>
          <div className="bg-gray-900 p-6 rounded-lg border border-gray-700 flex justify-center overflow-x-auto">
            <PreviewComprovante template={template} paperFormat={paperFormat} tipo={previewTipo} />
          </div>
        </div>
      )}
    </div>
  )
}