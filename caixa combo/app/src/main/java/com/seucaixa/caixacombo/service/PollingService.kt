package com.seucaixa.caixacombo.service

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.util.Log
import com.seucaixa.caixacombo.MainActivity
import com.seucaixa.caixacombo.R
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Serviço de polling REST para comunicação com o Dashboard CaixaCombo.
 * Substitui WebSocket (proibido pela norma PCI para POS).
 * 
 * O dispositivo faz POST /api/device/poll a cada 15 segundos,
 * enviando status e recebendo comandos pendentes do dashboard.
 */
class PollingService : Service() {

    companion object {
        private const val TAG = "PollingService"
        private const val POLL_INTERVAL_MS = 15_000L // 15 segundos
        private const val MAX_RETRIES = 5
        private const val PREFS_NAME = "server_config"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "https://caixa-dashboard-mt-novo.onrender.com"

        private var SERVER_URL = DEFAULT_SERVER_URL
        private var pollingDeviceId: String? = null
        private var deviceName: String? = null
        private var serialNumber: String? = null
        
        // Empresa aprovada - armazenada quando terminal é aprovado
        private var approvedEmpresaId: String? = null

        // Admin para reboot
        private var devicePolicyManager: android.app.admin.DevicePolicyManager? = null
        private var adminComponentName: android.content.ComponentName? = null

        // Callbacks
        private var onConnectionChange: ((Boolean) -> Unit)? = null
        private var onCommandReceived: ((String, JSONObject?) -> Unit)? = null
        private var onDataRequested: (() -> Unit)? = null
        private var onProdutosReceived: ((JSONArray) -> Unit)? = null
        private var onLockPasswordReceived: ((String) -> Unit)? = null
        private var onUnlockResponse: ((Boolean, String?) -> Unit)? = null
        private var onReprintRequested: ((String?) -> Unit)? = null      // atk da venda
        private var onCancelRequested: ((String, Long?) -> Unit)? = null  // atk, amount em centavos
        private var onClientesReceived: ((JSONArray) -> Unit)? = null     // clientes do servidor
        private var onCategoriasReceived: ((JSONArray) -> Unit)? = null   // categorias do servidor
        private var onTerminalApproved: ((String, String?) -> Unit)? = null // (companyId, companyName) callback

        fun setOnApprovalStatus(cb: ((Boolean, String?, String?) -> Unit)?) {
            onApprovalStatus = cb
        }

        fun setOnTerminalApproved(cb: ((String, String?) -> Unit)?) {
            onTerminalApproved = cb
        }
        private var onEmpresasReceived: ((JSONArray) -> Unit)? = null     // empresas do servidor
        private var onSyncComplete: ((Int, Int, Int) -> Unit)? = null     // produtos, categorias, clientes
        private var onApprovalStatus: ((Boolean, String?, String?) -> Unit)? = null  // approved, status, empresaId
        private var onEmpresaConfig: ((JSONObject) -> Unit)? = null       // empresa config (whitelabel)
        private var onFuncionariosReceived: ((JSONArray) -> Unit)? = null  // funcionarios do servidor
        private var onPrintConfigReceived: ((JSONObject) -> Unit)? = null

        private var isRunning = false
        private var consecutiveErrors = 0
        private var pendingCaixaData: JSONObject? = null
        private var needsProductSync = true // Solicitar sync de produtos na primeira conexão
        private var wasDisconnected = false  // Rastrear se estava desconectado para detectar reconexão
        private var terminalToken: String? = null // Token de autenticação do terminal
        private var terminalCompanyId: String? = null // companyId do terminal

        fun configureServer(url: String) {
            SERVER_URL = url.trimEnd('/')
        }

        fun configureServer(context: android.content.Context, url: String) {
            SERVER_URL = url.trimEnd('/')
            context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_SERVER_URL, SERVER_URL).apply()
        }

        fun loadServerUrl(context: android.content.Context) {
            val saved = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_SERVER_URL, null)
            if (saved != null) {
                SERVER_URL = saved.trimEnd('/')
            }
        }

        fun getServerUrl(): String = SERVER_URL
        fun getDeviceId(): String? = pollingDeviceId
        fun getApprovedEmpresaId(): String? = approvedEmpresaId
        
        fun setApprovedEmpresaId(empresaId: String?) {
            approvedEmpresaId = empresaId
            Log.d(TAG, "Empresa aprovada configurada: $empresaId")
        }
        
        fun setDeviceInfo(id: String, name: String, serial: String? = null) {
            pollingDeviceId = id
            deviceName = name
            serialNumber = serial
        }

        fun setAdminInfo(dpm: android.app.admin.DevicePolicyManager, cn: android.content.ComponentName) {
            devicePolicyManager = dpm
            adminComponentName = cn
        }

        fun setCallbacks(
            onConnectionChange: ((Boolean) -> Unit)?,
            onCommandReceived: ((String, JSONObject?) -> Unit)?,
            onDataRequested: (() -> Unit)?,
            onLockPasswordReceived: ((String) -> Unit)? = null,
            onProdutosReceived: ((JSONArray) -> Unit)? = null,
            onUnlockResponse: ((Boolean, String?) -> Unit)? = null,
            onReprintRequested: ((String?) -> Unit)? = null,
            onCancelRequested: ((String, Long?) -> Unit)? = null,
            onClientesReceived: ((JSONArray) -> Unit)? = null,
            onCategoriasReceived: ((JSONArray) -> Unit)? = null,
            onEmpresasReceived: ((JSONArray) -> Unit)? = null,
            onSyncComplete: ((Int, Int, Int) -> Unit)? = null,
            onApprovalStatus: ((Boolean, String?, String?) -> Unit)? = null,
            onEmpresaConfig: ((JSONObject) -> Unit)? = null,
            onFuncionariosReceived: ((JSONArray) -> Unit)? = null,
            onPrintConfigReceived: ((JSONObject) -> Unit)? = null
        ) {
            this.onConnectionChange = onConnectionChange
            this.onCommandReceived = onCommandReceived
            this.onDataRequested = onDataRequested
            this.onLockPasswordReceived = onLockPasswordReceived
            this.onProdutosReceived = onProdutosReceived
            this.onUnlockResponse = onUnlockResponse
            this.onReprintRequested = onReprintRequested
            this.onCancelRequested = onCancelRequested
            this.onClientesReceived = onClientesReceived
            this.onCategoriasReceived = onCategoriasReceived
            this.onEmpresasReceived = onEmpresasReceived
            this.onSyncComplete = onSyncComplete
            this.onApprovalStatus = onApprovalStatus
            this.onEmpresaConfig = onEmpresaConfig
            this.onFuncionariosReceived = onFuncionariosReceived
            this.onPrintConfigReceived = onPrintConfigReceived
        }

        fun isConnected(): Boolean = isRunning && consecutiveErrors < MAX_RETRIES

        /** Forçar solicitação de sync de produtos no próximo poll */
        fun requestProductSync() {
            needsProductSync = true
            Log.d(TAG, "requestProductSync: flag needsProductSync ativada")
        }

        // ==================== MÉTODOS DE ENVIO (REST) ====================

        fun sendSaleData(sale: JSONObject) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/sale")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("sale", sale)
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    val code = conn.responseCode
                    System.out.println("[SALE] Venda enviada: code=$code, deviceId=$id, url=$SERVER_URL/api/device/sale")
                } catch (e: Exception) {
                    System.out.println("[SALE] Erro ao enviar venda: ${e.message}")
                }
            }
        }

        fun sendCaixaData(caixa: JSONObject) {
            pendingCaixaData = caixa
            Log.d(TAG, "Caixa data armazenada para envio no próximo poll")
        }

        fun sendDeviceStatus(status: String) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/status")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("status", status)
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    Log.d(TAG, "Status enviado: $status")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar status", e)
                }
            }
        }

        fun sendEstoqueUpdate(produtoId: Long, novoEstoque: Double) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/estoque")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("produtoId", produtoId)
                        put("novoEstoque", novoEstoque)
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    Log.d(TAG, "Estoque atualizado: produtoId=$produtoId")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar estoque", e)
                }
            }
        }

        fun sendOperacaoCaixa(tipo: String, valor: Double, nomeOperador: String, observacao: String? = null) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/operacao")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("tipo", tipo)
                        put("valor", valor)
                        put("deviceId", id)
                        put("nomeOperador", nomeOperador)
                        put("observacao", observacao ?: "")
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    val code = conn.responseCode
                    Log.d(TAG, "Operação caixa enviada: code=$code")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar operação caixa", e)
                }
            }
        }

        fun sendProdutosSync(produtosJson: JSONArray) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/produtos-sync")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("produtos", produtosJson)
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    Log.d(TAG, "Produtos sincronizados: ${produtosJson.length()}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar produtos", e)
                }
            }
        }

        fun sendLockConfirmed() {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/lock-confirmed")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply { put("deviceId", id) }
                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    Log.d(TAG, "Bloqueio confirmado")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao confirmar bloqueio", e)
                }
            }
        }

        fun sendUnlockConfirmed() {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/unlock-confirmed")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply { put("deviceId", id) }
                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                    Log.d(TAG, "Desbloqueio confirmado")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao confirmar desbloqueio", e)
                }
            }
        }

        fun sendUnlockAttempt(password: String) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/unlock-attempt")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("password", password)
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }

                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", "")

                    onUnlockResponse?.invoke(success, message)
                    Log.d(TAG, "Tentativa de desbloqueio: success=$success")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao tentar desbloqueio", e)
                    onUnlockResponse?.invoke(false, "Erro de conexão")
                }
            }
        }

        fun sendControlResult(action: String, success: Boolean, error: String? = null) {
            val id = pollingDeviceId ?: return
            thread {
                try {
                    val url = URL("$SERVER_URL/api/device/control-result")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    val data = JSONObject().apply {
                        put("deviceId", id)
                        put("action", action)
                        put("success", success)
                        error?.let { put("error", it) }
                    }

                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar control result", e)
                }
            }
        }
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PollingService = this@PollingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isRunning = true
        Log.e("POLL_DEBUG", "🎬 onCreate - PollingService criado - deviceId: $pollingDeviceId, URL: $SERVER_URL")
        startForeground(1, createNotification())
        Log.d(TAG, "PollingService iniciado como foreground service")
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("POLL_DEBUG", "🚀 onStartCommand chamado - isRunning: $isRunning, deviceId: $pollingDeviceId, URL: $SERVER_URL")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "polling_service",
                "Sincronização",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sincronização com o dashboard"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "polling_service")
                .setContentTitle("CaixaCombo")
                .setContentText("Sincronizando com dashboard...")
                .setSmallIcon(R.drawable.caixacombo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("CaixaCombo")
                .setContentText("Sincronizando com dashboard...")
                .setSmallIcon(R.drawable.caixacombo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    private fun startPolling() {
        Log.e("POLL_DEBUG", "🔄 startPolling chamado - deviceId: $pollingDeviceId, URL: $SERVER_URL")
        thread(name = "PollingThread") {
            Log.e("POLL_DEBUG", "✅ PollingThread iniciado")
            while (isRunning) {
                try {
                    val syncResult = doPoll()
                    consecutiveErrors = 0
                    // Detectar reconexão após desconexão
                    if (wasDisconnected) {
                        wasDisconnected = false
                        onConnectionChange?.invoke(true)
                        // Se houve sync na reconexão, notificar
                        if (syncResult != null && (syncResult.first > 0 || syncResult.second > 0 || syncResult.third > 0)) {
                            onSyncComplete?.invoke(syncResult.first, syncResult.second, syncResult.third)
                        }
                    } else {
                        onConnectionChange?.invoke(true)
                    }
                } catch (e: Exception) {
                    consecutiveErrors++
                    Log.e("POLL_DEBUG", "❌ Poll erro #$consecutiveErrors: ${e.message}")
                    Log.e(TAG, "Poll erro #$consecutiveErrors: ${e.message}")
                    if (consecutiveErrors >= MAX_RETRIES) {
                        wasDisconnected = true
                        onConnectionChange?.invoke(false)
                    }
                    // Após erro de conexão, marcar que precisa re-sincronizar produtos quando reconectar
                    needsProductSync = true
                }

                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    /** Retorna Triple(produtos, categorias, clientes) se houve sync, ou null */
    private fun doPoll(): Triple<Int, Int, Int>? {
        val id = pollingDeviceId ?: run {
            Log.e("POLL_DEBUG", "❌ deviceId não configurado - polling cancelado")
            return null
        }
        val name = deviceName ?: "Dispositivo"

        Log.d("POLL_DEBUG", "📡 Fazendo poll - deviceId: $id, deviceName: $name, URL: $SERVER_URL")

        val url = URL("$SERVER_URL/api/device/poll")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val data = JSONObject().apply {
            put("deviceId", id)
            put("deviceName", name)
            put("deviceType", "Android")
            put("status", "online")
            serialNumber?.let { put("serialNumber", it) }
            // Incluir dados de caixa pendentes
            pendingCaixaData?.let { put("caixaData", it) }
            // Solicitar sync de produtos se necessário
            if (needsProductSync) {
                put("needsProductSync", true)
            }
        }

        Log.d("POLL_DEBUG", "📤 Enviando: $data")

        // Limpar caixa data após incluir no poll
        pendingCaixaData = null

        conn.outputStream.use { it.write(data.toString().toByteArray()) }

        val responseCode = conn.responseCode
        Log.d("POLL_DEBUG", "📥 Response code: $responseCode")
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "Sem detalhes"
            Log.e("POLL_DEBUG", "❌ Poll falhou - HTTP $responseCode: $errorBody")
            throw Exception("Poll HTTP $responseCode: $errorBody")
        }

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val commands = json.optJSONArray("commands")
        Log.e("SYNC_DEBUG", "Poll OK - commands: ${commands?.length() ?: 0}")
        Log.d("POLL_DEBUG", "✅ Poll response: ${response.take(200)}...")

        var syncProdutos = 0
        var syncCategorias = 0
        var syncClientes = 0
        var hadSync = false

        if (commands != null && commands.length() > 0) {
            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                val command = cmd.optString("command", "")
                val params = cmd.optJSONObject("params")

                Log.d(TAG, "Comando recebido via poll: $command")
                handleCommand(command, params)
                
                // Contar itens sincronizados
                when (command) {
                    "produtos_sync" -> {
                        needsProductSync = false
                        val produtosArray = params?.optJSONArray("produtos")
                        syncProdutos = produtosArray?.length() ?: 0
                        hadSync = true
                        // Log de debug para ver empresaId dos produtos
                        if (produtosArray != null) {
                            val sampleProdutos = (0 until minOf(3, produtosArray.length())).map { j ->
                                val p = produtosArray.getJSONObject(j)
                                "${p.optString("nome", "sem nome")} (emp=${p.optString("empresaId", "null")})"
                            }.joinToString(", ")
                            Log.d("SYNC_DEBUG", "📦 Sync de produtos - empresaId aprovada: $approvedEmpresaId, produtos: $syncProdutos, amostras: $sampleProdutos")
                        }
                        Log.d(TAG, "Sync de produtos recebido ($syncProdutos itens) - flag needsProductSync resetada")
                    }
                    "categorias_sync" -> {
                        syncCategorias = params?.optJSONArray("categorias")?.length() ?: 0
                        hadSync = true
                    }
                    "clientes_sync" -> {
                        syncClientes = params?.optJSONArray("clientes")?.length() ?: 0
                        hadSync = true
                    }
                }
            }
        }
        
        return if (hadSync) Triple(syncProdutos, syncCategorias, syncClientes) else null
    }

    private fun handleCommand(command: String, params: JSONObject?) {
        when (command) {
            "device_locked" -> {
                val reason = params?.optString("reason", "Bloqueado pelo administrador") ?: "Bloqueado"
                val lockPassword = params?.optString("lockPassword", "") ?: ""
                if (lockPassword.isNotEmpty()) {
                    onLockPasswordReceived?.invoke(lockPassword)
                }
                onCommandReceived?.invoke("lock_device", JSONObject().put("reason", reason))
            }
            "device_unlocked" -> {
                onCommandReceived?.invoke("unlock_device", null)
            }
            "usage_time_set" -> {
                val minutes = params?.optInt("minutes", 0) ?: 0
                onCommandReceived?.invoke("set_usage_time", JSONObject().put("minutes", minutes))
            }
            "control_command" -> {
                val action = params?.optString("action", "") ?: ""
                when (action) {
                    "restart" -> {
                        val success = restartDevice()
                        sendControlResult(action, success, if (!success) "Sem permissões" else null)
                    }
                    "shutdown" -> {
                        val success = shutdownDevice()
                        sendControlResult(action, success, if (!success) "Sem permissões" else null)
                    }
                    "open_app" -> {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            sendControlResult(action, true, null)
                        }
                    }
                    "close_app" -> {
                        onCommandReceived?.invoke("close_app", null)
                        sendControlResult(action, true, null)
                    }
                    else -> {
                        onCommandReceived?.invoke(action, params)
                    }
                }
            }
            "execute_command" -> {
                val cmd = params?.optString("command", "") ?: ""
                when (cmd) {
                    "atualizar", "sincronizar" -> onDataRequested?.invoke()
                    "reiniciar_app" -> {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                    else -> onCommandReceived?.invoke(cmd, params)
                }
            }
            "request_data", "request_sync" -> {
                onDataRequested?.invoke()
            }
            "reimprimir_venda" -> {
                val atk = params?.optString("atk", null)
                Log.d(TAG, "Comando reimprimir_venda recebido, atk=$atk")
                onReprintRequested?.invoke(atk)
            }
            "cancelar_venda" -> {
                val atk = params?.optString("atk", "") ?: ""
                val amount = params?.optLong("amount", 0)?.takeIf { it > 0 }
                Log.d(TAG, "Comando cancelar_venda recebido, atk=$atk, amount=$amount")
                if (atk.isNotEmpty()) {
                    onCancelRequested?.invoke(atk, amount)
                } else {
                    Log.w(TAG, "Cancelamento sem ATK - não é possível cancelar via deeplink")
                }
            }
            "produtos_sync" -> {
                val produtos = params?.optJSONArray("produtos")
                Log.e("SYNC_DEBUG", "produtos_sync recebido: ${produtos?.length() ?: 0} produtos")
                if (produtos != null) {
                    onProdutosReceived?.invoke(produtos)
                }
            }
            "clientes_sync" -> {
                val clientes = params?.optJSONArray("clientes")
                if (clientes != null) {
                    Log.d(TAG, "Recebidos ${clientes.length()} clientes do servidor")
                    onClientesReceived?.invoke(clientes)
                }
            }
            "categorias_sync" -> {
                val categorias = params?.optJSONArray("categorias")
                Log.e("SYNC_DEBUG", "categorias_sync recebido: ${categorias?.length() ?: 0} categorias")
                if (categorias != null) {
                    onCategoriasReceived?.invoke(categorias)
                }
            }
            "empresas_sync" -> {
                val empresas = params?.optJSONArray("empresas")
                Log.d(TAG, "empresas_sync recebido: ${empresas?.length() ?: 0} empresas")
                if (empresas != null) {
                    onEmpresasReceived?.invoke(empresas)
                }
            }
            "approval_status" -> {
                val approved = params?.optBoolean("approved", false) ?: false
                val status = params?.optString("status", "pending") ?: "pending"
                val empresaId = params?.optString("empresaId", null)
                Log.d(TAG, "approval_status recebido: approved=$approved, status=$status, empresaId=$empresaId")
                // Salvar empresaId aprovada para usar no sync
                if (approved && empresaId != null) {
                    approvedEmpresaId = empresaId
                    Log.d(TAG, "💾 Empresa salva no PollingService: $empresaId")
                }
                onApprovalStatus?.invoke(approved, status, empresaId)
            }
            "empresa_config" -> {
                Log.d(TAG, "empresa_config recebido: $params")
                if (params != null) {
                    onEmpresaConfig?.invoke(params)
                }
            }
            "empresa_config_updated" -> {
                Log.d(TAG, "empresa_config_updated recebido: $params")
                if (params != null) {
                    onEmpresaConfig?.invoke(params)
                }
            }
            "funcionarios_sync" -> {
                val funcionarios = params?.optJSONArray("funcionarios")
                Log.d(TAG, "funcionarios_sync recebido: ${funcionarios?.length() ?: 0} funcionarios")
                if (funcionarios != null) {
                    onFuncionariosReceived?.invoke(funcionarios)
                }
            }
            "print_config_sync" -> {
                Log.d(TAG, "print_config_sync recebido")
                if (params != null) {
                    onPrintConfigReceived?.invoke(params)
                }
            }
            else -> {
                Log.w(TAG, "Comando desconhecido: $command")
                onCommandReceived?.invoke(command, params)
            }
        }
    }

    // ==================== TERMINAL ACTIVATION & SYNC ====================

    /**
     * Ativa o terminal com código de ativação e recebe companyId + token
     */
    fun activateTerminal(activationCode: String, callback: (Boolean, String?, String?, String?) -> Unit) {
        thread {
            try {
                val serverUrl = SERVER_URL
                if (serverUrl == null) {
                    callback(false, null, null, "Server URL não configurado")
                    return@thread
                }

                val url = URL("$serverUrl/api/terminal/activate")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val data = JSONObject().apply {
                    put("activationCode", activationCode)
                    put("deviceId", pollingDeviceId)
                    put("deviceName", deviceName)
                    put("deviceType", "Android")
                    serialNumber?.let { put("serialNumber", it) }
                }

                conn.outputStream.use { it.write(data.toString().toByteArray()) }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    terminalToken = json.optString("token", null)
                    terminalCompanyId = json.optString("companyId", null)
                    val companyName = json.optString("companyName", "")

                    Log.e("TERMINAL_DEBUG", "✅ Terminal ativado - companyId: $terminalCompanyId, company: $companyName")

                    callback(true, terminalCompanyId, companyName, terminalToken)
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "Erro na ativação"
                    Log.e("TERMINAL_DEBUG", "❌ Falha na ativação: $error")
                    callback(false, null, null, error)
                }
            } catch (e: Exception) {
                Log.e("TERMINAL_DEBUG", "❌ Erro na ativação: ${e.message}")
                callback(false, null, null, e.message)
            }
        }
    }

    /**
     * Sincroniza dados do terminal usando o novo endpoint /api/terminal/sync
     */
    fun syncTerminalData(callback: ((JSONObject?) -> Unit)? = null) {
        thread {
            try {
                val serverUrl = SERVER_URL
                val token = terminalToken
                if (serverUrl == null || token == null) {
                    Log.w("TERMINAL_DEBUG", "⚠️ Terminal não ativado ou sem token")
                    callback?.invoke(null)
                    return@thread
                }

                val url = URL("$serverUrl/api/terminal/sync")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    Log.e("TERMINAL_DEBUG", "📦 Sync OK - companyId: ${terminalCompanyId}")
                    Log.e("TERMINAL_DEBUG", "📦 Produtos: ${json.optJSONArray("products")?.length() ?: 0}")
                    Log.e("TERMINAL_DEBUG", "📦 Categorias: ${json.optJSONArray("categories")?.length() ?: 0}")

                    // Enviar dados para os callbacks existentes
                    json.optJSONArray("products")?.let { onProdutosReceived?.invoke(it) }
                    json.optJSONArray("categories")?.let { onCategoriasReceived?.invoke(it) }
                    json.optJSONArray("customers")?.let { onClientesReceived?.invoke(it) }

                    callback?.invoke(json)
                } else {
                    Log.e("TERMINAL_DEBUG", "❌ Sync falhou - HTTP ${conn.responseCode}")
                    callback?.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("TERMINAL_DEBUG", "❌ Erro no sync: ${e.message}")
                callback?.invoke(null)
            }
        }
    }

    // ==================== RESTART / SHUTDOWN ====================

    private fun restartDevice(): Boolean {
        try {
            if (devicePolicyManager != null && adminComponentName != null) {
                try {
                    devicePolicyManager?.reboot(adminComponentName!!)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Admin API reboot falhou: ${e.message}")
                }
            }
            // SUNMI
            try {
                sendBroadcast(Intent("com.sunmi.action.REBOOT"))
                return true
            } catch (_: Exception) {}
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reiniciar", e)
            return false
        }
    }

    private fun shutdownDevice(): Boolean {
        try {
            // SUNMI
            try {
                sendBroadcast(Intent("com.sunmi.action.SHUTDOWN"))
                return true
            } catch (_: Exception) {}
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desligar", e)
            return false
        }
    }
}
