export const DEFAULT_PERMISSIONS = Object.freeze({
  dashboard: false,
  produtos: false,
  categorias: false,
  vendas: false,
  caixa: false,
  auditoria: false
})

export const DEFAULT_COMPANY_FORM = Object.freeze({
  nome: '',
  cnpj: '',
  email: '',
  telefone: '',
  login: '',
  senha: '',
  permissoes: DEFAULT_PERMISSIONS
})

export const DEFAULT_CLIENT_FORM = Object.freeze({
  nome: '',
  cpfCnpj: '',
  telefone: '',
  email: '',
  endereco: '',
  cidade: '',
  cep: '',
  observacao: ''
})

export const DEFAULT_APP_CONFIG = Object.freeze({
  primaryColor: '#3b82f6',
  secondaryColor: '#06b6d4',
  accentColor: '#10b981',
  companyName: 'CaixaCombo',
  logoUrl: ''
})

export const createCompanyForm = (overrides = {}) => ({
  ...DEFAULT_COMPANY_FORM,
  ...overrides,
  permissoes: {
    ...DEFAULT_PERMISSIONS,
    ...(overrides.permissoes || {})
  }
})

export const createClientForm = (overrides = {}) => ({
  ...DEFAULT_CLIENT_FORM,
  ...overrides
})
