const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const PDFDocument = require('pdfkit');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();

// Constant-time comparison para prevenir timing attacks em PINs
function safeCompare(a, b) {
  const bufA = Buffer.from(String(a || ''));
  const bufB = Buffer.from(String(b || ''));
  if (bufA.length !== bufB.length) {
    crypto.timingSafeEqual(bufA, Buffer.alloc(bufA.length));
    return false;
  }
  return crypto.timingSafeEqual(bufA, bufB);
}

// Funcoes de validacao de entrada
function validateEmail(email) {
  if (!email || typeof email !== 'string') return false;
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email.trim().toLowerCase());
}

function validateString(value, minLength = 1, maxLength = 1000) {
  if (!value || typeof value !== 'string') return false;
  const trimmed = value.trim();
  return trimmed.length >= minLength && trimmed.length <= maxLength;
}

function validateNumber(value, min = 0, max = Number.MAX_SAFE_INTEGER) {
  const num = Number(value);
  return !isNaN(num) && num >= min && num <= max;
}

function sanitizeInput(value) {
  if (typeof value !== 'string') return value;
  return value.replace(/[<>]/g, '').trim();
}

// Middleware de validacao para login
function validateLoginInput(req, res, next) {
  const { username, password, email, pin } = req.body;
  
  // Se usa email, validar formato
  if (email && !validateEmail(email)) {
    return res.status(400).json({ error: 'Formato de email invalido' });
  }
  
  // Se usa username, validar formato
  if (username && !validateString(username, 3, 50)) {
    return res.status(400).json({ error: 'Username deve ter entre 3 e 50 caracteres' });
  }
  
  // Validar senha/PIN
  const credential = password || pin;
  if (!credential || credential.length < 4 || credential.length > 128) {
    return res.status(400).json({ error: 'Credenciais invalidas' });
  }
  
  // Sanitizar inputs
  if (username) req.body.username = sanitizeInput(username);
  if (email) req.body.email = sanitizeInput(email);
  
  next();
}

// Security headers (Stone compliance)
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "blob:"],
      connectSrc: ["'self'"],
      fontSrc: ["'self'"],
      objectSrc: ["'none'"],
      mediaSrc: ["'self'"],
      frameSrc: ["'none'"]
    }
  },
  crossOriginEmbedderPolicy: false,
  crossOriginResourcePolicy: { policy: "same-site" },
  referrerPolicy: { policy: "strict-origin-when-cross-origin" },
  hsts: { maxAge: 31536000, includeSubDomains: true, preload: true },
  noSniff: true,
  xssFilter: true
}));

// Rate limiting
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 200,
  message: { error: 'Muitas requisições. Tente novamente mais tarde.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  message: { error: 'Muitas tentativas de login. Conta bloqueada temporariamente.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const unlockLimiter = rateLimit({
  windowMs: 5 * 60 * 1000,
  max: 10,
  message: { error: 'Muitas tentativas de desbloqueio.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const deviceLimiter = rateLimit({
  windowMs: 1 * 60 * 1000,
  max: 60,
  message: { error: 'Muitas requisições do dispositivo.' },
  standardHeaders: true,
  legacyHeaders: false,
});

app.use('/api/', apiLimiter);
app.use('/api/auth/', authLimiter);

// CORS (seguro - origens específicas)
const allowedOrigins = (process.env.CORS_ORIGINS || '').split(',').filter(Boolean);
app.use(cors({ 
  origin: allowedOrigins.length > 0 ? allowedOrigins : ['http://localhost:5173'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Authorization', 'Content-Type']
}));
app.use(express.json({ limit: '10mb' }));

// Criar pasta uploads se não existir
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Servir arquivos estáticos da pasta uploads
app.use('/uploads', express.static(uploadsDir));

// Fallback para imagens de upload que foram perdidas (ex: filesystem efêmero do Render)
app.use('/uploads', (req, res) => {
  const ext = req.path.split('.').pop()?.toLowerCase();
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext)) {
    res.type('svg').status(200).send(`<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
      <rect width="200" height="200" fill="#f0f0f0" rx="8"/>
      <text x="100" y="85" text-anchor="middle" font-size="48" fill="#ccc">📷</text>
      <text x="100" y="125" text-anchor="middle" font-size="14" fill="#aaa">Imagem indisponível</text>
    </svg>`);
  } else {
    res.status(404).json({ error: 'Arquivo não encontrado' });
  }
});

// Configurar multer para upload de imagens
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, 'uploads/');
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'produto-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  },
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Apenas arquivos de imagem são permitidos'));
    }
  }
});

// ==================== PERSISTÊNCIA DE DADOS (Supabase PostgreSQL) ====================
const { connectDatabase, query, queryOne, queryMany } = require('./database');
const { db, loadAll, debouncedSave, flushToDb, saveData, saveAuditoria, setDbConnected } = require('./db');

// Gerador de IDs único
let _idCounter = 0;
let _lastIdTime = 0;
function generateId() {
  const now = Date.now();
  if (now === _lastIdTime) {
    _idCounter++;
  } else {
    _idCounter = 0;
    _lastIdTime = now;
  }
  return now * 1000 + _idCounter;
}

// Gerar slug único a partir do nome da empresa
function generateSlug(nome, existingSlugs = []) {
  // Remove acentos, converte para minúsculo, substitui espaços por hífen
  let base = nome
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '') // Remove acentos
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '') // Remove caracteres especiais
    .trim()
    .replace(/\s+/g, '-'); // Substitui espaços por hífen

  let slug = base;
  let counter = 1;

  // Garante que o slug é único
  while (existingSlugs.includes(slug)) {
    slug = `${base}-${counter}`;
    counter++;
  }

  return slug;
}

const serverStartTime = new Date(); // Para detectar restart e forçar sync inicial
const connectedDevices = new Map();
const connectedDashboards = new Map(); // Guardar usuário do dashboard
const pendingCommands = new Map(); // Fila de comandos pendentes por deviceId (para polling REST)
const deviceLastSync = new Map(); // Rastrear último sync de cada dispositivo

// Emitir evento apenas para dashboards da empresa específica (ou todos se empresaId=null)
function emitToEmpresa(event, data, empresaId = null) {
  // Enviar para dashboards da empresa (admin vê tudo)
  connectedDashboards.forEach((info, socketId) => {
    const socket = io.sockets.sockets.get(socketId);
    if (!socket) return;
    // Admin vê tudo, empresa só vê seus dados
    if (info.role === 'admin' || !empresaId || info.empresaId === empresaId) {
      socket.emit(event, data);
    }
  });
  
  // Enviar para terminais da empresa (usando sala específica)
  if (empresaId) {
    io.to(`empresa_${empresaId}`).emit(event, data);
  }
}

// Emitir evento de dispositivo filtrado pelo empresaId do dispositivo
function emitDeviceEvent(event, data) {
  const deviceEmpresaId = connectedDevices.get(data.deviceId)?.empresaId || null;
  emitToEmpresa(event, data, deviceEmpresaId);
}

// Inicialização assíncrona: conectar PostgreSQL e carregar dados
async function initializeApp() {
  const connected = await connectDatabase();
  setDbConnected(connected);
  
  if (!connected) {
    // Modo offline: carregar dados do data.json
    console.log('📦 Rodando em modo offline (data.json)...');
    const fs = require('fs');
    const path = require('path');
    const dataFile = path.join(__dirname, 'data.json');
    if (fs.existsSync(dataFile)) {
      const data = JSON.parse(fs.readFileSync(dataFile, 'utf8'));
      db.empresas = data.empresas || [];
      db.usuarios = data.usuarios || [];
      db.funcionarios = data.funcionarios || [];
      db.categorias = data.categorias || [];
      db.produtos = data.produtos || [];
      db.vendas = data.vendas || [];
      db.operacoes = data.operacoes || [];
      db.dispositivos = data.dispositivos || [];
      db.auditoria = data.auditoria || [];
      db.clientes = data.clientes || [];
      db.config = data.config || {};
      db.impressaoTemplate = data.impressaoTemplate || null;
      console.log(`📊 Offline: ${db.produtos.length} produtos, ${db.categorias.length} categorias, ${db.usuarios.length} usuários`);
    } else {
      console.log('📊 Sem dados anteriores — iniciando vazio');
    }
  } else {
    // PostgreSQL: carregar dados do banco
    await loadAll();
    const { seedAdmin } = require('./database');
    await seedAdmin();
    await loadAll();
  }

  // Carregar dispositivos do banco para o mapa ao iniciar
  if (db.dispositivos && db.dispositivos.length > 0) {
    db.dispositivos.forEach(d => {
      connectedDevices.set(d.deviceId, {
        socketId: null,
        deviceName: d.deviceName || 'Dispositivo',
        deviceType: d.deviceType || 'Android',
        serialNumber: d.serialNumber || null,
        status: d.status || 'online',
        lockPassword: d.lockPassword || null,
        lastPoll: d.lastPoll || null,
        lastLogin: d.lastLogin || null,
        lastLoginUser: d.lastLoginUser || null,
        empresaId: d.empresaId || null,
        usageTimeLimit: d.usageTimeLimit || null,
        usageStartTime: d.usageStartTime || null
      });
    });
    console.log(`📱 ${connectedDevices.size} dispositivos carregados do banco`);
  }
}

// Função para adicionar logs de auditoria
function addAuditoria(tipo, deviceId, detalhes, usuario = null) {
  const deviceEmpresaId = deviceId ? (connectedDevices.get(deviceId)?.empresaId || null) : null;
  
  const log = {
    id: generateId(),
    timestamp: new Date().toISOString(),
    tipo,
    deviceId,
    deviceName: connectedDevices.get(deviceId)?.deviceName || deviceId,
    detalhes,
    usuario: usuario || (connectedDevices.get(deviceId)?.deviceName || 'Sistema'),
    ip: null,
    empresaId: deviceEmpresaId
  };
  
  db.auditoria.unshift(log);
  
  // Manter apenas os últimos 2000 logs
  if (db.auditoria.length > 2000) {
    db.auditoria = db.auditoria.slice(0, 2000);
  }
  
  saveAuditoria();
  
  emitToEmpresa('auditoria_update', log, deviceEmpresaId);
  console.log(`[AUDITORIA] ${tipo.toUpperCase()}: ${deviceId} - ${detalhes}`);
}

// Salvar dados periodicamente a cada 30 segundos
setInterval(async () => {
  await flushToDb();
  console.log('💾 Dados sincronizados com PostgreSQL');
}, 30000);

// ==================== CONTROLE VIA ADB ====================
const { exec, execFile } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);
const execFilePromise = util.promisify(execFile);

/**
 * Envia comando ADB diretamente ao dispositivo para reiniciar ou desligar
 * @param {string} action - 'reboot' ou 'shutdown'
 * @param {string} deviceId - Serial number do dispositivo (mesmo que o ADB usa)
 */
function sanitizeDeviceId(deviceId) {
  // Previne command injection: só aceita caracteres alfanuméricos, hífens e underscores
  if (!deviceId || typeof deviceId !== 'string') return null;
  const sanitized = deviceId.replace(/[^a-zA-Z0-9\-_.:]/g, '');
  return sanitized.length > 0 && sanitized.length <= 128 ? sanitized : null;
}

async function sendAdbCommand(action, deviceId) {
  const safeDeviceId = sanitizeDeviceId(deviceId);
  if (!safeDeviceId) {
    console.error(`❌ [ADB] deviceId inválido: ${deviceId}`);
    return { success: false, error: 'deviceId inválido' };
  }

  console.log(`🔌 [ADB] Enviando comando ${action} para ${safeDeviceId}`);

  const ADB_TIMEOUT = 5000; // 5 segundos de timeout para comandos ADB

  // Helper para executar com timeout
  const execWithTimeout = (cmd) => {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('Timeout: comando ADB excedeu 5s')), ADB_TIMEOUT);
      execPromise(cmd).then(
        (result) => { clearTimeout(timer); resolve(result); },
        (err) => { clearTimeout(timer); reject(err); }
      );
    });
  };

  // Verificar se dispositivo está conectado
  try {
    const { stdout } = await execWithTimeout('adb devices');
    const isConnected = stdout.includes(safeDeviceId);

    if (!isConnected) {
      console.log(`❌ [ADB] Dispositivo ${safeDeviceId} não está conectado via ADB`);
      return { success: false, error: 'Dispositivo não está conectado via USB/WiFi ADB' };
    }

    // Enviar comando — usando execFile para evitar shell injection
    const adbArgs = ['-s', safeDeviceId, 'shell'];
    if (action === 'shutdown') {
      adbArgs.push('reboot', '-p');
    } else {
      adbArgs.push('reboot');
    }

    console.log(`🔌 [ADB] Executando: adb ${adbArgs.join(' ')}`);

    await execFilePromise('adb', adbArgs);
    console.log(`✅ [ADB] Comando ${action} enviado com sucesso para ${safeDeviceId}`);

    return { success: true, method: 'adb' };
  } catch (error) {
    console.error(`❌ [ADB] Erro:`, error.message);
    return { success: false, error: error.message };
  }
}

// Salvar dados ao encerrar servidor
process.on('SIGINT', async () => {
  console.log('\n🔄 Salvando dados antes de encerrar...');
  await flushToDb();
  process.exit(0);
});

// ==================== CRIAR SERVER E IO ====================
const server = http.createServer(app);
const io = new Server(server, {
  cors: { 
    origin: allowedOrigins.length > 0 ? allowedOrigins : ['http://localhost:5173'],
    credentials: true,
    methods: ['GET', 'POST']
  },
  transports: ["polling"],
  pingTimeout: 60000,
  pingInterval: 25000,
  allowEIO3: false
});

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('❌ JWT_SECRET não definido! Defina a variável de ambiente JWT_SECRET.');
  process.exit(1);
}

// Função helper para obter companyId (empresaId) de forma segura
function getCompanyId(req) {
  // Se é empresa (role='empresa'), usar empresaId do token
  if (req.user?.role === 'empresa') {
    return req.user.empresaId;
  }
  // Se é admin do sistema (role='admin'), retornar null (deve especificar empresaId no body)
  if (req.user?.role === 'admin') {
    return null;
  }
  // Funcionário também usa empresaId do token
  if (req.user?.role === 'funcionario') {
    return req.user.empresaId;
  }
  return null;
}

// Função para filtrar array por empresaId
function filterByEmpresaId(array, empresaId, field = 'empresaId') {
  if (!empresaId) return [];
  return array.filter(item => item[field] && String(item[field]) === String(empresaId));
}

function authenticateToken(req, res, next) {
  const token = req.headers['authorization']?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Não autorizado' });
  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Token inválido' });
    req.user = user;
    // Adicionar helper para identificar tipo de usuário
    req.isSuperAdmin = user?.role === 'admin' && !user?.empresaId;
    req.isCompanyAdmin = user?.role === 'empresa';
    req.isFuncionario = user?.role === 'funcionario';
    // Obter companyId (empresaId) do token ou body
    req.companyId = getCompanyId(req);
    next();
  });
}

// ==================== ROTAS API ====================
// Middleware para logging de tentativas de login
const loginAttempts = new Map();
const LOGIN_ATTEMPT_WINDOW = 15 * 60 * 1000; // 15 minutos
const MAX_LOGIN_ATTEMPTS = 10;

function checkLoginAttempts(identifier) {
  const now = Date.now();
  const attempts = loginAttempts.get(identifier) || [];
  
  // Limpar tentativas antigas
  const recentAttempts = attempts.filter(t => now - t < LOGIN_ATTEMPT_WINDOW);
  
  if (recentAttempts.length >= MAX_LOGIN_ATTEMPTS) {
    return false; // Bloqueado
  }
  
  return true;
}

function recordLoginAttempt(identifier, success) {
  const now = Date.now();
  const attempts = loginAttempts.get(identifier) || [];
  
  if (success) {
    loginAttempts.delete(identifier);
  } else {
    attempts.push(now);
    loginAttempts.set(identifier, attempts);
  }
}

app.post('/api/auth/login', validateLoginInput, async (req, res) => {
  const { username, password, email, pin } = req.body;

  const providedEmail = (email || '').toString().trim().toLowerCase();
  const providedPassword = (password || pin || '').toString().trim();
  
  // Verificar rate limit de tentativas de login
  const identifier = providedEmail || username || 'unknown';
  if (!checkLoginAttempts(identifier)) {
    console.warn(`[SECURITY] Login bloqueado por muitas tentativas: ${identifier}`);
    return res.status(429).json({ error: 'Muitas tentativas de login. Tente novamente mais tarde.' });
  }

  if (providedEmail && providedPassword) {
    console.log(`[LOGIN] Tentativa por email`);
    
    // DeviceId enviado pelo terminal Android
    const { deviceId } = req.body;

    // Buscar funcionário pelo email
    const funcionario = (db.funcionarios || []).find(f => ((f.email || '').toString().trim().toLowerCase()) === providedEmail && f.ativo);
    if (!funcionario) {
      // Tentar buscar por código (caso o frontend tenha enviado código no campo de email)
      const funcionarioByCodigo = (db.funcionarios || []).find(f => ((f.codigo || '').toString().trim()) === providedEmail && f.ativo);
      if (!funcionarioByCodigo) {
        // Tentar admin/usuario por email
        const admin = (db.usuarios || []).find(u => ((u.email || '').toString().trim().toLowerCase()) === providedEmail && u.ativo);
        if (!admin) return res.status(401).json({ error: 'Credenciais inválidas' });
        if (!admin.password) return res.status(401).json({ error: 'Credenciais inválidas' });
        const okAdmin = bcrypt.compareSync(providedPassword, admin.password);
        if (!okAdmin) {
          recordLoginAttempt(identifier, false);
          console.warn(`[SECURITY] Login falhou para admin: ${providedEmail}`);
          return res.status(401).json({ error: 'Credenciais inválidas' });
        }
        recordLoginAttempt(identifier, true);
        const token = jwt.sign({ id: admin.id, email: admin.email, role: admin.role || 'admin' }, JWT_SECRET, { expiresIn: '24h' });
        return res.json({ token, user: { id: admin.id, email: admin.email, username: admin.username || admin.nome, role: admin.role || 'admin' } });
      }

      // Found by codigo
      if (!funcionarioByCodigo.ativo) return res.status(403).json({ error: 'Funcionário desativado. Contate o administrador.' });
      const empresa = (db.empresas || []).find(e => e.id === funcionarioByCodigo.empresaId);
      if (!empresa || !empresa.ativo) return res.status(403).json({ error: 'Empresa desativada ou não encontrada.' });

      // check password/pin
    let ok = false;
    if (funcionarioByCodigo.password) ok = bcrypt.compareSync(providedPassword, funcionarioByCodigo.password);
    else if (funcionarioByCodigo.pin) ok = safeCompare(providedPassword, funcionarioByCodigo.pin);
    if (!ok) return res.status(401).json({ error: 'Credenciais inválidas' });
      
      // VERIFICAR TERMINAL SE deviceId FOR ENVIADO
      const terminalResult = verifyTerminalAndLogin(deviceId, funcionarioByCodigo.empresaId, funcionarioByCodigo?.nome || 'desconhecido', req);
      if (terminalResult) return res.status(terminalResult.status).json({ error: terminalResult.error });

      const token = jwt.sign({ id: funcionarioByCodigo.id, username: funcionarioByCodigo.nome, role: 'funcionario', empresaId: funcionarioByCodigo.empresaId, funcionarioId: funcionarioByCodigo.id, permissoes: funcionarioByCodigo.permissoes, paginasPermitidas: ['dashboard','vendas','caixa'] }, JWT_SECRET, { expiresIn: '24h' });
      return res.json({ token, user: { id: funcionarioByCodigo.id, username: funcionarioByCodigo.nome, role: 'funcionario', empresaNome: empresa.nome, empresaId: funcionarioByCodigo.empresaId, funcionarioId: funcionarioByCodigo.id, permissoes: funcionarioByCodigo.permissoes, paginasPermitidas: ['dashboard','vendas','caixa'], branding: { primaryColor: empresa.primaryColor || '#3b82f6', secondaryColor: empresa.secondaryColor || '#06b6d4', accentColor: empresa.accentColor || '#10b981', logoUrl: empresa.logoUrl || '', companyName: empresa.nome } } });
    }

    // Found by email
    if (!funcionario.ativo) return res.status(403).json({ error: 'Funcionário desativado. Contate o administrador.' });
    const empresa = (db.empresas || []).find(e => e.id === funcionario.empresaId);
    if (!empresa || !empresa.ativo) return res.status(403).json({ error: 'Empresa desativada ou não encontrada.' });

    let ok = false;
    if (funcionario.password) ok = bcrypt.compareSync(providedPassword, funcionario.password);
    else if (funcionario.pin) ok = safeCompare(providedPassword, funcionario.pin);
    if (!ok) return res.status(401).json({ error: 'Credenciais inválidas' });
    
    // VERIFICAR TERMINAL SE deviceId FOR ENVIADO
    const terminalResult = verifyTerminalAndLogin(deviceId, funcionario.empresaId, funcionario?.nome || 'desconhecido', req);
    if (terminalResult) return res.status(terminalResult.status).json({ error: terminalResult.error });

    const token = jwt.sign({ id: funcionario.id, username: funcionario.nome, role: 'funcionario', empresaId: funcionario.empresaId, funcionarioId: funcionario.id, permissoes: funcionario.permissoes, paginasPermitidas: ['dashboard','vendas','caixa'] }, JWT_SECRET, { expiresIn: '24h' });
    return res.json({ token, user: { id: funcionario.id, username: funcionario.nome, role: 'funcionario', empresaNome: empresa.nome, empresaId: funcionario.empresaId, funcionarioId: funcionario.id, permissoes: funcionario.permissoes, paginasPermitidas: ['dashboard','vendas','caixa'], branding: { primaryColor: empresa.primaryColor || '#3b82f6', secondaryColor: empresa.secondaryColor || '#06b6d4', accentColor: empresa.accentColor || '#10b981', logoUrl: empresa.logoUrl || '', companyName: empresa.nome } } });
  }

  // Login de admin/empresa (username + password)
  console.log(`[LOGIN] Tentativa: username="${username}"`);
  
  // Verificar se é usuário do sistema (checar banco direto para senha atualizada)
  let user = null;
  try {
    user = await queryOne('SELECT * FROM usuarios WHERE username = $1 AND ativo = true', [username]);
  } catch (e) {
    user = db.usuarios.find(u => u.username === username && u.ativo);
  }
  console.log(`[LOGIN] Usuário encontrado: ${user ? `id=${user.id}, role=${user.role}` : 'NENHUM'}`);
  
  // Se não for usuário do sistema, verificar se é empresa
  if (!user) {
    let empresa = null;
    try {
      empresa = await queryOne('SELECT * FROM empresas WHERE login = $1 AND ativa = true', [username]);
    } catch (e) {
      empresa = db.empresas.find(e => e.login === username && e.ativa);
    }
    console.log(`[LOGIN] Empresa encontrada: ${empresa ? `id=${empresa.id}, login=${empresa.login}, temSenha=${!!empresa.senha}` : 'NENHUMA'}`);
    if (empresa) {
      if (!empresa.ativa) {
        return res.status(403).json({ error: 'Empresa desativada. Contate o administrador.' });
      }
      if (!empresa.senha || !bcrypt.compareSync(password, empresa.senha)) {
        console.log(`[LOGIN] Senha da empresa não confere ou senha ausente`);
        recordLoginAttempt(identifier, false);
        return res.status(401).json({ error: 'Credenciais inválidas' });
      }
      
      recordLoginAttempt(identifier, true);
      const token = jwt.sign(
        { id: empresa.id, username: empresa.login, role: 'empresa', empresaId: empresa.id, permissoes: empresa.permissoes, paginasPermitidas: empresa.paginas_permitidas },
        JWT_SECRET,
        { expiresIn: '24h' }
      );
      return res.json({
        token,
        user: {
          id: empresa.id,
          username: empresa.login,
          role: 'empresa',
          empresaNome: empresa.nome,
          empresaId: empresa.id,
          slug: empresa.slug,
          permissoes: empresa.permissoes,
          paginasPermitidas: empresa.paginas_permitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa', 'terminais', 'impressao', 'config'],
          branding: {
            primaryColor: empresa.primary_color || '#3b82f6',
            secondaryColor: empresa.secondary_color || '#06b6d4',
            accentColor: empresa.accent_color || '#10b981',
            logoUrl: empresa.logo_url || '',
            companyName: empresa.nome
          }
        }
      });
    }
  }
  
  if (!user) {
    recordLoginAttempt(identifier, false);
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }
  
  if (!bcrypt.compareSync(password, user.password)) {
    console.log(`[LOGIN] Senha do usuário não confere para "${username}"`);
    recordLoginAttempt(identifier, false);
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }
  
  recordLoginAttempt(identifier, true);
  const token = jwt.sign({ id: user.id, username: user.username, role: user.role, empresaId: user.empresa_id }, JWT_SECRET, { expiresIn: '24h' });
  res.json({ token, user: { id: user.id, username: user.username, role: user.role, empresaId: user.empresa_id } });
});

app.get('/api/auth/verify', authenticateToken, async (req, res) => {
  const userData = { ...req.user };
  // Se for empresa, buscar branding atualizado
  if (req.user.role === 'empresa' && req.user.empresaId) {
    const empresa = db.empresas.find(e => e.id === req.user.empresaId);
    if (empresa) {
      userData.paginasPermitidas = empresa.paginasPermitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa', 'terminais', 'impressao', 'config'];
      userData.branding = {
        primaryColor: empresa.primaryColor || '#3b82f6',
        secondaryColor: empresa.secondaryColor || '#06b6d4',
        accentColor: empresa.accentColor || '#10b981',
        logoUrl: empresa.logoUrl || '',
        companyName: empresa.nome
      };
      userData.empresaNome = empresa.nome;
      if (!empresa.ativo) return res.status(403).json({ error: 'Empresa desativada' });
    }
  }
  res.json({ valid: true, user: userData });
});

// Verificar senha para ações sensíveis
app.post('/api/auth/verify-password', authenticateToken, async (req, res) => {
  const { password } = req.body;
  const user = db.usuarios.find(u => u.id === req.user.id || u.username === req.user.username);
  if (!user) return res.status(401).json({ valid: false, error: 'Usuário não encontrado' });
  if (!bcrypt.compareSync(password, user.password)) return res.status(401).json({ valid: false, error: 'Senha incorreta' });
  res.json({ valid: true });
});

// Reimprimir venda
app.post('/api/vendas/:id/reimprimir', authenticateToken, async (req, res) => {
  const { id } = req.params;
  const venda = (db.vendas || []).find(v => v.id == id);
  if (!venda) return res.status(404).json({ error: 'Venda não encontrada' });
  // Empresa só pode reimprimir suas próprias vendas
  if (req.user.role === 'empresa' && venda.empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  
  const deviceId = venda.deviceId;
  const atk = venda.stoneAtk || venda.atk || null;
  
  // Enviar comando de reimpressão para o dispositivo com o ATK
  enqueueDeviceCommand(deviceId, 'reimprimir_venda', { vendaId: id, atk });
  const device = connectedDevices.get(deviceId);
  if (device?.socketId) {
    io.to(device.socketId).emit('reimprimir_venda', { vendaId: id, atk });
  }
  
  addAuditoria('reimpressao', deviceId, `Reimpressão da venda #${id}${atk ? ' (atk: ' + atk + ')' : ''}`, req.user.username);
  res.json({ success: true, message: 'Comando de reimpressão enviado' });
});

// Cancelar venda
app.post('/api/vendas/:id/cancelar', authenticateToken, async (req, res) => {
  const { id } = req.params;
  const { motivo } = req.body;
  const vendaIndex = (db.vendas || []).findIndex(v => v.id == id);
  if (vendaIndex === -1) return res.status(404).json({ error: 'Venda não encontrada' });
  
  const venda = db.vendas[vendaIndex];
  // Empresa só pode cancelar suas próprias vendas
  if (req.user.role === 'empresa' && venda.empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const deviceId = venda.deviceId;
  const atk = venda.stoneAtk || venda.atk || null;
  
  // Verificar se venda de cartão/PIX tem ATK (necessário para cancelamento via deeplink Stone)
  const formasStone = ['CARTAO_CREDITO', 'CARTAO_DEBITO', 'CREDITO', 'DEBITO', 'PIX'];
  if (!atk && formasStone.includes(venda.formaPagamento)) {
    return res.status(400).json({ error: 'Venda sem ATK da Stone - cancelamento via deeplink não disponível. Use cancelamento manual.' });
  }
  
  // Marcar venda como cancelada
  db.vendas[vendaIndex].cancelada = true;
  db.vendas[vendaIndex].canceladaEm = new Date().toISOString();
  db.vendas[vendaIndex].canceladaPor = req.user.username;
  db.vendas[vendaIndex].motivoCancelamento = motivo || '';
  debouncedSaveData();
  
  // Enviar comando de cancelamento para o dispositivo com ATK e amount
  const amount = Math.round((venda.total || 0) * 100); // valor em centavos
  enqueueDeviceCommand(deviceId, 'cancelar_venda', { vendaId: id, atk, amount });
  const device = connectedDevices.get(deviceId);
  if (device?.socketId) {
    io.to(device.socketId).emit('cancelar_venda', { vendaId: id, atk, amount });
  }
  
  // Notificar dashboards da empresa
  const vendaEmpresaId = venda.empresaId || null;
  emitToEmpresa('venda_cancelada', { vendaId: id, deviceId }, vendaEmpresaId);
  
  addAuditoria('cancelamento', deviceId, `Venda #${id} cancelada: ${motivo || 'sem motivo'}${atk ? ' (atk: ' + atk + ')' : ''}`, req.user.username);
  res.json({ success: true, message: 'Venda cancelada com sucesso' });
});

app.get('/api/dispositivos', authenticateToken, (req, res) => {
  const now = new Date()
  const seenIds = new Set()
  let list = Array.from(connectedDevices.entries())
    .filter(([id]) => !BLOCKED_DEVICE_IDS.includes(id))
    .map(([id, d]) => {
      seenIds.add(id)
      const isPollingRecent = d.lastPoll && (now - new Date(d.lastPoll)) < 120000
      const isOnline = d.socketId !== null || isPollingRecent
      const dbEntry = db.dispositivos?.find(dd => dd.deviceId === id) || {}
      return { deviceId: id, ...dbEntry, ...d, online: isOnline }
    })
  if (db.dispositivos && db.dispositivos.length > 0) {
    db.dispositivos.forEach(d => {
      if (!seenIds.has(d.deviceId) && !BLOCKED_DEVICE_IDS.includes(d.deviceId)) {
        list.push({ ...d, online: false })
        seenIds.add(d.deviceId)
      }
    })
  }
  if (req.user.role === 'empresa' && req.user.empresaId) {
    list = list.filter(d => d.empresaId === req.user.empresaId)
  }
  console.log(`[DISPOSITIVOS] role=${req.user.role}, empresaId=${req.user.empresaId}, total=${list.length}, pending=${list.filter(d => d.status === 'pending').length}`)
  res.json(list)
})

// ==================== CONFIGURAÇÕES WHITELABEL ====================
app.get('/api/config', authenticateToken, (req, res) => {
  if (!db.config) db.config = {};
  res.json(db.config);
});

app.post('/api/config', authenticateToken, async (req, res) => {
  if (req.user.role !== 'admin') return res.status(403).json({ error: 'Apenas admin' });
  db.config = { ...db.config, ...req.body, updatedAt: new Date().toISOString() };
  debouncedSaveData();
  io.emit('config_updated', db.config);
  addAuditoria('config', null, 'Configurações atualizadas', req.user.username);
  res.json({ success: true, config: db.config });
});

// ==================== TEMPLATE DE IMPRESSÃO ====================
const defaultImpressaoTemplate = {
  cabecalho: { nomeEmpresa: true, cnpj: true, endereco: true, telefone: true, email: true, cidade: true },
  logo: { enabled: false, width: 120, height: 60, spacingTop: 10, spacingBottom: 10 },
  itens: { nome: true, quantidade: true, valorUnitario: true, valorTotal: true, separador: true },
  adicionais: { subtotal: true, desconto: true, total: true, formaPagamento: true, valorRecebido: true, troco: true, numeroVenda: true, dataHora: true },
  rodape: { linha1: 'Agradecemos sua vinda', linha2: 'Volte sempre', linha3: '', linha4: '' },
  estilo: { alinhamento: 'centro', tamanhoFonte: 'medio', espacoEntreLinhas: 8 }
}

function getEmpresaTemplate(empresaId) {
  const empresa = (db.empresas || []).find(e => e.id === empresaId)
  if (empresa?.impressaoTemplate) return empresa.impressaoTemplate
  return db.impressaoTemplate || defaultImpressaoTemplate
}

function setEmpresaTemplate(empresaId, template) {
  console.log(`[PRINT] setEmpresaTemplate empresaId=${empresaId}, empresas count=${(db.empresas || []).length}`)
  let saved = false
  if (empresaId) {
    const empresa = (db.empresas || []).find(e => e.id === empresaId)
    console.log(`[PRINT] Empresa ${empresaId}: ${empresa ? 'encontrada=' + empresa.nome : 'NAO ENCONTRADA'}`)
    if (empresa) {
      empresa.impressaoTemplate = template
      saved = true
    }
  } else {
    db.impressaoTemplate = template
    saved = true
  }
  debouncedSaveData()
  return saved ? 'OK' : 'NOT_FOUND'
}

app.get('/api/impressao/template', authenticateToken, (req, res) => {
  const empresaId = req.user.role === 'empresa' ? req.user.empresaId : (req.user.empresaId || req.query.empresaId || null)
  console.log(`[PRINT] GET empresaId=${empresaId}, role=${req.user.role}, user keys=${Object.keys(req.user || {})}`)
  const template = getEmpresaTemplate(empresaId)
  console.log(`[PRINT] Template: keys=${Object.keys(template || {}).length}`)
  res.json(template)
})

app.post('/api/impressao/template', authenticateToken, (req, res) => {
  const empresaId = req.user.role === 'empresa' ? req.user.empresaId : (req.user.empresaId || req.body.empresaId || null)
  console.log(`[PRINT] POST empresaId=${empresaId}, role=${req.user.role}`)
  console.log(`[PRINT] req.user keys: ${Object.keys(req.user || {})}`)
  const result = setEmpresaTemplate(empresaId, req.body)
  console.log(`[PRINT] Result: ${result}`)
  addAuditoria('impressao', null, 'Template de impressão atualizado', req.user.username)
  if (result === 'NOT_FOUND') {
    return res.status(404).json({ error: 'Empresa não encontrada', empresaId })
  }
  res.json({ success: true })
})

app.get('/api/auditoria', authenticateToken, (req, res) => {
  const { limit = 50, tipo, deviceId } = req.query;
  let logs = db.auditoria;

  // Filtrar por empresa se role=empresa - SÓ dados da própria empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    logs = logs.filter(log => log.empresaId === req.user.empresaId);
  }

  // Filtrar por tipo se especificado
  if (tipo) {
    logs = logs.filter(log => log.tipo === tipo);
  }

  // Filtrar por dispositivo se especificado
  if (deviceId) {
    logs = logs.filter(log => log.deviceId === deviceId);
  }

  // Limitar número de resultados
  logs = logs.slice(0, parseInt(limit));

  res.json(logs);
});

// ==================== ROTAS DE CATEGORIAS ====================
app.get('/api/categorias', authenticateToken, (req, res) => {
  // Se for empresa, filtrar APENAS suas categorias
  if (req.user.role === 'empresa' && req.user.empresaId) {
    return res.json(db.categorias.filter(c => c.empresaId === req.user.empresaId));
  }
  res.json(db.categorias);
});

app.post('/api/categorias', authenticateToken, async (req, res) => {
  // Validacao de entrada
  if (!validateString(req.body.nome, 1, 100)) {
    return res.status(400).json({ error: 'Nome da categoria obrigatorio (1-100 caracteres)' });
  }
  
  // Sanitizar inputs
  req.body.nome = sanitizeInput(req.body.nome);
  if (req.body.descricao) req.body.descricao = sanitizeInput(req.body.descricao);
  
  let empresaId = null;
  if (req.user.role === 'empresa') {
    empresaId = req.user.empresaId;
  } else if (req.user.role === 'admin') {
    empresaId = req.body.empresaId || null;
    if (!empresaId) {
      return res.status(400).json({ error: 'Admin deve selecionar uma empresa para cadastrar a categoria' });
    }
  }
  const categoria = {
    id: generateId(),
    nome: req.body.nome,
    descricao: req.body.descricao,
    cor: req.body.cor || null,
    icone: req.body.icone || null,
    ordem: req.body.ordem || 0,
    ativa: req.body.ativa !== false,
    empresaId,
    createdAt: new Date()
  };
  db.categorias.push(categoria);
  debouncedSaveData();
  emitToEmpresa('categoria_added', categoria, categoria.empresaId);
  broadcastSync('categorias', 'categorias_sync', { action: 'added', data: categoria });
  res.json(categoria);
});

app.put('/api/categorias/:id', authenticateToken, async (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  // Empresa só pode editar suas próprias categorias
  if (req.user.role === 'empresa' && db.categorias[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  db.categorias[index] = { ...db.categorias[index], ...req.body };
  debouncedSaveData();
  emitToEmpresa('categoria_updated', db.categorias[index], db.categorias[index].empresaId);
  broadcastSync('categorias', 'categorias_sync', { action: 'updated', data: db.categorias[index] });
  res.json(db.categorias[index]);
});

app.delete('/api/categorias/:id', authenticateToken, async (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  // Empresa só pode deletar suas próprias categorias
  if (req.user.role === 'empresa' && db.categorias[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const deleted = db.categorias.splice(index, 1)[0];
  debouncedSaveData();
  emitToEmpresa('categoria_deleted', deleted, deleted.empresaId);
  broadcastSync('categorias', 'categorias_sync', { action: 'deleted', data: deleted });
  res.json(deleted);
});

app.post('/api/upload', authenticateToken, upload.single('imagem'), (req, res) => {
  console.log('🔍 [DEBUG] Upload request recebido')
  console.log('🔍 [DEBUG] req.file:', req.file)
  
  if (!req.file) {
    console.log('❌ [DEBUG] Nenhum arquivo recebido')
    return res.status(400).json({ error: 'Nenhuma imagem enviada' });
  }
  
  console.log('🔍 [DEBUG] Arquivo recebido:', req.file.filename)
  console.log('🔍 [DEBUG] URL gerada:', `/uploads/${req.file.filename}`)
  
  res.json({ 
    filename: req.file.filename,
    url: `/uploads/${req.file.filename}`
  });
});

// Upload de imagem via base64 (usado pelo ProdutoModal)
app.post('/api/upload-base64', authenticateToken, async (req, res) => {
  try {
    const { base64 } = req.body;
    if (!base64) return res.status(400).json({ error: 'Nenhuma imagem enviada' });
    
    // Extrair mime type e dados do base64
    const matches = base64.match(/^data:image\/([a-zA-Z+]+);base64,(.+)$/);
    if (!matches) return res.status(400).json({ error: 'Formato de imagem inválido' });
    
    const ext = matches[1] === 'jpeg' ? 'jpg' : matches[1].replace('+', '');
    const buffer = Buffer.from(matches[2], 'base64');
    
    const filename = 'produto-' + Date.now() + '-' + Math.round(Math.random() * 1E9) + '.' + ext;
    const filepath = path.join(uploadsDir, filename);
    
    fs.writeFileSync(filepath, buffer);
    console.log('✅ Imagem salva via base64:', filename);
    
    res.json({ filename, url: `/uploads/${filename}` });
  } catch (err) {
    console.error('Erro ao salvar imagem base64:', err);
    res.status(500).json({ error: 'Erro ao salvar imagem' });
  }
});

// ==================== ROTAS DE PRODUTOS ====================
app.get('/api/produtos', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  let result = db.produtos.filter(p => p.ativo !== false);
  
  // Filtrar por empresa se role=empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    result = result.filter(p => p.empresaId === req.user.empresaId);
  }
  
  const total = result.length;
  if (!limit && !offset) return res.json(result);
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total });
});

// Rota para buscar vendas
app.get('/api/vendas', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  let result = db.vendas || [];
  
  // Empresa só vê vendas da própria empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    result = result.filter(v => v.empresaId === req.user.empresaId);
  } else if (req.query.empresaId) {
    result = result.filter(v => v.empresaId === req.query.empresaId);
  }
  
  const total = result.length;
  if (!limit && !offset) return res.json(result);
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total });
});

// Endpoint admin para limpar dados antigos
app.post('/api/admin/clear-old-data', authenticateToken, async (req, res) => {
  if (req.user.role !== 'admin') return res.status(403).json({ error: 'Apenas admin' });
  
  const today = new Date().toDateString();
  const beforeVendas = (db.vendas || []).length;
  const beforeOperacoes = (db.operacoes || []).length;
  const beforeAuditoria = (db.auditoria || []).length;
  
  // Manter só vendas de hoje
  db.vendas = (db.vendas || []).filter(v => {
    const d = new Date(v.createdAt || v.dataHora);
    return d.toDateString() === today;
  });
  
  // Manter só operações de hoje
  db.operacoes = (db.operacoes || []).filter(o => {
    const d = new Date(o.dataHora || o.timestamp);
    return d.toDateString() === today;
  });
  
  // Limpar auditoria
  db.auditoria = [];
  
  // Remover dispositivos com deviceId null (o app re-registra com ANDROID_ID correto no próximo poll)
  const nullCount = db.dispositivos.filter(d => !d.deviceId).length;
  db.dispositivos = (db.dispositivos || []).filter(d => d.deviceId);

  // Remover dispositivos de teste (por deviceId ou serialNumber)
  const testIds = ['test-check', 'test-local', 'test-render', 'deploy-check'];
  db.dispositivos = db.dispositivos.filter(d => 
    !testIds.includes(d.deviceId) && !testIds.includes(d.serialNumber)
  );

  // Remover do mapa de conectados também
  testIds.forEach(id => connectedDevices.delete(id));
  // Remover dispositivos com deviceId null do mapa
  for (const [key, val] of connectedDevices.entries()) {
    if (!key || testIds.includes(key)) connectedDevices.delete(key);
  }

  // Remover vendas e operações de teste
  db.vendas = (db.vendas || []).filter(v => !testIds.includes(v.deviceId));
  db.operacoes = (db.operacoes || []).filter(o => !testIds.includes(o.deviceId));

  console.log(`🧹 Limpeza: ${nullCount} dispositivos com deviceId null removidos, testes removidos`);
  
  await saveData();
  
  res.json({
    success: true,
    vendasRemovidas: beforeVendas - db.vendas.length,
    operacoesRemovidas: beforeOperacoes - db.operacoes.length,
    auditoriaRemovidas: beforeAuditoria,
    vendasRestantes: db.vendas.length,
    operacoesRestantes: db.operacoes.length
  });
});

app.post('/api/produtos', authenticateToken, async (req, res) => {
  // Validacao de entrada
  if (!validateString(req.body.nome, 1, 200)) {
    return res.status(400).json({ error: 'Nome do produto obrigatorio (1-200 caracteres)' });
  }
  if (req.body.preco !== undefined && !validateNumber(req.body.preco, 0, 9999999.99)) {
    return res.status(400).json({ error: 'Preco invalido' });
  }
  if (req.body.estoque !== undefined && !validateNumber(req.body.estoque, 0, 999999)) {
    return res.status(400).json({ error: 'Estoque invalido' });
  }
  
  // Sanitizar inputs
  req.body.nome = sanitizeInput(req.body.nome);
  if (req.body.descricao) req.body.descricao = sanitizeInput(req.body.descricao);
  
  let empresaId = null;
  let empresaNome = 'desconhecida';
  
  if (req.user.role === 'empresa') {
    empresaId = req.user.empresaId;
    const empresa = (db.empresas || []).find(e => e.id === empresaId);
    empresaNome = empresa?.nome || 'não encontrada';
    console.log(`📦 [DEBUG-PRODUTO] Empresa criando produto - empresaId=${empresaId} (${empresaNome}), role=${req.user.role}`);
  } else if (req.user.role === 'admin') {
    empresaId = req.body.empresaId || null;
    if (!empresaId) {
      return res.status(400).json({ error: 'Admin deve selecionar uma empresa para cadastrar o produto' });
    }
    const empresa = (db.empresas || []).find(e => e.id === empresaId);
    empresaNome = empresa?.nome || 'não encontrada';
    console.log(`📦 [DEBUG-PRODUTO] Admin criando produto - empresaId=${empresaId} (${empresaNome}), produtoNome=${req.body.nome}`);
  }
  
  console.log(`📦 [PRODUTO-CRIADO] nome=${req.body.nome}, empresaId=${empresaId}, empresa=${empresaNome}, role=${req.user.role}`);
  const produto = {
    id: generateId(),
    nome: req.body.nome,
    descricao: req.body.descricao,
    preco: req.body.preco,
    categoriaId: req.body.categoriaId ? Number(req.body.categoriaId) : null,
    codigoBarras: req.body.codigoBarras || Date.now().toString(),
    estoque: req.body.estoque || 0,
    unidade: req.body.unidade || 'un',
    imagem: (req.body.imagem && (req.body.imagem.startsWith('/uploads/') || req.body.imagem.startsWith('data:image/'))) ? req.body.imagem : null,
    empresaId,
    createdAt: new Date()
  };
  db.produtos.push(produto);
  debouncedSaveData();
  emitToEmpresa('produto_added', produto, produto.empresaId);
  broadcastSync('produtos', 'produtos_sync', { action: 'added', data: produto, logData: true });
  res.json(produto);
});

app.put('/api/produtos/:id', authenticateToken, async (req, res) => {
  const index = db.produtos.findIndex(p => p.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });
  // Empresa só pode editar seus próprios produtos
  if (req.user.role === 'empresa' && db.produtos[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const updateData = { ...req.body };
  if (updateData.categoriaId !== undefined) {
    updateData.categoriaId = updateData.categoriaId ? Number(updateData.categoriaId) : null;
  }
  db.produtos[index] = { ...db.produtos[index], ...updateData };
  debouncedSaveData();
  emitToEmpresa('produto_updated', db.produtos[index], db.produtos[index].empresaId);
  broadcastSync('produtos', 'produtos_sync', { action: 'updated', data: db.produtos[index], logData: true });
  
  res.json(db.produtos[index]);
});

app.delete('/api/produtos/:id', authenticateToken, async (req, res) => {
  const index = db.produtos.findIndex(p => p.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });
  // Empresa só pode deletar seus próprios produtos
  if (req.user.role === 'empresa' && db.produtos[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const deleted = db.produtos.splice(index, 1)[0];
  
  // Remover imagem associada se existir
  if (deleted.imagem) {
    const imagePath = path.join(__dirname, 'uploads', path.basename(deleted.imagem));
    if (fs.existsSync(imagePath)) {
      fs.unlinkSync(imagePath);
    }
  }
  
  debouncedSaveData();
  emitToEmpresa('produto_deleted', deleted, deleted.empresaId);
  broadcastSync('produtos', 'produtos_sync', { action: 'deleted', data: deleted, logData: true });
  
  res.json(deleted);
});

// ==================== ROTAS DE EMPRESAS ====================
app.get('/api/empresas', authenticateToken, (req, res) => {
  if (req.user.role !== 'admin') return res.status(403).json({ error: 'Apenas admin' });
  res.json(db.empresas || []);
});

app.post('/api/empresas', authenticateToken, async (req, res) => {
  if (req.user.role !== 'admin') return res.status(403).json({ error: 'Apenas admin' });
  const { nome, cnpj, email, telefone, login, senha, permissoes, primaryColor, secondaryColor, accentColor, logoUrl, paginasPermitidas } = req.body;

  // Validacao de entrada
  if (!validateString(nome, 2, 200)) {
    return res.status(400).json({ error: 'Nome da empresa obrigatorio (2-200 caracteres)' });
  }
  if (!validateString(login, 3, 50)) {
    return res.status(400).json({ error: 'Login obrigatorio (3-50 caracteres)' });
  }
  if (!senha || senha.length < 8 || senha.length > 128) {
    return res.status(400).json({ error: 'Senha obrigatoria (minimo 8 caracteres)' });
  }
  if (email && !validateEmail(email)) {
    return res.status(400).json({ error: 'Formato de email invalido' });
  }
  if (cnpj && !validateString(cnpj, 14, 18)) {
    return res.status(400).json({ error: 'CNPJ invalido' });
  }

  // Sanitizar inputs
  const nomeSanitizado = sanitizeInput(nome);
  const loginSanitizado = sanitizeInput(login);

  // Verificar se login já existe
  const existingLogin = (db.empresas || []).find(e => e.login === loginSanitizado);
  if (existingLogin) {
    return res.status(400).json({ error: 'Login já existe' });
  }

  // Gerar slug único baseado no nome
  const existingSlugs = (db.empresas || []).map(e => e.slug);
  const slug = generateSlug(nomeSanitizado, existingSlugs);

  const empresa = {
    id: generateId(),
    nome: nomeSanitizado,
    slug,
    cnpj: cnpj || null,
    email: email || null,
    telefone: telefone || null,
    login: loginSanitizado,
    senha: bcrypt.hashSync(senha, 10),
    permissoes: permissoes || {
      dashboard: false,
      produtos: false,
      categorias: false,
      vendas: false,
      caixa: false,
      auditoria: false,
      empresas: true,
      config: true
    },
    primaryColor: primaryColor || '#3b82f6',
    secondaryColor: secondaryColor || '#06b6d4',
    accentColor: accentColor || '#10b981',
    logoUrl: logoUrl || '',
    paginasPermitidas: paginasPermitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa', 'impressao', 'config'],
    ativo: true,
    createdAt: new Date()
  };

  if (!db.empresas) db.empresas = [];
  db.empresas.push(empresa);
  debouncedSaveData();

  // Sincronizar empresa com os terminais
  broadcastEmpresasSync();

  // Retornar empresa com URL completa
  const baseUrl = process.env.FRONTEND_URL || req.headers.origin || 'https://caixacombo.com';
  res.json({
    ...empresa,
    url: `${baseUrl}/${slug}`
  });
});

app.put('/api/empresas/:id', authenticateToken, async (req, res) => {
  const isAdmin = req.user.role === 'admin'
  const isOwnEmpresa = req.user.role === 'empresa' && req.user.empresaId == req.params.id

  if (!isAdmin && !isOwnEmpresa) {
    return res.status(403).json({ error: 'Acesso negado' })
  }

  const index = (db.empresas || []).findIndex(e => e.id == req.params.id)
  if (index === -1) return res.status(404).json({ error: 'Empresa não encontrada' })

  const { nome, cnpj, email, telefone, login, senha, permissoes, primaryColor, secondaryColor, accentColor, logoUrl, paginasPermitidas, ativo } = req.body

  // Verificar se login já existe (excluindo a empresa atual) - apenas admin pode mudar login
  if (login && isAdmin) {
    const existingLogin = (db.empresas || []).find(e => e.login === login && e.id != req.params.id)
    if (existingLogin) return res.status(400).json({ error: 'Login já existe' })
  }

  // Empresa só pode atualizar suas próprias cores/nome/logo
  if (isOwnEmpresa) {
    db.empresas[index] = {
      ...db.empresas[index],
      nome: nome || db.empresas[index].nome,
      primaryColor: primaryColor !== undefined ? primaryColor : (db.empresas[index].primaryColor || '#3b82f6'),
      secondaryColor: secondaryColor !== undefined ? secondaryColor : (db.empresas[index].secondaryColor || '#06b6d4'),
      accentColor: accentColor !== undefined ? accentColor : (db.empresas[index].accentColor || '#10b981'),
      logoUrl: logoUrl !== undefined ? logoUrl : (db.empresas[index].logoUrl || ''),
      updatedAt: new Date()
    }
  } else {
    // Admin pode atualizar tudo
    db.empresas[index] = {
      ...db.empresas[index],
      nome: nome || db.empresas[index].nome,
      cnpj: cnpj !== undefined ? cnpj : db.empresas[index].cnpj,
      email: email !== undefined ? email : db.empresas[index].email,
      telefone: telefone !== undefined ? telefone : db.empresas[index].telefone,
      login: login || db.empresas[index].login,
      senha: senha ? bcrypt.hashSync(senha, 10) : db.empresas[index].senha,
      permissoes: permissoes || db.empresas[index].permissoes,
      primaryColor: primaryColor !== undefined ? primaryColor : (db.empresas[index].primaryColor || '#3b82f6'),
      secondaryColor: secondaryColor !== undefined ? secondaryColor : (db.empresas[index].secondaryColor || '#06b6d4'),
      accentColor: accentColor !== undefined ? accentColor : (db.empresas[index].accentColor || '#10b981'),
      logoUrl: logoUrl !== undefined ? logoUrl : (db.empresas[index].logoUrl || ''),
      paginasPermitidas: paginasPermitidas || db.empresas[index].paginasPermitidas || ['dashboard', 'empresas', 'categorias', 'produtos', 'vendas', 'caixa', 'impressao', 'config'],
      ativo: ativo !== undefined ? ativo : (db.empresas[index].ativo !== undefined ? db.empresas[index].ativo : true),
      updatedAt: new Date()
    }
  }

  debouncedSaveData()
  broadcastEmpresasSync()
  
  // Enviar empresa_config_updated para terminais da empresa quando branding muda
  const updatedEmpresa = db.empresas[index];
  const empresaConfig = {
    empresaId: updatedEmpresa.id,
    nome: updatedEmpresa.nome,
    primaryColor: updatedEmpresa.primaryColor || '#3b82f6',
    secondaryColor: updatedEmpresa.secondaryColor || '#06b6d4',
    accentColor: updatedEmpresa.accentColor || '#10b981',
    logoUrl: updatedEmpresa.logoUrl || ''
  };
  connectedDevices.forEach((deviceInfo, deviceId) => {
    if (deviceInfo.empresaId === updatedEmpresa.id) {
      const deviceSocket = deviceInfo.socketId ? io.sockets.sockets.get(deviceInfo.socketId) : null;
      if (deviceSocket) {
        deviceSocket.emit('empresa_config_updated', empresaConfig);
        console.log(`🎨 [WHITELABEL] Config atualizada enviada para terminal ${deviceId}`);
      }
    }
  });
  
  res.json(db.empresas[index])
});

app.delete('/api/empresas/:id', authenticateToken, async (req, res) => {
  if (req.user.role !== 'admin') return res.status(403).json({ error: 'Apenas admin' });
  const index = (db.empresas || []).findIndex(e => e.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Empresa não encontrada' });

  const deleted = db.empresas.splice(index, 1)[0];
  debouncedSaveData();
  broadcastEmpresasSync();
  res.json(deleted);
});

// Rota para verificar status/dados da empresa (nova ou com dados)
app.get('/api/empresas/:id/status', authenticateToken, (req, res) => {
  const isAdmin = req.user.role === 'admin'
  const isOwnEmpresa = req.user.role === 'empresa' && req.user.empresaId == req.params.id

  if (!isAdmin && !isOwnEmpresa) {
    return res.status(403).json({ error: 'Acesso negado' })
  }

  const empresa = (db.empresas || []).find(e => e.id == req.params.id)
  if (!empresa) return res.status(404).json({ error: 'Empresa não encontrada' })

  const empresaId = req.params.id

  // Contar dados desta empresa
  const stats = {
    produtos: db.produtos.filter(p => p.empresaId === empresaId).length,
    categorias: db.categorias.filter(c => c.empresaId === empresaId).length,
    vendas: db.vendas.filter(v => v.empresaId === empresaId).length,
    clientes: (db.clientes || []).filter(c => c.empresaId === empresaId).length,
    operacoes: (db.operacoes || []).filter(o => o.empresaId === empresaId).length
  }

  const isNova = stats.produtos === 0 && stats.categorias === 0 && stats.vendas === 0

  res.json({
    empresa: {
      id: empresa.id,
      nome: empresa.nome,
      slug: empresa.slug,
      createdAt: empresa.createdAt
    },
    stats,
    isNova,
    mensagem: isNova
      ? 'Esta é uma empresa nova. Comece cadastrando categorias e produtos.'
      : `Empresa com ${stats.produtos} produtos, ${stats.categorias} categorias e ${stats.vendas} vendas.`
  })
});

// ==================== ROTAS DE CLIENTES ====================
app.get('/api/clientes', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  let result = db.clientes || [];
  // Filtrar por empresa se role=empresa - SÓ dados da própria empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    result = result.filter(c => c.empresaId === req.user.empresaId);
  }
  const total = result.length;
  if (!limit && !offset) return res.json(result);
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total });
});

app.post('/api/clientes', authenticateToken, async (req, res) => {
  const { nome, cpfCnpj, telefone, email, endereco, cidade, cep, observacao } = req.body;
  if (!nome) return res.status(400).json({ error: 'Nome é obrigatório' });
  const cliente = {
    id: generateId(),
    nome,
    cpfCnpj: cpfCnpj || '',
    telefone: telefone || '',
    email: email || '',
    endereco: endereco || '',
    cidade: cidade || '',
    cep: cep || '',
    observacao: observacao || '',
    ativo: true,
    empresaId: req.user.role === 'empresa' ? req.user.empresaId : (req.body.empresaId || null),
    dataCriacao: Date.now()
  };
  
  if (!db.clientes) db.clientes = [];
  db.clientes.push(cliente);
  debouncedSaveData();
  
  // Notificar terminais via WebSocket e polling
  broadcastSync('clientes', 'clientes_sync');
  
  res.json(cliente);
});

app.put('/api/clientes/:id', authenticateToken, async (req, res) => {
  const index = (db.clientes || []).findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Cliente não encontrado' });
  // Empresa só pode editar seus próprios clientes
  if (req.user.role === 'empresa' && db.clientes[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  db.clientes[index] = { ...db.clientes[index], ...req.body, id: db.clientes[index].id };
  debouncedSaveData();
  broadcastSync('clientes', 'clientes_sync');
  res.json(db.clientes[index]);
});

app.delete('/api/clientes/:id', authenticateToken, async (req, res) => {
  const index = (db.clientes || []).findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Cliente não encontrado' });
  // Empresa só pode deletar seus próprios clientes
  if (req.user.role === 'empresa' && db.clientes[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const deleted = db.clientes.splice(index, 1)[0];
  debouncedSaveData();
  
  broadcastSync('clientes', 'clientes_sync');
  
  res.json(deleted);
});

// Função auxiliar: verificar/gerenciar terminal no login
function verifyTerminalAndLogin(deviceId, empresaDoFuncionario, nomeFuncionario, req) {
  if (!deviceId) return null;
  const terminalNoBanco = db.dispositivos?.find(d => d.deviceId === deviceId);

  if (!terminalNoBanco) {
    db.dispositivos = db.dispositivos || [];
    const novoTerminal = {
      deviceId,
      deviceName: req.body.deviceName || 'Terminal Android',
      deviceType: 'Android',
      serialNumber: req.body.serialNumber || null,
      status: 'pending',
      empresaId: empresaDoFuncionario,
      lastPoll: new Date()
    };
    db.dispositivos.push(novoTerminal);
    connectedDevices.set(deviceId, {
      deviceId,
      deviceName: req.body.deviceName || 'Terminal Android',
      deviceType: 'Android',
      serialNumber: req.body.serialNumber || null,
      status: 'pending',
      empresaId: empresaDoFuncionario,
      connectedAt: new Date(),
      socketId: null
    });
    debouncedSaveData();
    console.log(`📱 [LOGIN] Novo terminal ${deviceId} registrado como pendente para empresa ${empresaDoFuncionario}`);
    return { error: 'Terminal aguardando aprovação da empresa.', status: 403 };
  }

  if (terminalNoBanco.empresaId !== empresaDoFuncionario) {
    terminalNoBanco.empresaId = empresaDoFuncionario;
    terminalNoBanco.status = 'pending';
    terminalNoBanco.lastPoll = new Date();
    const cd = connectedDevices.get(deviceId);
    if (cd) { cd.empresaId = empresaDoFuncionario; cd.status = 'pending' }
    debouncedSaveData();
    console.log(`📱 [LOGIN] Terminal transferido para empresa ${empresaDoFuncionario} - pendente de aprovação`);
    return { error: 'Terminal aguardando aprovação da empresa.', status: 403 };
  }

  if (terminalNoBanco.status === 'blocked') {
    return { error: 'Terminal bloqueado pela empresa.', status: 403 };
  }

  if (terminalNoBanco.status === 'online') {
    terminalNoBanco.lastPoll = new Date();
    terminalNoBanco.lastLogin = new Date();
    terminalNoBanco.lastLoginUser = nomeFuncionario;
    const cd = connectedDevices.get(deviceId);
    if (cd) { cd.status = 'online'; cd.empresaId = empresaDoFuncionario; cd.lastLogin = new Date(); cd.lastLoginUser = nomeFuncionario }
    debouncedSaveData();
    console.log(`✅ [LOGIN] Terminal ${deviceId} já aprovado - empresa ${empresaDoFuncionario}`);
    return null;
  }

  if (terminalNoBanco.status !== 'pending') {
    terminalNoBanco.status = 'pending';
    terminalNoBanco.lastPoll = new Date();
    const cd = connectedDevices.get(deviceId);
    if (cd) { cd.status = 'pending' }
    debouncedSaveData();
    console.log(` [LOGIN] Terminal ${deviceId} resetado para pendente - empresa ${empresaDoFuncionario}`);
  }
  return { error: 'Terminal aguardando aprovação da empresa.', status: 403 };
}

// Função auxiliar genérica: sincronizar dados para todos os terminais e dashboards
function broadcastSync(dataName, eventName, options = {}) {
  const { action = 'sync', data = null, filterKey = 'empresaId', mapFn = null, logData = false } = options;
  const items = db[dataName] || [];
  
  if (logData) {
    console.log(`📤 [BROADCAST-${dataName.toUpperCase()}] Total no banco: ${items.length}, action=${action}`);
  }

  // Via WebSocket para dashboards - cada empresa só recebe seus dados
  connectedDashboards.forEach((info, socketId) => {
    const socket = io.sockets.sockets.get(socketId);
    if (!socket) return;
    const filtered = info.role === 'empresa' && info.empresaId
      ? items.filter(i => i[filterKey] === info.empresaId)
      : items;
    const payload = { [dataName]: filtered, timestamp: new Date(), action: action || 'sync' };
    if (data) payload.data = data;
    socket.emit(eventName, payload);
  });

  // Via polling para terminais
  const deviceIds = [];
  connectedDevices.forEach((deviceInfo, deviceId) => {
    const filtered = deviceInfo.empresaId
      ? items.filter(i => i[filterKey] && String(i[filterKey]) === String(deviceInfo.empresaId))
      : items;
    const payload = { [dataName]: filtered };
    enqueueDeviceCommand(deviceId, eventName, payload);
    deviceIds.push(deviceId);
  });

  // Dispositivos do banco não conectados
  (db.dispositivos || []).forEach(d => {
    if (!deviceIds.includes(d.deviceId) && d.empresaId) {
      const filtered = items.filter(i => i[filterKey] && String(i[filterKey]) === String(d.empresaId));
      if (filtered.length > 0) {
        enqueueDeviceCommand(d.deviceId, eventName, { [dataName]: filtered });
      }
    }
  });
}

// Mantido específico pois faz map de campos e usa io.emit global
function broadcastEmpresasSync() {
  const empresas = (db.empresas || []).map(e => ({
    id: e.id,
    nome: e.nome,
    cnpj: e.cnpj,
    email: e.email,
    telefone: e.telefone,
    permissoes: e.permissoes
  }));
  io.emit('empresas_sync', { empresas });
  const deviceIds = [];
  connectedDevices.forEach((deviceInfo, deviceId) => {
    enqueueDeviceCommand(deviceId, 'empresas_sync', { empresas });
    deviceIds.push(deviceId);
  });
  console.log(`📤 [BROADCAST-EMPRESAS] ${empresas.length} empresas para ${deviceIds.length} dispositivos: ${deviceIds.join(', ')}`);
}

// Rota para aprovar dispositivo e associar a empresa
app.put('/api/dispositivos/:deviceId/aprovar', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const { empresaId } = req.body;
  const dashboardInfo = connectedDashboards.get(req.socket?.id);

  // Admin pode aprovar qualquer terminal, empresa só pode aprovar terminais pendentes para si mesma
  let targetEmpresaId = empresaId;
  
  if (req.user?.role === 'admin') {
    if (!targetEmpresaId) {
      return res.status(400).json({ error: 'empresaId é obrigatório para admin' });
    }
  } else if (req.user?.role === 'empresa') {
    // Empresa só pode aprovar terminais para si mesma
    targetEmpresaId = req.user.empresaId;
    if (!targetEmpresaId) {
      return res.status(403).json({ error: 'Empresa não possui empresaId' });
    }
  } else {
    return res.status(403).json({ error: 'Apenas admin ou empresa pode aprovar dispositivos' });
  }

  const empresa = (db.empresas || []).find(e => String(e.id) === String(targetEmpresaId));
  if (!empresa) {
    return res.status(404).json({ error: 'Empresa não encontrada' });
  }

  if (!empresa.ativo) {
    return res.status(403).json({ error: 'Empresa desativada' });
  }

  // Atualizar em memória
  const device = connectedDevices.get(deviceId);
  if (device) {
    device.empresaId = targetEmpresaId;
    device.status = 'online'; // Aprovado = online
  }

  // Atualizar no banco de dados
  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  console.log(`✅ [APROVAR] deviceId=${deviceId}, targetEmpresaId=${targetEmpresaId}, deviceIndex=${deviceIndex}, empresaId atual=${deviceIndex !== -1 ? db.dispositivos[deviceIndex].empresaId : 'N/A'}, status atual=${deviceIndex !== -1 ? db.dispositivos[deviceIndex].status : 'N/A'}`);
  if (deviceIndex !== -1) {
    db.dispositivos[deviceIndex].empresaId = targetEmpresaId;
    db.dispositivos[deviceIndex].status = 'online';
    console.log(`✅ [APROVAR] After update: empresaId=${db.dispositivos[deviceIndex].empresaId}, status=${db.dispositivos[deviceIndex].status}`);
    debouncedSaveData();
    console.log(`✅ [APROVAR] saveData enqueued for deviceId=${deviceId}`);
  } else {
    // Se dispositivo não existe no banco, criar
    db.dispositivos.push({
      deviceId,
      deviceName: device?.deviceName || 'Dispositivo',
      deviceType: device?.deviceType || 'Android',
      serialNumber: device?.serialNumber || null,
      status: 'online',
      empresaId: targetEmpresaId,
      lastPoll: new Date()
    });
    debouncedSaveData();
  }

  // Notificar dashboards
  emitDeviceEvent('device_status_update', { deviceId, empresaId: targetEmpresaId, status: 'online' });

  // Enviar empresa_config para o terminal (whitelabel) - via WebSocket E Polling
  const empresaConfig = {
    empresaId: empresa.id,
    nome: empresa.nome,
    primaryColor: empresa.primaryColor || '#3b82f6',
    secondaryColor: empresa.secondaryColor || '#06b6d4',
    accentColor: empresa.accentColor || '#10b981',
    logoUrl: empresa.logoUrl || ''
  };
  // SOMENTE produtos da empresa (NUNCA produtos globais)
  const produtosEmpresa = db.produtos.filter(p => p.empresaId && String(p.empresaId) === String(targetEmpresaId));
  const categoriasEmpresa = db.categorias.filter(c => c.empresaId && String(c.empresaId) === String(targetEmpresaId));
  const clientesEmpresa = (db.clientes || []).filter(c => c.empresaId && String(c.empresaId) === String(targetEmpresaId));

  // Via WebSocket (se conectado)
  const deviceSocket = device?.socketId ? io.sockets.sockets.get(device.socketId) : null;
  if (deviceSocket) {
    deviceSocket.emit('approval_status', { approved: true, status: 'online', empresaId: targetEmpresaId });
    deviceSocket.emit('empresa_config', empresaConfig);
    deviceSocket.emit('produtos_sync', { produtos: produtosEmpresa, timestamp: new Date() });
    deviceSocket.emit('categorias_sync', { categorias: categoriasEmpresa, timestamp: new Date() });
    deviceSocket.emit('clientes_sync', clientesEmpresa);
  }

  // Via Polling (garantir entrega para terminais SUNMI)
  enqueueDeviceCommand(deviceId, 'approval_status', { approved: true, status: 'online', empresaId: targetEmpresaId });
  enqueueDeviceCommand(deviceId + '_new', 'approval_status', { approved: true, status: 'online', empresaId: targetEmpresaId });
  enqueueDeviceCommand(deviceId, 'empresa_config', empresaConfig);
  enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: produtosEmpresa });
  if (categoriasEmpresa.length > 0) enqueueDeviceCommand(deviceId, 'categorias_sync', { categorias: categoriasEmpresa });
  if (clientesEmpresa.length > 0) enqueueDeviceCommand(deviceId, 'clientes_sync', { clientes: clientesEmpresa });
  const funcionariosEmpresa = (db.funcionarios || []).filter(f => f.empresaId === String(targetEmpresaId));
  if (funcionariosEmpresa.length > 0) enqueueDeviceCommand(deviceId, 'funcionarios_sync', { funcionarios: funcionariosEmpresa });
  console.log(`✅ Terminal ${deviceId} aprovado - empresa ${empresa.nome} (${targetEmpresaId}) - ${produtosEmpresa.length} produtos, ${categoriasEmpresa.length} categorias`);

  // Auditoria
  addAuditoria('mudanca_status', deviceId, `Dispositivo aprovado e associado à empresa ${empresa.nome}`, dashboardInfo?.usuario || req.user?.username);

  res.json({ success: true, empresaId: targetEmpresaId, status: 'online' });
});

// Rota para rejeitar/desaprovar dispositivo
app.put('/api/dispositivos/:deviceId/rejeitar', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const dashboardInfo = connectedDashboards.get(req.socket?.id);

  const device = connectedDevices.get(deviceId);
  const deviceDb = db.dispositivos?.find(d => d.deviceId === deviceId);
  const currentEmpresaId = device?.empresaId || deviceDb?.empresaId;

  // Admin pode rejeitar qualquer terminal, empresa só pode rejeitar terminais da própria empresa
  if (req.user?.role === 'empresa') {
    if (String(currentEmpresaId) !== String(req.user.empresaId)) {
      return res.status(403).json({ error: 'Empresa só pode rejeitar terminais da própria empresa' });
    }
  } else if (req.user?.role !== 'admin') {
    return res.status(403).json({ error: 'Apenas admin ou empresa pode rejeitar dispositivos' });
  }

  if (device) {
    device.empresaId = null;
    device.status = 'pending';
  }

  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  if (deviceIndex !== -1) {
    db.dispositivos[deviceIndex].empresaId = null;
    db.dispositivos[deviceIndex].status = 'pending';
    debouncedSaveData();
  }

  // Notificar terminal que foi rejeitado - via WebSocket E Polling
  const deviceSocket = device?.socketId ? io.sockets.sockets.get(device.socketId) : null;
  if (deviceSocket) {
    deviceSocket.emit('approval_status', { approved: false, status: 'pending', empresaId: null });
    deviceSocket.emit('produtos_sync', { produtos: [], timestamp: new Date() });
  }
  // Via Polling (garantir entrega)
  enqueueDeviceCommand(deviceId, 'approval_status', { approved: false, status: 'pending', empresaId: null });
  enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: [] });
  enqueueDeviceCommand(deviceId + '_new', 'approval_status', { approved: false, status: 'pending', empresaId: null });

  emitDeviceEvent('device_status_update', { deviceId, empresaId: null, status: 'pending' });

  addAuditoria('mudanca_status', deviceId, 'Dispositivo rejeitado/desassociado', dashboardInfo?.usuario || req.user?.username);

  res.json({ success: true, status: 'pending' });
});

// Rota para excluir dispositivo completamente
app.delete('/api/dispositivos/:deviceId', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const device = connectedDevices.get(deviceId);
  const deviceDb = db.dispositivos?.find(d => d.deviceId === deviceId);
  const currentEmpresaId = device?.empresaId || deviceDb?.empresaId;

  if (req.user?.role === 'empresa') {
    if (String(currentEmpresaId) !== String(req.user.empresaId)) {
      return res.status(403).json({ error: 'Empresa só pode excluir terminais da própria empresa' });
    }
  } else if (req.user?.role !== 'admin') {
    return res.status(403).json({ error: 'Apenas admin ou empresa pode excluir dispositivos' });
  }

  // Notificar terminal que foi excluído - via WebSocket E Polling
  const deviceSocket = device?.socketId ? io.sockets.sockets.get(device.socketId) : null;
  if (deviceSocket) {
    deviceSocket.emit('approval_status', { approved: false, status: 'pending', empresaId: null });
    deviceSocket.emit('produtos_sync', { produtos: [], timestamp: new Date() });
    deviceSocket.disconnect(true);
  }
  enqueueDeviceCommand(deviceId, 'approval_status', { approved: false, status: 'pending', empresaId: null });
  enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: [] });
  enqueueDeviceCommand(deviceId + '_new', 'approval_status', { approved: false, status: 'pending', empresaId: null });

  if (device) {
    connectedDevices.delete(deviceId);
  }

  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  if (deviceIndex !== -1) {
    db.dispositivos.splice(deviceIndex, 1);
    debouncedSaveData();
  }

  emitDeviceEvent('device_status_update', { deviceId, removed: true });

  addAuditoria('excluir_dispositivo', deviceId, 'Dispositivo excluído', req.user?.username);

  res.json({ success: true });
});

// ==================== ROTAS DE FUNCIONÁRIOS ====================

// Listar funcionários (admin vê todos, empresa vê só os seus)
app.get('/api/funcionarios', authenticateToken, (req, res) => {
  let funcionarios = db.funcionarios || [];
  if (req.user.role === 'empresa' && req.user.empresaId) {
    funcionarios = funcionarios.filter(f => f.empresaId === req.user.empresaId);
  }
  // Não expor dados sensíveis como PIN, mas precisamos mostrar os campos de contato
  res.json(funcionarios.map(f => ({
    id: f.id,
    nome: f.nome,
    codigo: f.codigo,
    cargo: f.cargo,
    email: f.email || '',
    cpfCnpj: f.cpfCnpj || '',
    telefone: f.telefone || '',
    permissoes: f.permissoes,
    empresaId: f.empresaId,
    ativo: f.ativo,
    createdAt: f.createdAt
  })));
});

// Criar funcionário
app.post('/api/funcionarios', authenticateToken, async (req, res) => {
  const { nome, codigo, email, pin, senha, cargo, permissoes, empresaId, ativo } = req.body;
  const targetEmpresaId = req.user.role === 'admin' ? (empresaId || req.user.empresaId) : req.user.empresaId;

  if (!nome || !targetEmpresaId) {
    return res.status(400).json({ error: 'Nome e empresaId são obrigatórios' });
  }

  const senhaFinal = senha || pin;
  let finalCodigo = (codigo || '').toString().trim();
  if (!finalCodigo) {
    const existingCodigos = (db.funcionarios || []).filter(f => f.empresaId === targetEmpresaId).map(f => f.codigo);
    do { finalCodigo = 'F' + crypto.randomInt(100000, 999999); } while (existingCodigos.includes(finalCodigo));
  }
  const existing = (db.funcionarios || []).find(f => f.codigo === finalCodigo && f.empresaId === targetEmpresaId);
  if (existing) return res.status(400).json({ error: 'Código já existe nesta empresa' });

  const funcionario = {
    id: generateId(),
    nome,
    codigo: finalCodigo,
    email: email || '',
    password: senhaFinal ? bcrypt.hashSync(senhaFinal, 10) : undefined,
    cpfCnpj: req.body.cpfCnpj || '',
    telefone: req.body.telefone || '',
    cargo: cargo || 'caixa',
    permissoes: permissoes || { vendas: true, caixa: true, produtos: false, categorias: false, relatorios: false, desconto: false, cancelar_venda: false, operacoes_caixa: true },
    empresaId: targetEmpresaId,
    ativo: ativo !== undefined ? ativo : true,
    createdAt: new Date().toISOString()
  };

  if (!db.funcionarios) db.funcionarios = [];
  db.funcionarios.push(funcionario);
  await debouncedSaveData();

  addAuditoria('criacao_funcionario', null, `Funcionário "${nome}" (${cargo}) criado com código ${finalCodigo}`, req.user?.username);
  broadcastFuncionariosSync(targetEmpresaId);
  res.json(funcionario);
});

// Atualizar funcionário
app.put('/api/funcionarios/:id', authenticateToken, async (req, res) => {
  const { id } = req.params;
  const { nome, codigo, email, pin, senha, cpfCnpj, telefone, cargo, permissoes, ativo } = req.body;

  const index = (db.funcionarios || []).findIndex(f => f.id == id);
  if (index === -1) return res.status(404).json({ error: 'Funcionário não encontrado' });

  const func = db.funcionarios[index];

  // Empresa só pode editar seus funcionários
  if (req.user.role === 'empresa' && func.empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Sem permissão' });
  }

  // Verificar código único se mudou
  if (codigo && codigo !== func.codigo) {
    const existing = db.funcionarios.find(f => f.codigo === codigo && f.empresaId === func.empresaId && f.id != id);
    if (existing) {
      return res.status(400).json({ error: 'Código já existe nesta empresa' });
    }
  }

  db.funcionarios[index] = {
    ...func,
    nome: nome || func.nome,
    codigo: codigo || func.codigo,
    email: email !== undefined ? email : func.email,
    // Atualizar senha (hash) se enviado; caso contrário manter password/pin
    password: senha ? bcrypt.hashSync(senha, 10) : func.password,
    pin: (!senha && pin) ? pin : func.pin,
    cpfCnpj: cpfCnpj !== undefined ? cpfCnpj : func.cpfCnpj,
    telefone: telefone !== undefined ? telefone : func.telefone,
    cargo: cargo || func.cargo,
    permissoes: permissoes || func.permissoes,
    ativo: ativo !== undefined ? ativo : func.ativo
  };

  await debouncedSaveData();
  addAuditoria('edicao_funcionario', null, `Funcionário "${func.nome}" atualizado`, req.user?.username);
  broadcastFuncionariosSync(func.empresaId);
  res.json(db.funcionarios[index]);
});

// Deletar funcionário
app.delete('/api/funcionarios/:id', authenticateToken, async (req, res) => {
  const { id } = req.params;
  const index = (db.funcionarios || []).findIndex(f => f.id == id);
  if (index === -1) return res.status(404).json({ error: 'Funcionário não encontrado' });

  const func = db.funcionarios[index];
  if (req.user.role === 'empresa' && func.empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Sem permissão' });
  }

  const deleted = db.funcionarios.splice(index, 1)[0];
  await debouncedSaveData();
  addAuditoria('remocao_funcionario', null, `Funcionário "${func.nome}" removido`, req.user?.username);
  broadcastFuncionariosSync(func.empresaId);
  res.json(deleted);
});

// Login de funcionário no terminal (via código)
app.post('/api/auth/funcionario', async (req, res) => {
  const { codigo, deviceId } = req.body;

  if (!codigo) {
    return res.status(400).json({ error: 'Código é obrigatório' });
  }

  // Descobrir empresaId do terminal
  let empresaId = null;
  if (deviceId) {
    const device = connectedDevices.get(deviceId);
    const deviceDb = (db.dispositivos || []).find(d => d.deviceId === deviceId);
    empresaId = device?.empresaId || deviceDb?.empresaId;
  }

  if (!empresaId) {
    return res.status(400).json({ error: 'Terminal não associado a nenhuma empresa' });
  }

  const funcionario = (db.funcionarios || []).find(f => f.codigo === codigo && f.empresaId === empresaId && f.ativo);
  if (!funcionario) {
    return res.status(401).json({ error: 'Código inválido ou funcionário inativo' });
  }

  const token = jwt.sign(
    { id: funcionario.id, nome: funcionario.nome, cargo: funcionario.cargo, permissoes: funcionario.permissoes, empresaId: funcionario.empresaId, role: 'funcionario' },
    JWT_SECRET,
    { expiresIn: '12h' }
  );

  res.json({
    token,
    funcionario: {
      id: funcionario.id,
      nome: funcionario.nome,
      cargo: funcionario.cargo,
      permissoes: funcionario.permissoes,
      empresaId: funcionario.empresaId
    }
  });
});

// Sincronizar funcionários para terminais (via poll)
function broadcastFuncionariosSync(empresaId) {
  const funcionarios = (db.funcionarios || [])
    .filter(f => f.empresaId === empresaId && f.ativo)
    .map(f => ({
      id: f.id,
      nome: f.nome,
      codigo: f.codigo,
      cargo: f.cargo,
      permissoes: f.permissoes,
      empresaId: f.empresaId
    }));

  // Via polling para terminais da empresa
  connectedDevices.forEach((deviceInfo, deviceId) => {
    if (deviceInfo.empresaId === empresaId) {
      enqueueDeviceCommand(deviceId, 'funcionarios_sync', { funcionarios });
    }
  });
  console.log(`📤 [FUNCIONARIOS-SYNC] ${funcionarios.length} funcionários para empresa ${empresaId}`);
}

// ==================== ROTAS DE DISPOSITIVOS ====================
app.put('/api/dispositivos/:deviceId/password', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const dashboardInfo = connectedDashboards.get(req.socket?.id);

  console.log(`🔑 Solicitação para mudar senha do dispositivo: ${sanitizeDeviceId(deviceId) || 'invalid'}`);

  // Encontrar dispositivo nos dados persistidos
  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  if (deviceIndex === -1) {
    console.log(`❌ [DEBUG] Dispositivo não encontrado: ${deviceId}`);
    return res.status(404).json({ error: 'Dispositivo não encontrado' });
  }

  // Gerar nova senha de 6 dígitos (crypto-secure)
  const newPassword = crypto.randomInt(100000, 999999).toString();

  // Atualizar senha no banco de dados
  db.dispositivos[deviceIndex].lockPassword = newPassword;
  debouncedSaveData(); // Salvar imediatamente

  // Atualizar também no mapa de dispositivos conectados
  const connectedDevice = connectedDevices.get(deviceId);
  if (connectedDevice) {
    connectedDevice.lockPassword = newPassword;

    // Enviar nova senha para o dispositivo
    if (connectedDevice.socketId) {
      io.to(connectedDevice.socketId).emit('device_locked', {
        reason: 'Senha de bloqueio atualizada',
        lockPassword: newPassword
      });
    }
  }

  // Notificar dashboards sobre nova senha
  emitDeviceEvent('device_password_updated', { deviceId, lockPassword: newPassword });

  // Auditoria
  addAuditoria('mudanca_status', deviceId, 'Senha de bloqueio atualizada', dashboardInfo?.usuario);

  console.log(`🔑 Nova senha gerada para ${sanitizeDeviceId(deviceId) || 'invalid'}`);

  res.json({
    message: 'Senha de bloqueio atualizada com sucesso',
    deviceId,
    lockPassword: newPassword
  });
});

// ==================== API DE CONTROLE DE DISPOSITIVOS ====================
app.post('/api/dispositivos/:deviceId/control', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const { action } = req.body;
  const dashboardInfo = { usuario: 'dashboard' };

  console.log(`🎮 [CONTROL] Comando recebido: ${deviceId} - ${action}`);
  console.log(`🎮 [CONTROL] Dispositivos conectados:`, Array.from(connectedDevices.keys()));

  const device = connectedDevices.get(deviceId);

  // Verificar se o comando requer permissões especiais
  const requiresSpecialPermissions = ['restart', 'shutdown'].includes(action);

  // Se comando reiniciar/desligar e dispositivo estiver conectado via ADB, usar ADB direto
  if (requiresSpecialPermissions) {
    console.log(`🔌 [CONTROL] Tentando via ADB para ${deviceId}`);

    try {
      const result = await sendAdbCommand(action, deviceId);

      if (result.success) {
        // Auditoria
        addAuditoria('mudanca_status', deviceId, `Comando ${action} enviado via ADB`, dashboardInfo?.usuario);

        res.json({
          message: `Comando ${action === 'restart' ? 'reiniciar' : 'desligar'} enviado via ADB. Dispositivo vai ${action === 'restart' ? 'reiniciar' : 'desligar'} agora.`,
          deviceId,
          action,
          method: 'adb',
          timestamp: new Date()
        });
        return;
      } else {
        console.log(`⚠️ [CONTROL] ADB falhou: ${result.error}`);
      }
    } catch (error) {
      console.error(`❌ [CONTROL] Erro ADB:`, error.message);
    }
  }

  // Fallback: usar WebSocket para dispositivos não-SUNMI ou comandos simples
  if (!device) {
    console.log(`❌ [CONTROL] Dispositivo ${deviceId} não encontrado em connectedDevices`);
    return res.status(404).json({ error: 'Dispositivo não encontrado ou desconectado' });
  }

  console.log(`🎮 [CONTROL] Dispositivo encontrado: ${device.deviceName}, socketId: ${device.socketId}`);

  if (requiresSpecialPermissions) {
    console.log(`⚠️ [CONTROL] Comando '${action}' requer permissões especiais (Admin ou Root)`);
  }

  // Enviar comando para o dispositivo via WebSocket e/ou enfileirar para polling
  enqueueDeviceCommand(deviceId, 'control_command', { action });

  if (device.socketId) {
    io.to(device.socketId).emit('control_command', { action });
    console.log(`📤 [CONTROL] Comando '${action}' enviado para socketId ${device.socketId} (deviceId: ${deviceId})`);
  } else {
    console.log(`📤 [CONTROL] Comando '${action}' enfileirado para polling (deviceId: ${deviceId})`);
  }

  // Auditoria
  addAuditoria('mudanca_status', deviceId, `Comando de controle enviado: ${action}`, dashboardInfo?.usuario);

  res.json({
    message: requiresSpecialPermissions
      ? `Comando enviado. Pode não funcionar se o dispositivo não tiver permissões de Admin ou Root`
      : 'Comando enviado com sucesso',
    deviceId,
    action,
    socketId: device.socketId,
    requiresSpecialPermissions,
    timestamp: new Date()
  });
});

// ==================== API DE OPERAÇÕES DE CAIXA ====================
app.get('/api/operacoes', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  let result = db.operacoes || [];
  // Empresa não filtra - vê todas as operações dos funcionários
  // Admin pode filtrar por empresa via query
  if (req.query.empresaId) {
    result = result.filter(o => o.empresaId === req.query.empresaId);
  }
  const total = result.length;
  if (!limit && !offset) return res.json(result);
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total });
});

app.post('/api/operacoes', authenticateToken, async (req, res) => {
  const { tipo, valor, deviceId, nomeOperador, observacao } = req.body;

  const operacao = {
    id: generateId(),
    tipo,
    valor: parseFloat(valor) || 0,
    deviceId,
    nomeOperador: nomeOperador || 'dashboard',
    observacao: observacao || '',
    dataHora: new Date().toISOString(),
    timestamp: Date.now(),
    empresaId: req.user.role === 'empresa' ? req.user.empresaId : (req.body.empresaId || null)
  };
  
  const valorProcessado = operacao.valor;
  
  // Salvar no banco local
  if (!db.operacoes) db.operacoes = [];
  db.operacoes.push(operacao);
  
  // Se for fechamento, salvar sessão de caixa no histórico
  if (tipo === 'fechamento') {
    if (!db.caixaSessoes) db.caixaSessoes = [];

    // Encontrar a última abertura do mesmo dispositivo
    const aberturas = db.operacoes.filter(o =>
      o.tipo === 'abertura' && (o.deviceId === deviceId || (!o.deviceId && !deviceId))
    );
    const ultimaAbertura = aberturas[aberturas.length - 1];

    if (ultimaAbertura) {
      // Operações da sessão (entre abertura e este fechamento)
      const opsSessao = db.operacoes.filter(o =>
        o.timestamp >= ultimaAbertura.timestamp && o.timestamp <= operacao.timestamp &&
        (o.deviceId === deviceId || (!o.deviceId && !deviceId) || o.tipo === 'fechamento')
      );

      // Vendas da sessão
      const vendasSessao = (db.vendas || []).filter(v => {
        const vTime = new Date(v.createdAt || v.dataHora).getTime();
        return vTime >= ultimaAbertura.timestamp && vTime <= operacao.timestamp &&
        (v.deviceId === deviceId || !deviceId);
      });

      const sessao = {
        id: generateId(),
        deviceId: deviceId || 'geral',
        empresaId: operacao.empresaId || null,
        aberturaEm: ultimaAbertura.dataHora,
        fechamentoEm: operacao.dataHora,
        operadorAbertura: ultimaAbertura.nomeOperador,
        operadorFechamento: nomeOperador || 'dashboard',
        totalAbertura: opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0),
        totalSuprimento: opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0),
        totalSangria: opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0),
        totalFechamento: opsSessao.filter(o => o.tipo === 'fechamento').reduce((s, o) => s + (o.valor || 0), 0),
        totalVendas: vendasSessao.reduce((s, v) => s + (v.total || 0), 0),
        vendasDinheiro: vendasSessao.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        vendasPix: vendasSessao.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        vendasCredito: vendasSessao.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        vendasDebito: vendasSessao.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasSessao.length
      };

      db.caixaSessoes.push(sessao);

      // Remover vendas e operações da sessão fechada do banco ativo
      // (já foram salvas no histórico caixaSessoes)
      const vendaIdsSessao = new Set(vendasSessao.map(v => v.id));
      db.vendas = (db.vendas || []).filter(v => !vendaIdsSessao.has(v.id));
      db.operacoes = db.operacoes.filter(o =>
        !(o.timestamp >= ultimaAbertura.timestamp && o.timestamp <= operacao.timestamp &&
          (o.deviceId === deviceId || (!o.deviceId && !deviceId)))
      );

      console.log(`🧹 [FECHAMENTO] Sessão fechada para ${deviceId || 'geral'}: removidas ${vendaIdsSessao.size} vendas e ${opsSessao.length} operações do banco ativo`);

      // Enfileirar comando de impressão do fechamento para o terminal
      if (deviceId && deviceId !== 'geral') {
        const vendasPrint = vendasSessao.map(v => ({
          id: v.id,
          total: v.total,
          formaPagamento: v.formaPagamento,
          createdAt: v.createdAt || v.dataHora,
          itens: (v.itens || []).map(i => ({
            produtoNome: i.produtoNome || i.nome || 'Produto',
            quantidade: i.quantidade || 1,
            total: i.total || 0
          }))
        }));
        const sangriasPrint = opsSessao
          .filter(o => o.tipo === 'sangria')
          .map(s => ({ valor: s.valor, observacao: s.observacao || 'Sangria' }));

        const printData = {
          operador: nomeOperador || 'dashboard',
          dataAbertura: ultimaAbertura.dataHora,
          dataFechamento: operacao.dataHora,
          dataAberturaTimestamp: ultimaAbertura.timestamp,
          dataFechamentoTimestamp: operacao.timestamp,
          valorInicial: ultimaAbertura.valor || 0,
          totalVendas: sessao.totalVendas,
          totalSangria: sessao.totalSangria,
          vendas: vendasPrint,
          sangrias: sangriasPrint
        };

        enqueueDeviceCommand(deviceId, 'print_fechamento', printData);

        const deviceConfig = connectedDevices.get(deviceId);
        if (deviceConfig?.socketId) {
          io.to(deviceConfig.socketId).emit('print_fechamento', printData);
        }
      }
    }
  }
  
  debouncedSaveData();
  
  // Broadcast para dashboards da empresa
  emitToEmpresa('operacao_adicionada', operacao, operacao.empresaId);

  // Broadcast para dashboards da empresa
  const opsFiltered = operacao.empresaId
    ? db.operacoes.filter(o => o.empresaId === operacao.empresaId)
    : db.operacoes;
  emitToEmpresa('operacoes_sync', { operacoes: opsFiltered }, operacao.empresaId);

  // Se fechamento, sincronizar vendas também (foram removidas do banco ativo)
  if (tipo === 'fechamento') {
    const vendasFiltered = operacao.empresaId
      ? (db.vendas || []).filter(v => v.empresaId === operacao.empresaId)
      : db.vendas || [];
    emitToEmpresa('vendas_sync', vendasFiltered, operacao.empresaId);
  }

  // Auditoria
  addAuditoria('operacao_caixa', deviceId, `${tipo} registrada: R$ ${valorProcessado.toFixed(2)}`, nomeOperador);
  
  res.json(operacao);
});

// DELETE operação (para limpar dados incorretos)
app.delete('/api/operacoes/:id', authenticateToken, async (req, res) => {
  const { id } = req.params;
  
  if (!db.operacoes) {
    return res.status(404).json({ error: 'Operações não encontradas' });
  }
  
  const index = db.operacoes.findIndex(op => op.id == id);
  if (index === -1) {
    return res.status(404).json({ error: 'Operação não encontrada' });
  }
  // Empresa só pode deletar suas próprias operações
  if (req.user.role === 'empresa' && db.operacoes[index].empresaId !== req.user.empresaId) {
    return res.status(403).json({ error: 'Acesso negado' });
  }
  const operacaoRemovida = db.operacoes.splice(index, 1)[0];
  debouncedSaveData();
  
  // Broadcast para dashboards
  emitDeviceEvent('operacao_removida', { id: id });
  
  // Auditoria
  addAuditoria('operacao_caixa', 'dashboard', `Operação removida: ${operacaoRemovida.tipo} R$ ${operacaoRemovida.valor}`, 'dashboard');
  
  res.json({ message: 'Operação removida com sucesso', operacao: operacaoRemovida });
});

// ==================== HISTÓRICO DE CAIXA E FATURAMENTO ====================
app.get('/api/caixa-sessoes', authenticateToken, (req, res) => {
  let sessoes = db.caixaSessoes || [];
  // Filtrar por empresa se role=empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    sessoes = sessoes.filter(s => s.empresaId === req.user.empresaId);
  }
  res.json(sessoes);
});

app.get('/api/faturamento', authenticateToken, (req, res) => {
  const periodo = req.query.periodo || 'diario'; // diario, semanal, mensal, anual
  let sessoes = db.caixaSessoes || [];
  let vendas = db.vendas || [];
  // Filtrar por empresa se role=empresa
  if (req.user.role === 'empresa' && req.user.empresaId) {
    sessoes = sessoes.filter(s => s.empresaId === req.user.empresaId);
    vendas = vendas.filter(v => v.empresaId === req.user.empresaId);
  }
  const now = new Date();
  
  let resultado = [];
  
  if (periodo === 'diario') {
    // Últimos 30 dias
    for (let i = 0; i < 30; i++) {
      const dia = new Date(now);
      dia.setDate(dia.getDate() - i);
      const diaStr = dia.toDateString();
      
      const vendasDia = vendas.filter(v => {
        const d = new Date(v.createdAt || v.dataHora);
        return d.toDateString() === diaStr;
      });
      
      resultado.push({
        label: dia.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
        date: dia.toISOString().split('T')[0],
        totalVendas: vendasDia.reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasDia.length,
        dinheiro: vendasDia.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        pix: vendasDia.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        credito: vendasDia.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        debito: vendasDia.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
      });
    }
  } else if (periodo === 'semanal') {
    // Últimas 12 semanas
    for (let i = 0; i < 12; i++) {
      const inicioSemana = new Date(now);
      inicioSemana.setDate(inicioSemana.getDate() - (i * 7) - inicioSemana.getDay());
      inicioSemana.setHours(0, 0, 0, 0);
      const fimSemana = new Date(inicioSemana);
      fimSemana.setDate(fimSemana.getDate() + 7);
      
      const vendasSemana = vendas.filter(v => {
        const d = new Date(v.createdAt || v.dataHora);
        return d >= inicioSemana && d < fimSemana;
      });
      
      resultado.push({
        label: `Sem ${i === 0 ? 'atual' : i}`,
        date: inicioSemana.toISOString().split('T')[0],
        totalVendas: vendasSemana.reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasSemana.length,
        dinheiro: vendasSemana.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        pix: vendasSemana.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        credito: vendasSemana.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        debito: vendasSemana.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
      });
    }
  } else if (periodo === 'mensal') {
    // Últimos 12 meses
    for (let i = 0; i < 12; i++) {
      const mes = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const mesFim = new Date(now.getFullYear(), now.getMonth() - i + 1, 0);
      
      const vendasMes = vendas.filter(v => {
        const d = new Date(v.createdAt || v.dataHora);
        return d.getMonth() === mes.getMonth() && d.getFullYear() === mes.getFullYear();
      });
      
      resultado.push({
        label: mes.toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' }),
        date: mes.toISOString().split('T')[0],
        totalVendas: vendasMes.reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasMes.length,
        dinheiro: vendasMes.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        pix: vendasMes.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        credito: vendasMes.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        debito: vendasMes.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
      });
    }
  } else if (periodo === 'anual') {
    // Últimos 5 anos
    for (let i = 0; i < 5; i++) {
      const ano = now.getFullYear() - i;
      const vendasAno = vendas.filter(v => new Date(v.createdAt || v.dataHora).getFullYear() === ano);
      
      resultado.push({
        label: ano.toString(),
        date: `${ano}-01-01`,
        totalVendas: vendasAno.reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasAno.length,
        dinheiro: vendasAno.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        pix: vendasAno.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        credito: vendasAno.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        debito: vendasAno.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0)
      });
    }
  }
  
  res.json(resultado);
});

// ==================== GERAÇÃO DE PDF ====================
app.post('/api/fechamento-pdf', authenticateToken, async (req, res) => {
  try {
    const dados = req.body;
    
    if (!dados || dados.totalAbertura === undefined) {
      return res.status(400).json({ error: 'Dados inválidos para gerar PDF' });
    }
    
    const doc = new PDFDocument({ margin: 40, size: 'A4' });
    const chunks = [];
    
    doc.on('data', chunk => chunks.push(chunk));
    doc.on('end', () => {
      const pdfBuffer = Buffer.concat(chunks);
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `attachment; filename=fechamento-geral-${new Date().toISOString().split('T')[0]}.pdf`);
      res.send(pdfBuffer);
    });
    
    // Cabeçalho
    doc.fontSize(20).fillColor('#6200EE').text('FECHAMENTO GERAL DO CAIXA', { align: 'center' });
    doc.fontSize(12).fillColor('#666666').text('CaixaCombo - Sistema de PDV', { align: 'center' });
    doc.fontSize(10).text(`Data: ${dados.dataHora}`, { align: 'center' });
    doc.moveDown(1.5);
    
    // Resumo Financeiro
    doc.fontSize(14).fillColor('#333333').text('RESUMO FINANCEIRO');
    doc.moveTo(40, doc.y).lineTo(555, doc.y).strokeColor('#6200EE').lineWidth(2).stroke();
    doc.moveDown(0.5);
    
    const addRow = (label, value, color = '#333333') => {
      doc.fontSize(10).fillColor('#666666').text(label, 40, doc.y, { continued: true, width: 300 });
      doc.fillColor(color).text(value, { align: 'right', width: 215 });
    };
    
    addRow('Total de Aberturas:', `+ R$ ${(dados.totalAbertura || 0).toFixed(2)}`, '#00C853');
    addRow('Total de Suprimentos:', `+ R$ ${(dados.totalSuprimento || 0).toFixed(2)}`, '#00C853');
    addRow('Total de Sangrias:', `- R$ ${(dados.totalSangria || 0).toFixed(2)}`, '#D50000');
    addRow('Total de Vendas:', `R$ ${(dados.totalVendas || 0).toFixed(2)}`, '#00C853');
    doc.moveDown(0.5);
    doc.moveTo(40, doc.y).lineTo(555, doc.y).strokeColor('#6200EE').lineWidth(1).stroke();
    doc.moveDown(0.3);
    addRow('SALDO FINAL:', `R$ ${(dados.totalFechamento || 0).toFixed(2)}`, '#6200EE');
    doc.moveDown(1.5);
    
    // Operações de Caixa
    if (dados.operacoes && dados.operacoes.length > 0) {
      doc.fontSize(14).fillColor('#333333').text('OPERAÇÕES DE CAIXA');
      doc.moveTo(40, doc.y).lineTo(555, doc.y).strokeColor('#6200EE').lineWidth(2).stroke();
      doc.moveDown(0.5);
      
      // Cabeçalho da tabela
      const tableTop = doc.y;
      doc.fontSize(9).fillColor('#FFFFFF');
      doc.rect(40, tableTop, 515, 20).fill('#6200EE');
      doc.fillColor('#FFFFFF').text('Tipo', 45, tableTop + 5, { width: 100 });
      doc.text('Valor', 145, tableTop + 5, { width: 100 });
      doc.text('Data/Hora', 245, tableTop + 5, { width: 150 });
      doc.text('Obs.', 395, tableTop + 5, { width: 155 });
      
      let rowY = tableTop + 22;
      dados.operacoes.forEach((op, i) => {
        if (rowY > 750) { doc.addPage(); rowY = 40; }
        if (i % 2 === 0) { doc.rect(40, rowY - 2, 515, 18).fill('#f9f9f9'); }
        const isPositive = op.tipo === 'abertura' || op.tipo === 'suprimento';
        doc.fontSize(8).fillColor('#333333').text(op.tipo.toUpperCase(), 45, rowY + 2, { width: 100 });
        doc.fillColor(isPositive ? '#00C853' : '#D50000').text(`${isPositive ? '+' : '-'} R$ ${(op.valor || 0).toFixed(2)}`, 145, rowY + 2, { width: 100 });
        doc.fillColor('#333333').text(new Date(op.dataHora || op.createdAt).toLocaleString('pt-BR'), 245, rowY + 2, { width: 150 });
        doc.text(op.observacao || '-', 395, rowY + 2, { width: 155 });
        rowY += 20;
      });
      doc.moveDown(1.5);
    }
    
    // Vendas
    if (dados.vendas && dados.vendas.length > 0) {
      if (doc.y > 600) doc.addPage();
      doc.fontSize(14).fillColor('#333333').text('VENDAS REALIZADAS');
      doc.moveTo(40, doc.y).lineTo(555, doc.y).strokeColor('#6200EE').lineWidth(2).stroke();
      doc.moveDown(0.5);
      
      const tableTop = doc.y;
      doc.fontSize(9).fillColor('#FFFFFF');
      doc.rect(40, tableTop, 515, 20).fill('#6200EE');
      doc.text('Nº', 45, tableTop + 5, { width: 80 });
      doc.text('Data/Hora', 125, tableTop + 5, { width: 150 });
      doc.text('Pagamento', 275, tableTop + 5, { width: 120 });
      doc.text('Total', 395, tableTop + 5, { width: 155 });
      
      let rowY = tableTop + 22;
      dados.vendas.forEach((venda, i) => {
        if (rowY > 750) { doc.addPage(); rowY = 40; }
        if (i % 2 === 0) { doc.rect(40, rowY - 2, 515, 18).fill('#f9f9f9'); }
        doc.fontSize(8).fillColor('#333333').text(String(venda.id || venda.numero || ''), 45, rowY + 2, { width: 80 });
        doc.text(new Date(venda.dataHora || venda.createdAt).toLocaleString('pt-BR'), 125, rowY + 2, { width: 150 });
        doc.text(venda.formaPagamento || '-', 275, rowY + 2, { width: 120 });
        doc.fillColor('#00C853').text(`R$ ${(venda.total || 0).toFixed(2)}`, 395, rowY + 2, { width: 155 });
        rowY += 20;
      });
    }
    
    // Rodapé
    if (doc.y > 700) doc.addPage();
    doc.moveDown(2);
    doc.fontSize(8).fillColor('#999999').text('Documento gerado automaticamente pelo sistema CaixaCombo', { align: 'center' });
    doc.text('Documento digital válido', { align: 'center' });
    
    doc.end();
  } catch (error) {
    console.error('Erro ao gerar PDF:', error);
    res.status(500).json({ error: 'Erro ao gerar PDF', details: error.message });
  }
});

// ==================== TERMINAL ACTIVATION & SYNC (Multi-Tenant) ====================

// POST /api/terminal/activate - Ativar terminal com código de ativação
app.post('/api/terminal/activate', async (req, res) => {
  const { activationCode, deviceId, deviceName, deviceType, serialNumber } = req.body;
  
  if (!activationCode) {
    return res.status(400).json({ error: 'Código de ativação obrigatório' });
  }
  
  console.log(`📱 [TERMINAL-ACTIVATE] Código: ${activationCode}, deviceId: ${deviceId}`);
  
  // Buscar terminal pelo código de ativação
  const terminal = db.dispositivos?.find(d => d.activationCode === activationCode);
  
  if (!terminal) {
    return res.status(404).json({ error: 'Código de ativação inválido' });
  }
  
  if (terminal.status === 'blocked') {
    return res.status(403).json({ error: 'Terminal bloqueado. Contate o administrador.' });
  }
  
  const empresa = db.empresas?.find(e => e.id === terminal.empresaId);
  if (!empresa) {
    return res.status(404).json({ error: 'Empresa whitelabel não encontrada para este terminal' });
  }
  
  if (!empresa.ativo) {
    return res.status(403).json({ error: 'Empresa desativada. Contate o administrador.' });
  }
  
  // Atualizar dados do terminal
  terminal.deviceId = deviceId || terminal.deviceId;
  terminal.deviceName = deviceName || terminal.deviceName;
  terminal.deviceType = deviceType || terminal.deviceType;
  terminal.serialNumber = serialNumber || terminal.serialNumber;
  terminal.lastPoll = new Date();
  
  // Gerar token de autenticação para o terminal
  const token = jwt.sign({
    role: 'terminal',
    deviceId: terminal.deviceId,
    empresaId: terminal.empresaId,
    isTerminal: true
  }, JWT_SECRET, { expiresIn: '30d' });
  
  console.log(`✅ [TERMINAL-ACTIVATE] Terminal ${terminal.deviceId} ativado - empresa: ${empresa.nome} (${empresa.id})`);
  
  res.json({
    success: true,
    terminalId: terminal.deviceId,
    companyId: terminal.empresaId,
    companyName: empresa.nome,
    token,
    status: terminal.status || 'pending',
    config: {
      primaryColor: empresa.primaryColor || '#3b82f6',
      secondaryColor: empresa.secondaryColor || '#06b6d4',
      accentColor: empresa.accentColor || '#10b981',
      logoUrl: empresa.logoUrl || ''
    }
  });
});

// Middleware para autenticar token de terminal
function authenticateTerminalToken(req, res, next) {
  const token = req.headers['authorization']?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Token de terminal obrigatório' });
  
  jwt.verify(token, JWT_SECRET, (err, terminal) => {
    if (err) return res.status(403).json({ error: 'Token de terminal inválido' });
    if (terminal.role !== 'terminal') return res.status(403).json({ error: 'Token não é de terminal' });
    
    req.terminal = terminal;
    req.terminalCompanyId = terminal.empresaId;
    next();
  });
}

// GET /api/terminal/sync - Sincronizar dados do terminal (multi-tenant)
app.get('/api/terminal/sync', authenticateTerminalToken, async (req, res) => {
  const companyId = req.terminalCompanyId;
  
  console.log(`📱 [TERMINAL-SYNC] companyId=${companyId}`);
  
  // Buscar dados SOMENTE da empresa vinculada ao terminal
  const empresa = db.empresas?.find(e => e.id === companyId);
  
  if (!empresa) {
    return res.status(404).json({ error: 'Empresa não encontrada' });
  }
  
  // SOMENTE produtos da empresa (NUNCA produtos globais)
  const produtosEmpresa = (db.produtos || []).filter(p => p.empresaId && String(p.empresaId) === String(companyId));
  const categoriasEmpresa = (db.categorias || []).filter(c => c.empresaId && String(c.empresaId) === String(companyId));
  const clientesEmpresa = (db.clientes || []).filter(c => c.empresaId && String(c.empresaId) === String(companyId));
  
  // Configurações da empresa
  const settings = {
    primaryColor: empresa.primaryColor || '#3b82f6',
    secondaryColor: empresa.secondaryColor || '#06b6d4',
    accentColor: empresa.accentColor || '#10b981',
    logoUrl: empresa.logoUrl || ''
  };
  
  console.log(`📱 [TERMINAL-SYNC] ${companyId}: ${produtosEmpresa.length} produtos, ${categoriasEmpresa.length} categorias, ${clientesEmpresa.length} clientes`);
  
  res.json({
    company: {
      id: empresa.id,
      name: empresa.nome,
      settings
    },
    products: produtosEmpresa,
    categories: categoriasEmpresa,
    customers: clientesEmpresa,
    updatedAt: new Date().toISOString()
  });
});

// ==================== POLLING REST API (Stone Compliance - sem WebSocket no POS) ====================

// Blocklist de deviceIds de teste
const BLOCKED_DEVICE_IDS = ['test-check', 'test-local', 'test-render', 'deploy-check'];

function isValidSerial(serial) {
  return typeof serial === 'string' && serial.trim().length > 0 && serial.trim().toUpperCase() !== 'UNKNOWN';
}

// Middleware: validar que deviceId é uma string válida e não está na blocklist
function validateDeviceRequest(req, res, next) {
  const { deviceId } = req.body || req.params || {};
  if (!deviceId || typeof deviceId !== 'string' || deviceId.trim().length === 0) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }
  if (BLOCKED_DEVICE_IDS.includes(deviceId)) {
    return res.status(403).json({ error: 'Dispositivo bloqueado' });
  }
  // Sanitizar: só alphanumeric, hífens, underscores, pontos
  if (!/^[a-zA-Z0-9\-_.:]{1,128}$/.test(deviceId)) {
    return res.status(400).json({ error: 'deviceId com formato inválido' });
  }
  next();
}

// Dispositivo faz heartbeat e recebe comandos pendentes
app.post('/api/device/poll', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, deviceName, deviceType, serialNumber, status, caixaData } = req.body;
  
  if (!deviceId) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }

  // Bloquear dispositivos de teste
  if (BLOCKED_DEVICE_IDS.includes(deviceId)) {
    return res.status(403).json({ error: 'Dispositivo bloqueado' });
  }

  const existing = connectedDevices.get(deviceId);
  const existingDb = db.dispositivos?.find(d => d.deviceId === deviceId);
  const existingSerial = existing?.serialNumber || existingDb?.serialNumber || null;
  const providedSerial = isValidSerial(serialNumber) ? serialNumber : null;

  if (existingSerial && providedSerial && existingSerial !== providedSerial) {
    addAuditoria('bloqueio', deviceId, `Serial mismatch recebido: esperado=${existingSerial} recebido=${providedSerial}`, 'Sistema');
    return res.status(403).json({ error: 'Serial number mismatch do deviceId. Contate o administrador.' });
  }

  if (!existingSerial && providedSerial && existing) {
    addAuditoria('bloqueio', deviceId, `Serial válido registrado pela primeira vez: ${providedSerial}`, 'Sistema');
  }

  if (!existing && !existingDb && !providedSerial) {
    addAuditoria('bloqueio', deviceId, 'Novo dispositivo conectado sem serial válido; manter em pending', 'Sistema');
  }

  // Registrar/atualizar dispositivo no mapa
  // Preservar senha existente (do mapa ou do banco) - NÃO gerar nova senha no poll
  const lockPassword = existing?.lockPassword || existingDb?.lockPassword || null;
  const pollEmpresaId = existingDb?.empresaId || existing?.empresaId || null;
  const dbStatus = existingDb?.status || existing?.status || null;
  const isApproved = pollEmpresaId && dbStatus !== 'pending';
  const pollStatus = isApproved ? (dbStatus || 'online') : 'pending';
  
console.log(`📱 [POLL] deviceId=${deviceId}, isApproved=${isApproved}, pollEmpresaId=${pollEmpresaId}, dbStatus=${dbStatus}, pollStatus=${pollStatus}`);
  if (isApproved && pollEmpresaId) {
    // Terminal aprovado: enviar dados SOMENTE da empresa (NUNCA produtos globais)
    const produtosEmpresa = (db.produtos || []).filter(p => p.empresaId && String(p.empresaId) === String(pollEmpresaId));
    console.log(`📦 [POLL-SYNC] ${deviceId} - empresa ${pollEmpresaId}: ${produtosEmpresa.length} produtos`);
    console.log(`📦 [POLL-SYNC] Produtos: ${produtosEmpresa.map(p => `${p.nome}(emp=${p.empresaId})`).join(', ')}`);
    const categoriasEmpresa = (db.categorias || []).filter(c => c.empresaId && String(c.empresaId) === String(pollEmpresaId));
    const clientesEmpresa = (db.clientes || []).filter(c => c.empresaId && String(c.empresaId) === String(pollEmpresaId));
    enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: produtosEmpresa });
    if (categoriasEmpresa.length > 0) enqueueDeviceCommand(deviceId, 'categorias_sync', { categorias: categoriasEmpresa });
    if (clientesEmpresa.length > 0) enqueueDeviceCommand(deviceId, 'clientes_sync', { clientes: clientesEmpresa });
    // Enviar config da empresa (whitelabel)
    const empresa = (db.empresas || []).find(e => e.id === pollEmpresaId);
    const impressaoTemplate = empresa?.impressaoTemplate || db.impressaoTemplate || null;
    if (empresa) {
      enqueueDeviceCommand(deviceId, 'empresa_config', {
        empresaId: empresa.id, nome: empresa.nome,
        primaryColor: empresa.primaryColor || '#3b82f6', secondaryColor: empresa.secondaryColor || '#06b6d4',
        accentColor: empresa.accentColor || '#10b981', logoUrl: empresa.logoUrl || '',
        designApp: impressaoTemplate?.designApp || { tipo: 'mercado' }
      });
    }
    if (impressaoTemplate) {
      enqueueDeviceCommand(deviceId, 'print_config_sync', { config: impressaoTemplate });
    }
    const funcionariosEmpresa = (db.funcionarios || []).filter(f => f.empresaId === String(pollEmpresaId));
    if (funcionariosEmpresa.length > 0) {
      enqueueDeviceCommand(deviceId, 'funcionarios_sync', { funcionarios: funcionariosEmpresa });
    }
  } else {
    // Terminal pendente: sem dados
    console.log(`⏳ [POLL-SYNC] ${deviceId} - PENDENTE, sem sync`);
    enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: [] });
  }
  // Sempre enviar approval_status
  enqueueDeviceCommand(deviceId, 'approval_status', { approved: isApproved, status: pollStatus, empresaId: pollEmpresaId });

  // Retornar comandos pendentes para o dispositivo
  const commands = pendingCommands.get(deviceId) || [];
  pendingCommands.delete(deviceId);
  const newCmds = pendingCommands.get(deviceId + '_new') || [];
  pendingCommands.delete(deviceId + '_new');
  commands.push(...newCmds);
  
  // Log detalhado dos comandos enviados
  if (commands.length > 0) {
    const cmdSummary = commands.map(c => `${c.command}(${c.params?.categorias?.length || c.params?.produtos?.length || c.params?.clientes?.length || ''})`).join(', ');
    console.log(`📤 [POLL-RESPONSE] ${deviceId}: ${commands.length} comandos: ${cmdSummary}`);
  }

  res.json({ 
    success: true, 
    commands: commands,
    serverTime: Date.now()
  });
});

// Enviar venda via REST
app.post('/api/device/sale', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, sale } = req.body;
  
  console.log(`💰 [SALE] Venda recebida via REST - deviceId: ${deviceId}, sale.id: ${sale?.id}, total: ${sale?.total}, forma: ${sale?.formaPagamento}`);
  
  // Bloquear vendas de terminais não aprovados
  const deviceInfo = connectedDevices.get(deviceId);
  const dbDevice = db.dispositivos?.find(d => d.deviceId === deviceId);
  if ((!deviceInfo?.empresaId && !dbDevice?.empresaId) || deviceInfo?.status === 'pending' || dbDevice?.status === 'pending') {
    console.log(`❌ [SALE] Terminal ${deviceId} não aprovado - venda rejeitada`);
    return res.status(403).json({ error: 'Terminal não aprovado. Aguarde aprovação do administrador.' });
  }
  
  if (!deviceId || !sale) {
    console.log(`❌ [SALE] Dados inválidos - deviceId: ${deviceId}, sale: ${!!sale}`);
    return res.status(400).json({ error: 'deviceId e sale obrigatórios' });
  }

  // Processar venda igual ao WebSocket
  if (!db.vendas) db.vendas = [];
  
  // Adicionar deviceId e createdAt se não existirem
  const saleDeviceInfo = connectedDevices.get(deviceId);
  const enrichedSale = {
    ...sale,
    deviceId: sale.deviceId || deviceId,
    deviceName: sale.deviceName || saleDeviceInfo?.deviceName || deviceId,
    empresaId: sale.empresaId || saleDeviceInfo?.empresaId || null,
    createdAt: sale.createdAt || new Date().toISOString()
  };
  
  const existingIndex = db.vendas.findIndex(v => v.id === enrichedSale.id);
  if (existingIndex === -1) {
    db.vendas.push(enrichedSale);
  } else {
    db.vendas[existingIndex] = enrichedSale;
  }
  debouncedSaveData();

  // Notificar dashboards da empresa
  emitToEmpresa('venda_added', enrichedSale, enrichedSale.empresaId);

  // Auto-unlock se dispositivo estava bloqueado
  const device = connectedDevices.get(deviceId);
  if (device && device.status === 'locked') {
    device.status = 'online';
  }

  res.json({ success: true });
});

// Enviar operação de caixa via REST (sem autenticação - usado pelos terminais Android)
app.post('/api/device/operacao', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, tipo, valor, nomeOperador, observacao } = req.body;
  
  if (!deviceId || !tipo) {
    return res.status(400).json({ error: 'deviceId e tipo obrigatórios' });
  }

  // Bloquear operações de terminais não aprovados
  const deviceInfo = connectedDevices.get(deviceId);
  const dbDevice = db.dispositivos?.find(d => d.deviceId === deviceId);
  if ((!deviceInfo?.empresaId && !dbDevice?.empresaId) || deviceInfo?.status === 'pending' || dbDevice?.status === 'pending') {
    return res.status(403).json({ error: 'Terminal não aprovado. Aguarde aprovação do administrador.' });
  }

  const operacao = {
    id: generateId(),
    tipo,
    valor: parseFloat(valor) || 0,
    deviceId,
    nomeOperador: nomeOperador || 'terminal',
    observacao: observacao || '',
    dataHora: new Date().toISOString(),
    timestamp: Date.now(),
    empresaId: deviceInfo?.empresaId || null
  };

  // Salvar no banco local
  if (!db.operacoes) db.operacoes = [];
  db.operacoes.push(operacao);

  // Se for fechamento, salvar sessão de caixa no histórico
  if (tipo === 'fechamento') {
    if (!db.caixaSessoes) db.caixaSessoes = [];
    
    const aberturas = db.operacoes.filter(o => 
      o.tipo === 'abertura' && (o.deviceId === deviceId || (!o.deviceId && !deviceId))
    );
    const ultimaAbertura = aberturas[aberturas.length - 1];
    
    if (ultimaAbertura) {
      const opsSessao = db.operacoes.filter(o => 
        o.timestamp >= ultimaAbertura.timestamp && o.timestamp <= operacao.timestamp &&
        (o.deviceId === deviceId || (!o.deviceId && !deviceId) || o.tipo === 'fechamento')
      );
      
      const vendasSessao = (db.vendas || []).filter(v => {
        const vTime = new Date(v.createdAt || v.dataHora).getTime();
        return vTime >= ultimaAbertura.timestamp && vTime <= operacao.timestamp &&
        (v.deviceId === deviceId || !deviceId);
      });
      
      db.caixaSessoes.push({
        id: generateId(),
        deviceId: deviceId || 'geral',
        empresaId: operacao.empresaId || null,
        aberturaEm: ultimaAbertura.dataHora,
        fechamentoEm: operacao.dataHora,
        operadorAbertura: ultimaAbertura.nomeOperador,
        operadorFechamento: nomeOperador || 'terminal',
        totalAbertura: opsSessao.filter(o => o.tipo === 'abertura').reduce((s, o) => s + (o.valor || 0), 0),
        totalVendas: vendasSessao.reduce((s, v) => s + (v.total || 0), 0),
        totalSangrias: opsSessao.filter(o => o.tipo === 'sangria').reduce((s, o) => s + (o.valor || 0), 0),
        totalSuprimentos: opsSessao.filter(o => o.tipo === 'suprimento').reduce((s, o) => s + (o.valor || 0), 0),
        vendasDinheiro: vendasSessao.filter(v => v.formaPagamento === 'DINHEIRO').reduce((s, v) => s + (v.total || 0), 0),
        vendasPix: vendasSessao.filter(v => v.formaPagamento === 'PIX').reduce((s, v) => s + (v.total || 0), 0),
        vendasCredito: vendasSessao.filter(v => v.formaPagamento === 'CREDITO' || v.formaPagamento === 'CARTAO_CREDITO').reduce((s, v) => s + (v.total || 0), 0),
        vendasDebito: vendasSessao.filter(v => v.formaPagamento === 'DEBITO' || v.formaPagamento === 'CARTAO_DEBITO').reduce((s, v) => s + (v.total || 0), 0),
        qtdVendas: vendasSessao.length,
        vendas: vendasSessao
      });

      // Remover vendas e operações da sessão fechada do banco ativo
      const vendaIdsSessao = new Set(vendasSessao.map(v => v.id));
      db.vendas = (db.vendas || []).filter(v => !vendaIdsSessao.has(v.id));
      db.operacoes = db.operacoes.filter(o =>
        !(o.timestamp >= ultimaAbertura.timestamp && o.timestamp <= operacao.timestamp &&
          (o.deviceId === deviceId || (!o.deviceId && !deviceId)))
      );

      console.log(`🧹 [FECHAMENTO-TERMINAL] Sessão fechada para ${deviceId}: removidas ${vendaIdsSessao.size} vendas e ${opsSessao.length} operações do banco ativo`);
    }
  }

  debouncedSaveData();

  // Broadcast para dashboards da empresa
  emitToEmpresa('operacao_adicionada', operacao, operacao.empresaId);
  const opsFiltered2 = operacao.empresaId
    ? db.operacoes.filter(o => o.empresaId === operacao.empresaId)
    : db.operacoes;
  emitToEmpresa('operacoes_sync', { operacoes: opsFiltered2 }, operacao.empresaId);

  // Se fechamento, sincronizar vendas também
  if (tipo === 'fechamento') {
    const vendasFiltered2 = operacao.empresaId
      ? (db.vendas || []).filter(v => v.empresaId === operacao.empresaId)
      : db.vendas || [];
    emitToEmpresa('vendas_sync', vendasFiltered2, operacao.empresaId);
  }

  // Auditoria
  addAuditoria('operacao_caixa', deviceId, `${tipo}: R$ ${operacao.valor.toFixed(2)}`, nomeOperador);

  console.log(`✅ Operação de caixa via REST: ${tipo} de ${deviceId} - R$ ${operacao.valor.toFixed(2)}`);

  res.json({ success: true, operacao });
});

// Enviar status do dispositivo via REST
app.post('/api/device/status', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, status } = req.body;
  
  if (!deviceId) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }

  const device = connectedDevices.get(deviceId);
  if (device) {
    const statusAnterior = device.status;
    device.status = status;
    device.lastPoll = new Date();
    
    // Auditoria se houve mudança de status
    if (statusAnterior !== status) {
      addAuditoria('mudanca_status', deviceId, `Status alterado via REST: ${statusAnterior} → ${status}`);
    }
    
    emitDeviceEvent('device_status_update', { deviceId, status, lockReason: device.lockReason, lockedAt: device.lockedAt, usageTimeLimit: device.usageTimeLimit, usageStartTime: device.usageStartTime });
  }

  res.json({ success: true });
});

// Enviar atualização de estoque via REST
app.post('/api/device/estoque', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, produtoId, novoEstoque } = req.body;
  
  if (!deviceId) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }

  const produto = db.produtos.find(p => p.id === produtoId);
  if (produto) {
    const estoqueAnterior = produto.estoque;
    produto.estoque = novoEstoque;
    debouncedSaveData();
    emitDeviceEvent('estoque_update', { deviceId, produtoId, novoEstoque });
    addAuditoria('estoque', deviceId, `Estoque atualizado: ${produto.nome} (${estoqueAnterior} -> ${novoEstoque})`, connectedDevices.get(deviceId)?.deviceName);
  }

  res.json({ success: true });
});

// Confirmar bloqueio via REST
app.post('/api/device/lock-confirmed', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId } = req.body;
  const device = connectedDevices.get(deviceId);
  if (device) {
    device.status = 'locked';
    emitDeviceEvent('lock_confirmed', { deviceId });
    addAuditoria('bloqueio', deviceId, 'Dispositivo bloqueado com sucesso', 'Sistema');
  }
  res.json({ success: true });
});

// Confirmar desbloqueio via REST
app.post('/api/device/unlock-confirmed', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId } = req.body;
  const device = connectedDevices.get(deviceId);
  if (device) {
    device.status = 'online';
    emitDeviceEvent('unlock_confirmed', { deviceId });
    addAuditoria('desbloqueio', deviceId, 'Dispositivo desbloqueado', 'Sistema');
  }
  res.json({ success: true });
});

// Tentativa de desbloqueio via REST (com rate limiting)
app.post('/api/device/unlock-attempt', unlockLimiter, async (req, res) => {
  const { deviceId, password } = req.body;
  const device = connectedDevices.get(deviceId);
  
  if (!device) {
    return res.status(404).json({ success: false, message: 'Dispositivo não encontrado' });
  }

  if (device.lockPassword && password === device.lockPassword) {
    device.status = 'online';
    device.lockReason = null;
    device.lockedAt = null;
    // NÃO gerar nova senha aqui - manter a mesma até o próximo bloqueio
    emitDeviceEvent('unlock_response', { deviceId, success: true, message: 'Desbloqueado com sucesso' });
    emitDeviceEvent('device_status_update', { deviceId, status: 'online' });
    addAuditoria('desbloqueio', deviceId, 'Desbloqueio via senha', 'Terminal');
    res.json({ success: true, message: 'Desbloqueado com sucesso' });
  } else {
    addAuditoria('bloqueio', deviceId, `Tentativa de desbloqueio falhou: senha incorreta`, 'Terminal');
    res.json({ success: false, message: 'Senha incorreta' });
  }
});

// Enviar resultado de controle via REST
app.post('/api/device/control-result', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, action, success, error } = req.body;
  emitDeviceEvent('control_result', { deviceId, action, success, error });
  res.json({ success: true });
});

// Forçar sincronização de produtos/categorias para todos os terminais (dashboard)
app.post('/api/force-sync', authenticateToken, async (req, res) => {
  const { type } = req.body; // 'produtos', 'categorias', 'all'
  let synced = [];
  
  console.log(`🔄 [FORCE-SYNC] Estado atual: ${db.produtos.length} produtos, ${db.categorias.length} categorias`);
  if (db.categorias.length > 0) {
    console.log(`🔄 [FORCE-SYNC] Categorias: ${db.categorias.map(c => `id=${c.id} nome=${c.nome}`).join(', ')}`);
  }
  if (db.produtos.length > 0) {
    console.log(`🔄 [FORCE-SYNC] Produtos: ${db.produtos.map(p => `id=${p.id} nome=${p.nome} catId=${p.categoriaId}`).join(', ')}`);
  }
  
  if (type === 'produtos' || type === 'all') {
    broadcastSync('produtos', 'produtos_sync', { action: 'force_sync', logData: true });
    // Também enfileirar para dispositivos do banco que podem não estar no connectedDevices
    (db.dispositivos || []).forEach(d => {
      if (!connectedDevices.has(d.deviceId)) {
        enqueueDeviceCommand(d.deviceId, 'produtos_sync', { produtos: db.produtos || [] });
      }
    });
    synced.push('produtos');
  }
  if (type === 'categorias' || type === 'all') {
    broadcastSync('categorias', 'categorias_sync', { action: 'force_sync' });
    (db.dispositivos || []).forEach(d => {
      if (!connectedDevices.has(d.deviceId)) {
        enqueueDeviceCommand(d.deviceId, 'categorias_sync', { categorias: db.categorias || [] });
      }
    });
    synced.push('categorias');
  }
  if (type === 'clientes' || type === 'all') {
    broadcastSync('clientes', 'clientes_sync');
    (db.dispositivos || []).forEach(d => {
      if (!connectedDevices.has(d.deviceId)) {
        enqueueDeviceCommand(d.deviceId, 'clientes_sync', { clientes: db.clientes || [] });
      }
    });
    synced.push('clientes');
  }
  const totalDevices = Math.max(connectedDevices.size, (db.dispositivos || []).length);
  console.log(`🔄 [FORCE-SYNC] Sincronização forçada: ${synced.join(', ')} para ${totalDevices} dispositivo(s)`);
  res.json({ success: true, synced, devices: totalDevices });
});

// Sincronizar produtos via REST (dispositivo envia seus produtos)
app.post('/api/device/produtos-sync', async (req, res) => {
  const { deviceId, produtos } = req.body;
  // Notificar dashboards
  emitDeviceEvent('produtos_sync', { deviceId, produtos });
  res.json({ success: true });
});

// Terminal salva/edita produto sem autenticação JWT (usa deviceId)
app.post('/api/device/produto-save', deviceLimiter, validateDeviceRequest, async (req, res) => {
  const { deviceId, produto } = req.body;
  if (!produto || !produto.nome) {
    return res.status(400).json({ error: 'Dados do produto obrigatórios' });
  }

  if (produto.id && produto.id !== 0) {
    // Editar produto existente
    const index = db.produtos.findIndex(p => p.id == produto.id);
    if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });

    const updateData = { ...produto };
    if (updateData.categoriaId !== undefined) {
      updateData.categoriaId = updateData.categoriaId ? Number(updateData.categoriaId) : null;
    }
    // Sanitizar imagem: aceitar base64 ou null
    if (updateData.imagem && !updateData.imagem.startsWith('data:image/') && !updateData.imagem.startsWith('http')) {
      updateData.imagem = null;
    }

    db.produtos[index] = { ...db.produtos[index], ...updateData };
    debouncedSaveData();
    emitToEmpresa('produto_updated', db.produtos[index], db.produtos[index].empresaId);
  broadcastSync('produtos', 'produtos_sync', { action: 'updated', data: db.produtos[index], logData: true });
    console.log(`📝 [DEVICE-PRODUTO] Device ${deviceId} editou produto: ${produto.nome}`);
    res.json(db.produtos[index]);
  } else {
    // Criar novo produto
    const novo = {
      id: generateId(),
      nome: produto.nome,
      descricao: produto.descricao || '',
      preco: produto.preco || 0,
      categoriaId: produto.categoriaId ? Number(produto.categoriaId) : null,
      codigoBarras: produto.codigoBarras || Date.now().toString(),
      estoque: produto.estoque || 0,
      unidade: produto.unidade || 'un',
      imagem: (produto.imagem && produto.imagem.startsWith('data:image/')) ? produto.imagem : null,
      createdAt: new Date()
    };
    db.produtos.push(novo);
    debouncedSaveData();
    emitToEmpresa('produto_added', novo, novo.empresaId);
    broadcastSync('produtos', 'produtos_sync', { action: 'added', data: novo, logData: true });
    console.log(`📝 [DEVICE-PRODUTO] Device ${deviceId} criou produto: ${novo.nome}`);
    res.json(novo);
  }
});

// Função auxiliar: enfileirar comando para dispositivo (usado pelo dashboard)
function enqueueDeviceCommand(deviceId, command, params = {}) {
  if (!pendingCommands.has(deviceId)) {
    pendingCommands.set(deviceId, []);
  }
  pendingCommands.get(deviceId).push({
    command,
    params,
    timestamp: Date.now()
  });
}

// ==================== WEBSOCKET ====================
io.on('connection', (socket) => {
  console.log('🔌 Socket:', socket.id);

  socket.on('device_connect', async (data) => {
    const { deviceId, deviceName, deviceType, serialNumber } = data;

    const existing = connectedDevices.get(deviceId);

    // Detectar se dispositivo estava bloqueado e está reconectando (possível desbloqueio)
    if (existing && existing.status === 'locked') {
      console.log(`🔓 [AUTO-UNLOCK] Dispositivo ${deviceId} reconectando - possível desbloqueio via terminal`);

      // Marcar como online se estava bloqueado e reconectou
      existing.status = 'online';
      delete existing.lockReason;
      delete existing.lockedAt;
      delete existing.usageTimeLimit;
      delete existing.usageStartTime;

      // Auditoria: Desbloqueio detectado por reconexão
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado automaticamente (reconexão detectada)');

      // Notificar dashboards sobre desbloqueio
      emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
    }

    // Auditoria: Conexão
    addAuditoria('conexao', deviceId, `Dispositivo conectado - ${deviceType} (${serialNumber})`);

    if (existing && existing.socketId && existing.socketId !== socket.id) {
      const oldSocket = io.sockets.sockets.get(existing.socketId);
      if (oldSocket) oldSocket.disconnect();
    }

    const existingDb = db.dispositivos?.find(d => d.deviceId === deviceId);
    const lockPassword = existing?.lockPassword || existingDb?.lockPassword || null;
    // Dispositivo novo sem empresaId = pending; já aprovado mantém status
    const isApproved = !!(existing?.empresaId || existingDb?.empresaId);
    const isLocked = existing && existing.status === 'locked';
    const deviceStatus = isLocked ? 'locked' : (isApproved ? 'online' : 'pending');
    const deviceEmpresaId = existing?.empresaId || existingDb?.empresaId || null;

    connectedDevices.set(deviceId, {
      socketId: socket.id,
      deviceName: deviceName || 'Dispositivo',
      deviceType: deviceType || 'Android',
      serialNumber: serialNumber || deviceId,
      lastLogin: existing?.lastLogin || existingDb?.lastLogin || null,
      lastLoginUser: existing?.lastLoginUser || existingDb?.lastLoginUser || null,
      lastPoll: existing?.lastPoll || existingDb?.lastPoll || null,
      connectedAt: new Date(),
      status: deviceStatus,
      lockPassword: lockPassword,
      lockReason: existing?.lockReason || existingDb?.lockReason || null,
      lockedAt: existing?.lockedAt || existingDb?.lockedAt || null,
      usageTimeLimit: existing?.usageTimeLimit ?? existingDb?.usageTimeLimit ?? null,
      usageStartTime: existing?.usageStartTime ?? existingDb?.usageStartTime ?? null,
      empresaId: deviceEmpresaId
    });

    // Salvar dispositivo no banco de dados se não existir
    const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
    if (deviceIndex === -1) {
      db.dispositivos.push({
        deviceId,
        deviceName: deviceName || 'Dispositivo',
        deviceType: deviceType || 'Android',
        serialNumber: serialNumber || deviceId,
        status: deviceStatus,
        lockPassword: lockPassword,
        empresaId: deviceEmpresaId,
        lastLogin: existing?.lastLogin || existingDb?.lastLogin || null,
        lastLoginUser: existing?.lastLoginUser || existingDb?.lastLoginUser || null,
        lastPoll: existing?.lastPoll || existingDb?.lastPoll || null
      });
      debouncedSaveData();
      console.log(`💾 Dispositivo ${deviceId} salvo no banco [${deviceStatus}]`);
    } else {
      // Atualizar dispositivo existente
      const existingDev = db.dispositivos[deviceIndex];
      existingDev.status = deviceStatus;
      existingDev.lockPassword = lockPassword;
      existingDev.connectedAt = new Date();
      existingDev.lastLogin = existing?.lastLogin || existingDb?.lastLogin || existingDev.lastLogin || null;
      existingDev.lastLoginUser = existing?.lastLoginUser || existingDb?.lastLoginUser || existingDev.lastLoginUser || null;
      existingDev.lastPoll = existing?.lastPoll || existingDb?.lastPoll || existingDev.lastPoll || null;
      debouncedSaveData();
    }

    console.log(`📱 ${deviceName} (${deviceId}) [${deviceStatus}] empresaId=${deviceEmpresaId || 'nenhum'}`);
    emitDeviceEvent('device_connected', { deviceId, ...connectedDevices.get(deviceId), online: true });

    // Entrar na sala da empresa (para receber eventos apenas da empresa correta)
    if (deviceEmpresaId) {
      socket.join(`empresa_${deviceEmpresaId}`);
      console.log(`🏠 Terminal ${deviceId} entrou na sala empresa_${deviceEmpresaId}`);
    }

    // Notificar terminal sobre seu status de aprovação
    socket.emit('approval_status', { approved: isApproved, status: deviceStatus, empresaId: deviceEmpresaId });

    // Se aprovado, enviar configuração da empresa (whitelabel)
    if (isApproved && deviceEmpresaId) {
      const empresa = (db.empresas || []).find(e => e.id === deviceEmpresaId);
      if (empresa) {
        socket.emit('empresa_config', {
          empresaId: empresa.id,
          nome: empresa.nome,
          primaryColor: empresa.primaryColor || '#3b82f6',
          secondaryColor: empresa.secondaryColor || '#06b6d4',
          accentColor: empresa.accentColor || '#10b981',
          logoUrl: empresa.logoUrl || ''
        });
      }
      // Enviar produtos filtrados pela empresa
      const produtosEmpresa = db.produtos.filter(p => p.empresaId && String(p.empresaId) === String(deviceEmpresaId));
      socket.emit('produtos_sync', { produtos: produtosEmpresa, timestamp: new Date() });
      console.log(`📦 Enviados ${produtosEmpresa.length} produtos para ${deviceId} (empresa ${deviceEmpresaId})`);
    } else {
      // Dispositivo pendente: enviar lista vazia
      socket.emit('produtos_sync', { produtos: [], timestamp: new Date() });
      console.log(`⏳ Dispositivo ${deviceId} pendente - sem produtos enviados`);
    }
  });

  socket.on('device_status', async (data) => {
    const { deviceId, status } = data;
    const device = connectedDevices.get(deviceId);
    if (device) {
      const statusAnterior = device.status;
      device.status = status;
      
      // Se estava bloqueado e mudou para online, registrar desbloqueio
      if (statusAnterior === 'locked' && status === 'online') {
        console.log(`🔓 [AUTO-DETECT] Dispositivo ${deviceId} desbloqueado via terminal (status: ${statusAnterior} → ${status})`);
        delete device.lockReason;
        delete device.lockedAt;
        delete device.usageTimeLimit;
        delete device.usageStartTime;
        
        // Auditoria: Desbloqueio detectado
        addAuditoria('desbloqueio', deviceId, 'Desbloqueado via terminal (detectado automaticamente)');
      }
      
      // Auditoria: Mudança de status
      if (statusAnterior !== status) {
        addAuditoria('mudanca_status', deviceId, `Status alterado: ${statusAnterior} → ${status}`);
      }
      
      emitDeviceEvent('device_status_update', { deviceId, status, lockReason: device.lockReason, lockedAt: device.lockedAt, usageTimeLimit: device.usageTimeLimit, usageStartTime: device.usageStartTime });
    }
  });

  // Receber atualizações de estoque dos dispositivos
  socket.on('estoque_update', async (data) => {
    console.log(`📦 [ESTOQUE] Atualização de ${data.deviceId}: produto ${data.produtoId} -> ${data.novoEstoque}`);
    
    // Atualizar estoque no banco local
    const produto = db.produtos.find(p => p.id == data.produtoId);
    if (produto) {
      const estoqueAnterior = produto.estoque;
      produto.estoque = data.novoEstoque;
      debouncedSaveData();
      
      // Broadcast para todos os dashboards
      emitDeviceEvent('estoque_atualizado', {
        produtoId: data.produtoId,
        nome: produto.nome,
        estoqueAnterior,
        novoEstoque: data.novoEstoque,
        deviceId: data.deviceId,
        timestamp: data.timestamp
      });
      
      // Broadcast para outros dispositivos
      socket.broadcast.emit('produtos_sync', {
        produtos: db.produtos,
        timestamp: new Date()
      });
      
      addAuditoria('estoque', data.deviceId, `Estoque atualizado: ${produto.nome} (${estoqueAnterior} -> ${data.novoEstoque})`, connectedDevices.get(data.deviceId)?.deviceName);
    }
  });

  // Receber operações de caixa do Android
  socket.on('operacao_data', async (data) => {
    console.log('🔓 [DEBUG] operacao_data recebido:', data);
    const { deviceId, operacao } = data;
    console.log(`💰 [OPERAÇÃO] Recebida de ${deviceId}:`, operacao);
    
    // Bloquear operações de terminais não aprovados
    const opDevice = connectedDevices.get(deviceId);
    const opDbDevice = db.dispositivos?.find(d => d.deviceId === deviceId);
    if ((!opDevice?.empresaId && !opDbDevice?.empresaId) || opDevice?.status === 'pending' || opDbDevice?.status === 'pending') {
      console.log(`❌ [OP] Terminal ${deviceId} não aprovado - operação rejeitada`);
      socket.emit('operacao_rejected', { error: 'Terminal não aprovado.' });
      return;
    }
    
    // Salvar no banco local
    if (!db.operacoes) db.operacoes = [];
    const deviceInfo = connectedDevices.get(deviceId);
    db.operacoes.push({
      ...operacao,
      deviceId,
      timestamp: Date.now(),
      empresaId: operacao.empresaId || deviceInfo?.empresaId || null
    });
    debouncedSaveData();
    
    // Broadcast para dashboards da empresa
    emitToEmpresa('operacao_adicionada', { ...operacao, deviceId }, operacao.empresaId);
    
    // Auditoria
    addAuditoria('operacao_caixa', deviceId, `${operacao.tipo}: R$ ${operacao.valor}`, operacao.nomeOperador);
  });

  // Receber dados de venda do Android
  socket.on('sale_data', async (data) => {
    const { deviceId, sale } = data;
    console.log('💰 Venda recebida do dispositivo:', deviceId);
    
    // Bloquear vendas de terminais não aprovados
    const saleDevice = connectedDevices.get(deviceId);
    const saleDbDevice = db.dispositivos?.find(d => d.deviceId === deviceId);
    if ((!saleDevice?.empresaId && !saleDbDevice?.empresaId) || saleDevice?.status === 'pending' || saleDbDevice?.status === 'pending') {
      console.log(`❌ [SALE] Terminal ${deviceId} não aprovado - venda rejeitada via WS`);
      socket.emit('sale_rejected', { error: 'Terminal não aprovado. Aguarde aprovação do administrador.' });
      return;
    }
    
    // Verificar se dispositivo estava bloqueado e agora está ativo (desbloqueado automaticamente)
    const device = connectedDevices.get(deviceId);
    if (device && device.status === 'locked') {
      console.log(`🔓 [AUTO-UNLOCK] Dispositivo ${deviceId} detectado como ativo via venda - desbloqueando automaticamente`);
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      delete device.usageTimeLimit;
      delete device.usageStartTime;
      
      // Auditoria: Desbloqueio automático detectado
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado automaticamente (atividade detectada)');
      
      // Notificar dashboards sobre desbloqueio
      emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
    }
    
    // Salvar venda no banco de dados
    const venda = {
      id: generateId(),
      deviceId: deviceId,
      numero: sale.numero || `V${Date.now()}`,
      itens: sale.itens || [],
      subtotal: sale.subtotal || 0,
      desconto: sale.desconto || 0,
      total: sale.total || 0,
      formaPagamento: sale.formaPagamento || 'DINHEIRO',
      valorRecebido: sale.valorRecebido || 0,
      troco: sale.troco || 0,
      empresaId: device?.empresaId || null,
      createdAt: new Date()
    };
    
    db.vendas.push(venda);
    debouncedSaveData();
    
    // Emitir evento para atualizar dashboards da empresa
    emitToEmpresa('venda_added', venda, venda.empresaId);
    
    console.log('✅ Venda processada e salva:', venda.id);
  });

  // Validar senha de desbloqueio enviada pelo terminal (com rate limiting)
  const unlockAttemptCounts = new Map(); // { deviceId -> [timestamps] }
  socket.on('unlock_attempt', async (data) => {
    const { deviceId, password } = data;
    const device = connectedDevices.get(deviceId);

    // Rate limiting manual para WebSocket unlock
    const now = Date.now();
    const attempts = unlockAttemptCounts.get(deviceId) || [];
    const recentAttempts = attempts.filter(t => now - t < 5 * 60 * 1000); // 5 min window
    if (recentAttempts.length >= 10) {
      console.log(`🔒 [UNLOCK-RATE] deviceId=${deviceId} bloqueado por muitas tentativas`);
      socket.emit('unlock_response', { deviceId, success: false, message: 'Muitas tentativas. Aguarde 5 minutos.' });
      return;
    }
    recentAttempts.push(now);
    unlockAttemptCounts.set(deviceId, recentAttempts);

    console.log(`🔑 Tentativa de desbloqueio: ${sanitizeDeviceId(deviceId) || 'invalid'}`);

    if (device) {
      if (device.lockPassword === password) {
        // Senha correta - desbloquear
        console.log(`✅ Senha correta para ${deviceId} - desbloqueando`);
        device.status = 'online';
        delete device.lockReason;
        delete device.lockedAt;
        delete device.usageTimeLimit;
        delete device.usageStartTime;
        
        // Auditoria: Desbloqueio via terminal
        addAuditoria('desbloqueio', deviceId, 'Desbloqueado via terminal do dispositivo');
        
        // Responder sucesso para o dispositivo
        socket.emit('unlock_response', { deviceId, success: true, message: 'Senha correta' });
        
        // Notificar dashboards sobre desbloqueio
        emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
      } else {
        // Senha incorreta
        console.log(`❌ Senha incorreta para ${deviceId}`);
        
        // Auditoria: Tentativa de senha incorreta
        addAuditoria('mudanca_status', deviceId, 'Tentativa de desbloqueio com senha incorreta');
        
        // Responder erro para o dispositivo
        socket.emit('unlock_response', { deviceId, success: false, message: 'Senha incorreta' });
      }
    } else {
      console.log(`❌ [DEBUG] Dispositivo não encontrado para unlock_attempt: ${deviceId}`);
      socket.emit('unlock_response', { deviceId, success: false, message: 'Dispositivo não encontrado' });
    }
  });

  // Dispositivo confirmando desbloqueio via terminal (legado - manter compatibilidade)
  socket.on('unlock_confirmed', async (data) => {
    console.log('🔓 [DEBUG] Evento unlock_confirmed recebido:', data);
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    if (device) {
      console.log(`🔓 Desbloqueio confirmado pelo dispositivo: ${deviceId}`);
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      delete device.usageTimeLimit;
      delete device.usageStartTime;

      // Auditoria: Desbloqueio via terminal
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado via terminal do dispositivo');

      emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
    } else {
      console.log(`❌ [DEBUG] Dispositivo não encontrado para unlock_confirmed: ${deviceId}`);
    }
  });

  // Comando de controle do app vindo do dashboard via WebSocket
  socket.on('control_app', async (data) => {
    const { deviceId, action } = data;
    const dashboardInfo = connectedDashboards.get(socket.id);
    console.log(`🎮 [CONTROL_APP] Comando via WebSocket: ${deviceId} - ${action} de ${dashboardInfo?.usuario || 'desconhecido'}`);

    const requiresSpecialPermissions = ['restart', 'shutdown'].includes(action);

    // Se comando reiniciar/desligar, tentar via ADB primeiro
    if (requiresSpecialPermissions) {
      try {
        const result = await sendAdbCommand(action, deviceId);
        if (result.success) {
          addAuditoria('mudanca_status', deviceId, `Comando ${action} enviado via ADB`, dashboardInfo?.usuario);
          socket.emit('control_app_result', { success: true, message: `Comando ${action} enviado via ADB`, deviceId, action, method: 'adb' });
          return;
        }
      } catch (error) {
        console.error(`❌ [CONTROL_APP] Erro ADB:`, error.message);
      }
    }

    // Fallback: enviar via WebSocket para o dispositivo
    const device = connectedDevices.get(deviceId);
    if (!device) {
      console.log(`❌ [CONTROL_APP] Dispositivo ${deviceId} não encontrado`);
      socket.emit('control_app_result', { success: false, error: 'Dispositivo não encontrado ou desconectado', deviceId, action });
      return;
    }

    if (device.socketId) {
      io.to(device.socketId).emit('control_command', { action });
      console.log(`📤 [CONTROL_APP] Comando '${action}' enviado para ${device.socketId}`);
      addAuditoria('mudanca_status', deviceId, `Comando de controle enviado: ${action}`, dashboardInfo?.usuario);
      socket.emit('control_app_result', {
        success: true,
        message: requiresSpecialPermissions ? 'Comando enviado. Pode não funcionar sem permissões de Admin ou Root' : 'Comando enviado com sucesso',
        deviceId,
        action
      });
    } else {
      socket.emit('control_app_result', { success: false, error: 'Dispositivo não está conectado', deviceId, action });
    }
  });

  // Resultado de comando de controle do dispositivo
  socket.on('control_result', async (data) => {
    const { deviceId, action, success, error } = data;
    console.log(`🎮 [CONTROL_RESULT] ${deviceId} - ${action} - sucesso=${success} ${error ? `- erro: ${error}` : ''}`);

    // Encaminhar resultado para todos os dashboards conectados
    emitDeviceEvent('control_result', data);

    // Auditoria se houve erro
    if (!success && error) {
      addAuditoria('mudanca_status', deviceId, `Erro ao executar ${action}: ${error}`);
    }
  });

  // Log de comando recebido pelo dispositivo (para rastreamento)
  socket.on('control_log', async (data) => {
    const { deviceId, action, timestamp } = data;
    console.log(`📝 [CONTROL_LOG] Dispositivo ${deviceId} recebeu comando: ${action} em ${new Date(timestamp).toLocaleString('pt-BR')}`);
  });

  // Endpoint alternativo para forçar desbloqueio (se o app não enviar unlock_confirmed)
  socket.on('force_unlock', async (data) => {
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      console.log(`🔓 Forçando desbloqueio do dispositivo: ${deviceId} por ${dashboardInfo?.usuario}`);
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      
      // Auditoria: Desbloqueio forçado
      addAuditoria('desbloqueio', deviceId, 'Desbloqueio forçado via dashboard', dashboardInfo?.usuario);
      
      emitDeviceEvent('device_status_update', { deviceId, status: 'online' });
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'online';
        db.dispositivos[deviceIndex].lockReason = null;
        db.dispositivos[deviceIndex].lockedAt = null;
        debouncedSaveData();
        emitDeviceEvent('device_status_update', { deviceId, status: 'online' });
        addAuditoria('desbloqueio', deviceId, 'Desbloqueio forçado (offline)', dashboardInfo?.usuario);
      }
    }
  });

  // Definir tempo de uso para dispositivo
  socket.on('set_usage_time', async (data) => {
    const { deviceId, minutes } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      console.log(`⏱️ Definindo tempo de uso: ${deviceId} - ${minutes} minutos por ${dashboardInfo?.usuario}`);
      device.usageTimeLimit = minutes;
      device.usageStartTime = new Date();
      // Mudar status para 'in_use' quando timer está ativo
      if (device.status !== 'locked') {
        device.status = 'in_use';
      }
      
      // Enviar comando para o dispositivo
      enqueueDeviceCommand(deviceId, 'usage_time_set', { minutes, startTime: device.usageStartTime });
      if (device.socketId) {
        io.to(device.socketId).emit('usage_time_set', { minutes, startTime: device.usageStartTime });
      }
      
      // Auditoria: Tempo de uso definido
      addAuditoria('mudanca_status', deviceId, `Tempo de uso definido: ${minutes} minutos`, dashboardInfo?.usuario);
      
      emitDeviceEvent('device_status_update', { 
        deviceId, 
        status: device.status,
        usageTimeLimit: minutes, 
        usageStartTime: device.usageStartTime 
      });
    }
  });

  // Verificar tempos de uso expirados (roda a cada segundo para precisão)
  setInterval(() => {
    for (const [deviceId, device] of connectedDevices.entries()) {
      if (device.usageTimeLimit && device.usageStartTime) {
        const elapsed = Math.floor((Date.now() - new Date(device.usageStartTime).getTime()) / 1000); // segundos
        const remaining = Math.max(0, device.usageTimeLimit * 60 - elapsed); // segundos restantes (nunca negativo)
        
        // Notificar tempo restante a cada segundo (apenas se dispositivo não estiver bloqueado por tempo expirado)
        if (device.status !== 'locked' || remaining > 0) {
          emitDeviceEvent('time_update', { 
            deviceId, 
            elapsed, 
            remaining, 
            total: device.usageTimeLimit * 60 
          });
        }
        
        // Bloquear quando tempo expirar
        if (remaining <= 0 && device.status !== 'locked') {
          console.log(`⏰ Tempo expirado para ${deviceId} - Bloqueando automaticamente`);
          
          device.status = 'locked';
          device.lockReason = 'Tempo de uso expirado';
          device.lockedAt = new Date();
          
          // Notificar dispositivo
          if (device.socketId) {
            io.to(device.socketId).emit('time_expired', { reason: 'Tempo de uso expirado' });
            io.to(device.socketId).emit('device_locked', { 
              reason: 'Tempo de uso expirado', 
              lockPassword: device.lockPassword 
            });
          }
          
          // Auditoria
          addAuditoria('bloqueio', deviceId, 'Bloqueado automaticamente - tempo expirado', 'Sistema');
          
          // Notificar dashboards
          emitDeviceEvent('device_status_update', { 
            deviceId, 
            status: 'locked',
            lockReason: 'Tempo de uso expirado',
            usageTimeLimit: null,
            usageStartTime: null
          });
        }
      }
    }
  }, 5000); // Verificar a cada 5 segundos (suficiente para UX, reduz carga)

  // Endpoint para sincronizar dados com dispositivos
  socket.on('sync_data', async (data) => {
    const { deviceId, type } = data;
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    console.log(`📡 [SYNC] Dispositivo ${deviceId} solicitando sincronização de ${type}`);
    
    // Verificar se dispositivo estava bloqueado e agora está ativo (desbloqueado automaticamente)
    const device = connectedDevices.get(deviceId);
    if (device && device.status === 'locked') {
      console.log(`🔓 [AUTO-UNLOCK] Dispositivo ${deviceId} detectado como ativo via sync - desbloqueando automaticamente`);
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      delete device.usageTimeLimit;
      delete device.usageStartTime;
      
      // Auditoria: Desbloqueio automático detectado
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado automaticamente (sincronização detectada)');
      
      // Notificar dashboards sobre desbloqueio
      emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
    }
    
    if (type === 'produtos' || type === 'all') {
      // Enviar produtos para o dispositivo
      if (socket.id) {
        io.to(socket.id).emit('produtos_sync', {
          produtos: db.produtos,
          timestamp: new Date()
        });
      }
    }
    
    if (type === 'categorias' || type === 'all') {
      // Enviar categorias para o dispositivo
      if (socket.id) {
        io.to(socket.id).emit('categorias_sync', {
          categorias: db.categorias,
          timestamp: new Date()
        });
      }
    }
    
    // Auditoria
    addAuditoria('mudanca_status', deviceId, `Sincronização solicitada: ${type}`, dashboardInfo?.usuario);
  });

  socket.on('dashboard_connect', async (data) => {
    const { token } = data || {};
    let usuario = 'dashboard';
    let role = null;
    let empresaId = null;

    // Tentar identificar o usuário do dashboard
    if (token) {
      try {
        const decoded = jwt.verify(token, JWT_SECRET);
        usuario = decoded.username;
        role = decoded.role;
        empresaId = decoded.empresaId || null;
        connectedDashboards.set(socket.id, { usuario, socketId: socket.id, role, empresaId });
      } catch (e) {
        console.log('Token inválido no dashboard_connect');
      }
    }

    console.log(`🖥️ Dashboard conectado: ${usuario} (role=${role}, empresaId=${empresaId})`);

    // Enviar dispositivos conectados (WebSocket ou polling recente)
    const now = new Date();
    const seenIds = new Set();
    let list = Array.from(connectedDevices.entries())
      .filter(([id]) => !BLOCKED_DEVICE_IDS.includes(id))
      .map(([id, d]) => {
        seenIds.add(id);
        const isPollingRecent = d.lastPoll && (now - new Date(d.lastPoll)) < 120000; // 2 min
        const isOnline = d.socketId !== null || isPollingRecent;
        return { deviceId: id, ...d, online: isOnline };
      });
    if (db.dispositivos && db.dispositivos.length > 0) {
      db.dispositivos.forEach(d => {
        if (!seenIds.has(d.deviceId) && !BLOCKED_DEVICE_IDS.includes(d.deviceId)) {
          list.push(d);
          seenIds.add(d.deviceId);
        }
      });
    }

    // Filtrar dispositivos por empresa
    if (role === 'empresa' && empresaId) {
      list = list.filter(d => d.empresaId === empresaId);
    }

    console.log(`📊 [DEBUG] Dispositivos conectados: ${list.length}`);
    console.log(`📊 [DEBUG] DeviceIds:`, list.map(d => d.deviceId));

    socket.emit('devices_list', list);

    // Enviar vendas recentes (últimas 50) - empresa vê tudo
    let recentVendas = (db.vendas || []).slice(-50).reverse();
    socket.emit('vendas_history', recentVendas);

    // Enviar operações de caixa para sincronização - empresa vê tudo
    let operacoes = db.operacoes || [];
    socket.emit('operacoes_sync', { operacoes });
  });

  socket.on('lock_device', async (data) => {
    const { deviceId, reason } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      // Gerar nova senha apenas se não existir (primeiro bloqueio)
      if (!device.lockPassword) {
        device.lockPassword = crypto.randomInt(100000, 999999).toString();
      }
      device.status = 'locked';
      device.lockReason = reason;
      device.lockedAt = new Date();
      // Cancelar timer de uso ao bloquear manualmente
      device.usageTimeLimit = null;
      device.usageStartTime = null;
      enqueueDeviceCommand(deviceId, 'device_locked', { reason, lockPassword: device.lockPassword });
      if (device.socketId) io.to(device.socketId).emit('device_locked', { reason, lockPassword: device.lockPassword });
      emitDeviceEvent('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, lockPassword: device.lockPassword, usageTimeLimit: null, usageStartTime: null });
      
      // Auditoria: Bloqueio via dashboard
      addAuditoria('bloqueio', deviceId, `Bloqueado: ${reason}`, dashboardInfo?.usuario);
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'locked';
        db.dispositivos[deviceIndex].lockReason = reason;
        db.dispositivos[deviceIndex].lockedAt = new Date();
        debouncedSaveData();
        emitDeviceEvent('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, usageTimeLimit: null, usageStartTime: null });
        addAuditoria('bloqueio', deviceId, `Bloqueado (offline): ${reason}`, dashboardInfo?.usuario);
      }
    }
  });

  socket.on('unlock_device', async (data) => {
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      device.status = 'online';
      // Limpar timer de uso ao desbloquear
      device.usageTimeLimit = null;
      device.usageStartTime = null;
      delete device.lockReason;
      delete device.lockedAt;
      enqueueDeviceCommand(deviceId, 'device_unlocked', {});
      if (device.socketId) io.to(device.socketId).emit('device_unlocked', {});
      emitDeviceEvent('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
      
      // Auditoria: Desbloqueio via dashboard
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado via dashboard', dashboardInfo?.usuario);
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'online';
        db.dispositivos[deviceIndex].lockReason = null;
        db.dispositivos[deviceIndex].lockedAt = null;
        debouncedSaveData();
        emitDeviceEvent('device_status_update', { deviceId, status: 'online' });
        addAuditoria('desbloqueio', deviceId, 'Desbloqueado (offline)', dashboardInfo?.usuario);
      }
    }
  });

  socket.on('disconnect', () => {
    // Verificar se é um dashboard desconectando
    if (connectedDashboards.has(socket.id)) {
      connectedDashboards.delete(socket.id);
      return;
    }
    
    // Verificar se é um dispositivo desconectando
    for (const [id, d] of connectedDevices.entries()) {
      if (d.socketId === socket.id) {
        // Auditoria: Desconexão
        addAuditoria('desconexao', id, `Dispositivo desconectado`);
        
        // Remover completamente do mapa para limpar dashboard
        connectedDevices.delete(id);
        emitDeviceEvent('device_disconnected', { deviceId: id });
        console.log(`📱 Dispositivo ${id} removido do mapa de conectados`);
        break;
      }
    }
  });
});

// ==================== SPA CATCH-ALL ====================
// Servir arquivos estáticos do frontend (após build) - apenas se existir
const frontendDist = path.join(__dirname, 'frontend/dist');
if (fs.existsSync(frontendDist)) {
  app.use(express.static(frontendDist));
  app.get('*', (req, res) => {
    if (req.path.startsWith('/api') || req.path.startsWith('/uploads')) {
      return res.status(404).json({ error: 'Not found' });
    }
    res.sendFile(path.join(frontendDist, 'index.html'));
  });
} else {
  app.get('*', (req, res) => {
    if (req.path.startsWith('/api') || req.path.startsWith('/uploads')) {
      return res.status(404).json({ error: 'Not found' });
    }
    res.json({ message: 'CaixaCombo API', docs: '/api/health' });
  });
}

const PORT = process.env.PORT || 3001;

// Vercel serverless: inicializar sob demanda e exportar app
let initialized = false;
async function ensureInitialized() {
  if (!initialized) {
    try {
      await initializeApp();
    } catch (e) {
      console.error('⚠️ Erro na inicialização:', e.message);
    }
    initialized = true;
  }
}

// Middleware para inicializar sob demanda (Vercel serverless)
app.use(async (req, res, next) => {
  if (!initialized) {
    await ensureInitialized();
  }
  next();
});

// Modo standalone (Render, local, etc)
if (!process.env.VERCEL) {
  initializeApp().then(() => {
    server.listen(PORT, '0.0.0.0', () => {
      console.log(`🚀 Server na porta ${PORT}`);
    });
  }).catch(err => {
    console.error('❌ Falha na inicialização:', err);
    server.listen(PORT, '0.0.0.0', () => {
      console.log(`🚀 Server na porta ${PORT} (modo offline)`);
    });
  });
}

// Exportar para Vercel serverless
module.exports = app;
