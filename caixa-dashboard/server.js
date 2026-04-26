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
const puppeteer = require('puppeteer');
require('dotenv').config();

const app = express();

app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

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

// ==================== PERSISTÊNCIA DE DADOS ====================
const DATA_FILE = path.join(__dirname, 'data.json');
const AUDITORIA_FILE = path.join(__dirname, 'auditoria.json');

// Carregar dados do arquivo
function loadData() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const data = fs.readFileSync(DATA_FILE, 'utf8');
      if (data.trim()) {
        return JSON.parse(data);
      }
    }
  } catch (error) {
    console.error('Erro ao carregar dados:', error.message);
  }
  
  // Dados padrão se arquivo não existir ou estiver vazio
  return {
    produtos: [],
    categorias: [],
    vendas: [],
    operacoes: [],
    usuarios: [
      { id: 1, username: 'rodrigodevmt', password: bcrypt.hashSync('1985', 10), role: 'admin' }
    ],
    dispositivos: []
  };
}

// Carregar auditoria do arquivo
function loadAuditoria() {
  try {
    if (fs.existsSync(AUDITORIA_FILE)) {
      const data = fs.readFileSync(AUDITORIA_FILE, 'utf8');
      if (data.trim()) {
        return JSON.parse(data);
      }
    }
  } catch (error) {
    console.error('Erro ao carregar auditoria:', error.message);
  }
  return [];
}

// Salvar dados em arquivo
function saveData() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (error) {
    console.error('Erro ao salvar dados:', error.message);
  }
}

// Salvar auditoria em arquivo
function saveAuditoria() {
  try {
    fs.writeFileSync(AUDITORIA_FILE, JSON.stringify(db.auditoria, null, 2), 'utf8');
  } catch (error) {
    console.error('Erro ao salvar auditoria:', error.message);
  }
}

// ==================== DADOS COM PERSISTÊNCIA ====================
let db = loadData();
db.auditoria = loadAuditoria();

const connectedDevices = new Map();
const connectedDashboards = new Map(); // Guardar usuário do dashboard

// Função para adicionar logs de auditoria
function addAuditoria(tipo, deviceId, detalhes, usuario = null) {
  const log = {
    id: Date.now(),
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
setInterval(() => {
  saveData();
  console.log('💾 Dados salvos automaticamente');
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
process.on('SIGINT', () => {
  console.log('\n🔄 Salvando dados antes de encerrar...');
  saveData();
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

const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

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
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;
  console.log(`[LOGIN] ${username}`);
  
  // Verificar se é usuário do sistema
  let user = db.usuarios.find(u => u.username === username);
  
  // Se não for usuário do sistema, verificar se é empresa
  if (!user) {
    const empresa = (db.empresas || []).find(e => e.login === username);
    if (empresa) {
      if (!bcrypt.compareSync(password, empresa.senha)) {
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
  
  if (!bcrypt.compareSync(password, user.password)) return res.status(401).json({ error: 'Credenciais inválidas' });
  
  const token = jwt.sign({ id: user.id, username: user.username, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
  res.json({ token, user: { id: user.id, username: user.username, role: user.role } });
});

app.get('/api/auth/verify', authenticateToken, (req, res) => {
  res.json({ valid: true, user: req.user });
});

app.get('/api/dispositivos', authenticateToken, (req, res) => {
  const list = Array.from(connectedDevices.entries()).map(([id, d]) => ({
    deviceId: id, ...d, online: d.socketId !== null
  }));
  res.json(list);
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

app.post('/api/categorias', authenticateToken, (req, res) => {
  const categoria = {
    id: Date.now(),
    nome: req.body.nome,
    descricao: req.body.descricao,
    createdAt: new Date()
  };
  db.categorias.push(categoria);
  saveData(); // Salvar imediatamente
  io.emit('categoria_added', categoria);
  
  // Notificar dispositivos sobre nova categoria
  io.emit('categorias_sync', {
    categorias: db.categorias,
    timestamp: new Date(),
    action: 'added',
    data: categoria
  });
  
  res.json(categoria);
});

app.put('/api/categorias/:id', authenticateToken, (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  
  db.categorias[index] = { ...db.categorias[index], ...req.body };
  saveData(); // Salvar imediatamente
  io.emit('categoria_updated', db.categorias[index]);
  
  // Notificar dispositivos sobre categoria atualizada
  io.emit('categorias_sync', {
    categorias: db.categorias,
    timestamp: new Date(),
    action: 'updated',
    data: db.categorias[index]
  });
  
  res.json(db.categorias[index]);
});

app.delete('/api/categorias/:id', authenticateToken, (req, res) => {
  const index = db.categorias.findIndex(c => c.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Categoria não encontrada' });
  
  const deleted = db.categorias.splice(index, 1)[0];
  saveData(); // Salvar imediatamente
  io.emit('categoria_deleted', deleted);
  
  // Notificar dispositivos sobre categoria excluída
  io.emit('categorias_sync', {
    categorias: db.categorias,
    timestamp: new Date(),
    action: 'deleted',
    data: deleted
  });
  
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

// ==================== ROTAS DE PRODUTOS ====================
app.get('/api/produtos', authenticateToken, (req, res) => {
  res.json(db.produtos);
});

// Rota para buscar vendas
app.get('/api/vendas', authenticateToken, (req, res) => {
  res.json(db.vendas);
});

app.post('/api/produtos', authenticateToken, (req, res) => {
  const produto = {
    id: Date.now(),
    nome: req.body.nome,
    descricao: req.body.descricao,
    preco: req.body.preco,
    categoriaId: req.body.categoriaId,
    codigoBarras: req.body.codigoBarras || Date.now().toString(),
    estoque: req.body.estoque || 0,
    unidade: req.body.unidade || 'un',
    imagem: req.body.imagem || null,
    createdAt: new Date()
  };
  db.produtos.push(produto);
  saveData(); // Salvar imediatamente
  io.emit('produto_added', produto);
  
  // Notificar dispositivos sobre novo produto
  io.emit('produtos_sync', {
    produtos: db.produtos,
    timestamp: new Date(),
    action: 'added',
    data: produto
  });
  
  res.json(produto);
});

app.put('/api/produtos/:id', authenticateToken, (req, res) => {
  const index = db.produtos.findIndex(p => p.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Produto não encontrado' });
  
  db.produtos[index] = { ...db.produtos[index], ...req.body };
  saveData(); // Salvar imediatamente
  io.emit('produto_updated', db.produtos[index]);
  
  // Notificar dispositivos sobre produto atualizado
  io.emit('produtos_sync', {
    produtos: db.produtos,
    timestamp: new Date(),
    action: 'updated',
    data: db.produtos[index]
  });
  
  res.json(db.produtos[index]);
});

app.delete('/api/produtos/:id', authenticateToken, (req, res) => {
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
  
  saveData(); // Salvar imediatamente
  io.emit('produto_deleted', deleted);
  
  // Notificar dispositivos sobre produto excluído
  io.emit('produtos_sync', {
    produtos: db.produtos,
    timestamp: new Date(),
    action: 'deleted',
    data: deleted
  });
  
  res.json(deleted);
});

// ==================== ROTAS DE EMPRESAS ====================
app.get('/api/empresas', authenticateToken, (req, res) => {
  res.json(db.empresas || []);
});

app.post('/api/empresas', authenticateToken, (req, res) => {
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
    id: Date.now(),
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
  saveData();
  
  res.json(empresa);
});

app.put('/api/empresas/:id', authenticateToken, (req, res) => {
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
  
  saveData();
  res.json(db.empresas[index]);
});

app.delete('/api/empresas/:id', authenticateToken, (req, res) => {
  const index = (db.empresas || []).findIndex(e => e.id == req.params.id);
  if (index === -1) return res.status(404).json({ error: 'Empresa não encontrada' });
  
  const deleted = db.empresas.splice(index, 1)[0];
  saveData();
  res.json(deleted);
});

// Rota para associar dispositivo a empresa
app.put('/api/dispositivos/:deviceId/empresa', authenticateToken, (req, res) => {
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
    saveData();
  }

  io.emit('device_status_update', { deviceId, empresaId });

  // Auditoria
  addAuditoria('mudanca_status', deviceId, `Dispositivo associado à empresa ${empresaId}`, dashboardInfo?.usuario);

  res.json({ success: true, empresaId });
});

// ==================== ROTAS DE DISPOSITIVOS ====================
app.put('/api/dispositivos/:deviceId/password', authenticateToken, (req, res) => {
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
  saveData(); // Salvar imediatamente

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

  // Enviar comando para o dispositivo via WebSocket
  if (device.socketId) {
    io.to(device.socketId).emit('control_command', { action });
    console.log(`📤 [CONTROL] Comando '${action}' enviado para socketId ${device.socketId} (deviceId: ${deviceId})`);

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
  } else {
    console.log(`❌ [CONTROL] Dispositivo ${deviceId} não possui socketId`);
    res.status(400).json({ error: 'Dispositivo não está conectado' });
  }
});

// ==================== API DE OPERAÇÕES DE CAIXA ====================
app.get('/api/operacoes', (req, res) => {
  // Retornar operações do banco local (do dashboard)
  res.json(db.operacoes || []);
});

app.post('/api/operacoes', (req, res) => {
  const { tipo, valor, deviceId, nomeOperador, observacao } = req.body;
  
  const operacao = {
    id: Date.now(),
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
  saveData();
  
  // Broadcast para dashboards
  io.emit('operacao_adicionada', operacao);
  
  // Broadcast para dispositivos
  io.emit('operacoes_sync', {
    operacoes: db.operacoes,
    timestamp: new Date()
  });
  
  // Auditoria
  addAuditoria('operacao_caixa', deviceId, `${tipo} registrada: R$ ${valorProcessado.toFixed(2)}`, nomeOperador);
  
  res.json(operacao);
});

// DELETE operação (para limpar dados incorretos)
app.delete('/api/operacoes/:id', (req, res) => {
  const { id } = req.params;
  
  if (!db.operacoes) {
    return res.status(404).json({ error: 'Operações não encontradas' });
  }
  
  const index = db.operacoes.findIndex(op => op.id == id);
  if (index === -1) {
    return res.status(404).json({ error: 'Operação não encontrada' });
  }
  
  const operacaoRemovida = db.operacoes.splice(index, 1)[0];
  saveData();
  
  // Broadcast para dashboards
  io.emit('operacao_removida', { id: id });
  
  // Auditoria
  addAuditoria('operacao_caixa', 'dashboard', `Operação removida: ${operacaoRemovida.tipo} R$ ${operacaoRemovida.valor}`, 'dashboard');
  
  res.json({ message: 'Operação removida com sucesso', operacao: operacaoRemovida });
});

// ==================== GERAÇÃO DE PDF ====================
app.post('/api/fechamento-pdf', authenticateToken, async (req, res) => {
  try {
    const dados = req.body;
    
    console.log('📄 Gerando PDF do fechamento geral:', dados.dataHora);
    console.log('📊 Dados recebidos:', {
      totalAbertura: dados.totalAbertura,
      totalSuprimento: dados.totalSuprimento,
      totalSangria: dados.totalSangria,
      totalFechamento: dados.totalFechamento,
      totalVendas: dados.totalVendas,
      operacoes: dados.operacoes?.length || 0,
      vendas: dados.vendas?.length || 0
    });
    
    if (!dados || dados.totalAbertura === undefined) {
      console.error('❌ Dados inválidos para gerar PDF');
      return res.status(400).json({ error: 'Dados inválidos para gerar PDF' });
    }
    
    const browser = await puppeteer.launch({
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    // Criar HTML para o PDF
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <style>
          body { font-family: Arial, sans-serif; padding: 40px; }
          .header { text-align: center; margin-bottom: 40px; }
          .header h1 { color: #6200EE; margin: 0; }
          .header p { color: #666; margin: 5px 0; }
          .section { margin-bottom: 30px; }
          .section h2 { color: #333; border-bottom: 2px solid #6200EE; padding-bottom: 10px; }
          .row { display: flex; justify-content: space-between; margin: 10px 0; }
          .label { color: #666; }
          .value { font-weight: bold; color: #333; }
          .total { font-size: 18px; color: #6200EE; }
          .table { width: 100%; border-collapse: collapse; margin-top: 20px; }
          .table th, .table td { border: 1px solid #ddd; padding: 10px; text-align: left; }
          .table th { background-color: #6200EE; color: white; }
          .table tr:nth-child(even) { background-color: #f9f9f9; }
          .positive { color: #00C853; }
          .negative { color: #D50000; }
        </style>
      </head>
      <body>
        <div class="header">
          <h1>FECHAMENTO GERAL DO CAIXA</h1>
          <p>CaixaCombo - Sistema de PDV</p>
          <p>Data: ${dados.dataHora}</p>
        </div>
        
        <div class="section">
          <h2>RESUMO FINANCEIRO</h2>
          <div class="row">
            <span class="label">Total de Aberturas:</span>
            <span class="value positive">+ R$ ${dados.totalAbertura.toFixed(2)}</span>
          </div>
          <div class="row">
            <span class="label">Total de Suprimentos:</span>
            <span class="value positive">+ R$ ${dados.totalSuprimento.toFixed(2)}</span>
          </div>
          <div class="row">
            <span class="label">Total de Sangrias:</span>
            <span class="value negative">- R$ ${dados.totalSangria.toFixed(2)}</span>
          </div>
          <div class="row">
            <span class="label">Total de Vendas:</span>
            <span class="value positive">R$ ${dados.totalVendas.toFixed(2)}</span>
          </div>
          <div class="row" style="margin-top: 20px; padding-top: 20px; border-top: 2px solid #6200EE;">
            <span class="label total">SALDO FINAL:</span>
            <span class="value total">R$ ${dados.totalFechamento.toFixed(2)}</span>
          </div>
        </div>
        
        ${dados.operacoes && dados.operacoes.length > 0 ? `
        <div class="section">
          <h2>OPERAÇÕES DE CAIXA</h2>
          <table class="table">
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Valor</th>
                <th>Data/Hora</th>
                <th>Observação</th>
              </tr>
            </thead>
            <tbody>
              ${dados.operacoes.map(op => `
                <tr>
                  <td>${op.tipo.toUpperCase()}</td>
                  <td class="${op.tipo === 'abertura' || op.tipo === 'suprimento' ? 'positive' : 'negative'}">
                    ${op.tipo === 'abertura' || op.tipo === 'suprimento' ? '+' : '-'} R$ ${(op.valor || 0).toFixed(2)}
                  </td>
                  <td>${new Date(op.dataHora || op.createdAt).toLocaleString('pt-BR')}</td>
                  <td>${op.observacao || '-'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
        ` : ''}
        
        ${dados.vendas && dados.vendas.length > 0 ? `
        <div class="section">
          <h2>VENDAS REALIZADAS</h2>
          <table class="table">
            <thead>
              <tr>
                <th>Nº</th>
                <th>Data/Hora</th>
                <th>Forma Pagamento</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              ${dados.vendas.map(venda => `
                <tr>
                  <td>${venda.id || venda.numero}</td>
                  <td>${new Date(venda.dataHora || venda.createdAt).toLocaleString('pt-BR')}</td>
                  <td>${venda.formaPagamento}</td>
                  <td class="positive">R$ ${(venda.total || 0).toFixed(2)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
        ` : ''}
        
        <div class="section" style="margin-top: 50px; text-align: center; color: #666; font-size: 12px;">
          <p>Documento gerado automaticamente pelo sistema CaixaCombo</p>
          <p>Não é necessário assinatura - documento digital válido</p>
        </div>
      </body>
      </html>
    `;
    
    await page.setContent(html, { waitUntil: 'networkidle0' });
    
    const pdfBuffer = await page.pdf({
      format: 'A4',
      printBackground: true,
      margin: {
        top: '20px',
        right: '20px',
        bottom: '20px',
        left: '20px'
      }
    });
    
    await browser.close();
    
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename=fechamento-geral-${new Date().toISOString().split('T')[0]}.pdf`);
    res.send(pdfBuffer);
    
    console.log('✅ PDF gerado com sucesso');
  } catch (error) {
    console.error('❌ Erro ao gerar PDF:', error);
    res.status(500).json({ error: 'Erro ao gerar PDF', details: error.message });
  }
});

// ==================== WEBSOCKET ====================
io.on('connection', (socket) => {
  console.log('🔌 Socket:', socket.id);

  socket.on('device_connect', (data) => {
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

    const lockPassword = (existing && existing.lockPassword) ? existing.lockPassword : Math.floor(100000 + Math.random() * 900000).toString();

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
      saveData();
      console.log(`💾 Dispositivo ${deviceId} salvo no banco de dados`);
    } else {
      // Atualizar dispositivo existente
      db.dispositivos[deviceIndex].status = connectedDevices.get(deviceId).status;
      db.dispositivos[deviceIndex].lockPassword = lockPassword;
      db.dispositivos[deviceIndex].connectedAt = new Date();
      saveData();
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

  socket.on('device_status', (data) => {
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
  socket.on('estoque_update', (data) => {
    console.log(`📦 [ESTOQUE] Atualização de ${data.deviceId}: produto ${data.produtoId} -> ${data.novoEstoque}`);
    
    // Atualizar estoque no banco local
    const produto = db.produtos.find(p => p.id == data.produtoId);
    if (produto) {
      const estoqueAnterior = produto.estoque;
      produto.estoque = data.novoEstoque;
      saveData();
      
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
  socket.on('operacao_data', (data) => {
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
    saveData();
    
    // Broadcast para todos os dashboards
    io.emit('operacao_adicionada', {
      ...operacao,
      deviceId
    });
    
    // Auditoria
    addAuditoria('operacao_caixa', deviceId, `${operacao.tipo}: R$ ${operacao.valor}`, operacao.nomeOperador);
  });

  // Receber dados de venda do Android
  socket.on('sale_data', (data) => {
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
      id: Date.now(),
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
    saveData();
    
    // Emitir evento para atualizar dashboards
    io.emit('venda_added', venda);
    io.emit('sale_update', { sale: venda });
    
    console.log('✅ Venda processada e salva:', venda.id);
  });

  // Validar senha de desbloqueio enviada pelo terminal
  socket.on('unlock_attempt', (data) => {
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
        socket.emit('unlock_response', { deviceId, success: false, message: 'Senha incorreta', correctPassword: device.lockPassword });
      }
    } else {
      console.log(`❌ [DEBUG] Dispositivo não encontrado para unlock_attempt: ${deviceId}`);
      socket.emit('unlock_response', { deviceId, success: false, message: 'Dispositivo não encontrado' });
    }
  });

  // Dispositivo confirmando desbloqueio via terminal (legado - manter compatibilidade)
  socket.on('unlock_confirmed', (data) => {
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
  socket.on('control_result', (data) => {
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
  socket.on('control_log', (data) => {
    const { deviceId, action, timestamp } = data;
    console.log(`📝 [CONTROL_LOG] Dispositivo ${deviceId} recebeu comando: ${action} em ${new Date(timestamp).toLocaleString('pt-BR')}`);
  });

  // Endpoint alternativo para forçar desbloqueio (se o app não enviar unlock_confirmed)
  socket.on('force_unlock', (data) => {
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
        saveData();
        io.emit('device_status_update', { deviceId, status: 'online' });
        addAuditoria('desbloqueio', deviceId, 'Desbloqueio forçado (offline)', dashboardInfo?.usuario);
      }
    }
  });

  // Definir tempo de uso para dispositivo
  socket.on('set_usage_time', (data) => {
    const { deviceId, minutes } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      console.log(`⏱️ Definindo tempo de uso: ${deviceId} - ${minutes} minutos por ${dashboardInfo?.usuario}`);
      device.usageTimeLimit = minutes;
      device.usageStartTime = new Date();
      
      // Enviar comando para o dispositivo
      if (device.socketId) {
        io.to(device.socketId).emit('set_time_limit', { minutes, startTime: device.usageStartTime });
      }
      
      // Auditoria: Tempo de uso definido
      addAuditoria('mudanca_status', deviceId, `Tempo de uso definido: ${minutes} minutos`, dashboardInfo?.usuario);
      
      io.emit('device_status_update', { 
        deviceId, 
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
  socket.on('sync_data', (data) => {
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

  socket.on('dashboard_connect', (data) => {
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
    
    // Enviar APENAS dispositivos conectados via WebSocket (em tempo real)
    const list = Array.from(connectedDevices.entries()).map(([id, d]) => ({
      deviceId: id, ...d, online: d.socketId !== null
    }));
    
    console.log(`📊 [DEBUG] Dispositivos conectados: ${list.length}`);
    console.log(`📊 [DEBUG] DeviceIds:`, list.map(d => d.deviceId));
    
    socket.emit('devices_list', list);
  });

  socket.on('lock_device', (data) => {
    const { deviceId, reason } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      device.status = 'locked';
      device.lockReason = reason;
      device.lockedAt = new Date();
      if (device.socketId) io.to(device.socketId).emit('device_locked', { reason, lockPassword: device.lockPassword });
      io.emit('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, usageTimeLimit: null, usageStartTime: null });
      
      // Auditoria: Bloqueio via dashboard
      addAuditoria('bloqueio', deviceId, `Bloqueado: ${reason}`, dashboardInfo?.usuario);
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'locked';
        db.dispositivos[deviceIndex].lockReason = reason;
        db.dispositivos[deviceIndex].lockedAt = new Date();
        saveData();
        io.emit('device_status_update', { deviceId, status: 'locked', lockReason: reason, lockedAt: device.lockedAt, usageTimeLimit: null, usageStartTime: null });
        addAuditoria('bloqueio', deviceId, `Bloqueado (offline): ${reason}`, dashboardInfo?.usuario);
      }
    }
  });

  socket.on('unlock_device', (data) => {
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      device.status = 'online';
      if (device.socketId) io.to(device.socketId).emit('device_unlocked', {});
      io.emit('device_status_update', { deviceId, status: 'online' });
      
      // Auditoria: Desbloqueio via dashboard
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado via dashboard', dashboardInfo?.usuario);
    } else {
      // Dispositivo offline - atualizar no banco de dados
      const deviceIndex = db.dispositivos.findIndex(d => d.deviceId === deviceId);
      if (deviceIndex !== -1) {
        db.dispositivos[deviceIndex].status = 'online';
        db.dispositivos[deviceIndex].lockReason = null;
        db.dispositivos[deviceIndex].lockedAt = null;
        saveData();
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
        
        d.socketId = null;
        io.emit('device_disconnected', { deviceId: id });
        break;
      }
    }
  });
});

const PORT = 3001;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server na porta ${PORT}`);
  console.log(`👤 Usuário: rodrigodevmt / 1985`);
});
