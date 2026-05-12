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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.seucaixa.caixacombo.ui.screens.dashboard.DashboardScreen
import com.seucaixa.caixacombo.ui.screens.dashboard.DashboardViewModel
import com.seucaixa.caixacombo.data.backup.BackupScheduler
import com.seucaixa.caixacombo.service.PollingService
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var checkoutViewModel: CheckoutViewModel
    private lateinit var produtosViewModel: ProdutosViewModel
    private lateinit var vendasViewModel: VendasViewModel
    private lateinit var caixaViewModel: CaixaViewModel
    private lateinit var dashboardViewModel: DashboardViewModel
    private var pollingService: PollingService? = null
    private lateinit var configuracaoImpressaoViewModel: ConfiguracaoImpressaoViewModel

    private lateinit var lockPrefs: android.content.SharedPreferences

    // Estado de sincronização para o dialog
    private val syncResultState = androidx.compose.runtime.mutableStateOf<SyncResult?>(null)

    // Estado de aprovação do terminal
    private val isApprovedState = androidx.compose.runtime.mutableStateOf(true) // Assume aprovado até saber o contrário
    private var approvalDialog: android.app.Dialog? = null

    // Callback para resultado do Stone deeplink
    private var stonePaymentCallback: ((StoneDeeplinkService.PaymentResult?) -> Unit)? = null
    private var stoneCancelCallback: ((StoneDeeplinkService.CancelResult?) -> Unit)? = null
    private var stoneReprintCallback: ((StoneDeeplinkService.ReprintResult?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Processar intent de deeplink Stone se recebido na criação
        processStoneDeeplinkIntent(intent)

        // Configuração de tela cheia imersiva
        setupImmersiveMode()

        // Iniciar modo quiosque (lock task mode) - apenas se habilitado explicitamente
        // Stone não permite Device Owner no terminal, kiosk mode fica desabilitado por padrão
        val kioskEnabled = getSharedPreferences("lock_state", Context.MODE_PRIVATE)
            .getBoolean("kiosk_enabled", false)
        if (kioskEnabled) startLockTaskMode()

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
        PollingService.loadServerUrl(this)
        
        // Aplicar configuração de whitelabel estática baseada no flavor
        applyFlavorWhitelabelConfig()
        
        // Configurar Admin para reboot sem root
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val cn = android.content.ComponentName(this, AdminReceiver::class.java)
        PollingService.setAdminInfo(dpm, cn)

        // Verificar e solicitar ativação automática do Device Admin
        // STONE: Não solicitar Device Admin em terminais Stone - não é permitido
        // Só ativa se kiosk_enabled=true nas preferências
        val kioskEnabledForAdmin = getSharedPreferences("lock_state", Context.MODE_PRIVATE)
            .getBoolean("kiosk_enabled", false)
        if (kioskEnabledForAdmin && !android.os.Build.MODEL.equals("D2s", ignoreCase = true)) {
            checkAndRequestDeviceAdmin(dpm, cn)
        }

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
            },
            onReprintRequested = { atk ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Reimpressão solicitada via dashboard, atk=$atk")
                    stoneReprintCallback = { result ->
                        android.util.Log.d("MainActivity", "Reimpressão resultado: success=${result?.success}")
                        PollingService.sendControlResult("reimprimir_venda", result?.success ?: false, if (result?.success != true) result?.reason else null)
                    }
                    StoneDeeplinkService.sendReprint(this@MainActivity, atk)
                }
            },
            onCancelRequested = { atk, amount ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Cancelamento solicitado via dashboard, atk=$atk, amount=$amount")
                    stoneCancelCallback = { result ->
                        android.util.Log.d("MainActivity", "Cancelamento resultado: success=${result?.success}")
                        PollingService.sendControlResult("cancelar_venda", result?.success ?: false, if (result?.success != true) result?.reason else null)
                    }
                    StoneDeeplinkService.sendCancel(this@MainActivity, atk, amount, false)
                }
            },
            onClientesReceived = { clientesJson ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Recebidos ${clientesJson.length()} clientes do servidor para sincronização")
                    syncClientesFromServer(clientesJson)
                }
            },
            onCategoriasReceived = { categoriasJson ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Recebidas ${categoriasJson.length()} categorias do servidor para sincronização")
                    syncCategoriasFromServer(categoriasJson)
                }
            },
            onEmpresasReceived = { empresasJson ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Recebidas ${empresasJson.length()} empresas do servidor para sincronização")
                    syncEmpresasFromServer(empresasJson)
                }
            },
            onSyncComplete = { produtos, categorias, clientes ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Sincronização completa: $produtos produtos, $categorias categorias, $clientes clientes")
                    syncResultState.value = SyncResult(produtos, categorias, clientes)
                }
            },
            onApprovalStatus = { approved, status, empresaId ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Approval status: approved=$approved, status=$status, empresaId=$empresaId")
                    isApprovedState.value = approved
                    if (!approved) {
                        // Terminal pendente - mostrar tela de aguardando aprovação
                        showApprovalPendingScreen()
                    } else {
                        // Terminal aprovado - fechar tela de pendência se aberta
                        hideApprovalPendingScreen()
                    }
                }
            },
            onEmpresaConfig = { config ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Empresa config recebida: $config")
                    applyWhitelabelConfig(config)
                }
            },
            onFuncionariosReceived = { funcionariosJson ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Recebidos ${funcionariosJson.length()} funcionários do servidor")
                    syncFuncionariosFromServer(funcionariosJson)
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

        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(app.vendaRepository)
        )[DashboardViewModel::class.java]

        setContent {
            CaixaComboTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val deviceType = rememberDeviceType()
                    val caixaAberto by caixaViewModel.caixaAberto.collectAsState()

                    // Dialog de sincronização automática
                    val syncResult by syncResultState
                    SyncDialog(
                        syncResult = syncResult,
                        onDismiss = { syncResultState.value = null }
                    )

                    // Tela de aguardando aprovação
                    val isApproved by isApprovedState
                    if (!isApproved) {
                        ApprovalPendingScreen()
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (com.seucaixa.caixacombo.data.SecurePrefs.getOperatorId(this@MainActivity) > 0) {
                            if (caixaAberto) "checkout" else "caixa"
                        } else "home"
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
                                        onSendStonePayment = { amount, transactionType, installmentType, orderId, callback ->
                                            stonePaymentCallback = callback
                                            StoneDeeplinkService.sendPayment(this@MainActivity, amount, transactionType, installmentType, 0, orderId)
                                        },
                                        onLogout = {
                                            com.seucaixa.caixacombo.data.SecurePrefs.clearOperator(this@MainActivity)
                                            getSharedPreferences("cores_sistema", Context.MODE_PRIVATE).edit()
                                                .remove("operador_nome")
                                                .remove("operador_cargo")
                                                .remove("operador_id")
                                                .apply()
                                            navController.navigate("home") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onNavigateToDashboard = {
                                            dashboardViewModel.refresh()
                                            navController.navigate("dashboard")
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
                                        },
                                        onLogout = {
                                            com.seucaixa.caixacombo.data.SecurePrefs.clearOperator(this@MainActivity)
                                            getSharedPreferences("cores_sistema", Context.MODE_PRIVATE).edit()
                                                .remove("operador_nome")
                                                .remove("operador_cargo")
                                                .remove("operador_id")
                                                .apply()
                                            navController.navigate("home") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onNavigateToDashboard = {
                                            dashboardViewModel.refresh()
                                            navController.navigate("dashboard")
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

                        composable("dashboard") {
                            val dashboardData by dashboardViewModel.data.collectAsState()
                            val primaryColor = Color(getSharedPreferences("cores_sistema", Context.MODE_PRIVATE).getInt("primary_color", 0xFF6200EE.toInt()))
                            DashboardScreen(
                                data = dashboardData,
                                primaryColor = primaryColor,
                                onBack = { navController.popBackStack() }
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
                        cpf = "",
                        telefone = "",
                        email = "",
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
                    "Ative para permitir o gerenciamento do dispositivo")
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        processStoneDeeplinkIntent(intent)
    }

    /**
     * Processa retorno dos deeplinks Stone (pagamento, cancelamento, reimpressao)
     * A Stone retorna via scheme "caixacombo" configurado como returnscheme
     */
    private fun processStoneDeeplinkIntent(intent: Intent?) {
        if (intent == null || intent.data == null) return
        val uri = intent.data!!
        val scheme = uri.scheme ?: return

        if (scheme != "caixacombo") return

        android.util.Log.d("MainActivity", "Deeplink Stone recebido: $uri")
        val authority = uri.authority ?: return

        when (authority) {
            "pay-response" -> {
                val result = StoneDeeplinkService.parsePaymentResult(intent)
                stonePaymentCallback?.invoke(result)
                android.util.Log.d("MainActivity", "Deeplink pagamento: success=${result?.success}")
            }
            "cancel" -> {
                val result = StoneDeeplinkService.parseCancelResult(intent)
                stoneCancelCallback?.invoke(result)
                android.util.Log.d("MainActivity", "Deeplink cancelamento: success=${result?.success}")
            }
            "reprint" -> {
                val result = StoneDeeplinkService.parseReprintResult(intent)
                stoneReprintCallback?.invoke(result)
                android.util.Log.d("MainActivity", "Deeplink reimpressão: success=${result?.success}")
            }
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

        // Stone cancel result
        if (requestCode == StoneDeeplinkService.REQUEST_CODE_CANCEL) {
            val result = StoneDeeplinkService.parseCancelResult(data)
            stoneCancelCallback?.invoke(result)
            android.util.Log.d("MainActivity", "Resultado cancelamento Stone: success=${result?.success}, reason=${result?.reason}")
        }

        // Stone reprint result
        if (requestCode == StoneDeeplinkService.REQUEST_CODE_REPRINT) {
            val result = StoneDeeplinkService.parseReprintResult(data)
            stoneReprintCallback?.invoke(result)
            android.util.Log.d("MainActivity", "Resultado reimpressão Stone: success=${result?.success}, reason=${result?.reason}")
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
                val password = params?.optString("lockPassword", "") ?: ""
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
    
    /**
     * Exibe tela de aguardando aprovação do admin
     */
    private fun showApprovalPendingScreen() {
        if (approvalDialog?.isShowing == true) return
        try {
            val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(viewsApprovalPending())
            dialog.setCancelable(false)
            dialog.show()
            approvalDialog = dialog
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao mostrar tela de aprovação", e)
        }
    }
    
    private fun hideApprovalPendingScreen() {
        approvalDialog?.dismiss()
        approvalDialog = null
    }
    
    private fun viewsApprovalPending(): android.view.View {
        val context = this
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(64, 0, 64, 0)
            setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        }
        val icon = android.widget.TextView(context).apply {
            text = "⏳"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
        }
        val title = android.widget.TextView(context).apply {
            text = "Aguardando Aprovação"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        val subtitle = android.widget.TextView(context).apply {
            text = "Este terminal precisa ser aprovado\npelo administrador no dashboard.\n\nAguarde..."
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(icon)
        layout.addView(title)
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 24
        layout.addView(subtitle, params)
        return layout
    }
    
    /**
     * Aplica configuração de whitelabel recebida do servidor
     */
    private fun applyWhitelabelConfig(config: org.json.JSONObject) {
        try {
            val primaryColor = config.optString("primaryColor", "#3b82f6")
            val secondaryColor = config.optString("secondaryColor", "#06b6d4")
            val accentColor = config.optString("accentColor", "#10b981")
            val logoUrl = config.optString("logoUrl", "")
            val nome = config.optString("nome", "")
            val empresaId = config.optString("empresaId", "")

            val prefs = getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("primary_color_hex", primaryColor)
                .putString("secondary_color_hex", secondaryColor)
                .putString("accent_color_hex", accentColor)
                .putString("logo_url", logoUrl)
                .putString("empresa_nome", nome)
                .putString("empresa_id", empresaId)
                // Também salvar como int para compatibilidade com código existente
                .putInt("primary_color", parseColor(primaryColor))
                .putInt("secondary_color", parseColor(secondaryColor))
                .putInt("tertiary_color", parseColor(accentColor))
                .apply()

            android.util.Log.d("MainActivity", "Whitelabel aplicado: nome=$nome, primary=$primaryColor, logo=$logoUrl")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao aplicar whitelabel", e)
        }
    }

    /**
     * Aplica configuração de whitelabel estática baseada no flavor
     */
    private fun applyFlavorWhitelabelConfig() {
        try {
            val flavor = BuildConfig.FLAVOR_empresa
            val prefs = getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
            
            when (flavor) {
                "empresa1" -> {
                    // Configurações estáticas para empresa1
                    prefs.edit()
                        .putString("primary_color_hex", "#FF5722")
                        .putString("secondary_color_hex", "#FF9800")
                        .putString("accent_color_hex", "#4CAF50")
                        .putInt("primary_color", 0xFFFF5722.toInt())
                        .putInt("secondary_color", 0xFFFF9800.toInt())
                        .putInt("tertiary_color", 0xFF4CAF50.toInt())
                        .apply()
                    android.util.Log.d("MainActivity", "Whitelabel estático aplicado: empresa1")
                }
                "empresa2" -> {
                    // Configurações estáticas para empresa2
                    prefs.edit()
                        .putString("primary_color_hex", "#9C27B0")
                        .putString("secondary_color_hex", "#E91E63")
                        .putString("accent_color_hex", "#00BCD4")
                        .putInt("primary_color", 0xFF9C27B0.toInt())
                        .putInt("secondary_color", 0xFFE91E63.toInt())
                        .putInt("tertiary_color", 0xFF00BCD4.toInt())
                        .apply()
                    android.util.Log.d("MainActivity", "Whitelabel estático aplicado: empresa2")
                }
                else -> {
                    // CaixaCombo padrão
                    android.util.Log.d("MainActivity", "Usando whitelabel padrão: caixacombo")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao aplicar whitelabel estático", e)
        }
    }
    
    private fun parseColor(hex: String): Int {
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            0xFF6200EE.toInt()
        }
    }
    
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
            android.util.Log.e("SYNC_DEBUG", "Atualizando ${produtos.length()} produtos do servidor")
            
            // Converter JSONArray para lista de produtos
            val produtosList = mutableListOf<Produto>()
            for (i in 0 until produtos.length()) {
                val produtoJson = produtos.getJSONObject(i)
                val categoriaId: Long? = when {
                    !produtoJson.has("categoriaId") -> null
                    produtoJson.isNull("categoriaId") -> null
                    else -> {
                        val catVal = produtoJson.opt("categoriaId")
                        when (catVal) {
                            is Number -> catVal.toLong().takeIf { it != 0L }
                            is String -> catVal.toLongOrNull()?.takeIf { it != 0L }
                            else -> null
                        }
                    }
                }
                val produto = Produto(
                    id = produtoJson.getLong("id"),
                    nome = produtoJson.getString("nome"),
                    descricao = produtoJson.optString("descricao", ""),
                    precoVenda = produtoJson.getDouble("preco"),
                    categoriaId = categoriaId,
                    codigoBarras = produtoJson.optString("codigoBarras", ""),
                    estoque = produtoJson.optDouble("estoque", 0.0),
                    imagem = produtoJson.optString("imagem", "").ifBlank { null },
                    unidade = produtoJson.optString("unidade", "UN")
                )
                produtosList.add(produto)
            }
            
            // Usar upsert (REPLACE) para evitar flash vazio na UI
            val db = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(applicationContext)
            val produtoDao = db.produtoDao()
            lifecycleScope.launch(Dispatchers.IO) {
                produtoDao.insertAll(produtosList)
                // Remover produtos que não vieram do servidor
                val serverIds = produtosList.map { it.id }
                val allLocal = produtoDao.getAllProdutosList()
                val toDelete = allLocal.filter { it.id !in serverIds }
                toDelete.forEach { produtoDao.delete(it) }
            }
            
            // Atualizar ViewModels
            caixaViewModel.atualizarProdutos(produtosList)
            checkoutViewModel.atualizarProdutosServidor(produtosList)
            
            android.util.Log.e("SYNC_DEBUG", "✅ ${produtosList.size} produtos salvos - primeiro: ${produtosList.firstOrNull()?.nome} catId=${produtosList.firstOrNull()?.categoriaId}")
            
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "❌ Erro ao atualizar produtos: ${e.message}", e)
        }
    }

    /**
     * Sincroniza clientes recebidos do servidor para o banco local
     */
    private fun syncClientesFromServer(clientesJson: org.json.JSONArray) {
        try {
            val db = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(applicationContext)
            val clienteDao = db.clienteDao()

            lifecycleScope.launch(Dispatchers.IO) {
                // Limpar clientes locais e substituir pelos do servidor
                clienteDao.deleteAll()

                for (i in 0 until clientesJson.length()) {
                    val c = clientesJson.getJSONObject(i)
                    val cliente = com.seucaixa.caixacombo.data.model.Cliente(
                        id = c.optLong("id", System.currentTimeMillis()),
                        nome = c.optString("nome", ""),
                        cpfCnpj = c.optString("cpfCnpj", ""),
                        telefone = c.optString("telefone", ""),
                        email = c.optString("email", ""),
                        endereco = c.optString("endereco", ""),
                        cidade = c.optString("cidade", ""),
                        cep = c.optString("cep", ""),
                        observacao = c.optString("observacao", ""),
                        ativo = c.optBoolean("ativo", true),
                        dataCriacao = c.optLong("dataCriacao", System.currentTimeMillis())
                    )
                    clienteDao.insert(cliente)
                }

                android.util.Log.d("MainActivity", "✅ ${clientesJson.length()} clientes sincronizados do servidor")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao sincronizar clientes do servidor", e)
        }
    }

    /**
     * Sincroniza empresas recebidas do servidor para o banco local
     */
    private fun syncEmpresasFromServer(empresasJson: org.json.JSONArray) {
        try {
            val db = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(applicationContext)
            val empresaDao = db.empresaDao()

            lifecycleScope.launch(Dispatchers.IO) {
                // Usar upsert (REPLACE) para evitar flash vazio
                for (i in 0 until empresasJson.length()) {
                    val e = empresasJson.getJSONObject(i)
                    val empresa = com.seucaixa.caixacombo.data.model.Empresa(
                        id = 1, // Sempre ID 1 (empresa única no terminal)
                        razaoSocial = e.optString("nome", ""),
                        nomeFantasia = e.optString("nome", ""),
                        cnpj = e.optString("cnpj", ""),
                        telefone = e.optString("telefone", ""),
                        email = e.optString("email", "")
                    )
                    empresaDao.insert(empresa)
                }

                // Remover empresas que não vieram do servidor (exceto ID 1 que é fixo)
                android.util.Log.d("MainActivity", "✅ ${empresasJson.length()} empresas sincronizadas do servidor")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao sincronizar empresas do servidor", e)
        }
    }

    /**
     * Sincroniza funcionários recebidos do servidor para SharedPreferences
     */
    private fun syncFuncionariosFromServer(funcionariosJson: org.json.JSONArray) {
        try {
            val prefs = getSharedPreferences("funcionarios_data", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("funcionarios_json", funcionariosJson.toString())
            editor.apply()
            android.util.Log.d("MainActivity", "✅ ${funcionariosJson.length()} funcionários sincronizados do servidor")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao sincronizar funcionários", e)
        }
    }

    /**
     * Login de funcionário via código de acesso
     */
    private fun loginFuncionario(codigo: String, callback: ((Boolean, String?, String?, org.json.JSONObject?) -> Unit)?) {
        val deviceId = PollingService.getDeviceId() ?: run {
            callback?.invoke(false, "Dispositivo não configurado", null, null)
            return
        }
        val serverUrl = PollingService.getServerUrl()

        thread {
            try {
                val url = java.net.URL("$serverUrl/api/auth/funcionario")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val data = org.json.JSONObject().apply {
                    put("codigo", codigo)
                    put("deviceId", deviceId)
                }
                conn.outputStream.use { it.write(data.toString().toByteArray()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = org.json.JSONObject(response)
                    val funcionario = json.getJSONObject("funcionario")
                    val nome = funcionario.optString("nome", "")
                    val cargo = funcionario.optString("cargo", "caixa")
                    val permissoes = funcionario.optJSONObject("permissoes")
                    val empresaId = funcionario.optString("empresaId", "")

                    // Salvar operador logado
                    val funcId = funcionario.optLong("id", -1)
                    com.seucaixa.caixacombo.data.SecurePrefs.saveOperator(this@MainActivity, nome, cargo, funcId)

                    // Salvar permissões
                    val permPrefs = getSharedPreferences("funcionario_permissoes", Context.MODE_PRIVATE)
                    permPrefs.edit()
                        .putString("cargo", cargo)
                        .putString("empresaId", empresaId)
                        .putBoolean("vendas", permissoes?.optBoolean("vendas", true) ?: true)
                        .putBoolean("caixa", permissoes?.optBoolean("caixa", true) ?: true)
                        .putBoolean("produtos", permissoes?.optBoolean("produtos", false) ?: false)
                        .putBoolean("categorias", permissoes?.optBoolean("categorias", false) ?: false)
                        .putBoolean("relatorios", permissoes?.optBoolean("relatorios", false) ?: false)
                        .putBoolean("desconto", permissoes?.optBoolean("desconto", false) ?: false)
                        .putBoolean("cancelar_venda", permissoes?.optBoolean("cancelar_venda", false) ?: false)
                        .putBoolean("operacoes_caixa", permissoes?.optBoolean("operacoes_caixa", true) ?: true)
                        .apply()

                    runOnUiThread {
                        callback?.invoke(true, nome, cargo, permissoes)
                    }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "Erro desconhecido"
                    val errorMsg = try { org.json.JSONObject(error).optString("error", "Erro") } catch (_: Exception) { "Código inválido" }
                    runOnUiThread {
                        callback?.invoke(false, errorMsg, null, null)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro no login funcionário", e)
                runOnUiThread {
                    callback?.invoke(false, "Erro de conexão", null, null)
                }
            }
        }
    }

    /**
     * Sincroniza categorias recebidas do servidor para o banco local
     */
    private fun syncCategoriasFromServer(categoriasJson: org.json.JSONArray) {
        try {
            val db = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(applicationContext)
            val categoriaDao = db.categoriaDao()

            lifecycleScope.launch(Dispatchers.IO) {
                // Usar insertAll com REPLACE para evitar deleteAll que causa flash vazio na UI
                val categoriasList = mutableListOf<com.seucaixa.caixacombo.data.model.Categoria>()
                for (i in 0 until categoriasJson.length()) {
                    val c = categoriasJson.getJSONObject(i)
                    val catId = c.optLong("id", 0L)
                    val categoria = com.seucaixa.caixacombo.data.model.Categoria(
                        id = catId,
                        nome = c.optString("nome", ""),
                        cor = c.optString("cor", null),
                        icone = c.optString("icone", null),
                        ordem = c.optInt("ordem", 0),
                        ativa = c.optBoolean("ativa", true)
                    )
                    categoriasList.add(categoria)
                }
                
                // Inserir com REPLACE (upsert) e depois remover categorias que não vieram do servidor
                categoriaDao.insertAll(categoriasList)
                
                // Remover categorias que não estão na lista do servidor
                val serverIds = categoriasList.map { it.id }
                val allLocal = categoriaDao.getAllCategoriasList()
                val toDelete = allLocal.filter { it.id !in serverIds }
                toDelete.forEach { categoriaDao.delete(it) }

                android.util.Log.e("SYNC_DEBUG", "✅ ${categoriasJson.length()} categorias salvas: ${categoriasList.map { "${it.nome}(id=${it.id})" }}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "❌ Erro ao sincronizar categorias: ${e.message}", e)
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

data class SyncResult(
    val produtos: Int,
    val categorias: Int,
    val clientes: Int
)

@Composable
fun SyncDialog(
    syncResult: SyncResult?,
    onDismiss: () -> Unit
) {
    if (syncResult == null) return

    // Auto-dismiss após 5 segundos
    val dismissKey = syncResult // recompor quando mudar
    LaunchedEffect(dismissKey) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Sincronização Automática",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Dados atualizados com o servidor:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Produtos
                if (syncResult.produtos > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Inventory, null, modifier = Modifier.size(20.dp), tint = Color(0xFF4CAF50))
                        Text("${syncResult.produtos} produtos cadastrados", fontWeight = FontWeight.Medium)
                    }
                }
                // Categorias
                if (syncResult.categorias > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Category, null, modifier = Modifier.size(20.dp), tint = Color(0xFF2196F3))
                        Text("${syncResult.categorias} categorias", fontWeight = FontWeight.Medium)
                    }
                }
                // Clientes
                if (syncResult.clientes > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF9800))
                        Text("${syncResult.clientes} clientes", fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ApprovalPendingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .zIndex(999f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⏳",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Aguardando Aprovação",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Este terminal precisa ser aprovado\npelo administrador no dashboard.",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
