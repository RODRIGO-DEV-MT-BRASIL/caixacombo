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
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.service.PollingService
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import com.seucaixa.caixacombo.ui.components.ProdutoImagem
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenPremium(
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

    var usuarioLogado by remember { mutableStateOf<Usuario?>(null) }
    var empresaNome by remember { mutableStateOf("PREMIUM") }
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(Unit) {
        try {
            val operatorId = com.seucaixa.caixacombo.data.SecurePrefs.getOperatorId(context)
            if (operatorId > 0) {
                val dao = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(context).usuarioDao()
                usuarioLogado = dao.getUsuarioById(operatorId)
            }
            val empresaDao = AppDatabase.getDatabase(context).empresaDao()
            val empresa = empresaDao.getEmpresaOnce()
            if (empresa != null) {
                val nome = empresa.nomeFantasia.ifBlank { empresa.razaoSocial }
                if (nome.isNotBlank()) empresaNome = nome
            }
            val logoFile = java.io.File(context.filesDir, "logo.png")
            if (logoFile.exists()) {
                logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckoutPremium", "Erro ao carregar dados", e)
        }
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
    var showBusca by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showCarrinhoDialog by remember { mutableStateOf(false) }
    var clienteSelecionado by remember { mutableStateOf<Cliente?>(null) }

    var stonePaymentResult by remember { mutableStateOf<StoneDeeplinkService.PaymentResult?>(null) }
    var stonePaymentError by remember { mutableStateOf<String?>(null) }
    var paymentProcessed by remember { mutableStateOf(false) }
    val isStoneAvailable = remember { StoneDeeplinkService.isStoneInstalled(context) }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("dd MMM HH:mm", Locale("pt", "BR")).format(Date())
            delay(1000)
        }
    }

    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(
            venda = ultimaVenda!!,
            onDismiss = { viewModel.resetVendaFinalizada() },
            nomeCliente = clienteSelecionado?.nome
        )
    }

    stonePaymentError?.let { error ->
        AlertDialog(
            onDismissRequest = { stonePaymentError = null },
            title = { Text("Erro no Pagamento") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { stonePaymentError = null }) { Text("OK") } }
        )
    }

    fun processarPagamento(forma: FormaPagamento) {
        showFormaPagamentoDialog = false
        stonePaymentError = null
        paymentProcessed = false
        when (forma) {
            FormaPagamento.DINHEIRO -> {
                showValorDialog = true
            }
            FormaPagamento.PIX, FormaPagamento.CARTAO_CREDITO, FormaPagamento.CARTAO_DEBITO -> {
                if (isStoneAvailable && onSendStonePayment != null && StoneDeeplinkService.shouldUseStone(forma)) {
                    val transactionType = StoneDeeplinkService.mapFormaPagamentoToStone(forma) ?: return
                    val centavos = (total * 100).toLong()
                    stonePaymentResult = null
                    onSendStonePayment?.invoke(centavos, transactionType, StoneDeeplinkService.InstallmentType.NONE, "") { result ->
                        if (paymentProcessed) return@invoke
                        if (result != null && result.success) {
                            paymentProcessed = true
                            stonePaymentResult = result
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
            else -> {
                viewModel.finalizarVenda(forma, 0.0, null, null)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().background(primaryColor).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (logoBitmap != null) {
                        androidx.compose.foundation.Image(
                            painter = BitmapPainter(logoBitmap!!.asImageBitmap()),
                            contentDescription = "Logo",
                            modifier = Modifier.height(28.dp).width(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(empresaNome, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentTime, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                if (permCaixa) IconButton(onClick = onNavigateToCaixa, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.AccountBalance, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                if (permVendas) IconButton(onClick = onNavigateToVendas, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Receipt, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                if (permProdutos) IconButton(onClick = onNavigateToProdutos, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Inventory, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                if (permConfig) IconButton(onClick = onNavigateToConfiguracaoTipoImpressao, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Print, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                IconButton(onClick = onNavigateToDashboard, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Dashboard, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                IconButton(onClick = onLogout, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ExitToApp, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }

        if (showBusca) {
            OutlinedTextField(value = searchText, onValueChange = { searchText = it; viewModel.buscarProdutos(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = { showBusca = false; searchText = ""; viewModel.buscarProdutos("") }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    item {
                        FilterChip(selected = categoriaSelecionada == null, onClick = { viewModel.selecionarCategoria(null) },
                            label = { Text("Todos", fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor))
                    }
                    items(categorias) { cat ->
                        FilterChip(selected = categoriaSelecionada?.id == cat.id, onClick = { viewModel.selecionarCategoria(cat) },
                            label = { Text(cat.nome, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor))
                    }
                }
                IconButton(onClick = { showBusca = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filtered = if (busca.isNotBlank()) produtos.filter { it.nome.contains(busca, true) || (it.codigoBarras ?: "").contains(busca) }
                else if (categoriaSelecionada != null) produtos.filter { it.categoriaId == categoriaSelecionada!!.id }
                else produtos
            items(filtered.take(60)) { produto ->
                val vendidos = vendidosPorProduto[produto.id] ?: 0
                Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { viewModel.adicionarAoCarrinho(produto) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            ProdutoImagem(
                                imagem = produto.imagem,
                                contentDescription = produto.nome,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                serverUrl = PollingService.getServerUrl()
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(produto.nome, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (produto.descricao != null && produto.descricao!!.isNotBlank()) {
                            Text(produto.descricao!!, fontSize = 7.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        Text("R$ ${"%.2f".format(produto.precoVenda)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Est:${"%.0f".format(produto.estoque)}", fontSize = 7.sp, color = Color.Gray)
                            Text(" | ", fontSize = 7.sp, color = Color.Gray.copy(alpha = 0.3f))
                            Text("Ven:$vendidos", fontSize = 7.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TOTAL", fontSize = 9.sp, color = Color.Gray)
                    Text("R$ ${"%.2f".format(total)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                if (carrinho.isNotEmpty()) {
                    OutlinedButton(onClick = { showCarrinhoDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Itens (${carrinho.size})", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Button(onClick = { if (carrinho.isNotEmpty()) showFormaPagamentoDialog = true },
                    enabled = carrinho.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("VENDER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showFormaPagamentoDialog) {
        AlertDialog(
            onDismissRequest = { showFormaPagamentoDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = backgroundColor,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Pagamento", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("R$ ${"%.2f".format(total)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    PaymentOptionCard(
                        icon = Icons.Default.Money,
                        label = "Dinheiro",
                        desc = "Receber em espécie",
                        color = Color(0xFF4CAF50),
                        onClick = { processarPagamento(FormaPagamento.DINHEIRO) }
                    )
                    PaymentOptionCard(
                        icon = Icons.Default.QrCode,
                        label = "PIX",
                        desc = if (isStoneAvailable) "Pagamento via PIX Stone" else "Pagamento PIX",
                        color = Color(0xFF0080FF),
                        enabled = isStoneAvailable,
                        onClick = { processarPagamento(FormaPagamento.PIX) }
                    )
                    PaymentOptionCard(
                        icon = Icons.Default.CreditCard,
                        label = "Cartão de Crédito",
                        desc = if (isStoneAvailable) "Pagamento via Stone" else "Pagamento offline",
                        color = Color(0xFF9C27B0),
                        onClick = { processarPagamento(FormaPagamento.CARTAO_CREDITO) }
                    )
                    PaymentOptionCard(
                        icon = Icons.Default.CreditScore,
                        label = "Cartão de Débito",
                        desc = if (isStoneAvailable) "Pagamento via Stone" else "Pagamento offline",
                        color = Color(0xFFE91E63),
                        onClick = { processarPagamento(FormaPagamento.CARTAO_DEBITO) }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFormaPagamentoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCarrinhoDialog) {
        AlertDialog(
            onDismissRequest = { showCarrinhoDialog = false },
            containerColor = backgroundColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Carrinho (${carrinho.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.limparCarrinho(); showCarrinhoDialog = false }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                        Spacer(Modifier.width(2.dp))
                        Text("Limpar", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            text = {
                if (carrinho.isEmpty()) {
                    Text("Carrinho vazio", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(carrinho) { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.produtoNome, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("R$ ${"%.2f".format(item.precoUnitario)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.atualizarQuantidade(item.produtoId, item.quantidade - 1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                    }
                                    Text("%.0f".format(item.quantidade), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 20.dp), textAlign = TextAlign.Center)
                                    IconButton(onClick = { viewModel.atualizarQuantidade(item.produtoId, item.quantidade + 1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("R$ ${"%.2f".format(item.total)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
                                IconButton(onClick = { viewModel.removerDoCarrinho(item.produtoId) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total: R$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
                    Button(onClick = { showCarrinhoDialog = false; showFormaPagamentoDialog = true }) { Text("VENDER") }
                }
            }
        )
    }

    if (showValorDialog) {
        AlertDialog(onDismissRequest = { showValorDialog = false },
            title = { Text("Valor Recebido", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = valorRecebido, onValueChange = { valorRecebido = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) },
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
                if (valorRecebido.isNotBlank()) {
                    val numVal = valorRecebido.toDoubleSafe()
                    if (numVal >= total) {
                        Text("Troco: R$ ${"%.2f".format(numVal - total)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                    } else if (numVal > 0) {
                        Text("Faltam: R$ ${"%.2f".format(total - numVal)}", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = { Button(onClick = {
                val valor = valorRecebido.toDoubleSafe()
                viewModel.finalizarVenda(FormaPagamento.DINHEIRO, valor, null, null)
                showValorDialog = false; valorRecebido = ""
            }, enabled = valorRecebido.toDoubleSafe() >= total) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showValorDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PaymentOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (enabled) color.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (enabled) Color.Black else Color.Gray)
                Text(desc, fontSize = 11.sp, color = if (enabled) Color.Black.copy(alpha = 0.5f) else Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
