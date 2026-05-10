package com.seucaixa.caixacombo.ui.screens.checkout

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalDensity
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenPOS(
    viewModel: CheckoutViewModel,
    caixaAberto: Boolean,
    deviceType: DeviceType,
    onNavigateToHome: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToCaixa: () -> Unit,
    onNavigateToConfiguracaoTipoImpressao: () -> Unit = {},
    onNavigateToAcessos: () -> Unit = {},
    onNavigateToCadastro: () -> Unit = {},
    onSendStonePayment: ((Long, String, String, String, (StoneDeeplinkService.PaymentResult?) -> Unit) -> Unit)? = null,
    onLogout: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }

    // Usuário logado - carregar permissões
    var usuarioLogado by remember { mutableStateOf<Usuario?>(null) }
    LaunchedEffect(Unit) {
        try {
            val operatorId = com.seucaixa.caixacombo.data.SecurePrefs.getOperatorId(context)
            if (operatorId > 0) {
                val dao = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(context).usuarioDao()
                usuarioLogado = dao.getUsuarioById(operatorId)
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckoutPOS", "Erro ao carregar usuário", e)
        }
    }

    // Logo carregado do arquivo no disco (evita CursorWindow overflow com base64 grande no Room)
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var logoCheckoutPDV by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val logoFile = java.io.File(context.filesDir, "logo.png")
            if (logoFile.exists()) {
                logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            }
            val dao = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(context).configuracaoImpressaoDao()
            val config = dao.getConfiguracaoSemLogo()
            logoCheckoutPDV = config?.logoCheckoutPDV ?: false
        } catch (e: Exception) {
            android.util.Log.e("CheckoutPOS", "Erro ao carregar logo", e)
        }
    }

    val primaryColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt())))
    }
    val backgroundColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt())))
    }

    val produtos by viewModel.produtos.collectAsState()
    val carrinho by viewModel.carrinho.collectAsState()
    val total by viewModel.total.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaSelecionada by viewModel.categoriaSelecionada.collectAsState()
    val vendaFinalizada by viewModel.vendaFinalizada.collectAsState()
    val ultimaVenda by viewModel.ultimaVenda.collectAsState()
    val vendidosPorProduto by viewModel.vendidosPorProduto.collectAsState()

    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var formaPagamentoSelecionada by remember { mutableStateOf<FormaPagamento?>(null) }
    var showValorDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }
    var showProdutoGrid by remember { mutableStateOf(false) }
    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var showBuscarClienteDialog by remember { mutableStateOf(false) }
    var clienteSelecionado by remember { mutableStateOf<Cliente?>(null) }
    var empresaSelecionada by remember { mutableStateOf<Empresa?>(null) }

    // Stone deeplink
    var stonePaymentResult by remember { mutableStateOf<StoneDeeplinkService.PaymentResult?>(null) }
    var stonePaymentError by remember { mutableStateOf<String?>(null) }
    val isStoneAvailable = remember { StoneDeeplinkService.isStoneInstalled(context) }

    // Relógio atualizado
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    // Diálogo de sucesso
    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(
            venda = ultimaVenda!!,
            onDismiss = { viewModel.resetVendaFinalizada() },
            nomeCliente = clienteSelecionado?.nome ?: empresaSelecionada?.nomeFantasia?.ifBlank { empresaSelecionada?.razaoSocial }
        )
    }

    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
    val isSmallScreen = screenWidthDp < 600

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor).statusBarsPadding()) {
        // ==================== TOPO ====================
        TopBarPDV(
            primaryColor = primaryColor,
            currentTime = currentTime,
            onNavigateToHome = onNavigateToHome,
            onLogout = onLogout,
            logoBitmap = logoBitmap,
            operadorNome = sharedPreferences.getString("operador_nome", null),
            isSmallScreen = isSmallScreen,
            clienteNome = clienteSelecionado?.nome ?: empresaSelecionada?.nomeFantasia?.ifBlank { empresaSelecionada?.razaoSocial }
        )

        // ==================== NOME PRODUTO + INFO ====================
        ProdutoNomeBar(
            produto = produtoSelecionado,
            primaryColor = primaryColor,
            isSmallScreen = isSmallScreen,
            vendidosPorProduto = vendidosPorProduto
        )

        // ==================== CONTEÚDO PRINCIPAL ====================
        if (isSmallScreen) {
            // P2: Sem card esquerdo, tabela ocupa largura total + botão busca
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Botão buscar produtos compacto
                Button(
                    onClick = { showProdutoGrid = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BUSCAR PRODUTOS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Header da tabela
                CarrinhoTableHeader(primaryColor, isSmallScreen)

                // Lista de itens
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(carrinho) { item ->
                        CarrinhoTableRow(
                            item = item,
                            onQuantidadeChange = { novaQtd ->
                                if (novaQtd > 0) viewModel.atualizarQuantidade(item.produtoId, novaQtd)
                                else viewModel.removerDoCarrinho(item.produtoId)
                            },
                            onRemover = { viewModel.removerDoCarrinho(item.produtoId) },
                            isSmallScreen = isSmallScreen
                        )
                    }
                }

                // Rodapé da tabela - total de itens
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${carrinho.sumOf { it.quantidade.toInt() }} itens",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "R$ %.2f".format(total),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = primaryColor
                    )
                }
            }
        } else {
            // Layout normal: Card esquerdo + Tabela direita
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ESQUERDA: Foto do produto + Grid de produtos (quando aberto)
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    // Foto do produto selecionado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (produtoSelecionado != null) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Imagem do produto
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (produtoSelecionado!!.imagem != null) {
                                            val imageUrl = if (produtoSelecionado!!.imagem!!.startsWith("http") || produtoSelecionado!!.imagem!!.startsWith("data:")) produtoSelecionado!!.imagem!! else "${com.seucaixa.caixacombo.service.PollingService.getServerUrl()}${produtoSelecionado!!.imagem}"
                                            coil.compose.AsyncImage(
                                                model = imageUrl,
                                                contentDescription = produtoSelecionado!!.nome,
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Inventory,
                                                null,
                                                modifier = Modifier.size(60.dp),
                                                tint = primaryColor.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        produtoSelecionado!!.nome,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    if (!produtoSelecionado!!.descricao.isNullOrEmpty()) {
                                        Text(
                                            produtoSelecionado!!.descricao!!,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val estoqueCor = when {
                                        produtoSelecionado!!.estoque <= 0 -> MaterialTheme.colorScheme.error
                                        produtoSelecionado!!.estoque <= 5 -> Color(0xFFFF9800)
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                    val estoqueLabel = if (produtoSelecionado!!.estoque <= 0) "ESGOTADO" else "Estoque: ${produtoSelecionado!!.estoqueFormatado()}"
                                    Text(
                                        estoqueLabel,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = estoqueCor
                                    )
                                    val vendidos = vendidosPorProduto[produtoSelecionado!!.id] ?: 0
                                    if (vendidos > 0) {
                                        Text(
                                            "Vendidos: $vendidos",
                                            fontSize = 14.sp,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (logoCheckoutPDV && logoBitmap != null) {
                                    Image(
                                        painter = BitmapPainter(logoBitmap!!.asImageBitmap()),
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .height(340.dp)
                                            .width(340.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Selecione um produto",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Botão para abrir grid de produtos
                    Button(
                        onClick = { showProdutoGrid = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUSCAR PRODUTOS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // DIREITA: Tabela de itens do carrinho
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Header da tabela
                    CarrinhoTableHeader(primaryColor, isSmallScreen)

                    // Lista de itens
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(carrinho) { item ->
                            CarrinhoTableRow(
                                item = item,
                                onQuantidadeChange = { novaQtd ->
                                    if (novaQtd > 0) viewModel.atualizarQuantidade(item.produtoId, novaQtd)
                                    else viewModel.removerDoCarrinho(item.produtoId)
                                },
                                onRemover = { viewModel.removerDoCarrinho(item.produtoId) },
                                isSmallScreen = isSmallScreen
                            )
                        }
                    }

                    // Rodapé da tabela - total de itens
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${carrinho.sumOf { it.quantidade.toInt() }} itens",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "R$ %.2f".format(total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                    }
                }
            }
        }

        // ==================== RODAPÉ: Preço + Total + Botões ====================
        RodapePDV(
            produtoSelecionado = produtoSelecionado,
            total = total,
            carrinho = carrinho,
            caixaAberto = caixaAberto,
            primaryColor = primaryColor,
            isSmallScreen = isSmallScreen,
            usuarioLogado = usuarioLogado,
            onFinalizar = {
                if (caixaAberto) showFormaPagamentoDialog = true
                else onNavigateToCaixa()
            },
            onLimpar = { viewModel.limparCarrinho() },
            onNavigateToCaixa = onNavigateToCaixa,
            onNavigateToProdutos = onNavigateToProdutos,
            onNavigateToVendas = onNavigateToVendas,
            onNavigateToHome = onNavigateToHome,
            onNavigateToConfiguracaoTipoImpressao = onNavigateToConfiguracaoTipoImpressao,
            onNavigateToAcessos = onNavigateToAcessos,
            onNavigateToCadastro = onNavigateToCadastro,
            onBuscarCliente = { showBuscarClienteDialog = true },
            onNavigateToDashboard = onNavigateToDashboard
        )
    }

    // Grid de produtos (dialog)
    if (showProdutoGrid) {
        ProdutoGridDialog(
            produtos = produtos,
            categorias = categorias,
            categoriaSelecionada = categoriaSelecionada,
            busca = busca,
            carrinho = carrinho,
            onBuscaChange = viewModel::buscarProdutos,
            onCategoriaChange = viewModel::selecionarCategoria,
            onProdutoClick = { produto ->
                viewModel.adicionarAoCarrinho(produto)
                produtoSelecionado = produto
            },
            onDismiss = { showProdutoGrid = false },
            primaryColor = primaryColor,
            vendidosPorProduto = vendidosPorProduto
        )
    }

    // Dialog de busca de cliente/empresa
    if (showBuscarClienteDialog) {
        BuscarClienteDialog(
            onClienteSelecionado = { cliente ->
                clienteSelecionado = cliente
                empresaSelecionada = null
            },
            onEmpresaSelecionada = { empresa ->
                empresaSelecionada = empresa
                clienteSelecionado = null
            },
            onDismiss = { showBuscarClienteDialog = false }
        )
    }

    // Dialog de escolha de forma de pagamento
    if (showFormaPagamentoDialog) {
        EscolhaFormaPagamentoDialogPOS(
            onFormaSelecionada = { forma ->
                showFormaPagamentoDialog = false

                // Cartão/PIX com Stone instalado -> enviar direto ao terminal com valor total
                if (isStoneAvailable && onSendStonePayment != null && StoneDeeplinkService.shouldUseStone(forma)) {
                    val transactionType = StoneDeeplinkService.mapFormaPagamentoToStone(forma)!!
                    val centavos = StoneDeeplinkService.toCentavos(total)
                    stonePaymentError = null
                    stonePaymentResult = null
                    formaPagamentoSelecionada = forma

                    onSendStonePayment?.invoke(centavos, transactionType, StoneDeeplinkService.InstallmentType.NONE, "") { result ->
                        if (result != null && result.success) {
                            stonePaymentResult = result
                            formaPagamentoSelecionada = null
                            val stoneAtk = result.authorizationCode.ifEmpty { null }
                            if (viewModel.finalizarVenda(forma, total, clienteSelecionado?.id, stoneAtk)) {
                                produtoSelecionado = null
                            }
                        } else {
                            // Tratar erro 401 (Stone não ativado/autenticado)
                            val code = result?.code ?: -1
                            val reason = result?.reason ?: ""
                            stonePaymentError = when {
                                code == 401 -> "Terminal não ativado na Stone. Ative o terminal antes de usar cartão/débito."
                                code == 1000 -> "App Stone não encontrado. Instale o Stone Payment App no terminal."
                                reason.contains("NOT_FOUND", ignoreCase = true) -> "App de pagamento não encontrado no terminal."
                                reason.isNotBlank() -> "Pagamento recusado: $reason (código: $code)"
                                else -> "Pagamento recusado no terminal (código: $code)"
                            }
                        }
                    }
                } else {
                    // Dinheiro / outras formas -> mostrar dialog de valor
                    formaPagamentoSelecionada = forma
                    showValorDialog = true
                }
            },
            onCancelar = {
                showFormaPagamentoDialog = false
                formaPagamentoSelecionada = null
            },
            isStoneAvailable = isStoneAvailable
        )
    }

    // Dialog de valor (apenas para Dinheiro - troco)
    if (showValorDialog && formaPagamentoSelecionada != null) {
        ValorPagamentoDialogPOS(
            total = total,
            formaPagamento = formaPagamentoSelecionada!!,
            valorRecebido = valorRecebido,
            onValorRecebidoChange = { valorRecebido = it },
            onConfirmar = {
                val recebido = valorRecebido.toDoubleSafe(total)
                val forma = formaPagamentoSelecionada!!

                // Dinheiro -> finalizar direto
                if (viewModel.finalizarVenda(forma, recebido, clienteSelecionado?.id)) {
                    showValorDialog = false
                    valorRecebido = ""
                    formaPagamentoSelecionada = null
                    produtoSelecionado = null
                }
            },
            onCancelar = {
                showValorDialog = false
                valorRecebido = ""
                formaPagamentoSelecionada = null
            }
        )
    }

}

// ==================== COMPONENTES DO LAYOUT PDV ====================

@Composable
private fun TopBarPDV(
    primaryColor: Color,
    currentTime: String,
    onNavigateToHome: () -> Unit,
    onLogout: () -> Unit = {},
    logoBitmap: android.graphics.Bitmap? = null,
    operadorNome: String? = null,
    isSmallScreen: Boolean = false,
    clienteNome: String? = null
) {
    if (isSmallScreen) {
        // P2: Tudo numa linha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(primaryColor)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Esquerda: Logo + Nome
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoBitmap != null) {
                    Image(
                        painter = BitmapPainter(logoBitmap.asImageBitmap()),
                        contentDescription = "Logo",
                        modifier = Modifier.height(28.dp).width(28.dp)
                    )
                } else {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Caixa Combo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            // Centro: Cliente + Operador
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (clienteNome != null) "Cli: $clienteNome" else "Cli: ---", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        if (operadorNome != null) "Op: $operadorNome" else "Op: ---",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
            // Direita: Hora + Sair
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Schedule, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Text(currentTime, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onLogout, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    } else {
        // Layout normal: tudo em uma linha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(primaryColor)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + Nome
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoBitmap != null) {
                    Image(
                        painter = BitmapPainter(logoBitmap.asImageBitmap()),
                        contentDescription = "Logo",
                        modifier = Modifier.height(40.dp).width(40.dp)
                    )
                } else {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Caixa Combo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Centro: Cliente / Vendedor
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cliente: ---", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (operadorNome != null) "Operador: $operadorNome" else "Operador: ---",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Direita: Hora + Sair
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Schedule, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                Text(currentTime, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProdutoNomeBar(
    produto: Produto?,
    primaryColor: Color,
    isSmallScreen: Boolean = false,
    vendidosPorProduto: Map<Long, Int> = emptyMap()
) {
    if (isSmallScreen) {
        // P2: Nome + preço + estoque inline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(primaryColor.copy(alpha = 0.1f))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = "Produto",
                tint = if (produto != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(if (produto != null) 22.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                produto?.nome ?: "Selecione um produto",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (produto != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (produto != null) {
                val estoqueCor = when {
                    produto.estoque <= 0 -> MaterialTheme.colorScheme.error
                    produto.estoque <= 5 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.tertiary
                }
                val estoqueLabel = if (produto.estoque <= 0) "ESGOTADO" else "Estq:${produto.estoqueFormatado()}"
                Text(
                    estoqueLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = estoqueCor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    produto.precoFormatado(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                val vendidos = vendidosPorProduto[produto.id] ?: 0
                if (vendidos > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Vend:$vendidos",
                        fontSize = 9.sp,
                        color = primaryColor
                    )
                }
            }
        }
    } else {
        // Layout normal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(primaryColor.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "Código de barras",
                    tint = if (produto != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    produto?.nome ?: "Selecione um produto",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (produto != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CarrinhoTableHeader(primaryColor: Color, isSmallScreen: Boolean = false) {
    val fs = if (isSmallScreen) 11.sp else 13.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(primaryColor.copy(alpha = 0.15f))
            .padding(horizontal = if (isSmallScreen) 4.dp else 12.dp, vertical = if (isSmallScreen) 5.dp else 8.dp)
    ) {
        Text("CÓD", modifier = Modifier.weight(if (isSmallScreen) 0.13f else 0.1f), fontWeight = FontWeight.Bold, fontSize = fs, color = primaryColor)
        Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 0.dp))
        Text("PRODUTO", modifier = Modifier.weight(if (isSmallScreen) 0.28f else 0.3f), fontWeight = FontWeight.Bold, fontSize = fs, color = primaryColor)
        Text("QTD", modifier = Modifier.weight(if (isSmallScreen) 0.16f else 0.12f), fontWeight = FontWeight.Bold, fontSize = fs, color = primaryColor, textAlign = TextAlign.Center)
        Text("VL UNIT", modifier = Modifier.weight(0.16f), fontWeight = FontWeight.Bold, fontSize = fs, color = primaryColor, textAlign = TextAlign.End)
        Text("TOTAL", modifier = Modifier.weight(0.16f), fontWeight = FontWeight.Bold, fontSize = fs, color = primaryColor, textAlign = TextAlign.End)
        Spacer(modifier = Modifier.weight(0.16f))
    }
}

@Composable
private fun CarrinhoTableRow(
    item: ItemCarrinho,
    onQuantidadeChange: (Double) -> Unit,
    onRemover: () -> Unit,
    isSmallScreen: Boolean = false
) {
    val fs = if (isSmallScreen) 11.sp else 13.sp
    val iconSize = if (isSmallScreen) 22.dp else 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = if (isSmallScreen) 4.dp else 12.dp, vertical = if (isSmallScreen) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.produtoId.toString(), modifier = Modifier.weight(if (isSmallScreen) 0.13f else 0.1f), fontSize = fs, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 0.dp))
        Text(item.produtoNome, modifier = Modifier.weight(if (isSmallScreen) 0.28f else 0.3f), fontSize = fs, fontWeight = FontWeight.Medium, maxLines = 1)
        Row(modifier = Modifier.weight(0.16f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Remove, null, modifier = Modifier.size(iconSize).clickable { onQuantidadeChange(item.quantidade - 1) }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%.0f".format(item.quantidade), fontSize = if (isSmallScreen) 14.sp else 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
            Icon(Icons.Default.Add, null, modifier = Modifier.size(iconSize).clickable { onQuantidadeChange(item.quantidade + 1) }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("R$%.2f".format(item.precoUnitario), modifier = Modifier.weight(0.16f), fontSize = fs, textAlign = TextAlign.End)
        Text("R$%.2f".format(item.total), modifier = Modifier.weight(0.16f), fontSize = fs, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.Close, null, modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp).weight(0.16f).clickable { onRemover() }, tint = MaterialTheme.colorScheme.error)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun RodapePDV(
    produtoSelecionado: Produto?,
    total: Double,
    carrinho: List<ItemCarrinho>,
    caixaAberto: Boolean,
    primaryColor: Color,
    isSmallScreen: Boolean = false,
    usuarioLogado: Usuario? = null,
    onFinalizar: () -> Unit,
    onLimpar: () -> Unit,
    onNavigateToCaixa: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToConfiguracaoTipoImpressao: () -> Unit = {},
    onNavigateToAcessos: () -> Unit = {},
    onNavigateToCadastro: () -> Unit = {},
    onBuscarCliente: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Linha: Preço do produto + Total da compra
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = if (isSmallScreen) 8.dp else 16.dp, vertical = if (isSmallScreen) 4.dp else 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preço do produto selecionado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PREÇO: ", fontSize = if (isSmallScreen) 11.sp else 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    produtoSelecionado?.precoFormatado() ?: "R$ 0,00",
                    fontSize = if (isSmallScreen) 18.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            // Total da compra
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL: ", fontSize = if (isSmallScreen) 11.sp else 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "R$ %.2f".format(total),
                    fontSize = if (isSmallScreen) 20.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }

        // Barra de botões F1-F9 + Finalizar
        // Permissões: ADMIN vê tudo, outros só o que têm permissão
        val isAdmin = usuarioLogado?.cargo == CargoUsuario.ADMIN
        val permCaixa = isAdmin || usuarioLogado?.permCaixa == true
        val permVendas = isAdmin || usuarioLogado?.permVendas == true
        val permProdutos = isAdmin || usuarioLogado?.permProdutos == true
        val permConfig = isAdmin || usuarioLogado?.permConfiguracoes == true
        val permAcessos = isAdmin || usuarioLogado?.permAcessos == true
        val permCadastro = isAdmin || permAcessos // Cadastro ligado a Acessos

        if (isSmallScreen) {
            // P2: 2 linhas de botões (só os permitidos)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FuncaoBotao("F1", "Dashboard", Icons.Default.Dashboard, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToDashboard() }
                    if (permCaixa) FuncaoBotao("F2", "Caixa", Icons.Default.AccountBalance, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToCaixa() }
                    if (permVendas) FuncaoBotao("F3", "Vendas", Icons.Default.Receipt, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToVendas() }
                    if (permProdutos) FuncaoBotao("F4", "Produtos", Icons.Default.Inventory, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToProdutos() }
                    if (permConfig) FuncaoBotao("F5", "Impressão", Icons.Default.Print, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToConfiguracaoTipoImpressao() }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FuncaoBotao("F6", "Cliente", Icons.Default.Person, Color.White, Modifier.weight(1f), isSmallScreen) { onBuscarCliente() }
                    FuncaoBotao("F7", "Limpar", Icons.Default.Delete, Color.White, Modifier.weight(1f), isSmallScreen) { onLimpar() }
                    if (permAcessos) FuncaoBotao("F8", "Auditoria", Icons.Default.Assessment, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToAcessos() }
                    if (permCadastro) FuncaoBotao("F9", "Cadastro", Icons.Default.AppRegistration, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToCadastro() }
                    // Botão FINALIZAR
                    Button(
                        onClick = onFinalizar,
                        modifier = Modifier.weight(2f).fillMaxHeight().padding(horizontal = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (caixaAberto) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        ),
                        enabled = carrinho.isNotEmpty() || !caixaAberto,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (caixaAberto) Icons.Default.CheckCircle else Icons.Default.Lock,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            if (caixaAberto) "FINALIZAR" else "ABRIR CAIXA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Layout normal: 1 linha (só os permitidos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(primaryColor),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuncaoBotao("F1", "Dashboard", Icons.Default.Dashboard, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToDashboard() }
                if (permCaixa) FuncaoBotao("F2", "Caixa", Icons.Default.AccountBalance, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToCaixa() }
                if (permVendas) FuncaoBotao("F3", "Vendas", Icons.Default.Receipt, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToVendas() }
                if (permProdutos) FuncaoBotao("F4", "Produtos", Icons.Default.Inventory, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToProdutos() }
                if (permConfig) FuncaoBotao("F5", "Impress.", Icons.Default.Print, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToConfiguracaoTipoImpressao() }
                FuncaoBotao("F6", "Cliente", Icons.Default.Person, Color.White, Modifier.weight(1f), isSmallScreen) { onBuscarCliente() }
                FuncaoBotao("F7", "Limpar", Icons.Default.Delete, Color.White, Modifier.weight(1f), isSmallScreen) { onLimpar() }
                if (permAcessos) FuncaoBotao("F8", "Auditoria", Icons.Default.Assessment, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToAcessos() }
                if (permCadastro) FuncaoBotao("F9", "Cadastro", Icons.Default.AppRegistration, Color.White, Modifier.weight(1f), isSmallScreen) { onNavigateToCadastro() }
                // Botão FINALIZAR (destaque)
                Button(
                    onClick = onFinalizar,
                    modifier = Modifier.weight(2f).fillMaxHeight().padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (caixaAberto) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    ),
                    enabled = carrinho.isNotEmpty() || !caixaAberto
                ) {
                    Icon(
                        if (caixaAberto) Icons.Default.CheckCircle else Icons.Default.Lock,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (caixaAberto) "FINALIZAR" else "ABRIR CAIXA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FuncaoBotao(
    codigo: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    modifier: Modifier = Modifier,
    isSmallScreen: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(if (isSmallScreen) 22.dp else 24.dp))
        if (label.isNotBlank()) {
            Text(label, color = textColor, fontSize = if (isSmallScreen) 9.sp else 10.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        }
    }
}

// ==================== DIALOG: GRID DE PRODUTOS ====================

@Composable
private fun ProdutoGridDialog(
    produtos: List<Produto>,
    categorias: List<Categoria>,
    categoriaSelecionada: Categoria?,
    busca: String,
    carrinho: List<ItemCarrinho>,
    onBuscaChange: (String) -> Unit,
    onCategoriaChange: (Categoria?) -> Unit,
    onProdutoClick: (Produto) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    vendidosPorProduto: Map<Long, Int>
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Produtos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                // Campo de busca estilizado
                OutlinedTextField(
                    value = busca,
                    onValueChange = onBuscaChange,
                    placeholder = { Text("Buscar produto...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Abas de categorias - chips horizontais
                if (categorias.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = categoriaSelecionada == null,
                                onClick = { onCategoriaChange(null) },
                                label = { Text("Todos", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryColor
                                )
                            )
                        }
                        items(categorias) { cat ->
                            FilterChip(
                                selected = categoriaSelecionada?.id == cat.id,
                                onClick = { onCategoriaChange(if (categoriaSelecionada?.id == cat.id) null else cat) },
                                label = { Text(cat.nome, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid de produtos - 4 colunas compactas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(produtos, key = { it.id }) { produto ->
                        val qtdNoCarrinho = carrinho.find { it.produtoId == produto.id }?.quantidade?.toInt() ?: 0
                        val vendidos = vendidosPorProduto[produto.id] ?: 0
                        val semEstque = produto.estoque <= 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!semEstque) onProdutoClick(produto) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (semEstque) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Imagem do produto
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (produto.imagem != null) {
                                            val imageUrl = if (produto.imagem!!.startsWith("http") || produto.imagem!!.startsWith("data:")) produto.imagem!! else "${com.seucaixa.caixacombo.service.PollingService.getServerUrl()}${produto.imagem}"
                                            coil.compose.AsyncImage(
                                                model = imageUrl,
                                                contentDescription = produto.nome,
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Inventory, null,
                                                modifier = Modifier.size(22.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Nome do produto
                                    Text(
                                        produto.nome,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (semEstque) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Preço
                                    Text(
                                        produto.precoFormatado(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (semEstque) primaryColor.copy(alpha = 0.3f) else primaryColor
                                    )

                                    // Estoque
                                    if (semEstque) {
                                        Text(
                                            "ESGOTADO",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else if (produto.estoque <= 5) {
                                        Text(
                                            "Estq: ${produto.estoque.toInt()}",
                                            fontSize = 8.sp,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                }

                                // Badge quantidade no carrinho
                                if (qtdNoCarrinho > 0) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                        containerColor = Color(0xFF4CAF50)
                                    ) {
                                        Text("${qtdNoCarrinho}x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== DIALOG: VENDA SUCESSO ====================

@Composable
fun DialogVendaSucesso(
    venda: Venda,
    onDismiss: () -> Unit,
    nomeCliente: String? = null
) {
    val context = LocalContext.current
    val printService = remember { com.seucaixa.caixacombo.service.SunmiPrintService(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Erro de Impressão") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        title = {
            Text("Venda Finalizada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total: R$ %.2f".format(venda.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Forma: ${venda.formaPagamento.name.replace("_", " ")}", style = MaterialTheme.typography.bodyMedium)
                        Text("Nº ${venda.numero}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Opções de Impressão:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

                Button(
                    onClick = {
                        try {
                            isPrinting = true
                            printService.imprimirVenda(venda, nomeCliente = nomeCliente)
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = "Erro ao imprimir: ${e.message}"
                            isPrinting = false
                        }
                    },
                    enabled = !isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imprimir Total")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                isPrinting = true
                                venda.itens.forEach { item ->
                                    repeat(item.quantidade.toInt()) {
                                        printService.imprimirFichaProducao(
                                            item = item,
                                            numeroVenda = venda.numero,
                                            dataHora = venda.dataHora,
                                            formaPagamento = venda.formaPagamento.name.replace("_", " "),
                                            quantidadeUnidade = 1
                                        )
                                        delay(800)
                                    }
                                }
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = "Erro ao imprimir fichas: ${e.message}"
                                isPrinting = false
                            }
                        }
                    },
                    enabled = !isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val totalUnidades = venda.itens.sumOf { it.quantidade.toInt() }
                    Text("Imprimir Fichas ($totalUnidades unidades)")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}
