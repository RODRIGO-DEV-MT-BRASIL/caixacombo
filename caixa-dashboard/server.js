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
require('dotenv').config();

const app = express();

const allowedOrigins = (process.env.CORS_ORIGINS || '').split(',').filter(Boolean);
app.use(cors({ 
  origin: allowedOrigins.length > 0 ? allowedOrigins : true, 
  credentials: true 
}));
app.use(express.json({ limit: '10mb' }));

// Servir arquivos estáticos da pasta uploads
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Servir arquivos estáticos do frontend (após build)
app.use(express.static(path.join(__dirname, 'frontend/dist')));

// Criar pasta uploads se não existir
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

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

// ==================== PERSISTÊNCIA DE DADOS (MongoDB) ====================
const { db, saveData, saveAuditoria, connectMongo } = require('./database-mongo');

// Debounce para saveData - evita múltiplos bulkWrites seguidos
let saveDataTimer = null;
const SAVEDATA_DEBOUNCE_MS = 2000; // 2 segundos
function debouncedSaveData() {
  if (saveDataTimer) clearTimeout(saveDataTimer);
  saveDataTimer = setTimeout(async () => {
    await saveData();
    saveDataTimer = null;
  }, SAVEDATA_DEBOUNCE_MS);
}

// Seed do admin será feito após connectMongo() para verificar dados do MongoDB primeiro

// Gerador de IDs único - evita colisões quando múltiplas operações rodam no mesmo milissegundo
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
  return now * 1000 + _idCounter; // Garante unicidade mesmo no mesmo ms
}

const serverStartTime = new Date(); // Para detectar restart e forçar sync inicial
const connectedDevices = new Map();
const connectedDashboards = new Map(); // Guardar usuário do dashboard
const pendingCommands = new Map(); // Fila de comandos pendentes por deviceId (para polling REST)

// Inicialização assíncrona: conectar MongoDB e carregar dados
async function initializeApp() {
  await connectMongo();

  // Seed do admin padrão após carregar dados do MongoDB
  const adminUsername = process.env.ADMIN_USERNAME || 'admin';
  const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';
  const existingAdmin = (db.usuarios || []).find(u => u.username === adminUsername);
  if (!existingAdmin) {
    if (!db.usuarios) db.usuarios = [];
    db.usuarios.push({
      id: generateId(),
      username: adminUsername,
      password: bcrypt.hashSync(adminPassword, 10),
      role: 'admin'
    });
    await saveData();
    console.log(`👤 Admin criado: ${adminUsername}`);
  } else if (!bcrypt.compareSync(adminPassword, existingAdmin.password)) {
    // Admin existe mas a senha não confere com a env var - atualizar
    existingAdmin.password = bcrypt.hashSync(adminPassword, 10);
    await saveData();
    console.log(`👤 Admin "${adminUsername}" atualizado com nova senha`);
  } else {
    console.log(`👤 Admin "${adminUsername}" já existe com senha correta`);
  }

  // Limpar imagens de produtos que apontam para /uploads/ (filesystem efêmero do Render)
  let imagensLimpas = 0;
  if (db.produtos && db.produtos.length > 0) {
    db.produtos.forEach(p => {
      if (p.imagem && p.imagem.startsWith('/uploads/')) {
        p.imagem = null;
        imagensLimpas++;
      }
    });
    if (imagensLimpas > 0) {
      await saveData();
      console.log(`🧹 ${imagensLimpas} imagens /uploads/ limpas (arquivos não existem mais no Render)`);
    }
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
  const log = {
    id: generateId(),
    timestamp: new Date(),
    tipo, // 'conexao', 'desconexao', 'bloqueio', 'desbloqueio', 'mudanca_status'
    deviceId,
    deviceName: connectedDevices.get(deviceId)?.deviceName || deviceId,
    detalhes,
    usuario: usuario || (connectedDevices.get(deviceId)?.deviceName || 'Sistema'),
    ip: null // Poderia ser extraído do socket se necessário
  };
  
  db.auditoria.unshift(log); // Adicionar no início (mais recente primeiro)
  
  // Manter apenas os últimos 1000 logs para não sobrecarregar memória
  if (db.auditoria.length > 1000) {
    db.auditoria = db.auditoria.slice(0, 1000);
  }
  
  // Salvar auditoria em disco
  saveAuditoria();
  
  // Notificar dashboards sobre novo log
  io.emit('auditoria_update', log);
  
  console.log(`[AUDITORIA] ${tipo.toUpperCase()}: ${deviceId} - ${detalhes}`);
}

// Salvar dados periodicamente a cada 30 segundos
setInterval(async () => {
  await saveData();
  if (isConnected()) console.log('💾 Dados salvos no MongoDB');
  else console.log('⚠️ MongoDB desconectado - dados apenas em memória');
}, 30000);

// ==================== CONTROLE VIA ADB ====================
const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);

/**
 * Envia comando ADB diretamente ao dispositivo para reiniciar ou desligar
 * @param {string} action - 'reboot' ou 'shutdown'
 * @param {string} deviceId - Serial number do dispositivo (mesmo que o ADB usa)
 */
async function sendAdbCommand(action, deviceId) {
  console.log(`🔌 [ADB] Enviando comando ${action} para ${deviceId}`);

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
    const isConnected = stdout.includes(deviceId);

    if (!isConnected) {
      console.log(`❌ [ADB] Dispositivo ${deviceId} não está conectado via ADB`);
      return { success: false, error: 'Dispositivo não está conectado via USB/WiFi ADB' };
    }

    // Enviar comando
    const adbCommand = action === 'shutdown'
      ? `adb -s ${deviceId} shell reboot -p`
      : `adb -s ${deviceId} shell reboot`;

    console.log(`🔌 [ADB] Executando: ${adbCommand}`);

    await execWithTimeout(adbCommand);
    console.log(`✅ [ADB] Comando ${action} enviado com sucesso para ${deviceId}`);

    return { success: true, method: 'adb' };
  } catch (error) {
    console.error(`❌ [ADB] Erro:`, error.message);
    return { success: false, error: error.message };
  }
}

// Salvar dados ao encerrar servidor
process.on('SIGINT', async () => {
  console.log('\n🔄 Salvando dados antes de encerrar...');
  await saveData();
  saveAuditoria();
  process.exit(0);
});

// ==================== CRIAR SERVER E IO ====================
const server = http.createServer(app);
const io = new Server(server, {
  cors: { 
    origin: process.env.NODE_ENV === 'production' ? '*' : true,
    credentials: true 
  },
  transports: ["websocket", "polling"],
  pingTimeout: 60000,
  pingInterval: 25000,
  allowEIO3: true
});

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  console.error('❌ JWT_SECRET não definido! Defina a variável de ambiente JWT_SECRET.');
  process.exit(1);
}

function authenticateToken(req, res, next) {
  const token = req.headers['authorization']?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Não autorizado' });
  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Token inválido' });
    req.user = user;
    next();
  });
}

// ==================== ROTAS API ====================
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;
  console.log(`[LOGIN] Tentativa: username="${username}", usuarios no db=${db.usuarios.length}, empresas no db=${(db.empresas || []).length}`);
  
  // Verificar se é usuário do sistema
  let user = db.usuarios.find(u => u.username === username);
  console.log(`[LOGIN] Usuário encontrado: ${user ? `id=${user.id}, role=${user.role}` : 'NENHUM'}`);
  
  // Se não for usuário do sistema, verificar se é empresa
  if (!user) {
    const empresa = (db.empresas || []).find(e => e.login === username);
    console.log(`[LOGIN] Empresa encontrada: ${empresa ? `id=${empresa.id}, login=${empresa.login}, temSenha=${!!empresa.senha}` : 'NENHUMA'}`);
    if (empresa) {
      if (!empresa.senha || !bcrypt.compareSync(password, empresa.senha)) {
        console.log(`[LOGIN] Senha da empresa não confere ou senha ausente`);
        return res.status(401).json({ error: 'Credenciais inválidas' });
      }
      
      const token = jwt.sign(
        { id: empresa.id, username: empresa.login, role: 'empresa', empresaId: empresa.id, permissoes: empresa.permissoes },
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
          permissoes: empresa.permissoes
        }
      });
    }
  }
  
  if (!user) return res.status(401).json({ error: 'Credenciais inválidas' });
  
  if (!bcrypt.compareSync(password, user.password)) {
    console.log(`[LOGIN] Senha do usuário não confere para "${username}"`);
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }
  
  const token = jwt.sign({ id: user.id, username: user.username, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
  res.json({ token, user: { id: user.id, username: user.username, role: user.role } });
});

app.get('/api/auth/verify', authenticateToken, (req, res) => {
  res.json({ valid: true, user: req.user });
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
  
  // Notificar dashboards
  io.emit('venda_cancelada', { vendaId: id, deviceId });
  
  addAuditoria('cancelamento', deviceId, `Venda #${id} cancelada: ${motivo || 'sem motivo'}${atk ? ' (atk: ' + atk + ')' : ''}`, req.user.username);
  res.json({ success: true, message: 'Venda cancelada com sucesso' });
});

app.get('/api/dispositivos', authenticateToken, (req, res) => {
  const list = Array.from(connectedDevices.entries())
    .filter(([id]) => !BLOCKED_DEVICE_IDS.includes(id))
    .map(([id, d]) => ({
      deviceId: id, ...d, online: d.socketId !== null
    }));
  res.json(list);
});

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

app.get('/api/auditoria', authenticateToken, (req, res) => {
  const { limit = 50, tipo, deviceId } = req.query;
  let logs = db.auditoria;
  
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
  res.json(db.categorias);
});

app.post('/api/categorias', authenticateToken, async (req, res) => {
  const categoria = {
    id: generateId(),
    nome: req.body.nome,
    descricao: req.body.descricao,
    cor: req.body.cor || null,
    icone: req.body.icone || null,
    ordem: req.body.ordem || 0,
    ativa: req.body.ativa !== false,
    createdAt: new Date()
  };
  db.categorias.push(categoria);
  debouncedSaveData(); // Salvar imediatamente
  io.emit('categoria_added', categoria);
  broadcastCategoriasSync('added', categoria);
  
  res.json(categoria);
});

app.put('/api/categorias/:id', authenticateToken, async (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  
  db.categorias[index] = { ...db.categorias[index], ...req.body };
  debouncedSaveData(); // Salvar imediatamente
  io.emit('categoria_updated', db.categorias[index]);
  broadcastCategoriasSync('updated', db.categorias[index]);
  
  res.json(db.categorias[index]);
});

app.delete('/api/categorias/:id', authenticateToken, async (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  
  const deleted = db.categorias.splice(index, 1)[0];
  debouncedSaveData(); // Salvar imediatamente
  io.emit('categoria_deleted', deleted);
  broadcastCategoriasSync('deleted', deleted);
  
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
  if (!limit && !offset) return res.json(db.produtos); // Retrocompatível: sem paginação retorna array
  let result = db.produtos;
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total: db.produtos.length });
});

// Rota para buscar vendas
app.get('/api/vendas', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  if (!limit && !offset) return res.json(db.vendas); // Retrocompatível: sem paginação retorna array
  let result = db.vendas;
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total: db.vendas.length });
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
    createdAt: new Date()
  };
  db.produtos.push(produto);
  debouncedSaveData(); // Salvar imediatamente
  io.emit('produto_added', produto);
  broadcastProdutosSync('added', produto);
  
  res.json(produto);
});

app.put('/api/produtos/:id', authenticateToken, async (req, res) => {
  const index = db.produtos.findIndex(p => p.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });
  
  const updateData = { ...req.body };
  if (updateData.categoriaId !== undefined) {
    updateData.categoriaId = updateData.categoriaId ? Number(updateData.categoriaId) : null;
  }
  db.produtos[index] = { ...db.produtos[index], ...updateData };
  debouncedSaveData(); // Salvar imediatamente
  io.emit('produto_updated', db.produtos[index]);
  broadcastProdutosSync('updated', db.produtos[index]);
  
  res.json(db.produtos[index]);
});

app.delete('/api/produtos/:id', authenticateToken, async (req, res) => {
  const index = db.produtos.findIndex(p => p.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });
  
  const deleted = db.produtos.splice(index, 1)[0];
  
  // Remover imagem associada se existir
  if (deleted.imagem) {
    const imagePath = path.join(__dirname, 'uploads', path.basename(deleted.imagem));
    if (fs.existsSync(imagePath)) {
      fs.unlinkSync(imagePath);
    }
  }
  
  debouncedSaveData(); // Salvar imediatamente
  io.emit('produto_deleted', deleted);
  broadcastProdutosSync('deleted', deleted);
  
  res.json(deleted);
});

// ==================== ROTAS DE EMPRESAS ====================
app.get('/api/empresas', authenticateToken, (req, res) => {
  res.json(db.empresas || []);
});

app.post('/api/empresas', authenticateToken, async (req, res) => {
  const { nome, cnpj, email, telefone, login, senha, permissoes } = req.body;
  
  if (!nome || !login || !senha) {
    return res.status(400).json({ error: 'Nome, login e senha são obrigatórios' });
  }
  
  // Verificar se login já existe
  const existingLogin = (db.empresas || []).find(e => e.login === login);
  if (existingLogin) {
    return res.status(400).json({ error: 'Login já existe' });
  }
  
  const empresa = {
    id: generateId(),
    nome,
    cnpj: cnpj || null,
    email: email || null,
    telefone: telefone || null,
    login,
    senha: bcrypt.hashSync(senha, 10),
    permissoes: permissoes || {
      dashboard: false,
      produtos: false,
      categorias: false,
      vendas: false,
      caixa: false,
      auditoria: false
    },
    createdAt: new Date()
  };
  
  if (!db.empresas) db.empresas = [];
  db.empresas.push(empresa);
  debouncedSaveData();
  
  // Sincronizar empresa com os terminais
  broadcastEmpresasSync();
  
  res.json(empresa);
});

app.put('/api/empresas/:id', authenticateToken, async (req, res) => {
  const index = (db.empresas || []).findIndex(e => e.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Empresa não encontrada' });
  
  const { nome, cnpj, email, telefone, login, senha, permissoes } = req.body;
  
  // Verificar se login já existe (excluindo a empresa atual)
  const existingLogin = (db.empresas || []).find(e => e.login === login && e.id != req.params.id);
  if (existingLogin) {
    return res.status(400).json({ error: 'Login já existe' });
  }
  
  db.empresas[index] = {
    ...db.empresas[index],
    nome: nome || db.empresas[index].nome,
    cnpj: cnpj !== undefined ? cnpj : db.empresas[index].cnpj,
    email: email !== undefined ? email : db.empresas[index].email,
    telefone: telefone !== undefined ? telefone : db.empresas[index].telefone,
    login: login || db.empresas[index].login,
    senha: senha ? bcrypt.hashSync(senha, 10) : db.empresas[index].senha,
    permissoes: permissoes || db.empresas[index].permissoes,
    updatedAt: new Date()
  };
  
  debouncedSaveData();
  broadcastEmpresasSync();
  res.json(db.empresas[index]);
});

app.delete('/api/empresas/:id', authenticateToken, async (req, res) => {
  const index = (db.empresas || []).findIndex(e => e.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Empresa não encontrada' });
  
  const deleted = db.empresas.splice(index, 1)[0];
  debouncedSaveData();
  broadcastEmpresasSync();
  res.json(deleted);
});

// ==================== ROTAS DE CLIENTES ====================
app.get('/api/clientes', authenticateToken, (req, res) => {
  const { limit, offset } = req.query;
  if (!limit && !offset) return res.json(db.clientes || []); // Retrocompatível: sem paginação retorna array
  let result = db.clientes || [];
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total: (db.clientes || []).length });
});

app.post('/api/clientes', authenticateToken, async (req, res) => {
  const { nome, cpfCnpj, telefone, email, endereco, cidade, cep, observacao } = req.body;
  
  if (!nome) {
    return res.status(400).json({ error: 'Nome é obrigatório' });
  }
  
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
    dataCriacao: Date.now()
  };
  
  if (!db.clientes) db.clientes = [];
  db.clientes.push(cliente);
  debouncedSaveData();
  
  // Notificar terminais via WebSocket e polling
  broadcastClientesSync();
  
  res.json(cliente);
});

app.put('/api/clientes/:id', authenticateToken, async (req, res) => {
  const index = (db.clientes || []).findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Cliente não encontrado' });
  
  db.clientes[index] = { ...db.clientes[index], ...req.body, id: db.clientes[index].id };
  debouncedSaveData();
  
  broadcastClientesSync();
  
  res.json(db.clientes[index]);
});

app.delete('/api/clientes/:id', authenticateToken, async (req, res) => {
  const index = (db.clientes || []).findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Cliente não encontrado' });
  
  const deleted = db.clientes.splice(index, 1)[0];
  debouncedSaveData();
  
  broadcastClientesSync();
  
  res.json(deleted);
});

// Função auxiliar: sincronizar clientes para todos os terminais
function broadcastClientesSync() {
  const clientes = db.clientes || [];
  // Via WebSocket para dashboards
  io.emit('clientes_sync', clientes);
  // Via polling para terminais Android
  connectedDevices.forEach((deviceInfo, deviceId) => {
    enqueueDeviceCommand(deviceId, 'clientes_sync', { clientes });
  });
}

// Função auxiliar: sincronizar produtos para todos os terminais
function broadcastProdutosSync(action = 'sync', data = null) {
  const produtos = db.produtos || [];
  const payload = { produtos, timestamp: new Date(), action };
  if (data) payload.data = data;
  // Via WebSocket para dashboards
  io.emit('produtos_sync', payload);
  // Via polling para terminais Android
  const deviceIds = [];
  connectedDevices.forEach((deviceInfo, deviceId) => {
    enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos });
    deviceIds.push(deviceId);
  });
  console.log(`📤 [BROADCAST-PRODUTOS] ${produtos.length} produtos para ${deviceIds.length} dispositivos: ${deviceIds.join(', ')}`);
}

// Função auxiliar: sincronizar categorias para todos os terminais
function broadcastCategoriasSync(action = 'sync', data = null) {
  const categorias = db.categorias || [];
  const payload = { categorias, timestamp: new Date(), action };
  if (data) payload.data = data;
  // Via WebSocket para dashboards
  io.emit('categorias_sync', payload);
  // Via polling para terminais Android
  const deviceIds = [];
  connectedDevices.forEach((deviceInfo, deviceId) => {
    enqueueDeviceCommand(deviceId, 'categorias_sync', { categorias });
    deviceIds.push(deviceId);
  });
  console.log(`📤 [BROADCAST-CATEGORIAS] ${categorias.length} categorias para ${deviceIds.length} dispositivos: ${deviceIds.join(', ')}`);
}

// Função auxiliar: sincronizar empresas para todos os terminais
function broadcastEmpresasSync() {
  const empresas = (db.empresas || []).map(e => ({
    id: e.id,
    nome: e.nome,
    cnpj: e.cnpj,
    email: e.email,
    telefone: e.telefone,
    permissoes: e.permissoes
  }));
  // Via WebSocket para dashboards
  io.emit('empresas_sync', { empresas });
  // Via polling para terminais Android
  const deviceIds = [];
  connectedDevices.forEach((deviceInfo, deviceId) => {
    enqueueDeviceCommand(deviceId, 'empresas_sync', { empresas });
    deviceIds.push(deviceId);
  });
  console.log(`📤 [BROADCAST-EMPRESAS] ${empresas.length} empresas para ${deviceIds.length} dispositivos: ${deviceIds.join(', ')}`);
}

// Rota para associar dispositivo a empresa
app.put('/api/dispositivos/:deviceId/empresa', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const { empresaId } = req.body;
  const dashboardInfo = connectedDashboards.get(req.socket?.id);

  // Verificar se é admin
  if (req.user?.role !== 'admin') {
    return res.status(403).json({ error: 'Apenas admin pode associar dispositivos a empresas' });
  }

  const device = connectedDevices.get(deviceId);
  if (device) {
    device.empresaId = empresaId;
  }

  // Atualizar no banco de dados
  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  if (deviceIndex !== -1) {
    db.dispositivos[deviceIndex].empresaId = empresaId;
    debouncedSaveData();
  }

  io.emit('device_status_update', { deviceId, empresaId });

  // Auditoria
  addAuditoria('mudanca_status', deviceId, `Dispositivo associado à empresa ${empresaId}`, dashboardInfo?.usuario);

  res.json({ success: true, empresaId });
});

// ==================== ROTAS DE DISPOSITIVOS ====================
app.put('/api/dispositivos/:deviceId/password', authenticateToken, async (req, res) => {
  const { deviceId } = req.params;
  const dashboardInfo = connectedDashboards.get(req.socket?.id);

  console.log(`🔑 [DEBUG] Solicitação para mudar senha do dispositivo: ${deviceId}`);

  // Encontrar dispositivo nos dados persistidos
  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  if (deviceIndex === -1) {
    console.log(`❌ [DEBUG] Dispositivo não encontrado: ${deviceId}`);
    return res.status(404).json({ error: 'Dispositivo não encontrado' });
  }

  // Gerar nova senha de 6 dígitos
  const newPassword = Math.floor(100000 + Math.random() * 900000).toString();

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
  io.emit('device_password_updated', { deviceId, lockPassword: newPassword });

  // Auditoria
  addAuditoria('mudanca_status', deviceId, 'Senha de bloqueio atualizada', dashboardInfo?.usuario);

  console.log(`🔑 Nova senha gerada para ${deviceId}: ${newPassword}`);

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
  // Retornar operações do banco local (do dashboard)
  const { limit, offset } = req.query;
  if (!limit && !offset) return res.json(db.operacoes || []); // Retrocompatível: sem paginação retorna array
  let result = db.operacoes || [];
  if (offset) result = result.slice(Number(offset));
  if (limit) result = result.slice(0, Number(limit));
  res.json({ data: result, total: (db.operacoes || []).length });
});

app.post('/api/operacoes', authenticateToken, async (req, res) => {
  const { tipo, valor, deviceId, nomeOperador, observacao } = req.body;
  
  const operacao = {
    id: generateId(),
    tipo, // 'abertura', 'fechamento', 'suprimento', 'sangria'
    valor: parseFloat(valor) || 0,
    deviceId,
    nomeOperador: nomeOperador || 'dashboard',
    observacao: observacao || '',
    dataHora: new Date().toISOString(),
    timestamp: Date.now()
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
    }
  }
  
  debouncedSaveData();
  
  // Broadcast para dashboards
  io.emit('operacao_adicionada', operacao);
  
  // Broadcast para dispositivos
  io.emit('operacoes_sync', {
    operacoes: db.operacoes,
  });

  // Se fechamento, sincronizar vendas também (foram removidas do banco ativo)
  if (tipo === 'fechamento') {
    io.emit('vendas_sync', db.vendas || []);
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
  
  const operacaoRemovida = db.operacoes.splice(index, 1)[0];
  debouncedSaveData();
  
  // Broadcast para dashboards
  io.emit('operacao_removida', { id: id });
  
  // Auditoria
  addAuditoria('operacao_caixa', 'dashboard', `Operação removida: ${operacaoRemovida.tipo} R$ ${operacaoRemovida.valor}`, 'dashboard');
  
  res.json({ message: 'Operação removida com sucesso', operacao: operacaoRemovida });
});

// ==================== HISTÓRICO DE CAIXA E FATURAMENTO ====================
app.get('/api/caixa-sessoes', authenticateToken, (req, res) => {
  res.json(db.caixaSessoes || []);
});

app.get('/api/faturamento', authenticateToken, (req, res) => {
  const periodo = req.query.periodo || 'diario'; // diario, semanal, mensal, anual
  const sessoes = db.caixaSessoes || [];
  const vendas = db.vendas || [];
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

// ==================== POLLING REST API (Stone Compliance - sem WebSocket no POS) ====================

// Blocklist de deviceIds de teste
const BLOCKED_DEVICE_IDS = ['test-check', 'test-local', 'test-render', 'deploy-check'];

// Dispositivo faz heartbeat e recebe comandos pendentes
app.post('/api/device/poll', async (req, res) => {
  const { deviceId, deviceName, deviceType, serialNumber, status, caixaData } = req.body;
  
  if (!deviceId) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }

  // Bloquear dispositivos de teste
  if (BLOCKED_DEVICE_IDS.includes(deviceId)) {
    return res.status(403).json({ error: 'Dispositivo bloqueado' });
  }

  // Registrar/atualizar dispositivo no mapa
  const existing = connectedDevices.get(deviceId);
  // Preservar senha existente (do mapa ou do banco) - NÃO gerar nova senha no poll
  const existingDb = db.dispositivos?.find(d => d.deviceId === deviceId);
  const lockPassword = existing?.lockPassword || existingDb?.lockPassword || null;

  connectedDevices.set(deviceId, {
    socketId: existing?.socketId || null,
    deviceName: deviceName || existing?.deviceName || 'Dispositivo',
    deviceType: deviceType || 'Android',
    serialNumber: serialNumber || existing?.serialNumber || null,
    status: status || existing?.status || 'online',
    lockPassword: lockPassword,
    lastPoll: new Date(),
    empresaId: existing?.empresaId || null,
    usageTimeLimit: existing?.usageTimeLimit || null,
    usageStartTime: existing?.usageStartTime || null
  });

  // Salvar dispositivo no banco
  if (!db.dispositivos) db.dispositivos = [];
  const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
  const deviceData = {
    deviceId,
    deviceName: deviceName || existing?.deviceName || 'Dispositivo',
    deviceType: deviceType || 'Android',
    serialNumber: serialNumber || existing?.serialNumber || null,
    status: status || 'online',
    lockPassword: lockPassword,
    lastPoll: new Date()
  };
  if (deviceIndex === -1) {
    db.dispositivos.push(deviceData);
  } else {
    db.dispositivos[deviceIndex] = { ...db.dispositivos[deviceIndex], ...deviceData };
  }
  debouncedSaveData();

  // Detectar mudança de status e notificar dashboards
  const newStatus = status || existing?.status || 'online';
  if (existing && existing.status !== newStatus) {
    io.emit('device_status_update', {
      deviceId,
      status: newStatus,
      lockReason: connectedDevices.get(deviceId)?.lockReason,
      lockedAt: connectedDevices.get(deviceId)?.lockedAt,
      usageTimeLimit: connectedDevices.get(deviceId)?.usageTimeLimit,
      usageStartTime: connectedDevices.get(deviceId)?.usageStartTime
    });
  }

  // Processar dados de caixa se enviado
  if (caixaData) {
    io.emit('caixa_data', { deviceId, caixa: caixaData });
  }

  // Notificar dashboards sobre o estado do dispositivo
  const now = new Date();
  const isPollingRecent = connectedDevices.get(deviceId)?.lastPoll && (now - new Date(connectedDevices.get(deviceId).lastPoll)) < 120000;
  io.emit('device_connected', { deviceId, ...connectedDevices.get(deviceId), online: true });

  // Se dispositivo é novo, reconectando após cold start, ou não sincronizou desde o restart do servidor
  // (equivalente ao que o WebSocket faz em device_connect com socket.emit('produtos_sync'))
  const deviceLastPoll = existing?.lastPoll ? new Date(existing.lastPoll) : null;
  const needsInitialSync = !existing || !existing.lastPoll || (deviceLastPoll && deviceLastPoll < serverStartTime);
  const appRequestedSync = req.body.needsProductSync === true;
  
  // Sempre enviar sync se o servidor reiniciou recentemente (últimos 10 minutos) 
  // e o dispositivo ainda não sincronizou com esta instância
  const serverAgeMs = Date.now() - serverStartTime.getTime();
  const serverJustStarted = serverAgeMs < 600000; // 10 minutos
  const forceSyncOnRestart = serverJustStarted && (!deviceLastPoll || deviceLastPoll < serverStartTime);
  
  if (needsInitialSync || appRequestedSync || forceSyncOnRestart) {
    const reason = !existing ? 'novo' : !existing.lastPoll ? 'sem lastPoll' : appRequestedSync ? 'app solicitou' : forceSyncOnRestart ? 'force-restart' : 'pós-restart servidor';
    console.log(`📦 [POLL-SYNC] Dispositivo ${deviceId} (${reason}) - enviando sync: ${db.produtos.length} produtos, ${db.categorias.length} categorias, ${db.clientes.length} clientes`);
    enqueueDeviceCommand(deviceId, 'produtos_sync', { produtos: db.produtos || [] });
    if (db.categorias && db.categorias.length > 0) {
      enqueueDeviceCommand(deviceId, 'categorias_sync', { categorias: db.categorias });
    }
    if (db.clientes && db.clientes.length > 0) {
      enqueueDeviceCommand(deviceId, 'clientes_sync', { clientes: db.clientes });
    }
  }

  // Retornar comandos pendentes para o dispositivo
  const commands = pendingCommands.get(deviceId) || [];
  pendingCommands.delete(deviceId); // Limpar após enviar
  
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
app.post('/api/device/sale', async (req, res) => {
  const { deviceId, sale } = req.body;
  
  console.log(`💰 [SALE] Venda recebida via REST - deviceId: ${deviceId}, sale.id: ${sale?.id}, total: ${sale?.total}, forma: ${sale?.formaPagamento}`);
  
  if (!deviceId || !sale) {
    console.log(`❌ [SALE] Dados inválidos - deviceId: ${deviceId}, sale: ${!!sale}`);
    return res.status(400).json({ error: 'deviceId e sale obrigatórios' });
  }

  // Processar venda igual ao WebSocket
  if (!db.vendas) db.vendas = [];
  
  // Adicionar deviceId e createdAt se não existirem
  const deviceInfo = connectedDevices.get(deviceId);
  const enrichedSale = {
    ...sale,
    deviceId: sale.deviceId || deviceId,
    deviceName: sale.deviceName || deviceInfo?.deviceName || deviceId,
    createdAt: sale.createdAt || new Date().toISOString()
  };
  
  const existingIndex = db.vendas.findIndex(v => v.id === enrichedSale.id);
  if (existingIndex === -1) {
    db.vendas.push(enrichedSale);
  } else {
    db.vendas[existingIndex] = enrichedSale;
  }
  debouncedSaveData();

  // Notificar dashboards
  io.emit('venda_added', enrichedSale);

  // Auto-unlock se dispositivo estava bloqueado
  const device = connectedDevices.get(deviceId);
  if (device && device.status === 'locked') {
    device.status = 'online';
  }

  res.json({ success: true });
});

// Enviar operação de caixa via REST (sem autenticação - usado pelos terminais Android)
app.post('/api/device/operacao', async (req, res) => {
  const { deviceId, tipo, valor, nomeOperador, observacao } = req.body;
  
  if (!deviceId || !tipo) {
    return res.status(400).json({ error: 'deviceId e tipo obrigatórios' });
  }

  const deviceInfo = connectedDevices.get(deviceId);

  const operacao = {
    id: generateId(),
    tipo,
    valor: parseFloat(valor) || 0,
    deviceId,
    nomeOperador: nomeOperador || 'terminal',
    observacao: observacao || '',
    dataHora: new Date().toISOString(),
    timestamp: Date.now()
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

  // Broadcast para todos os dashboards
  io.emit('operacao_adicionada', operacao);
  io.emit('operacoes_sync', {
    operacoes: db.operacoes
  });

  // Se fechamento, sincronizar vendas também (foram removidas do banco ativo)
  if (tipo === 'fechamento') {
    io.emit('vendas_sync', db.vendas || []);
  }

  // Auditoria
  addAuditoria('operacao_caixa', deviceId, `${tipo}: R$ ${operacao.valor.toFixed(2)}`, nomeOperador);

  console.log(`✅ Operação de caixa via REST: ${tipo} de ${deviceId} - R$ ${operacao.valor.toFixed(2)}`);

  res.json({ success: true, operacao });
});

// Enviar status do dispositivo via REST
app.post('/api/device/status', async (req, res) => {
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
    
    io.emit('device_status_update', { deviceId, status, lockReason: device.lockReason, lockedAt: device.lockedAt, usageTimeLimit: device.usageTimeLimit, usageStartTime: device.usageStartTime });
  }

  res.json({ success: true });
});

// Enviar atualização de estoque via REST
app.post('/api/device/estoque', async (req, res) => {
  const { deviceId, produtoId, novoEstoque } = req.body;
  
  if (!deviceId) {
    return res.status(400).json({ error: 'deviceId obrigatório' });
  }

  const produto = db.produtos.find(p => p.id === produtoId);
  if (produto) {
    const estoqueAnterior = produto.estoque;
    produto.estoque = novoEstoque;
    debouncedSaveData();
    io.emit('estoque_update', { deviceId, produtoId, novoEstoque });
    addAuditoria('estoque', deviceId, `Estoque atualizado: ${produto.nome} (${estoqueAnterior} -> ${novoEstoque})`, connectedDevices.get(deviceId)?.deviceName);
  }

  res.json({ success: true });
});

// Confirmar bloqueio via REST
app.post('/api/device/lock-confirmed', async (req, res) => {
  const { deviceId } = req.body;
  const device = connectedDevices.get(deviceId);
  if (device) {
    device.status = 'locked';
    io.emit('lock_confirmed', { deviceId });
    addAuditoria('bloqueio', deviceId, 'Dispositivo bloqueado com sucesso', 'Sistema');
  }
  res.json({ success: true });
});

// Confirmar desbloqueio via REST
app.post('/api/device/unlock-confirmed', async (req, res) => {
  const { deviceId } = req.body;
  const device = connectedDevices.get(deviceId);
  if (device) {
    device.status = 'online';
    io.emit('unlock_confirmed', { deviceId });
    addAuditoria('desbloqueio', deviceId, 'Dispositivo desbloqueado', 'Sistema');
  }
  res.json({ success: true });
});

// Tentativa de desbloqueio via REST
app.post('/api/device/unlock-attempt', async (req, res) => {
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
    io.emit('unlock_response', { deviceId, success: true, message: 'Desbloqueado com sucesso' });
    io.emit('device_status_update', { deviceId, status: 'online' });
    addAuditoria('desbloqueio', deviceId, 'Desbloqueio via senha', 'Terminal');
    res.json({ success: true, message: 'Desbloqueado com sucesso' });
  } else {
    addAuditoria('bloqueio', deviceId, `Tentativa de desbloqueio falhou: senha incorreta`, 'Terminal');
    res.json({ success: false, message: 'Senha incorreta' });
  }
});

// Enviar resultado de controle via REST
app.post('/api/device/control-result', async (req, res) => {
  const { deviceId, action, success, error } = req.body;
  io.emit('control_result', { deviceId, action, success, error });
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
    broadcastProdutosSync('force_sync');
    // Também enfileirar para dispositivos do banco que podem não estar no connectedDevices
    (db.dispositivos || []).forEach(d => {
      if (!connectedDevices.has(d.deviceId)) {
        enqueueDeviceCommand(d.deviceId, 'produtos_sync', { produtos: db.produtos || [] });
      }
    });
    synced.push('produtos');
  }
  if (type === 'categorias' || type === 'all') {
    broadcastCategoriasSync('force_sync');
    (db.dispositivos || []).forEach(d => {
      if (!connectedDevices.has(d.deviceId)) {
        enqueueDeviceCommand(d.deviceId, 'categorias_sync', { categorias: db.categorias || [] });
      }
    });
    synced.push('categorias');
  }
  if (type === 'clientes' || type === 'all') {
    broadcastClientesSync();
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
  io.emit('produtos_sync', { deviceId, produtos });
  res.json({ success: true });
});

// Terminal salva/edita produto sem autenticação JWT (usa deviceId)
app.post('/api/device/produto-save', async (req, res) => {
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
    io.emit('produto_updated', db.produtos[index]);
    broadcastProdutosSync('updated', db.produtos[index]);
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
    io.emit('produto_added', novo);
    broadcastProdutosSync('added', novo);
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
      io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
    }

    // Auditoria: Conexão
    addAuditoria('conexao', deviceId, `Dispositivo conectado - ${deviceType} (${serialNumber})`);

    if (existing && existing.socketId && existing.socketId !== socket.id) {
      const oldSocket = io.sockets.sockets.get(existing.socketId);
      if (oldSocket) oldSocket.disconnect();
    }

    const existingDb = db.dispositivos?.find(d => d.deviceId === deviceId);
    const lockPassword = existing?.lockPassword || existingDb?.lockPassword || null;

    connectedDevices.set(deviceId, {
      socketId: socket.id,
      deviceName: deviceName || 'Dispositivo',
      deviceType: deviceType || 'Android',
      serialNumber: serialNumber || deviceId,
      connectedAt: new Date(),
      status: (existing && existing.status === 'locked') ? 'locked' : 'online',
      lockPassword: lockPassword,
      usageTimeLimit: existing ? existing.usageTimeLimit : null,
      usageStartTime: existing ? existing.usageStartTime : null,
      empresaId: existing ? existing.empresaId : null
    });

    // Salvar dispositivo no banco de dados se não existir
    const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
    if (deviceIndex === -1) {
      db.dispositivos.push({
        deviceId,
        deviceName: deviceName || 'Dispositivo',
        deviceType: deviceType || 'Android',
        serialNumber: serialNumber || deviceId,
        status: (existing && existing.status === 'locked') ? 'locked' : 'online',
        lockPassword: lockPassword,
        empresaId: existing ? existing.empresaId : null
      });
      debouncedSaveData();
      console.log(`💾 Dispositivo ${deviceId} salvo no banco de dados`);
    } else {
      // Atualizar dispositivo existente
      db.dispositivos[deviceIndex].status = connectedDevices.get(deviceId).status;
      db.dispositivos[deviceIndex].lockPassword = lockPassword;
      db.dispositivos[deviceIndex].connectedAt = new Date();
      debouncedSaveData();
    }

    console.log(`📱 ${deviceName} (${deviceId}) [${connectedDevices.get(deviceId).status}]`);
    io.emit('device_connected', { deviceId, ...connectedDevices.get(deviceId), online: true });

    // Enviar produtos para o dispositivo recém-conectado
    socket.emit('produtos_sync', {
      produtos: db.produtos,
      timestamp: new Date()
    });
    console.log(`📦 Enviados ${db.produtos.length} produtos para ${deviceId}`);
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
      
      io.emit('device_status_update', { deviceId, status, lockReason: device.lockReason, lockedAt: device.lockedAt, usageTimeLimit: device.usageTimeLimit, usageStartTime: device.usageStartTime });
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
      io.emit('estoque_atualizado', {
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
    
    // Salvar no banco local
    if (!db.operacoes) db.operacoes = [];
    db.operacoes.push({
      ...operacao,
      deviceId,
      timestamp: Date.now()
    });
    debouncedSaveData();
    
    // Broadcast para todos os dashboards
    io.emit('operacao_adicionada', {
      ...operacao,
      deviceId
    });
    
    // Auditoria
    addAuditoria('operacao_caixa', deviceId, `${operacao.tipo}: R$ ${operacao.valor}`, operacao.nomeOperador);
  });

  // Receber dados de venda do Android
  socket.on('sale_data', async (data) => {
    const { deviceId, sale } = data;
    console.log('💰 Venda recebida do dispositivo:', deviceId);
    
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
      io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
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
      createdAt: new Date()
    };
    
    db.vendas.push(venda);
    debouncedSaveData();
    
    // Emitir evento para atualizar dashboards
    io.emit('venda_added', venda);
    
    console.log('✅ Venda processada e salva:', venda.id);
  });

  // Validar senha de desbloqueio enviada pelo terminal
  socket.on('unlock_attempt', async (data) => {
    const { deviceId, password } = data;
    const device = connectedDevices.get(deviceId);
    
    console.log(`🔑 [DEBUG] Tentativa de desbloqueio: ${deviceId} - senha: ${password}`);
    
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
        io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
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

      io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
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
    io.emit('control_result', data);

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
      
      io.emit('device_status_update', { deviceId, status: 'online' });
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'online';
        db.dispositivos[deviceIndex].lockReason = null;
        db.dispositivos[deviceIndex].lockedAt = null;
        debouncedSaveData();
        io.emit('device_status_update', { deviceId, status: 'online' });
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
      
      io.emit('device_status_update', { 
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
          io.emit('time_update', { 
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
          io.emit('device_status_update', { 
            deviceId, 
            status: 'locked',
            lockReason: 'Tempo de uso expirado',
            usageTimeLimit: null,
            usageStartTime: null
          });
        }
      }
    }
  }, 1000); // Verificar a cada segundo para contador preciso

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
      io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
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
    
    // Tentar identificar o usuário do dashboard
    if (token) {
      try {
        const decoded = jwt.verify(token, JWT_SECRET);
        usuario = decoded.username;
        connectedDashboards.set(socket.id, { usuario, socketId: socket.id });
      } catch (e) {
        console.log('Token inválido no dashboard_connect');
      }
    }
    
    console.log(`🖥️ Dashboard conectado: ${usuario}`);
    
    // Enviar dispositivos conectados (WebSocket ou polling recente)
    const now = new Date();
    const list = Array.from(connectedDevices.entries())
      .filter(([id]) => !BLOCKED_DEVICE_IDS.includes(id))
      .map(([id, d]) => {
        const isPollingRecent = d.lastPoll && (now - new Date(d.lastPoll)) < 120000; // 2 min
        const isOnline = d.socketId !== null || isPollingRecent;
        return { deviceId: id, ...d, online: isOnline };
      });
    
    console.log(`📊 [DEBUG] Dispositivos conectados: ${list.length}`);
    console.log(`📊 [DEBUG] DeviceIds:`, list.map(d => d.deviceId));
    
    socket.emit('devices_list', list);
    
    // Enviar vendas recentes (últimas 50)
    const recentVendas = (db.vendas || []).slice(-50).reverse();
    socket.emit('vendas_history', recentVendas);

    // Enviar operações de caixa para sincronização
    socket.emit('operacoes_sync', {
      operacoes: db.operacoes || []
    });
  });

  socket.on('lock_device', async (data) => {
    const { deviceId, reason } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      // Gerar nova senha apenas se não existir (primeiro bloqueio)
      if (!device.lockPassword) {
        device.lockPassword = Math.floor(100000 + Math.random() * 900000).toString();
      }
      device.status = 'locked';
      device.lockReason = reason;
      device.lockedAt = new Date();
      // Cancelar timer de uso ao bloquear manualmente
      device.usageTimeLimit = null;
      device.usageStartTime = null;
      enqueueDeviceCommand(deviceId, 'device_locked', { reason, lockPassword: device.lockPassword });
      if (device.socketId) io.to(device.socketId).emit('device_locked', { reason, lockPassword: device.lockPassword });
      io.emit('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, lockPassword: device.lockPassword, usageTimeLimit: null, usageStartTime: null });
      
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
        io.emit('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, usageTimeLimit: null, usageStartTime: null });
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
      io.emit('device_status_update', { deviceId, status: 'online', lockReason: null, lockedAt: null, usageTimeLimit: null, usageStartTime: null });
      
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
        io.emit('device_status_update', { deviceId, status: 'online' });
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
        io.emit('device_disconnected', { deviceId: id });
        console.log(`📱 Dispositivo ${id} removido do mapa de conectados`);
        break;
      }
    }
  });
});

const PORT = 3001;
initializeApp().then(() => {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server na porta ${PORT}`);
  });
}).catch(err => {
  console.error('❌ Falha na inicialização:', err);
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server na porta ${PORT} (sem MongoDB)`);
  });
});
