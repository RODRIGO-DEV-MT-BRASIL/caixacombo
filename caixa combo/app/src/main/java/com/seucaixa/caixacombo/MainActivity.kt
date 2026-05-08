package com.seucaixa.caixacombo

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.seucaixa.caixacombo.ui.screens.HomeScreen
import com.seucaixa.caixacombo.ui.screens.AcessosScreen
import com.seucaixa.caixacombo.ui.screens.CadastroScreen
import com.seucaixa.caixacombo.ui.screens.ProdutosScreen
import com.seucaixa.caixacombo.ui.screens.VendasScreen
import com.seucaixa.caixacombo.ui.screens.caixa.CaixaOperacoesScreen
import com.seucaixa.caixacombo.ui.screens.configuracao.ConfiguracaoCoresScreen
import com.seucaixa.caixacombo.ui.screens.configuracao.ConfiguracaoImpressaoScreen
import com.seucaixa.caixacombo.ui.screens.configuracao.ConfiguracaoTipoImpressaoScreen
import com.seucaixa.caixacombo.ui.screens.checkout.CheckoutScreenMobile
import com.seucaixa.caixacombo.ui.screens.checkout.CheckoutScreenPOS
import com.seucaixa.caixacombo.ui.theme.CaixaComboTheme
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.theme.rememberDeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CaixaViewModel
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ConfiguracaoImpressaoViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ProdutosViewModel
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.ui.viewmodel.VendasViewModel
import com.seucaixa.caixacombo.data.backup.BackupScheduler
import com.seucaixa.caixacombo.service.PollingService
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var checkoutViewModel: CheckoutViewModel
    private lateinit var produtosViewModel: ProdutosViewModel
    private lateinit var vendasViewModel: VendasViewModel
    private lateinit var caixaViewModel: CaixaViewModel
    private var pollingService: PollingService? = null
    private lateinit var configuracaoImpressaoViewModel: ConfiguracaoImpressaoViewModel

    private lateinit var lockPrefs: android.content.SharedPreferences

    // Callback para resultado do Stone deeplink
    private var stonePaymentCallback: ((StoneDeeplinkService.PaymentResult?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração de tela cheia imersiva
        setupImmersiveMode()

        // Iniciar modo quiosque (lock task mode)
        startLockTaskMode()

        // Agendar backup automático offline (WorkManager)
        BackupScheduler.schedule(applicationContext)

        // Inicializar SharedPreferences para persistência de bloqueio
        lockPrefs = getSharedPreferences("lock_state", Context.MODE_PRIVATE)

        // Verificar se o dispositivo está bloqueado ao iniciar
        val isLocked = lockPrefs.getBoolean("is_locked", false)
        val lockReason = lockPrefs.getString("lock_reason", "Bloqueado pelo administrador") ?: "Bloqueado"
        if (isLocked) {
            android.util.Log.w("MainActivity", "Dispositivo estava bloqueado, mostrando tela de bloqueio")
            showLockScreen(lockReason)
        }

        // Iniciar Polling Service REST para comunicação com Dashboard (Stone compliance)
        // Usar ANDROID_ID como ID principal (funciona sem modo desenvolvedor)
        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        
        // Usar ANDROID_ID como deviceId (único por dispositivo, funciona sem modo desenvolvedor)
        val deviceId = androidId
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val deviceName = "${manufacturer.capitalize()} $model"
        val serialNumber = android.os.Build.SERIAL ?: "UNKNOWN"
        PollingService.setDeviceInfo(deviceId, deviceName, serialNumber)
        
        // Configurar Admin para reboot sem root
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val cn = android.content.ComponentName(this, AdminReceiver::class.java)
        PollingService.setAdminInfo(dpm, cn)

        // Verificar e solicitar ativação automática do Device Admin
        checkAndRequestDeviceAdmin(dpm, cn)

        // Configurar callbacks do WebSocket para bloqueio/desbloqueio
        PollingService.setCallbacks(
            onConnectionChange = { isConnected ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "WebSocket conexão: $isConnected")
                }
            },
            onCommandReceived = { command, params ->
                runOnUiThread {
                    handleWebSocketCommand(command, params)
                }
            },
            onDataRequested = {
                runOnUiThread {
                    // Enviar dados do caixa para o servidor quando solicitado
                    sendCaixaDataToServer()
                }
            },
            onProdutosReceived = { produtos ->
                runOnUiThread {
                    // Processar produtos recebidos do servidor
                    updateProdutosFromServer(produtos)
                }
            },
            onLockPasswordReceived = { password ->
                runOnUiThread {
                    currentLockPassword = password
                    android.util.Log.d("MainActivity", "Senha de bloqueio recebida: $password")
                }
            },
            onUnlockResponse = { success, message ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Resposta de desbloqueio do servidor: success=$success, message=$message")
                    if (success) {
                        // Senha validada com sucesso pelo servidor
                        hideLockScreen()
                    } else {
                        // Senha incorreta no servidor
                        android.util.Log.w("MainActivity", "Senha rejeitada pelo servidor: $message")
                    }
                }
            }
        )
        
        val webSocketIntent = Intent(this, PollingService::class.java)
        startService(webSocketIntent)

        // Garantir que existe pelo menos um usuário admin no banco
        ensureAdminUserExists()

        // Obter repositórios da Application
        val app = application as CaixaApplication
        
        // Criar ViewModels com factory
        checkoutViewModel = ViewModelProvider(
            this,
            CheckoutViewModel.Factory(app.produtoRepository, app.vendaRepository, app.categoriaRepository)
        )[CheckoutViewModel::class.java]
        
        produtosViewModel = ViewModelProvider(
            this,
            ProdutosViewModel.Factory(app.produtoRepository, app.categoriaRepository, app.vendaRepository, this)
        )[ProdutosViewModel::class.java]
        
        vendasViewModel = ViewModelProvider(
            this,
            VendasViewModel.Factory(app.vendaRepository)
        )[VendasViewModel::class.java]

        caixaViewModel = ViewModelProvider(
            this,
            CaixaViewModel.Factory(app.operacaoCaixaRepository, app.vendaRepository, app.printService, app.configuracaoImpressaoRepository)
        )[CaixaViewModel::class.java]

        configuracaoImpressaoViewModel = ViewModelProvider(
            this,
            ConfiguracaoImpressaoViewModel.Factory(app.configuracaoImpressaoRepository)
        )[ConfiguracaoImpressaoViewModel::class.java]

        setContent {
            CaixaComboTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val deviceType = rememberDeviceType()
                    val caixaAberto by caixaViewModel.caixaAberto.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                caixaAberto = caixaAberto,
                                onNavigateToCheckout = {
                                    navController.navigate("checkout")
                                },
                                onNavigateToCaixa = {
                                    navController.navigate("caixa")
                                },
                                onNavigateToProdutos = {
                                    navController.navigate("produtos")
                                },
                                onNavigateToVendas = {
                                    navController.navigate("vendas")
                                },
                                onNavigateToConfiguracao = {
                                    navController.navigate("configuracao_impressao")
                                },
                                onNavigateToConfiguracaoCores = {
                                    navController.navigate("configuracao_cores")
                                },
                                onNavigateToConfiguracaoTipoImpressao = {
                                    navController.navigate("configuracao_tipo_impressao")
                                }
                            )
                        }

                        composable("checkout") {
                            // Seleciona layout baseado no dispositivo
                            when (deviceType) {
                                DeviceType.POS, DeviceType.TABLET -> {
                                    // SUNMI V1/V2 e D2S/VI/V2 - Layout POS (tela grande)
                                    CheckoutScreenPOS(
                                        viewModel = checkoutViewModel,
                                        caixaAberto = caixaAberto,
                                        deviceType = deviceType,
                                        onNavigateToHome = {
                                            navController.navigate("home")
                                        },
                                        onNavigateToProdutos = {
                                            navController.navigate("produtos")
                                        },
                                        onNavigateToVendas = {
                                            navController.navigate("vendas")
                                        },
                                        onNavigateToCaixa = {
                                            navController.navigate("caixa")
                                        },
                                        onNavigateToConfiguracaoTipoImpressao = {
                                            navController.navigate("configuracao_tipo_impressao")
                                        },
                                        onNavigateToAcessos = {
                                            navController.navigate("acessos")
                                        },
                                        onNavigateToCadastro = {
                                            navController.navigate("cadastro")
                                        },
                                        onSendStonePayment = { amount, type, installmentCount, orderId, callback ->
                                            stonePaymentCallback = callback
                                            StoneDeeplinkService.sendPayment(this@MainActivity, amount, type, installmentCount, orderId)
                                        }
                                    )
                                }
                                else -> {
                                    // Mobile - Layout compacto
                                    CheckoutScreenMobile(
                                        viewModel = checkoutViewModel,
                                        caixaAberto = caixaAberto,
                                        onNavigateToHome = {
                                            navController.navigate("home")
                                        },
                                        onNavigateToProdutos = {
                                            navController.navigate("produtos")
                                        },
                                        onNavigateToVendas = {
                                            navController.navigate("vendas")
                                        },
                                        onNavigateToCaixa = {
                                            navController.navigate("caixa")
                                        }
                                    )
                                }
                            }
                        }
                        
                        composable("produtos") {
                            ProdutosScreen(
                                viewModel = produtosViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable("vendas") {
                            VendasScreen(
                                viewModel = vendasViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("caixa") {
                            com.seucaixa.caixacombo.ui.screens.caixa.CaixaOperacoesScreen(
                                viewModel = caixaViewModel,
                                onNavigateBack = {
                                    navController.navigate("checkout") {
                                        popUpTo("checkout") { inclusive = true }
                                    }
                                },
                                onNavigateToConfiguracao = {
                                    navController.navigate("configuracao_impressao")
                                }
                            )
                        }

                        composable("configuracao_impressao") {
                            ConfiguracaoImpressaoScreen(
                                viewModel = configuracaoImpressaoViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("configuracao_cores") {
                            ConfiguracaoCoresScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("configuracao_tipo_impressao") {
                            ConfiguracaoTipoImpressaoScreen(
                                viewModel = configuracaoImpressaoViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("acessos") {
                            AcessosScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("cadastro") {
                            CadastroScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== MODO TELA CHEIA IMERSIVA ====================

    private fun setupImmersiveMode() {
        // Manter tela sempre ligada
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 e abaixo (API 29-)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    private fun startLockTaskMode() {
        // Modo quiosque - bloqueia o app na tela
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName) || dpm.isAdminActive(admin)) {
            try {
                startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopLockTaskMode() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, AdminReceiver::class.java)
            if (dpm.isDeviceOwnerApp(packageName) || dpm.isAdminActive(admin)) {
                try {
                    stopLockTask()
                    android.util.Log.d("MainActivity", "Lock task parado com sucesso")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Não estava em lock task ou erro ao parar: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao verificar permissões de lock task: ${e.message}", e)
        }
    }

    /**
     * Desprovisiona o Device Owner, permitindo desinstalação do app.
     * Chamado via: adb shell am broadcast -a com.seucaixa.caixacombo.UNPROVISION
     */
    private fun unprovisionDeviceOwner() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, AdminReceiver::class.java)

            // Parar lock task primeiro
            stopLockTaskMode()

            // Remover Device Owner (permite desinstalação)
            if (dpm.isDeviceOwnerApp(packageName)) {
                dpm.clearDeviceOwnerApp(packageName)
                android.util.Log.d("MainActivity", "Device Owner removido com sucesso")
                android.widget.Toast.makeText(this, "Device Owner removido. App pode ser desinstalado.", android.widget.Toast.LENGTH_LONG).show()
            } else if (dpm.isAdminActive(admin)) {
                dpm.removeActiveAdmin(admin)
                android.util.Log.d("MainActivity", "Device Admin removido com sucesso")
                android.widget.Toast.makeText(this, "Device Admin removido. App pode ser desinstalado.", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao desprovisionar: ${e.message}", e)
            android.widget.Toast.makeText(this, "Erro ao remover admin: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Garante que existe pelo menos um usuário admin no banco.
     * Código padrão: 1234
     */
    private fun ensureAdminUserExists() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(applicationContext)
                val dao = db.usuarioDao()
                val admin = dao.getUsuarioByCodigo("1234")
                if (admin == null) {
                    val id = dao.insert(com.seucaixa.caixacombo.data.model.Usuario(
                        nome = "Admin",
                        codigo = "1234",
                        cargo = com.seucaixa.caixacombo.data.model.CargoUsuario.ADMIN,
                        ativo = true,
                        permVender = true,
                        permCaixa = true,
                        permProdutos = true,
                        permVendas = true,
                        permRelatorios = true,
                        permConfiguracoes = true,
                        permSangria = true,
                        permSuprimento = true,
                        permFechamento = true,
                        permAcessos = true
                    ))
                    android.util.Log.d("MainActivity", "Usuário admin padrão criado (código: 1234, id: $id)")
                } else {
                    android.util.Log.d("MainActivity", "Usuário admin já existe: ${admin.nome}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro ao verificar/criar admin: ${e.message}", e)
            }
        }
    }

    /**
     * Verifica se o app está ativo como Device Admin e solicita ativação se necessário.
     * Necessário para usar comandos de reiniciar/desligar dispositivo.
     */
    private fun checkAndRequestDeviceAdmin(dpm: DevicePolicyManager, admin: ComponentName) {
        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        val isAdminActive = dpm.isAdminActive(admin)

        android.util.Log.d("MainActivity", "Device Owner: $isDeviceOwner, Admin Active: $isAdminActive")

        if (!isDeviceOwner && !isAdminActive) {
            android.util.Log.w("MainActivity", "Device Admin não ativo. Solicitando ativação...")

            // Solicitar ativação do Device Admin
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Ative para permitir:\n• Bloquear tela remotamente\n• Reiniciar dispositivo\n• Desligar dispositivo")
            }

            try {
                startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro ao solicitar Device Admin: ${e.message}")
            }
        } else {
            android.util.Log.d("MainActivity", "✅ Permissões de Device Admin já ativas")
        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 2001
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Stone deeplink result
        if (requestCode == StoneDeeplinkService.REQUEST_CODE_PAYMENT) {
            val result = StoneDeeplinkService.parsePaymentResult(data)
            stonePaymentCallback?.invoke(result)
        }

        // Device Admin result
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, AdminReceiver::class.java)
            val isAdminActive = dpm.isAdminActive(admin)

            if (resultCode == RESULT_OK && isAdminActive) {
                android.util.Log.d("MainActivity", "✅ Device Admin ativado com sucesso")
            } else {
                android.util.Log.w("MainActivity", "⚠️ Device Admin não foi ativado. Botões reiniciar/desligar não funcionarão.")
            }
        }
    }

    /**
     * Trata comandos recebidos do Dashboard via WebSocket
     */
    private fun handleWebSocketCommand(command: String, params: org.json.JSONObject?) {
        android.util.Log.d("MainActivity", "Comando recebido: $command")
        
        when (command) {
            "lock_device" -> {
                val reason = params?.optString("reason", "Bloqueado pelo administrador") ?: "Bloqueado"
                val password = params?.optString("lockPassword", "")
                if (password.isNotEmpty()) {
                    currentLockPassword = password
                    android.util.Log.d("MainActivity", "Senha de bloqueio atualizada via comando: $password")
                }
                showLockScreen(reason)
            }
            "unlock_device" -> {
                hideLockScreen()
            }
            "set_usage_time" -> {
                val minutes = params?.optInt("minutes", 0) ?: 0
                startUsageTimer(minutes)
            }
            "close_app" -> {
                android.util.Log.d("MainActivity", "Fechando o aplicativo via comando remoto")
                stopLockTaskMode() // Parar modo quiosque antes de fechar
                finishAndRemoveTask()
            }
            "shutdown" -> {
                // Desligar o dispositivo (requer permissões de root)
                try {
                    val process = Runtime.getRuntime().exec("su")
                    val os = process.outputStream
                    os.write("reboot -p\n".toByteArray())
                    os.flush()
                    os.close()
                    process.waitFor()
                    android.util.Log.d("MainActivity", "Comando de desligamento enviado")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Erro ao desligar dispositivo (pode requerer root)", e)
                    // Alternativa sem root: mostrar tela de desligamento do sistema
                    try {
                        val intent = Intent(Intent.ACTION_SHUTDOWN)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } catch (ex: Exception) {
                        android.util.Log.e("MainActivity", "Erro ao mostrar tela de desligamento", ex)
                    }
                }
            }
        }
    }
    
    /**
     * Exibe tela de bloqueio moderna
     */
    private var lockDialog: android.app.Dialog? = null
    private var currentLockPassword: String? = null
    
    private fun showLockScreen(reason: String) {
        try {
            android.util.Log.w("MainActivity", "Dispositivo bloqueado: $reason")
            
            // Salvar estado de bloqueio persistente
            lockPrefs.edit()
                .putBoolean("is_locked", true)
                .putString("lock_reason", reason)
                .apply()
            
            // Parar modo quiosque com verificação robusta
            try {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = ComponentName(this, AdminReceiver::class.java)
                if (dpm.isDeviceOwnerApp(packageName) || dpm.isAdminActive(admin)) {
                    try {
                        stopLockTask()
                        android.util.Log.d("MainActivity", "Lock task parado com sucesso")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Não estava em lock task ou erro ao parar: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro ao verificar permissões de lock task: ${e.message}", e)
            }
            
            // Usar LockActivity em vez de diálogo overlay (Stone compliance - sem SYSTEM_ALERT_WINDOW)
            val lockPassword = currentLockPassword ?: ""
            LockActivity.start(this, reason, lockPassword)
            PollingService.sendLockConfirmed()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro crítico ao bloquear dispositivo: ${e.message}", e)
        }
    }
    
    /**
     * Cria view de bloqueio moderna
     */
    private fun createLockView(reason: String): android.view.View {
        val context = this
        
        return android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#1a1a2e"))
            setPadding(48, 48, 48, 48)
            
            // Ícone de bloqueio
            addView(android.widget.ImageView(context).apply {
                setImageResource(android.R.drawable.ic_lock_lock)
                setColorFilter(android.graphics.Color.parseColor("#e94560"))
                layoutParams = android.widget.LinearLayout.LayoutParams(120, 120).apply {
                    bottomMargin = 32
                }
            })
            
            // Título
            addView(android.widget.TextView(context).apply {
                text = "⚠️ TERMINAL BLOQUEADO"
                textSize = 24f
                setTextColor(android.graphics.Color.parseColor("#e94560"))
                setPadding(0, 0, 0, 24)
                gravity = android.view.Gravity.CENTER
            })
            
            // Motivo
            addView(android.widget.TextView(context).apply {
                text = "Motivo: $reason"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 0, 0, 32)
                gravity = android.view.Gravity.CENTER
            })
            
            // Separador
            addView(android.view.View(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#16213e"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2
                ).apply { bottomMargin = 32 }
            })
            
            // Contato
            addView(android.widget.TextView(context).apply {
                text = "Para desbloquear, entre em contato:"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#a2a2a2"))
                setPadding(0, 0, 0, 16)
                gravity = android.view.Gravity.CENTER
            })
            
            addView(android.widget.TextView(context).apply {
                text = "📞 Rodrigo Dev MT"
                textSize = 18f
                setTextColor(android.graphics.Color.parseColor("#0f3460"))
                setPadding(0, 0, 0, 8)
                gravity = android.view.Gravity.CENTER
            })
            
            addView(android.widget.TextView(context).apply {
                text = "📱 (66) 99618-4323"
                textSize = 22f
                setTextColor(android.graphics.Color.parseColor("#4e9f3d"))
                setPadding(0, 0, 0, 32)
                gravity = android.view.Gravity.CENTER
            })
            
            // Separador
            addView(android.view.View(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#16213e"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2
                ).apply { bottomMargin = 32 }
            })
            
            // Campo de senha
            addView(android.widget.TextView(context).apply {
                text = "Senha de Administrador:"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#a2a2a2"))
                setPadding(0, 0, 0, 8)
            })
            
            val passwordInput = android.widget.EditText(context).apply {
                hint = "Digite a senha"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                setTextColor(android.graphics.Color.WHITE)
                setHintTextColor(android.graphics.Color.parseColor("#999999"))
                setBackgroundColor(android.graphics.Color.parseColor("#2a2a3e"))
                setPadding(24, 20, 24, 20)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    bottomMargin = 24
                    setMargins(0, 0, 0, 0)
                }
                
                // Configurar para teclado não cobrir o campo
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        // Ajustar window para teclado não cobrir
                        window?.setSoftInputMode(
                            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        )
                    }
                }
            }
            addView(passwordInput)
            
            // Botão desbloquear
            addView(android.widget.Button(context).apply {
                text = "DESBLOQUEAR"
                setBackgroundColor(android.graphics.Color.parseColor("#4e9f3d"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setPadding(32, 20, 32, 20)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    bottomMargin = 16
                    setMargins(0, 0, 0, 0)
                }
                setOnClickListener {
                    try {
                        val password = passwordInput.text.toString()
                        if (password.isNotEmpty()) {
                            // Validar senha localmente e enviar para servidor
                            if (password == currentLockPassword) {
                                android.util.Log.d("MainActivity", "Senha correta, desbloqueando")
                                // Enviar confirmação para servidor
                                try {
                                    PollingService.sendUnlockAttempt(password)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Erro ao enviar tentativa de desbloqueio", e)
                                }
                                hideLockScreen()
                            } else {
                                android.util.Log.w("MainActivity", "Senha incorreta")
                                passwordInput.error = "Senha incorreta"
                                passwordInput.text?.clear()
                                passwordInput.requestFocus()
                                // Vibrar para feedback
                                try {
                                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(200)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Erro ao vibrar", e)
                                }
                            }
                        } else {
                            passwordInput.error = "Digite a senha"
                            passwordInput.requestFocus()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Erro ao validar senha", e)
                        passwordInput.error = "Erro ao validar senha"
                    }
                }
            })
        }
    }
    
    /**
     * Esconde tela de bloqueio
     */
    private fun hideLockScreen() {
        android.util.Log.d("MainActivity", "Dispositivo desbloqueado")
        
        // Remover estado de bloqueio persistente
        lockPrefs.edit()
            .putBoolean("is_locked", false)
            .remove("lock_reason")
            .apply()
        
        // Notificar servidor que o dispositivo foi desbloqueado
        PollingService.sendUnlockConfirmed()
        
        lockDialog?.dismiss()
        lockDialog = null
        startLockTaskMode()
    }
    
    /**
     * Inicia timer de uso com contador regressivo
     */
    private var usageTimer: android.os.CountDownTimer? = null
    private var usageMinutesTotal: Int = 0
    private var usageTimeDialog: android.app.Dialog? = null
    private var timerTextViewId: Int = 0
    
    private fun startUsageTimer(minutes: Int) {
        android.util.Log.d("MainActivity", "Tempo de uso definido: $minutes minutos")
        usageMinutesTotal = minutes
        
        // Cancelar timer anterior e fechar diálogo anterior
        usageTimer?.cancel()
        usageTimeDialog?.dismiss()
        
        val totalMs = minutes * 60 * 1000L
        
        // Criar diálogo com contador regressivo
        usageTimeDialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(createUsageTimerView(minutes))
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.black)
        }
        
        usageTimeDialog?.show()
        
        // Timer com contador regressivo
        usageTimer = object : android.os.CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val minutesLeft = secondsLeft / 60
                val secondsRemain = secondsLeft % 60
                updateTimerDisplay(minutesLeft, secondsRemain)
            }
            
            override fun onFinish() {
                android.util.Log.d("MainActivity", "Tempo de uso expirado")
                usageTimeDialog?.dismiss()
                // Bloquear automaticamente (mantém a senha atual se existir)
                if (currentLockPassword != null) {
                    android.util.Log.d("MainActivity", "Mantendo senha atual para desbloqueio: $currentLockPassword")
                }
                showLockScreen("Tempo de uso expirado")
            }
        }.start()
    }
    
    /**
     * Cria view do contador regressivo
     */
    private fun createUsageTimerView(totalMinutes: Int): android.view.View {
        val context = this
        
        return android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#1a1a2e"))
            setPadding(48, 48, 48, 48)
            
            // Título
            addView(android.widget.TextView(context).apply {
                text = "⏰ TEMPO DE USO"
                textSize = 24f
                setTextColor(android.graphics.Color.parseColor("#e94560"))
                setPadding(0, 0, 0, 32)
                gravity = android.view.Gravity.CENTER
            })
            
            // Contador
            val timerText = android.widget.TextView(context).apply {
                timerTextViewId = android.view.View.generateViewId()
                id = timerTextViewId
                text = "${totalMinutes}:00"
                textSize = 72f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 0, 0, 32)
                gravity = android.view.Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            addView(timerText)
            
            // Mensagem
            addView(android.widget.TextView(context).apply {
                text = "Tempo restante para bloqueio automático"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#a2a2a2"))
                setPadding(0, 0, 0, 48)
                gravity = android.view.Gravity.CENTER
            })
            
            // Botão cancelar (apenas para admin)
            addView(android.widget.Button(context).apply {
                text = "CANCELAR TEMPO"
                setBackgroundColor(android.graphics.Color.parseColor("#e94560"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setPadding(32, 24, 32, 24)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    // Enviar solicitação de cancelamento para o servidor
                    PollingService.sendDeviceStatus("cancel_usage_time")
                    usageTimer?.cancel()
                    usageTimeDialog?.dismiss()
                }
            })
        }
    }
    
    /**
     * Atualiza display do timer
     */
    private fun updateTimerDisplay(minutes: Int, seconds: Int) {
        usageTimeDialog?.findViewById<android.widget.TextView>(timerTextViewId)?.text = 
            String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Exibe notificação do sistema
     */
    private fun showSystemNotification(title: String, message: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "caixa_combo_alerts"
            val channel = android.app.NotificationChannel(
                channelId,
                "Alertas do Sistema",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
            
            val notification = android.app.Notification.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
            
            manager.notify(1, notification)
        }
    }
    
    /**
     * Envia dados do caixa para o servidor
     */
    private fun sendCaixaDataToServer() {
        try {
            val caixaData = org.json.JSONObject().apply {
                put("saldo", caixaViewModel.saldoAtual.value ?: 0.0)
                put("vendas", caixaViewModel.totalVendas.value ?: 0.0)
                put("status", "aberto")
            }
            PollingService.sendCaixaData(caixaData)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao enviar dados do caixa", e)
        }
    }

    /**
     * Atualiza produtos recebidos do servidor
     */
    private fun updateProdutosFromServer(produtos: org.json.JSONArray) {
        try {
            android.util.Log.d("MainActivity", "Atualizando ${produtos.length()} produtos do servidor")
            
            // Converter JSONArray para lista de produtos
            val produtosList = mutableListOf<Produto>()
            for (i in 0 until produtos.length()) {
                val produtoJson = produtos.getJSONObject(i)
                val produto = Produto(
                    id = produtoJson.getLong("id"),
                    nome = produtoJson.getString("nome"),
                    descricao = produtoJson.optString("descricao", ""),
                    precoVenda = produtoJson.getDouble("preco"),
                    categoriaId = produtoJson.optLong("categoriaId", 0),
                    codigoBarras = produtoJson.optString("codigoBarras", ""),
                    estoque = produtoJson.optDouble("estoque", 0.0),
                    imagem = produtoJson.optString("imagem", ""),
                    unidade = produtoJson.optString("unidade", "UN")
                )
                produtosList.add(produto)
            }
            
            // Atualizar ViewModels
            caixaViewModel.atualizarProdutos(produtosList)
            checkoutViewModel.atualizarProdutosServidor(produtosList)
            
            android.util.Log.d("MainActivity", "✅ Produtos atualizados com sucesso")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao atualizar produtos do servidor", e)
        }
    }

    // Bloquear botão voltar durante checkout
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Navegar para checkout se estiver em outra tela
        // Isso evita tela branca
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }

    override fun onDestroy() {
        stopLockTaskMode()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        // Reativar modo imersivo ao retornar
        setupImmersiveMode()
    }
}
