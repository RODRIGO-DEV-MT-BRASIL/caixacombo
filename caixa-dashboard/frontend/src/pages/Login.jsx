import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Lock, User, AlertCircle, Loader2, Zap, QrCode, BarChart3, Brain, Cloud } from 'lucide-react'

const features = [
  { icon: Zap, label: 'Vendas rápidas' },
  { icon: QrCode, label: 'QR Code' },
  { icon: BarChart3, label: 'Relatórios completos' },
  { icon: Brain, label: 'Gestão inteligente' },
  { icon: Cloud, label: '100% na nuvem' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [loginType, setLoginType] = useState('admin')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [email, setEmail] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (loginType === 'funcionario') {
        await login(null, null, email, pin)
      } else {
        await login(username, password)
      }
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="h-screen flex flex-col lg:flex-row relative overflow-hidden" style={{ fontFamily: "'Inter', 'Poppins', system-ui, sans-serif" }}>

      {/* ===== LADO ESQUERDO - BRANDING ===== */}
      <div className="hidden lg:flex flex-1 relative flex-col items-center justify-center overflow-hidden"
        style={{ background: 'linear-gradient(135deg, #020617 0%, #0c1a3e 40%, #0a1628 100%)' }}>

        {/* Grid tecnológico de fundo */}
        <div className="absolute inset-0 opacity-[0.04]"
          style={{ backgroundImage: 'linear-gradient(rgba(59,130,246,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(59,130,246,0.5) 1px, transparent 1px)', backgroundSize: '60px 60px' }} />

        {/* Partículas digitais */}
        <div className="absolute inset-0 overflow-hidden">
          {[...Array(20)].map((_, i) => (
            <div key={i} className="absolute rounded-full bg-blue-400"
              style={{
                width: Math.random() * 4 + 1 + 'px',
                height: Math.random() * 4 + 1 + 'px',
                left: Math.random() * 100 + '%',
                top: Math.random() * 100 + '%',
                opacity: Math.random() * 0.5 + 0.1,
                animation: `float ${Math.random() * 6 + 4}s ease-in-out infinite`,
                animationDelay: Math.random() * 4 + 's',
              }} />
          ))}
        </div>

        {/* Círculos luminosos */}
        <div className="absolute top-[15%] left-[10%] w-64 h-64 rounded-full bg-blue-500/5 blur-3xl animate-pulse" />
        <div className="absolute bottom-[20%] right-[15%] w-80 h-80 rounded-full bg-cyan-500/5 blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
        <div className="absolute top-[50%] left-[50%] -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] rounded-full bg-blue-600/3 blur-[100px]" />

        {/* Linhas tecnológicas */}
        <div className="absolute top-[30%] left-0 w-full h-px bg-gradient-to-r from-transparent via-blue-500/20 to-transparent" />
        <div className="absolute top-[70%] left-0 w-full h-px bg-gradient-to-r from-transparent via-cyan-500/15 to-transparent" />

        {/* Glow atrás da logo */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-[60%] w-[400px] h-[400px] rounded-full bg-blue-500/10 blur-[80px]" />

        {/* Conteúdo central - Apenas Logo */}
        <div className="relative z-10 flex flex-col items-center px-12 max-w-xl">
          {/* Logo */}
          <img src="/controle.png" alt="CaixaCombo" className="w-[380px] max-w-full object-contain" />
        </div>
      </div>

      {/* ===== LADO DIREITO - LOGIN ===== */}
      <div className="flex-1 flex items-center justify-center relative"
        style={{ background: 'linear-gradient(160deg, #030712 0%, #0a1628 50%, #020617 100%)' }}>

        {/* Glow sutil */}
        <div className="absolute top-1/3 right-1/4 w-72 h-72 rounded-full bg-blue-600/5 blur-[80px]" />

        {/* Mobile logo */}
        <div className="lg:hidden absolute top-4 left-1/2 -translate-x-1/2">
          <img src="/controle.png" alt="CaixaCombo" className="w-28 object-contain" />
        </div>

        <div className="relative z-10 w-full max-w-md px-3 lg:px-6 lg:pt-0 pt-14 pb-2">
          {/* Card glassmorphism */}
          <div className="rounded-3xl p-3 lg:p-6 backdrop-blur-xl"
            style={{
              background: 'rgba(15,23,42,0.6)',
              border: '1px solid rgba(59,130,246,0.12)',
              boxShadow: '0 0 60px rgba(59,130,246,0.06), 0 25px 50px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.03)',
            }}>

            <h2 className="text-lg font-bold text-white mb-1">Bem-vindo de volta</h2>
            <p className="text-[10px] text-gray-400 mb-3">Entre na sua conta para continuar</p>

            {/* Login Type Toggle */}
            <div className="flex gap-2 mb-2">
              <button
                type="button"
                onClick={() => setLoginType('admin')}
                className={`flex-1 py-1.5 px-3 rounded-lg text-xs font-medium transition-all ${loginType === 'admin' ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:text-white'}`}
              >
                Admin
              </button>
              <button
                type="button"
                onClick={() => setLoginType('funcionario')}
                className={`flex-1 py-1.5 px-3 rounded-lg text-xs font-medium transition-all ${loginType === 'funcionario' ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:text-white'}`}
              >
                Funcionário
              </button>
            </div>

            {error && (
              <div className="flex items-center gap-2 p-1.5 mb-2 rounded-xl text-red-400 text-[10px]"
                style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)' }}>
                <AlertCircle size={12} className="shrink-0" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-2">
              {loginType === 'admin' ? (
                <>
                  <div>
                    <label className="block text-[10px] font-medium text-gray-300 mb-0.5">Usuário</label>
                    <div className="relative group">
                      <User size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="text"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        className="w-full pl-8 pr-2.5 py-1.5 rounded-lg text-white placeholder-gray-500 text-[10px] outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
                        style={{
                          background: 'rgba(15,23,42,0.5)',
                          border: '1px solid rgba(59,130,246,0.15)',
                        }}
                        placeholder="Digite seu usuário"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-medium text-gray-300 mb-0.5">Senha</label>
                    <div className="relative group">
                      <Lock size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        className="w-full pl-8 pr-2.5 py-1.5 rounded-lg text-white placeholder-gray-500 text-[10px] outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
                        style={{
                          background: 'rgba(15,23,42,0.5)',
                          border: '1px solid rgba(59,130,246,0.15)',
                        }}
                        placeholder="Digite sua senha"
                        required
                      />
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className="block text-[10px] font-medium text-gray-300 mb-0.5">Email</label>
                    <div className="relative group">
                      <User size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        className="w-full pl-8 pr-2.5 py-1.5 rounded-lg text-white placeholder-gray-500 text-[10px] outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
                        style={{
                          background: 'rgba(15,23,42,0.5)',
                          border: '1px solid rgba(59,130,246,0.15)',
                        }}
                        placeholder="Digite seu email"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-medium text-gray-300 mb-0.5">PIN</label>
                    <div className="relative group">
                      <Lock size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="password"
                        value={pin}
                        onChange={e => setPin(e.target.value)}
                        className="w-full pl-8 pr-2.5 py-1.5 rounded-lg text-white placeholder-gray-500 text-[10px] outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
                        style={{
                          background: 'rgba(15,23,42,0.5)',
                          border: '1px solid rgba(59,130,246,0.15)',
                        }}
                        placeholder="Digite seu PIN"
                        maxLength={6}
                        required
                      />
                    </div>
                  </div>
                </>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 py-1.5 rounded-lg text-white font-semibold text-xs transition-all duration-300 hover:scale-[1.02] hover:shadow-lg hover:shadow-blue-500/20 active:scale-[0.98] disabled:opacity-50 disabled:hover:scale-100"
                style={{
                  background: 'linear-gradient(135deg, #2563eb 0%, #0891b2 100%)',
                  boxShadow: '0 4px 20px rgba(37,99,235,0.3)',
                }}
              >
                {loading ? (
                  <>
                    <Loader2 size={14} className="animate-spin" />
                    Entrando...
                  </>
                ) : (
                  'Entrar'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>

      {/* Animação CSS inline */}
      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); opacity: 0.3; }
          50% { transform: translateY(-20px); opacity: 0.7; }
        }
      `}</style>
    </div>
  )
}
