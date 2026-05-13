/**
 * Tipos compartilhados do dashboard CaixaCombo.
 *
 * O projeto usa JavaScript; este arquivo centraliza contratos via JSDoc para
 * documentação, autocomplete e futuras migrações incrementais para TypeScript.
 */

/**
 * @typedef {'admin' | 'empresa' | 'usuario'} UserRole
 */

/**
 * @typedef {Object} Permissions
 * @property {boolean} dashboard
 * @property {boolean} produtos
 * @property {boolean} categorias
 * @property {boolean} vendas
 * @property {boolean} caixa
 * @property {boolean} auditoria
 */

/**
 * @typedef {Object} EmpresaForm
 * @property {string} nome
 * @property {string} cnpj
 * @property {string} email
 * @property {string} telefone
 * @property {string} login
 * @property {string} senha
 * @property {Permissions} permissoes
 */

/**
 * @typedef {Object} ClienteForm
 * @property {string} nome
 * @property {string} cpfCnpj
 * @property {string} telefone
 * @property {string} email
 * @property {string} endereco
 * @property {string} cidade
 * @property {string} cep
 * @property {string} observacao
 */

/**
 * @typedef {Object} AppConfig
 * @property {string} primaryColor
 * @property {string} secondaryColor
 * @property {string} accentColor
 * @property {string} companyName
 * @property {string} logoUrl
 */

/**
 * @typedef {Object} TerminalDevice
 * @property {string} deviceId
 * @property {string} [deviceName]
 * @property {'online' | 'offline' | 'locked' | 'in_use'} [status]
 * @property {boolean} [online]
 * @property {string} [empresaId]
 */

export {}
