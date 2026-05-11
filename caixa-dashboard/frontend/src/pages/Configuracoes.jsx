import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Palette, Save, Loader2, RotateCw, Shield } from 'lucide-react'
import SenhaConfirmModal from '../components/SenhaConfirmModal'

const defaultConfig = {
  primaryColor: '#3b82f6',
  secondaryColor: '#06b6d4',
  accentColor: '#10b981',
  companyName: 'CaixaCombo',
  logoUrl: ''
}

export default function Configuracoes() {
  const { token, user } = useAuth()
  const [config, setConfig] = useState(defaultConfig)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showSenhaModal, setShowSenhaModal] = useState(false)
  const [isAdmin, setIsAdmin] = useState(user?.role === 'admin')

  const isEmpresa = user?.role === 'empresa'
  const empresaId = user?.empresaId

  useEffect(() => {
    // Se for empresa, usar cores do user.branding
    if (isEmpresa && user?.branding) {
      setConfig({
        ...defaultConfig,
        primaryColor: user.branding.primaryColor || defaultConfig.primaryColor,
        secondaryColor: user.branding.secondaryColor || defaultConfig.secondaryColor,
        accentColor: user.branding.accentColor || defaultConfig.accentColor,
        companyName: user.branding.companyName || user.empresaNome || defaultConfig.companyName,
        logoUrl: user.branding.logoUrl || defaultConfig.logoUrl
      })
      setLoading(false)
    } else {
      // Admin: buscar config global
      fetch(apiUrl('/api/config'), { headers: { Authorization: `Bearer ${token}` } })
        .then(res => res.json())
        .then(data => { setConfig({ ...defaultConfig, ...data }); setLoading(false) })
        .catch(() => setLoading(false))
    }
  }, [token, user, isEmpresa])

  const handleSave = async () => {
    setSaving(true)
    try {
      let res
      if (isEmpresa && empresaId) {
        // Empresa: atualizar sua própria empresa
        res = await fetch(apiUrl(`/api/empresas/${empresaId}`), {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({
            primaryColor: config.primaryColor,
            secondaryColor: config.secondaryColor,
            accentColor: config.accentColor,
            logoUrl: config.logoUrl,
            nome: config.companyName
          })
        })
      } else {
        // Admin: atualizar config global
        res = await fetch(apiUrl('/api/config'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify(config)
        })
      }
      const data = await res.json()
      if (res.ok) {
        alert('✅ Configurações salvas com sucesso')
        applyColors(config)
      } else {
        alert('❌ Erro: ' + (data.error || 'Erro ao salvar'))
      }
    } catch (e) {
      alert('❌ Erro ao salvar configurações')
    }
    setSaving(false)
  }

  const applyColors = (cfg) => {
    const root = document.documentElement
    root.style.setProperty('--color-primary', cfg.primaryColor)
    root.style.setProperty('--color-secondary', cfg.secondaryColor)
    root.style.setProperty('--color-accent', cfg.accentColor)
  }

  const handleReset = () => {
    setConfig(defaultConfig)
    applyColors(defaultConfig)
  }

  if (loading) {
    return (
      <div className="glass p-12 text-center">
        <Loader2 size={32} className="animate-spin mx-auto text-blue-400 mb-3" />
        <p className="text-gray-400">Carregando configurações...</p>
      </div>
    )
  }

  // Empresa pode acessar diretamente, Admin precisa confirmar senha
  if (!isAdmin && !isEmpresa) {
    return (
      <div className="glass p-12 text-center">
        <Shield size={48} className="mx-auto text-red-400 mb-3" />
        <p className="text-red-400 font-medium">Acesso restrito</p>
        <p className="text-gray-500 text-sm mt-1">Você não tem permissão para acessar esta página</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Palette size={24} className="text-blue-400" />
          {isEmpresa ? 'Minha Marca' : 'Configurações Whitelabel'}
        </h2>
        <div className="flex gap-2">
          <button onClick={handleReset} className="btn-ghost flex items-center gap-2 text-sm">
            <RotateCw size={16} /> Resetar
          </button>
          <button onClick={handleSave} disabled={saving} className="btn-primary flex items-center gap-2 text-sm">
            {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Salvar
          </button>
        </div>
      </div>

      {/* Preview */}
      <div className="glass p-4">
        <p className="text-xs text-gray-400 mb-3">Preview</p>
        <div className="flex items-center gap-4">
          <div className="flex gap-2">
            <div className="w-16 h-16 rounded-xl flex items-center justify-center text-white text-xs font-bold" style={{ backgroundColor: config.primaryColor }}>Primary</div>
            <div className="w-16 h-16 rounded-xl flex items-center justify-center text-white text-xs font-bold" style={{ backgroundColor: config.secondaryColor }}>Second</div>
            <div className="w-16 h-16 rounded-xl flex items-center justify-center text-white text-xs font-bold" style={{ backgroundColor: config.accentColor }}>Accent</div>
          </div>
          <div className="flex-1">
            <div className="h-3 rounded-full mb-2" style={{ background: `linear-gradient(to right, ${config.primaryColor}, ${config.secondaryColor})` }}></div>
            <div className="h-2 rounded-full w-3/4" style={{ backgroundColor: config.accentColor, opacity: 0.5 }}></div>
          </div>
        </div>
      </div>

      {/* Cores */}
      <div className="glass p-4 space-y-4">
        <h3 className="text-sm font-semibold text-gray-300">Cores do Sistema</h3>
        
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">Cor Primária</label>
            <div className="flex items-center gap-2">
              <input type="color" value={config.primaryColor} onChange={e => setConfig({ ...config, primaryColor: e.target.value })} className="w-10 h-10 rounded cursor-pointer border-0 bg-transparent" />
              <input type="text" value={config.primaryColor} onChange={e => setConfig({ ...config, primaryColor: e.target.value })} className="input-field flex-1 text-sm" />
            </div>
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">Cor Secundária</label>
            <div className="flex items-center gap-2">
              <input type="color" value={config.secondaryColor} onChange={e => setConfig({ ...config, secondaryColor: e.target.value })} className="w-10 h-10 rounded cursor-pointer border-0 bg-transparent" />
              <input type="text" value={config.secondaryColor} onChange={e => setConfig({ ...config, secondaryColor: e.target.value })} className="input-field flex-1 text-sm" />
            </div>
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">Cor de Destaque</label>
            <div className="flex items-center gap-2">
              <input type="color" value={config.accentColor} onChange={e => setConfig({ ...config, accentColor: e.target.value })} className="w-10 h-10 rounded cursor-pointer border-0 bg-transparent" />
              <input type="text" value={config.accentColor} onChange={e => setConfig({ ...config, accentColor: e.target.value })} className="input-field flex-1 text-sm" />
            </div>
          </div>
        </div>
      </div>

      {/* Identidade */}
      <div className="glass p-4 space-y-4">
        <h3 className="text-sm font-semibold text-gray-300">Identidade da Marca</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">Nome da Empresa</label>
            <input type="text" value={config.companyName} onChange={e => setConfig({ ...config, companyName: e.target.value })} className="input-field text-sm" />
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">URL do Logo (opcional)</label>
            <input type="text" value={config.logoUrl} onChange={e => setConfig({ ...config, logoUrl: e.target.value })} className="input-field text-sm" placeholder="https://..." />
          </div>
        </div>
      </div>

      {/* Info */}
      <div className="glass p-4 space-y-3 border border-blue-500/20">
        <h3 className="text-sm font-semibold text-blue-400 flex items-center gap-2">
          <Shield size={16} /> {isEmpresa ? 'Sua Marca' : 'Configurações'}
        </h3>
        <p className="text-xs text-gray-400">
          {isEmpresa
            ? 'Personalize as cores e identidade da sua empresa. As alterações serão aplicadas imediatamente após salvar.'
            : 'Configure as cores padrão do sistema. Empresas podem personalizar suas próprias cores.'}
        </p>
      </div>
    </div>
  )
}
