// Script para simular um dispositivo Android conectando ao Dashboard
const { io } = require('socket.io-client');

const socket = io('http://localhost:3001', {
  transports: ['websocket', 'polling']
});

socket.on('connect', () => {
  console.log('✅ Conectado ao servidor WebSocket');
  
  // Registrar dispositivo
  socket.emit('device_connect', {
    deviceId: 'device-test-001',
    deviceName: 'Caixa Teste 01',
    deviceType: 'Android'
  });
  
  console.log('📱 Dispositivo registrado: Caixa Teste 01');
});

socket.on('device_connected', (data) => {
  console.log('🔔 Confirmação de conexão:', data);
});

socket.on('execute_command', (data) => {
  console.log('📨 Comando recebido:', data.command);
});

socket.on('request_data', () => {
  console.log('📊 Solicitação de dados recebida');
  
  // Simular envio de dados
  socket.emit('sale_data', {
    deviceId: 'device-test-001',
    sale: {
      id: Date.now(),
      total: 150.00,
      formaPagamento: 'DINHEIRO',
      itens: [{ produto: 'Coca-Cola 2L', quantidade: 2, preco: 8.99 }]
    }
  });
});

// Enviar status a cada 10 segundos
setInterval(() => {
  socket.emit('device_status', {
    deviceId: 'device-test-001',
    status: 'online'
  });
  console.log('💚 Status enviado: online');
}, 10000);

// Manter rodando
process.on('SIGINT', () => {
  console.log('\n👋 Desconectando...');
  socket.disconnect();
  process.exit(0);
});

console.log('🔄 Aguardando conexão...');
