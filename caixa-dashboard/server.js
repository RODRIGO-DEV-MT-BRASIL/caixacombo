const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const app = express();

app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

// ==================== DADOS EM MEMÓRIA (SEM ARQUIVOS) ====================
const db = {
  produtos: [],
  categorias: [],
  vendas: [],
  operacoes: [],
  usuarios: [
    { id: 1, username: 'rodrigodevmt', password: bcrypt.hashSync('1985', 10), role: 'admin' }
  ],
  dispositivos: [],
  auditoria: [] // Novo sistema de auditoria
};

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
  
  // Notificar dashboards sobre novo log
  io.emit('auditoria_update', log);
  
  console.log(`[AUDITORIA] ${tipo.toUpperCase()}: ${deviceId} - ${detalhes}`);
}

// ==================== CRIAR SERVER E IO ====================
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: true, credentials: true },
  transports: ["websocket", "polling"]
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
  
  const user = db.usuarios.find(u => u.username === username);
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

// ==================== WEBSOCKET ====================
io.on('connection', (socket) => {
  console.log('🔌 Socket:', socket.id);

  socket.on('device_connect', (data) => {
    const { deviceId, deviceName, deviceType, serialNumber } = data;
    
    // Auditoria: Conexão
    addAuditoria('conexao', deviceId, `Dispositivo conectado - ${deviceType} (${serialNumber})`);
    
    const existing = connectedDevices.get(deviceId);
    if (existing && existing.socketId && existing.socketId !== socket.id) {
      const oldSocket = io.sockets.sockets.get(existing.socketId);
      if (oldSocket) oldSocket.disconnect();
    }

    connectedDevices.set(deviceId, {
      socketId: socket.id,
      deviceName: deviceName || 'Dispositivo',
      deviceType: deviceType || 'Android',
      serialNumber: serialNumber || deviceId,
      connectedAt: new Date(),
      status: (existing && existing.status === 'locked') ? 'locked' : 'online',
      lockPassword: (existing && existing.lockPassword) ? existing.lockPassword : Math.floor(100000 + Math.random() * 900000).toString()
    });

    console.log(`📱 ${deviceName} (${deviceId}) [${connectedDevices.get(deviceId).status}]`);
    io.emit('device_connected', { deviceId, ...connectedDevices.get(deviceId), online: true });
  });

  socket.on('device_status', (data) => {
    const { deviceId, status } = data;
    const device = connectedDevices.get(deviceId);
    if (device) {
      const statusAnterior = device.status;
      device.status = status;
      
      // Auditoria: Mudança de status
      if (statusAnterior !== status) {
        addAuditoria('mudanca_status', deviceId, `Status alterado: ${statusAnterior} → ${status}`);
      }
      
      io.emit('device_status_update', { deviceId, status });
    }
  });

  // Dispositivo confirmando desbloqueio via terminal
  socket.on('unlock_confirmed', (data) => {
    console.log('🔓 [DEBUG] Evento unlock_confirmed recebido:', data);
    const { deviceId } = data;
    const device = connectedDevices.get(deviceId);
    if (device) {
      console.log(`🔓 Desbloqueio confirmado pelo dispositivo: ${deviceId}`);
      device.status = 'online';
      delete device.lockReason;
      delete device.lockedAt;
      
      // Auditoria: Desbloqueio via terminal
      addAuditoria('desbloqueio', deviceId, 'Desbloqueado via terminal do dispositivo');
      
      io.emit('device_status_update', { deviceId, status: 'online' });
    } else {
      console.log(`❌ [DEBUG] Dispositivo não encontrado para unlock_confirmed: ${deviceId}`);
    }
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
    }
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
      } catch (err) {
        connectedDashboards.set(socket.id, { usuario: 'dashboard', socketId: socket.id });
      }
    }
    
    socket.emit('devices_list', Array.from(connectedDevices.entries()).map(([id, d]) => ({ deviceId: id, ...d, online: d.socketId !== null })));
  });

  socket.on('lock_device', (data) => {
    const { deviceId, reason } = data;
    const device = connectedDevices.get(deviceId);
    const dashboardInfo = connectedDashboards.get(socket.id);
    
    if (device) {
      device.status = 'locked';
      device.lockReason = reason;
      if (device.socketId) io.to(device.socketId).emit('device_locked', { reason, lockPassword: device.lockPassword });
      io.emit('device_status_update', { deviceId, status: 'locked' });
      
      // Auditoria: Bloqueio via dashboard
      addAuditoria('bloqueio', deviceId, `Bloqueado: ${reason}`, dashboardInfo?.usuario);
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
