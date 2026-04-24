package com.seucaixa.caixacombo.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

/**
 * Serviço WebSocket para comunicação em tempo real com o Dashboard CaixaCombo.
 * 
 * Configuração:
 * - Altere SOCKET_URL para o IP do seu servidor
 * - O serviço conecta automaticamente ao iniciar
 * - Envia dados de vendas e status do dispositivo
 */
class WebSocketService : Service() {

    companion object {
        private const val TAG = "WebSocketService"
        
        // ==================== CONFIGURAÇÃO DO SERVIDOR ====================
        // Altere para o IP do seu servidor WebSocket
        // Exemplos: 
        //   - Local: "http://localhost:3001"
        //   - Rede local: "http://192.168.1.100:3001"
        //   - Produção: "https://seu-servidor.com"
        private var SOCKET_URL = "http://192.168.1.154:3001"
        
        // Token de autenticação (obtido via login no dashboard)
        private var authToken: String? = null
        
        // ID único do dispositivo
        private var wsDeviceId: String? = null
        private var wsDeviceName: String? = null
        private var wsSerialNumber: String? = null
        
        // Instância do socket
        private var socket: Socket? = null
        
        // Callbacks
        private var onConnectionChange: ((Boolean) -> Unit)? = null
        private var onCommandReceived: ((String, JSONObject?) -> Unit)? = null
        private var onDataRequested: (() -> Unit)? = null
        private var onLockPasswordReceived: ((String) -> Unit)? = null
        
        /**
         * Configura a URL do servidor WebSocket
         */
        fun configureServer(url: String) {
            SOCKET_URL = url
        }
        
        /**
         * Define o token de autenticação
         */
        fun setAuthToken(token: String) {
            authToken = token
        }
        
        /**
         * Define informações do dispositivo
         */
        fun setDeviceInfo(id: String, name: String, serialNumber: String? = null) {
            wsDeviceId = id
            wsDeviceName = name
            wsSerialNumber = serialNumber
        }
        
        /**
         * Define callbacks
         */
        fun setCallbacks(
            onConnectionChange: ((Boolean) -> Unit)?,
            onCommandReceived: ((String, JSONObject?) -> Unit)?,
            onDataRequested: (() -> Unit)?,
            onLockPasswordReceived: ((String) -> Unit)? = null
        ) {
            this.onConnectionChange = onConnectionChange
            this.onCommandReceived = onCommandReceived
            this.onDataRequested = onDataRequested
            this.onLockPasswordReceived = onLockPasswordReceived
        }
        
        /**
         * Verifica se está conectado
         */
        fun isConnected(): Boolean {
            return socket?.connected() == true
        }
        
        /**
         * Envia dados de uma venda para o servidor
         */
        fun sendSaleData(sale: JSONObject) {
            wsDeviceId?.let { id ->
                val data = JSONObject().apply {
                    put("deviceId", id)
                    put("sale", sale)
                }
                socket?.emit("sale_data", data)
                Log.d(TAG, "Venda enviada: ${sale.optString("id", "sem id")}")
            }
        }
        
        /**
         * Envia dados do caixa para o servidor
         */
        fun sendCaixaData(caixa: JSONObject) {
            wsDeviceId?.let { id ->
                val data = JSONObject().apply {
                    put("deviceId", id)
                    put("caixa", caixa)
                }
                socket?.emit("caixa_data", data)
                Log.d(TAG, "Dados do caixa enviados")
            }
        }
        
        /**
         * Envia status do dispositivo
         */
        fun sendDeviceStatus(status: String) {
            wsDeviceId?.let { id ->
                val data = JSONObject().apply {
                    put("deviceId", id)
                    put("status", status)
                }
                socket?.emit("device_status", data)
                Log.d(TAG, "Status enviado: $status")
            }
        }
    }
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeSocket()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connect()
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
    
    /**
     * Inicializa o socket WebSocket
     */
    private fun initializeSocket() {
        try {
            val options = IO.Options().apply {
                // Configurar reconexão automática com delays maiores
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 5000
                reconnectionDelayMax = 30000
                timeout = 60000

                // Usar apenas polling para evitar erros de WebSocket
                transports = arrayOf("polling")
            }
            
            socket = IO.socket(SOCKET_URL, options)
            
            // Eventos de conexão
            socket?.on(Socket.EVENT_CONNECT, onConnect)
            socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
            socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            
            // Eventos do servidor
            socket?.on("execute_command", onExecuteCommand)
            socket?.on("request_data", onRequestData)
            socket?.on("request_sync", onRequestSync)
            socket?.on("auth_error", onAuthError)

            // Eventos de controle de app
            socket?.on("app_control", onAppControl)

            // Eventos de bloqueio e tempo de uso
            socket?.on("device_locked", onDeviceLocked)
            socket?.on("device_unlocked", onDeviceUnlocked)
            socket?.on("usage_time_set", onUsageTimeSet)
            
            Log.d(TAG, "Socket inicializado: $SOCKET_URL")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar socket", e)
        }
    }
    
    /**
     * Conecta ao servidor WebSocket
     */
    fun connect() {
        if (socket?.connected() != true) {
            socket?.connect()
            Log.d(TAG, "Conectando ao servidor...")
        }
    }
    
    /**
     * Desconecta do servidor WebSocket
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        Log.d(TAG, "Desconectado do servidor")
    }
    
    // ==================== EVENTOS DO SOCKET ====================
    
    private val onConnect = Emitter.Listener {
        Log.d(TAG, "Conectado ao servidor WebSocket")
        onConnectionChange?.invoke(true)

        // Enviar informações do dispositivo ao conectar
        wsDeviceId?.let { id ->
            wsDeviceName?.let { name ->
                val data = JSONObject().apply {
                    put("deviceId", id)
                    put("deviceName", name)
                    put("deviceType", "Android")
                    wsSerialNumber?.let { serial ->
                        put("serialNumber", serial)
                    }
                    authToken?.let { token ->
                        put("token", token)
                    }
                }
                socket?.emit("device_connect", data)
                Log.d(TAG, "Enviado device_connect: $id - $name - Serial: $wsSerialNumber")
            }
        }
    }
    
    private val onDisconnect = Emitter.Listener {
        Log.d(TAG, "Desconectado do servidor WebSocket")
        onConnectionChange?.invoke(false)
    }
    
    private val onConnectError = Emitter.Listener { args ->
        val error = if (args.isNotEmpty()) args[0].toString() else "Erro desconhecido"
        Log.e(TAG, "Erro de conexão: $error")
        onConnectionChange?.invoke(false)
    }
    
    private val onExecuteCommand = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val command = data.getString("command")
                val params = data.optJSONObject("params")
                
                Log.d(TAG, "Comando recebido: $command")
                onCommandReceived?.invoke(command, params)
                
                // Executar comandos predefinidos
                executePredefinedCommand(command, params)
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar comando", e)
            }
        }
    }
    
    private val onRequestData = Emitter.Listener {
        Log.d(TAG, "Solicitação de dados recebida")
        onDataRequested?.invoke()
    }
    
    private val onRequestSync = Emitter.Listener {
        Log.d(TAG, "Solicitação de sincronização recebida")
        onDataRequested?.invoke()
    }
    
    private val onAuthError = Emitter.Listener { args ->
        val error = if (args.isNotEmpty()) {
            (args[0] as JSONObject).optString("error", "Erro de autenticação")
        } else {
            "Erro de autenticação"
        }
        Log.e(TAG, "Erro de autenticação: $error")
    }
    
    private val onDeviceLocked = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val reason = data.optString("reason", "Bloqueado pelo administrador")
                val lockPassword = data.optString("lockPassword", "")
                Log.w(TAG, "Dispositivo bloqueado: $reason - Senha: $lockPassword")
                
                // Notificar MainActivity sobre a senha de bloqueio
                if (lockPassword.isNotEmpty()) {
                    onLockPasswordReceived?.invoke(lockPassword)
                }
                
                // Notificar MainActivity para bloquear o app
                onCommandReceived?.invoke("lock_device", JSONObject().put("reason", reason))
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar bloqueio", e)
            }
        }
    }
    
    private val onDeviceUnlocked = Emitter.Listener {
        Log.d(TAG, "Dispositivo desbloqueado")
        
        // Notificar MainActivity para desbloquear o app
        onCommandReceived?.invoke("unlock_device", null)
    }
    
    private val onUsageTimeSet = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val minutes = data.optInt("minutes", 0)
                Log.d(TAG, "Tempo de uso definido: $minutes minutos")

                // Notificar MainActivity sobre tempo de uso
                onCommandReceived?.invoke("set_usage_time", JSONObject().put("minutes", minutes))

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar tempo de uso", e)
            }
        }
    }

    private val onAppControl = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val action = data.optString("action", "")
                Log.d(TAG, "Controle de app recebido: $action")

                when (action) {
                    "open_app" -> {
                        // Abrir o app
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                    "close_app" -> {
                        // Fechar o app
                        onCommandReceived?.invoke("close_app", null)
                    }
                    "shutdown_device" -> {
                        // Desligar o dispositivo (requer permissões de root)
                        try {
                            val process = Runtime.getRuntime()
                            process.exec(arrayOf("su", "-c", "reboot -p"))
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao desligar dispositivo", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Ação de controle desconhecida: $action")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar controle de app", e)
            }
        }
    }
    
    // ==================== MÉTODOS DE ENVIO ====================
    
    fun sendLockConfirmed() {
        val deviceId = wsDeviceId ?: return
        if (deviceId.isNotEmpty() && socket?.connected() == true) {
            socket?.emit("lock_confirmed", JSONObject().put("deviceId", deviceId))
            Log.d(TAG, "Confirmação de bloqueio enviada: $deviceId")
        }
    }
    
    fun sendUnlockConfirmed() {
        val deviceId = wsDeviceId ?: return
        if (deviceId.isNotEmpty() && socket?.connected() == true) {
            socket?.emit("unlock_confirmed", JSONObject().put("deviceId", deviceId))
            Log.d(TAG, "Confirmação de desbloqueio enviada: $deviceId")
        }
    }
    
    // ==================== COMANDOS PREDEFINIDOS ====================
    
    private fun executePredefinedCommand(command: String, params: JSONObject?) {
        when (command) {
            "atualizar" -> {
                // Solicitar atualização de dados
                onDataRequested?.invoke()
            }
            "reiniciar_app" -> {
                // Reiniciar o aplicativo
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            "sincronizar" -> {
                // Forçar sincronização
                onDataRequested?.invoke()
            }
            else -> {
                Log.w(TAG, "Comando desconhecido: $command")
            }
        }
    }
}
