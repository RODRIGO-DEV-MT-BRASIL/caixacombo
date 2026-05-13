import { Search } from 'lucide-react'

export default function SearchField({
  value,
  onChange,
  placeholder = 'Buscar...',
  className = '',
  inputClassName = 'input-field pl-9 py-2.5 text-sm'
}) {
  return (
    <div className={`relative ${className}`.trim()}>
      <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
      <input
        type="text"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={inputClassName}
        placeholder={placeholder}
      />
    </div>
  )
}
