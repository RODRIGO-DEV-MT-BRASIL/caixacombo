const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const DB_PATH = process.env.SQLITE_PATH || path.join(__dirname, 'caixacombo.db');
const DATA_FILE = path.join(__dirname, 'data.json');

const sqlite = new Database(DB_PATH);
sqlite.pragma('journal_mode = WAL');
sqlite.pragma('synchronous = NORMAL');

// Criar tabelas
sqlite.exec(`
  CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY,
    nome TEXT NOT NULL,
    descricao TEXT DEFAULT '',
    preco REAL NOT NULL,
    categoriaId INTEGER,
    codigoBarras TEXT DEFAULT '',
    estoque REAL DEFAULT 0,
    imagem TEXT DEFAULT '',
    unidade TEXT DEFAULT 'UN',
    ativo INTEGER DEFAULT 1,
    createdAt TEXT
  );

  CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY,
    nome TEXT NOT NULL,
    cor TEXT,
    icone TEXT,
    ordem INTEGER DEFAULT 0,
    ativa INTEGER DEFAULT 1
  );

  CREATE TABLE IF NOT EXISTS vendas (
    id TEXT PRIMARY KEY,
    deviceId TEXT,
    numero INTEGER,
    total REAL,
    formaPagamento TEXT,
    dataHora TEXT,
    createdAt TEXT,
    nomeOperador TEXT,
    itens TEXT DEFAULT '[]',
    observacao TEXT DEFAULT '',
    cancelada INTEGER DEFAULT 0,
    canceladaEm TEXT,
    canceladaPor TEXT,
    motivoCancelamento TEXT DEFAULT '',
    stoneAtk TEXT,
    atk TEXT,
    empresaId TEXT
  );

  CREATE TABLE IF NOT EXISTS operacoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,
    valor REAL,
    deviceId TEXT,
    nomeOperador TEXT DEFAULT '',
    observacao TEXT DEFAULT '',
    dataHora TEXT,
    timestamp INTEGER,
    empresaId TEXT
  );

  CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    empresaId TEXT,
    createdAt TEXT
  );

  CREATE TABLE IF NOT EXISTS dispositivos (
    deviceId TEXT PRIMARY KEY,
    deviceName TEXT,
    deviceType TEXT DEFAULT 'Android',
    status TEXT DEFAULT 'online',
    lockPassword TEXT,
    lockReason TEXT,
    lockedAt TEXT,
    empresaId TEXT,
    serialNumber TEXT,
    connectedAt TEXT,
    lastPoll TEXT,
    usageTimeLimit INTEGER,
    usageStartTime TEXT
  );

  CREATE TABLE IF NOT EXISTS empresas (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    cnpj TEXT DEFAULT '',
    endereco TEXT DEFAULT '',
    telefone TEXT DEFAULT '',
    email TEXT DEFAULT '',
    observacao TEXT DEFAULT '',
    createdAt TEXT,
    updatedAt TEXT
  );

  CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY,
    nome TEXT DEFAULT '',
    cpfCnpj TEXT DEFAULT '',
    telefone TEXT DEFAULT '',
    email TEXT DEFAULT '',
    endereco TEXT DEFAULT '',
    cidade TEXT DEFAULT '',
    cep TEXT DEFAULT '',
    observacao TEXT DEFAULT '',
    ativo INTEGER DEFAULT 1,
    dataCriacao INTEGER
  );

  CREATE TABLE IF NOT EXISTS caixa_sessoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    deviceId TEXT,
    operadorAbertura TEXT,
    operadorFechamento TEXT,
    aberturaEm TEXT,
    fechamentoEm TEXT,
    totalAbertura REAL DEFAULT 0,
    totalSuprimento REAL DEFAULT 0,
    totalSangria REAL DEFAULT 0,
    totalFechamento REAL DEFAULT 0,
    totalVendas REAL DEFAULT 0,
    qtdVendas INTEGER DEFAULT 0,
    vendasDinheiro REAL DEFAULT 0,
    vendasPix REAL DEFAULT 0,
    vendasCredito REAL DEFAULT 0,
    vendasDebito REAL DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS auditoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT,
    deviceId TEXT,
    descricao TEXT,
    username TEXT,
    timestamp TEXT
  );

  CREATE TABLE IF NOT EXISTS config (
    key TEXT PRIMARY KEY,
    value TEXT
  );
`);

// Migrar dados do data.json se existir e o SQLite estiver vazio
function migrateFromJson() {
  if (!fs.existsSync(DATA_FILE)) return;
  
  const produtosCount = sqlite.prepare('SELECT COUNT(*) as count FROM produtos').get().count;
  if (produtosCount > 0) return; // Já tem dados, não migrar

  console.log('📦 Migrando dados do data.json para SQLite...');
  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf8');
    if (!raw.trim()) return;
    const data = JSON.parse(raw);

    const insertProduto = sqlite.prepare(`INSERT OR REPLACE INTO produtos (id, nome, descricao, preco, categoriaId, codigoBarras, estoque, imagem, unidade, ativo, createdAt)
      VALUES (@id, @nome, @descricao, @preco, @categoriaId, @codigoBarras, @estoque, @imagem, @unidade, @ativo, @createdAt)`);
    const insertCategoria = sqlite.prepare(`INSERT OR REPLACE INTO categorias (id, nome, cor, icone, ordem, ativa) VALUES (@id, @nome, @cor, @icone, @ordem, @ativa)`);
    const insertVenda = sqlite.prepare(`INSERT OR REPLACE INTO vendas (id, deviceId, numero, total, formaPagamento, dataHora, createdAt, nomeOperador, itens, observacao, stoneAtk, atk, empresaId, cancelada, canceladaEm, canceladaPor, motivoCancelamento)
      VALUES (@id, @deviceId, @numero, @total, @formaPagamento, @dataHora, @createdAt, @nomeOperador, @itens, @observacao, @stoneAtk, @atk, @empresaId, @cancelada, @canceladaEm, @canceladaPor, @motivoCancelamento)`);
    const insertOperacao = sqlite.prepare(`INSERT INTO operacoes (tipo, valor, deviceId, nomeOperador, observacao, dataHora, timestamp, empresaId) VALUES (@tipo, @valor, @deviceId, @nomeOperador, @observacao, @dataHora, @timestamp, @empresaId)`);
    const insertUsuario = sqlite.prepare(`INSERT OR REPLACE INTO usuarios (id, username, password, role, empresaId, createdAt) VALUES (@id, @username, @password, @role, @empresaId, @createdAt)`);
    const insertDispositivo = sqlite.prepare(`INSERT OR REPLACE INTO dispositivos (deviceId, deviceName, deviceType, status, lockPassword, lockReason, lockedAt, empresaId, serialNumber, connectedAt, usageTimeLimit, usageStartTime)
      VALUES (@deviceId, @deviceName, @deviceType, @status, @lockPassword, @lockReason, @lockedAt, @empresaId, @serialNumber, @connectedAt, @usageTimeLimit, @usageStartTime)`);
    const insertEmpresa = sqlite.prepare(`INSERT OR REPLACE INTO empresas (id, nome, cnpj, endereco, telefone, email, observacao, createdAt, updatedAt) VALUES (@id, @nome, @cnpj, @endereco, @telefone, @email, @observacao, @createdAt, @updatedAt)`);
    const insertCliente = sqlite.prepare(`INSERT OR REPLACE INTO clientes (id, nome, cpfCnpj, telefone, email, endereco, cidade, cep, observacao, ativo, dataCriacao) VALUES (@id, @nome, @cpfCnpj, @telefone, @email, @endereco, @cidade, @cep, @observacao, @ativo, @dataCriacao)`);
    const insertSessao = sqlite.prepare(`INSERT INTO caixa_sessoes (deviceId, operadorAbertura, operadorFechamento, aberturaEm, fechamentoEm, totalAbertura, totalSuprimento, totalSangria, totalFechamento, totalVendas, qtdVendas, vendasDinheiro, vendasPix, vendasCredito, vendasDebito)
      VALUES (@deviceId, @operadorAbertura, @operadorFechamento, @aberturaEm, @fechamentoEm, @totalAbertura, @totalSuprimento, @totalSangria, @totalFechamento, @totalVendas, @qtdVendas, @vendasDinheiro, @vendasPix, @vendasCredito, @vendasDebito)`);
    const insertAuditoria = sqlite.prepare(`INSERT INTO auditoria (tipo, deviceId, descricao, username, timestamp) VALUES (@tipo, @deviceId, @descricao, @username, @timestamp)`);

    const migrateAll = sqlite.transaction(() => {
      if (data.produtos) data.produtos.forEach(p => {
        insertProduto.run({ id: p.id, nome: p.nome, descricao: p.descricao || '', preco: p.preco, categoriaId: p.categoriaId, codigoBarras: p.codigoBarras || '', estoque: p.estoque || 0, imagem: p.imagem || '', unidade: p.unidade || 'UN', ativo: p.ativo !== false ? 1 : 0, createdAt: p.createdAt || new Date().toISOString() });
      });
      if (data.categorias) data.categorias.forEach(c => {
        insertCategoria.run({ id: c.id, nome: c.nome, cor: c.cor || null, icone: c.icone || null, ordem: c.ordem || 0, ativa: c.ativa !== false ? 1 : 0 });
      });
      if (data.vendas) data.vendas.forEach(v => {
        insertVenda.run({ id: v.id, deviceId: v.deviceId || null, numero: v.numero || null, total: v.total || 0, formaPagamento: v.formaPagamento || '', dataHora: v.dataHora || null, createdAt: v.createdAt || null, nomeOperador: v.nomeOperador || '', itens: JSON.stringify(v.itens || []), observacao: v.observacao || '', stoneAtk: v.stoneAtk || v.atk || null, atk: v.atk || null, empresaId: v.empresaId || null, cancelada: v.cancelada ? 1 : 0, canceladaEm: v.canceladaEm || null, canceladaPor: v.canceladaPor || null, motivoCancelamento: v.motivoCancelamento || '' });
      });
      if (data.operacoes) data.operacoes.forEach(o => {
        insertOperacao.run({ tipo: o.tipo, valor: o.valor || 0, deviceId: o.deviceId || null, nomeOperador: o.nomeOperador || '', observacao: o.observacao || '', dataHora: o.dataHora || null, timestamp: o.timestamp || null, empresaId: o.empresaId || null });
      });
      if (data.usuarios) data.usuarios.forEach(u => {
        insertUsuario.run({ id: u.id, username: u.username, password: u.password, role: u.role || 'admin', empresaId: u.empresaId || null, createdAt: u.createdAt || null });
      });
      if (data.dispositivos) data.dispositivos.forEach(d => {
        insertDispositivo.run({ deviceId: d.deviceId, deviceName: d.deviceName || null, deviceType: d.deviceType || 'Android', status: d.status || 'online', lockPassword: d.lockPassword || null, lockReason: d.lockReason || null, lockedAt: d.lockedAt || null, empresaId: d.empresaId || null, serialNumber: d.serialNumber || null, connectedAt: d.connectedAt || null, usageTimeLimit: d.usageTimeLimit || null, usageStartTime: d.usageStartTime || null });
      });
      if (data.empresas) data.empresas.forEach(e => {
        insertEmpresa.run({ id: e.id, nome: e.nome, cnpj: e.cnpj || '', endereco: e.endereco || '', telefone: e.telefone || '', email: e.email || '', observacao: e.observacao || '', createdAt: e.createdAt || null, updatedAt: e.updatedAt || null });
      });
      if (data.clientes) data.clientes.forEach(c => {
        insertCliente.run({ id: c.id, nome: c.nome || '', cpfCnpj: c.cpfCnpj || '', telefone: c.telefone || '', email: c.email || '', endereco: c.endereco || '', cidade: c.cidade || '', cep: c.cep || '', observacao: c.observacao || '', ativo: c.ativo !== false ? 1 : 0, dataCriacao: c.dataCriacao || null });
      });
      if (data.caixaSessoes) data.caixaSessoes.forEach(s => {
        insertSessao.run({ deviceId: s.deviceId || null, operadorAbertura: s.operadorAbertura || '', operadorFechamento: s.operadorFechamento || '', aberturaEm: s.aberturaEm || null, fechamentoEm: s.fechamentoEm || null, totalAbertura: s.totalAbertura || 0, totalSuprimento: s.totalSuprimento || 0, totalSangria: s.totalSangria || 0, totalFechamento: s.totalFechamento || 0, totalVendas: s.totalVendas || 0, qtdVendas: s.qtdVendas || 0, vendasDinheiro: s.vendasDinheiro || 0, vendasPix: s.vendasPix || 0, vendasCredito: s.vendasCredito || 0, vendasDebito: s.vendasDebito || 0 });
      });
      if (data.auditoria) data.auditoria.forEach(a => {
        insertAuditoria.run({ tipo: a.tipo || '', deviceId: a.deviceId || '', descricao: a.descricao || '', username: a.username || '', timestamp: a.timestamp || new Date().toISOString() });
      });
    });

    migrateAll();
    console.log('✅ Migração do data.json para SQLite concluída!');
  } catch (err) {
    console.error('❌ Erro na migração:', err.message);
  }
}

migrateFromJson();

// ==================== DB PROXY (mantém interface compatível com server.js) ====================
// Carrega tudo em memória como arrays (como o data.json fazia) mas salva no SQLite

function loadFromSqlite() {
  const produtos = sqlite.prepare('SELECT * FROM produtos').all().map(p => ({ ...p, ativo: !!p.ativo, categoriaId: p.categoriaId || null }));
  const categorias = sqlite.prepare('SELECT * FROM categorias').all().map(c => ({ ...c, ativa: !!c.ativa }));
  const vendas = sqlite.prepare('SELECT * FROM vendas').all().map(v => ({ ...v, itens: JSON.parse(v.itens || '[]'), cancelada: !!v.cancelada }));
  const operacoes = sqlite.prepare('SELECT * FROM operacoes').all();
  const usuarios = sqlite.prepare('SELECT * FROM usuarios').all();
  const dispositivos = sqlite.prepare('SELECT * FROM dispositivos').all();
  const empresas = sqlite.prepare('SELECT * FROM empresas').all();
  const clientes = sqlite.prepare('SELECT * FROM clientes').all().map(c => ({ ...c, ativo: !!c.ativo }));
  const caixaSessoes = sqlite.prepare('SELECT * FROM caixa_sessoes').all();
  const auditoria = sqlite.prepare('SELECT * FROM auditoria').all();
  const configRow = sqlite.prepare("SELECT value FROM config WHERE key = 'app'").get();
  const config = configRow ? JSON.parse(configRow.value) : {};

  return { produtos, categorias, vendas, operacoes, usuarios, dispositivos, empresas, clientes, caixaSessoes, auditoria, config };
}

let db = loadFromSqlite();

// Salvar tudo no SQLite
function saveData() {
  const saveAll = sqlite.transaction(() => {
    // Produtos
    sqlite.prepare('DELETE FROM produtos').run();
    const insP = sqlite.prepare(`INSERT OR REPLACE INTO produtos (id, nome, descricao, preco, categoriaId, codigoBarras, estoque, imagem, unidade, ativo, createdAt) VALUES (@id, @nome, @descricao, @preco, @categoriaId, @codigoBarras, @estoque, @imagem, @unidade, @ativo, @createdAt)`);
    db.produtos.forEach(p => insP.run({ id: p.id, nome: p.nome, descricao: p.descricao || '', preco: p.preco, categoriaId: p.categoriaId, codigoBarras: p.codigoBarras || '', estoque: p.estoque || 0, imagem: p.imagem || '', unidade: p.unidade || 'UN', ativo: p.ativo !== false ? 1 : 0, createdAt: p.createdAt || new Date().toISOString() }));

    // Categorias
    sqlite.prepare('DELETE FROM categorias').run();
    const insC = sqlite.prepare(`INSERT OR REPLACE INTO categorias (id, nome, cor, icone, ordem, ativa) VALUES (@id, @nome, @cor, @icone, @ordem, @ativa)`);
    db.categorias.forEach(c => insC.run({ id: c.id, nome: c.nome, cor: c.cor || null, icone: c.icone || null, ordem: c.ordem || 0, ativa: c.ativa !== false ? 1 : 0 }));

    // Vendas
    sqlite.prepare('DELETE FROM vendas').run();
    const insV = sqlite.prepare(`INSERT OR REPLACE INTO vendas (id, deviceId, numero, total, formaPagamento, dataHora, createdAt, nomeOperador, itens, observacao, stoneAtk, atk, empresaId, cancelada, canceladaEm, canceladaPor, motivoCancelamento) VALUES (@id, @deviceId, @numero, @total, @formaPagamento, @dataHora, @createdAt, @nomeOperador, @itens, @observacao, @stoneAtk, @atk, @empresaId, @cancelada, @canceladaEm, @canceladaPor, @motivoCancelamento)`);
    db.vendas.forEach(v => insV.run({ id: v.id, deviceId: v.deviceId || null, numero: v.numero || null, total: v.total || 0, formaPagamento: v.formaPagamento || '', dataHora: v.dataHora || null, createdAt: v.createdAt || null, nomeOperador: v.nomeOperador || '', itens: JSON.stringify(v.itens || []), observacao: v.observacao || '', stoneAtk: v.stoneAtk || v.atk || null, atk: v.atk || null, empresaId: v.empresaId || null, cancelada: v.cancelada ? 1 : 0, canceladaEm: v.canceladaEm || null, canceladaPor: v.canceladaPor || null, motivoCancelamento: v.motivoCancelamento || '' }));

    // Operações
    sqlite.prepare('DELETE FROM operacoes').run();
    const insO = sqlite.prepare(`INSERT INTO operacoes (tipo, valor, deviceId, nomeOperador, observacao, dataHora, timestamp, empresaId) VALUES (@tipo, @valor, @deviceId, @nomeOperador, @observacao, @dataHora, @timestamp, @empresaId)`);
    db.operacoes.forEach(o => insO.run({ tipo: o.tipo, valor: o.valor || 0, deviceId: o.deviceId || null, nomeOperador: o.nomeOperador || '', observacao: o.observacao || '', dataHora: o.dataHora || null, timestamp: o.timestamp || null, empresaId: o.empresaId || null }));

    // Usuários
    sqlite.prepare('DELETE FROM usuarios').run();
    const insU = sqlite.prepare(`INSERT OR REPLACE INTO usuarios (id, username, password, role, empresaId, createdAt) VALUES (@id, @username, @password, @role, @empresaId, @createdAt)`);
    db.usuarios.forEach(u => insU.run({ id: u.id, username: u.username, password: u.password, role: u.role || 'admin', empresaId: u.empresaId || null, createdAt: u.createdAt || null }));

    // Dispositivos
    sqlite.prepare('DELETE FROM dispositivos').run();
    const insD = sqlite.prepare(`INSERT OR REPLACE INTO dispositivos (deviceId, deviceName, deviceType, status, lockPassword, lockReason, lockedAt, empresaId, serialNumber, connectedAt, usageTimeLimit, usageStartTime) VALUES (@deviceId, @deviceName, @deviceType, @status, @lockPassword, @lockReason, @lockedAt, @empresaId, @serialNumber, @connectedAt, @usageTimeLimit, @usageStartTime)`);
    db.dispositivos.forEach(d => insD.run({ deviceId: d.deviceId, deviceName: d.deviceName || null, deviceType: d.deviceType || 'Android', status: d.status || 'online', lockPassword: d.lockPassword || null, lockReason: d.lockReason || null, lockedAt: d.lockedAt || null, empresaId: d.empresaId || null, serialNumber: d.serialNumber || null, connectedAt: d.connectedAt || null, usageTimeLimit: d.usageTimeLimit || null, usageStartTime: d.usageStartTime || null }));

    // Empresas
    sqlite.prepare('DELETE FROM empresas').run();
    const insE = sqlite.prepare(`INSERT OR REPLACE INTO empresas (id, nome, cnpj, endereco, telefone, email, observacao, createdAt, updatedAt) VALUES (@id, @nome, @cnpj, @endereco, @telefone, @email, @observacao, @createdAt, @updatedAt)`);
    db.empresas.forEach(e => insE.run({ id: e.id, nome: e.nome, cnpj: e.cnpj || '', endereco: e.endereco || '', telefone: e.telefone || '', email: e.email || '', observacao: e.observacao || '', createdAt: e.createdAt || null, updatedAt: e.updatedAt || null }));

    // Clientes
    sqlite.prepare('DELETE FROM clientes').run();
    const insCli = sqlite.prepare(`INSERT OR REPLACE INTO clientes (id, nome, cpfCnpj, telefone, email, endereco, cidade, cep, observacao, ativo, dataCriacao) VALUES (@id, @nome, @cpfCnpj, @telefone, @email, @endereco, @cidade, @cep, @observacao, @ativo, @dataCriacao)`);
    db.clientes.forEach(c => insCli.run({ id: c.id, nome: c.nome || '', cpfCnpj: c.cpfCnpj || '', telefone: c.telefone || '', email: c.email || '', endereco: c.endereco || '', cidade: c.cidade || '', cep: c.cep || '', observacao: c.observacao || '', ativo: c.ativo !== false ? 1 : 0, dataCriacao: c.dataCriacao || null }));

    // Caixa Sessões
    sqlite.prepare('DELETE FROM caixa_sessoes').run();
    const insS = sqlite.prepare(`INSERT INTO caixa_sessoes (deviceId, operadorAbertura, operadorFechamento, aberturaEm, fechamentoEm, totalAbertura, totalSuprimento, totalSangria, totalFechamento, totalVendas, qtdVendas, vendasDinheiro, vendasPix, vendasCredito, vendasDebito) VALUES (@deviceId, @operadorAbertura, @operadorFechamento, @aberturaEm, @fechamentoEm, @totalAbertura, @totalSuprimento, @totalSangria, @totalFechamento, @totalVendas, @qtdVendas, @vendasDinheiro, @vendasPix, @vendasCredito, @vendasDebito)`);
    (db.caixaSessoes || []).forEach(s => insS.run({ deviceId: s.deviceId || null, operadorAbertura: s.operadorAbertura || '', operadorFechamento: s.operadorFechamento || '', aberturaEm: s.aberturaEm || null, fechamentoEm: s.fechamentoEm || null, totalAbertura: s.totalAbertura || 0, totalSuprimento: s.totalSuprimento || 0, totalSangria: s.totalSangria || 0, totalFechamento: s.totalFechamento || 0, totalVendas: s.totalVendas || 0, qtdVendas: s.qtdVendas || 0, vendasDinheiro: s.vendasDinheiro || 0, vendasPix: s.vendasPix || 0, vendasCredito: s.vendasCredito || 0, vendasDebito: s.vendasDebito || 0 }));

    // Auditoria
    sqlite.prepare('DELETE FROM auditoria').run();
    const insA = sqlite.prepare(`INSERT INTO auditoria (tipo, deviceId, descricao, username, timestamp) VALUES (@tipo, @deviceId, @descricao, @username, @timestamp)`);
    (db.auditoria || []).forEach(a => insA.run({ tipo: a.tipo || '', deviceId: a.deviceId || '', descricao: a.descricao || '', username: a.username || '', timestamp: a.timestamp || new Date().toISOString() }));

    // Config
    sqlite.prepare("INSERT OR REPLACE INTO config (key, value) VALUES ('app', ?)").run(JSON.stringify(db.config || {}));
  });

  saveAll();
}

function saveAuditoria() {
  // Já é salvo no saveData()
}

module.exports = { db, saveData, saveAuditoria, sqlite, loadFromSqlite };
