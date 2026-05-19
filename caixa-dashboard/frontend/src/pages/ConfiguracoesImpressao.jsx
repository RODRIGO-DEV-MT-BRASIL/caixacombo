import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Printer, Save, Loader2, Image, Building2, List, Eye, ChevronDown, Type, Ruler, Palette, AlignLeft, AlignCenter, AlignRight, Upload, Trash2, Edit3, Phone, Mail, MapPin, Hash, Building, DollarSign, Receipt, FileText, CreditCard, ShoppingBag, QrCode } from 'lucide-react'
import { useToast } from '../components/Toast'

const PAPER_FORMATS = {
  '80mm': { label: '80mm Padrão', width: 200 },
  '58mm': { label: '58mm Pequeno', width: 145 },
  '76mm': { label: '76mm', width: 190 },
  '57mm': { label: '57mm', width: 142 },
}

const TEMPLATE_TYPES = {
  venda: { label: 'Comprovante de Venda', icon: ShoppingBag },
  nfce: { label: 'NFC-e (Cupom Fiscal)', icon: FileText },
  fechamento: { label: 'Fechamento de Caixa', icon: Receipt },
  sangria: { label: 'Sangria', icon: DollarSign },
  suprimento: { label: 'Suprimento', icon: DollarSign },
  abertura: { label: 'Abertura de Caixa', icon: Receipt },
}

const defaultTemplate = {
  tipo: 'venda',
  empresa: { nome: 'Rodrigo Dev MT', cnpj: '12.345.678/0001-90', ie: '', endereco: '', telefone: '', email: '', cidade: '', cep: '' },
  logo: { enabled: false, width: 120, height: 60, spacingTop: 10, spacingBottom: 10, base64: null },
  cabecalho: { titulo: 'COMPROVANTE DE VENDA', subtitulo: '' },
  nfce: {
    inscricaoMunicipal: '', regimeTributario: 'Simples Nacional',
    csc: '', cscId: '000001',
    numeroNFCe: '', serie: '001',
  },
  itens: { nome: true, quantidade: true, valorUnitario: true, valorTotal: true, separador: true, un: true },
  adicionais: { subtotal: true, desconto: true, total: true, formaPagamento: true, valorRecebido: true, troco: true, numeroVenda: true, dataHora: true, hora: true },
  pagamento: {
    dinheiro: true, pix: true, credito: true, debito: true,
    troco: true, valorTotal: true, cpfCnpj: true
  },
  imposto: { Tributos: true, totalTributos: 0, icms: true, pis: true, cofins: true, ibnpt: true },
  qrCode: { enabled: false, url: '' },
  rodape: { linha1: 'Agradecemos sua vinda', linha2: 'Volte sempre', linha3: '', linha4: '' },
  estilo: { alinhamento: 'centro', tamanhoFonte: 'medio', espacoEntreLinhas: 8 },
  tamanhos: {
    empresa: 28, titulo: 32, subtitulo: 24, corpo: 22,
    numeroNFCe: 18, serie: 16, dataHora: 18,
    itensTitulo: 20, itensNome: 20, itensValores: 16,
    subtotal: 18, desconto: 18, total: 24,
    formaPagamento: 18, valorRecebido: 18, troco: 18,
    tributos: 14, qrCode: 12, rodape: 20
  }
}

function PreviewNFCe({ template, paperFormat = '80mm' }) {
  const exampleVenda = { id: 123, data: '19/05/2026', hora: '15:45:00', itens: [{ nome: 'Coca-Cola 350ml', un: 'UN', qtd: 2, valorUnit: 3.00, total: 6.00 }, { nome: 'Sanduíche Natural', un: 'UN', qtd: 1, valorUnit: 8.00, total: 8.00 }], subtotal: 14.00, desconto: 0.00, total: 14.00, formaPagamento: 'DINHEIRO', valorRecebido: 20.00, troco: 6.00, cpf: '' }
  const fs = { pequeno: 9, medio: 11, grande: 13 }[template.estilo?.tamanhoFonte] || 11
  const t = template.tamanhos || {}
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
        <div className="font-bold" style={{ fontSize: (t.empresa || 28) * 0.4 }}>{template.empresa?.nome || 'Empresa'}</div>
        {template.empresa?.cnpj && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>CNPJ: {template.empresa.cnpj}</div>}
        {template.empresa?.ie && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>IE: {template.empresa.ie}</div>}
        {template.empresa?.endereco && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.endereco}</div>}
        {template.empresa?.cidade && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.cidade}{template.empresa?.cep ? ` - ${template.empresa.cep}` : ''}</div>}
        {template.empresa?.telefone && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>FONE: {template.empresa.telefone}</div>}
        {template.empresa?.email && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.email}</div>}
        {template.nfce?.inscricaoMunicipal && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>IM: {template.nfce.inscricaoMunicipal}</div>}
      </div>
      
      <div className={`${alignClass} my-2 border-t border-b border-dashed border-gray-400 py-1`} style={{ fontSize: (t.titulo || 32) * 0.4 }}>
        {template.cabecalho?.titulo || 'DADOS DA NFC-e'}
      </div>
      
      <div className={alignClass}>
        {template.nfce?.numeroNFCe && <div style={{ fontSize: (t.numeroNFCe || 18) * 0.38 }}>NFC-e N° {exampleVenda.id.toString().padStart(9, '0')}</div>}
        {template.nfce?.serie && <div style={{ fontSize: (t.serie || 16) * 0.38 }}>Série: {template.nfce.serie}</div>}
        {template.adicionais?.dataHora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>Dt: {exampleVenda.data} {template.adicionais?.hora ? exampleVenda.hora : ''}</div>}
      </div>
      
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        <div className="font-bold" style={{ fontSize: (t.itensTitulo || 20) * 0.4 }}>ITENS:</div>
        {exampleVenda.itens.map((item, i) => (
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
        {template.adicionais?.subtotal && (
          <div className="flex justify-between" style={{ fontSize: (t.subtotal || 18) * 0.38 }}>
            <span>Subtotal:</span><span>R$ {exampleVenda.subtotal.toFixed(2)}</span>
          </div>
        )}
        {template.adicionais?.desconto && (
          <div className="flex justify-between" style={{ fontSize: (t.desconto || 18) * 0.38 }}>
            <span>Desconto:</span><span>R$ {exampleVenda.desconto.toFixed(2)}</span>
          </div>
        )}
        <div className="flex justify-between font-bold border-t border-gray-600 mt-1 pt-1" style={{ fontSize: (t.total || 24) * 0.4 }}>
          <span>TOTAL:</span><span>R$ {exampleVenda.total.toFixed(2)}</span>
        </div>
      </div>

      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        {template.pagamento?.dinheiro && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>Dinheiro: R$ {exampleVenda.total.toFixed(2)}</div>}
        {template.pagamento?.valorTotal && (
          <div className="font-bold" style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>
            Valor Total: R$ {exampleVenda.total.toFixed(2)}
          </div>
        )}
        {template.adicionais?.valorRecebido && <div style={{ fontSize: (t.valorRecebido || 18) * 0.38 }}>Dinheiro: R$ {exampleVenda.valorRecebido.toFixed(2)}</div>}
        {template.adicionais?.troco && <div style={{ fontSize: (t.troco || 18) * 0.38 }}>Troco: R$ {exampleVenda.troco.toFixed(2)}</div>}
        {template.pagamento?.cpfCnpj && exampleVenda.cpf && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>Consumidor: {exampleVenda.cpf}</div>}
      </div>

      {template.imposto?.Tributos && (
        <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`} style={{ fontSize: (t.tributos || 14) * 0.38 }}>
          <div>Tributos Totais Incidentes (Lei 12.741/12)</div>
          {template.imposto?.ibpt && <div>IBPT: R$ {(exampleVenda.total * 0.12).toFixed(2)}</div>}
          {template.imposto?.icms && <div>ICMS: R$ {(exampleVenda.total * 0.08).toFixed(2)}</div>}
          {template.imposto?.pis && <div>PIS: R$ {(exampleVenda.total * 0.02).toFixed(2)}</div>}
          {template.imposto?.cofins && <div>COFINS: R$ {(exampleVenda.total * 0.02).toFixed(2)}</div>}
        </div>
      )}

      {template.qrCode?.enabled && (
        <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
          <div className="border border-gray-400 p-2 rounded flex items-center justify-center" style={{ fontSize: (t.qrCode || 12) * 0.38 }}>
            <div className="w-16 h-16 bg-gray-300 flex items-center justify-center mx-auto">QR</div>
          </div>
          {template.nfce?.csc && <div style={{ fontSize: (t.qrCode || 12) * 0.35 }}> CSC: {template.nfce.csc}</div>}
        </div>
      )}

      <div className={`${alignClass} mt-3 pt-2 border-t border-dashed border-gray-400`}>
        {template.rodape?.linha1 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha1}</div>}
        {template.rodape?.linha2 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha2}</div>}
        {template.rodape?.linha3 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha3}</div>}
        {template.rodape?.linha4 && <div style={{ fontSize: (t.rodape || 20) * 0.38 }}>{template.rodape.linha4}</div>}
      </div>
    </div>
  )
}

function PreviewComprovante({ template, paperFormat = '80mm' }) {
  const exampleVenda = { id: 123, data: '19/05/2026 15:45', itens: [{ nome: 'Coca-Cola 350ml', qtd: 2, valorUnit: 3.00, total: 6.00 }, { nome: 'Sanduíche Natural', qtd: 1, valorUnit: 8.00, total: 8.00 }], subtotal: 14.00, desconto: 0.00, total: 14.00, formaPagamento: 'DINHEIRO', valorRecebido: 20.00, troco: 6.00 }
  const fs = { pequeno: 9, medio: 11, grande: 13 }[template.estilo?.tamanhoFonte] || 11
  const t = template.tamanhos || {}
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
        <div className="font-bold" style={{ fontSize: (t.empresa || 28) * 0.4 }}>{template.empresa?.nome || 'Empresa'}</div>
        {template.empresa?.cnpj && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>CNPJ: {template.empresa.cnpj}</div>}
        {template.empresa?.ie && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>IE: {template.empresa.ie}</div>}
        {template.empresa?.endereco && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.endereco}</div>}
        {template.empresa?.cidade && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.cidade}</div>}
        {template.empresa?.telefone && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>Tel: {template.empresa.telefone}</div>}
        {template.empresa?.email && <div style={{ fontSize: (t.corpo || 22) * 0.35 }}>{template.empresa.email}</div>}
      </div>
      <div className={`${alignClass} my-2 border-t border-b border-dashed border-gray-400 py-1`} style={{ fontSize: (t.titulo || 32) * 0.4 }}>
        {template.cabecalho?.titulo || 'COMPROVANTE DE VENDA'}
      </div>
      {template.cabecalho?.subtitulo && <div className={alignClass} style={{ fontSize: (t.subtitulo || 24) * 0.35 }}>{template.cabecalho.subtitulo}</div>}
      <div className={alignClass}>
        {template.adicionais?.numeroVenda && <div style={{ fontSize: (t.numeroVenda || 18) * 0.38 }}>Nr: {exampleVenda.id.toString().padStart(6, '0')}</div>}
        {template.adicionais?.dataHora && <div style={{ fontSize: (t.dataHora || 18) * 0.38 }}>DATA: {exampleVenda.data}</div>}
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        <div className="font-bold" style={{ fontSize: (t.itensTitulo || 20) * 0.4 }}>ITENS:</div>
        {exampleVenda.itens.map((item, i) => (
          <div key={i} className="mt-1">
            {template.itens?.nome && <div style={{ fontSize: (t.itensNome || 20) * 0.38 }}>{item.nome}</div>}
            {template.itens?.separador && <div>----------------------------</div>}
            {(template.itens?.quantidade || template.itens?.valorUnitario || template.itens?.valorTotal) && (
              <div style={{ fontSize: (t.itensValores || 16) * 0.38 }}>
                {template.itens?.quantidade && `QTD: ${item.qtd}`}
                {template.itens?.valorUnitario && ` x R$ ${item.valorUnit.toFixed(2)}`}
                {template.itens?.valorTotal && ` = R$ ${item.total.toFixed(2)}`}
              </div>
            )}
          </div>
        ))}
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        {template.adicionais?.subtotal && (
          <div className="flex justify-between" style={{ fontSize: (t.subtotal || 18) * 0.38 }}>
            <span>SUBTOTAL:</span><span>R$ {exampleVenda.subtotal.toFixed(2)}</span>
          </div>
        )}
        {template.adicionais?.desconto && (
          <div className="flex justify-between" style={{ fontSize: (t.desconto || 18) * 0.38 }}>
            <span>DESCONTO:</span><span>R$ {exampleVenda.desconto.toFixed(2)}</span>
          </div>
        )}
        <div className="flex justify-between font-bold border-t border-gray-600 mt-1 pt-1" style={{ fontSize: (t.total || 24) * 0.4 }}>
          <span>TOTAL:</span><span>R$ {exampleVenda.total.toFixed(2)}</span>
        </div>
      </div>
      <div className={`${alignClass} my-2 border-t border-dashed border-gray-400 pt-1`}>
        {template.adicionais?.formaPagamento && <div style={{ fontSize: (t.formaPagamento || 18) * 0.38 }}>FORMA: {exampleVenda.formaPagamento}</div>}
        {template.adicionais?.valorRecebido && <div style={{ fontSize: (t.valorRecebido || 18) * 0.38 }}>RECEBIDO: R$ {exampleVenda.valorRecebido.toFixed(2)}</div>}
        {template.adicionais?.troco && <div style={{ fontSize: (t.troco || 18) * 0.38 }}>TROCO: R$ {exampleVenda.troco.toFixed(2)}</div>}
      </div>
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
  const [activeTab, setActiveTab] = useState('tipo')
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
      const data = await res.json()
      if (res.ok) toast.success('✅ Template salvo com sucesso')
      else toast.error(data.error || '❌ Erro ao salvar')
    } catch { toast.error('❌ Erro ao salvar') }
    setSaving(false)
  }

  const updateEmpresa = (field, value) => setTemplate(prev => ({ ...prev, empresa: { ...prev.empresa, [field]: value } }))
  const updateNfce = (field, value) => setTemplate(prev => ({ ...prev, nfce: { ...prev.nfce, [field]: value } }))
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
    { id: 'tipo', label: 'Tipo', icon: FileText },
    { id: 'empresa', label: 'Empresa', icon: Building },
    { id: 'nfce', label: 'NFC-e', icon: Receipt },
    { id: 'logo', label: 'Logo', icon: Image },
    { id: 'itens', label: 'Itens', icon: List },
    { id: 'pagamento', label: 'Pagamento', icon: CreditCard },
    { id: 'imposto', label: 'Tributos', icon: DollarSign },
    { id: 'qrcode', label: 'QR Code', icon: QrCode },
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
        {/* Tipo de Template */}
        {activeTab === 'tipo' && (
          <SectionCard title="Tipo de Comprovante" icon={FileText}>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {Object.entries(TEMPLATE_TYPES).map(([key, { label, icon: Icon }]) => (
                <button key={key} onClick={() => setTemplate(prev => ({ ...prev, tipo: key }))}
                  className={`p-4 rounded-xl border transition-all flex flex-col items-center gap-2 ${template.tipo === key ? 'bg-blue-600/20 border-blue-500 text-blue-400' : 'border-gray-700 text-gray-400 hover:bg-white/5'}`}>
                  <Icon size={24} />
                  <span className="text-xs text-center">{label}</span>
                </button>
              ))}
            </div>
          </SectionCard>
        )}

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
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><MapPin size={12} /> Cidade/UF - CEP</label>
                  <input type="text" value={template.empresa?.cidade || ''} onChange={e => updateEmpresa('cidade', e.target.value)} className="input-field text-sm" placeholder="Cidade - UF" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Hash size={12} /> CEP</label>
                  <input type="text" value={template.empresa?.cep || ''} onChange={e => updateEmpresa('cep', e.target.value)} className="input-field text-sm" placeholder="00000-000" />
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Phone size={12} /> Telefone</label>
                  <input type="text" value={template.empresa?.telefone || ''} onChange={e => updateEmpresa('telefone', e.target.value)} className="input-field text-sm" placeholder="(00) 00000-0000" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1 flex items-center gap-1"><Mail size={12} /> E-mail</label>
                  <input type="text" value={template.empresa?.email || ''} onChange={e => updateEmpresa('email', e.target.value)} className="input-field text-sm" placeholder="contato@empresa.com" />
                </div>
              </div>
            </div>
          </SectionCard>
        )}

        {/* NFC-e Config */}
        {activeTab === 'nfce' && (
          <SectionCard title="Configurações NFC-e" icon={Receipt}>
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1">Inscrição Municipal (IM)</label>
                  <input type="text" value={template.nfce?.inscricaoMunicipal || ''} onChange={e => updateNfce('inscricaoMunicipal', e.target.value)} className="input-field text-sm" placeholder="00000000" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1">Regime Tributário</label>
                  <DarkSelect value={template.nfce?.regimeTributario || 'Simples Nacional'} onChange={v => updateNfce('regimeTributario', v)} options={[
                    { value: 'Simples Nacional', label: 'Simples Nacional' },
                    { value: 'Simples Nacional - Excesso', label: 'Simples Nacional - Excesso' },
                    { value: 'Regime Normal', label: 'Regime Normal' },
                  ]} className="w-full" />
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1">CSC (Código de Segurança)</label>
                  <input type="text" value={template.nfce?.csc || ''} onChange={e => updateNfce('csc', e.target.value)} className="input-field text-sm" placeholder="Código de Segurança" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1">CSC ID</label>
                  <input type="text" value={template.nfce?.cscId || '000001'} onChange={e => updateNfce('cscId', e.target.value)} className="input-field text-sm" placeholder="000001" />
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1">Nr. NFC-e Inicial</label>
                  <input type="number" value={template.nfce?.numeroNFCe || ''} onChange={e => updateNfce('numeroNFCe', e.target.value)} className="input-field text-sm" placeholder="000000001" />
                </div>
                <div>
                  <label className="block text-xs text-gray-400 mb-1">Série</label>
                  <input type="text" value={template.nfce?.serie || '001'} onChange={e => updateNfce('serie', e.target.value)} className="input-field text-sm" placeholder="001" />
                </div>
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

        {/* Itens */}
        {activeTab === 'itens' && (
          <SectionCard title="Campos dos Itens" icon={List}>
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
                {[['subtotal', 'Subtotal'], ['desconto', 'Desconto'], ['total', 'TOTAL'], ['numeroVenda', 'Número NFC-e'], ['dataHora', 'Data/Hora'], ['hora', 'Hora']].map(([key, label]) => (
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
              {[['dinheiro', 'Dinheiro'], ['pix', 'PIX'], ['credito', 'Crédito'], ['debito', 'Débito']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.pagamento?.[key] ?? true} onChange={() => toggleField('pagamento', key)} className="rounded" />
                  <span className="text-sm text-gray-300">{label}</span>
                </label>
              ))}
              {[['valorTotal', 'Valor Total'], ['troco', 'Troco'], ['cpfCnpj', 'CPF/CNPJ Consumidor']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                  <input type="checkbox" checked={template.pagamento?.[key] ?? true} onChange={() => toggleField('pagamento', key)} className="rounded" />
                  <span className="text-sm text-gray-300">{label}</span>
                </label>
              ))}
            </div>
          </SectionCard>
        )}

        {/* Imposto/Tributos */}
        {activeTab === 'imposto' && (
          <SectionCard title="Informações de Tributos (OBRIGATÓRIO NFC-e)" icon={DollarSign}>
            <div className="grid grid-cols-2 gap-2 mb-4">
              <label className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                <input type="checkbox" checked={template.imposto?.Tributos ?? true} onChange={() => toggleField('imposto', 'Tributos')} className="rounded" />
                <span className="text-sm text-gray-300">Mostrar Total de Tributos</span>
              </label>
            </div>
            {template.imposto?.Tributos && (
              <div className="grid grid-cols-2 gap-2">
                {[['icms', 'ICMS'], ['pis', 'PIS'], ['cofins', 'COFINS'], ['ibnpt', 'IBPT (Lei 12.741/12)']].map(([key, label]) => (
                  <label key={key} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-white/5">
                    <input type="checkbox" checked={template.imposto?.[key] ?? true} onChange={() => toggleField('imposto', key)} className="rounded" />
                    <span className="text-sm text-gray-300">{label}</span>
                  </label>
                ))}
              </div>
            )}
          </SectionCard>
        )}

        {/* QR Code */}
        {activeTab === 'qrcode' && (
          <SectionCard title="QR Code (Obrigatório NFC-e)" icon={QrCode}>
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-gray-300">Exibir QR Code</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" checked={template.qrCode?.enabled ?? false} onChange={() => updateField('qrCode', 'enabled', !template.qrCode?.enabled)} className="sr-only peer" />
                <div className="w-11 h-6 bg-gray-700 peer-focus:ring-2 peer-focus:ring-blue-500/30 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
            {template.qrCode?.enabled && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs text-gray-400 mb-1">URL do QR Code (opcional)</label>
                  <input type="text" value={template.qrCode?.url || ''} onChange={e => updateField('qrCode', 'url', e.target.value)} className="input-field text-sm" placeholder="https://..." />
                </div>
                <div className="p-3 bg-blue-500/10 border border-blue-500/20 rounded-lg">
                  <p className="text-xs text-blue-400">O QR Code contém a chave de acesso da NFC-e e hash para verificação fiscal. Configure o CSC nas abas NFC-e.</p>
                </div>
              </div>
            )}
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
                <RangeSlider label="Título do Comprovante" value={template.tamanhos?.titulo || 32} onChange={v => updateSize('titulo', v)} min={20} max={48} />
                <RangeSlider label="Nr. NFC-e" value={template.tamanhos?.numeroNFCe || 18} onChange={v => updateSize('numeroNFCe', v)} min={12} max={28} />
                <RangeSlider label="Série" value={template.tamanhos?.serie || 16} onChange={v => updateSize('serie', v)} min={12} max={28} />
                <RangeSlider label="Data/Hora" value={template.tamanhos?.dataHora || 18} onChange={v => updateSize('dataHora', v)} min={12} max={28} />
                <RangeSlider label="Corpo (dados empresa)" value={template.tamanhos?.corpo || 22} onChange={v => updateSize('corpo', v)} min={14} max={32} />
              </div>
              <div className="border-t border-white/10 pt-4">
                <h4 className="text-xs text-gray-400 mb-3">Campos do Comprovante</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <RangeSlider label="Título ITENS" value={template.tamanhos?.itensTitulo || 20} onChange={v => updateSize('itensTitulo', v)} min={14} max={32} />
                  <RangeSlider label="Nome dos Produtos" value={template.tamanhos?.itensNome || 20} onChange={v => updateSize('itensNome', v)} min={14} max={32} />
                  <RangeSlider label="Valores dos Itens" value={template.tamanhos?.itensValores || 16} onChange={v => updateSize('itensValores', v)} min={12} max={28} />
                </div>
              </div>
              <div className="border-t border-white/10 pt-4">
                <h4 className="text-xs text-gray-400 mb-3">Totais e Pagamento</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <RangeSlider label="Subtotal" value={template.tamanhos?.subtotal || 18} onChange={v => updateSize('subtotal', v)} min={12} max={28} />
                  <RangeSlider label="Desconto" value={template.tamanhos?.desconto || 18} onChange={v => updateSize('desconto', v)} min={12} max={28} />
                  <RangeSlider label="TOTAL (destaque)" value={template.tamanhos?.total || 24} onChange={v => updateSize('total', v)} min={16} max={40} />
                  <RangeSlider label="Forma Pagamento" value={template.tamanhos?.formaPagamento || 18} onChange={v => updateSize('formaPagamento', v)} min={12} max={28} />
                  <RangeSlider label="Valor Recebido" value={template.tamanhos?.valorRecebido || 18} onChange={v => updateSize('valorRecebido', v)} min={12} max={28} />
                  <RangeSlider label="Troco" value={template.tamanhos?.troco || 18} onChange={v => updateSize('troco', v)} min={12} max={28} />
                </div>
              </div>
              <div className="border-t border-white/10 pt-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <RangeSlider label="Tributos" value={template.tamanhos?.tributos || 14} onChange={v => updateSize('tributos', v)} min={10} max={22} />
                  <RangeSlider label="QR Code" value={template.tamanhos?.qrCode || 12} onChange={v => updateSize('qrCode', v)} min={10} max={20} />
                  <RangeSlider label="Rodapé" value={template.tamanhos?.rodape || 20} onChange={v => updateSize('rodape', v)} min={12} max={28} />
                </div>
              </div>
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
            {template.tipo === 'nfce' ? <PreviewNFCe template={template} paperFormat={paperFormat} /> : <PreviewComprovante template={template} paperFormat={paperFormat} />}
          </div>
        </div>
      )}
    </div>
  )
}