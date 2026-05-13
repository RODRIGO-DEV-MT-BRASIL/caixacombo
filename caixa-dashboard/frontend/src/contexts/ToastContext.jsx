import { createContext, useContext } from 'react'
import { useToast } from '../components/Toast'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const toast = useToast()

  return (
    <ToastContext.Provider value={toast}>
      {children}
    </ToastContext.Provider>
  )
}

export const useToastContext = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToastContext deve ser usado dentro de ToastProvider')
  }
  return context
}
export { useToast } from '../components/Toast'
