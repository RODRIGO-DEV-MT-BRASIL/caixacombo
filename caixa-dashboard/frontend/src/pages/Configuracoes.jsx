import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { apiUrl } from '../utils/api'
import { Palette, Save, Loader2, RotateCw, Shield } from 'lucide-react'
import SenhaConfirmModal from '../components/SenhaConfirmModal'
import { LoadingState } from '../components/ui'
import { DEFAULT_APP_CONFIG } from '../constants/forms'

export default function Configuracoes() {
  const { token, user } = useAuth()
  /** @type {[import('../types/entities').AppConfig, Function]} */
  const [config, setConfig] = useState(DEFAULT_APP_CONFIG)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showSenhaModal, setShowSenhaModal] = useState(false)
  const [isAdmin, setIsAdmin] = useState(user?.role === 'admin')

  useEffect(() => {
    fetch(apiUrl('/api/config'), { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(data => { setConfig({ ...DEFAULT_APP_CONFIG, ...data }); setLoading(false) })
      .catch(() => setLoading(false))
  }, [token])

  const handleSave = async () => {
    setSaving(true)
    try {
      const res = await fetch(apiUrl('/api/config'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(config)
      })
      const data = await res.json()
      if (data.success) {
        alert('✅ Configurações salvas com sucesso')
        // Aplicar cores via CSS custom properties
        applyColors(config)
      } else {
        alert('❌ Erro: ' + data.error)
      }
    } catch {
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
    setConfig(DEFAULT_APP_CONFIG)
    applyColors(DEFAULT_APP_CONFIG)
  }

  if (loading) {
    return <LoadingState message="Carregando configurações..." className="glass p-12 text-center" />
  }

  if (!isAdmin) {
    return (
      <div className="glass p-12 text-center">
        <Shield size={48} className="mx-auto text-red-400 mb-3" />
        <p className="text-red-400 font-medium">Acesso restrito a administradores</p>
        <p className="text-gray-500 text-sm mt-1">Confirme sua senha para acessar</p>
        <button onClick={() => setShowSenhaModal(true)} className="btn-primary mt-4 text-sm">Confirmar Acesso</button>
        <SenhaConfirmModal
          open={showSenhaModal}
          onClose={() => setShowSenhaModal(false)}
          onConfirm={() => { setIsAdmin(true); setShowSenhaModal(false) }}
          title="Acesso Administrativo"
          description="Confirme sua senha para acessar as configurações"
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Palette size={24} className="text-blue-400" /> Configurações Whitelabel
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

      {/* Ações Sensíveis */}
      <div className="glass p-4 space-y-3 border border-red-500/20">
        <h3 className="text-sm font-semibold text-red-400 flex items-center gap-2">
          <Shield size={16} /> Ações Sensíveis
        </h3>
        <p className="text-xs text-gray-500">As ações abaixo requerem confirmação de senha do administrador:</p>
        <ul className="text-xs text-gray-400 space-y-1 list-disc list-inside">
          <li>Reimpressão de comprovante de venda</li>
          <li>Cancelamento de venda</li>
          <li>Fechamento geral do caixa</li>
          <li>Alteração de configurações whitelabel</li>
        </ul>
      </div>
    </div>
  )
}
