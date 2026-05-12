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
    <div className="min-h-screen flex flex-col lg:flex-row relative overflow-hidden" style={{ fontFamily: "'Inter', 'Poppins', system-ui, sans-serif" }}>

      {/* ===== LADO ESQUERDO - BRANDING ===== */}
      <div className="hidden lg:flex flex-[1.8] relative flex-col items-center justify-center overflow-hidden"
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

        {/* Conteúdo central */}
        <div className="relative z-10 flex flex-col items-center px-12 max-w-xl">
          {/* Logo */}
          <img src="/controle.png" alt="CaixaCombo" className="w-[420px] max-w-full object-contain" />

          {/* Slogan */}
          <p className="mt-6 text-lg text-blue-200/70 text-center font-light tracking-wide leading-relaxed">
            Mais controle, mais agilidade, mais vendas para o seu evento!
          </p>

          {/* Separador neon */}
          <div className="mt-8 w-24 h-px bg-gradient-to-r from-transparent via-cyan-400/60 to-transparent" />

          {/* Feature cards */}
          <div className="mt-8 grid grid-cols-2 gap-3 w-full max-w-md">
            {features.map((f, i) => (
              <div key={i} className={`flex items-center gap-3 px-4 py-3 rounded-xl backdrop-blur-md transition-all duration-300 hover:scale-105 ${i === 4 ? 'col-span-2 justify-center' : ''}`}
                style={{ background: 'rgba(59,130,246,0.06)', border: '1px solid rgba(59,130,246,0.1)' }}>
                <f.icon size={18} className="text-cyan-400 shrink-0" />
                <span className="text-sm text-blue-100/80 font-medium">{f.label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ===== LADO DIREITO - LOGIN ===== */}
      <div className="flex-1 flex items-center justify-center relative min-h-screen lg:min-h-0"
        style={{ background: 'linear-gradient(160deg, #030712 0%, #0a1628 50%, #020617 100%)' }}>

        {/* Glow sutil */}
        <div className="absolute top-1/3 right-1/4 w-72 h-72 rounded-full bg-blue-600/5 blur-[80px]" />

        {/* Mobile logo */}
        <div className="lg:hidden absolute top-8 left-1/2 -translate-x-1/2">
          <img src="/controle.png" alt="CaixaCombo" className="w-48 object-contain" />
        </div>

        <div className="relative z-10 w-full max-w-md px-6 lg:px-8 lg:pt-0 pt-28">
          {/* Card glassmorphism */}
          <div className="rounded-3xl p-8 lg:p-10 backdrop-blur-xl"
            style={{
              background: 'rgba(15,23,42,0.6)',
              border: '1px solid rgba(59,130,246,0.12)',
              boxShadow: '0 0 60px rgba(59,130,246,0.06), 0 25px 50px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.03)',
            }}>

            <h2 className="text-2xl font-bold text-white mb-2">Bem-vindo de volta</h2>
            <p className="text-sm text-gray-400 mb-6">Entre na sua conta para continuar</p>

            {/* Login Type Toggle */}
            <div className="flex gap-2 mb-6">
              <button
                type="button"
                onClick={() => setLoginType('admin')}
                className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all ${loginType === 'admin' ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:text-white'}`}
              >
                Admin
              </button>
              <button
                type="button"
                onClick={() => setLoginType('funcionario')}
                className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all ${loginType === 'funcionario' ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:text-white'}`}
              >
                Funcionário
              </button>
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 mb-5 rounded-xl text-red-400 text-sm"
                style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)' }}>
                <AlertCircle size={16} className="shrink-0" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              {loginType === 'admin' ? (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">Usuário</label>
                    <div className="relative group">
                      <User size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="text"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        className="w-full pl-11 pr-4 py-3.5 rounded-xl text-white placeholder-gray-500 text-sm outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
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
                    <label className="block text-sm font-medium text-gray-300 mb-2">Senha</label>
                    <div className="relative group">
                      <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        className="w-full pl-11 pr-4 py-3.5 rounded-xl text-white placeholder-gray-500 text-sm outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
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
                    <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
                    <div className="relative group">
                      <User size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        className="w-full pl-11 pr-4 py-3.5 rounded-xl text-white placeholder-gray-500 text-sm outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
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
                    <label className="block text-sm font-medium text-gray-300 mb-2">PIN</label>
                    <div className="relative group">
                      <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="password"
                        value={pin}
                        onChange={e => setPin(e.target.value)}
                        className="w-full pl-11 pr-4 py-3.5 rounded-xl text-white placeholder-gray-500 text-sm outline-none transition-all duration-300 focus:ring-2 focus:ring-blue-500/40"
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
                className="w-full flex items-center justify-center gap-2 py-3.5 rounded-xl text-white font-semibold text-base transition-all duration-300 hover:scale-[1.02] hover:shadow-lg hover:shadow-blue-500/20 active:scale-[0.98] disabled:opacity-50 disabled:hover:scale-100"
                style={{
                  background: 'linear-gradient(135deg, #2563eb 0%, #0891b2 100%)',
                  boxShadow: '0 4px 20px rgba(37,99,235,0.3)',
                }}
              >
                {loading ? (
                  <>
                    <Loader2 size={20} className="animate-spin" />
                    Entrando...
                  </>
                ) : (
                  'Entrar'
                )}
              </button>
            </form>

            <p className="text-center text-gray-600 text-xs mt-8">
              CaixaCombo · Dashboard v1.1
            </p>
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
