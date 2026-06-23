// =============================================================================
// CAIXA COMBO - Camada de Compatibilidade db (PostgreSQL → In-Memory Sync)
// =============================================================================
// Carrega dados do PostgreSQL em arrays em memória (compatível com código existente).
// Escrita: atualiza memória + enfileira sync para PostgreSQL.
// =============================================================================

const { query, queryOne, queryMany, connectDatabase, seedAdmin } = require('./database');

// Objeto db global — mesmas propriedades que o código existente espera
const db = {
  empresas: [],
  usuarios: [],
  funcionarios: [],
  categorias: [],
  produtos: [],
  vendas: [],
  operacoes: [],
  dispositivos: [],
  auditoria: [],
  config: {},
  impressaoTemplate: null,
};

// =============================================================================
// LOAD: carregar todas as tabelas do PostgreSQL para memória
// =============================================================================
async function loadAll() {
  console.log('📥 Carregando dados do PostgreSQL...');
  const t0 = Date.now();

  const [empresas, usuarios, funcionarios, categorias, produtos, vendas, operacoes, dispositivos, auditoria] = await Promise.all([
    queryMany('SELECT * FROM empresas ORDER BY created_at'),
    queryMany('SELECT * FROM usuarios ORDER BY created_at'),
    queryMany('SELECT * FROM funcionarios ORDER BY created_at'),
    queryMany('SELECT * FROM categorias ORDER BY ordem, created_at'),
    queryMany('SELECT * FROM produtos ORDER BY created_at'),
    queryMany(`SELECT * FROM vendas ORDER BY created_at DESC LIMIT 10000`),
    queryMany(`SELECT * FROM operacoes ORDER BY created_at DESC LIMIT 10000`),
    queryMany('SELECT * FROM dispositivos'),
    queryMany('SELECT * FROM auditoria ORDER BY created_at DESC LIMIT 2000'),
  ]);

  db.empresas = empresas;
  db.usuarios = usuarios;
  db.funcionarios = funcionarios;
  db.categorias = categorias;
  db.produtos = produtos;
  db.vendas = vendas;
  db.operacoes = operacoes;
  db.dispositivos = dispositivos;
  db.auditoria = auditoria;

  // Converter campos snake_case → camelCase para compatibilidade com o código existente
  db.empresas = db.empresas.map(mapEmpresa);
  db.usuarios = db.usuarios.map(mapUsuario);
  db.funcionarios = db.funcionarios.map(mapFuncionario);
  db.categorias = db.categorias.map(mapCategoria);
  db.produtos = db.produtos.map(mapProduto);
  db.vendas = db.vendas.map(mapVenda);
  db.operacoes = db.operacoes.map(mapOperacao);
  db.dispositivos = db.dispositivos.map(mapDispositivo);
  db.auditoria = db.auditoria.map(mapAuditoria);

  console.log(`📊 PostgreSQL carregado em ${Date.now() - t0}ms: ${db.produtos.length} produtos, ${db.categorias.length} categorias, ${db.vendas.length} vendas, ${db.operacoes.length} operações, ${db.usuarios.length} usuários, ${db.dispositivos.length} dispositivos`);
}

// =============================================================================
// MAPPERS: snake_case (DB) → camelCase (código existente)
// =============================================================================
function mapEmpresa(r) {
  return {
    ...r,
    primaryColor: r.primary_color || '#3b82f6',
    secondaryColor: r.secondary_color || '#06b6d4',
    accentColor: r.accent_color || '#10b981',
    logoUrl: r.logo_url || '',
    paginasPermitidas: r.paginas_permitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa', 'terminais', 'impressao', 'config'],
    impressaoTemplate: r.impressao_template || null,
    ativo: r.ativa,
  };
}

function mapUsuario(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
    empresaNome: r.empresa_nome,
    empresaSlug: r.empresa_slug,
    paginasPermitidas: r.paginas_permitidas || ['dashboard', 'vendas', 'caixa'],
  };
}

function mapFuncionario(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
  };
}

function mapCategoria(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
  };
}

function mapProduto(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
    categoriaId: r.categoria_id,
    codigoBarras: r.codigo_barras,
  };
}

function mapVenda(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
    dataHora: r.data_hora,
    formaPagamento: r.forma_pagamento,
    valorRecebido: r.valor_recebido,
    stoneAtk: r.stone_atk,
    deviceId: r.device_id,
    empresaNome: r.empresa_nome,
    empresaSlug: r.empresa_slug,
    vendedorId: r.vendedor_id,
    vendedorNome: r.vendedor_nome,
    canceladaEm: r.cancelada_em,
    canceladaPor: r.cancelada_por,
    motivoCancelamento: r.motivo_cancelamento,
  };
}

function mapOperacao(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
    nomeOperador: r.nome_operador,
    operadorId: r.operador_id,
    dataHora: r.data_hora,
    valorInicial: r.valor_inicial,
    deviceId: r.device_id,
    empresaNome: r.empresa_nome,
    empresaSlug: r.empresa_slug,
  };
}

function mapDispositivo(r) {
  return {
    ...r,
    empresaId: r.empresa_id,
    deviceName: r.device_name,
    deviceType: r.device_type,
    serialNumber: r.serial_number,
    lockPassword: r.lock_password,
    empresaNome: r.empresa_nome,
    empresaSlug: r.empresa_slug,
    usageTimeLimit: r.usage_time_limit,
    usageStartTime: r.usage_start_time,
    lastPoll: r.last_poll,
    lastLogin: r.last_login,
    lastLoginUser: r.last_login_user,
  };
}

function mapAuditoria(r) {
  return {
    ...r,
    deviceId: r.device_id,
    deviceName: r.device_name,
    empresaId: r.empresa_id,
  };
}

// =============================================================================
// SAVE: persistir alterações de volta ao PostgreSQL
// =============================================================================

let _saveTimer = null;
const SAVE_DEBOUNCE_MS = 3000;

// Debounced save — evita múltiplas escritas seguidas
function debouncedSave() {
  if (_saveTimer) clearTimeout(_saveTimer);
  _saveTimer = setTimeout(() => flushToDb(), SAVE_DEBOUNCE_MS);
}

// Flush imediato
async function flushToDb() {
  try {
    await Promise.all([
      syncTable('empresas', db.empresas, mapEmpresaToDb),
      syncTable('usuarios', db.usuarios, mapUsuarioToDb),
      syncTable('funcionarios', db.funcionarios, mapFuncionarioToDb),
      syncTable('categorias', db.categorias, mapCategoriaToDb),
      syncTable('produtos', db.produtos, mapProdutoToDb),
      syncTable('vendas', db.vendas, mapVendaToDb),
      syncTable('operacoes', db.operacoes, mapOperacaoToDb),
      syncTable('dispositivos', db.dispositivos, mapDispositivoToDb),
    ]);
  } catch (err) {
    console.error('❌ Erro ao sincronizar dados:', err.message);
  }
}

// Sincronizar uma tabela: upsert de todos os registros
async function syncTable(tableName, data, mapper) {
  if (!data || data.length === 0) return;

  for (const item of data) {
    if (!item.id) continue;
    try {
      const dbRow = mapper(item);
      const keys = Object.keys(dbRow);
      const values = keys.map(k => dbRow[k]);
      const placeholders = keys.map((_, i) => `$${i + 1}`);

      // Usar UPSERT (INSERT ... ON CONFLICT UPDATE)
      const conflictKey = getConflictKey(tableName);
      const updateSet = keys.filter(k => k !== conflictKey).map((k, i) => `${k} = EXCLUDED.${k}`);

      const sql = `
        INSERT INTO ${tableName} (${keys.join(', ')})
        VALUES (${placeholders.join(', ')})
        ON CONFLICT (${conflictKey}) DO UPDATE SET ${updateSet.join(', ')}
      `;
      await query(sql, values);
    } catch (err) {
      console.error(`❌ Erro ao salvar ${tableName}:`, err.message);
    }
  }
}

function getConflictKey(tableName) {
  const keys = {
    empresas: 'id',
    usuarios: 'id',
    funcionarios: 'id',
    categorias: 'id',
    produtos: 'id',
    vendas: 'id',
    operacoes: 'id',
    dispositivos: 'device_id',
  };
  return keys[tableName] || 'id';
}

// =============================================================================
// MAPPERS reversos: camelCase → snake_case (DB)
// =============================================================================
function mapEmpresaToDb(item) {
  return {
    id: item.id,
    nome: item.nome,
    slug: item.slug,
    cnpj: item.cnpj,
    email: item.email,
    telefone: item.telefone,
    login: item.login,
    senha: item.senha,
    permissoes: typeof item.permissoes === 'object' ? JSON.stringify(item.permissoes) : item.permissoes || '{}',
    primary_color: item.primaryColor || item.primary_color || '#3b82f6',
    secondary_color: item.secondaryColor || item.secondary_color || '#06b6d4',
    accent_color: item.accentColor || item.accent_color || '#10b981',
    logo_url: item.logoUrl || item.logo_url || '',
    paginas_permitidas: item.paginasPermitidas || item.paginas_permitidas || ['dashboard'],
    impressao_template: item.impressaoTemplate ? JSON.stringify(item.impressaoTemplate) : null,
    ativa: item.ativo !== undefined ? item.ativo : true,
  };
}

function mapUsuarioToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    username: item.username,
    nome: item.nome,
    email: item.email,
    password: item.password,
    role: item.role,
    ativo: item.ativo !== undefined ? item.ativo : true,
    empresa_nome: item.empresaNome || item.empresa_nome,
    empresa_slug: item.empresaSlug || item.empresa_slug,
    permissoes: typeof item.permissoes === 'object' ? JSON.stringify(item.permissoes) : item.permissoes || '{}',
    paginas_permitidas: item.paginasPermitidas || ['dashboard', 'vendas', 'caixa'],
  };
}

function mapFuncionarioToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    nome: item.nome,
    codigo: item.codigo,
    cpf: item.cpf,
    telefone: item.telefone,
    email: item.email,
    password: item.password,
    pin: item.pin,
    cargo: item.cargo || 'FUNCIONARIO',
    ativo: item.ativo !== undefined ? item.ativo : true,
    permissoes: typeof item.permissoes === 'object' ? JSON.stringify(item.permissoes) : item.permissoes || '{}',
  };
}

function mapCategoriaToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    nome: item.nome,
    descricao: item.descricao,
    cor: item.cor,
    icone: item.icone,
    ordem: item.ordem || 0,
    ativa: item.ativa !== undefined ? item.ativa : true,
  };
}

function mapProdutoToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    categoria_id: item.categoriaId || item.categoria_id,
    nome: item.nome,
    descricao: item.descricao,
    preco: item.preco,
    codigo_barras: item.codigoBarras || item.codigo_barras,
    estoque: item.estoque || 0,
    unidade: item.unidade || 'UN',
    imagem: item.imagem,
    ativo: item.ativo !== undefined ? item.ativo : true,
  };
}

function mapVendaToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    numero: item.numero,
    data_hora: item.dataHora || item.data_hora || new Date(),
    subtotal: item.subtotal || 0,
    desconto: item.desconto || 0,
    total: item.total || 0,
    forma_pagamento: item.formaPagamento || item.forma_pagamento,
    valor_recebido: item.valorRecebido || item.valor_recebido || 0,
    troco: item.troco || 0,
    status: item.status || 'FINALIZADA',
    cancelada: item.cancelada || false,
    cancelada_em: item.canceladaEm || item.cancelada_em,
    cancelada_por: item.canceladaPor || item.cancelada_por,
    motivo_cancelamento: item.motivoCancelamento || item.motivo_cancelamento,
    stone_atk: item.stoneAtk || item.stone_atk,
    device_id: item.deviceId || item.device_id,
    empresa_nome: item.empresaNome || item.empresa_nome,
    empresa_slug: item.empresaSlug || item.empresa_slug,
    vendedor_id: item.vendedorId || item.vendedor_id,
    vendedor_nome: item.vendedorNome || item.vendedor_nome,
    observacao: item.observacao,
  };
}

function mapOperacaoToDb(item) {
  return {
    id: item.id,
    empresa_id: item.empresaId || item.empresa_id,
    tipo: item.tipo,
    nome_operador: item.nomeOperador || item.nome_operador,
    operador_id: item.operadorId || item.operador_id,
    data_hora: item.dataHora || item.data_hora || new Date(),
    valor: item.valor || 0,
    valor_inicial: item.valorInicial || item.valor_inicial,
    observacao: item.observacao,
    device_id: item.deviceId || item.device_id,
    empresa_nome: item.empresaNome || item.empresa_nome,
    empresa_slug: item.empresaSlug || item.empresa_slug,
  };
}

function mapDispositivoToDb(item) {
  return {
    id: item.id,
    device_id: item.deviceId || item.device_id,
    device_name: item.deviceName || item.device_name || 'Dispositivo',
    device_type: item.deviceType || item.device_type || 'Android',
    serial_number: item.serialNumber || item.serial_number,
    status: item.status || 'offline',
    lock_password: item.lockPassword || item.lock_password,
    empresa_id: item.empresaId || item.empresa_id,
    empresa_nome: item.empresaNome || item.empresa_nome,
    empresa_slug: item.empresaSlug || item.empresa_slug,
    usage_time_limit: item.usageTimeLimit || item.usage_time_limit,
    usage_start_time: item.usageStartTime || item.usage_start_time,
    last_poll: item.lastPoll || item.last_poll,
    last_login: item.lastLogin || item.last_login,
    last_login_user: item.lastLoginUser || item.last_login_user,
  };
}

// =============================================================================
// EXPORTS
// =============================================================================
module.exports = {
  db,
  loadAll,
  debouncedSave,
  flushToDb,
  saveAuditoria: debouncedSave,
  saveData: debouncedSave,
  isConnected: () => true,
};
