const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();

// Configuração CORS restrita
const allowedOrigins = process.env.CORS_ORIGINS 
  ? process.env.CORS_ORIGINS.split(',') 
  : ['http://localhost:3000', 'http://localhost:3001'];

app.use(cors({
  origin: (origin, callback) => {
    // Permitir requisições sem origem (ex: mobile apps, Postman)
    // e origens na lista permitida
    if (!origin || allowedOrigins.some(allowed => origin.startsWith(allowed.trim()))) {
      callback(null, true);
    } else {
      // Em desenvolvimento, permitir todas as origens localhost
      if (process.env.NODE_ENV !== 'production' && origin.includes('localhost')) {
        callback(null, true);
      } else {
        callback(null, false);
      }
    }
  },
  credentials: true
}));
app.use(express.json());

// ==================== PERSISTÊNCIA DE DADOS ====================
const DATA_FILE = path.join(__dirname, 'data.json');

// Carregar dados do arquivo
function loadData() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const data = fs.readFileSync(DATA_FILE, 'utf8');
      return JSON.parse(data);
    }
  } catch (error) {
    console.error('Erro ao carregar dados:', error.message);
  }
  return {
    produtos: [],
    categorias: [],
    vendas: [],
    operacoes: [],
    usuarios: []
  };
}

// Salvar dados no arquivo
function saveData() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (error) {
    console.error('Erro ao salvar dados:', error.message);
  }
}

// Banco de dados (carregado do arquivo)
const db = loadData();

// Carregar estado de dispositivos conectados do arquivo
const DEVICE_STATE_FILE = path.join(__dirname, 'device_state.json');

function loadDeviceState() {
  try {
    if (fs.existsSync(DEVICE_STATE_FILE)) {
      const data = fs.readFileSync(DEVICE_STATE_FILE, 'utf8');
      return JSON.parse(data);
    }
  } catch (error) {
    console.error('Erro ao carregar estado de dispositivos:', error.message);
  }
  return {};
}

function saveDeviceState() {
  try {
    const state = {};
    for (const [deviceId, device] of connectedDevices.entries()) {
      state[deviceId] = {
        deviceId,
        deviceName: device.deviceName,
        deviceType: device.deviceType,
        status: device.status,
        lockReason: device.lockReason,
        lockedAt: device.lockedAt,
        usageTimeLimit: device.usageTimeLimit,
        usageStartTime: device.usageStartTime,
        connectedAt: device.connectedAt
      };
    }
    fs.writeFileSync(DEVICE_STATE_FILE, JSON.stringify(state, null, 2), 'utf8');
  } catch (error) {
    console.error('Erro ao salvar estado de dispositivos:', error.message);
  }
}

// ==================== CRIAR SERVER E IO ANTES DAS ROTAS ====================
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: allowedOrigins,
    methods: ["GET", "POST", "PUT", "DELETE"],
    credentials: true
  },
  transports: ["websocket", "polling"]
});

// Armazenar dispositivos conectados
const connectedDevices = new Map();
const connectedDashboards = new Set();

// ==================== AUTENTICAÇÃO JWT ====================
const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key-change-in-production';

// Middleware de autenticação
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ error: 'Token não fornecido' });
  }
  
  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
    req.user = user;
    next();
  });
}

// Middleware opcional (permite acesso sem token, mas valida se presente)
function optionalAuth(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (token) {
    jwt.verify(token, JWT_SECRET, (err, user) => {
      if (!err) {
        req.user = user;
      }
    });
  }
  next();
}

// ==================== ROTAS DE AUTENTICAÇÃO ====================

// Login
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;
  
  // Buscar usuário
  const user = db.usuarios.find(u => u.username === username);
  
  if (!user || !bcrypt.compareSync(password, user.password)) {
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }
  
  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    JWT_SECRET,
    { expiresIn: '24h' }
  );
  
  res.json({ token, user: { id: user.id, username: user.username, role: user.role } });
});

// Registrar usuário (apenas admin)
app.post('/api/auth/register', authenticateToken, (req, res) => {
  if (req.user.role !== 'admin') {
    return res.status(403).json({ error: 'Apenas administradores podem criar usuários' });
  }
  
  const { username, password, role = 'user' } = req.body;
  
  if (db.usuarios.find(u => u.username === username)) {
    return res.status(400).json({ error: 'Usuário já existe' });
  }
  
  const hashedPassword = bcrypt.hashSync(password, 10);
  const user = {
    id: Date.now(),
    username,
    password: hashedPassword,
    role,
    createdAt: new Date()
  };
  
  db.usuarios.push(user);
  saveData();
  
  res.status(201).json({ id: user.id, username: user.username, role: user.role });
});

// Verificar token
app.get('/api/auth/verify', authenticateToken, (req, res) => {
  res.json({ valid: true, user: req.user });
});

// ==================== API REST - PRODUTOS ====================

app.get('/api/produtos', optionalAuth, (req, res) => {
  res.json(db.produtos);
});

app.post('/api/produtos', authenticateToken, (req, res) => {
  const produto = {
    id: Date.now(),
    ...req.body,
    createdBy: req.user?.username || 'system',
    createdAt: new Date()
  };
  db.produtos.push(produto);
  saveData();
  
  io.emit('produto_added', produto);
  
  res.json(produto);
});

app.put('/api/produtos/:id', authenticateToken, (req, res) => {
  const index = db.produtos.findIndex(p => p.id === parseInt(req.params.id));
  if (index !== -1) {
    db.produtos[index] = { 
      ...db.produtos[index], 
      ...req.body,
      updatedBy: req.user?.username || 'system',
      updatedAt: new Date()
    };
    saveData();
    
    io.emit('produto_updated', db.produtos[index]);
    
    res.json(db.produtos[index]);
  } else {
    res.status(404).json({ error: 'Produto não encontrado' });
  }
});

app.delete('/api/produtos/:id', authenticateToken, (req, res) => {
  const index = db.produtos.findIndex(p => p.id === parseInt(req.params.id));
  if (index !== -1) {
    const produto = db.produtos.splice(index, 1)[0];
    saveData();
    
    io.emit('produto_deleted', { id: produto.id });
    
    res.json({ message: 'Produto deletado' });
  } else {
    res.status(404).json({ error: 'Produto não encontrado' });
  }
});

// ==================== API REST - CATEGORIAS ====================

app.get('/api/categorias', optionalAuth, (req, res) => {
  res.json(db.categorias);
});

app.post('/api/categorias', authenticateToken, (req, res) => {
  const categoria = {
    id: Date.now(),
    ...req.body,
    createdBy: req.user?.username || 'system',
    createdAt: new Date()
  };
  db.categorias.push(categoria);
  saveData();
  
  io.emit('categoria_added', categoria);
  
  res.json(categoria);
});

app.put('/api/categorias/:id', authenticateToken, (req, res) => {
  const index = db.categorias.findIndex(c => c.id === parseInt(req.params.id));
  if (index !== -1) {
    db.categorias[index] = { 
      ...db.categorias[index], 
      ...req.body,
      updatedBy: req.user?.username || 'system',
      updatedAt: new Date()
    };
    saveData();
    
    io.emit('categoria_updated', db.categorias[index]);
    
    res.json(db.categorias[index]);
  } else {
    res.status(404).json({ error: 'Categoria não encontrada' });
  }
});

app.delete('/api/categorias/:id', authenticateToken, (req, res) => {
  const index = db.categorias.findIndex(c => c.id === parseInt(req.params.id));
  if (index !== -1) {
    db.categorias.splice(index, 1);
    saveData();
    
    io.emit('categoria_deleted', { id: parseInt(req.params.id) });
    
    res.json({ message: 'Categoria deletada' });
  } else {
    res.status(404).json({ error: 'Categoria não encontrada' });
  }
});

// ==================== API REST - VENDAS ====================

app.get('/api/vendas', optionalAuth, (req, res) => {
  res.json(db.vendas);
});

app.post('/api/vendas', optionalAuth, (req, res) => {
  const venda = {
    id: Date.now(),
    ...req.body,
    createdAt: new Date()
  };
  db.vendas.push(venda);
  saveData();
  
  io.emit('venda_added', venda);
  
  res.json(venda);
});

// ==================== API REST - OPERAÇÕES DE CAIXA ====================

app.get('/api/operacoes', optionalAuth, (req, res) => {
  res.json(db.operacoes);
});

app.post('/api/operacoes', optionalAuth, (req, res) => {
  const operacao = {
    id: Date.now(),
    ...req.body,
    createdAt: new Date()
  };
  db.operacoes.push(operacao);
  saveData();
  
  io.emit('operacao_added', operacao);
  
  res.json(operacao);
});

// ==================== SINCRONIZAÇÃO ====================

app.post('/api/sync/produtos', optionalAuth, (req, res) => {
  const { produtos } = req.body;
  if (Array.isArray(produtos)) {
    db.produtos = produtos;
    saveData();
    io.emit('produtos_synced', db.produtos);
    res.json({ message: `${produtos.length} produtos sincronizados` });
  } else {
    res.status(400).json({ error: 'Formato inválido' });
  }
});

app.post('/api/sync/categorias', optionalAuth, (req, res) => {
  const { categorias } = req.body;
  if (Array.isArray(categorias)) {
    db.categorias = categorias;
    saveData();
    io.emit('categorias_synced', db.categorias);
    res.json({ message: `${categorias.length} categorias sincronizadas` });
  } else {
    res.status(400).json({ error: 'Formato inválido' });
  }
});

app.post('/api/sync/vendas', optionalAuth, (req, res) => {
  const { vendas } = req.body;
  if (Array.isArray(vendas)) {
    db.vendas = vendas;
    saveData();
    io.emit('vendas_synced', db.vendas);
    res.json({ message: `${vendas.length} vendas sincronizadas` });
  } else {
    res.status(400).json({ error: 'Formato inválido' });
  }
});

// Sincronização por deviceId
app.post('/api/sync', optionalAuth, (req, res) => {
  const { deviceId } = req.body;
  if (deviceId) {
    const device = connectedDevices.get(deviceId);
    if (device) {
      io.to(device.socketId).emit('request_sync', {
        timestamp: new Date()
      });
      res.json({ message: `Sincronização solicitada para dispositivo ${deviceId}` });
    } else {
      res.status(404).json({ error: 'Dispositivo não encontrado' });
    }
  } else {
    res.status(400).json({ error: 'deviceId não fornecido' });
  }
});

// Endpoint para dispositivos solicitarem dados
app.get('/api/sync/data', optionalAuth, (req, res) => {
  res.json({
    produtos: db.produtos,
    categorias: db.categorias,
    vendas: db.vendas,
    operacoes: db.operacoes
  });
});

// ==================== WEBSOCKET ====================

io.on('connection', (socket) => {
  console.log('Cliente conectado:', socket.id);

  // Dispositivo Android conectando
  socket.on('device_connect', (data) => {
    const { deviceId, deviceName, deviceType, token } = data;
    
    // Validar token se fornecido
    if (token) {
      try {
        const decoded = jwt.verify(token, JWT_SECRET);
        socket.user = decoded;
      } catch (err) {
        console.log('Token inválido para dispositivo:', deviceId);
      }
    }
    
    connectedDevices.set(deviceId, {
      socketId: socket.id,
      deviceName,
      deviceType,
      connectedAt: new Date(),
      status: 'online'
    });
    
    console.log(`Dispositivo conectado: ${deviceName} (${deviceId})`);
    
    io.emit('device_connected', {
      deviceId,
      deviceName,
      deviceType,
      status: 'online',
      connectedAt: new Date()
    });
  });

  // Dispositivo enviando status
  socket.on('device_status', (data) => {
    const { deviceId, status } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      // Tratar cancelamento de tempo de uso
      if (status === 'cancel_usage_time') {
        device.status = 'online';
        delete device.usageTimeLimit;
        delete device.usageStartTime;
        saveData();
        saveDeviceState();
        
        io.emit('device_status_update', {
          deviceId,
          status: 'online',
          timestamp: new Date()
        });
        return;
      }
      
      device.status = status;
      device.lastUpdate = new Date();
      
      io.emit('device_status_update', {
        deviceId,
        status,
        timestamp: new Date()
      });
    }
  });

  // Dispositivo enviando dados de venda
  socket.on('sale_data', (data) => {
    const { deviceId, sale } = data;
    
    console.log(`Venda recebida do dispositivo ${deviceId}:`, sale);
    
    // Salvar venda
    const venda = {
      id: Date.now(),
      ...sale,
      deviceId,
      createdAt: new Date()
    };
    db.vendas.push(venda);
    saveData();
    
    io.emit('sale_update', {
      deviceId,
      sale: venda,
      timestamp: new Date()
    });
  });

  // Dispositivo enviando dados do caixa
  socket.on('caixa_data', (data) => {
    const { deviceId, caixa } = data;
    
    console.log(`Dados do caixa recebidos do dispositivo ${deviceId}`);
    
    io.emit('caixa_update', {
      deviceId,
      caixa,
      timestamp: new Date()
    });
  });

  // Dashboard conectando
  socket.on('dashboard_connect', (data) => {
    const { token } = data || {};
    
    // Validar token se fornecido
    if (token) {
      try {
        const decoded = jwt.verify(token, JWT_SECRET);
        socket.user = decoded;
      } catch (err) {
        socket.emit('auth_error', { error: 'Token inválido' });
        return;
      }
    }
    
    connectedDashboards.add(socket.id);
    console.log('Dashboard conectado');
    
    // Enviar lista de dispositivos conectados E desconectados (com estado salvo)
    const devicesList = Array.from(connectedDevices.entries()).map(([deviceId, device]) => ({
      deviceId,
      ...device,
      online: device.socketId !== null // Marcar como online se tiver socketId
    }));
    
    socket.emit('devices_list', devicesList);
  });

  // Dashboard enviando comando para dispositivo
  socket.on('command_device', (data) => {
    const { deviceId, command, params } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      io.to(device.socketId).emit('execute_command', {
        command,
        params,
        timestamp: new Date()
      });
      
      console.log(`Comando enviado para dispositivo ${deviceId}:`, command);
    } else {
      socket.emit('command_error', {
        deviceId,
        error: 'Dispositivo não encontrado'
      });
    }
  });

  // Dashboard solicitando dados de dispositivo
  socket.on('request_device_data', (data) => {
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      io.to(device.socketId).emit('request_data', {
        timestamp: new Date()
      });
    }
  });

  // ==================== BLOQUEIO DE DISPOSITIVO ====================
  
  // Dashboard bloqueando dispositivo
  socket.on('lock_device', (data) => {
    const { deviceId, reason } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      device.status = 'locked';
      device.lockReason = reason || 'Bloqueado pelo administrador';
      device.lockedAt = new Date();
      saveData();
      saveDeviceState();
      
      io.to(device.socketId).emit('device_locked', {
        reason: device.lockReason,
        timestamp: new Date()
      });
      
      io.emit('device_status_update', {
        deviceId,
        status: 'locked',
        lockReason: device.lockReason,
        lockedAt: device.lockedAt
      });
      
      console.log(`Dispositivo ${deviceId} bloqueado: ${device.lockReason}`);
    }
  });
  
  // Dashboard desbloqueando dispositivo
  socket.on('unlock_device', (data) => {
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      delete device.usageTimeLimit;
      delete device.usageStartTime;
      saveData();
      saveDeviceState();
      
      io.to(device.socketId).emit('device_unlocked', {
        timestamp: new Date()
      });
      
      io.emit('device_status_update', {
        deviceId,
        status: 'online'
      });
      
      console.log(`Dispositivo ${deviceId} desbloqueado`);
    }
  });
  
  // Dashboard configurando tempo de uso
  socket.on('set_usage_time', (data) => {
    const { deviceId, minutes } = data;
    const device = connectedDevices.get(deviceId);
    
    if (device) {
      device.usageTimeLimit = minutes;
      device.usageStartTime = new Date();
      device.status = 'in_use';
      saveData();
      saveDeviceState();
      
      io.to(device.socketId).emit('usage_time_set', {
        minutes,
        startTime: device.usageStartTime,
        timestamp: new Date()
      });
      
      io.emit('device_status_update', {
        deviceId,
        status: 'in_use',
        usageTimeLimit: minutes,
        usageStartTime: device.usageStartTime
      });
      
      console.log(`Tempo de uso definido para ${deviceId}: ${minutes} minutos`);
      
      // Agendar bloqueio automático
      setTimeout(() => {
        const currentDevice = connectedDevices.get(deviceId);
        if (currentDevice && currentDevice.status === 'in_use') {
          io.to(currentDevice.socketId).emit('device_locked', {
            reason: 'Tempo de uso expirado',
            timestamp: new Date()
          });
          
          currentDevice.status = 'locked';
          currentDevice.lockReason = 'Tempo de uso expirado';
          currentDevice.lockedAt = new Date();
          saveData();
          
          io.emit('device_status_update', {
            deviceId,
            status: 'locked',
            lockReason: 'Tempo de uso expirado',
            lockedAt: currentDevice.lockedAt
          });
          
          console.log(`Dispositivo ${deviceId} bloqueado automaticamente - tempo expirado`);
        }
      }, minutes * 60 * 1000);
    }
  });

  // Dispositivo desconectando
  socket.on('disconnect', () => {
    for (const [deviceId, device] of connectedDevices.entries()) {
      if (device.socketId === socket.id) {
        connectedDevices.delete(deviceId);
        
        console.log(`Dispositivo desconectado: ${device.deviceName} (${deviceId})`);
        
        io.emit('device_disconnected', {
          deviceId,
          deviceName: device.deviceName,
          timestamp: new Date()
        });
        break;
      }
    }
    
    connectedDashboards.delete(socket.id);
  });
});

// ==================== INICIALIZAÇÃO ====================

// Carregar estado de dispositivos salvo
const savedDeviceState = loadDeviceState();
for (const [deviceId, state] of Object.entries(savedDeviceState)) {
  connectedDevices.set(deviceId, {
    ...state,
    socketId: null, // Será atualizado quando o dispositivo reconectar
    lastUpdate: new Date(state.connectedAt)
  });
  console.log(`Estado do dispositivo ${deviceId} carregado: ${state.status}`);
}

// Criar usuário admin padrão se não existir
if (!db.usuarios.find(u => u.username === 'admin')) {
  db.usuarios.push({
    id: 1,
    username: 'admin',
    password: bcrypt.hashSync('admin123', 10),
    role: 'admin',
    createdAt: new Date()
  });
  saveData();
  console.log('Usuário admin criado: admin / admin123');
}

const PORT = process.env.PORT || 3001;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
  console.log(`WebSocket Server rodando em http://${HOST}:${PORT}`);
  console.log(`Origens CORS permitidas: ${allowedOrigins.join(', ')}`);
  console.log('Persistência de dados ativada: data.json');
});
