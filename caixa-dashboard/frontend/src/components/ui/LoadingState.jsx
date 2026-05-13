import { Loader2 } from 'lucide-react'

export default function LoadingState({ message, size = 32, className = 'py-12 text-center' }) {
  return (
    <div className={className}>
      <Loader2 size={size} className="animate-spin mx-auto text-blue-400 mb-3" />
      {message && <p className="text-gray-400">{message}</p>}
    </div>
  )
}
