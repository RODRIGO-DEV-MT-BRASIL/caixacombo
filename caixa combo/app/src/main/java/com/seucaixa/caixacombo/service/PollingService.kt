package com.seucaixa.caixacombo.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
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
        private const val DEFAULT_SERVER_URL = "https://caixa-dashboard-mt.onrender.com"

        private var SERVER_URL = DEFAULT_SERVER_URL
        private var pollingDeviceId: String? = null
        private var deviceName: String? = null
        private var serialNumber: String? = null

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

        private var isRunning = false
        private var consecutiveErrors = 0

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
            onClientesReceived: ((JSONArray) -> Unit)? = null
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
        }

        fun isConnected(): Boolean = isRunning && consecutiveErrors < MAX_RETRIES

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
            // Enviado junto com o poll
            Log.d(TAG, "Caixa data será enviada no próximo poll")
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
        isRunning = true
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    private fun startPolling() {
        thread(name = "PollingThread") {
            while (isRunning) {
                try {
                    doPoll()
                    consecutiveErrors = 0
                    onConnectionChange?.invoke(true)
                } catch (e: Exception) {
                    consecutiveErrors++
                    Log.e(TAG, "Poll erro #$consecutiveErrors: ${e.message}")
                    if (consecutiveErrors >= MAX_RETRIES) {
                        onConnectionChange?.invoke(false)
                    }
                }

                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun doPoll() {
        val id = pollingDeviceId ?: return
        val name = deviceName ?: "Dispositivo"

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
        }

        conn.outputStream.use { it.write(data.toString().toByteArray()) }

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Poll HTTP $responseCode")
        }

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val commands = json.optJSONArray("commands")

        if (commands != null && commands.length() > 0) {
            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                val command = cmd.optString("command", "")
                val params = cmd.optJSONObject("params")

                Log.d(TAG, "Comando recebido via poll: $command")
                handleCommand(command, params)
            }
        }
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
            else -> {
                Log.w(TAG, "Comando desconhecido: $command")
                onCommandReceived?.invoke(command, params)
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
