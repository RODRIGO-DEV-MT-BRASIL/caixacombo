package com.seucaixa.caixacombo.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException

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
        //   - Local: "http://localhost:3000"
        //   - Rede local: "http://192.168.1.100:3000"
        //   - Produção: "https://seu-servidor.com"
        private var SOCKET_URL = "http://192.168.1.154:3001"
        
        // Token de autenticação (obtido via login no dashboard)
        private var authToken: String? = null
        
        // ID único do dispositivo
        private var wsDeviceId: String? = null
        private var wsDeviceName: String? = null
        private var wsSerialNumber: String? = null
        
        // Admin para reboot sem root
        private var devicePolicyManager: android.app.admin.DevicePolicyManager? = null
        private var adminComponentName: android.content.ComponentName? = null
        
        // Instância do socket
        private var socket: Socket? = null
        
        // Callbacks
        private var onConnectionChange: ((Boolean) -> Unit)? = null
        private var onCommandReceived: ((String, JSONObject?) -> Unit)? = null
        private var onDataRequested: (() -> Unit)? = null
        private var onLockPasswordReceived: ((String) -> Unit)? = null
        private var onProdutosReceived: ((JSONArray) -> Unit)? = null
        
        /**
         * Configura o Admin para permitir reboot sem root
         */
        fun setAdminInfo(dpm: android.app.admin.DevicePolicyManager, cn: android.content.ComponentName) {
            devicePolicyManager = dpm
            adminComponentName = cn
        }
        
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
            onLockPasswordReceived: ((String) -> Unit)? = null,
            onProdutosReceived: ((JSONArray) -> Unit)? = null
        ) {
            this.onConnectionChange = onConnectionChange
            this.onCommandReceived = onCommandReceived
            this.onDataRequested = onDataRequested
            this.onLockPasswordReceived = onLockPasswordReceived
            this.onProdutosReceived = onProdutosReceived
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
        
        /**
         * Envia atualizações de estoque para o servidor
         */
        fun sendEstoqueUpdate(produtoId: Long, novoEstoque: Double) {
            wsDeviceId?.let { id ->
                val data = JSONObject().apply {
                    put("deviceId", id)
                    put("produtoId", produtoId)
                    put("novoEstoque", novoEstoque)
                    put("timestamp", System.currentTimeMillis())
                }
                socket?.emit("estoque_update", data)
                Log.d(TAG, "Estoque atualizado: produtoId=$produtoId, estoque=$novoEstoque")
            }
        }
        
        /**
         * Envia operações de caixa para o servidor via API REST
         */
        fun sendOperacaoCaixa(tipo: String, valor: Double, nomeOperador: String, observacao: String? = null) {
            wsDeviceId?.let { id ->
                Log.d(TAG, "🔓 sendOperacaoCaixa: deviceId=$id, tipo=$tipo, valor=$valor")
                
                // Enviar via API REST em vez de WebSocket
                Thread {
                    try {
                        val url = URL("http://192.168.1.154:3001/api/operacoes")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        
                        val data = JSONObject().apply {
                            put("tipo", tipo)
                            put("valor", valor)
                            put("deviceId", id)
                            put("nomeOperador", nomeOperador)
                            put("observacao", observacao ?: "")
                        }
                        
                        conn.outputStream.use { output ->
                            output.write(data.toString().toByteArray())
                        }
                        
                        val responseCode = conn.responseCode
                        Log.d(TAG, "🔓 API Response: $responseCode")
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d(TAG, "🔓 Operação de caixa enviada via API: $tipo R$$valor")
                        } else {
                            Log.e(TAG, "❌ Erro ao enviar operação: $responseCode")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erro na API de operações", e)
                    }
                }.start()
            } ?: run {
                Log.e(TAG, "❌ sendOperacaoCaixa: wsDeviceId é null!")
            }
        }
        
        /**
         * Envia produtos para sincronização com o servidor
         */
        fun sendProdutosSync(produtosJson: JSONArray) {
            wsDeviceId?.let { id ->
                Log.d(TAG, "🔄 sendProdutosSync: deviceId=$id, produtos=${produtosJson.length()}")
                
                Thread {
                    try {
                        val url = URL("http://192.168.1.154:3001/api/produtos/sync")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        
                        val data = JSONObject().apply {
                            put("deviceId", id)
                            put("produtos", produtosJson)
                        }
                        
                        conn.outputStream.use { output ->
                            output.write(data.toString().toByteArray())
                        }
                        
                        val responseCode = conn.responseCode
                        Log.d(TAG, "🔄 Sync Response: $responseCode")
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d(TAG, "✅ Produtos sincronizados com sucesso: ${produtosJson.length()}")
                        } else {
                            Log.e(TAG, "❌ Erro ao sincronizar produtos: $responseCode")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erro na sincronização de produtos", e)
                    }
                }.start()
            } ?: run {
                Log.e(TAG, "❌ sendProdutosSync: wsDeviceId é null!")
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

                // Usar apenas polling para evitar problemas de websocket em alguns terminais
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
            socket?.on("control_command", onControlCommand)
            socket?.on("produtos_sync", onProdutosSync)

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
    
    private val onProdutosSync = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val produtos = data.getJSONArray("produtos")
                Log.d(TAG, "Recebidos ${produtos.length()} produtos do servidor")
                onProdutosReceived?.invoke(produtos)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar produtos_sync", e)
            }
        }
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
    
    private val onControlCommand = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            try {
                val data = args[0] as JSONObject
                val action = data.getString("action")
                Log.d(TAG, "Comando de controle: $action para $wsDeviceId")

                when (action) {
                    "restart" -> {
                        Log.d(TAG, "Reiniciando dispositivo")
                        val success = restartDevice()
                        sendControlResult(action, success, if (!success) "Sem permissões de Admin ou Root" else null)
                    }
                    "shutdown" -> {
                        Log.d(TAG, "Desligando dispositivo")
                        val success = shutdownDevice()
                        sendControlResult(action, success, if (!success) "Sem permissões de Root" else null)
                    }
                    "open_app" -> {
                        Log.d(TAG, "Abrindo app")
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            sendControlResult(action, true, null)
                        } else {
                            sendControlResult(action, false, "Intent não encontrado")
                        }
                    }
                    "close_app" -> {
                        Log.d(TAG, "Fechando app")
                        onCommandReceived?.invoke("close_app", null)
                        sendControlResult(action, true, null)
                    }
                    else -> {
                        Log.w(TAG, "Comando desconhecido: $action")
                        sendControlResult(action, false, "Comando desconhecido")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar comando", e)
                sendControlResult("unknown", false, "Erro: ${e.message}")
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

    /**
     * Envia resultado de execução de comando de controle para o dashboard
     */
    private fun sendControlResult(action: String, success: Boolean, error: String? = null) {
        val deviceId = wsDeviceId ?: return
        if (deviceId.isNotEmpty() && socket?.connected() == true) {
            val data = JSONObject().apply {
                put("deviceId", deviceId)
                put("action", action)
                put("success", success)
                error?.let { put("error", it) }
            }
            socket?.emit("control_result", data)
            Log.d(TAG, "Resultado de controle enviado: $action - sucesso=$success ${error?.let { "- erro: $it" } ?: ""}")
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

    /**
     * Tenta reiniciar o hardware do dispositivo de verdade.
     * 1. DevicePolicyManager (Requer que o app seja Administrador do Dispositivo)
     * 2. APIs de fabricantes POS (Gertec, PAX, SUNMI)
     * 3. Intent de sistema ACTION_REBOOT
     * 4. Comando su (Requer Root)
     * @return true se o comando foi enviado com sucesso, false caso contrário
     */
    private fun restartDevice(): Boolean {
        try {
            // Método 1: DevicePolicyManager (Funciona sem root se for Admin)
            if (devicePolicyManager != null && adminComponentName != null) {
                try {
                    Log.d(TAG, "SOLICITANDO REBOOT VIA ADMIN API: $adminComponentName")
                    devicePolicyManager?.reboot(adminComponentName!!)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao usar Admin API para reboot: ${e.message}")
                }
            }

            // Método 2: Intent de sistema para reiniciar (funciona em alguns dispositivos com permissão REBOOT)
            try {
                Log.d(TAG, "SOLICITANDO REBOOT VIA INTENT DE SISTEMA")
                val intent = Intent("android.intent.action.REBOOT")
                intent.putExtra("nowait", 1)
                intent.putExtra("interval", 1)
                intent.putExtra("window", 0)
                sendBroadcast(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Intent de reboot não suportado: ${e.message}")
            }

            // Método 3: APIs de fabricantes POS
            if (restartViaPOSAPIs()) {
                return true
            }

            // Método 4: Comando su (Para dispositivos com root)
            try {
                Log.d(TAG, "SOLICITANDO REBOOT VIA COMANDO SU (ROOT)")
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Dispositivo sem root ou permissão negada para comando su: ${e.message}")
            }

            Log.e(TAG, "ERRO CRÍTICO: NÃO FOI POSSÍVEL REINICIAR O HARDWARE. SEM PRIVILÉGIOS DE ADMIN OU ROOT.")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Erro geral ao tentar reiniciar dispositivo", e)
            return false
        }
    }

    /**
     * Tenta reiniciar usando APIs específicas de fabricantes POS
     * (Gertec, PAX, SUNMI, etc.)
     */
    private fun restartViaPOSAPIs(): Boolean {
        // Gertec APIs
        try {
            val gertecIntent = Intent("com.gertec.action.REBOOT")
            sendBroadcast(gertecIntent)
            Log.d(TAG, "Reboot via API Gertec solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API Gertec não disponível: ${e.message}")
        }

        // PAX APIs
        try {
            val paxIntent = Intent("com.pax.action.REBOOT")
            sendBroadcast(paxIntent)
            Log.d(TAG, "Reboot via API PAX solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API PAX não disponível: ${e.message}")
        }

        // SUNMI APIs
        try {
            val sunmiIntent = Intent("com.sunmi.action.REBOOT")
            sendBroadcast(sunmiIntent)
            Log.d(TAG, "Reboot via API SUNMI solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API SUNMI não disponível: ${e.message}")
        }

        // Intent genérico via sistema
        try {
            val sysIntent = Intent(Intent.ACTION_REBOOT)
            sysIntent.putExtra("nowait", 1)
            sendBroadcast(sysIntent)
            Log.d(TAG, "Reboot via Intent.ACTION_REBOOT solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Intent ACTION_REBOOT não permitido: ${e.message}")
        }

        return false
    }

    /**
     * Tenta desligar o dispositivo usando múltiplos métodos
     * 1. APIs de fabricantes POS (Gertec, PAX, SUNMI)
     * 2. PowerManager (requer permissões de sistema)
     * 3. Comando su -c reboot -p (requer root)
     * @return true se o comando foi enviado com sucesso, false caso contrário
     */
    private fun shutdownDevice(): Boolean {
        try {
            // Método 1: APIs de fabricantes POS
            if (shutdownViaPOSAPIs()) {
                return true
            }

            // Método 2: PowerManager (funciona em dispositivos com permissões de sistema)
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                // shutdown() não é público na API, mas pode funcionar em alguns dispositivos
                try {
                    val shutdownMethod = powerManager.javaClass.getMethod("shutdown", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                    shutdownMethod.invoke(powerManager, false, true)
                    Log.d(TAG, "Desligando dispositivo via PowerManager")
                    return true
                } catch (e: NoSuchMethodException) {
                    Log.w(TAG, "Método shutdown não disponível no PowerManager")
                }
            } catch (e: Exception) {
                Log.w(TAG, "PowerManager não suportado: ${e.message}")
            }

            // Método 3: Comando su -c reboot -p (requer root)
            try {
                val process = Runtime.getRuntime()
                process.exec(arrayOf("su", "-c", "reboot -p"))
                Log.d(TAG, "Desligando dispositivo via su -c reboot -p")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Comando su não suportado (sem root): ${e.message}")
            }

            Log.e(TAG, "Não foi possível desligar o dispositivo (requer root ou permissões de sistema)")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tentar desligar dispositivo", e)
            return false
        }
    }

    /**
     * Tenta desligar usando APIs específicas de fabricantes POS
     * (Gertec, PAX, SUNMI, etc.)
     */
    private fun shutdownViaPOSAPIs(): Boolean {
        // Gertec APIs
        try {
            val gertecIntent = Intent("com.gertec.action.SHUTDOWN")
            sendBroadcast(gertecIntent)
            Log.d(TAG, "Shutdown via API Gertec solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API Gertec shutdown não disponível: ${e.message}")
        }

        // PAX APIs
        try {
            val paxIntent = Intent("com.pax.action.SHUTDOWN")
            sendBroadcast(paxIntent)
            Log.d(TAG, "Shutdown via API PAX solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API PAX shutdown não disponível: ${e.message}")
        }

        // SUNMI APIs
        try {
            val sunmiIntent = Intent("com.sunmi.action.SHUTDOWN")
            sendBroadcast(sunmiIntent)
            Log.d(TAG, "Shutdown via API SUNMI solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "API SUNMI shutdown não disponível: ${e.message}")
        }

        // Intent genérico via sistema
        try {
            val sysIntent = Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN")
            sysIntent.putExtra("android.intent.extra.KEY_CONFIRM", false)
            sysIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(sysIntent)
            Log.d(TAG, "Shutdown via Intent.ACTION_REQUEST_SHUTDOWN solicitado")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Intent ACTION_REQUEST_SHUTDOWN não permitido: ${e.message}")
        }

        return false
    }
}
