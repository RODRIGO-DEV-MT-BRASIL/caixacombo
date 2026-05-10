const mongoose = require('mongoose');

// ==================== SCHEMAS ====================

const ProdutoSchema = new mongoose.Schema({
  id: { type: Number, required: true },
  nome: { type: String, required: true },
  descricao: { type: String, default: '' },
  preco: { type: Number, required: true },
  categoriaId: { type: Number, default: null },
  codigoBarras: { type: String, default: '' },
  estoque: { type: Number, default: 0 },
  imagem: { type: String, default: '' },
  unidade: { type: String, default: 'UN' },
  ativo: { type: Boolean, default: true },
  createdAt: { type: String }
}, { versionKey: false });

const CategoriaSchema = new mongoose.Schema({
  id: { type: Number, required: true },
  nome: { type: String, required: true },
  cor: { type: String, default: null },
  icone: { type: String, default: null },
  ordem: { type: Number, default: 0 },
  ativa: { type: Boolean, default: true }
}, { versionKey: false });

const VendaItemSchema = new mongoose.Schema({
  produtoId: mongoose.Schema.Types.Mixed,
  produtoNome: { type: String, default: '' },
  nome: { type: String, default: '' },
  quantidade: { type: Number, default: 0 },
  precoUnitario: { type: Number, default: 0 },
  total: { type: Number, default: 0 }
}, { _id: false });

const VendaSchema = new mongoose.Schema({
  id: { type: String, required: true },
  deviceId: { type: String, default: null },
  numero: { type: Number, default: null },
  total: { type: Number, default: 0 },
  formaPagamento: { type: String, default: '' },
  dataHora: { type: String, default: null },
  createdAt: { type: String, default: null },
  nomeOperador: { type: String, default: '' },
  itens: { type: [VendaItemSchema], default: [] },
  observacao: { type: String, default: '' },
  cancelada: { type: Boolean, default: false },
  canceladaEm: { type: String, default: null },
  canceladaPor: { type: String, default: null },
  motivoCancelamento: { type: String, default: '' },
  stoneAtk: { type: String, default: null },
  atk: { type: String, default: null },
  empresaId: { type: String, default: null }
}, { versionKey: false });

const OperacaoSchema = new mongoose.Schema({
  tipo: { type: String, required: true },
  valor: { type: Number, default: 0 },
  deviceId: { type: String, default: null },
  nomeOperador: { type: String, default: '' },
  observacao: { type: String, default: '' },
  dataHora: { type: String, default: null },
  timestamp: { type: Number, default: null },
  empresaId: { type: String, default: null }
}, { versionKey: false });

const UsuarioSchema = new mongoose.Schema({
  id: { type: Number, required: true },
  username: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  role: { type: String, default: 'admin' },
  empresaId: { type: String, default: null },
  createdAt: { type: String, default: null }
}, { versionKey: false });

const DispositivoSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  deviceName: { type: String, default: null },
  deviceType: { type: String, default: 'Android' },
  status: { type: String, default: 'online' },
  lockPassword: { type: String, default: null },
  lockReason: { type: String, default: null },
  lockedAt: { type: String, default: null },
  empresaId: { type: String, default: null },
  serialNumber: { type: String, default: null },
  connectedAt: { type: String, default: null },
  lastPoll: { type: String, default: null },
  usageTimeLimit: { type: Number, default: null },
  usageStartTime: { type: String, default: null }
}, { versionKey: false });

const EmpresaSchema = new mongoose.Schema({
  id: { type: String, required: true },
  nome: { type: String, required: true },
  cnpj: { type: String, default: '' },
  endereco: { type: String, default: '' },
  telefone: { type: String, default: '' },
  email: { type: String, default: '' },
  observacao: { type: String, default: '' },
  createdAt: { type: String, default: null },
  updatedAt: { type: String, default: null }
}, { versionKey: false });

const ClienteSchema = new mongoose.Schema({
  id: { type: Number, required: true },
  nome: { type: String, default: '' },
  cpfCnpj: { type: String, default: '' },
  telefone: { type: String, default: '' },
  email: { type: String, default: '' },
  endereco: { type: String, default: '' },
  cidade: { type: String, default: '' },
  cep: { type: String, default: '' },
  observacao: { type: String, default: '' },
  ativo: { type: Boolean, default: true },
  dataCriacao: { type: Number, default: null }
}, { versionKey: false });

const CaixaSessaoSchema = new mongoose.Schema({
  deviceId: { type: String, default: null },
  operadorAbertura: { type: String, default: '' },
  operadorFechamento: { type: String, default: '' },
  aberturaEm: { type: String, default: null },
  fechamentoEm: { type: String, default: null },
  totalAbertura: { type: Number, default: 0 },
  totalSuprimento: { type: Number, default: 0 },
  totalSangria: { type: Number, default: 0 },
  totalFechamento: { type: Number, default: 0 },
  totalVendas: { type: Number, default: 0 },
  qtdVendas: { type: Number, default: 0 },
  vendasDinheiro: { type: Number, default: 0 },
  vendasPix: { type: Number, default: 0 },
  vendasCredito: { type: Number, default: 0 },
  vendasDebito: { type: Number, default: 0 }
}, { versionKey: false });

const AuditoriaSchema = new mongoose.Schema({
  tipo: { type: String, default: '' },
  deviceId: { type: String, default: '' },
  descricao: { type: String, default: '' },
  username: { type: String, default: '' },
  timestamp: { type: String, default: '' }
}, { versionKey: false });

const ConfigSchema = new mongoose.Schema({
  key: { type: String, required: true },
  value: mongoose.Schema.Types.Mixed
}, { versionKey: false });

// ==================== MODELS ====================

const Produto = mongoose.models.Produto || mongoose.model('Produto', ProdutoSchema);
const Categoria = mongoose.models.Categoria || mongoose.model('Categoria', CategoriaSchema);
const Venda = mongoose.models.Venda || mongoose.model('Venda', VendaSchema);
const Operacao = mongoose.models.Operacao || mongoose.model('Operacao', OperacaoSchema);
const Usuario = mongoose.models.Usuario || mongoose.model('Usuario', UsuarioSchema);
const Dispositivo = mongoose.models.Dispositivo || mongoose.model('Dispositivo', DispositivoSchema);
const Empresa = mongoose.models.Empresa || mongoose.model('Empresa', EmpresaSchema);
const Cliente = mongoose.models.Cliente || mongoose.model('Cliente', ClienteSchema);
const CaixaSessao = mongoose.models.CaixaSessao || mongoose.model('CaixaSessao', CaixaSessaoSchema);
const AuditoriaDoc = mongoose.models.AuditoriaDoc || mongoose.model('AuditoriaDoc', AuditoriaSchema);
const ConfigDoc = mongoose.models.ConfigDoc || mongoose.model('ConfigDoc', ConfigSchema);

// ==================== DB PROXY ====================
// Mantém a mesma interface do data.json (arrays em memória) mas sincroniza com MongoDB

const db = {
  produtos: [],
  categorias: [],
  vendas: [],
  operacoes: [],
  usuarios: [],
  dispositivos: [],
  empresas: [],
  clientes: [],
  caixaSessoes: [],
  auditoria: [],
  config: {}
};

let isConnected = false;

async function connectMongo() {
  const MONGO_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/caixacombo';
  try {
    await mongoose.connect(MONGO_URI);
    isConnected = true;
    console.log('📦 MongoDB conectado');
    await loadFromMongo();
  } catch (err) {
    console.error('❌ Erro ao conectar MongoDB:', err.message);
    // Fallback: tentar carregar do data.json se existir
    tryMigrateFromJson();
  }
}

async function loadFromMongo() {
  try {
    db.produtos = (await Produto.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.categorias = (await Categoria.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.vendas = (await Venda.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.operacoes = (await Operacao.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.usuarios = (await Usuario.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.dispositivos = (await Dispositivo.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.empresas = (await Empresa.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.clientes = (await Cliente.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.caixaSessoes = (await CaixaSessao.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    db.auditoria = (await AuditoriaDoc.find().lean()).map(p => { const o = p.toObject ? p.toObject() : p; delete o._id; delete o.__v; return o; });
    const configDoc = await ConfigDoc.findOne({ key: 'app' }).lean();
    db.config = configDoc ? (typeof configDoc.value === 'string' ? JSON.parse(configDoc.value) : configDoc.value) : {};

    console.log(`📊 MongoDB carregado: ${db.produtos.length} produtos, ${db.categorias.length} categorias, ${db.vendas.length} vendas, ${db.operacoes.length} operações, ${db.usuarios.length} usuários, ${db.dispositivos.length} dispositivos`);
  } catch (err) {
    console.error('❌ Erro ao carregar dados do MongoDB:', err.message);
  }
}

// Migração do data.json se MongoDB estiver vazio
async function tryMigrateFromJson() {
  const fs = require('fs');
  const path = require('path');
  const DATA_FILE = path.join(__dirname, 'data.json');
  if (!fs.existsSync(DATA_FILE)) return;

  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf8');
    if (!raw.trim()) return;
    const data = JSON.parse(raw);

    // Só migrar se MongoDB estiver vazio
    const count = await Produto.countDocuments();
    if (count > 0) return;

    console.log('📦 Migrando dados do data.json para MongoDB...');
    await migrateData(data);
    console.log('✅ Migração do data.json para MongoDB concluída!');
    await loadFromMongo();
  } catch (err) {
    console.error('❌ Erro na migração:', err.message);
  }
}

async function migrateData(data) {
  if (data.produtos && data.produtos.length) await Produto.insertMany(data.produtos);
  if (data.categorias && data.categorias.length) await Categoria.insertMany(data.categorias);
  if (data.vendas && data.vendas.length) await Venda.insertMany(data.vendas);
  if (data.operacoes && data.operacoes.length) await Operacao.insertMany(data.operacoes);
  if (data.usuarios && data.usuarios.length) await Usuario.insertMany(data.usuarios);
  if (data.dispositivos && data.dispositivos.length) await Dispositivo.insertMany(data.dispositivos);
  if (data.empresas && data.empresas.length) await Empresa.insertMany(data.empresas);
  if (data.clientes && data.clientes.length) await Cliente.insertMany(data.clientes);
  if (data.caixaSessoes && data.caixaSessoes.length) await CaixaSessao.insertMany(data.caixaSessoes);
  if (data.auditoria && data.auditoria.length) await AuditoriaDoc.insertMany(data.auditoria);
  if (data.config && Object.keys(data.config).length) {
    await ConfigDoc.findOneAndUpdate({ key: 'app' }, { key: 'app', value: data.config }, { upsert: true });
  }
}

// saveData: sincroniza as arrays em memória para o MongoDB
async function saveData() {
  if (!isConnected) return;
  try {
    // Usar bulkWrite com replace + upsert para cada collection
    const bulkSync = async (Model, items, keyField) => {
      if (!items || items.length === 0) {
        await Model.deleteMany({});
        return;
      }
      const ops = items.map(item => {
        const filter = {};
        filter[keyField] = item[keyField];
        const doc = { ...item };
        return {
          replaceOne: {
            filter,
            replacement: doc,
            upsert: true
          }
        };
      });
      // Remover documentos que não estão mais na lista
      const ids = items.map(i => i[keyField]);
      await Model.deleteMany({ [keyField]: { $nin: ids } });
      if (ops.length > 0) await Model.bulkWrite(ops, { ordered: false });
    };

    await bulkSync(Produto, db.produtos, 'id');
    await bulkSync(Categoria, db.categorias, 'id');
    await bulkSync(Venda, db.vendas, 'id');
    await bulkSync(Usuario, db.usuarios, 'id');
    await bulkSync(Dispositivo, db.dispositivos, 'deviceId');
    await bulkSync(Empresa, db.empresas, 'id');
    await bulkSync(Cliente, db.clientes, 'id');

    // Operações e auditoria: deleteAll + insertAll (não têm ID estável)
    await Operacao.deleteMany({});
    if (db.operacoes.length) await Operacao.insertMany(db.operacoes);

    await AuditoriaDoc.deleteMany({});
    if (db.auditoria.length) await AuditoriaDoc.insertMany(db.auditoria);

    await CaixaSessao.deleteMany({});
    if (db.caixaSessoes.length) await CaixaSessao.insertMany(db.caixaSessoes);

    // Config
    if (db.config && Object.keys(db.config).length) {
      await ConfigDoc.findOneAndUpdate({ key: 'app' }, { key: 'app', value: db.config }, { upsert: true });
    }
  } catch (err) {
    console.error('❌ Erro ao salvar no MongoDB:', err.message);
  }
}

async function saveAuditoria() {
  // Já é salvo no saveData()
}

module.exports = { db, saveData, saveAuditoria, connectMongo, loadFromMongo, isConnected: () => isConnected };
