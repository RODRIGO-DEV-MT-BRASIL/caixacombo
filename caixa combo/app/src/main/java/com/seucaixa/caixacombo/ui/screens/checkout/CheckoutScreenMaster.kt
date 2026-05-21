package com.seucaixa.caixacombo.ui.screens.checkout

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.service.PollingService
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import com.seucaixa.caixacombo.ui.components.ProdutoImagem
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenMaster(
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

    val primaryColor by remember { mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) }
    val backgroundColor by remember { mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFF5F5F5.toInt()))) }
    val accentColor by remember { mutableStateOf(Color(sharedPreferences.getInt("accent_color", primaryColor.hashCode()))) }

    val surfaceCard = if (backgroundColor == Color(0xFFF5F5F5) || backgroundColor == Color(0xFF121212))
        Color(0xFFFFFFFF) else backgroundColor.copy(alpha = 0.15f)
    val textOnBg = if (backgroundColor == Color(0xFFF5F5F5) || backgroundColor == Color(0xFFFFFFFF)) Color(0xFF1A1A2E) else Color.White
    val textSecondary = if (backgroundColor == Color(0xFFF5F5F5) || backgroundColor == Color(0xFFFFFFFF)) Color.Gray else Color.White.copy(alpha = 0.6f)

    var usuarioLogado by remember { mutableStateOf<Usuario?>(null) }
    var empresaNome by remember { mutableStateOf("") }
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(Unit) {
        try {
            val operatorId = com.seucaixa.caixacombo.data.SecurePrefs.getOperatorId(context)
            if (operatorId > 0) {
                val dao = AppDatabase.getDatabase(context).usuarioDao()
                usuarioLogado = dao.getUsuarioById(operatorId)
            }
            val nomePrefs = sharedPreferences.getString("empresa_nome", "")
            if (nomePrefs.isNullOrBlank()) {
                val empresaDao = AppDatabase.getDatabase(context).empresaDao()
                val empresa = empresaDao.getEmpresaOnce()
                if (empresa != null) {
                    val nome = empresa.nomeFantasia.ifBlank { empresa.razaoSocial }
                    if (nome.isNotBlank()) empresaNome = nome
                }
            } else {
                empresaNome = nomePrefs
            }
            val logoFile = java.io.File(context.filesDir, "logo.png")
            if (logoFile.exists()) {
                logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            }
        } catch (e: Exception) { android.util.Log.e("CheckoutMaster", "Erro ao carregar dados", e) }
    }

    val isAdmin = usuarioLogado?.cargo == CargoUsuario.ADMIN
    val permCaixa = isAdmin || usuarioLogado?.permCaixa == true
    val permVendas = isAdmin || usuarioLogado?.permVendas == true
    val permProdutos = isAdmin || usuarioLogado?.permProdutos == true
    val permConfig = isAdmin || usuarioLogado?.permConfiguracoes == true

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
    var showValorDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }
    var showBuscaDialog by remember { mutableStateOf(false) }
    var buscaText by remember { mutableStateOf("") }
    var clienteSelecionado by remember { mutableStateOf<Cliente?>(null) }

    var stonePaymentError by remember { mutableStateOf<String?>(null) }
    var paymentProcessed by remember { mutableStateOf(false) }
    val isStoneAvailable = remember { StoneDeeplinkService.isStoneInstalled(context) }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
            delay(1000)
        }
    }

    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(venda = ultimaVenda!!, onDismiss = { viewModel.resetVendaFinalizada() }, nomeCliente = null)
    }

    stonePaymentError?.let { error ->
        AlertDialog(
            onDismissRequest = { stonePaymentError = null },
            title = { Text("Erro no Pagamento", fontWeight = FontWeight.Bold) },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { stonePaymentError = null }) { Text("OK") } }
        )
    }

    fun processarPagamento(forma: FormaPagamento) {
        showFormaPagamentoDialog = false
        stonePaymentError = null
        paymentProcessed = false
        when (forma) {
            FormaPagamento.DINHEIRO -> showValorDialog = true
            FormaPagamento.PIX, FormaPagamento.CARTAO_CREDITO, FormaPagamento.CARTAO_DEBITO -> {
                if (isStoneAvailable && onSendStonePayment != null && StoneDeeplinkService.shouldUseStone(forma)) {
                    val transactionType = StoneDeeplinkService.mapFormaPagamentoToStone(forma) ?: return
                    val centavos = (total * 100).toLong()
                    onSendStonePayment?.invoke(centavos, transactionType, StoneDeeplinkService.InstallmentType.NONE, "") { result ->
                        if (paymentProcessed) return@invoke
                        if (result != null && result.success) {
                            paymentProcessed = true
                            stonePaymentError = null
                            val stoneAtk = result.authorizationCode.ifEmpty { null }
                            viewModel.finalizarVenda(forma, total, clienteSelecionado?.id, stoneAtk)
                        } else {
                            val reason = result?.reason ?: ""
                            val code = result?.code ?: 0
                            stonePaymentError = when {
                                code == 401 -> "Terminal não ativado na Stone"
                                code == 1000 -> "App Stone não encontrado"
                                result == null -> "Pagamento cancelado ou não concluído"
                                reason.contains("NOT_FOUND") -> "App de pagamento não encontrado"
                                reason.isNotBlank() -> "Pagamento recusado: $reason (código: $code)"
                                else -> "Pagamento recusado no terminal (código: $code)"
                            }
                        }
                    }
                } else {
                    viewModel.finalizarVenda(forma, 0.0, null, null)
                }
            }
            else -> viewModel.finalizarVenda(forma, 0.0, null, null)
        }
    }

    if (showBuscaDialog) {
        Dialog(onDismissRequest = { showBuscaDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Buscar Produto", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textOnBg, modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(
                        value = buscaText, onValueChange = { buscaText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Código ou nome do produto", color = textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textOnBg,
                            unfocusedTextColor = textOnBg,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textSecondary,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(onClick = { showBuscaDialog = false; viewModel.buscarProdutos(buscaText) }) {
                                Icon(Icons.Default.Search, null, tint = primaryColor)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        val filtered = if (buscaText.isBlank()) emptyList() else produtos.filter {
                            it.nome.contains(buscaText, ignoreCase = true) || (it.codigoBarras ?: "").contains(buscaText)
                        }.take(20)
                        items(filtered) { produto ->
                            ListItem(
                                headlineContent = { Text(produto.nome, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textOnBg) },
                                supportingContent = { Text("R$ ${"%.2f".format(produto.precoVenda)}", color = primaryColor) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    viewModel.adicionarAoCarrinho(produto)
                                    showBuscaDialog = false
                                    buscaText = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(primaryColor).padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (logoBitmap != null) {
                        androidx.compose.foundation.Image(
                            painter = BitmapPainter(logoBitmap!!.asImageBitmap()),
                            contentDescription = "Logo",
                            modifier = Modifier.height(36.dp).width(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(if (empresaNome.isNotBlank()) empresaNome else "MASTER POS", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentTime, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                IconButton(onClick = { showBuscaDialog = true }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.weight(0.28f).fillMaxHeight().padding(8.dp)) {
                Text("CARRINHO", fontSize = 10.sp, color = textSecondary, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(carrinho) { item ->
                        CarrinhoItemMaster(item = item, onRemove = { viewModel.removerDoCarrinho(item.produtoId) }, onUpdateQtd = { qtd -> viewModel.atualizarQuantidade(item.produtoId, qtd.toDouble()) }, primaryColor = primaryColor, textOnBg = textOnBg, textSecondary = textSecondary, containerColor = surfaceCard)
                    }
                    if (carrinho.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Carrinho vazio", color = textSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(primaryColor).padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TOTAL", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                            Text("R$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (carrinho.isNotEmpty()) {
                            Button(
                                onClick = { showFormaPagamentoDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text("PAGAR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = primaryColor)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(0.72f).fillMaxHeight().padding(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    item {
                        FilterChip(
                            selected = categoriaSelecionada == null,
                            onClick = { viewModel.selecionarCategoria(null) },
                            label = { Text("Todos", fontSize = 13.sp, color = if (categoriaSelecionada == null) Color.White else textOnBg) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                containerColor = surfaceCard
                            )
                        )
                    }
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaSelecionada?.id == cat.id,
                            onClick = { viewModel.selecionarCategoria(cat) },
                            label = { Text(cat.nome, fontSize = 13.sp, color = if (categoriaSelecionada?.id == cat.id) Color.White else textOnBg) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                containerColor = surfaceCard
                            )
                        )
                    }
                }
                val filtered = if (categoriaSelecionada != null) produtos.filter { it.categoriaId == categoriaSelecionada!!.id } else produtos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered.take(30)) { produto ->
                        val vendidos = vendidosPorProduto[produto.id] ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.adicionarAoCarrinho(produto) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProdutoImagem(
                                        imagem = produto.imagem,
                                        contentDescription = produto.nome,
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        serverUrl = PollingService.getServerUrl()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(produto.nome, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = textOnBg, modifier = Modifier.fillMaxWidth())
                                Text("R$ ${"%.2f".format(produto.precoVenda)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Text("Est:${"%.0f".format(produto.estoque)}", fontSize = 9.sp, color = textSecondary)
                                    Text(" | ", fontSize = 9.sp, color = textSecondary.copy(alpha = 0.3f))
                                    Text("V:$vendidos", fontSize = 9.sp, color = textSecondary)
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty() && produtos.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum produto nesta categoria", color = textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                if (produtos.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nenhum produto disponível", color = textSecondary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Aguardando sincronização...", color = textSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surfaceCard).padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NavButtonMaster(icon = Icons.Default.Receipt, label = "Vendas", enabled = permVendas, onClick = onNavigateToVendas, primaryColor = primaryColor, textSecondary = textSecondary)
                    NavButtonMaster(icon = Icons.Default.Inventory, label = "Produtos", enabled = permProdutos, onClick = onNavigateToProdutos, primaryColor = primaryColor, textSecondary = textSecondary)
                    NavButtonMaster(icon = Icons.Default.AccountBalance, label = "Caixa", enabled = permCaixa, onClick = onNavigateToCaixa, primaryColor = primaryColor, textSecondary = textSecondary)
                    NavButtonMaster(icon = Icons.Default.Print, label = "Config", enabled = permConfig, onClick = onNavigateToConfiguracaoTipoImpressao, primaryColor = primaryColor, textSecondary = textSecondary)
                    NavButtonMaster(icon = Icons.Default.ExitToApp, label = "Sair", enabled = true, onClick = onLogout, primaryColor = primaryColor, textSecondary = textSecondary)
                }
            }
        }
    }

    if (showFormaPagamentoDialog) {
        FormaPagamentoDialogMaster(total = total, onDismiss = { showFormaPagamentoDialog = false }, onSelect = { forma ->
            processarPagamento(forma)
        }, primaryColor = primaryColor, surfaceColor = surfaceCard, textColor = textOnBg)
    }

    if (showValorDialog) {
        ValorDialogoMaster(total = total, onDismiss = { showValorDialog = false }, onConfirm = { valor ->
            val numVal = valor.toDoubleSafe()
            viewModel.finalizarVenda(FormaPagamento.DINHEIRO, numVal, null, null)
            showValorDialog = false
        }, primaryColor = primaryColor, surfaceColor = surfaceCard, textColor = textOnBg)
    }
}

@Composable
fun ValorDialogoMaster(total: Double, onDismiss: () -> Unit, onConfirm: (String) -> Unit, primaryColor: Color, surfaceColor: Color, textColor: Color) {
    var valor by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = surfaceColor,
        title = { Text("Valor Recebido", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column {
                OutlinedTextField(
                    value = valor, onValueChange = { valor = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Valor", color = textColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("R$", fontWeight = FontWeight.Bold, color = textColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                        cursorColor = primaryColor
                    )
                )
                if (valor.isNotBlank()) {
                    val numVal = valor.toDoubleSafe()
                    if (numVal >= total) {
                        Text("Troco: R$ ${"%.2f".format(numVal - total)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                    } else {
                        Text("Faltam: R$ ${"%.2f".format(total - numVal)}", color = Color.Red, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(valor) }, enabled = valor.toDoubleSafe() >= total, colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(10.dp)) { Text("Confirmar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = textColor.copy(alpha = 0.6f)) } }
    )
}

@Composable
fun CarrinhoItemMaster(item: ItemCarrinho, onRemove: () -> Unit, onUpdateQtd: (Double) -> Unit, primaryColor: Color, textOnBg: Color, textSecondary: Color, containerColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(containerColor).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.produtoNome, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textOnBg)
                Text("R$ ${"%.2f".format(item.precoUnitario)}", fontSize = 10.sp, color = textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdateQtd(item.quantidade - 1) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Remove, null, tint = textOnBg, modifier = Modifier.size(16.dp)) }
                Text("${item.quantidade.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textOnBg, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { onUpdateQtd(item.quantidade + 1) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Add, null, tint = textOnBg, modifier = Modifier.size(16.dp)) }
            }
            Text("R$ ${"%.2f".format(item.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = primaryColor.copy(alpha = 0.6f)) }
        }
    }
}

@Composable
fun FormaPagamentoDialogMaster(total: Double, onDismiss: () -> Unit, onSelect: (FormaPagamento) -> Unit, primaryColor: Color, surfaceColor: Color, textColor: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = surfaceColor,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Pagamento", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text("R$ ${"%.2f".format(total)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                val opcoes = listOf(
                    Triple(FormaPagamento.DINHEIRO, "Dinheiro", "Receber em espécie") to Icons.Default.Money,
                    Triple(FormaPagamento.PIX, "PIX", "Pagamento instantâneo") to Icons.Default.QrCode,
                    Triple(FormaPagamento.CARTAO_CREDITO, "Crédito", "Cartão de crédito") to Icons.Default.CreditCard,
                    Triple(FormaPagamento.CARTAO_DEBITO, "Débito", "Cartão de débito") to Icons.Default.CreditScore
                )
                opcoes.forEach { (triple, icon) ->
                    val (forma, label, desc) = triple
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(forma) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(primaryColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
                                Text(desc, fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = textColor.copy(alpha = 0.6f)) } }
    )
}

@Composable
fun NavButtonMaster(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, primaryColor: Color, textSecondary: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled) { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = if (enabled) primaryColor else textSecondary.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = if (enabled) primaryColor else textSecondary.copy(alpha = 0.3f))
    }
}
